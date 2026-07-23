<template>
  <view class="page">
    <view class="hero">
      <view class="hero-main">
        <view class="eyebrow">消息中心</view>
        <view class="hero-title">通知列表</view>
      </view>
      <view class="hero-badge" v-if="unreadCount > 0">
        <text class="hero-badge-num">{{ unreadCount }}</text>
        <text class="hero-badge-label">未读</text>
      </view>
    </view>

    <view class="action-bar">
      <text class="action-tip">共 {{ rows.length }} 条通知</text>
      <button class="read-all-btn" :disabled="markingAll || unreadCount === 0" @tap="handleMarkAllRead">
        {{ markingAll ? '处理中' : '全部已读' }}
      </button>
    </view>

    <scroll-view
      scroll-y
      class="scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="refresh"
      @scrolltolower="loadMore"
    >
      <view
        class="notice-card"
        hover-class="notice-card--active"
        v-for="item in rows"
        :key="item.id"
        @tap="openNotice(item)"
      >
        <view class="notice-dot" v-if="isUnread(item)"></view>
        <view class="notice-body">
          <view class="notice-head">
            <text class="notice-title">{{ noticeTitle(item) }}</text>
            <text class="notice-type" :class="noticeTypeClass(item)">{{ noticeTypeText(item) }}</text>
          </view>
          <text class="notice-content">{{ noticeContent(item) }}</text>
          <view class="notice-foot">
            <text class="notice-time">{{ item.createTime || '-' }}</text>
            <text class="notice-arrow" v-if="notificationTarget(item)">去处理 ›</text>
          </view>
        </view>
      </view>

      <view class="load-error" v-if="!loading && loadError">
        <view class="empty-title">通知加载失败</view>
        <view class="empty-subtitle">{{ loadError }}</view>
        <button class="retry-button" @tap="refresh">重新加载</button>
      </view>
      <view class="empty" v-if="!loading && !loadError && rows.length === 0">
        <view class="empty-mark">邮</view>
        <view class="empty-title">暂无通知</view>
        <view class="empty-subtitle">新的消息会在这里提醒你</view>
      </view>
      <view class="loading" v-if="loading">加载中</view>
      <view class="loading" v-if="finished && rows.length > 0">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script>
import { getNotifications, getUnreadCount, markRead, markAllRead } from '@/api/workflow.js'
import { hasExactPermission, hasModulePermission } from '@/utils/permission.js'
import { resolveNotificationTarget } from '@/utils/notificationTarget.js'

