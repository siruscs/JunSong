import { readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

const src = readFileSync('scripts/permission-menu-gap-health.mjs', 'utf8')

test('R12-FIX: mysql password must not have a hardcoded default', () => {
  assert.doesNotMatch(src, /MYSQL_ROOT_PASSWORD\s*=\s*process\.env\.MYSQL_ROOT_PASSWORD\s*\|\|\s*['"]root_123['"]/)
  assert.match(src, /MYSQL_ROOT_PASSWORD/)
  assert.match(src, /process\.exit\(2\)/)
})

test('R12-FIX: prod-only exemptions are not global exemptions', () => {
  assert.match(src, /PROD_EXEMPT_PERMS/)
  assert.match(src, /activeExemptPerms\s*=\s*ENV\s*===\s*['"]prod['"]/)
})
