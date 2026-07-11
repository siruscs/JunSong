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
      <text class="version-text">松·云助手 v1.1.0</text>
    </view>
  </view>
</template>

<script>
import { setToken } from '@/api/index.js'
import { isAdmin } from '@/utils/permission.js'

export default {
  data() {
    return {
      userInfo: {},
      statusBarH: 0,
      isAdmin: false
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
    this.statusBarH = uni.getSystemInfoSync().statusBarHeight || 20
    this.userInfo = uni.getStorageSync('userInfo') || {}
    const modules = uni.getStorageSync('modules') || []
    this.isAdmin = isAdmin(modules)
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
  background: #F0F4F8;
}

.profile-header {
  position: relative;
  overflow: hidden;
  padding-bottom: 80rpx;
}

.profile-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #173B57 0%, #2A6F97 40%, #3A8DB8 80%, #8EC8D2 100%);
}

.profile-bg::after {
  content: '';
  position: absolute;
  bottom: -60rpx;
  left: -10%;
  right: -10%;
  height: 140rpx;
  background: #F0F4F8;
  border-radius: 50% 50% 0 0;
}

.profile-content {
  position: relative;
  display: flex;
  align-items: center;
  padding: 32rpx 40rpx 0;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
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
  color: #ffffff;
}

.profile-info {
  margin-left: 28rpx;
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
}

.dept-name {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 8rpx;
}

.menu-card {
  margin: 0 28rpx;
  background: #ffffff;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 28rpx;
  border-bottom: 1rpx solid #F0F4F8;
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
  background: rgba(42, 111, 151, 0.08);
}

.password-bg {
  background: rgba(16, 185, 129, 0.08);
}

.settings-bg {
  background: rgba(42, 111, 151, 0.08);
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
  color: #2A6F97;
}

.password-icon-color {
  color: #10B981;
}

.settings-icon-color {
  color: #2A6F97;
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
