import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const modules = fs.readFileSync(new URL('../src/config/modules.js', import.meta.url), 'utf8')

test('mini-program seckill activity clearly labels discount amount and deal price', () => {
  const seckill = modules.match(/seckill: \{([\s\S]*?)(?=\n  seckillRecord: \{)/)?.[1] || ''
  assert.match(seckill, /key: 'seckillAmount', label: '原价（每份）'/)
  assert.match(seckill, /key: 'seckillPrice', label: '秒杀价（每份）'/)
  assert.doesNotMatch(seckill, /label: '秒杀金额'|label: '秒杀单价'|label: '每份优惠金额'|label: '每份成交价'/)
})