export default {
  data() {
    return {
      rows: [],
      pageNum: 1,
      pageSize: 10,
      loading: false,
      refreshing: false,
      finished: false,
      unreadCount: 0,
      markingAll: false,
      loadError: '',
      requestVersion: 0,
      unreadVersion: 0,
      markingReadIds: {}
    }
  },
  onShow() {
    this.refresh()
  },
  methods: {
    isUnread(item) {
      return String(item.isRead ?? item.readStatus ?? item.status ?? '0') === '0'
    },
    noticeTitle(item) {
      return item.title || item.notificationTitle || item.taskName || '系统通知'
    },
    noticeContent(item) {
      return item.content || item.notificationContent || item.message || ''
    },
    noticeTypeText(item) {
      const type = String(item.type ?? item.notificationType ?? '0')
      const map = {
        '0': '系统', '1': '审批', '2': '待办', '3': '催办',
        wf_todo: '待办', wf_timeout_urge: '催办', wf_timeout_transfer: '转办',
        wf_finished: '已办结', wf_rejected: '已驳回', finance_alert: '经营提醒'
      }
      return map[type] || '通知'
    },
    noticeTypeClass(item) {
      const type = String(item.type ?? item.notificationType ?? '0')
      const map = {
        '0': 'type-system', '1': 'type-approve', '2': 'type-todo', '3': 'type-urgent',
        wf_todo: 'type-todo', wf_timeout_urge: 'type-urgent', wf_timeout_transfer: 'type-urgent',
        wf_finished: 'type-approve', wf_rejected: 'type-urgent', finance_alert: 'type-urgent'
      }
      return map[type] || 'type-system'
    },
    notificationTarget(item) {
      return resolveNotificationTarget(item, {
        workflowTodo: hasModulePermission('wfTodo') && hasExactPermission('workflow:task:list'),
        workflowDone: hasModulePermission('wfDone') && hasExactPermission('workflow:task:list'),
        expenseList: hasModulePermission('expense') && hasExactPermission('finance:expense:list')
      })
    },
    async refresh() {
      this.pageNum = 1
      this.finished = false
      this.refreshing = true
      const requestVersion = this.requestVersion + 1
      await this.fetchList(true)
      if (requestVersion === this.requestVersion) {
        this.refreshing = false
        this.loadUnreadCount()
      }
    },
    async loadMore() {
      if (this.loading || this.finished) return
      const previousPage = this.pageNum
      const requestVersion = this.requestVersion + 1
      this.pageNum += 1
      const loaded = await this.fetchList(false)
      if (!loaded && requestVersion === this.requestVersion) this.pageNum = previousPage
    },
    async fetchList(reset) {
      const requestVersion = ++this.requestVersion
      this.loading = true
      if (reset) this.loadError = ''
      try {
        const res = await getNotifications({ pageNum: this.pageNum, pageSize: this.pageSize })
        const list = res.rows || res.data || []
        if (requestVersion !== this.requestVersion) return false
        if (!Array.isArray(list)) throw new Error('通知数据格式异常')
        this.rows = reset ? list : this.rows.concat(list)
        this.finished = list.length < this.pageSize
        return true
      } catch (e) {
        if (requestVersion !== this.requestVersion) return false
        if (reset) this.loadError = e?.msg || e?.message || '请检查网络后重试'
        else uni.showToast({ title: '加载更多失败，请重试', icon: 'none' })
        return false
      } finally {
        if (requestVersion === this.requestVersion) this.loading = false
      }
    },
    async loadUnreadCount() {
      const unreadVersion = ++this.unreadVersion
      try {
        const res = await getUnreadCount()
        if (unreadVersion !== this.unreadVersion) return
        const count = res.data
        this.unreadCount = Number(count === undefined || count === null ? 0 : count) || 0
      } catch (e) {
        // 列表中的未读状态仍可继续使用。
      }
    },
    async openNotice(item) {
      const target = this.notificationTarget(item)
      if (this.isUnread(item) && item.id && !this.markingReadIds[item.id]) {
        this.markingReadIds[item.id] = true
        try {
          await markRead(item.id)
          this.unreadVersion += 1
          item.isRead = '1'
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (e) {
          uni.showToast({ title: '已读状态暂未同步', icon: 'none' })
        } finally {
          delete this.markingReadIds[item.id]
        }
      }
      if (target) {
        uni.navigateTo({ url: target })
      } else {
        uni.showToast({ title: '暂无可打开的移动端页面', icon: 'none' })
      }
    },
    handleMarkAllRead() {
      if (this.unreadCount === 0) {
        uni.showToast({ title: '没有未读通知', icon: 'none' })
        return
      }
      uni.showModal({
        title: '全部已读',
        content: '确定将所有通知标记为已读吗？',
        success: async (res) => {
          if (!res.confirm) return
          this.markingAll = true
          try {
            await markAllRead()
            this.unreadVersion += 1
            this.rows.forEach((item) => { item.isRead = '1' })
            this.unreadCount = 0
            uni.showToast({ title: '已全部标记已读', icon: 'success' })
          } catch (e) {
            console.error('全部已读失败', e)
          } finally {
            this.markingAll = false
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
  width: 100vw;
  max-width: 100vw;
  overflow-x: hidden;
  box-sizing: border-box;
}

.hero {
  margin: 24rpx 28rpx 0;
  padding: 36rpx 30rpx;
  background: linear-gradient(135deg, #C7DCF2 0%, #EAF3FC 100%);
  border: 1rpx solid #B7D1EB;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(45, 72, 98, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.eyebrow {
  font-size: 22rpx;
  color: #5F7893;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.hero-title {
  margin-top: 8rpx;
  font-size: 42rpx;
  font-weight: 800;
  color: #1F2D3D;
}

.hero-badge {
  width: 110rpx;
  height: 110rpx;
  border-radius: 28rpx;
  background: #EEF5FF;
  border: 1rpx solid #CFE0F8;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-badge-num {
  font-size: 40rpx;
  font-weight: 800;
  color: #087CF0;
}

.hero-badge-label {
  margin-top: 2rpx;
  font-size: 20rpx;
  color: #6E86A0;
}

.action-bar {
  margin: 20rpx 28rpx 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.action-tip {
  font-size: 24rpx;
  color: #708196;
}

.read-all-btn {
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 28rpx;
  background: #FFFFFF;
  color: #087CF0;
  font-size: 24rpx;
  font-weight: 700;
  border-radius: 999rpx;
  border: 1rpx solid rgba(8, 124, 240, 0.18);
  box-shadow: 0 2rpx 12rpx rgba(8, 124, 240, 0.06);
}

.read-all-btn::after {
  border: none;
}

.read-all-btn[disabled] {
  background: #F1F5F9;
  color: #94A3B8;
  border-color: #E2E8F0;
}

.scroll {
  width: 100%;
  height: calc(100vh - 360rpx);
  padding: 18rpx 28rpx 150rpx;
  box-sizing: border-box;
  overflow-x: hidden;
}

.notice-card {
  position: relative;
  display: flex;
  margin-bottom: 18rpx;
  padding: 26rpx 28rpx;
  background: #FFFFFF;
  border-radius: 22rpx;
  box-shadow: 0 8rpx 26rpx rgba(8, 124, 240, 0.07);
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-sizing: border-box;
  overflow: hidden;
}

.notice-card--active {
  transform: scale(0.98);
  opacity: 0.9;
}

.notice-dot {
  position: absolute;
  top: 26rpx;
  right: 26rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #EF4444;
  box-shadow: 0 0 0 6rpx rgba(239, 68, 68, 0.14);
}

.notice-body {
  flex: 1;
  min-width: 0;
}

.notice-head {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.notice-title {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  line-height: 42rpx;
  font-weight: 800;
  color: #102A3A;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-type {
  flex-shrink: 0;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 700;
}

.notice-type.type-system {
  background: #E0F2FE;
  color: #075985;
}

.notice-type.type-approve {
  background: #D1FAE5;
  color: #065F46;
}

.notice-type.type-todo {
  background: #FEF3C7;
  color: #92400E;
}

.notice-type.type-urgent {
  background: #FEE2E2;
  color: #991B1B;
}

.notice-content {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 38rpx;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notice-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #F1F5F9;
}

.notice-time {
  font-size: 22rpx;
  color: #94A3B8;
}

.notice-arrow {
  font-size: 22rpx;
  color: #087CF0;
  font-weight: 700;
}

.empty,
.load-error {
  padding: 100rpx 40rpx;
  text-align: center;
}

.retry-button {
  width: 220rpx;
  height: 72rpx;
  margin: 28rpx auto 0;
  padding: 0;
  border: 1rpx solid #B8D4F0;
  border-radius: 8rpx;
  background: #E8F3FF;
  color: #087CF0;
  font-size: 26rpx;
  line-height: 72rpx;
}

.empty-mark {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  margin: 0 auto 22rpx;
  border-radius: 28rpx;
  background: #ECF4F7;
  color: #087CF0;
  font-size: 36rpx;
  font-weight: 800;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #102A3A;
}

.empty-subtitle {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #94A3B8;
}

.loading {
  padding: 50rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 24rpx;
}
</style>
