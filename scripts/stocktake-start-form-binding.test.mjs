import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const source = fs.readFileSync(new URL('../junsong-ui-v3/src/views/workflow/start/index.vue', import.meta.url), 'utf8')

test('发起页应按流程 processKey 解析业务对象 bizCode 后加载配置', () => {
  assert.match(source, /listBizObject/)
  assert.match(source, /processKey.*bizCode|bizCode.*processKey/)
})
