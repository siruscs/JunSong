import request from '../request'

export function listConfig(query: any) {
  return request({ url: '/system/config/list', method: 'get', params: query })
}

export function getConfig(configId: number) {
  return request({ url: '/system/config/' + configId, method: 'get' })
}

export function addConfig(data: any) {
  return request({ url: '/system/config', method: 'post', data })
}

export function updateConfig(data: any) {
  return request({ url: '/system/config', method: 'put', data })
}

export function delConfig(configId: number) {
  return request({ url: '/system/config/' + configId, method: 'delete' })
}

export function refreshConfigCache() {
  return request({ url: '/system/config/refreshCache', method: 'delete' })
}

export function getConfigKey(configKey: string) {
  return request({ url: '/system/config/configKey/' + configKey, method: 'get' })
}
