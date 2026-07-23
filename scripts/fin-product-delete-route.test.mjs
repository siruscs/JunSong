import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const source = fs.readFileSync(
  new URL('../junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinProductController.java', import.meta.url),
  'utf8'
)

test('商品删除路由使用非捕获组匹配批量商品编号', () => {
  assert.ok(source.includes('@DeleteMapping("/{productIds:\\\\d+(?:,\\\\d+)*}")'))
  assert.ok(!source.includes('@DeleteMapping("/{productIds:\\\\d+(,\\\\d+)*}")'))
})
