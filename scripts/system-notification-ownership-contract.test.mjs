import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const root = new URL('../', import.meta.url)
const controller = fs.readFileSync(new URL('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysNotificationController.java', root), 'utf8')
const mapperXml = fs.readFileSync(new URL('junsong-modules/junsong-system/src/main/resources/mapper/system/SysNotificationMapper.xml', root), 'utf8')

test('notification read endpoint always uses the authenticated user', () => {
  const markRead = controller.match(/public AjaxResult markRead\(@PathVariable Long id\)\s*\{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(markRead, /markAsRead\(id, SecurityUtils\.getUserId\(\)\)/)
})

test('notification read update is constrained by notification and user ids', () => {
  const update = mapperXml.match(/<update id="markAsRead">([\s\S]*?)<\/update>/)?.[1] || ''
  assert.match(update, /where\s+id\s*=\s*#\{id\}\s+and\s+user_id\s*=\s*#\{userId\}/i)
})
