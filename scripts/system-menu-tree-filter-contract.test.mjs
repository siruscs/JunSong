import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const menuPage = new URL('../junsong-ui-v3/src/views/system/menu/index.vue', import.meta.url)

test('system menu tree search includes permission identifiers', async () => {
  const source = await readFile(menuPage, 'utf8')

  assert.match(source, /placeholder="输入菜单名称或权限标识过滤"/)
  assert.match(source, /data\.perms/)
})
