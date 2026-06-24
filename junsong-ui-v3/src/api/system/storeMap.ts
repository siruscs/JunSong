import request from '../request'

/** 门店地图查询：返回所有门店和顶级部门坐标 */
export function getStoreMap() {
  return request({ url: '/system/dept/map/stores', method: 'get' })
}

/** 门店密度查询：按省市区街道过滤查询门店坐标 */
export function getStoreDensity(params: any) {
  return request({ url: '/system/dept/map/density', method: 'get', params })
}
