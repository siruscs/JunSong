<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">积分运营</text>
      <text class="hero-sub">积分流水 · 待领取兑换</text>
    </view>

    <!-- 切换 Tab -->
    <view class="tab-bar">
      <view class="tab-item" :class="{ active: activeTab === 'record' }" @tap="switchTab('record')">
        <text>积分流水</text>
      </view>
      <view class="tab-item" :class="{ active: activeTab === 'exchange' }" @tap="switchTab('exchange')">
        <text>待领取兑换</text>
        <view class="tab-badge" v-if="pendingExchangeCount > 0">{{ pendingExchangeCount }}</view>
      </view>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh" @scrolltolower="loadMore">

      <!-- 积分流水列表 -->
      <view v-if="activeTab === 'record'">
        <view class="list" v-if="recordList.length">
          <view class="record-card" v-for="(item, idx) in recordList" :key="idx">
            <view class="record-main">
              <text class="record-title">{{ item.changeType || '积分变动' }}</text>
              <text class="record-sub">{{ item.memberName || '-' }} · {{ formatTime(item.createTime) }}</text>
              <text class="record-remark" v-if="item.remark">{{ item.remark }}</text>
            </view>
            <view class="record-side">
              <text class="record-points" :class="pointsClass(item.changePoints)">{{ pointsText(item.changePoints) }}</text>
              <text class="record-balance" v-if="item.balancePoints !== undefined">余额 {{ item.balancePoints }}</text>
            </view>
          </view>
        </view>
        <view class="empty-inline" v-else-if="!loading">
          <text class="empty-inline-text">暂无积分流水</text>
        </view>
        <view class="load-more" v-if="recordHasMore">
          <text class="load-more-text">加载中...</text>
        </view>
      </view>

      <!-- 待领取兑换列表 -->
      <view v-if="activeTab === 'exchange'">
        <view class="list" v-if="exchangeList.length">
          <view class="exchange-card" v-for="(item, idx) in exchangeList" :key="idx">
            <view class="exchange-top">
              <text class="exchange-title">{{ item.goodsName || item.exchangeNo || '兑换记录' }}</text>
              <text class="exchange-status" :class="exchangeStatusClass(item.status)">{{ exchangeStatusText(item.status) }}</text>
            </view>
            <view class="exchange-meta">
              <text class="exchange-member" v-if="item.memberName">{{ item.memberName }}</text>
              <text class="exchange-points" v-if="item.pointsCost">消耗 {{ item.pointsCost }} 积分</text>
              <text class="exchange-qty" v-if="item.quantity">×{{ item.quantity }}</text>
            </view>
            <view class="exchange-footer">
              <text class="exchange-time">{{ formatTime(item.createTime) }}</text>
              <button class="claim-btn" v-if="canClaim(item)" @tap="openClaim(item)">领取</button>
            </view>
          </view>
        </view>
        <view class="empty-inline" v-else-if="!loading">
          <text class="empty-inline-text">暂无待领取兑换</text>
        </view>
        <view class="load-more" v-if="exchangeHasMore">
          <text class="load-more-text">加载中...</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="!recordList.length && !exchangeList.length && !loading">
        <text class="empty-title">暂无积分数据</text>
        <text class="empty-sub">请确认账号已分配积分模块权限</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { getMpDashboardOverview } from '@/api/dashboard.js'

const PAGE_SIZE = 20

