import { execFile, execFileSync } from 'node:child_process'
import { promisify } from 'node:util'
import assert from 'node:assert/strict'

const execFileAsync = promisify(execFile)
const base = 'http://localhost/prod-api'
const password = 'admin123'
const runId = `CONC-${Date.now()}`

function curlArgs(method, path, token, body, idempotencyKey) {
  const args = [
    'exec',
    'junsong-nginx',
    'curl',
    '-s',
    '-X',
    method,
    `${base}${path}`,
    '-H',
    'Content-Type: application/json',
    '-H',
    'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Genesis-E2E',
  ]
  if (token) args.push('-H', `Authorization: Bearer ${token}`)
  if (idempotencyKey) args.push('-H', `X-Idempotency-Key: ${idempotencyKey}`)
  if (body !== undefined) args.push('-d', JSON.stringify(body))
  return args
}

function parseJson(output, label) {
  try {
    return JSON.parse(output)
  } catch (error) {
    throw new Error(`接口返回不是 JSON: ${label}\n${output}`)
  }
}

function curlJson(method, path, token, body, idempotencyKey) {
  const output = execFileSync('docker', curlArgs(method, path, token, body, idempotencyKey), { encoding: 'utf8' })
  const parsed = parseJson(output, `${method} ${path}`)
  if (parsed.code !== 200) {
    throw new Error(`接口失败: ${method} ${path}\n${JSON.stringify(parsed, null, 2)}`)
  }
  return parsed
}

async function curlJsonRawAsync(method, path, token, body, idempotencyKey) {
  const { stdout } = await execFileAsync('docker', curlArgs(method, path, token, body, idempotencyKey), { encoding: 'utf8' })
  return parseJson(stdout, `${method} ${path}`)
}

function login(username) {
  const response = curlJson('POST', '/auth/login', null, {
    username,
    password,
    code: '',
    uuid: '',
  })
  const token = response.data?.access_token
  assert.ok(token, `${username} 登录未返回 token`)
  return token
}

function mysql(sql) {
  return execFileSync('docker', [
    'exec',
    'junsong-mysql',
    'mysql',
    '--default-character-set=utf8mb4',
    '-uroot',
    '-proot_123',
    'junsong-cloud',
    '-N',
    '-e',
    sql,
  ], { encoding: 'utf8' })
}

function sqlLiteral(value) {
  return `'${String(value).replaceAll("'", "''")}'`
}

function findTask(processInstanceId, expectedAssignee, expectedNamePart) {
  const where = [
    `t.PROC_INST_ID_=${sqlLiteral(processInstanceId)}`,
  ]
  if (expectedAssignee) {
    where.push(`(t.ASSIGNEE_=${sqlLiteral(expectedAssignee)} OR EXISTS (SELECT 1 FROM ACT_RU_IDENTITYLINK il WHERE il.TASK_ID_=t.ID_ AND il.USER_ID_=${sqlLiteral(expectedAssignee)}))`)
  }
  if (expectedNamePart) where.push(`t.NAME_ LIKE ${sqlLiteral(`%${expectedNamePart}%`)}`)
  const query = `
SELECT t.ID_, t.NAME_, IFNULL(t.ASSIGNEE_, ''), t.PROC_INST_ID_, t.TASK_DEF_KEY_
FROM ACT_RU_TASK t
WHERE ${where.join(' AND ')}
ORDER BY t.CREATE_TIME_ DESC
LIMIT 1
`
  const [line] = mysql(query).split('\n').map((item) => item.trim()).filter(Boolean)
  if (!line) {
    const current = mysql(`
SELECT t.ID_, t.NAME_, IFNULL(t.ASSIGNEE_, ''), t.PROC_INST_ID_, t.TASK_DEF_KEY_
FROM ACT_RU_TASK t
WHERE t.PROC_INST_ID_=${sqlLiteral(processInstanceId)}
ORDER BY t.CREATE_TIME_ DESC
`)
    throw new Error(`未找到待办: pi=${processInstanceId}, assignee=${expectedAssignee}, name=${expectedNamePart}\n当前流程待办=\n${current}`)
  }
  const [taskId, taskName, assignee, pi, taskDefKey] = line.split('\t')
  return { taskId, taskName, assignee, processInstanceId: pi, taskDefKey }
}

