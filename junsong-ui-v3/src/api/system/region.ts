import request from '../request'

export function getRegionTree() {
  return request({
    url: '/system/region/tree',
    method: 'get',
  })
}

export function listRegionChildren(parentCode: string) {
  return request({
    url: `/system/region/children/${parentCode}`,
    method: 'get',
  })
}

export function getRegion(code: string) {
  return request({
    url: `/system/region/${code}`,
    method: 'get',
  })
}

export function addRegion(data: any) {
  return request({
    url: '/system/region',
    method: 'post',
    data,
  })
}

export function updateRegion(data: any) {
  return request({
    url: '/system/region',
    method: 'put',
    data,
  })
}

export function delRegion(code: string) {
  return request({
    url: `/system/region/${code}`,
    method: 'delete',
  })
}
