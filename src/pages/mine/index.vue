<template>
  <view class="page">
    <!-- 头部 -->
    <view class="profile-header">
      <view class="profile-bg"></view>
      <view class="profile-content" :style="{ paddingTop: statusBarH + 'px' }">
        <view class="avatar">
          <image class="avatar-img" v-if="userInfo.avatar" :src="avatarUrl" mode="aspectFill"></image>
          <text class="avatar-text" v-else>{{ avatarChar }}</text>
        </view>
        <view class="profile-info">
          <text class="username">{{ displayName }}</text>
          <text class="dept-name">{{ userInfo.deptName || userInfo.phonenumber || '' }}</text>
        </view>
      </view>
    </view>

    <!-- 菜单 -->
    <view class="menu-card">
      <view class="menu-item" @tap="goProfile" hover-class="menu-item--active">
        <view class="menu-icon-wrap profile-menu-bg"><text class="menu-icon-text profile-icon-color">资</text></view>
        <text class="menu-label">个人资料设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goPassword" hover-class="menu-item--active">
        <view class="menu-icon-wrap password-bg"><text class="menu-icon-text password-icon-color">密</text></view>
        <text class="menu-label">密码修改</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="handleWechatBinding" hover-class="menu-item--active">
        <view class="menu-icon-wrap wechat-bg"><text class="menu-icon-text wechat-icon-color">微</text></view>
        <text class="menu-label">微信账号</text>
        <text class="menu-binding-status" v-if="wechatBound">已绑定</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" v-if="isAdmin" @tap="goSettings" hover-class="menu-item--active">
        <view class="menu-icon-wrap settings-bg"><text class="menu-icon-text settings-icon-color">设</text></view>
        <text class="menu-label">接口设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item menu-item--danger" @tap="handleLogout" hover-class="menu-item--active">
        <view class="menu-icon-wrap danger-bg"><text class="menu-icon-text danger-icon-color">出</text></view>
        <text class="menu-label">退出登录</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 版本 -->
    <view class="version">
      <text class="version-text">松·云助手 v1.7.0</text>
    </view>
  </view>
</template>

<script>
import miniProgramShare from '@/mixins/miniProgramShare.js'
import { setToken } from '@/api/index.js'
import { request } from '@/api/index.js'
import { isAdmin } from '@/utils/permission.js'
import { getStatusBarHeight } from '@/utils/systemInfo.js'

