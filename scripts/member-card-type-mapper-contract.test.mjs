import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('card type lookup does not declare a scalar parameter type for named tenant and department parameters', () => {
  const mapper = fs.readFileSync('junsong-modules/junsong-member/src/main/resources/mapper/member/MemMemberCardTypeMapper.xml', 'utf8')
  const lookup = mapper.slice(mapper.indexOf('<select id="selectCardTypeByTypeCode"'), mapper.indexOf('</select>', mapper.indexOf('<select id="selectCardTypeByTypeCode"')))
  assert.doesNotMatch(lookup, /parameterType="String"/)
  assert.match(lookup, /#\{tenantId\}/)
  assert.match(lookup, /#\{deptId\}/)
})
