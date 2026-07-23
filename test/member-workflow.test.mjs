import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveMemberSearchField, validateMemberContact } from '../src/utils/memberWorkflow.js'

test('routes member searches without treating a mobile number as a member number', () => {
  assert.equal(resolveMemberSearchField(' 张三 '), 'memberName')
  assert.equal(resolveMemberSearchField(' JS00001 '), 'memberNo')
  assert.equal(resolveMemberSearchField(' 13812345678 '), 'phone')
  assert.equal(resolveMemberSearchField('1234'), 'phone')
  assert.equal(resolveMemberSearchField('00001234'), 'phone')
})

test('accepts optional or correctly formatted member contacts', () => {
  assert.equal(validateMemberContact({ phone: '', idCard: '' }), '')
  assert.equal(validateMemberContact({ phone: '13812345678', idCard: '11010119900101123X' }), '')
  assert.equal(validateMemberContact({ phone: '', idCard: '110101900101123' }), '')
})

test('returns friendly validation messages for invalid member contacts', () => {
  assert.equal(validateMemberContact({ phone: '12345' }), '请输入正确的11位手机号码')
  assert.equal(validateMemberContact({ idCard: '11010119900101123A' }), '请输入正确的身份证号')
})
