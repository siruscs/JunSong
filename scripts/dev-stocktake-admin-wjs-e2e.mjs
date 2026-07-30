import { execFileSync } from 'node:child_process'
import assert from 'node:assert/strict'

const base = 'http://localhost/prod-api'
const password = 'admin123'
const runId = `E2E-${Date.now()}`

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

function curlJsonRaw(method, path, token, body, idempotencyKey) {
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
  try {
    return JSON.parse(output)
  } catch (error) {
    throw new Error(`接口返回不是 JSON: ${method} ${path}\n${output}`)
  }
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

function findTask(_token, processInstanceId, expectedAssignee, expectedNamePart) {
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

function approve(token, taskId, comment, variables) {
  return curlJson('POST', `/workflow/task/${taskId}/approve`, token, {
    comment,
    variables: variables || {},
  }, `${runId}-approve-${taskId}-${Date.now()}`)
}

function approveRaw(token, taskId, comment, idempotencyKey) {
  return curlJsonRaw('POST', `/workflow/task/${taskId}/approve`, token, {
    comment,
    variables: {},
  }, idempotencyKey || `${runId}-approve-raw-${taskId}-${Date.now()}`)
}

function rejectToInitiator(token, taskId, comment) {
  return curlJson('POST', `/workflow/task/${taskId}/reject`, token, {
    comment,
    targetType: 'INITIATOR_MODIFY',
    resubmitMode: 'FULL_RESTART',
  }, `${runId}-reject-${taskId}-${Date.now()}`)
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
      actual_quantity: 20,
    },
  ],
  remark: `${runId} 库存盘点接口链路`,
}

const saveResp = curlJson('POST', '/workflow/lowcode/biz/stocktake', adminToken, form, `${runId}-save`)
const instanceId = Number(saveResp.data)
assert.ok(instanceId > 0, '保存低代码业务单据未返回有效 ID')

const submitResp = curlJson('POST', `/workflow/lowcode/biz/stocktake/${instanceId}/submit`, adminToken, undefined, `${runId}-submit-1`)
const firstProcessInstanceId = submitResp.data?.processInstanceId
assert.ok(firstProcessInstanceId, '首次提交未返回流程实例 ID')

let adminTask = findTask(adminToken, firstProcessInstanceId, 'admin', '盘点人录入')
const unauthorizedApprove = approveRaw(wjsToken, adminTask.taskId, `${runId} WJS 越权审批 ADMIN 任务`)
assert.notEqual(unauthorizedApprove.code, 200, '非当前处理人不应能审批 ADMIN 任务')
approve(adminToken, adminTask.taskId, `${runId} ADMIN 盘点通过`)

let wjsTask = findTask(wjsToken, firstProcessInstanceId, 'wjs', '复盘人复盘')
rejectToInitiator(wjsToken, wjsTask.taskId, `${runId} wjs 驳回给发起人修改`)

let rejected = getInstanceRow(instanceId)
assert.equal(rejected.workflowStatus, 'REJECTED')
assert.equal(rejected.currentTaskName, '发起人修改')
assert.equal(rejected.lastRejectMode, 'FULL_RESTART')
assert.equal(countRuntimeTasks(firstProcessInstanceId), 0, '驳回给发起人修改后旧流程实例仍存在运行任务')

const updatedForm = {
  ...form,
  remark: `${runId} ADMIN 修改后重新提交`,
  stocktake_items: [
    {
      product_id: 8,
      actual_quantity: 21,
    },
  ],
}
curlJson('PUT', `/workflow/lowcode/biz/stocktake/${instanceId}`, adminToken, updatedForm, `${runId}-update`)

const resubmitResp = curlJson('POST', `/workflow/lowcode/biz/stocktake/${instanceId}/submit`, adminToken, undefined, `${runId}-submit-2`)
const secondProcessInstanceId = resubmitResp.data?.processInstanceId
assert.ok(secondProcessInstanceId, '重新提交未返回流程实例 ID')
assert.notEqual(secondProcessInstanceId, firstProcessInstanceId, '重新提交应创建新的流程实例')

adminTask = findTask(adminToken, secondProcessInstanceId, 'admin', '盘点人录入')
approve(adminToken, adminTask.taskId, `${runId} ADMIN 重新盘点通过`)

wjsTask = findTask(wjsToken, secondProcessInstanceId, 'wjs', '复盘人复盘')
approve(wjsToken, wjsTask.taskId, `${runId} wjs 复盘通过`)

adminTask = findTask(adminToken, secondProcessInstanceId, 'admin', '审批人审批')
approve(adminToken, adminTask.taskId, `${runId} ADMIN 终审通过`)
const duplicateFinalApprove = approveRaw(adminToken, adminTask.taskId, `${runId} ADMIN 重复终审`, `${runId}-duplicate-final-${adminTask.taskId}`)
assert.notEqual(duplicateFinalApprove.code, 200, '已完成任务不应允许重复审批')

const finalRow = getInstanceRow(instanceId)
assert.equal(finalRow.workflowStatus, 'APPROVED')
assert.equal(finalRow.approvalRound, 2)
assert.equal(countRuntimeTasks(secondProcessInstanceId), 0, '完成后新流程实例仍存在运行任务')

const auditSql = `
SELECT
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE PROC_INST_ID_='${firstProcessInstanceId}' AND ASSIGNEE_='wjs') first_wjs_history,
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE PROC_INST_ID_='${secondProcessInstanceId}' AND ASSIGNEE_='wjs') second_wjs_history,
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE PROC_INST_ID_='${secondProcessInstanceId}' AND ASSIGNEE_='admin') second_admin_history,
  (SELECT COUNT(*) FROM sys_notification WHERE user_id IN (1,103) AND biz_id IN ('${firstProcessInstanceId}', '${secondProcessInstanceId}')) notification_count
`
const [auditLine] = mysql(auditSql).split('\n').map((item) => item.trim()).filter(Boolean)
const [firstWjsHistory, secondWjsHistory, secondAdminHistory, notificationCount] = auditLine.split('\t').map(Number)
assert.ok(firstWjsHistory >= 1, '首次流程未记录 wjs 历史任务')
assert.ok(secondWjsHistory >= 1, '重提流程未记录 wjs 历史任务')
assert.ok(secondAdminHistory >= 2, '重提流程未记录 admin 盘点和终审任务')
assert.ok(notificationCount >= 2, '流程通知未按本次流程实例写入')

console.log(JSON.stringify({
  status: 'PASS',
  runId,
  instanceId,
  orderNo: finalRow.orderNo,
  firstProcessInstanceId,
  secondProcessInstanceId,
  approvalRound: finalRow.approvalRound,
  workflowStatus: finalRow.workflowStatus,
  audit: {
    firstWjsHistory,
    secondWjsHistory,
    secondAdminHistory,
    notificationCount,
    unauthorizedApproveCode: unauthorizedApprove.code,
    duplicateFinalApproveCode: duplicateFinalApprove.code,
  },
}, null, 2))
