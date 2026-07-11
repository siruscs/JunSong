import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R16 sql creates forecast snapshot and permissions', () => {
  const sql = read('sql/finance_cashflow_forecast_r16.sql')
  assert.match(sql, /CREATE TABLE IF NOT EXISTS finance_cashflow_forecast_snapshot/i)
  assert.match(sql, /finance:cashflowForecast:view/)
  assert.match(sql, /finance:cashflowForecast:snapshot/)
})

test('R16 backend exposes cashflow forecast endpoints with permissions', () => {
  const controller = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/CashflowForecastController.java')
  assert.match(controller, /@RequiresPermissions\("finance:cashflowForecast:view"\)/)
  assert.match(controller, /@RequiresPermissions\("finance:cashflowForecast:snapshot"\)/)
  assert.match(controller, /\/cashflow-forecast\/dashboard/)
  assert.match(controller, /\/cashflow-forecast\/snapshot/)
})

test('R16 service uses promised payments and pressure scoring', () => {
  const service = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/CashflowForecastServiceImpl.java')
  assert.match(service, /promisedAmount/)
  assert.match(service, /windowDays/)
  assert.match(service, /pressureScore/)
  assert.match(service, /CRITICAL/)
  assert.match(service, /forecastDeviation/)
})

test('R16 frontend adds forecast page and overview card', () => {
  const api = read('junsong-ui-v3/src/api/finance/cashflowForecast.ts')
  const page = read('junsong-ui-v3/src/views/finance/cashflowForecast/index.vue')
  const overview = read('junsong-ui-v3/src/views/finance/overview/index.vue')
  assert.match(api, /getCashflowForecastDashboard/)
  assert.match(api, /createCashflowForecastSnapshot/)
  assert.match(page, /现金流预测/)
  assert.match(page, /未来7天/)
  assert.match(page, /未来14天/)
  assert.match(page, /未来30天/)
  assert.match(page, /现金压力指数/)
  assert.match(overview, /现金流预测/)
})
