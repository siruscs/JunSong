import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, it } from 'node:test'

const root = new URL('..', import.meta.url).pathname
const source = readFileSync(
  join(root, 'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java'),
  'utf8'
)

describe('system user mini-program management contract', () => {
  it('checks data scope for every submitted user department', () => {
    assert.match(source, /private\s+void\s+checkDeptDataScope\(Long\s+deptId,\s+Long\[\]\s+deptIds\)/)
    assert.match(source, /for\s*\(Long\s+item\s*:\s*deptIds\)/)
    assert.match(source, /deptService\.checkDeptDataScope\(item\)/)
    assert.match(source, /checkDeptDataScope\(user\.getDeptId\(\),\s*user\.getDeptIds\(\)\)/)
  })

  it('checks user data scope before exposing WeChat binding state', () => {
    const method = source.slice(source.indexOf('@RequiresPermissions("system:user:list")'))
    assert.match(method, /@RequiresPermissions\("system:user:list"\)/)
    assert.match(method, /userService\.checkUserDataScope\(userId\)/)
    assert.match(method, /userMpBindingService\.selectByUserId\(user\.getTenantId\(\),\s*userId\)/)
  })

  it('returns all non-left user departments for detail and edit echo', () => {
    const method = source.slice(source.indexOf('public AjaxResult getInfo(@PathVariable'))
    assert.match(method, /selectUserDeptByUserId\(userId\)/)
    assert.match(method, /!"1"\.equals\(userDept\.getStatus\(\)\)/)
    assert.doesNotMatch(method, /userDeptQuery\.setStatus\("0"\)/)
  })
})
