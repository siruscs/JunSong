import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const required = [
  'sql/member_purchase_domain.sql',
  'sql/member_purchase_domain_reconcile.sql',
  'sql/member_purchase_menu.sql',
  'junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemPurchaseController.java',
  'junsong-ui-v3/src/api/member/purchase.ts',
  'junsong-miniprogram/src/api/memberPurchase.js'
]
const missing = required.filter((file) => !fs.existsSync(path.join(root, file)))
if (missing.length) {
  console.error('Missing member purchase release files:', missing.join(', '))
  process.exit(1)
}
const ddl = fs.readFileSync(path.join(root, 'sql/member_purchase_domain.sql'), 'utf8')
for (const table of ['mem_identity_policy', 'mem_member_no_sequence', 'mem_purchase_order', 'mem_purchase_item', 'mem_purchase_delivery', 'mem_purchase_payment']) {
  if (!ddl.includes(`CREATE TABLE IF NOT EXISTS \`${table}\``)) {
    console.error(`DDL table is missing: ${table}`)
    process.exit(1)
  }
}
console.log(`Member purchase release check passed: ${required.length} files and required tables verified.`)
