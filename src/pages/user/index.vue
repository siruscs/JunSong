<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-wrap">
      <input
        class="search"
        v-model="keyword"
        placeholder="搜索用户名或手机号"
        confirm-type="search"
        @confirm="refresh"
      />
      <button class="search-button" @tap="refresh">查询</button>
    </view>

    <view class="action-wrap">
      <view class="session-action-card">
        <view class="session-copy">
          <text class="session-title">微信会话管理</text>
          <text class="session-subtitle">让全部微信登录会话重新校验</text>
        </view>
        <button class="session-button" @tap="revokeWechatSessions">
          <text class="session-button-icon">↻</text>
          <text>一键清除</text>
        </button>
      </view>
    </view>

    <!-- 用户列表 -->
    <scroll-view
      scroll-y
      class="scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="refresh"
      @scrolltolower="loadMore"
    >
      <view
        class="record-card"
        v-for="item in rows"
        :key="item.userId"
        @tap="openDetail(item)"
      >
        <view class="card-bar"></view>
        <view class="card-body">
          <view class="card-header">
            <view class="record-title">
              {{ item.userName || '-' }}
              <text class="nick-name" v-if="item.nickName">（{{ item.nickName }}）</text>
            </view>
            <view class="user-status-badge" :class="item.status === '0' ? 'status-ok' : 'status-danger'">
              {{ item.status === '0' ? '正常' : '停用' }}
            </view>
          </view>

          <view class="summary-grid">
            <view class="summary-item">
              <text class="summary-label">手机号</text>
              <text class="summary-value">{{ item.phonenumber || '-' }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">所属部门</text>
              <text class="summary-value">{{ getDeptName(item) }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">角色</text>
              <text class="summary-value">{{ getRolesText(item) }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">微信绑定</text>
              <text class="summary-value" :class="item.mpBindingStatus === 'BOUND' ? 'status-ok' : ''">
                {{ wechatBindingText(item) }}
              </text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty" v-if="!loading && rows.length === 0">
        <view class="empty-title">暂无用户</view>
        <view class="empty-subtitle">点击右下角新增用户</view>
      </view>
      <view class="loading" v-if="loading">加载中</view>
      <view class="loading" v-if="finished && rows.length > 0">没有更多了</view>
    </scroll-view>

    <!-- 新增按钮 -->
    <view class="fab" v-if="canAdd" @tap="addUser">
      <text class="fab-icon">＋</text>
    </view>
  </view>
</template>

<script>
import { request, getBaseUrl } from '@/api/index.js'
import { isAdmin } from '@/utils/permission.js'

export default {
  data() {
    return {
      keyword: '',
      pageNum: 1,
      pageSize: 10,
      rows: [],
      loading: false,
      refreshing: false,
      finished: false,
      canAdd: false
    }
  },
  onLoad() {
    if (!isAdmin()) {
      uni.showToast({ title: '暂无管理权限', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
      return
    }
    this.canAdd = true
    this.refresh()
  },
  onShow() {
    if (this.canAdd) {
      this.refresh()
    }
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
    normalizeMpBindings(res) {
      const data = res.data || res.rows || res || []
      return Array.isArray(data) ? data : []
    },
    isActiveMpBinding(binding) {
      const status = String(binding.status || binding.bindStatus || binding.mpStatus || '').toUpperCase()
      if (status === 'ACTIVE' || status === 'BOUND' || status === '0') return true
      if (binding.bound === true || binding.bind === true || binding.isBound === true) return true
      const statusText = String(binding.statusText || binding.bindStatusText || binding.remark || '')
      return statusText.includes('绑定') && !statusText.includes('未绑定') && !statusText.includes('解绑')
    },
    async loadMpBindingStatus(list) {
      const users = list.filter(user => user && user.userId)
      await Promise.all(users.map(async (user) => {
        try {
          const res = await request({
            url: `/system/user/${user.userId}/mp-binding`,
            method: 'GET',
            noRedirect: true,
            silent: true,
            timeout: 8000
          })
          const bindings = this.normalizeMpBindings(res)
          const activeCount = bindings.filter(item => this.isActiveMpBinding(item)).length
          const target = this.rows.find(row => String(row.userId) === String(user.userId))
          if (target) {
            target.mpBindingStatus = activeCount > 0 ? 'BOUND' : 'UNBOUND'
            target.mpBindingCount = activeCount
          }
        } catch (e) {
          const target = this.rows.find(row => String(row.userId) === String(user.userId))
          if (target) {
            target.mpBindingStatus = 'UNKNOWN'
            target.mpBindingCount = 0
          }
        }
      }))
    },
    wechatBindingText(item) {
      if (item.mpBindingStatus === 'BOUND') {
        return item.mpBindingCount > 1 ? `微信已绑定${item.mpBindingCount}个` : '微信已绑定'
      }
      if (item.mpBindingStatus === 'UNBOUND') return '微信未绑定'
      return '微信状态未知'
    },
    getDeptName(item) {
      if (item.dept && item.dept.deptName) return item.dept.deptName
      if (item.deptName) return item.deptName
      return '-'
    },
    getRolesText(item) {
      if (item.roleNames) return item.roleNames
      const roles = item.roles || []
      if (!roles.length) return '-'
      return roles.map(r => r.roleName || r.roleKey).filter(Boolean).join('、') || '-'
    },
    wechatBindingClass(item) {
      return item.mpBindingStatus === 'BOUND' ? 'wechat-bound' : 'wechat-unbound'
    },
    buildQuery() {
      const query = { pageNum: this.pageNum, pageSize: this.pageSize }
      const val = this.keyword.trim()
      if (val) {
        if (/^\d+$/.test(val)) {
          query.phonenumber = val
        } else {
          query.userName = val
        }
      }
      return query
    },
    async fetchList(reset) {
      this.loading = true
      try {
        const res = await request({ url: '/system/user/list', method: 'GET', data: this.buildQuery() })
        const list = res.rows || res.data || []
        this.rows = reset ? list : this.rows.concat(list)
        this.finished = list.length < this.pageSize
        this.loadMpBindingStatus(list)
      } catch (e) {
        console.error('加载用户列表失败', e)
      } finally {
        this.loading = false
      }
    },
    async refresh() {
      this.pageNum = 1
      this.finished = false
      this.refreshing = true
      await this.fetchList(true)
      this.refreshing = false
    },
    async loadMore() {
      if (this.loading || this.finished) return
      this.pageNum += 1
      await this.fetchList(false)
    },
    openDetail(item) {
      uni.navigateTo({ url: `/pages/user/detail?id=${item.userId}` })
    },
    addUser() {
      uni.navigateTo({ url: '/pages/user/form' })
    },
    revokeWechatSessions() {
      uni.showModal({
        title: '一键清除微信会话',
        content: '清除后，当前租户下所有微信登录用户下次操作需要重新登录。是否继续？',
        confirmText: '清除',
        success: async (res) => {
          if (!res.confirm) return
          try {
            const reason = encodeURIComponent('小程序端管理员一键清除微信会话')
            await request({ url: `/system/wechat-session/revoke-all?reason=${reason}`, method: 'POST' })
            uni.showToast({ title: '已清除微信会话', icon: 'success' })
          } catch (e) {
            console.error('清除微信会话失败', e)
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
  background: #E8EEF5;
}

.search-wrap {
  display: flex;
  gap: 12rpx;
  padding: 20rpx 28rpx;
  background: #FFFFFF;
  box-shadow: 0 2rpx 8rpx rgba(8, 124, 240, 0.04);
}

.search {
  flex: 1;
  height: 80rpx;
  padding: 0 28rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.search-button {
  width: 108rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  font-size: 26rpx;
  border-radius: 999rpx;
}

.action-wrap {
  padding: 0 28rpx 18rpx;
  background: #FFFFFF;
}

.session-action-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 18rpx 20rpx;
  background: #E8EEF5;
  border: 2rpx solid #CBD5E1;
  border-radius: 16rpx;
}

.session-copy {
  flex: 1;
  min-width: 0;
}

.session-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
}

.session-subtitle {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #64748B;
}

.session-button {
  width: 190rpx;
  height: 68rpx;
  line-height: 68rpx;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  background: #334155;
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 12rpx;
}

.session-button::after {
  border: none;
}

.session-button-icon {
  font-size: 26rpx;
  color: #FFFFFF;
}

.scroll {
  height: calc(100vh - 220rpx);
  padding: 16rpx 0 30rpx;
}

.record-card {
  display: flex;
  margin: 0 28rpx 16rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
  overflow: hidden;
}

.card-bar {
  width: 4rpx;
  background: linear-gradient(180deg, #087CF0, #A8C7E5);
  flex-shrink: 0;
}

.card-body {
  flex: 1;
  padding: 24rpx 28rpx;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.record-title {
  flex: 1;
  font-size: 30rpx;
  line-height: 42rpx;
  font-weight: 700;
  color: #1A2332;
}

.record-title .nick-name {
  font-size: 26rpx;
  color: #5A6B7F;
  font-weight: 500;
}

.record-id {
  padding: 4rpx 14rpx;
  background: #E8EEF5;
  color: #5A6B7F;
  font-size: 20rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.user-status-badge {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-weight: 500;
  flex-shrink: 0;
}

.user-status-badge.status-ok {
  background: #D1FAE5;
  color: #065F46;
}

.user-status-badge.status-danger {
  background: #FEE2E2;
  color: #991B1B;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  margin-top: 16rpx;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.summary-label {
  font-size: 22rpx;
  color: #94A3B8;
}

.summary-value {
  font-size: 26rpx;
  color: #1A2332;
  font-weight: 500;
}

.summary-value.status-ok {
  color: #047857;
  font-weight: 700;
}

.summary-value.status-danger {
  color: #B91C1C;
  font-weight: 700;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid #E8EEF5;
}

.meta-text {
  font-size: 24rpx;
  color: #94A3B8;
}

.arrow-icon {
  font-size: 36rpx;
  color: #CBD5E1;
  font-weight: 300;
}

.fab {
  position: fixed;
  right: 36rpx;
  bottom: 80rpx;
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(8, 124, 240, 0.3);
  z-index: 100;
}

.fab-icon {
  font-size: 48rpx;
  color: #FFFFFF;
  font-weight: 300;
}

.empty,
.loading {
  padding: 60rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 26rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A2332;
}

.empty-subtitle {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #94A3B8;
}
</style>
