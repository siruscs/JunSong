import request from './request'

export function getUserDepts(username: string) {
  return request({
    url: '/system/user/deptsForLogin',
    headers: { isToken: false },
    method: 'get',
    params: { username },
  })
}

export function login(username: string, password: string, code: string, uuid: string, deptId?: number) {
  return request({
    url: '/auth/login',
    headers: { isToken: false, repeatSubmit: false },
    method: 'post',
    data: { username, password, code, uuid, deptId },
  })
}

export function register(data: any) {
  return request({
    url: '/auth/register',
    headers: { isToken: false },
    method: 'post',
    data,
  })
}

export function unlockScreen(password: string) {
  return request({
    url: '/auth/unlockscreen',
    method: 'post',
    data: { password },
  })
}

export function refreshToken() {
  return request({
    url: '/auth/refresh',
    method: 'post',
  })
}

export function getInfo() {
  return request({
    url: '/system/user/getInfo',
    method: 'get',
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'delete',
  })
}

export function getCodeImg() {
  const themeColor =
    typeof window === 'undefined'
      ? undefined
      : getComputedStyle(document.documentElement).getPropertyValue('--theme-primary').trim()

  return request({
    url: '/code',
    headers: { isToken: false },
    method: 'get',
    params: { color: themeColor },
    timeout: 20000,
  })
}
