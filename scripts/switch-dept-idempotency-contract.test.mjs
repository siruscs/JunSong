import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const source = fs.readFileSync(
  new URL('../junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java', import.meta.url),
  'utf8',
)

test('department switching is not blocked by a missing business idempotency key', () => {
  const method = source.match(/@Idempotent\(([^)]*)\)\s*@PostMapping\("switchDept\/\{deptId\}"\)/s)
  assert.ok(method, 'switchDept must remain explicitly annotated')
  assert.match(method[1], /required\s*=\s*false/)
})
