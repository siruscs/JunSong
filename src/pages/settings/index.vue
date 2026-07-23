<template>
  <view class="page">
    <!-- 头部 -->
    <view class="hero-card">
      <view class="hero-icon">⚙</view>
      <view class="hero-info">
        <view class="hero-title">接口设置</view>
        <view class="hero-meta">配置后端网关地址</view>
      </view>
    </view>

    <!-- 当前地址展示 -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot"></view>
        <text class="section-title">当前地址</text>
      </view>
      <view class="current-url-box">
        <text class="current-url">{{ currentUrl }}</text>
      </view>
    </view>

    <!-- 修改地址 -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot"></view>
        <text class="section-title">修改地址</text>
      </view>
      <view class="form-item">
        <text class="label">后端网关地址</text>
        <input
          class="control input"
          v-model="baseUrl"
          placeholder="例如 https://www.junsong.vip/prod-api"
          placeholder-class="input-placeholder"
        />
      </view>
      <button class="btn-primary" @tap="save">
        <text class="btn-icon">✓</text> 保存配置
      </button>
    </view>

    <!-- 测试连接 -->
    <view class="section-card">
      <view class="section-header">
        <view class="section-dot"></view>
        <text class="section-title">连接测试</text>
      </view>
      <button class="btn-test" @tap="testConnection" :disabled="testing">
        <text v-if="!testing">测试连接</text>
        <text v-else>测试中...</text>
      </button>
      <view class="conn-bar" :class="connStatus" v-if="connStatus !== 'idle'">
        <text class="conn-dot"></text>
        <text class="conn-text">{{ connText }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request, setBaseUrl, getBaseUrl } from '@/api/index.js'

export default {
  data() {
    return {
      baseUrl: '',
      currentUrl: '',
      testing: false,
      connStatus: 'idle',
      connText: ''
    }
  },
  onLoad() {
    this.baseUrl = getBaseUrl()
    this.currentUrl = getBaseUrl()
  },
  methods: {
    save() {
      if (!this.baseUrl.trim()) {
        uni.showToast({ title: '请输入网关地址', icon: 'none' })
        return
      }
      setBaseUrl(this.baseUrl.trim())
      this.currentUrl = getBaseUrl()
      uni.showToast({ title: '已保存' })
    },
    async testConnection() {
      this.testing = true
      this.connStatus = 'testing'
      this.connText = '正在检测连接...'
      try {
        await request({
          url: '/system/user/deptsForLogin',
          method: 'GET',
          data: { username: 'admin' }
        })
        this.connStatus = 'ok'
        this.connText = '连接成功'
      } catch (e) {
        this.connStatus = 'fail'
        this.connText = '连接失败，请检查地址是否正确'
        console.error('连接测试失败:', e)
      } finally {
        this.testing = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 0 0 40rpx;
  background: #E8EEF5;
}

/* 头部 */
.hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 28rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8, #A8C7E5);
  border-radius: 0 0 24rpx 24rpx;
}

.hero-icon {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 18rpx;
  font-size: 36rpx;
  color: #FFFFFF;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-info {
  flex: 1;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.hero-meta {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
}

/* 通用卡片 */
.section-card {
  margin: 20rpx 28rpx 0;
  padding: 28rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 20rpx;
}

.section-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #087CF0;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
}

/* 当前地址展示 */
.current-url-box {
  padding: 18rpx 24rpx;
  background: #E8EEF5;
  border: 1rpx solid #E2E8F0;
  border-radius: 12rpx;
}

.current-url {
  font-size: 26rpx;
  color: #087CF0;
  word-break: break-all;
}

/* 表单 */
.form-item {
  margin-bottom: 24rpx;
}

.label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #5A6B7F;
  font-weight: 500;
}

.control {
  width: 100%;
  padding: 0 24rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #1A2332;
  box-sizing: border-box;
}

.input {
  height: 84rpx;
}

.input-placeholder {
  color: #94A3B8;
  font-size: 28rpx;
}

/* 保存按钮 */
.btn-primary {
  margin-top: 12rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 999rpx;
  text-align: center;
  box-shadow: 0 6rpx 20rpx rgba(8, 124, 240, 0.25);
  border: none;
  padding: 0;
}

.btn-primary::after {
  border: none;
}

.btn-icon {
  font-size: 28rpx;
}

/* 测试按钮 */
.btn-test {
  height: 80rpx;
  line-height: 80rpx;
  background: #FFFFFF;
  color: #087CF0;
  font-size: 28rpx;
  font-weight: 600;
  border: 2rpx solid #087CF0;
  border-radius: 999rpx;
  text-align: center;
  padding: 0;
}

.btn-test::after {
  border: none;
}

.btn-test[disabled] {
  opacity: 0.5;
}

/* 连接状态 */
.conn-bar {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  gap: 10rpx;
}

.conn-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.conn-text {
  font-size: 24rpx;
  line-height: 1.4;
}

.conn-bar.testing {
  background: #FFFBEB;
}

.conn-bar.testing .conn-dot {
  background: #F59E0B;
  animation: blink 1s infinite;
}

.conn-bar.testing .conn-text {
  color: #92400E;
}

.conn-bar.ok {
  background: #D1FAE5;
}

.conn-bar.ok .conn-dot {
  background: #10B981;
}

.conn-bar.ok .conn-text {
  color: #065F46;
}

.conn-bar.fail {
  background: #FEF2F2;
}

.conn-bar.fail .conn-dot {
  background: #EF4444;
}

.conn-bar.fail .conn-text {
  color: #991B1B;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
