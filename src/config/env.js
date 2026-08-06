const PROD_BASE_URL = 'https://www.junsong.vip/prod-api'
const DEV_BASE_URL = 'http://127.0.0.1:8081/prod-api'
const STORAGE_KEY = 'baseUrlSource'

export const ENV_CONFIGS = [
  { key: 'prod', label: '生产', baseUrl: PROD_BASE_URL },
  { key: 'dev', label: '本地开发', baseUrl: DEV_BASE_URL }
]

export function getConfiguredBaseUrl() {
  const explicit = uni.getStorageSync(STORAGE_KEY)
  if (explicit) {
    const found = ENV_CONFIGS.find(c => c.key === explicit)
    if (found) return found.baseUrl
  }
  return PROD_BASE_URL
}

export function setEnvironment(key) {
  const cfg = ENV_CONFIGS.find(c => c.key === key)
  if (!cfg) return null
  uni.setStorageSync(STORAGE_KEY, key)
  uni.setStorageSync('baseUrl', cfg.baseUrl)
  return cfg
}

export function detectEnvironment() {
  return new Promise((resolve) => {
    const cfg = getConfiguredBaseUrl()
    if (cfg !== PROD_BASE_URL) {
      resolve(cfg)
      return
    }
    uni.request({
      url: PROD_BASE_URL + '/actuator/health',
      method: 'GET',
      timeout: 4000,
      success: () => {
        uni.setStorageSync('baseUrl', PROD_BASE_URL)
        resolve(PROD_BASE_URL)
      },
      fail: () => {
        uni.request({
          url: DEV_BASE_URL + '/actuator/health',
          method: 'GET',
          timeout: 2000,
          success: () => {
            uni.setStorageSync('baseUrl', DEV_BASE_URL)
            resolve(DEV_BASE_URL)
          },
          fail: () => resolve(PROD_BASE_URL)
        })
      }
    })
  })
}

export function getBaseUrlList() {
  return ENV_CONFIGS.map(c => ({ ...c, isActive: c.baseUrl === uni.getStorageSync('baseUrl') || c.baseUrl === getConfiguredBaseUrl() }))
}
