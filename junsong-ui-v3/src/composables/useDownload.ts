import request from '@/api/request'
import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import { blobValidate } from '@/utils/junsong'
import { saveAs } from 'file-saver'

const baseURL = import.meta.env.VITE_APP_BASE_API
let downloadLoadingInstance: ReturnType<typeof ElLoading.service>

export function useDownload() {
  function download(url: string, params: any, name: string) {
    downloadLoadingInstance = ElLoading.service({
      text: '正在下载数据，请稍候',
      background: 'rgba(0, 0, 0, 0.7)',
    })
    return request
      .post(url, params, {
        transformRequest: [(params: any) => {
          let result = ''
          Object.keys(params).forEach((key) => {
            if (!Object.is(params[key], undefined) && !Object.is(params[key], null)) {
              result += encodeURIComponent(key) + '=' + encodeURIComponent(params[key]) + '&'
            }
          })
          return result
        }],
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        responseType: 'blob',
      } as any)
      .then(async (data: any) => {
        const isBlob = blobValidate(data)
        if (isBlob) {
          const blob = new Blob([data])
          saveAs(blob, name)
        } else {
          const resText = await (data as Blob).text()
          const rspObj = JSON.parse(resText)
          const errMsg = rspObj.msg || '下载文件出现错误，请联系管理员！'
          ElMessage.error(errMsg)
        }
        downloadLoadingInstance.close()
      })
      .catch(() => {
        ElMessage.error('下载文件出现错误，请联系管理员！')
        downloadLoadingInstance.close()
      })
  }

  function downloadZip(url: string, name: string) {
    const fullUrl = baseURL + url
    downloadLoadingInstance = ElLoading.service({
      text: '正在下载数据，请稍候',
      background: 'rgba(0, 0, 0, 0.7)',
    })
    axios({
      method: 'get',
      url: fullUrl,
      responseType: 'blob',
      headers: { Authorization: 'Bearer ' + getToken() },
    })
      .then((res) => {
        const isBlob = blobValidate(res.data)
        if (isBlob) {
          const blob = new Blob([res.data])
          saveAs(blob, name)
        } else {
          ElMessage.error('下载文件出现错误！')
        }
        downloadLoadingInstance.close()
      })
      .catch(() => {
        ElMessage.error('下载文件出现错误，请联系管理员！')
        downloadLoadingInstance.close()
      })
  }

  function resource(resourceUrl: string) {
    const url = baseURL + resourceUrl
    window.open(url, '_blank')
  }

  return { download, downloadZip, resource }
}
