import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const expenseMapperXml = readFileSync(
  'junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinExpenseMapper.xml',
  'utf8',
)

function extractSelectBody(xml, id) {
  const pattern = new RegExp(`<select\\s+id="${id}"[^>]*>([\\s\\S]*?)<\\/select>`)
  const match = xml.match(pattern)
  assert.ok(match, `Expected to find select id="${id}"`)
  return match[1].replace(/\s+/g, ' ').trim().toLowerCase()
}

test('operation dashboard unverified expense amount uses the same net amount口径 as expense summary', () => {
  const summarySql = extractSelectBody(expenseMapperXml, 'sumUnverifiedExpensesByDeptId')
  const dashboardSql = extractSelectBody(expenseMapperXml, 'sumUnverifiedExpenseAmount')

  assert.match(summarySql, /sum\(expense_amount\)/)
  assert.match(dashboardSql, /sum\(expense_amount\)/)
  assert.doesNotMatch(dashboardSql, /sum\(abs\(expense_amount\)\)/)
})

test('cashflow dashboard uses existing finance amount columns', () => {
  // SQL 列引用实际位于 Mapper XML（而非 ServiceImpl Java 代码）
  const source = readFileSync(
    'junsong-modules/junsong-finance/src/main/resources/mapper/finance/CashflowDashboardMapper.xml',
    'utf8',
  )
  // fin_expense uses expense_amount, not current_amount
  assert.doesNotMatch(source, /current_amount/)
  // fin_investor_payment uses amount, not payment_amount
  assert.doesNotMatch(source, /payment_amount\).*FROM fin_investor_payment/i)
  // Must use expense_amount
  assert.match(source, /expense_amount/i)
  // Must use amount for investor payment
  assert.match(source, /SUM\(amount\).*FROM fin_investor_payment/is)
  // R7 回修：现金流日期必须按业务日期统计，不能用 create_time 做日期过滤
  assert.doesNotMatch(source, /create_time\s+BETWEEN/is,
    '现金流不得用 create_time 做日期过滤，应使用业务日期字段')
  assert.match(source, /payment_date\s+BETWEEN/is, 'fin_sale_payment 应按 payment_date 过滤')
  assert.match(source, /expense_date\s+BETWEEN/is, 'fin_expense 应按 expense_date 过滤')
})

test('finance review task menu sql defines parent variables and avoids all-role grants', () => {
  const sql = readFileSync('sql/finance_review_task.sql', 'utf8')

  // Must define @financeRootId
  assert.match(sql, /SET\s+@financeRootId\s*:=/i)
  // Must NOT have CROSS JOIN granting to all active roles
  assert.doesNotMatch(sql, /FROM\s+sys_role\s+r\s+CROSS\s+JOIN\s+sys_menu\s+m/is)
  // Must grant only to role_id=1 (admin) or use role_key
  assert.match(sql, /role_id\s*=\s*1|role_key\s+in/i)
})
