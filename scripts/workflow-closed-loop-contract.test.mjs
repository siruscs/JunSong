import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const root = new URL('..', import.meta.url).pathname
const instance = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/domain/LcBizInstance.java`, 'utf8')
const mapper = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/resources/mapper/lowcode/LcBizInstanceMapper.xml`, 'utf8')
const task = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/service/task/WorkflowTaskService.java`, 'utf8')
const sql = fs.readFileSync(`${root}/sql/workflow_closed_loop_migration.sql`, 'utf8')
const taskView = fs.readFileSync(`${root}/junsong-ui-v3/src/views/workflow/task/index.vue`, 'utf8')
const instanceView = fs.readFileSync(`${root}/junsong-ui-v3/src/views/workflow/instance/index.vue`, 'utf8')
const historyView = fs.readFileSync(`${root}/junsong-ui-v3/src/views/workflow/history/index.vue`, 'utf8')
const workflowRuntime = fs.readFileSync(`${root}/junsong-ui-v3/src/views/workflow/shared/runtime.ts`, 'utf8')
const startView = fs.readFileSync(`${root}/junsong-ui-v3/src/views/workflow/start/index.vue`, 'utf8')
const assemble = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/service/LcWorkflowAssembleService.java`, 'utf8')
const taskController = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/controller/TaskController.java`, 'utf8')
const metadataController = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/controller/LcMetadataController.java`, 'utf8')
const notificationMapper = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/mapper/WfNotificationMapper.java`, 'utf8')
const orphanRepair = fs.readFileSync(`${root}/sql/workflow_orphan_task_repair.sql`, 'utf8')
const lowcodeService = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/service/impl/LcBizServiceImpl.java`, 'utf8')
const genericSync = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/sync/GenericLowcodeWorkflowSyncHandler.java`, 'utf8')
test('generic low-code handler excludes specialized stocktake workflow', () => {
  assert.match(genericSync, /"stocktake_apply"\.equals\(key\)/)
})
const stocktakeSync = fs.readFileSync(`${root}/junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/service/sync/StocktakeWorkflowSyncHandler.java`, 'utf8')
test('stocktake sync handler has priority over generic low-code handler', () => {
  assert.match(stocktakeSync, /@Order\(0\)\s+public class StocktakeWorkflowSyncHandler/)
  assert.match(stocktakeSync, /int priority\(\)\s*\{\s*return 100;/)
  assert.match(stocktakeSync, /PROCESS_KEY\.equals\(processDefinitionId\)/)
})
const stocktakeService = fs.readFileSync(`${root}/junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinStocktakeServiceImpl.java`, 'utf8')

test('low-code instances persist approval round and rejection metadata', () => {
  assert.match(instance, /approvalRound/)
  assert.match(mapper, /approval_round/)
  assert.match(sql, /ADD COLUMN.*approval_round/i)
})

test('task detail exposes business form and does not expose raw flowable variables', () => {
  assert.match(task, /businessForm/)
  assert.doesNotMatch(task, /result\.put\("variables"/)
  assert.match(taskView, /detailDrawer\.data\.businessForm/)
  assert.doesNotMatch(taskView, /detailDrawer\.data\.variables/) 
})

test('task center keeps a compact overview and exposes start workflow entry', () => {
  assert.match(taskView, /runtime-overview--compact/)
  assert.match(taskView, /发起新流程/)
  assert.match(taskView, /router\.push\(['"]\/workflow\/start['"]\)/)
  assert.match(taskView, /<el-button type="primary" @click="router\.push\('\/workflow\/start'\)">发起新流程<\/el-button>/)
})

test('rejection requires a reason and notification points to the newly created task', () => {
  assert.match(task, /驳回原因不能为空|comment\.isBlank\(\)/)
  assert.match(task, /t\.getId\(\)/)
  assert.match(task, /isAllowedRejectTarget/)
})

test('task rejection UI exposes initiator modify and explicit return-to-node semantics', () => {
  assert.match(taskView, /targetType/)
  assert.match(taskView, /INITIATOR_MODIFY/)
  assert.match(taskView, /RETURN_TO_NODE/)
  assert.match(taskView, /驳回给发起人修改/)
  assert.match(taskView, /重新走完整流程/)
  assert.match(taskView, /重新提交到驳回节点/)
  assert.doesNotMatch(taskView, /确认终止|流程已终止|将终止整个流程实例/)
})

test('workflow snapshot persists approval round and migration uses the real notification table', () => {
  assert.match(mapper, /approval_round = #\{approvalRound\}/)
  assert.match(sql, /sys_notification/)
  assert.doesNotMatch(sql, /FROM wf_notification/)
})

test('instance and history details render configured business fields', () => {
  for (const source of [instanceView, historyView]) {
    assert.match(source, /getRuntimePage\([^\n]+['"]FORM['"]\)/)
    assert.match(source, /<FieldRenderer[\s\S]*businessForm/) 
    assert.doesNotMatch(source, /JSON\.stringify\([^)]*businessForm/)
  }
})

test('workflow start form uses the same runtime schema and remains editable', () => {
  assert.match(startView, /getRuntimePage\([^\n]+['"]FORM['"]\)/)
  assert.match(startView, /:span="fieldSpan\(field\)"/)
  assert.match(startView, /parseFieldExt/)
  assert.doesNotMatch(startView, /:readonly="true"/)
})

test('Flowable identities and high-risk workflow commands use username and idempotency', () => {
  assert.match(assemble, /Flowable 身份统一使用 username/)
  assert.match(assemble, /selectUserNameByUserId/)
  assert.doesNotMatch(assemble, /setAssignee\([^\n]*userId/)
  assert.match(taskController, /scene = "workflow:task:reject"/)
  assert.match(taskController, /scene = "workflow:task:approve"/)
  assert.match(notificationMapper, /INSERT IGNORE INTO sys_notification/)
})

test('workflow task users can resolve published business objects without low-code admin permission', () => {
  assert.match(metadataController, /@GetMapping\("\/object\/list"\)[\s\S]{0,160}@PreAuthorize|@PreAuthorize\("@ss\.hasPermi\('lowcode:meta:list'\) or @ss\.hasPermi\('workflow:task:list'\)"\)[\s\S]{0,160}@GetMapping\("\/object\/list"\)/)
})

test('generic low-code rejected workflows keep a business edit target', () => {
  assert.match(workflowRuntime, /fallback.*lowcode.*processDefinitionKey|`\/lowcode\/\$\{processDefinitionKey\}`/s)
  assert.match(workflowRuntime, /queryKey: 'orderNo'/)
})

test('initiator can edit and resubmit rejected records from applied tasks', () => {
  assert.match(taskView, /activeTab === 'applied'/)
  assert.match(taskView, /修改\/重新提交/)
})

test('orphan task repair is dry-run by default and only repairs deterministic usernames', () => {
  assert.match(orphanRepair, /SET @apply_repair := 0/)
  assert.match(orphanRepair, /ACT_HI_TASKINST/)
  assert.match(orphanRepair, /NOT REGEXP '\^\[0-9\]\+\$'/)
  assert.match(orphanRepair, /WHERE @apply_repair = 1/)
  assert.match(orphanRepair, /remaining_empty/)
  assert.doesNotMatch(orphanRepair, /DROP TABLE\s|TRUNCATE\s|DELETE\s+FROM\s+ACT_/i)
})

test('completed low-code stocktake synchronizes the business form into the native stocktake state machine', () => {
  assert.match(lowcodeService, /variables\.put\("formData", formData\)/)
  assert.match(lowcodeService, /variables\.put\("businessKey", instance\.getOrderNo\(\)\)/)
  assert.match(stocktakeSync, /setAction\("COMPLETE"\)/)
  assert.match(stocktakeSync, /setFormData\(formData\)/)
  assert.match(stocktakeService, /createAndPostWorkflowStocktake/)
  assert.match(stocktakeService, /postStocktake\(id, approved\.getVersion\(\)\)/)
  assert.match(stocktakeService, /selectStocktakeByTakeNo/)
})
