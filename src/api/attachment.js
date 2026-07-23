/**
 * 费用凭证上传 API。
 *
 * 安全边界：
 * 1. 上传只负责文件传输，不代表费用已提交
 * 2. 上传失败不产生已提交费用状态
 * 3. 上传重试不重复创建凭证（基于文件内容哈希或时间戳去重）
 * 4. 不得把身份证、Token、密码或内部错误写入日志
 * 5. 凭证 URL 由后端返回，前端不自行拼接
 *
 * 后端接口：
 * - 文件上传：POST /upload（SysFileController）
 * - OCR 识别：POST /expense/ocr（ExpenseOcrController）
 *
 * @module api/attachment
 */
import { getBaseUrl, getToken } from './index.js'

/**
 * 上传费用凭证文件。
 * 使用 uni.uploadFile 而非 request，因为后端接收 MultipartFile。
 *
 * @param {string} filePath 本地文件路径（由 uni.chooseImage 返回）
 * @param {string} [bizType] 业务类型（如 'expense'）
 * @param {string} [bizNo] 业务单号（用于幂等关联）
 * @returns {Promise<{url: string, name: string}>} 上传后的文件 URL 和名称
 */
export function uploadAttachment(filePath, bizType = '', bizNo = '') {
  return new Promise((resolve, reject) => {
    if (!filePath) {
      reject(new Error('文件路径不能为空'))
      return
    }
    const baseUrl = getBaseUrl()
    const token = getToken()
    const formData = {}
    if (bizType) formData.bizType = bizType
    if (bizNo) formData.bizNo = bizNo

    uni.uploadFile({
      url: baseUrl + '/upload',
      filePath,
      name: 'file',
      header: token ? { Authorization: 'Bearer ' + token } : {},
      formData,
      success: (res) => {
        try {
          const data = JSON.parse(res.data || '{}')
          if (res.statusCode >= 200 && res.statusCode < 300 && data.code === 200) {
            const file = data.data || {}
            resolve({
              url: file.url || '',
              name: file.name || ''
            })
          } else if (data.code === 401) {
            reject({ code: 'AUTH_EXPIRED', msg: '登录已超时，请重新登录' })
          } else if (data.code === 403) {
            reject({ code: 'PERMISSION_DENIED', msg: '暂无上传权限' })
          } else {
            reject({ code: 'UPLOAD_FAILED', msg: data.msg || '上传失败' })
          }
        } catch (e) {
          reject({ code: 'PARSE_ERROR', msg: '响应解析失败' })
        }
      },
      fail: (err) => {
        const errMsg = String(err?.errMsg || '')
        if (errMsg.includes('timeout')) {
          reject({ code: 'REQUEST_TIMEOUT', msg: '上传超时，请稍后重试' })
        } else if (errMsg.includes('network')) {
          reject({ code: 'NETWORK_ERROR', msg: '网络连接失败' })
        } else {
          reject({ code: 'UPLOAD_FAILED', msg: '上传失败' })
        }
      }
    })
  })
}

/**
 * OCR 识别费用凭证。
 * @param {string} filePath 本地文件路径
 * @returns {Promise<Object>} OCR 识别结果
 */
export function recognizeExpense(filePath) {
  return new Promise((resolve, reject) => {
    if (!filePath) {
      reject(new Error('文件路径不能为空'))
      return
    }
    const baseUrl = getBaseUrl()
    const token = getToken()

    uni.uploadFile({
      url: baseUrl + '/expense/ocr',
      filePath,
      name: 'file',
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success: (res) => {
        try {
          const data = JSON.parse(res.data || '{}')
          if (res.statusCode >= 200 && res.statusCode < 300 && data.code === 200) {
            resolve(data.data || {})
          } else if (data.code === 401) {
            reject({ code: 'AUTH_EXPIRED', msg: '登录已超时，请重新登录' })
          } else if (data.code === 403) {
            reject({ code: 'PERMISSION_DENIED', msg: '暂无OCR识别权限' })
          } else {
            reject({ code: 'OCR_FAILED', msg: data.msg || 'OCR识别失败' })
          }
        } catch (e) {
          reject({ code: 'PARSE_ERROR', msg: '响应解析失败' })
        }
      },
      fail: (err) => {
        reject({ code: 'OCR_FAILED', msg: 'OCR识别请求失败' })
      }
    })
  })
}
