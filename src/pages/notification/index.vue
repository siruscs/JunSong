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
            <text class="notice-arrow" v-if="item.taskId || item.businessId">查看详情 ›</text>
          </view>
        </view>
      </view>

      <view class="empty" v-if="!loading && rows.length === 0">
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
      markingAll: false
    }
  },
  onShow() {
    this.refresh()
  },
  methods: {
    isUnread(item) {
      return String(item.readStatus ?? item.status ?? '0') === '0'
    },
    noticeTitle(item) {
      return item.title || item.notificationTitle || item.taskName || '系统通知'
    },
    noticeContent(item) {
      return item.content || item.notificationContent || item.message || ''
    },
    noticeTypeText(item) {
      const type = String(item.notificationType ?? item.type ?? '0')
      const map = { '0': '系统', '1': '审批', '2': '待办', '3': '催办' }
      return map[type] || '通知'
    },
    noticeTypeClass(item) {
      const type = String(item.notificationType ?? item.type ?? '0')
      const map = { '0': 'type-system', '1': 'type-approve', '2': 'type-todo', '3': 'type-urgent' }
      return map[type] || 'type-system'
    },
    async refresh() {
      this.pageNum = 1
      this.finished = false
      this.refreshing = true
      await this.fetchList(true)
      this.refreshing = false
      this.loadUnreadCount()
    },
    async loadMore() {
      if (this.loading || this.finished) return
      this.pageNum += 1
      await this.fetchList(false)
    },
    async fetchList(reset) {
      this.loading = true
      try {
        const res = await getNotifications({ pageNum: this.pageNum, pageSize: this.pageSize })
        const list = res.rows || res.data || []
        this.rows = reset ? list : this.rows.concat(list)
        this.finished = list.length < this.pageSize
      } catch (e) {
        console.error('加载通知失败', e)
      } finally {
        this.loading = false
      }
    },
    async loadUnreadCount() {
      try {
        const res = await getUnreadCount()
        const count = res.data
        this.unreadCount = Number(count === undefined || count === null ? 0 : count) || 0
      } catch (e) {
        console.log('获取未读数失败', e)
      }
    },
    async openNotice(item) {
      if (this.isUnread(item) && item.id) {
        try {
          await markRead(item.id)
          item.readStatus = '1'
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (e) {
          console.log('标记已读失败', e)
        }
      }
      const taskId = item.taskId || item.businessId
      if (taskId) {
        uni.navigateTo({ url: '/pages/workflow/detail?taskId=' + taskId })
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
            this.rows.forEach((item) => { item.readStatus = '1' })
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
  background: #F0F4F8;
  width: 100vw;
  max-width: 100vw;
  overflow-x: hidden;
  box-sizing: border-box;
}

.hero {
  margin: 24rpx 28rpx 0;
  padding: 36rpx 30rpx;
  background: linear-gradient(135deg, #173B57, #2A6F97);
  border-radius: 24rpx;
  box-shadow: 0 20rpx 54rpx rgba(42, 111, 151, 0.18);
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
  color: rgba(255, 255, 255, 0.72);
  font-weight: 600;
  letter-spacing: 2rpx;
}

.hero-title {
  margin-top: 8rpx;
  font-size: 42rpx;
  font-weight: 800;
  color: #FFFFFF;
}

.hero-badge {
  width: 110rpx;
  height: 110rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.14);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-badge-num {
  font-size: 40rpx;
  font-weight: 800;
  color: #FFFFFF;
}

.hero-badge-label {
  margin-top: 2rpx;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.66);
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
  color: #2A6F97;
  font-size: 24rpx;
  font-weight: 700;
  border-radius: 999rpx;
  border: 1rpx solid rgba(42, 111, 151, 0.18);
  box-shadow: 0 2rpx 12rpx rgba(42, 111, 151, 0.06);
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
  box-shadow: 0 8rpx 26rpx rgba(42, 111, 151, 0.07);
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
  color: #2A6F97;
  font-weight: 700;
}

.empty {
  padding: 100rpx 40rpx;
  text-align: center;
}

.empty-mark {
  width: 96rpx;
  height: 96rpx;
  line-height: 96rpx;
  margin: 0 auto 22rpx;
  border-radius: 28rpx;
  background: #ECF4F7;
  color: #2A6F97;
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
