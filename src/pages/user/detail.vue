<template>
  <view class="detail-page">
    <!-- 加载状态 -->
    <view v-if="loading" class="loading-wrap">
      <text class="loading-text">加载中...</text>
    </view>

    <template v-else-if="user">
      <!-- 英雄卡片 -->
      <view class="hero-card">
        <view class="hero-bg"></view>
        <view class="hero-content">
          <view class="hero-avatar" :style="{ background: avatarColor(user.userName) }">
            {{ firstChar(user.userName) }}
          </view>
          <view class="hero-main">
            <view class="hero-name">{{ user.userName || '-' }}</view>
            <view class="hero-nick">{{ user.nickName || '-' }}</view>
            <view class="hero-meta-row">
              <text class="hero-dept" v-if="user.dept">{{ user.dept.deptName || '-' }}</text>
              <view class="hero-status" :class="user.status === '0' ? 'status-ok' : 'status-disabled'">
                {{ user.status === '0' ? '正常' : '停用' }}
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 基本信息 -->
      <view class="section-card">
        <view class="section-title">基本信息</view>
        <view class="field-row">
          <text class="field-label">用户名</text>
          <text class="field-value">{{ user.userName || '-' }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">昵称</text>
          <text class="field-value">{{ user.nickName || '-' }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">手机号</text>
          <text class="field-value">{{ user.phonenumber || '-' }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">邮箱</text>
          <text class="field-value">{{ user.email || '-' }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">性别</text>
          <text class="field-value">{{ sexText(user.sex) }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">状态</text>
          <view class="status-pill" :class="user.status === '0' ? 'status-ok' : 'status-disabled'">
            {{ user.status === '0' ? '正常' : '停用' }}
          </view>
        </view>
      </view>

      <!-- 角色信息 -->
      <view class="section-card" v-if="user.roles && user.roles.length">
        <view class="section-title">角色信息</view>
        <view class="role-list">
          <view class="role-tag" v-for="role in user.roles" :key="role.roleId">
            {{ role.roleName || role.roleKey }}
          </view>
        </view>
      </view>

      <!-- 底部占位 -->
      <view class="footer-placeholder"></view>
    </template>

    <!-- 空状态 -->
    <view v-else class="empty-wrap">
      <text class="empty-text">暂无数据</text>
    </view>

    <!-- 固定底部操作栏 -->
    <view v-if="user" class="footer-bar">
      <button class="action-btn edit-btn" @tap="handleEdit">编辑</button>
      <button class="action-btn delete-btn" @tap="handleDelete">删除</button>
    </view>
  </view>
</template>

<script>
import { request, deleteData } from '@/api/index.js'
import { isAdmin } from '@/utils/permission.js'

export default {
  data() {
    return {
      userId: '',
      loading: true,
      user: null
    }
  },
  onLoad(options) {
    if (!isAdmin()) {
      uni.showToast({ title: '暂无管理权限', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }

    this.userId = options.id || ''

    if (!this.userId) {
      uni.showToast({ title: '参数错误', icon: 'none' })
      this.loading = false
      return
    }

    this.loadUser()
  },
  methods: {
    firstChar(name) {
      if (!name) return '?'
      return name.charAt(0).toUpperCase()
    },
    avatarColor(name) {
      const colors = ['#2A6F97', '#3A8DB8', '#8EC8D2', '#059669', '#0284c7', '#7c3aed', '#db2777', '#ea580c']
      if (!name) return colors[0]
      let hash = 0
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash)
      }
      return colors[Math.abs(hash) % colors.length]
    },
    sexText(val) {
      const map = { '0': '男', '1': '女', '2': '未知' }
      return map[val] || '-'
    },
    async loadUser() {
      this.loading = true
      try {
        const res = await request({ url: `/system/user/${this.userId}`, method: 'GET' })
        this.user = res.data || res
      } catch (e) {
        console.error('加载用户详情失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    handleEdit() {
      uni.navigateTo({ url: `/pages/user/form?id=${this.userId}` })
    },
    handleDelete() {
      uni.showModal({
        title: '确认删除',
        content: '删除后不可恢复，是否继续？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await deleteData('/system/user', this.userId)
            uni.showToast({ title: '删除成功', icon: 'success' })
            setTimeout(() => uni.navigateBack(), 1500)
          } catch (e) {
            console.error('删除失败', e)
            uni.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #F0F4F8;
  padding-bottom: env(safe-area-inset-bottom);
}

.loading-wrap,
.empty-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.loading-text,
.empty-text {
  font-size: 28rpx;
  color: #94A3B8;
}

/* 英雄卡片 */
.hero-card {
  position: relative;
  margin: 24rpx 28rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2A6F97, #3A8DB8, #8EC8D2);
  border-radius: 20rpx;
}

.hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 28rpx;
  padding: 40rpx 36rpx;
}

.hero-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  font-weight: 700;
  color: #FFFFFF;
  background: rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.hero-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.hero-nick {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 6rpx;
}

.hero-meta-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 12rpx;
}

.hero-dept {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

.hero-status {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-weight: 500;
}

.hero-status.status-ok {
  background: rgba(255, 255, 255, 0.25);
  color: #FFFFFF;
}

.hero-status.status-disabled {
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
}

/* 通用卡片 */
.section-card {
  margin: 0 28rpx 24rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx 32rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
  margin-bottom: 20rpx;
  padding-left: 16rpx;
  border-left: 4rpx solid #2A6F97;
}

/* 字段列表 */
.field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #F0F4F8;
}

.field-row:last-child {
  border-bottom: none;
}

.field-label {
  font-size: 26rpx;
  color: #5A6B7F;
  flex-shrink: 0;
  margin-right: 24rpx;
}

.field-value {
  font-size: 26rpx;
  color: #1A2332;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.status-pill {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-weight: 500;
}

.status-ok {
  background: #D1FAE5;
  color: #065F46;
}

.status-disabled {
  background: #FEE2E2;
  color: #991B1B;
}

/* 角色标签 */
.role-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.role-tag {
  padding: 8rpx 22rpx;
  background: #E0F2FE;
  color: #2A6F97;
  font-size: 24rpx;
  font-weight: 500;
  border-radius: 999rpx;
  border: 1rpx solid #BAE6FD;
}

/* 底部占位 */
.footer-placeholder {
  height: 140rpx;
}

/* 固定底部操作栏 */
.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20rpx;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #FFFFFF;
  box-shadow: 0 -2rpx 16rpx rgba(42, 111, 151, 0.06);
  z-index: 100;
}

.action-btn {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 28rpx;
  font-weight: 500;
  border-radius: 16rpx;
  text-align: center;
  border: none;
  margin: 0;
  padding: 0;
}

.action-btn::after {
  border: none;
}

.edit-btn {
  background: #F0F4F8;
  color: #2A6F97;
}

.delete-btn {
  background: #FEF2F2;
  color: #EF4444;
}
</style>
