import request from './request'

export function getRouters() {
  return request({
    url: '/system/menu/getRouters',
    method: 'get',
  })
}
