<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">核销记录</text>
      <text class="hero-sub">费用核销批次 · 查看明细</text>
    </view>

    <view class="tab-bar">
      <view class="tab-item" :class="{ active: filterStatus === '' }" @tap="switchStatus('')">全部</view>
      <view class="tab-item" :class="{ active: filterStatus === 'VERIFIED' }" @tap="switchStatus('VERIFIED')">已核销</view>
      <view class="tab-item" :class="{ active: filterStatus === 'REVERSED' }" @tap="switchStatus('REVERSED')">已反核销</view>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh" @scrolltolower="loadMore">
      <view class="list" v-if="batchList.length">
        <view class="batch-card" v-for="item in batchList" :key="item.batchId" @tap="goDetail(item)">
          <view class="batch-top">
            <text class="batch-no">{{ item.batchNo }}</text>
            <view class="batch-tags">
              <text class="tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
              <text class="tag tag-type" v-if="item.sourceType === 'LEGACY'">历史</text>
            </view>
          </view>
          <view class="batch-meta">
            <text class="meta-item">{{ item.verifyBy || '-' }}</text>
            <text class="meta-dot">·</text>
            <text class="meta-item">{{ formatTime(item.verifyTime) }}</text>
          </view>
          <view class="batch-amounts">
            <view class="amount-group">
              <text class="amount-label">费用</text>
              <text class="amount-value expense-color">¥{{ money(item.totalExpenseAmount) }}</text>
            </view>
            <view class="amount-group">
              <text class="amount-label">借支</text>
              <text class="amount-value">¥{{ money(item.totalAdvanceAmount) }}</text>
            </view>
            <view class="amount-group" v-if="item.differenceAmount">
              <text class="amount-label">差额</text>
              <text class="amount-value" :class="diffClass(item.differenceAmount)">¥{{ money(item.differenceAmount) }}</text>
            </view>
          </view>
          <view class="batch-reverse" v-if="item.status === 'REVERSED' && item.reverseTime">
            <text class="reverse-text">反核销 {{ formatTime(item.reverseTime) }}</text>
          </view>
        </view>
      </view>

      <view class="load-more" v-if="hasMore">
        <text class="load-more-text">加载中...</text>
      </view>

      <view class="empty" v-if="!batchList.length && !loading">
        <text class="empty-title">暂无核销记录</text>
        <text class="empty-sub">费用核销后将在此展示批次记录</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { requireModulePermission } from '@/utils/permission.js'

export default {
  data() {
    return {
      batchList: [],
      filterStatus: '',
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      refreshing: false
    }
  },
  computed: {
    hasMore() {
      return this.batchList.length < this.total
    }
  },
  onLoad() {
    requireModulePermission('verificationRecord')
    this.getList(true)
  },
  onPullDownRefresh() {
    this.onRefresh()
  },
  methods: {
    getList(reset) {
      if (this.loading) return
      if (reset) {
        this.pageNum = 1
        this.batchList = []
      }
      this.loading = true
      const params = { pageNum: this.pageNum, pageSize: this.pageSize }
      if (this.filterStatus) params.status = this.filterStatus
      request({ url: '/finance/verification-batch/list', method: 'GET', data: params }).then(res => {
        const rows = res.rows || []
        this.total = res.total || 0
        this.batchList = reset ? rows : this.batchList.concat(rows)
        this.loading = false
        this.refreshing = false
      }).catch(() => {
        this.loading = false
        this.refreshing = false
      })
    },
    onRefresh() {
      this.refreshing = true
      this.getList(true)
    },
    loadMore() {
      if (!this.hasMore || this.loading) return
      this.pageNum++
      this.getList(false)
    },
    switchStatus(status) {
      this.filterStatus = status
      this.getList(true)
    },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/verification-record/detail?batchId=' + item.batchId })
    },
    statusText(status) {
      return status === 'VERIFIED' ? '已核销' : status === 'REVERSED' ? '已反核销' : status
    },
    statusClass(status) {
      return status === 'VERIFIED' ? 'tag-ok' : status === 'REVERSED' ? 'tag-muted' : ''
    },
    diffClass(val) {
      const n = Number(val)
      return n > 0 ? 'diff-positive' : n < 0 ? 'diff-negative' : ''
    },
    money(val) {
      const n = Number(val)
      return Number.isNaN(n) ? '0.00' : n.toFixed(2)
    },
    formatTime(t) {
      if (!t) return ''
      return String(t).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #E8EEF5;
}

.hero {
  margin: 24rpx 28rpx 0;
  padding: 32rpx 30rpx;
  background: linear-gradient(135deg, #1E40AF, #3B82F6);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(59, 130, 246, 0.18);
}

.hero-title {
  font-size: 38rpx;
  font-weight: 800;
  color: #FFFFFF;
  display: block;
}

.hero-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.72);
  margin-top: 8rpx;
  display: block;
}

.tab-bar {
  display: flex;
  margin: 24rpx 28rpx 0;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 6rpx;
  box-shadow: 0 2rpx 12rpx rgba(8, 124, 240, 0.06);
}

.tab-item {
  flex: 1;
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #5A6B7F;
  border-radius: 12rpx;
  transition: all 0.2s;
}

.tab-item.active {
  background: #087CF0;
  color: #FFFFFF;
  font-weight: 700;
}

.scroll {
  height: calc(100vh - 320rpx);
  padding: 24rpx 28rpx 46rpx;
  box-sizing: border-box;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.batch-card {
  padding: 24rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(8, 124, 240, 0.05);
}

.batch-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.batch-no {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-tags {
  display: flex;
  gap: 8rpx;
  flex-shrink: 0;
}

.tag {
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 14rpx;
  border-radius: 8rpx;
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.tag-ok {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.tag-muted {
  background: rgba(148, 163, 184, 0.12);
  color: #94A3B8;
}

.tag-type {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.batch-meta {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.meta-item {
  font-size: 22rpx;
  color: #94A3B8;
}

.meta-dot {
  font-size: 22rpx;
  color: #CBD5E1;
  margin: 0 8rpx;
}

.batch-amounts {
  display: flex;
  gap: 24rpx;
}

.amount-group {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.amount-label {
  font-size: 20rpx;
  color: #94A3B8;
}

.amount-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
  font-variant-numeric: tabular-nums;
}

.expense-color {
  color: #F59E0B;
}

.diff-positive {
  color: #EF4444;
}

.diff-negative {
  color: #10B981;
}

.batch-reverse {
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #F1F5F9;
}

.reverse-text {
  font-size: 20rpx;
  color: #94A3B8;
}

.load-more {
  padding: 24rpx 0;
  text-align: center;
}

.load-more-text {
  font-size: 22rpx;
  color: #94A3B8;
}

.empty {
  padding: 80rpx 30rpx;
  text-align: center;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A2332;
  display: block;
}

.empty-sub {
  font-size: 24rpx;
  color: #94A3B8;
  margin-top: 12rpx;
  display: block;
}
</style>
