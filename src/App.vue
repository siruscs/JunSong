<script>
export default {
  onLaunch() {
    const baseUrl = uni.getStorageSync('baseUrl')
    if (!baseUrl) {
      uni.setStorageSync('baseUrl', 'http://192.168.1.8:8081')
    }
  }
}
</script>

<style>
/* ===== 霁蓝晨雾 Design System ===== */
page {
  background: #F0F4F8;
  color: #1A2332;
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
  box-shadow: 0 2rpx 16rpx rgba(42,111,151,0.06);
}

/* 全局渐变头部 */
.g-header-gradient {
  background: linear-gradient(160deg, #173B57 0%, #2A6F97 40%, #3A8DB8 80%, #8EC8D2 100%);
}

/* 全局按钮 */
.g-btn-primary {
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #2A6F97, #3A8DB8);
  color: #FFFFFF;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 999rpx;
  text-align: center;
  border: none;
  padding: 0;
  box-shadow: 0 8rpx 24rpx rgba(42,111,151,0.25);
  transition: opacity 0.2s;
}

.g-btn-primary::after { border: none; }
.g-btn-primary[disabled] { opacity: 0.5; box-shadow: none; }

/* 全局输入框 */
.g-input {
  height: 96rpx;
  padding: 0 28rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #1A2332;
  transition: border-color 0.2s, background 0.2s;
}

.g-input:focus {
  border-color: #2A6F97;
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
</style>
