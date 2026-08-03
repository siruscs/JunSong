<template>
  <view class="page">
    <view class="bg-decor">
      <view class="halo halo-a"></view>
      <view class="halo halo-b"></view>
    </view>

    <view class="content" :style="{ paddingTop: (statusBarHeight + 36) + 'px' }">
      <view class="brand anim-fade-up">
        <view class="brand-mark">
          <image class="brand-logo-img" src="/static/logo.png" mode="aspectFit" />
        </view>
        <view class="brand-copy">
          <text class="brand-name">绑定微信账号</text>
          <text class="brand-sub">首次使用微信登录，请绑定已有系统账号</text>
        </view>
      </view>

      <view class="form-card anim-slide-up" style="animation-delay: 0.12s">
        <view class="form-head">
          <text class="form-title">账号绑定</text>
        </view>

        <view class="form-item">
          <view class="input-wrap">
            <view class="input-icon-wrap"><text class="input-icon-text">人</text></view>
            <input class="input-field" v-model="form.username" placeholder="请输入用户名" placeholder-class="input-placeholder" />
          </view>
        </view>
        <view class="form-item">
          <view class="input-wrap">
            <view class="input-icon-wrap"><text class="input-icon-text">密</text></view>
            <input class="input-field" v-model="form.password" password placeholder="请输入密码" placeholder-class="input-placeholder" />
          </view>
        </view>

        <button class="btn-bind" hover-class="btn-bind--active" :disabled="loading" @tap="handleBind">
          <text v-if="!loading">绑定并登录</text>
          <text v-else>{{ loadingText }}</text>
        </button>

        <view class="back-row" @tap="goBack">
          <text class="back-text">返回密码登录</text>
        </view>
      </view>

      <view class="tip-card anim-fade-up" style="animation-delay: 0.2s">
        <text class="tip-title">绑定说明</text>
        <text class="tip-line">· 绑定后可使用微信快捷登录，无需输入密码</text>
        <text class="tip-line">· 同一微信只能绑定一个系统账号</text>
        <text class="tip-line">· 可在"我的"页面随时解绑</text>
      </view>
    </view>

    <view class="safe-bottom"></view>
  </view>
</template>

<script>
import { request, setToken } from '@/api/index.js'
import { getStatusBarHeight } from '@/utils/systemInfo.js'

export default {
  data() {
    return {
      statusBarHeight: 0,
      form: {
        username: '',
        password: ''
      },
      loading: false,
      loadingText: '绑定中...'
    }
  },
  onLoad() {
    this.statusBarHeight = getStatusBarHeight()
  },
  methods: {
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.reLaunch({ url: '/pages/login/index' })
        }
      })
    },
    logRequestFailure(label, error) {
      const message = error?.msg || error?.errMsg || error?.message || String(error || '')
      console.warn(label + ': ' + message)
    },
    async handleBind() {
      if (this.loading) return
      if (!this.form.username) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!this.form.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }

      this.loading = true
      this.loadingText = '绑定中...'

      try {
        // 1. 调用 wx.login 获取新的临时 code
        const loginResult = await new Promise((resolve, reject) => {
          wx.login({ success: resolve, fail: reject })
        })
        const code = loginResult.code
        if (!code) {
          uni.showToast({ title: '微信授权失败，请重试', icon: 'none' })
          this.loading = false
          return
        }

        // 2. 调用后端绑定接口
        // isToken:false — 公开接口不带 token，避免旧 token 触发会话校验
        this.loadingText = '正在绑定...'
        const bindRes = await request({
          url: '/auth/mp/wechat/bind',
          method: 'POST',
          noRedirect: true,
          silent: true,
          timeout: 30000,
          header: { isToken: false },
          data: {
            code: code,
            username: this.form.username,
            password: this.form.password
          }
        })

        const tokenData = bindRes.data || bindRes
        const accessToken = tokenData.access_token
        if (!accessToken) {
          uni.showToast({ title: '绑定返回数据异常', icon: 'none' })
          this.loading = false
          return
        }

        // 3. 设置 token，加载用户信息
        setToken(accessToken)
        this.loadingText = '加载用户信息...'
        try {
          const userRes = await request({
            url: '/member/mp/userinfo',
            method: 'GET',
            noRedirect: true,
            silent: true,
            timeout: 12000
          })
          const userInfo = userRes.data || userRes
          uni.setStorageSync('userInfo', userInfo)
          uni.setStorageSync('modules', userInfo.modules || [])
          uni.setStorageSync('permissions', userInfo.permissions || [])
        } catch (e) {
          this.logRequestFailure('获取用户信息失败', e)
          uni.showToast({ title: '加载用户信息失败，请重试', icon: 'none' })
          this.loading = false
          return
        }

        uni.showToast({ title: '绑定成功' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/index/index' })
        }, 500)
        this.loading = false
      } catch (e) {
        this.logRequestFailure('绑定失败', e)
        const msg = e?.msg || e?.errMsg || ''
        if (msg.includes('已绑定')) {
          uni.showModal({
            title: '提示',
            content: '该微信已绑定其他账号，如需更换请先在"我的"页面解绑',
            showCancel: false
          })
        } else {
          uni.showToast({ title: msg || '绑定失败，请重试', icon: 'none' })
        }
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #EAF2F4;
  position: relative;
  overflow: hidden;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(40rpx); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(80rpx); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes softFloat {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(0, 22rpx, 0) scale(1.03); }
}

