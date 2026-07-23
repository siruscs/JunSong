import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const userPage = () => read('junsong-ui-v3/src/views/system/user/index.vue')

test('user/index.vue: avatar column renders with a fixed-size class', () => {
  const src = userPage()
  assert.match(src, /class="user-list-avatar"/)
  assert.match(src, /\.user-list-avatar\s*\{/)
  assert.match(src, /width:\s*32px/)
  assert.match(src, /height:\s*32px/)
})

test('user/index.vue: edit form exposes avatar field and binds it to form.avatar', () => {
  const src = userPage()
  assert.match(src, /label="用户头像"[\s\S]*v-model="form\.avatar"/)
  assert.match(src, /avatar:\s*''/)
})
