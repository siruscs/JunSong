import fs from 'node:fs'

const ddl = fs.readFileSync('sql/member_purchase_domain.sql', 'utf8')
const requiredMetrics = ['member_purchase_amount', 'member_receivable_amount', 'member_delivered_quantity']
const reconciliation = fs.readFileSync('sql/member_purchase_domain_reconcile.sql', 'utf8')
if (!ddl.includes('mem_member_metric_snapshot')) throw new Error('metric snapshot table is missing')
if (!reconciliation.includes('mem_purchase_order') || !reconciliation.includes('mem_purchase_item')) {
  throw new Error('reconciliation does not cover purchase domain')
}
console.log(`Metric contract ready: ${requiredMetrics.join(', ')}`)
