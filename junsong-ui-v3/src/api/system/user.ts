import request from '../request'

export function switchDept(deptId: number) {
  return request({
    url: '/system/user/switchDept/' + deptId,
    method: 'post',
  })
}

export function getUserDeptByUserId(userId: number) {
  return request({
    url: '/system/userDept/user/' + userId,
    method: 'get',
  })
}

export function listUser(query: any) {
  return request({ url: '/system/user/list', method: 'get', params: query })
}

export function getUser(userId: number) {
  return request({ url: '/system/user/' + userId, method: 'get' })
}

export function addUser(data: any) {
  return request({ url: '/system/user', method: 'post', data })
}

export function updateUser(data: any) {
  return request({ url: '/system/user', method: 'put', data })
}

export function delUser(userId: number | string) {
  return request({ url: '/system/user/' + userId, method: 'delete' })
}

export function resetUserPwd(userId: number, password: string) {
  const data = { userId, password }
  return request({ url: '/system/user/resetPwd', method: 'put', data })
}

export function changeUserStatus(userId: number, status: string) {
  const data = { userId, status }
  return request({ url: '/system/user/changeStatus', method: 'put', data })
}

export function getAuthRole(userId: number) {
  return request({ url: '/system/user/authRole/' + userId, method: 'get' })
}

export function updateAuthRole(data: any) {
  return request({ url: '/system/user/authRole', method: 'put', params: data })
}

export function getUserProfile() {
  return request({ url: '/system/user/profile', method: 'get' })
}

export function updateUserProfile(data: any) {
  return request({ url: '/system/user/profile', method: 'put', data })
}

export function updateUserPwd(oldPassword: string, newPassword: string) {
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'put',
    data: { oldPassword, newPassword }
  })
}

export function userAvatar(data: any) {
  return request({
    url: '/system/user/profile/avatar',
    method: 'post',
    headers: {
      repeatSubmit: false,
    },
    data,
  })
}

export function deptTreeSelect() {
  return request({ url: '/system/user/deptTree', method: 'get' })
}