export default {
  data() {
    return {
      activeTab: 'record',
      overview: null,
      recordList: [],
      exchangeList: [],
      recordPage: 1,
      exchangePage: 1,
      recordHasMore: false,
      exchangeHasMore: false,
      loading: false,
      refreshing: false
    }
  },
  computed: {
    pendingExchangeCount() {
      return this.overview?.points?.pendingExchangeCount || 0
    }
  },
  onShow() {
    this.loadOverview()
    if (this.activeTab === 'record' && !this.recordList.length) {
      this.loadRecordList(true)
    } else if (this.activeTab === 'exchange' && !this.exchangeList.length) {
      this.loadExchangeList(true)
    }
  },
  methods: {
    switchTab(tab) {
      if (this.activeTab === tab) return
      this.activeTab = tab
      if (tab === 'record' && !this.recordList.length) {
        this.loadRecordList(true)
      } else if (tab === 'exchange' && !this.exchangeList.length) {
        this.loadExchangeList(true)
      }
    },
    async loadOverview() {
      try {
        const res = await getMpDashboardOverview()
        const data = res.data || res
        if (data && typeof data === 'object') {
          this.overview = data
        }
      } catch (e) {
        console.log('overview load failed', e)
      }
    },
    async loadRecordList(reset = false) {
      if (this.loading) return
      this.loading = true
      if (reset) {
        this.recordPage = 1
        this.recordList = []
      }
      try {
        const res = await request({
          url: '/member/pointsRecord/list',
          method: 'GET',
          data: { pageNum: this.recordPage, pageSize: PAGE_SIZE }
        })
        const data = res.data || res || {}
        const rows = data.rows || []
        this.recordList = reset ? rows : this.recordList.concat(rows)
        const total = Number(data.total) || 0
        this.recordHasMore = this.recordList.length < total
        if (this.recordHasMore) this.recordPage++
      } catch (e) {
        console.log('points record list load failed', e)
      } finally {
        this.loading = false
      }
    },
    async loadExchangeList(reset = false) {
      if (this.loading) return
      this.loading = true
      if (reset) {
        this.exchangePage = 1
        this.exchangeList = []
      }
      try {
        const res = await request({
          url: '/member/pointsExchange/list',
          method: 'GET',
          data: { pageNum: this.exchangePage, pageSize: PAGE_SIZE, status: '0' }
        })
        const data = res.data || res || {}
        const rows = data.rows || []
        this.exchangeList = reset ? rows : this.exchangeList.concat(rows)
        const total = Number(data.total) || 0
        this.exchangeHasMore = this.exchangeList.length < total
        if (this.exchangeHasMore) this.exchangePage++
      } catch (e) {
        console.log('points exchange list load failed', e)
      } finally {
        this.loading = false
      }
    },
    loadMore() {
      if (this.activeTab === 'record' && this.recordHasMore && !this.loading) {
        this.loadRecordList(false)
      } else if (this.activeTab === 'exchange' && this.exchangeHasMore && !this.loading) {
        this.loadExchangeList(false)
      }
    },
    onRefresh() {
      this.refreshing = true
      const tasks = [this.loadOverview()]
      if (this.activeTab === 'record') {
        tasks.push(this.loadRecordList(true))
      } else {
        tasks.push(this.loadExchangeList(true))
      }
      Promise.all(tasks).finally(() => {
        this.refreshing = false
      })
    },
    pointsText(val) {
      const n = Number(val) || 0
      if (n >= 0) return '+' + n
      return String(n)
    },
    pointsClass(val) {
      const n = Number(val) || 0
      return n >= 0 ? 'plus' : 'minus'
    },
    exchangeStatusText(val) {
      if (val === '0') return '待领取'
      if (val === '1') return '已领取'
      if (val === '2') return '已取消'
      return val || '未知'
    },
    exchangeStatusClass(val) {
      if (val === '0') return 'warning'
      if (val === '1') return 'success'
      if (val === '2') return 'muted'
      return ''
    },
    canClaim(item) {
      return item.status === '0' || item.status === 0
    },
    openClaim(item) {
      // 跳转到积分兑换列表页处理领取
      uni.navigateTo({
        url: '/pages/list/index?module=pointsExchange'
      })
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
  background: #F0F4F8;
}

.hero {
  margin: 24rpx 28rpx 0;
  padding: 32rpx 30rpx;
  background: linear-gradient(135deg, #92400E, #F59E0B);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(245, 158, 11, 0.18);
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
  box-shadow: 0 2rpx 12rpx rgba(42, 111, 151, 0.06);
}

.tab-item {
  flex: 1;
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #5A6B7F;
  border-radius: 12rpx;
  position: relative;
  transition: all 0.2s;
}

.tab-item.active {
  background: #2A6F97;
  color: #FFFFFF;
  font-weight: 700;
}

.tab-badge {
  position: absolute;
  top: 6rpx;
  right: 24rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: #EF4444;
  color: #FFFFFF;
  font-size: 20rpx;
  font-weight: 700;
  text-align: center;
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

.record-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 22rpx 24rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(42, 111, 151, 0.05);
  gap: 16rpx;
}

.record-main {
  flex: 1;
  min-width: 0;
}

.record-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
  display: block;
}

.record-sub {
  font-size: 22rpx;
  color: #94A3B8;
  margin-top: 4rpx;
  display: block;
}

.record-remark {
  font-size: 22rpx;
  color: #5A6B7F;
  margin-top: 8rpx;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
  flex-shrink: 0;
}

.record-points {
  font-size: 30rpx;
  font-weight: 800;
}

.record-points.plus {
  color: #10B981;
}

.record-points.minus {
  color: #EF4444;
}

.record-balance {
  font-size: 20rpx;
  color: #94A3B8;
}

.exchange-card {
  padding: 22rpx 24rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(42, 111, 151, 0.05);
}

.exchange-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.exchange-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exchange-status {
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: rgba(148, 163, 184, 0.12);
  color: #64748B;
  flex-shrink: 0;
}

.exchange-status.warning {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.exchange-status.success {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.exchange-status.muted {
  background: rgba(148, 163, 184, 0.12);
  color: #94A3B8;
}

.exchange-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 16rpx;
  margin-bottom: 12rpx;
}

.exchange-member,
.exchange-points,
.exchange-qty {
  font-size: 22rpx;
  color: #5A6B7F;
  padding: 2rpx 10rpx;
  background: rgba(148, 163, 184, 0.08);
  border-radius: 8rpx;
}

.exchange-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.exchange-time {
  font-size: 20rpx;
  color: #94A3B8;
}

.claim-btn {
  font-size: 22rpx;
  font-weight: 600;
  color: #FFFFFF;
  background: #F59E0B;
  border-radius: 999rpx;
  padding: 4rpx 24rpx;
  line-height: 40rpx;
  height: 48rpx;
  min-height: 48rpx;
  border: none;
}

.claim-btn::after {
  border: none;
}

.empty-inline {
  padding: 60rpx 0;
  text-align: center;
}

.empty-inline-text {
  font-size: 24rpx;
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
