import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const files = [
  '../junsong-ui-v3/src/views/workflow/instance/index.vue',
  '../junsong-ui-v3/src/views/workflow/history/index.vue',
  '../junsong-ui-v3/src/views/workflow/task/index.vue',
]

test('流程详情相关页面必须先解析 bizCode 再加载 DETAIL Schema', () => {
  for (const file of files) {
    const source = fs.readFileSync(new URL(file, import.meta.url), 'utf8')
    assert.match(source, /resolveWorkflowBizCode/)
  }
})
