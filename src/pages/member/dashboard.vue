<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="hero-title">会员运营看板</text>
          <text class="hero-sub">会员增长 · 成长体系 · 分层洞察</text>
        </view>
        <view class="hero-refresh" @tap="loadOverview">
          <text class="hero-refresh-text">刷新</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

      <!-- 会员增长 -->
      <view class="section-card" v-if="overviewMember">
        <view class="section-header">
          <view class="section-dot" style="background:#10B981"></view>
          <text class="section-title">会员增长</text>
        </view>
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value primary">{{ overviewMember.todayMembers || 0 }}</text>
            <text class="stat-label">今日新增</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ overviewMember.totalMembers || 0 }}</text>
            <text class="stat-label">总会员数</text>
          </view>
          <view class="stat-item">
            <text class="stat-value success">{{ overviewMember.activeMembers || 0 }}</text>
            <text class="stat-label">活跃会员</text>
          </view>
          <view class="stat-item">
            <text class="stat-value warning">{{ overviewMember.silentMembers || 0 }}</text>
            <text class="stat-label">沉默会员</text>
          </view>
        </view>
      </view>

      <!-- 成长体系 -->
      <view class="section-card" v-if="overviewGrowth">
        <view class="section-header">
          <view class="section-dot" style="background:#8B5CF6"></view>
          <text class="section-title">成长体系</text>
          <text class="section-link" @tap="goGrowth">查看 ›</text>
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

      <!-- 分层洞察 -->
      <view class="section-card" v-if="segmentDistribution.length">
        <view class="section-header">
          <view class="section-dot" style="background:#EC4899"></view>
          <text class="section-title">分层洞察</text>
        </view>
        <view class="stats-row">
          <view class="stat-item" v-for="(item, idx) in segmentDistribution" :key="idx">
            <text class="stat-value" :class="segmentTone(item.name)">{{ item.count }}</text>
            <text class="stat-label">{{ item.name }}</text>
          </view>
        </view>
      </view>

      <!-- 增长动作 -->
      <view class="section-card" v-if="overviewGrowthActions">
        <view class="section-header">
          <view class="section-dot" style="background:#0EA5E9"></view>
          <text class="section-title">增长动作</text>
          <text class="section-link" @tap="goActions">查看 ›</text>
        </view>
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value warning">{{ overviewGrowthActions.pending }}</text>
            <text class="stat-label">待执行动作</text>
          </view>
          <view class="stat-item">
            <text class="stat-value success">{{ overviewGrowthActions.completed }}</text>
            <text class="stat-label">已完成动作</text>
          </view>
          <view class="stat-item">
            <text class="stat-value primary">{{ overviewGrowthActions.effectRate }}%</text>
            <text class="stat-label">完成率</text>
          </view>
        </view>
      </view>

      <!-- 积分运营 -->
      <view class="section-card" v-if="overviewPoints">
        <view class="section-header">
          <view class="section-dot" style="background:#F59E0B"></view>
          <text class="section-title">积分运营</text>
          <text class="section-link" @tap="goPoints">查看 ›</text>
        </view>
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value warning">{{ overviewPoints.pendingExchangeCount || 0 }}</text>
            <text class="stat-label">待领取兑换</text>
          </view>
          <view class="stat-item">
            <text class="stat-value success">{{ overviewPoints.todayPointsIssued || 0 }}</text>
            <text class="stat-label">今日发放积分</text>
          </view>
          <view class="stat-item">
            <text class="stat-value primary">{{ overviewPoints.todayPointsConsumed || 0 }}</text>
            <text class="stat-label">今日消耗积分</text>
          </view>
        </view>
      </view>

      <!-- 活动表现 -->
      <view class="section-card" v-if="overviewActivity">
        <view class="section-header">
          <view class="section-dot" style="background:#F97316"></view>
          <text class="section-title">活动表现</text>
        </view>
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value primary">{{ overviewActivity.activeSeckillCount || 0 }}</text>
            <text class="stat-label">进行中活动</text>
          </view>
          <view class="stat-item">
            <text class="stat-value success">{{ overviewActivity.todayActivityMembers || 0 }}</text>
            <text class="stat-label">今日参与人数</text>
          </view>
          <view class="stat-item">
            <text class="stat-value warning">{{ fmtMoney(overviewActivity.todayActivityAmount) }}</text>
            <text class="stat-label">今日活动金额</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="!overview && !loading">
        <text class="empty-title">暂无会员运营数据</text>
        <text class="empty-sub">请确认账号已分配会员模块权限</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getMpDashboardOverview } from '@/api/dashboard.js'

export default {
  data() {
    return {
      overview: null,
      loading: false,
      refreshing: false,
      barColors: ['#2A6F97', '#3B82F6', '#8B5CF6', '#10B981', '#F59E0B', '#EC4899']
    }
  },
  computed: {
    overviewMember() {
      return this.overview?.member || null
    },
    overviewGrowth() {
      return this.overview?.growth || null
    },
    overviewGrowthActions() {
      const g = this.overviewGrowth
      if (!g) return null
      return {
        pending: g.pendingGrowthActions || 0,
        completed: g.completedGrowthActions || 0,
        effectRate: g.growthActionEffectRate || 0
      }
    },
    overviewPoints() {
      return this.overview?.points || null
    },
    overviewActivity() {
      return this.overview?.activity || null
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
    },
    segmentDistribution() {
      const dist = this.overview?.segment?.distribution
      if (!Array.isArray(dist) || !dist.length) return []
      return dist.map(d => ({
        name: d.segmentName || '未知',
        count: Number(d.count) || 0
      }))
    }
  },
  onShow() {
    this.loadOverview()
  },
  methods: {
    async loadOverview() {
      this.loading = true
      try {
        const res = await getMpDashboardOverview()
        const data = res.data || res
        if (data && typeof data === 'object') {
          this.overview = data
        }
      } catch (e) {
        console.log('overview load failed', e)
      } finally {
        this.loading = false
      }
    },
    onRefresh() {
      this.refreshing = true
      this.loadOverview().finally(() => {
        this.refreshing = false
      })
    },
    fmtMoney(val) {
      if (!val && val !== 0) return '0'
      const n = Number(val)
      if (isNaN(n)) return '0'
      if (n >= 10000) return (n / 10000).toFixed(1) + '万'
      return n.toFixed(n % 1 === 0 ? 0 : 2)
    },
    segmentTone(name) {
      if (!name) return ''
      if (name.indexOf('高价值') >= 0) return 'primary'
      if (name.indexOf('活跃') >= 0) return 'success'
      if (name.indexOf('沉默') >= 0 || name.indexOf('待唤醒') >= 0) return 'warning'
      return ''
    },
    goGrowth() {
      uni.navigateTo({ url: '/pages/member/growth' })
    },
    goActions() {
      uni.navigateTo({ url: '/pages/member/actions' })
    },
    goPoints() {
      uni.navigateTo({ url: '/pages/member/points' })
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
  background: linear-gradient(135deg, #173B57, #2A6F97);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(42, 111, 151, 0.18);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.hero-refresh {
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
}

.hero-refresh-text {
  font-size: 24rpx;
  color: #FFFFFF;
  font-weight: 600;
}

.scroll {
  height: calc(100vh - 220rpx);
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

.section-link {
  font-size: 22rpx;
  color: #94A3B8;
  font-weight: 500;
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
.stat-value.warning { color: #F59E0B; }

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
