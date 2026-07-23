import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const pages = [
  'junsong-ui-v3/src/views/finance/investorPayment/index.vue',
  'junsong-ui-v3/src/views/finance/investRecord/index.vue',
  'junsong-ui-v3/src/views/finance/compositeAccounting/index.vue',
]

test('security: investor selectors use bounded remote search', () => {
  for (const path of pages) {
    const source = read(path)
    assert.doesNotMatch(source, /listInvestor\s*\(\s*\{[^}]*pageSize\s*:\s*(?:1000|9999)/s, path)
    assert.match(source, /remote-method=/, path)
    assert.match(source, /listInvestor\s*\(\s*\{[^}]*investorName[^}]*pageSize\s*:\s*20/s, path)
  }
})

test('security: investor searches ignore stale responses and preserve selected labels', () => {
  for (const path of pages) {
    const source = read(path)
    assert.match(source, /investorSearchRequestId/, path)
    assert.match(source, /requestId\s*!==\s*this\.investorSearchRequestId/, path)
    assert.match(source, /selected/, path)
  }
})

test('security: department-scoped investor forms send the selected department', () => {
  for (const path of pages.slice(0, 2)) {
    const source = read(path)
    assert.match(source, /deptId:\s*this\.form\.deptId\s*\|\|\s*undefined/, path)
  }
})

test('security: investor list applies authoritative department scope before pagination', () => {
  const service = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinInvestorServiceImpl.java')
  const mapper = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinInvestorMapper.xml')
  const composite = read('junsong-ui-v3/src/views/finance/compositeAccounting/index.vue')

  assert.match(service, /@DataScope\(deptAlias\s*=\s*"i"[^)]*permission\s*=\s*"finance:investor:list"/)
  assert.match(mapper, /\$\{params\.dataScope\}/)
  assert.doesNotMatch(composite, /response\.rows[\s\S]{0,300}filter\(inv\s*=>\s*userDeptIds/)
})

test('security: investor forms do not overwrite the selected department on submit', () => {
  for (const path of pages.slice(0, 2)) {
    const source = read(path)
    const resetBody = source.match(/reset\(\)\s*\{([\s\S]*?)\n\s*\},\n\s*handleQuery/)?.[1] || ''
    assert.doesNotMatch(source, /this\.form\.deptId\s*=\s*userStore\.currentDeptId/, path)
    assert.match(resetBody, /deptId:\s*userStore\.currentDeptId/, path)
  }
})
