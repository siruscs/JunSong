import { execFileSync } from 'node:child_process'
import assert from 'node:assert/strict'

const base = 'http://localhost/prod-api'
const password = 'admin123'
const runId = `RTN-${Date.now()}`

function curlJson(method, path, token, body, idempotencyKey) {
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
  const output = execFileSync('docker', args, { encoding: 'utf8' })
  let parsed
  try {
    parsed = JSON.parse(output)
  } catch (error) {
    throw new Error(`接口返回不是 JSON: ${method} ${path}\n${output}`)
  }
  if (parsed.code !== 200) {
    throw new Error(`接口失败: ${method} ${path}\n${JSON.stringify(parsed, null, 2)}`)
  }
  return parsed
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
SELECT t.ID_, t.NAME_, IFNULL(t.ASSIGNEE_, ''), t.PROC_INST_ID_, t.TASK_DEF_KEY_,
       GROUP_CONCAT(il.USER_ID_ ORDER BY il.USER_ID_) candidate_users
FROM ACT_RU_TASK t
LEFT JOIN ACT_RU_IDENTITYLINK il ON il.TASK_ID_=t.ID_
WHERE t.PROC_INST_ID_=${sqlLiteral(processInstanceId)}
GROUP BY t.ID_, t.NAME_, t.ASSIGNEE_, t.PROC_INST_ID_, t.TASK_DEF_KEY_
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

function rejectToNode(token, taskId, targetActivityId, comment) {
  return curlJson('POST', `/workflow/task/${taskId}/reject`, token, {
    comment,
    targetType: 'RETURN_TO_NODE',
    targetActivityId,
    resubmitMode: 'RETURN_TO_NODE',
  }, `${runId}-reject-node-${taskId}-${Date.now()}`)
}

function getInstanceRow(instanceId) {
  const sql = `
SELECT id, biz_code, order_no, workflow_status, current_task_name,
       process_instance_id, approval_round, last_reject_mode, last_reject_reason
FROM lc_biz_instance
WHERE id=${Number(instanceId)}
`
  const [line] = mysql(sql)
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
  if (!line) throw new Error(`未找到低代码业务单据: ${instanceId}`)
  const [id, bizCode, orderNo, workflowStatus, currentTaskName, processInstanceId, approvalRound, lastRejectMode, lastRejectReason] = line.split('\t')
  return { id: Number(id), bizCode, orderNo, workflowStatus, currentTaskName, processInstanceId, approvalRound: Number(approvalRound), lastRejectMode, lastRejectReason }
}

function countRuntimeTasks(processInstanceId) {
  const [line] = mysql(`SELECT COUNT(*) FROM ACT_RU_TASK WHERE PROC_INST_ID_=${sqlLiteral(processInstanceId)}`)
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
  return Number(line || 0)
}

const adminToken = login('admin')
const wjsToken = login('wjs')

const form = {
  dept_id: 100,
  scope_type: 'SELECTED_PRODUCTS',
  counter_user_id: 1,
  recount_user_id: 103,
  stocktake_items: [
    {
      product_id: 8,
      actual_quantity: 22,
    },
  ],
  remark: `${runId} 退回指定节点验收`,
}

const saveResp = curlJson('POST', '/workflow/lowcode/biz/stocktake', adminToken, form, `${runId}-save`)
const instanceId = Number(saveResp.data)
assert.ok(instanceId > 0, '保存低代码业务单据未返回有效 ID')

const submitResp = curlJson('POST', `/workflow/lowcode/biz/stocktake/${instanceId}/submit`, adminToken, undefined, `${runId}-submit`)
const processInstanceId = submitResp.data?.processInstanceId
assert.ok(processInstanceId, '提交未返回流程实例 ID')

let adminTask = findTask(processInstanceId, 'admin', '盘点人录入')
approve(adminToken, adminTask.taskId, `${runId} ADMIN 盘点通过`)

let wjsTask = findTask(processInstanceId, 'wjs', '复盘人复盘')
rejectToNode(wjsToken, wjsTask.taskId, 'Task_Count', `${runId} WJS 退回盘点人重新录入`)

adminTask = findTask(processInstanceId, 'admin', '盘点人录入')
assert.equal(adminTask.taskDefKey, 'Task_Count', '退回指定节点后未回到 Task_Count')
assert.equal(countRuntimeTasks(processInstanceId), 1, '退回指定节点后运行任务数不正确')

approve(adminToken, adminTask.taskId, `${runId} ADMIN 二次盘点通过`)
wjsTask = findTask(processInstanceId, 'wjs', '复盘人复盘')
approve(wjsToken, wjsTask.taskId, `${runId} WJS 二次复盘通过`)
adminTask = findTask(processInstanceId, 'admin', '审批人审批')
approve(adminToken, adminTask.taskId, `${runId} ADMIN 终审通过`)

const finalRow = getInstanceRow(instanceId)
assert.equal(finalRow.workflowStatus, 'APPROVED')
assert.equal(countRuntimeTasks(processInstanceId), 0, '完成后流程实例仍存在运行任务')

const auditSql = `
SELECT
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE PROC_INST_ID_=${sqlLiteral(processInstanceId)} AND TASK_DEF_KEY_='Task_Count' AND ASSIGNEE_='admin') admin_count_history,
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE PROC_INST_ID_=${sqlLiteral(processInstanceId)} AND TASK_DEF_KEY_='Task_Recount' AND ASSIGNEE_='wjs') wjs_recount_history,
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE PROC_INST_ID_=${sqlLiteral(processInstanceId)} AND TASK_DEF_KEY_='Task_Approve' AND ASSIGNEE_='admin') admin_approve_history
`
const [auditLine] = mysql(auditSql).split('\n').map((item) => item.trim()).filter(Boolean)
const [adminCountHistory, wjsRecountHistory, adminApproveHistory] = auditLine.split('\t').map(Number)
assert.ok(adminCountHistory >= 2, '退回指定节点后 ADMIN 盘点节点历史不足两次')
assert.ok(wjsRecountHistory >= 2, '退回指定节点后 WJS 复盘节点历史不足两次')
assert.ok(adminApproveHistory >= 1, '退回指定节点后 ADMIN 终审历史缺失')

console.log(JSON.stringify({
  status: 'PASS',
  runId,
  instanceId,
  orderNo: finalRow.orderNo,
  processInstanceId,
  workflowStatus: finalRow.workflowStatus,
  audit: {
    adminCountHistory,
    wjsRecountHistory,
    adminApproveHistory,
  },
}, null, 2))
