<template>
  <view class="page">
    <view class="summary-card">
      <view class="title">核销汇总</view>
      <view class="row"><text>费用（{{ expenses.length }}笔）</text><text>¥{{ money(expenseTotal) }}</text></view>
      <view class="row"><text>借支（{{ selectedAdvanceIds.length }}笔）</text><text>¥{{ money(advanceTotal) }}</text></view>
      <view class="row difference"><text>差额</text><text>¥{{ money(difference) }}</text></view>
      <view class="explanation">{{ differenceExplanation }}</view>
    </view>

    <view class="section-title">待核销费用</view>
    <view class="card" v-for="item in expenses" :key="item.expenseId">
      <text>{{ item.expenseContent || item.expenseNo || `费用 #${item.expenseId}` }}</text>
      <text>¥{{ money(item.expenseAmount) }}</text>
    </view>

    <view class="section-title">选择借支单</view>
    <view class="notice" v-if="advancePermissionNotice">{{ advancePermissionNotice }}</view>
    <view class="empty" v-if="!advancePermissionNotice && !loading && advances.length === 0">当前门店暂无未核销借支单</view>
    <view class="card selectable" :class="{ selected: isAdvanceSelected(item) }" v-for="item in advances" :key="item.advanceId" @tap="toggleAdvance(item)">
      <text class="check">{{ isAdvanceSelected(item) ? '✓' : '' }}</text>
      <view class="grow"><text>{{ item.advanceNo || `借支 #${item.advanceId}` }}</text><text class="meta">{{ item.borrower || '' }} {{ item.advanceDate || '' }}</text></view>
      <text>¥{{ money(item.advanceAmount) }}</text>
    </view>

    <button class="submit" :disabled="loading || submitting || !verificationReady" @tap="submitVerify">
      {{ submitting ? '提交中…' : '确认核销' }}
    </button>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { hasActionPermission } from '@/utils/permission.js'

