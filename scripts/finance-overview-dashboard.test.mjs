import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const file = path.resolve('junsong-ui-v3/src/views/finance/overview/index.vue')
const source = fs.readFileSync(file, 'utf8')

assert.match(source, /经营结论/)
assert.match(source, /风险待办/)
assert.match(source, /趋势与排行/)
assert.match(source, /报表工作台/)

for (const label of ['销售经营分析', '利润分析', '费用异常', '分润结算', '库存价值与对账', '门店经营分析']) {
  assert.match(source, new RegExp(label))
}

for (const route of [
  '/finance/report/sale',
  '/finance/report/profit',
  '/finance/report/expense',
  '/finance/report/profitShare',
  '/finance/report/stock',
  '/finance/report/store'
]) {
  assert.match(source, new RegExp(route.replaceAll('/', '\\/')))
}

for (const endpoint of ['/finance/dashboard/alerts', '/finance/cashflow/dashboard', '/finance/cashflow-forecast/dashboard']) {
  assert.match(source, new RegExp(endpoint.replaceAll('/', '\\/')))
}

console.log('finance overview dashboard static checks passed')
