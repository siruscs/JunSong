import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const root = new URL('../', import.meta.url)
const xml = readFileSync(new URL('junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinStockLedgerMapper.xml', root), 'utf8')
const service = readFileSync(new URL('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StockSnapshotServiceImpl.java', root), 'utf8')
const task = readFileSync(new URL('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/task/StockDailySnapshotTask.java', root), 'utf8')

test('snapshot queries explicitly scope tenant and use index-friendly day bounds', () => {
  for (const id of ['sumDailyFlow', 'selectSnapshotProductIds', 'selectPreviousSnapshot', 'selectFirstDailyLedger', 'selectLastLedgerBeforeDate']) {
    const block = xml.match(new RegExp(`<select id="${id}"[\\s\\S]*?<\\/select>`))?.[0]
    assert.ok(block, `missing ${id}`)
    assert.match(block, /tenant_id\s*=\s*#\{tenantId\}/i)
  }
  assert.doesNotMatch(xml.match(/<select id="sumDailyFlow"[\s\S]*?<\/select>/)?.[0] ?? '', /date\s*\(\s*create_time/i)
  assert.match(xml, /create_time\s*&gt;=\s*#\{snapshotDate\}/i)
  assert.match(xml, /create_time\s*&lt;\s*date_add\(#\{snapshotDate\},\s*interval 1 day\)/i)
})

test('snapshot upsert and scheduler preserve tenant scope and deterministic order', () => {
  const upsert = xml.match(/<insert id="upsertSnapshot"[\s\S]*?<\/insert>/)?.[0] ?? ''
  assert.match(upsert, /tenant_id/)
  assert.match(upsert, /#\{tenantId\}/)
  assert.match(xml, /order by tenant_id, dept_id/i)
  assert.match(task, /rebuildDailySnapshot\(scope\.getTenantId\(\), today, scope\.getDeptId\(\)\)/)
})

test('historical closing is replayed from previous snapshot and classified ledger flows', () => {
  assert.match(service, /selectPreviousSnapshot/)
  assert.match(service, /selectFirstDailyLedger/)
  assert.match(service, /selectLastLedgerBeforeDate/)
  assert.match(service, /opening, inQuantity/)
  assert.doesNotMatch(service, /selectPositionQuantityForUpdate|selectPositionsByDept/)
  assert.match(xml, /'PURCHASE_IN', 'SALE_REVERSE'/)
  assert.match(xml, /'SALE_OUT', 'PURCHASE_REVERSE'/)
})
