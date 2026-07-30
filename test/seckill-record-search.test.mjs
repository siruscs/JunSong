import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const page = readFileSync(new URL('../src/pages/list/index.vue', import.meta.url), 'utf8')
const modules = readFileSync(new URL('../src/config/modules.js', import.meta.url), 'utf8')
const claimMapper = readFileSync(new URL('../../junsong-modules/junsong-member/src/main/resources/mapper/member/MemSeckillClaimRecordMapper.xml', import.meta.url), 'utf8')
const recordMapper = readFileSync(new URL('../../junsong-modules/junsong-member/src/main/resources/mapper/member/MemSeckillRecordMapper.xml', import.meta.url), 'utf8')

test('routes seckill record number searches to memberNo', () => {
  const seckillRecord = modules.match(/seckillRecord:\s*\{([\s\S]*?)\n\s*\},\n\n\s*\/\/ ===== 财务管理 =====/)?.[1]
  assert.ok(seckillRecord, 'seckillRecord module config should exist')
  assert.match(seckillRecord, /searchKeys:\s*\['memberName',\s*'memberNo'\]/)
})

test('forwards the member number search to claim history', () => {
  const loader = page.match(/async loadClaimRows\(\)\s*\{([\s\S]*?)\n\s*\},\n\s*addItem\(\)/)?.[1]
  assert.ok(loader, 'loadClaimRows should remain present')
  assert.match(loader, /memberNo/)
  assert.match(loader, /memberName/)
  assert.match(loader, /queryValue/)
})

test('claim history supports fuzzy member name search', () => {
  assert.match(claimMapper, /c\.member_name like concat\('%', #\{memberName\}, '%'\)/)
})

test('seckill records support fuzzy member name search', () => {
  assert.match(recordMapper, /r\.member_name like concat\('%', #\{memberName\}, '%'\)/)
})