function approve(token, taskId, comment) {
  return curlJson('POST', `/workflow/task/${taskId}/approve`, token, {
    comment,
    variables: {},
  }, `${runId}-approve-${taskId}-${Date.now()}`)
}

function getInstanceRow(instanceId) {
  const [line] = mysql(`
SELECT id, order_no, workflow_status, process_instance_id
FROM lc_biz_instance
WHERE id=${Number(instanceId)}
`).split('\n').map((item) => item.trim()).filter(Boolean)
  if (!line) throw new Error(`未找到低代码业务单据: ${instanceId}`)
  const [id, orderNo, workflowStatus, processInstanceId] = line.split('\t')
  return { id: Number(id), orderNo, workflowStatus, processInstanceId }
}

const adminToken = login('admin')

const form = {
  dept_id: 100,
  scope_type: 'SELECTED_PRODUCTS',
  counter_user_id: 1,
  recount_user_id: null,
  stocktake_items: [
    {
      product_id: 8,
      actual_quantity: 23,
    },
  ],
  remark: `${runId} 并发审批验收`,
}

const saveResp = curlJson('POST', '/workflow/lowcode/biz/stocktake', adminToken, form, `${runId}-save`)
const instanceId = Number(saveResp.data)
assert.ok(instanceId > 0, '保存低代码业务单据未返回有效 ID')

const submitResp = curlJson('POST', `/workflow/lowcode/biz/stocktake/${instanceId}/submit`, adminToken, undefined, `${runId}-submit`)
const processInstanceId = submitResp.data?.processInstanceId
assert.ok(processInstanceId, '提交未返回流程实例 ID')

let adminTask = findTask(processInstanceId, 'admin', '盘点人录入')
const concurrentResults = await Promise.all([
  curlJsonRawAsync('POST', `/workflow/task/${adminTask.taskId}/approve`, adminToken, {
    comment: `${runId} 并发审批 A`,
    variables: {},
  }, `${runId}-concurrent-a`),
  curlJsonRawAsync('POST', `/workflow/task/${adminTask.taskId}/approve`, adminToken, {
    comment: `${runId} 并发审批 B`,
    variables: {},
  }, `${runId}-concurrent-b`),
])
const successCount = concurrentResults.filter((result) => result.code === 200).length
assert.equal(successCount, 1, `同一任务并发审批应该只有一个成功，实际结果=${JSON.stringify(concurrentResults)}`)

adminTask = findTask(processInstanceId, 'admin', '审批人审批')
approve(adminToken, adminTask.taskId, `${runId} ADMIN 终审通过`)

const finalRow = getInstanceRow(instanceId)
assert.equal(finalRow.workflowStatus, 'APPROVED')

const [historyLine] = mysql(`
SELECT
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE PROC_INST_ID_=${sqlLiteral(processInstanceId)} AND TASK_DEF_KEY_='Task_Count') count_task_history,
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE PROC_INST_ID_=${sqlLiteral(processInstanceId)} AND TASK_DEF_KEY_='Task_Approve') approve_task_history
`).split('\n').map((item) => item.trim()).filter(Boolean)
const [countTaskHistory, approveTaskHistory] = historyLine.split('\t').map(Number)
assert.equal(countTaskHistory, 1, '并发审批导致盘点节点历史重复')
assert.equal(approveTaskHistory, 1, '并发审批导致终审节点历史重复')

console.log(JSON.stringify({
  status: 'PASS',
  runId,
  instanceId,
  orderNo: finalRow.orderNo,
  processInstanceId,
  workflowStatus: finalRow.workflowStatus,
  concurrentCodes: concurrentResults.map((result) => result.code),
  audit: {
    countTaskHistory,
    approveTaskHistory,
  },
}, null, 2))
