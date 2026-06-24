import request from '../request'

export function listUserDept(query: any) {
  return request({ url: '/system/userDept/list', method: 'get', params: query })
}

export function getDeptsByUserId(userId: number) {
  return request({ url: '/system/userDept/user/' + userId, method: 'get' })
}

export function getUserDept(userDeptId: number) {
  return request({ url: '/system/userDept/' + userDeptId, method: 'get' })
}

export function addUserDept(data: any) {
  return request({ url: '/system/userDept', method: 'post', data })
}

export function updateUserDept(data: any) {
  return request({ url: '/system/userDept', method: 'put', data })
}

export function delUserDept(userDeptIds: number | string) {
  return request({ url: '/system/userDept/' + userDeptIds, method: 'delete' })
}

export function hireUserToDept(userId: number, deptId: number) {
  return request({ url: '/system/userDept/hire', method: 'post', data: { userId, deptId } })
}

export function leaveUserFromDept(userId: number, deptId: number) {
  return request({ url: '/system/userDept/leave', method: 'post', data: { userId, deptId } })
}

export function listStaffByDept(deptId: number) {
  return request({ url: '/system/userDept/staff/' + deptId, method: 'get' })
}

export function listUserDeptUsers(deptId: number) {
  return listStaffByDept(deptId)
}
