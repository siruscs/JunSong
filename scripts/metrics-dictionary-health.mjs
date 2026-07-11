import { readFileSync } from 'node:fs'

const dictionaryPath = 'docs/superpowers/plans/metrics-dictionary.zh-CN.md'
const requiredColumns = ['metricKey', 'metricName', 'module', 'sourceTables', 'businessDate', 'authScope', 'health']
const forbiddenPatterns = [
  /@Scheduled/,
  /Quartz/,
  /Webhook subscription/i,
  /短信|企微|公众号|小程序发送/,
  /what-if|预测模型|黑盒/,
  /压测/,
]

function parseRows(markdown) {
  return markdown
    .split('\n')
    .filter((line) => line.startsWith('|') && !line.includes('---'))
    .map((line) => line.split('|').slice(1, -1).map((cell) => cell.trim()))
    .filter((cells) => cells.length >= 10 && (cells[0] === 'metricKey' || cells[0]?.includes('.')))
}

export function checkMetricsDictionary({ path = dictionaryPath } = {}) {
  const markdown = readFileSync(path, 'utf8')
  const rows = parseRows(markdown)
  const errors = []
  const keySet = new Set()
  const modules = { finance: 0, member: 0, system: 0, stock: 0 }
  let currentHeader = []

  for (const cells of rows) {
    if (cells.includes('metricKey')) {
      currentHeader = cells
      for (const column of requiredColumns) {
        if (!currentHeader.includes(column)) {
          errors.push(`missing required column: ${column}`)
        }
      }
      continue
    }

    const row = Object.fromEntries(currentHeader.map((header, index) => [header, cells[index] || '']))
    if (!row.metricKey) errors.push(`missing metricKey in row: ${cells.join(' | ')}`)
    if (keySet.has(row.metricKey)) errors.push(`duplicate metricKey: ${row.metricKey}`)
    keySet.add(row.metricKey)

    for (const column of requiredColumns) {
      if (!row[column]) errors.push(`${row.metricKey} missing ${column}`)
    }
    if (Object.hasOwn(modules, row.module)) modules[row.module] += 1
  }

  for (const [moduleName, count] of Object.entries(modules)) {
    if (count === 0) errors.push(`module has no metrics: ${moduleName}`)
  }

  const forbiddenMatches = forbiddenPatterns
    .filter((pattern) => pattern.test(markdown))
    .map((pattern) => pattern.toString())

  return {
    ok: errors.length === 0 && forbiddenMatches.length === 0,
    totalMetrics: keySet.size,
    modules,
    errors,
    forbiddenMatches,
  }
}

if (process.argv[1] && process.argv[1].endsWith('metrics-dictionary-health.mjs')) {
  const report = checkMetricsDictionary()
  console.log(JSON.stringify(report, null, 2))
  if (!report.ok) process.exit(1)
}
