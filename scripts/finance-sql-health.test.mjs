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
  const source = readFileSync(
    'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceCashflowReportServiceImpl.java',
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