export default {
  data() {
    return {
      expenseIds: [],
      expenses: [],
      advances: [],
      selectedAdvanceIds: [],
      requestId: '',
      advancePermissionNotice: '',
      verificationReady: false,
      loading: true,
      submitting: false
    }
  },
  computed: {
    expenseTotal() {
      return this.expenses.reduce((sum, item) => sum + Number(item.expenseAmount || 0), 0)
    },
    advanceTotal() {
      return this.advances.filter((item) => this.selectedAdvanceIds.includes(Number(item.advanceId)))
        .reduce((sum, item) => sum + Number(item.advanceAmount || 0), 0)
    },
    difference() {
      return this.expenseTotal - this.advanceTotal
    },
    differenceExplanation() {
      if (this.difference > 0) return `费用高于借支 ¥${this.money(this.difference)}，核销后形成应补款。`
      if (this.difference < 0) return `借支高于费用 ¥${this.money(Math.abs(this.difference))}，核销后生成未核销节余借支单。`
      return '费用与借支金额相等，无补款或节余。'
    }
  },
  async onLoad(options) {
    if (!hasActionPermission('expense', 'verify')) {
      uni.showToast({ title: '暂无费用核销权限', icon: 'none' })
      return setTimeout(() => uni.navigateBack(), 500)
    }
    this.expenseIds = [...new Set(String(options.expenseIds || '').split(',').map(Number)
      .filter((id) => Number.isSafeInteger(id) && id > 0))]
    if (!this.expenseIds.length) {
      uni.showToast({ title: '费用参数无效', icon: 'none' })
      return setTimeout(() => uni.navigateBack(), 500)
    }
    this.requestId = this.createRequestId()
    await this.loadData()
  },
  methods: {
    money(value) {
      return Number(value || 0).toFixed(2)
    },
    createRequestId() {
      return `mp-verify-${Date.now()}-${Math.random().toString(36).slice(2)}`
    },
    async loadData() {
      this.loading = true
      this.verificationReady = false
      try {
        this.expenses = await Promise.all(this.expenseIds.map(async (id) => {
          const response = await request({ url: `/finance/expense/${id}/verificationCandidate`, method: 'GET' })
          return response.data || response
        }))
        const user = uni.getStorageSync('userInfo') || {}
        const currentDeptId = user.currentDeptId || user.deptId
        const requestedIds = [...this.expenseIds].sort((a, b) => a - b)
        const returnedIds = [...new Set(this.expenses.map((item) => Number(item.expenseId)))].sort((a, b) => a - b)
        if (this.expenses.length !== requestedIds.length || returnedIds.length !== requestedIds.length || returnedIds.some((id, index) => id !== requestedIds[index])) {
          throw new Error('费用数据与请求不一致，请返回重试')
        }
        if (!currentDeptId || this.expenses.some((item) => String(item.deptId) !== String(currentDeptId))) {
          throw new Error('费用不属于当前门店')
        }
        if (this.expenses.some((item) => String(item.status ?? '') !== '0')) {
          throw new Error('选择中包含不可核销费用')
        }
        this.advancePermissionNotice = ''
        if (!hasActionPermission('advance', 'list')) {
          this.advances = []
          this.advancePermissionNotice = '暂无借支单查看权限，可不选择借支单直接核销。'
        } else try {
          const response = await request({ url: '/finance/expense/unverifiedAdvances', method: 'GET' })
          const data = response.data || response
          this.advances = Array.isArray(data) ? data : (data.rows || [])
        } catch (permErr) {
          this.advances = []
          this.advancePermissionNotice = '暂无借支单查看权限，可不选择借支单直接核销。'
        }
        this.verificationReady = true
      } catch (error) {
        uni.showToast({ title: error?.message || error?.msg || '加载核销数据失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    isAdvanceSelected(item) {
      return this.selectedAdvanceIds.includes(Number(item.advanceId))
    },
    toggleAdvance(item) {
      const id = Number(item.advanceId)
      this.selectedAdvanceIds = this.isAdvanceSelected(item)
        ? this.selectedAdvanceIds.filter((value) => value !== id)
        : [...this.selectedAdvanceIds, id]
    },
    async submitVerify() {
      if (this.submitting || !this.verificationReady) return
      this.submitting = true
      try {
        await request({
          url: '/finance/expense/batchVerify',
          method: 'PUT',
          data: { expenseIds: this.expenseIds, advanceIds: this.selectedAdvanceIds, requestId: this.requestId }
        })
        uni.showToast({ title: '核销成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (error) {
        // requestId and selections intentionally remain stable for a safe retry.
        console.error('费用核销失败', error)
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx 28rpx 120rpx;
  background: #F0F4F8;
  color: #1A2332;
  box-sizing: border-box;
}

.summary-card {
  background: linear-gradient(135deg, #173B57 0%, #2A6F97 100%);
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 28rpx;
  color: #FFFFFF;
}

.summary-card .title {
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
  opacity: 0.9;
}

.summary-card .row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
  font-size: 28rpx;
  opacity: 0.95;
}

.summary-card .row:last-of-type {
  border-top: 1rpx solid rgba(255, 255, 255, 0.2);
  margin-top: 12rpx;
  padding-top: 20rpx;
}

.summary-card .difference {
  font-weight: 700;
  font-size: 32rpx;
}

.explanation {
  margin-top: 16rpx;
  padding: 16rpx 20rpx;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12rpx;
  font-size: 24rpx;
  line-height: 1.5;
  opacity: 0.9;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A2332;
  margin: 28rpx 8rpx 16rpx;
}

.notice {
  padding: 20rpx 24rpx;
  background: #FEF3C7;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #92400E;
  margin-bottom: 16rpx;
}

.empty {
  text-align: center;
  padding: 60rpx 0;
  color: #94A3B8;
  font-size: 26rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
}

.card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 16rpx;
  padding: 28rpx 24rpx;
  border-radius: 16rpx;
  background: #FFFFFF;
  box-shadow: 0 2rpx 8rpx rgba(42, 111, 151, 0.04);
}

.card .grow {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8rpx;
  min-width: 0;
}

.card .grow text:first-child {
  font-size: 28rpx;
  font-weight: 500;
  color: #1A2332;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card .meta {
  font-size: 24rpx;
  color: #64748B;
}

.card > text:last-child {
  font-size: 30rpx;
  font-weight: 700;
  color: #2A6F97;
  flex-shrink: 0;
}

.card.selectable {
  cursor: pointer;
  transition: all 0.2s;
}

.card.selectable .check {
  width: 44rpx;
  height: 44rpx;
  line-height: 44rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 700;
  color: transparent;
  border: 2rpx solid #CBD5E1;
  border-radius: 50%;
  flex-shrink: 0;
  transition: all 0.2s;
}

.card.selectable.selected {
  border: 2rpx solid #2A6F97;
  background: #F0F7FA;
}

.card.selectable.selected .check {
  color: #FFFFFF;
  background: #2A6F97;
  border-color: #2A6F97;
}

.submit {
  position: fixed;
  bottom: 40rpx;
  left: 28rpx;
  right: 28rpx;
  height: 88rpx;
  line-height: 88rpx;
  color: #fff;
  background: #2A6F97;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 24rpx rgba(42, 111, 151, 0.3);
}

.submit[disabled] {
  opacity: 0.55;
  box-shadow: none;
}
</style>
