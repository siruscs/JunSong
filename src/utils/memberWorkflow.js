export function resolveMemberSearchField(value) {
  const keyword = String(value || '').trim()
  if (/^\d+$/.test(keyword)) return 'phone'
  if (/^[A-Za-z0-9]+$/.test(keyword)) return 'memberNo'
  return 'memberName'
}

export function validateMemberContact(form = {}) {
  const phone = String(form.phone || '').trim()
  const idCard = String(form.idCard || '').trim()
  if (phone && !/^1\d{10}$/.test(phone)) return '请输入正确的11位手机号码'
  if (idCard && !/^(\d{15}|\d{17}[\dXx])$/.test(idCard)) return '请输入正确的身份证号'
  return ''
}
