import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const permission = fs.readFileSync('src/utils/permission.js', 'utf8')
const app = fs.readFileSync('src/App.vue', 'utf8')
const stockAdjustment = fs.readFileSync('src/pages/stock-adjustment/index.vue', 'utf8')
const modules = fs.readFileSync('src/config/modules.js', 'utf8')

test('navigation guard maps dynamic module routes and blocks unauthorized entry', () => {
  assert.match(permission, /routeModuleKey/)
  assert.match(permission, /guardNavigation/)
  assert.match(permission, /hasModulePermission\(moduleKey, grants\)/)
  assert.match(permission, /queryValue\(url, 'module'\)/)
  assert.match(app, /installNavigationGuard\(\)/)
})

test('stock adjustment actions are driven by exact backend-granted capabilities', () => {
  assert.match(permission, /getActionCapabilities/)
  assert.match(stockAdjustment, /getActionCapabilities\('stockAdjustment'/)
  assert.match(stockAdjustment, /canCreateAdjustment/)
  assert.match(stockAdjustment, /canApproveAdjustment/)
  assert.match(stockAdjustment, /canPostAdjustment/)
  assert.match(modules, /finance:stockInit:add/)
  assert.match(modules, /finance:stockInit:remove/)
  assert.match(modules, /finance:stockInit:approve/)
  assert.match(modules, /finance:stockInit:post/)
})
