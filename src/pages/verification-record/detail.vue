<template>
  <view class="page">
    <view v-if="loading" class="loading-state">
      <text class="loading-text">加载中...</text>
    </view>

    <template v-if="!loading && batch">
      <!-- 批次信息卡片 -->
      <view class="info-card">
        <view class="info-header">
          <text class="info-title">{{ batch.batchNo }}</text>
          <text class="status-tag" :class="statusClass(batch.status)">{{ statusText(batch.status) }}</text>
        </view>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">核销人</text>
            <text class="info-value">{{ batch.verifyBy || '-' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">核销时间</text>
            <text class="info-value">{{ formatTime(batch.verifyTime) }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">类型</text>
            <text class="info-value">{{ batch.sourceType === 'LEGACY' ? '历史' : '正常' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">批次ID</text>
            <text class="info-value">{{ batch.batchId }}</text>
          </view>
        </view>
        <view class="amount-row">
          <view class="amount-block">
            <text class="amount-label">费用合计</text>
            <text class="amount-value expense-color">¥{{ money(batch.totalExpenseAmount) }}</text>
          </view>
          <view class="amount-block">
            <text class="amount-label">借支合计</text>
            <text class="amount-value">¥{{ money(batch.totalAdvanceAmount) }}</text>
          </view>
          <view class="amount-block">
            <text class="amount-label">差额</text>
            <text class="amount-value" :class="diffClass(batch.differenceAmount)">¥{{ money(batch.differenceAmount) }}</text>
          </view>
        </view>
        <!-- 反核销信息 -->
        <view class="reverse-section" v-if="batch.status === 'REVERSED'">
          <view class="reverse-title">反核销信息</view>
          <view class="info-grid">
            <view class="info-item">
              <text class="info-label">反核销人</text>
              <text class="info-value">{{ batch.reverseBy || '-' }}</text>
            </view>
            <view class="info-item">
              <text class="info-label">反核销时间</text>
              <text class="info-value">{{ formatTime(batch.reverseTime) }}</text>
            </view>
          </view>
          <view class="reverse-reason" v-if="batch.reverseReason">
            <text class="info-label">原因</text>
            <text class="reason-text">{{ batch.reverseReason }}</text>
          </view>
        </view>
      </view>

      <!-- 费用明细 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">费用明细</text>
          <text class="section-count">{{ expenseDetails.length }}笔</text>
        </view>
        <view class="detail-list" v-if="expenseDetails.length">
          <view class="detail-card" v-for="(item, idx) in expenseDetails" :key="'e' + idx">
            <view class="detail-top">
              <text class="detail-no">{{ item.expenseNo || `费用 #${item.expenseId}` }}</text>
              <text class="detail-amount expense-color">¥{{ money(item.expenseAmount) }}</text>
            </view>
            <view class="detail-meta">
              <text class="meta-tag" v-if="item.expenseType">{{ item.expenseType }}</text>
              <text class="meta-text">{{ item.expenseDate || '' }}</text>
            </view>
            <text class="detail-content" v-if="item.expenseContent">{{ item.expenseContent }}</text>
          </view>
        </view>
        <view class="empty-inline" v-else>
          <text class="empty-inline-text">暂无费用明细</text>
        </view>
      </view>

      <!-- 借支明细 -->
      <view class="section" v-if="advanceDetails.length">
        <view class="section-header">
          <text class="section-title">借支明细</text>
          <text class="section-count">{{ advanceDetails.length }}笔</text>
        </view>
        <view class="detail-list">
          <view class="detail-card" v-for="(item, idx) in advanceDetails" :key="'a' + idx">
            <view class="detail-top">
              <text class="detail-no">{{ item.advanceNo || `借支 #${item.advanceId}` }}</text>
              <text class="detail-amount">¥{{ money(item.advanceAmount) }}</text>
            </view>
            <view class="detail-meta">
              <text class="meta-tag" :class="relationClass(item.relationType)">{{ relationText(item.relationType) }}</text>
              <text class="meta-tag generated" v-if="item.generatedFlag === '1'">生成</text>
              <text class="meta-text">{{ item.advanceDate || '' }}</text>
            </view>
            <text class="detail-content" v-if="item.purpose">{{ item.purpose }}</text>
          </view>
        </view>
      </view>
    </template>

    <view class="empty" v-if="!loading && !batch">
      <text class="empty-title">加载失败</text>
      <text class="empty-sub">无法获取核销批次详情</text>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { requireModulePermission } from '@/utils/permission.js'

export default {
  data() {
    return {
      batchId: null,
      batch: null,
      expenseDetails: [],
      advanceDetails: [],
      loading: true
    }
  },
  onLoad(options) {
    requireModulePermission('verificationRecord')
    this.batchId = options.batchId
    if (this.batchId) {
      this.getDetail()
    } else {
      this.loading = false
    }
  },
  methods: {
    getDetail() {
      this.loading = true
      request({ url: '/finance/verification-batch/' + this.batchId, method: 'GET' }).then(res => {
        const data = res.data || {}
        this.batch = data.batch || data
        this.expenseDetails = data.expenseDetails || []
        this.advanceDetails = data.advanceDetails || []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    statusText(status) {
      return status === 'VERIFIED' ? '已核销' : status === 'REVERSED' ? '已反核销' : status
    },
    statusClass(status) {
      return status === 'VERIFIED' ? 'status-ok' : status === 'REVERSED' ? 'status-muted' : ''
    },
    diffClass(val) {
      const n = Number(val)
      return n > 0 ? 'diff-positive' : n < 0 ? 'diff-negative' : ''
    },
    relationText(type) {
      const map = { SOURCE: '原始借支', SUPPLEMENT: '补款', SURPLUS: '节余' }
      return map[type] || type || ''
    },
    relationClass(type) {
      return type === 'SUPPLEMENT' ? 'tag-warn' : type === 'SURPLUS' ? 'tag-ok' : ''
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
  background: #F0F4F8;
  padding: 24rpx 28rpx 46rpx;
  box-sizing: border-box;
}

.loading-state {
  padding: 80rpx 0;
  text-align: center;
}

.loading-text {
  font-size: 26rpx;
  color: #94A3B8;
}

/* 批次信息卡片 */
.info-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
  margin-bottom: 24rpx;
}

.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.info-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #1A2332;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 16rpx;
  border-radius: 10rpx;
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
  flex-shrink: 0;
}

.status-ok {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.status-muted {
  background: rgba(148, 163, 184, 0.12);
  color: #94A3B8;
}

.info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.info-item {
  width: calc(50% - 8rpx);
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.info-label {
  font-size: 20rpx;
  color: #94A3B8;
}

.info-value {
  font-size: 24rpx;
  font-weight: 600;
  color: #1A2332;
}

.amount-row {
  display: flex;
  padding-top: 20rpx;
  border-top: 1rpx solid #F1F5F9;
}

.amount-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.amount-label {
  font-size: 20rpx;
  color: #94A3B8;
}

.amount-value {
  font-size: 30rpx;
  font-weight: 800;
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

/* 反核销信息 */
.reverse-section {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #F1F5F9;
}

.reverse-title {
  font-size: 24rpx;
  font-weight: 700;
  color: #94A3B8;
  margin-bottom: 12rpx;
}

.reverse-reason {
  margin-top: 8rpx;
}

.reason-text {
  font-size: 24rpx;
  color: #5A6B7F;
  display: block;
  margin-top: 4rpx;
  line-height: 1.6;
}

/* 明细区块 */
.section {
  margin-bottom: 24rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
  padding: 0 4rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
}

.section-count {
  font-size: 22rpx;
  color: #94A3B8;
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.detail-card {
  background: #FFFFFF;
  border-radius: 14rpx;
  padding: 20rpx 22rpx;
  box-shadow: 0 1rpx 8rpx rgba(42, 111, 151, 0.04);
}

.detail-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.detail-no {
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-amount {
  font-size: 28rpx;
  font-weight: 800;
  color: #1A2332;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 6rpx;
}

.meta-tag {
  font-size: 20rpx;
  font-weight: 600;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  background: rgba(148, 163, 184, 0.1);
  color: #64748B;
}

.meta-tag.tag-ok {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.meta-tag.tag-warn {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.meta-tag.generated {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.meta-text {
  font-size: 20rpx;
  color: #94A3B8;
}

.detail-content {
  font-size: 22rpx;
  color: #5A6B7F;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-inline {
  padding: 40rpx 0;
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
