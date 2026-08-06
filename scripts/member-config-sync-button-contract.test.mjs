import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const cases = [
  ['junsong-ui-v3/src/views/finance/product/index.vue', 'finance:product:sync'],
  ['junsong-ui-v3/src/views/finance/supplier/index.vue', 'finance:supplier:sync'],
  ['junsong-ui-v3/src/views/member/level/index.vue', 'member:level:sync'],
  ['junsong-ui-v3/src/views/member/campaignPolicy/index.vue', 'member:campaignPolicy:sync']
]

for (const [file, permission] of cases) {
  test(`${file} exposes a permission-gated sync entry`, () => {
    const source = fs.readFileSync(file, 'utf8')
    assert.match(source, new RegExp(`v-hasPermi=\\"\\['${permission}'\\]\\"`))
    assert.match(source, /同步(?:全部等级)?到其他机构/)
  })
}
