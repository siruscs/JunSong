import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const home = fs.readFileSync(new URL('../src/pages/index/index.vue', import.meta.url), 'utf8')

test('home projects shared work context on show and keeps cache when context is empty', () => {
  assert.match(home, /import \{[^}]*workContext[^}]*\} from '@\/utils\/workContext\.js'/)
  assert.match(home, /import \{ refreshForegroundSession \} from '@\/utils\/foregroundSession\.js'/)
  assert.match(home, /async onShow\(\)/)
  assert.match(home, /await refreshForegroundSession\(\)[\s\S]*?const context = workContext\.snapshot\(\)/)
  assert.match(home, /const context = workContext\.snapshot\(\)/)
  assert.match(home, /if \(context\.user \|\| context\.depts\.length\)/)
  assert.match(home, /this\.applyWorkContext\(context\)/)
  assert.match(home, /const userInfo = uni\.getStorageSync\('userInfo'\) \|\| \{\}/)
})

test('home continues from cached context when foreground refresh fails but token remains', () => {
  const block = home.match(/async onShow\(\) \{([\s\S]*?)(?=\n  methods:)/)?.[1] || ''
  assert.match(block, /try \{[\s\S]*?await refreshForegroundSession\(\)[\s\S]*?\} catch \(_\) \{[\s\S]*?if \(!getToken\(\)\) return[\s\S]*?\}/)
  assert.ok(block.indexOf('await refreshForegroundSession()') < block.indexOf('this.loadDashboard()'))
})

test('successful user context load hydrates the complete department collection', () => {
  assert.match(home, /workContext\.hydrate\(\{[\s\S]*?user,[\s\S]*?depts,[\s\S]*?currentDeptId:/)
  assert.match(home, /this\.applyWorkContext\(workContext\.snapshot\(\)\)/)
})

test('user context load ignores a response captured before department switch', () => {
  const block = home.match(/async loadUserContext\(\) \{([\s\S]*?)(?=\n    async loadAllDepts)/)?.[1] || ''
  assert.match(block, /withContextMeta:\s*true/)

  const staleGuard = block.indexOf('if (res.contextMeta?.staleContext) return')
  assert.ok(staleGuard >= 0, 'loadUserContext should return on stale context')
  for (const mutation of ['this.systemPermissions =', 'workContext.hydrate(', 'this.applyWorkContext(', 'uni.setStorageSync(']) {
    const mutationIndex = block.indexOf(mutation)
    if (mutationIndex >= 0) {
      assert.ok(staleGuard < mutationIndex, `stale guard should precede ${mutation}`)
    }
  }
})

test('both department switch paths select shared context only after server success', () => {
  const selectCalls = home.match(/workContext\.selectDept\(/g) || []
  assert.equal(selectCalls.length, 2)
  assert.match(home, /switchDept\/\$\{target\.id\}`,[\s\S]*?\}\)[\s\S]*?workContext\.selectDept\(target\.id\)/)
  assert.match(home, /switchDept\/\$\{this\.pendingDeptId\}`,[\s\S]*?\}\)[\s\S]*?workContext\.selectDept\(this\.pendingDeptId\)/)
})

test('both department switch paths clear old module access before refreshing current department modules', () => {
  const onDeptChange = home.match(/async onDeptChange\(e\) \{([\s\S]*?)(?=\n    async openDeptPicker)/)?.[1] || ''
  const confirmDeptSwitch = home.match(/async confirmDeptSwitch\(\) \{([\s\S]*?)(?=\n    onRefresh\(\))/)?.[1] || ''

  for (const [name, block, selectCall] of [
    ['onDeptChange', onDeptChange, 'workContext.selectDept(target.id)'],
    ['confirmDeptSwitch', confirmDeptSwitch, 'workContext.selectDept(this.pendingDeptId)']
  ]) {
    const selectIndex = block.indexOf(selectCall)
    const clearIndex = block.indexOf('this.clearModuleAccess()')
    const refreshIndex = block.indexOf('await this.refreshModules()')
    assert.ok(selectIndex >= 0, `${name} should select the new department context`)
    assert.ok(clearIndex >= 0, `${name} should clear old module access after switching department`)
    assert.ok(refreshIndex >= 0, `${name} should refresh modules after switching department`)
    assert.ok(selectIndex < clearIndex, `${name} should clear modules after selecting department context`)
    assert.ok(clearIndex < refreshIndex, `${name} should clear modules before refreshing current access`)
  }
})

test('clearing module access fails closed in memory and storage', () => {
  const block = home.match(/clearModuleAccess\(\) \{([\s\S]*?)(?=\n    async refreshModules)/)?.[1] || ''
  assert.match(block, /this\.modules\s*=\s*\[\]/)
  assert.match(block, /uni\.setStorageSync\('modules',\s*\[\]\)/)
})

test('five home loaders request context metadata and ignore stale responses', () => {
  for (const method of ['loadDashboard', 'loadOverview', 'loadPeriod', 'loadExpenseSummary']) {
    const block = home.match(new RegExp(`async ${method}\\(\\) \\{([\\s\\S]*?)(?=\\n    async |\\n    \\/\\*\\*)`))?.[1] || ''
    assert.match(block, /withContextMeta:\s*true/, `${method} should request context metadata`)
    assert.match(block, /if \(res\.contextMeta\?\.staleContext\) return/, `${method} should ignore stale responses`)
  }

  const seckill = home.match(/async loadSeckill\(\) \{([\s\S]*?)(?=\n    async loadServerStatus)/)?.[1] || ''
  assert.equal((seckill.match(/withContextMeta:\s*true/g) || []).length, 2)
  assert.match(seckill, /if \(res\.contextMeta\?\.staleContext\) return/)
  assert.match(seckill, /if \(statsRes\.contextMeta\?\.staleContext\) return null/)
  assert.match(seckill, /if \(results\.some\(item => item === null\)\) return/)
})

test('department refresh keeps the existing Promise.all including operating task count', () => {
  assert.match(home, /Promise\.all\(\[this\.loadDashboard\(\), this\.loadOverview\(\), this\.loadPeriod\(\), this\.loadSeckill\(\), this\.loadExpenseSummary\(\), this\.loadOperatingTaskCount\(\)\]\)/)
  assert.doesNotMatch(home, /loadAll\(\)/)
})

test('module refresh ignores stale department responses before updating page or storage', () => {
  const block = home.match(/async refreshModules\(\) \{([\s\S]*?)(?=\n    async loadUserContext)/)?.[1] || ''
  assert.match(block, /withContextMeta:\s*true/)
  const staleGuard = block.indexOf('if (res.contextMeta?.staleContext) return')
  assert.ok(staleGuard >= 0, 'refreshModules should ignore stale responses')
  assert.ok(staleGuard < block.indexOf('this.modules ='))
  assert.ok(staleGuard < block.indexOf("uni.setStorageSync('modules'"))
})

test('module refresh failure does not restore old department modules', () => {
  const block = home.match(/async refreshModules\(\) \{([\s\S]*?)(?=\n    async loadUserContext)/)?.[1] || ''
  const catchBlock = block.match(/catch \(e\) \{([\s\S]*?)\n      \}/)?.[1] || ''
  assert.match(catchBlock, /this\.logRequestFailure\('modules refresh failed', e\)/)
  assert.doesNotMatch(catchBlock, /this\.modules\s*=/)
  assert.doesNotMatch(catchBlock, /setStorageSync\('modules'/)
})
