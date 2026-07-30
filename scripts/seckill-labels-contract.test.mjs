import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('seckill create form uses business-friendly price labels', () => {
  const source = fs.readFileSync('junsong-ui-v3/src/views/member/seckill/index.vue', 'utf8')
  assert.match(source, /<el-form-item label="原价" prop="seckillAmount">/)
  assert.match(source, /<el-form-item label="秒杀价" prop="seckillPrice">/)
  assert.match(source, /message: "原价不能为空"/)
  assert.match(source, /message: "秒杀价不能为空"/)
})
