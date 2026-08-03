<script>
import { refreshForegroundSession } from '@/utils/foregroundSession.js'

export default {
  onLaunch() {
    const baseUrl = uni.getStorageSync('baseUrl')
    if (!baseUrl) {
      uni.setStorageSync('baseUrl', 'https://www.junsong.vip/prod-api')
    }

    const formatErrorMessage = (error) => {
      if (error == null) return ''
      if (typeof error === 'string') {
        return error
      }
      // 依次尝试常见错误消息字段
      const msg = error?.message || error?.errMsg || error?.msg
      if (typeof msg === 'string' && msg) {
        return msg
      }
      // 兜底：JSON 序列化，避免 String(object) 输出 "[object Object]"
      try {
        return JSON.stringify(error)
      } catch (_) {
        return String(error)
      }
    }

    if (typeof uni.onUnhandledRejection === 'function') {
      uni.onUnhandledRejection((res) => {
        console.warn('[unhandledRejection]', formatErrorMessage(res?.reason || res))
      })
    }
    if (typeof uni.onError === 'function') {
      uni.onError((err) => {
        const errStr = formatErrorMessage(err)
        if (errStr.includes('webapi_getwxaasyncsecinfo')) {
          console.warn('[appError] webapi_getwxaasyncsecinfo internal error (can safely ignore):', errStr)
          return
        }
        if (errStr.includes('SystemError') && errStr.includes('access_token missing')) {
          console.warn('[appError] SystemError access_token missing (WeChat internal):', errStr)
          return
        }
        console.warn('[appError]', errStr)
      })
    }
  },
  onShow() {
    refreshForegroundSession().catch(() => {})
  }
}
</script>

<style>
/* ===== 苹果银灰蓝 · 霁蓝晨雾 Design System ===== */
page {
  background: #E8EEF5;
  color: #263548;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", Helvetica, "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  font-size: 28rpx;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
  max-width: 750rpx;
  margin: 0 auto;
}

view, text, input, textarea, button, picker {
  box-sizing: border-box;
}

input { text-transform: none; }

button {
  margin: 0;
  padding: 0;
  border-radius: 0;
  background: transparent;
  line-height: inherit;
  font-size: inherit;
}

button::after { border: 0; }

/* 全局工具类 */
.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.justify-center { justify-content: center; }
.gap-8 { gap: 8rpx; }
.gap-12 { gap: 12rpx; }
.gap-16 { gap: 16rpx; }
.gap-20 { gap: 20rpx; }
.gap-24 { gap: 24rpx; }
.flex-1 { flex: 1; }
.flex-shrink-0 { flex-shrink: 0; }
.flex-wrap { flex-wrap: wrap; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.font-medium { font-weight: 500; }
.font-semibold { font-weight: 600; }
.font-bold { font-weight: 700; }

/* 全局卡片 */
.g-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  border: 1rpx solid #D5E0EC;
  box-shadow: 0 4rpx 18rpx rgba(45,72,98,0.07);
}

/* 全局渐变头部 */
.g-header-gradient {
  background: linear-gradient(160deg, #0B4E91 0%, #087CF0 52%, #5AA9E8 100%);
}

/* 全局按钮 */
.g-btn-primary {
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #087CF0, #1765C4);
  color: #FFFFFF;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 999rpx;
  text-align: center;
  border: none;
  padding: 0;
  box-shadow: 0 8rpx 24rpx rgba(8,124,240,0.23);
  transition: opacity 0.2s;
}

.g-btn-primary::after { border: none; }
.g-btn-primary[disabled] { opacity: 0.5; box-shadow: none; }

/* 全局输入框 */
.g-input {
  height: 96rpx;
  padding: 0 28rpx;
  background: #F7F9FC;
  border: 2rpx solid #D5E0EC;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #263548;
  transition: border-color 0.2s, background 0.2s;
}

.g-input:focus {
  border-color: #087CF0;
  background: #FFFFFF;
}

/* 全局状态标签 */
.g-status-pill {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 500;
}

.g-status-success { background: #D1FAE5; color: #065F46; }
.g-status-warning { background: #FEF3C7; color: #92400E; }
.g-status-danger { background: #FEE2E2; color: #991B1B; }
.g-status-info { background: #E0F2FE; color: #075985; }

.privacy-consent {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin: 18rpx 0;
  color: #64748B;
  font-size: 22rpx;
  line-height: 1.6;
}
.privacy-consent .consent-box {
  width: 30rpx;
  height: 30rpx;
  border: 2rpx solid #CBD5E1;
  border-radius: 6rpx;
  color: #FFFFFF;
  text-align: center;
  line-height: 26rpx;
}
.privacy-consent .consent-box.checked { border-color: #087CF0; background: #087CF0; }
.privacy-consent .link { color: #087CF0; }
</style>
