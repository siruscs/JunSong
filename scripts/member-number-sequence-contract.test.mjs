import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('member number sequence mapper matches the current department-prefix table schema', () => {
  const source = fs.readFileSync('junsong-modules/junsong-member/src/main/resources/mapper/member/MemMemberMapper.xml', 'utf8')
  const sequenceBlock = source.slice(source.indexOf('<insert id="insertMemberNoSequence">'), source.indexOf('<select id="selectDeptNameById"'))
  assert.match(sequenceBlock, /where dept_id = #\{deptId\} and prefix = #\{prefix\}/)
  assert.doesNotMatch(sequenceBlock, /mem_member_no_sequence\.tenant_id/)
  assert.doesNotMatch(sequenceBlock, /tenant_id = #\{tenantId\}/)
})

test('tenant interceptor leaves the global member number sequence table unscoped', () => {
  const source = fs.readFileSync('junsong-common/junsong-common-core/src/main/java/com/junsong/common/core/interceptor/TenantSqlInterceptor.java', 'utf8')
  assert.match(source, /mem_\(\?!refund_apply\$\|member_no_sequence\$\)/)
})

test('member number sequence is allocated independently by department', () => {
  const service = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemMemberServiceImpl.java', 'utf8')
  const mapper = fs.readFileSync('junsong-modules/junsong-member/src/main/resources/mapper/member/MemMemberMapper.xml', 'utf8')
  const generationBlock = service.slice(service.indexOf('public String generateMemberNo'), service.indexOf('    /**', service.indexOf('public String generateMemberNo') + 10))
  const sequenceBlock = mapper.slice(mapper.indexOf('<insert id="insertMemberNoSequence">'), mapper.indexOf('<select id="selectMemberNoSequenceForUpdate"'))
  assert.doesNotMatch(generationBlock, /Long sequenceDept = 0L/)
  assert.match(generationBlock, /insertMemberNoSequence\(deptId, prefix\)/)
  assert.match(generationBlock, /selectMemberNoSequenceForUpdate\(deptId, prefix\)/)
  assert.match(generationBlock, /incrementMemberNoSequence\(deptId, prefix\)/)
  assert.match(generationBlock, /String\.format\("%05d", sequence\)/)
  assert.match(sequenceBlock, /and dept_id = #\{deptId\}/)
})

test('member number normalization preserves the two-letter prefix plus five digits rule', () => {
  const service = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemMemberServiceImpl.java', 'utf8')
  const normalizationBlock = service.slice(service.indexOf('private String normalizeImportedMemberNo'), service.indexOf('    /**', service.indexOf('private String normalizeImportedMemberNo') + 10))
  assert.match(normalizationBlock, /String\.format\("%05d", digits\)/)
})

test('member creation does not reserve numbers before save', () => {
  const service = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemMemberServiceImpl.java', 'utf8')
  const controller = fs.readFileSync('junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemMemberController.java', 'utf8')
  const page = fs.readFileSync('junsong-ui-v3/src/views/member/index.vue', 'utf8')
  const insertStart = service.lastIndexOf('    @Transactional', service.indexOf('public int insertMemMember'))
  const insertBlock = service.slice(insertStart, service.indexOf('    /**', service.indexOf('public int insertMemMember') + 10))
  const addBlock = controller.slice(controller.indexOf('public AjaxResult add'), controller.indexOf('    /**', controller.indexOf('public AjaxResult add') + 10))
  assert.match(insertBlock, /@Transactional/)
  assert.match(insertBlock, /generateMemberNo\(memMember\.getDeptId\(\)\)/)
  assert.doesNotMatch(addBlock, /generateMemberNo\(/)
  assert.doesNotMatch(page, /fetchNextMemberNo\(\)/)
  assert.doesNotMatch(page, /getNextMemberNo\(/)
})
