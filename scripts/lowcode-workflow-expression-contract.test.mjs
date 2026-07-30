import test from 'node:test'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'

const stocktakeXmlPath = new URL('../junsong-modules/junsong-workflow/src/main/resources/processes/stocktake-apply.bpmn20.xml', import.meta.url)

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

function extractExpressionVariables(xml) {
  const variables = new Set()
  for (const match of xml.matchAll(/\$\{([^}]+)}/g)) {
    const expression = match[1]
    for (const variable of expression.matchAll(/execution\.getVariable\(['"]([A-Za-z][A-Za-z0-9_]*)['"]\)/g)) {
      variables.add(variable[1])
    }
    const withoutStringLiterals = expression.replaceAll(/(['"]).*?\1/g, '')
    for (const token of withoutStringLiterals.matchAll(/\b[A-Za-z][A-Za-z0-9_]*\b/g)) {
      const name = token[0]
      if (['true', 'false', 'null', 'execution', 'getVariable'].includes(name)) continue
      variables.add(name)
    }
  }
  return [...variables].sort()
}

function loadConfiguredProcessVariables(bizCode) {
  const sql = `
SELECT process_var_name
FROM lc_biz_node_assignee
WHERE biz_code='${bizCode}' AND tenant_id=1 AND del_flag='0' AND process_var_name IS NOT NULL AND process_var_name <> ''
UNION
SELECT target_var_name
FROM lc_biz_branch_rule
WHERE biz_code='${bizCode}' AND tenant_id=1 AND del_flag='0' AND target_var_name IS NOT NULL AND target_var_name <> ''
UNION
SELECT COALESCE(NULLIF(process_var_name, ''), field_key)
FROM lc_biz_field
WHERE biz_code='${bizCode}' AND tenant_id=1 AND del_flag='0' AND is_process_var='1'
`
  return mysql(sql).split('\n').map((item) => item.trim()).filter(Boolean).sort()
}

test('stocktake BPMN expression variables are backed by low-code runtime config', () => {
  const xml = readFileSync(stocktakeXmlPath, 'utf8')
  const expressionVariables = extractExpressionVariables(xml)
  const configuredVariables = loadConfiguredProcessVariables('stocktake')
  const configured = new Set(configuredVariables)
  const missing = expressionVariables.filter((variable) => !configured.has(variable))

  assert.deepEqual(expressionVariables, [
    'approverUsername',
    'counterUsername',
    'needRecount',
    'recountUsername',
  ])
  assert.deepEqual(missing, [], `BPMN 表达式变量未被低代码配置装配: ${missing.join(', ')}`)
})

test('stocktake BPMN conditions read optional variables safely through execution.getVariable', () => {
  const xml = readFileSync(stocktakeXmlPath, 'utf8')
  const unsafeDirectConditions = [...xml.matchAll(/<conditionExpression[\s\S]*?\$\{([^}]+)}[\s\S]*?<\/conditionExpression>/g)]
    .map((match) => match[1])
    .filter((expression) => /\bneedRecount\b/.test(expression) && !/execution\.getVariable\(['"]needRecount['"]\)/.test(expression))

  assert.deepEqual(unsafeDirectConditions, [], '网关条件不得直接使用 ${needRecount == true}，必须使用 execution.getVariable 防缺省变量报错')
})
