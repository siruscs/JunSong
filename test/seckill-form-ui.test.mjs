import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const formPage = fs.readFileSync(new URL('../src/pages/form/index.vue', import.meta.url), 'utf8')
const modules = fs.readFileSync(new URL('../src/config/modules.js', import.meta.url), 'utf8')

test('seckill edit fields use the shared full-width input control', () => {
  const seckillConfig = modules.match(/seckill: \{([\s\S]*?)(?=\n  seckillRecord: \{)/)?.[1] || ''
  assert.match(seckillConfig, /title: '秒杀活动'/)
  assert.match(formPage, /\.control\.input\s*\{[\s\S]*?display:\s*block[\s\S]*?width:\s*100%[\s\S]*?height:\s*84rpx/)
  assert.match(formPage, /\.control\.input:focus\s*\{[\s\S]*?border-color:\s*#087CF0/)
})
