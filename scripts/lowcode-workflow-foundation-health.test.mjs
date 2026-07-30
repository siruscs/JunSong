import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(path, 'utf8')

test('stocktake BPMN assignee variables are provided by runnable low-code config', () => {
  const bpmn = read('junsong-modules/junsong-workflow/src/main/resources/processes/stocktake-apply.bpmn20.xml')
  const sql = read('sql/dev_stocktake_lowcode_config.sql')

  for (const variable of ['counterUsername', 'recountUsername', 'approverUsername']) {
    assert.match(bpmn, new RegExp(variable), `BPMN must reference ${variable}`)
    assert.match(sql, new RegExp(variable), `DEV low-code config must provide ${variable}`)
  }

  assert.match(
    sql,
    /SELECT 'Task_Approve','库存审批','FIXED_USER','admin','approverUsername'/,
    'Task_Approve must provide approverUsername from a fixed username in the DEV baseline'
  )
  assert.match(
    sql,
    /SET assignee_source='FIXED_USER', assignee_value='admin', process_var_name='approverUsername'[\s\S]*?task_key='Task_Approve'/,
    'Repeated DEV migrations must repair existing Task_Approve assignee config'
  )
  assert.match(
    sql,
    /Gateway_NeedRecount[\s\S]*recount_user_id[\s\S]*NOT_EMPTY[\s\S]*needRecount/,
    'DEV SQL must provide a low-code branch rule that produces needRecount for the BPMN gateway'
  )
})
