import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(path, 'utf8')

test('lowcode runtime endpoint reads the published snapshot', () => {
  const source = read('junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/controller/LcMetadataController.java')
  assert.match(source, /getLatestPublishedConfigJson\(bizCode\)/)
  assert.match(source, /runtimePageAssembler\.assemble\(schema, fields\)/)
  const runtimeMethod = source.slice(source.indexOf('public R<LcRuntimePageAssembler.RuntimePage> runtimePage'))
  assert.doesNotMatch(runtimeMethod, /selectPageSchemaList\(query\)/)
})

test('runtime config selection excludes draft snapshots', () => {
  const mapper = read('junsong-modules/junsong-workflow/src/main/resources/mapper/lowcode/LcBizConfigSnapshotMapper.xml')
  const service = read('junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/service/impl/LcConfigVersionServiceImpl.java')
  assert.match(mapper, /selectMaxPublishedVersionByBizCode/)
  assert.match(mapper, /status = 'PUBLISHED'/)
  assert.match(service, /selectMaxPublishedVersionByBizCode\(bizCode\)/)
})

test('PC form/list/detail pages consume the runtime model with compatibility fallback', () => {
  const form = read('junsong-ui-v3/src/views/lowcode/SchemaForm.vue')
  const detail = read('junsong-ui-v3/src/views/lowcode/SchemaDetail.vue')
  const list = read('junsong-ui-v3/src/views/lowcode/SchemaList.vue')
  for (const [source, pageType] of [[form, 'FORM'], [detail, 'DETAIL'], [list, 'LIST']]) {
    assert.match(source, new RegExp(`getRuntimePage\\([^\\n]+['"]${pageType}['"]`))
    assert.match(source, /catch\(\(\) => null\)/)
  }
})

test('lowcode list does not request runtime Schema for a non-lowcode process key', () => {
  const list = read('junsong-ui-v3/src/views/lowcode/SchemaList.vue')
  assert.match(list, /if \(!bizObject\.value\)/)
  assert.match(list, /只有确认存在低代码对象后才请求运行时 Schema/)
})

test('lowcode write endpoints have backend idempotency protection', () => {
  const biz = read('junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/controller/LcBizController.java')
  const meta = read('junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/controller/LcMetadataController.java')
  assert.match(biz, /scene = "lowcode:biz:save"/)
  assert.match(biz, /scene = "lowcode:biz:submit"/)
  assert.match(meta, /scene = "lowcode:meta:config-save"/)
  assert.match(meta, /scene = "lowcode:meta:publish"/)
})

test('new workflow starts force a fresh idempotency key', () => {
  const start = read('junsong-ui-v3/src/views/workflow/start/index.vue')
  const instanceApi = read('junsong-ui-v3/src/api/workflow/instance.ts')
  const lowcodeApi = read('junsong-ui-v3/src/api/lowcode/index.ts')
  assert.match(start, /idempotencyNewKey:\s*true/)
  assert.match(instanceApi, /idempotencyNewKey\?/) 
  assert.match(lowcodeApi, /submitBizInstance\([^)]*options\?/) 
})

test('process assignee UI uses backend assigneeSource contract and UserSelect', () => {
  const source = read('junsong-ui-v3/src/views/lowcode/admin/edit.vue')
  assert.match(source, /v-model="row\.assigneeSource"/)
  assert.match(source, /<UserSelect v-if="row\.assigneeSource === 'FIXED_USER'/)
  assert.doesNotMatch(source, /v-model="row\.assigneeType"/)
})

test('history route with a process instance uses single-instance detail mode', () => {
  const history = read('junsong-ui-v3/src/views/workflow/history/index.vue')
  assert.match(history, /const singleInstanceMode = computed\(\(\) => Boolean\(String\(route\.query\.processInstanceId/)
  assert.match(history, /<el-form v-if="!singleInstanceMode"/)
  assert.match(history, /<RightToolbar v-if="!singleInstanceMode"/)
  assert.match(history, /<div v-if="!singleInstanceMode" class="history-layout__table">/)
  assert.match(history, /if \(queryInstanceId\) \{[\s\S]*?await loadByInstanceId\(queryInstanceId\)/)
})

test('gift_apply snapshot repair preserves nested page schema JSON as a string', () => {
  const sql = read('sql/workflow_dev_gift_apply_schema_repair.sql')
  assert.match(sql, /JSON_OBJECT\(/)
  assert.match(sql, /'fields', NULL/)
  assert.match(sql, /'workflowEnabled', '1'/)
  assert.match(sql, /'processKey', 'gift_apply'/)
  assert.match(sql, /JSON_UNQUOTE\(JSON_QUOTE\('\{\"fields\"/)
  assert.match(sql, /config_json LIKE CONCAT\([\s\S]*schemaJson/)
})

test('task detail renders business form data and keeps actions visible', () => {
  const task = read('junsong-ui-v3/src/views/workflow/task/index.vue')
  assert.match(task, /FieldRenderer :model-value="detailDrawer\.data\.businessForm\?\./)
  assert.match(task, /workflow-task-drawer__actions/)
  assert.match(task, /position:\s*sticky/)
})

test('history detail attaches business form from instance detail response', () => {
  const history = read('junsong-ui-v3/src/views/workflow/history/index.vue')
  assert.match(history, /businessForm:\s*detail\.businessForm\s*\|\|\s*\{\}/)
  assert.match(history, /<el-row :gutter="16">/)
})

test('instance detail uses lowcode grid and exposes rejected or withdrawn edit entry', () => {
  const instance = read('junsong-ui-v3/src/views/workflow/instance/index.vue')
  const runtime = read('junsong-ui-v3/src/views/workflow/shared/runtime.ts')
  assert.match(instance, /<el-row :gutter="16">/)
  assert.match(instance, /\['REJECTED', 'WITHDRAWN'\]/)
  assert.match(runtime, /gift_apply: \{ path: '\/lowcode\/gift_apply'/)
})
