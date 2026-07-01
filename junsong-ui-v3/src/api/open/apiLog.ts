import request from '@/api/request'

/** 查询 API 调用日志列表 */
export function listApiLog(params: Record<string, any>) {
  return request({ url: '/open/apiLog/list', method: 'get', params }) as Promise<any>
}

/** 导出 API 调用日志 */
export function exportApiLog(params: Record<string, any>) {
  return request({
    url: '/open/apiLog/export',
    method: 'post',
    params,
    responseType: 'blob',
  }) as Promise<any>
}
