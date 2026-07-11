<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">增长动作</text>
      <text class="hero-sub">待执行任务 · 候选会员 · 执行反馈</text>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

      <!-- 概览卡片 -->
      <view class="section-card" v-if="dashboard">
        <view class="section-header">
          <view class="section-dot" style="background:#0EA5E9"></view>
          <text class="section-title">动作概览</text>
        </view>
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value warning">{{ dashboard.pendingActionCount || 0 }}</text>
            <text class="stat-label">待执行动作</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ dashboard.pendingMemberCount || 0 }}</text>
            <text class="stat-label">待执行会员</text>
          </view>
          <view class="stat-item">
            <text class="stat-value success">{{ dashboard.executedMemberCount || 0 }}</text>
            <text class="stat-label">已执行会员</text>
          </view>
          <view class="stat-item">
            <text class="stat-value primary">{{ dashboard.effectRate || 0 }}%</text>
            <text class="stat-label">有效率</text>
          </view>
        </view>
        <view class="meta-row" v-if="dashboard.topSegmentType">
          <text class="meta-label">重点分层</text>
          <text class="meta-value">{{ dashboard.topSegmentType }}</text>
        </view>
        <view class="meta-row" v-if="dashboard.pressureLevel">
          <text class="meta-label">压力等级</text>
          <text class="meta-value">{{ dashboard.pressureLevel }}</text>
        </view>
      </view>

      <!-- 动作任务列表 -->
      <view class="section-card">
        <view class="section-header">
          <view class="section-dot" style="background:#2A6F97"></view>
          <text class="section-title">任务列表</text>
          <text class="section-count">{{ recentActions.length }} 条</text>
        </view>
        <view class="list" v-if="recentActions.length">
          <view class="action-card" v-for="item in recentActions" :key="item.actionId" @tap="openDetail(item)">
            <view class="action-top">
              <text class="action-title">{{ item.actionTitle || '未命名动作' }}</text>
              <text class="action-status" :class="actionStatusClass(item.status)">{{ actionStatusText(item.status) }}</text>
            </view>
            <view class="action-meta">
              <text class="action-meta-item" v-if="item.actionType">{{ item.actionType }}</text>
              <text class="action-meta-item" v-if="item.pressureLevel">压力 {{ item.pressureLevel }}</text>
              <text class="action-meta-item">{{ item.executedCount || 0 }}/{{ item.candidateCount || 0 }} 已执行</text>
            </view>
            <view class="action-footer">
              <text class="action-time">{{ formatTime(item.createTime) }}</text>
              <text class="action-arrow">查看详情 ›</text>
            </view>
          </view>
        </view>
        <view class="empty-inline" v-else>
          <text class="empty-inline-text">暂无增长动作任务</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="!dashboard && !recentActions.length && !loading">
        <text class="empty-title">暂无增长动作数据</text>
        <text class="empty-sub">请确认账号已分配会员模块权限</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'

export default {
  data() {
    return {
      dashboard: null,
      loading: false,
      refreshing: false
    }
  },
  computed: {
    recentActions() {
      return this.dashboard?.recentActions || []
    }
  },
  onShow() {
    this.loadDashboard()
  },
  methods: {
    async loadDashboard() {
      this.loading = true
      try {
        const res = await request({
          url: '/member/growth-action/dashboard',
          method: 'POST',
          data: { windowDays: 30 }
        })
        this.dashboard = res.data || res || null
      } catch (e) {
        console.log('growth action dashboard load failed', e)
        this.dashboard = null
      } finally {
        this.loading = false
      }
    },
    onRefresh() {
      this.refreshing = true
      this.loadDashboard().finally(() => {
        this.refreshing = false
      })
    },
    openDetail(item) {
      if (!item || !item.actionId) return
      uni.navigateTo({
        url: '/pages/member/actionDetail?actionId=' + item.actionId
      })
    },
    actionStatusText(val) {
      if (val === '0') return '待执行'
      if (val === '1') return '执行中'
      if (val === '2') return '已完成'
      if (val === '3') return '已关闭'
      return val || '未知'
    },
    actionStatusClass(val) {
      if (val === '0') return 'warning'
      if (val === '1') return 'primary'
      if (val === '2') return 'success'
      if (val === '3') return 'muted'
      return ''
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
  background: linear-gradient(135deg, #0C4A6E, #0EA5E9);
  border-radius: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(14, 165, 233, 0.18);
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
  font-size: 30rpx;
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

.meta-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
  padding: 12rpx 16rpx;
  background: #F8FBFD;
  border-radius: 12rpx;
}

.meta-label {
  font-size: 22rpx;
  color: #94A3B8;
}

.meta-value {
  font-size: 22rpx;
  font-weight: 600;
  color: #1A2332;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.action-card {
  padding: 22rpx 24rpx;
  background: #F8FBFD;
  border-radius: 16rpx;
  border: 1rpx solid rgba(42, 111, 151, 0.06);
}

.action-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.action-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-status {
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: rgba(148, 163, 184, 0.12);
  color: #64748B;
  flex-shrink: 0;
}

.action-status.warning {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.action-status.primary {
  background: rgba(42, 111, 151, 0.1);
  color: #2A6F97;
}

.action-status.success {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.action-status.muted {
  background: rgba(148, 163, 184, 0.12);
  color: #94A3B8;
}

.action-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 16rpx;
  margin-bottom: 12rpx;
}

.action-meta-item {
  font-size: 22rpx;
  color: #5A6B7F;
  padding: 2rpx 10rpx;
  background: rgba(148, 163, 184, 0.08);
  border-radius: 8rpx;
}

.action-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.action-time {
  font-size: 20rpx;
  color: #94A3B8;
}

.action-arrow {
  font-size: 22rpx;
  color: #2A6F97;
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
