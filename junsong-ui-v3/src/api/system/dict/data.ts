import request from '../../request'

export function getDictDataByType(dictType: string) {
  return request({
    url: '/system/dict/data/type/' + dictType,
    method: 'get',
  })
}

export function listData(query: any) {
  return request({
    url: '/system/dict/data/list',
    method: 'get',
    params: query,
  })
}

export function getData(dictCode: number) {
  return request({ url: '/system/dict/data/' + dictCode, method: 'get' })
}

export function addData(data: any) {
  return request({ url: '/system/dict/data', method: 'post', data })
}

export function updateData(data: any) {
  return request({ url: '/system/dict/data', method: 'put', data })
}

export function delData(dictCode: number) {
  return request({ url: '/system/dict/data/' + dictCode, method: 'delete' })
}