export default {
  mixins: [miniProgramShare],
  data() {
    return {
      userInfo: {},
      statusBarH: 0,
      isAdmin: false,
      wechatBound: false
    }
  },
  computed: {
    displayName() {
      return this.userInfo.nickName || this.userInfo.userName || this.userInfo.username || '未登录'
    },
    avatarChar() {
      const name = this.displayName || ''
      return name.charAt(0).toUpperCase() || '?'
    },
    avatarUrl() {
      const url = this.userInfo.avatar || ''
      if (!url) return ''
      if (url.startsWith('http://') || url.startsWith('https://')) return url
      const baseUrl = uni.getStorageSync('baseUrl') || 'https://www.junsong.vip/prod-api'
      if (url.startsWith('/statics/')) {
        return baseUrl.replace(/\/prod-api$/, '').replace(/\/dev-api$/, '') + url
      }
      return baseUrl + url
    }
  },
  onShow() {
    this.statusBarH = getStatusBarHeight()
    this.userInfo = uni.getStorageSync('userInfo') || {}
    const modules = uni.getStorageSync('modules') || []
    this.isAdmin = isAdmin(modules)
    this.loadWechatBindingStatus()
  },
  methods: {
    goProfile() {
      uni.navigateTo({ url: '/pages/profile/index' })
    },
    goPassword() {
      uni.navigateTo({ url: '/pages/password/index' })
    },
    goSettings() {
      uni.navigateTo({ url: '/pages/settings/index' })
    },
    logRequestFailure(label, error) {
      const message = error?.msg || error?.errMsg || error?.message || String(error || '')
      console.warn(label + ': ' + message)
    },
    async loadWechatBindingStatus() {
      try {
        const res = await request({
          url: '/auth/mp/wechat/binding',
          method: 'GET',
          noRedirect: true,
          silent: true,
          timeout: 10000
        })
        const data = res.data || res
        const list = Array.isArray(data) ? data : (data.rows || data.list || [])
        this.wechatBound = list.some(b => b.status === 'ACTIVE')
      } catch (e) {
        this.logRequestFailure('查询微信绑定状态失败', e)
        this.wechatBound = false
      }
    },
    handleWechatBinding() {
      if (this.wechatBound) {
        this.showUnbindConfirm()
      } else {
        uni.navigateTo({ url: '/pages/wechat-bind/index' })
      }
    },
    showUnbindConfirm() {
      uni.showModal({
        title: '解绑微信',
        content: '解绑后将无法使用微信快捷登录，确定要解绑吗？',
        confirmText: '确认解绑',
        confirmColor: '#EF4444',
        success: (res) => {
          if (res.confirm) {
            this.doUnbind()
          }
        }
      })
    },
    async doUnbind() {
      uni.showLoading({ title: '解绑中...' })
      try {
        await request({
          url: '/auth/mp/wechat/unbind',
          method: 'POST',
          noRedirect: true,
          silent: true,
          timeout: 15000
        })
        uni.hideLoading()
        this.wechatBound = false
        uni.showToast({ title: '解绑成功', icon: 'success' })
      } catch (e) {
        uni.hideLoading()
        this.logRequestFailure('解绑失败', e)
        const msg = e?.msg || e?.errMsg || ''
        uni.showToast({ title: msg || '解绑失败，请重试', icon: 'none' })
      }
    },
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            setToken('')
            uni.removeStorageSync('userInfo')
            uni.removeStorageSync('modules')
            uni.removeStorageSync('permissions')
            uni.reLaunch({ url: '/pages/login/index' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #E6EEF6 0%, #F3F6FA 46%, #E8EEF5 100%);
}

.profile-header {
  position: relative;
  overflow: hidden;
  padding-bottom: 54rpx;
  background: linear-gradient(180deg, #C7DCF2 0%, #E1ECF8 100%);
  border-bottom: 2rpx solid #AFCBE7;
}

.profile-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,.58) 0%, rgba(202,224,246,.9) 100%);
  border-bottom: 1rpx solid rgba(8,124,240,.08);
}

.profile-bg::before {
  content: '';
  position: absolute;
  width: 260rpx;
  height: 260rpx;
  right: -110rpx;
  top: -130rpx;
  border: 22rpx solid rgba(8,124,240,.05);
  border-radius: 50%;
}

.profile-bg::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2rpx;
  background: #A8C7E5;
  border-radius: 0;
}

.profile-content {
  position: relative;
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx 0;
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  background: #EAF3FF;
  border: 2rpx solid #CFE0F8;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(5, 53, 107, 0.18);
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.avatar-text {
  font-size: 48rpx;
  font-weight: 700;
  color: #087CF0;
}

.profile-info {
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 34rpx;
  font-weight: 700;
  color: #1F2D3D;
}

.dept-name {
  font-size: 24rpx;
  color: #8190A1;
  margin-top: 8rpx;
}

.menu-card {
  margin: 0 28rpx;
  background: #ffffff;
  border-radius: 20rpx;
  border: 1rpx solid #D5E0EC;
  box-shadow: 0 5rpx 18rpx rgba(45, 72, 98, 0.07);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 28rpx;
  border-bottom: 1rpx solid #E8EEF5;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item--active {
  background: #F5F8FA;
}

.menu-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.profile-menu-bg {
  background: rgba(8, 124, 240, 0.08);
}

.password-bg {
  background: rgba(16, 185, 129, 0.08);
}

.settings-bg {
  background: rgba(8, 124, 240, 0.08);
}

.wechat-bg {
  background: rgba(7, 193, 96, 0.08);
}

.admin-bg {
  background: rgba(99, 102, 241, 0.08);
}

.danger-bg {
  background: rgba(239, 68, 68, 0.08);
}

.menu-icon-text {
  font-size: 24rpx;
  font-weight: 700;
}

.profile-icon-color {
  color: #087CF0;
}

.password-icon-color {
  color: #10B981;
}

.settings-icon-color {
  color: #087CF0;
}

.wechat-icon-color {
  color: #07C160;
}

.menu-binding-status {
  font-size: 22rpx;
  color: #07C160;
  margin-right: 8rpx;
}

.admin-icon-color {
  color: #6366F1;
}

.danger-icon-color {
  color: #EF4444;
}

.menu-label {
  font-size: 30rpx;
  color: #1A2332;
  font-weight: 500;
  flex: 1;
}

.menu-item--danger .menu-label {
  color: #EF4444;
}

.menu-arrow {
  font-size: 32rpx;
  color: #CBD5E1;
  font-weight: 300;
}

.version {
  text-align: center;
  padding: 40rpx 0;
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

.version-text {
  font-size: 24rpx;
  color: #94A3B8;
}
</style>
