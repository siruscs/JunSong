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
          <view class="hero-avatar" :style="{ background: avatarUrl(user.avatar) ? '#F1F5F9' : avatarColor(user.userName) }">
            <image v-if="avatarUrl(user.avatar)" class="hero-avatar-img" :src="avatarUrl(user.avatar)" mode="aspectFill" />
            <text v-else class="avatar-fallback">{{ firstChar(user.userName) }}</text>
          </view>
          <view class="hero-main">
            <view class="hero-name">{{ user.userName || '-' }}</view>
            <view class="hero-nick">{{ user.nickName || '-' }}</view>
            <view class="hero-meta-row">
              <text class="hero-dept">{{ deptNames(user) }}</text>
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
        <view class="field-row">
          <text class="field-label">所属部门</text>
          <text class="field-value">{{ deptNames(user) }}</text>
        </view>
      </view>

      <!-- 微信绑定 -->
      <view class="section-card">
        <view class="section-title">微信绑定</view>
        <view class="field-row">
          <text class="field-label">绑定状态</text>
          <view class="status-pill" :class="mpBindingActive ? 'status-ok' : 'status-disabled'">
            {{ mpBindingText }}
          </view>
        </view>
      </view>

      <!-- 账号锁定状态 -->
      <view class="section-card" v-if="lockStatus">
        <view class="section-title">账号安全状态</view>
        <view class="field-row">
          <text class="field-label">密码错误次数</text>
          <text class="field-value">{{ lockStatus.errorCount || 0 }} / {{ lockStatus.maxRetryCount || 5 }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">账号锁定</text>
          <view class="status-pill" :class="lockStatus.locked ? 'status-disabled' : 'status-ok'">
            <block v-if="lockStatus.locked">
              已锁定
              <text v-if="lockStatus.lockTimeRemainingMinutes > 0">（剩余{{ lockStatus.lockTimeRemainingMinutes }}分钟）</text>
            </block>
            <block v-else>未锁定</block>
          </view>
        </view>
      </view>
      <view class="section-card" v-else>
        <view class="section-title">账号安全状态</view>
        <view class="field-row">
          <text class="field-label">密码错误次数</text>
          <text class="field-value">查询中...</text>
        </view>
        <view class="field-row">
          <text class="field-label">账号锁定</text>
          <view class="status-pill status-ok">未锁定</view>
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
      <button class="action-btn reset-btn" @tap="handleResetPassword">重置密码</button>
      <button
        class="action-btn unlock-btn"
        @tap="handleUnlock">
        {{ lockStatus && lockStatus.locked ? '解锁账号' : '账号正常' }}
      </button>
      <button class="action-btn delete-btn" @tap="handleDelete">删除</button>
    </view>
  </view>
</template>

<script>
import { request, deleteData, getBaseUrl } from '@/api/index.js'
import { isAdmin } from '@/utils/permission.js'

export default {
  data() {
    return {
      userId: '',
      loading: true,
      user: null,
      mpBindings: [],
      lockStatus: null
    }
  },
  computed: {
    mpBindingActive() {
      return this.mpBindings.some(item => item.status === 'ACTIVE')
    },
    mpBindingText() {
      const count = this.mpBindings.filter(item => item.status === 'ACTIVE').length
      if (!count) return '未绑定'
      return count > 1 ? `已绑定${count}个微信` : '已绑定'
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
      const colors = ['#087CF0', '#5AA9E8', '#A8C7E5', '#059669', '#0284c7', '#7c3aed', '#db2777', '#ea580c']
      if (!name) return colors[0]
      let hash = 0
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash)
      }
      return colors[Math.abs(hash) % colors.length]
    },
    avatarUrl(avatar) {
      if (!avatar) return ''
      if (/^(https?:)?\/\//.test(avatar) || avatar.startsWith('data:') || avatar.startsWith('wxfile://')) {
        return avatar
      }
      const baseUrl = getBaseUrl().replace(/\/$/, '')
      if (avatar.startsWith('/statics/')) {
        return baseUrl.replace(/\/prod-api$/, '').replace(/\/dev-api$/, '') + avatar
      }
      const path = avatar.startsWith('/') ? avatar : `/${avatar}`
      return `${baseUrl}${path}`
    },
    deptNames(user) {
      const depts = user.depts || user.deptList || []
      if (Array.isArray(depts) && depts.length) {
        return depts.map(dept => dept.deptName || dept.label || dept.name).filter(Boolean).join('、') || '-'
      }
      if (user.deptId && user.deptName) return user.deptName
      if (user.deptName) return user.deptName
      return user.dept && user.dept.deptName ? user.dept.deptName : '-'
    },
    sexText(val) {
      const map = { '0': '男', '1': '女', '2': '未知' }
      return map[val] || '-'
    },
    normalizeMpBindings(res) {
      const data = res.data || res.rows || res || []
      return Array.isArray(data) ? data : []
    },
    firstNonEmptyArray(...values) {
      return values.find(value => Array.isArray(value) && value.length) || []
    },
    async loadMpBindingStatus() {
      try {
        const res = await request({
          url: `/system/user/${this.userId}/mp-binding`,
          method: 'GET',
          noRedirect: true,
          silent: true,
          timeout: 8000
        })
        this.mpBindings = this.normalizeMpBindings(res)
      } catch (e) {
        this.mpBindings = []
      }
    },
    async loadUser() {
      this.loading = true
      try {
        const res = await request({ url: `/system/user/${this.userId}`, method: 'GET' })
        const data = res.data || res
        const deptIds = this.firstNonEmptyArray(data.deptIds, res.deptIds, data.deptId ? [data.deptId] : [])
        const depts = this.firstNonEmptyArray(data.depts, res.depts, data.dept ? [data.dept] : [])
        this.user = {
          ...data,
          deptIds,
          depts,
          deptId: data.deptId || res.deptId || '',
          deptName: data.deptName || res.deptName || (data.dept && data.dept.deptName) || ''
        }
        await this.loadMpBindingStatus()
        await this.loadLockStatus()
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
    handleResetPassword() {
      const defaultPwd = (this.lockStatus && this.lockStatus.initPassword) || ''
      uni.showModal({
        title: '重置密码',
        content: defaultPwd ? `将重置为初始密码 ${defaultPwd}，是否继续？` : '将重置为系统设置的初始密码，是否继续？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            const resp = await request({
              url: '/system/user/resetPwd',
              method: 'PUT',
              data: { userId: this.userId }
            })
            const realPwd = (resp && (resp.defaultPassword || resp.data && resp.data.defaultPassword)) || defaultPwd || '初始密码'
            uni.showToast({ title: `密码已重置为${realPwd}`, icon: 'success' })
            await this.loadLockStatus()
          } catch (e) {
            console.error('重置密码失败', e)
            uni.showToast({ title: '重置密码失败', icon: 'none' })
          }
        }
      })
    },
    async loadLockStatus() {
      try {
        const res = await request({
          url: `/system/user/${this.userId}/pwd-lock-status`,
          method: 'GET'
        })
        const payload = (res && (res.data || res)) || {}
        this.lockStatus = Object.assign({
          userName: '',
          errorCount: 0,
          maxRetryCount: 5,
          locked: false,
          lockTimeRemainingMinutes: 0,
          initPassword: '123456'
        }, payload || {})
      } catch (e) {
        console.error('loadLockStatus error', e)
        this.lockStatus = {
          userName: '',
          errorCount: 0,
          maxRetryCount: 5,
          locked: false,
          lockTimeRemainingMinutes: 0,
          initPassword: '123456'
        }
      }
    },
    handleUnlock() {
      uni.showModal({
        title: '解锁账号',
        content: this.lockStatus && this.lockStatus.locked
          ? `当前账号已锁定，将清除密码错误计数（${this.lockStatus.errorCount}/${this.lockStatus.maxRetryCount || 5}），解除锁定状态，是否继续？`
          : '将清除该账号的密码错误计数，是否继续？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({
              url: `/system/user/${this.userId}/unlock`,
              method: 'PUT'
            })
            uni.showToast({ title: '账号已解锁', icon: 'success' })
            await this.loadLockStatus()
          } catch (e) {
            console.error('解锁账号失败', e)
            uni.showToast({ title: '解锁失败', icon: 'none' })
          }
        }
      })
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
  background: #E8EEF5;
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
  background: linear-gradient(135deg, #087CF0, #5AA9E8, #A8C7E5);
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
  overflow: hidden;
}

.hero-avatar-img {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
}

.avatar-fallback {
  color: #FFFFFF;
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
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A2332;
  margin-bottom: 20rpx;
  padding-left: 16rpx;
  border-left: 4rpx solid #087CF0;
}

/* 字段列表 */
.field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #E8EEF5;
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
  color: #087CF0;
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
  box-shadow: 0 -2rpx 16rpx rgba(8, 124, 240, 0.06);
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
  background: #E8EEF5;
  color: #087CF0;
}

.reset-btn {
  background: #FEF3C7;
  color: #B45309;
}

.unlock-btn {
  background: #D1FAE5;
  color: #065F46;
}

.unlock-btn.disabled {
  background: #E8EEF5;
  color: #94A3B8;
  opacity: 0.75;
}

.delete-btn {
  background: #FEF2F2;
  color: #EF4444;
}

.action-btn {
  font-size: 26rpx;
}
</style>
