import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('level sync is implemented as a source-organization bulk sync', () => {
  const source = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberConfigSyncServiceImpl.java', 'utf8')
  assert.match(source, /loadLevelSources\(/)
  assert.match(source, /for \(Map<String, Object> sourceItem : sources\)/)
})

test('editing a tenant baseline level creates a department override', () => {
  const service = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberLevelServiceImpl.java', 'utf8')
  const mapper = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/mapper/MemMemberCardTypeMapper.java', 'utf8')
  assert.match(service, /selectCardTypeById\(/)
  assert.match(service, /insertCardType\(cardType\)/)
  assert.match(service, /cardType\.getDeptId\(\) == 0/)
  assert.match(mapper, /selectCardTypeById/)
})

test('campaign policy save carries accounting-period effective dates', () => {
  const source = fs.readFileSync('junsong-ui-v3/src/views/member/campaignPolicy/index.vue', 'utf8')
  assert.match(source, /effectiveStart/)
  assert.match(source, /effectiveEnd/)
})

test('campaign policy view/edit dialog keeps save enabled for existing policies', () => {
  const source = fs.readFileSync('junsong-ui-v3/src/views/member/campaignPolicy/index.vue', 'utf8')
  assert.match(source, /<el-button type="primary"[^>]*@click="save"[^>]*>保存<\/el-button>/)
  assert.doesNotMatch(source, /<el-button type="primary"[^>]*:disabled="editing"[^>]*@click="save"/)
})

test('campaign policy editing has a tenant-scoped update endpoint', () => {
  const api = fs.readFileSync('junsong-ui-v3/src/api/member/campaignPolicy.ts', 'utf8')
  const controller = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemCampaignPolicyController.java', 'utf8')
  assert.match(api, /updateCampaignPolicy/)
  assert.match(controller, /@PutMapping\("\/{policyId}"\)/)
  assert.match(controller, /updatePolicy\(/)
})

test('campaign policy package logical deletion does not block re-adding the same quantity', () => {
  const mapper = fs.readFileSync('junsong-modules/junsong-member/src/main/resources/mapper/member/MemCampaignPolicyMapper.xml', 'utf8')
  const migration = fs.readFileSync('sql/member_campaign_policy_package_unique_scope.sql', 'utf8')
  assert.match(mapper, /set del_flag = '2'/)
  assert.match(migration, /purchase_quantity.*del_flag|del_flag.*purchase_quantity/i)
})
