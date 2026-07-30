import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const assembler = fs.readFileSync(new URL('../junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/runtime/LcRuntimePageAssembler.java', import.meta.url), 'utf8')
const bpmn = fs.readFileSync(new URL('../junsong-modules/junsong-workflow/src/main/resources/processes/stocktake-apply.bpmn20.xml', import.meta.url), 'utf8')
const lcBizService = fs.readFileSync(new URL('../junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/lowcode/service/impl/LcBizServiceImpl.java', import.meta.url), 'utf8')
const taskService = fs.readFileSync(new URL('../junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/service/task/WorkflowTaskService.java', import.meta.url), 'utf8')

test('运行时字段必须保留 fieldExt，盘点流程必须初始化 needRecount', () => {
  assert.match(assembler, /fieldExt/)
  assert.match(bpmn, /execution\.getVariable\('needRecount'\)/)
  assert.match(lcBizService, /putIfAbsent\("needRecount", Boolean\.FALSE\)/)
  assert.match(taskService, /putIfAbsent\("needRecount", Boolean\.FALSE\)/)
})
