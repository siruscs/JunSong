<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">成长体系</text>
      <text class="hero-sub">等级分布 · 签到记录 · 成长数据</text>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

      <!-- 成长体系概览 -->
      <view class="section-card" v-if="overviewGrowth">
        <view class="section-header">
          <view class="section-dot" style="background:#8B5CF6"></view>
          <text class="section-title">成长概览</text>
        </view>
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value primary">{{ overviewGrowth.todaySignInCount || 0 }}</text>
            <text class="stat-label">今日签到</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ overviewGrowth.avgGrowthValue || 0 }}</text>
            <text class="stat-label">平均成长值</text>
          </view>
          <view class="stat-item">
            <text class="stat-value success">{{ overviewGrowth.completedGrowthActions || 0 }}</text>
            <text class="stat-label">已完成动作</text>
          </view>
        </view>
      </view>

      <!-- 等级分布 -->
      <view class="section-card" v-if="levelDistribution.length">
        <view class="section-header">
          <view class="section-dot" style="background:#3B82F6"></view>
          <text class="section-title">会员等级分布</text>
        </view>
        <view class="bar-chart">
          <view class="bar-row" v-for="(item, idx) in levelDistribution" :key="idx">
            <text class="bar-name">{{ item.name }}</text>
            <view class="bar-track">
              <view class="bar-fill" :style="{ width: item.percent + '%', background: barColors[idx % barColors.length] }"></view>
            </view>
            <text class="bar-val">{{ item.count }}</text>
          </view>
        </view>
      </view>

      <!-- 签到记录 -->
      <view class="section-card">
        <view class="section-header">
          <view class="section-dot" style="background:#10B981"></view>
          <text class="section-title">签到记录</text>
          <text class="section-count">{{ signInList.length }} 条</text>
        </view>
        <view class="list" v-if="signInList.length">
          <view class="list-item" v-for="(item, idx) in signInList" :key="idx">
            <view class="list-item-main">
              <text class="list-item-title">{{ item.memberName || '未知会员' }}</text>
              <text class="list-item-sub">签到日期 {{ item.signDate || '-' }}</text>
            </view>
            <view class="list-item-side">
              <text class="list-item-tag" :class="signInResultClass(item.signResult)">{{ signInResultText(item.signResult) }}</text>
              <text class="list-item-meta" v-if="item.pointsAwarded">+{{ item.pointsAwarded }}积分</text>
            </view>
          </view>
        </view>
        <view class="empty-inline" v-else>
          <text class="empty-inline-text">暂无签到记录</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="!overviewGrowth && !levelDistribution.length && !signInList.length && !loading">
        <text class="empty-title">暂无成长数据</text>
        <text class="empty-sub">请确认账号已分配会员模块权限</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { getMpDashboardOverview } from '@/api/dashboard.js'

export default {
  data() {
    return {
      overview: null,
      signInList: [],
      loading: false,
      refreshing: false,
      barColors: ['#2A6F97', '#3B82F6', '#8B5CF6', '#10B981', '#F59E0B', '#EC4899']
    }
  },
  computed: {
    overviewGrowth() {
      return this.overview?.growth || null
    },
    levelDistribution() {
      const dist = this.overview?.level?.distribution
      if (!Array.isArray(dist) || !dist.length) return []
      const maxCount = Math.max(...dist.map(d => Number(d.count) || 0), 1)
      return dist.map(d => ({
        name: d.levelName || '未分级',
        count: Number(d.count) || 0,
        percent: Math.round(((Number(d.count) || 0) / maxCount) * 100)
      }))
    }
  },
  onShow() {
    this.loadAll()
  },
  methods: {
    async loadAll() {
      this.loading = true
      await Promise.all([this.loadOverview(), this.loadSignInList()])
      this.loading = false
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
    async loadSignInList() {
      try {
        const res = await request({
          url: '/member/signIn/list',
          method: 'GET',
          data: { pageNum: 1, pageSize: 20 }
        })
        const data = res.data || res || {}
        this.signInList = data.rows || data || []
      } catch (e) {
        console.log('signIn list load failed', e)
        this.signInList = []
      }
    },
    onRefresh() {
      this.refreshing = true
      this.loadAll().finally(() => {
        this.refreshing = false
      })
    },
    signInResultText(val) {
      if (val === '1' || val === 1) return '成功'
      if (val === '0' || val === 0) return '未生效'
      return val || '签到'
    },
    signInResultClass(val) {
      if (val === '1' || val === 1) return 'success'
      if (val === '0' || val === 0) return 'warning'
      return ''
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
  background: linear-gradient(135deg, #4C1D95, #8B5CF6);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(139, 92, 246, 0.18);
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

.scroll {
  height: calc(100vh - 200rpx);
  padding: 24rpx 28rpx 46rpx;
  box-sizing: border-box;
}

.section-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.section-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
  flex: 1;
}

.section-count {
  font-size: 22rpx;
  color: #94A3B8;
}

.stats-row {
  display: flex;
  gap: 12rpx;
}

.stat-item {
  flex: 1;
  min-width: 0;
  background: #F8FBFD;
  border-radius: 16rpx;
  padding: 20rpx 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A2332;
}

.stat-value.primary { color: #2A6F97; }
.stat-value.success { color: #10B981; }

.stat-label {
  font-size: 20rpx;
  color: #94A3B8;
  margin-top: 6rpx;
}

.bar-chart {
  margin-top: 8rpx;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 14rpx;
}

.bar-name {
  font-size: 22rpx;
  color: #5A6B7F;
  width: 120rpx;
  flex-shrink: 0;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  flex: 1;
  min-width: 0;
  height: 24rpx;
  background: #F0F4F8;
  border-radius: 12rpx;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 12rpx;
  min-width: 8rpx;
}

.bar-val {
  font-size: 22rpx;
  font-weight: 600;
  color: #1A2332;
  width: 60rpx;
  text-align: right;
  flex-shrink: 0;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  background: #F8FBFD;
  border-radius: 16rpx;
  gap: 16rpx;
}

.list-item-main {
  flex: 1;
  min-width: 0;
}

.list-item-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1A2332;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-item-sub {
  font-size: 22rpx;
  color: #94A3B8;
  margin-top: 4rpx;
  display: block;
}

.list-item-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
  flex-shrink: 0;
}

.list-item-tag {
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: rgba(148, 163, 184, 0.12);
  color: #64748B;
}

.list-item-tag.success {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.list-item-tag.warning {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.list-item-meta {
  font-size: 20rpx;
  color: #10B981;
  font-weight: 600;
}

.empty-inline {
  padding: 32rpx 0;
  text-align: center;
}

.empty-inline-text {
  font-size: 24rpx;
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