.anim-fade-up {
  opacity: 0;
  animation: fadeInUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.anim-slide-up {
  opacity: 0;
  animation: slideUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.bg-decor {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(2rpx);
  animation: softFloat 9s ease-in-out infinite;
}

.halo-a {
  width: 720rpx;
  height: 720rpx;
  top: -180rpx;
  right: -260rpx;
  background: radial-gradient(circle, rgba(8, 124, 240, 0.24), rgba(8, 124, 240, 0));
}

.halo-b {
  width: 520rpx;
  height: 520rpx;
  bottom: 80rpx;
  left: -220rpx;
  background: radial-gradient(circle, rgba(142, 200, 210, 0.38), rgba(142, 200, 210, 0));
  animation-delay: 1.2s;
}

.content {
  position: relative;
  z-index: 1;
  padding-left: 36rpx;
  padding-right: 36rpx;
}

.brand {
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.brand-mark {
  position: relative;
  width: 108rpx;
  height: 108rpx;
  flex-shrink: 0;
}

.brand-logo-img {
  width: 100%;
  height: 100%;
  display: block;
}


.brand-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 38rpx;
  font-weight: 700;
  color: #102A3A;
  letter-spacing: 1rpx;
}

.brand-sub {
  font-size: 24rpx;
  color: #5A6B7F;
  margin-top: 8rpx;
}

.form-card {
  position: relative;
  margin-top: 36rpx;
  padding: 34rpx 30rpx 30rpx;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 32rpx;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 24rpx 64rpx rgba(23, 59, 87, 0.14);
}

.form-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
}

.form-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #102A3A;
}

.form-item {
  margin-bottom: 22rpx;
}

.input-wrap {
  display: flex;
  align-items: center;
  min-height: 94rpx;
  background: #F7FAFC;
  border: 2rpx solid #E2E8F0;
  border-radius: 22rpx;
  padding: 0 24rpx;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}

.input-wrap:focus-within {
  border-color: #087CF0;
  background: #FFFFFF;
  box-shadow: 0 0 0 4rpx rgba(8, 124, 240, 0.1);
}

.input-icon-wrap {
  width: 48rpx;
  height: 48rpx;
  border-radius: 16rpx;
  background: rgba(8, 124, 240, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.input-icon-text {
  font-size: 22rpx;
  font-weight: 700;
  color: #087CF0;
}

.input-field {
  flex: 1;
  height: 94rpx;
  font-size: 28rpx;
  color: #1A2332;
}

.input-placeholder {
  color: #94A3B8;
}

.btn-bind {
  margin-top: 30rpx;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #07A85A, #07C160);
  color: #FFFFFF;
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  border-radius: 999rpx;
  text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(7, 193, 96, 0.3);
  border: none;
  padding: 0;
  transition: transform 0.15s, box-shadow 0.15s;
}

.btn-bind--active,
.btn-bind:active {
  transform: scale(0.97);
  box-shadow: 0 4rpx 16rpx rgba(7, 193, 96, 0.2);
}

.btn-bind::after {
  border: none;
}

.btn-bind[disabled] {
  opacity: 0.5;
  box-shadow: none;
}

.back-row {
  margin-top: 24rpx;
  text-align: center;
  padding: 12rpx;
}

.back-text {
  font-size: 26rpx;
  color: #087CF0;
}

.tip-card {
  margin-top: 28rpx;
  padding: 24rpx 28rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.6);
  border: 1rpx solid rgba(255, 255, 255, 0.8);
}

.tip-title {
  display: block;
  font-size: 24rpx;
  font-weight: 700;
  color: #087CF0;
  margin-bottom: 12rpx;
}

.tip-line {
  display: block;
  font-size: 22rpx;
  color: #5A6B7F;
  line-height: 36rpx;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
