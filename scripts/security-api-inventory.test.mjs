import test from 'node:test'
import assert from 'node:assert/strict'
import { inventoryApis, parseControllerSource } from './security-api-inventory.mjs'

test('API parser expands array routes and recognizes PreAuthorize', () => {
  const rows = parseControllerSource(`
    @RequestMapping({"/mp", "/member/mp"})
    public class FixtureController {
      @PreAuthorize("@ss.hasPermi('member:list')")
      @GetMapping(value = {"/", "/{userId}"})
      public Object list() { return null; }
    }
  `)
  assert.deepEqual(rows.map((row) => row.path).sort(), ['/member/mp', '/member/mp/{userId}', '/mp', '/mp/{userId}'])
  assert.ok(rows.every((row) => row.permission === 'member:list'))
})

test('API parser keeps pagination detection inside the current method body', () => {
  const rows = parseControllerSource(`
    @RequestMapping("/items")
    public class FixtureController {
      @PostMapping
      public Object add() { return save(); }

      @GetMapping
      public Object list() { startPage(); return getDataTable(load()); }
    }
  `)
  assert.equal(rows.find((row) => row.method === 'POST').isPaged, false)
  assert.equal(rows.find((row) => row.method === 'GET').isPaged, true)
})

test('API inventory finds controllers and classifies authorization and exports', () => {
  const rows = inventoryApis(process.cwd())
  assert.ok(rows.length > 100)
  assert.ok(rows.some((row) => row.permission && row.permission.includes(':')))
  assert.ok(rows.some((row) => row.isExport))
  assert.ok(rows.some((row) => row.isPaged))
  assert.ok(rows.every((row) => row.file && row.method && row.path !== undefined))
})

test('API inventory exposes review candidates without treating public endpoints as safe', () => {
  const rows = inventoryApis(process.cwd())
  const candidates = rows.filter((row) => !row.permission && !row.internalOnly)
  assert.ok(candidates.length > 0)
  assert.ok(candidates.every((row) => row.reviewStatus === 'REVIEW'))
})
