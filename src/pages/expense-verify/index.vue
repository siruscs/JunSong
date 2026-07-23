<template>
  <view class="page">
    <view class="load-state" v-if="loading">正在加载核销数据...</view>
    <view class="load-error" v-else-if="loadError">
      <text class="load-error-title">核销数据加载失败</text>
      <text class="load-error-message">{{ loadError }}</text>
      <button class="retry" @tap="loadData">重新加载</button>
    </view>

    <template v-else>
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
    </template>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { hasActionPermission } from '@/utils/permission.js'
import { isUnknownWriteOutcome } from '@/utils/operationState.js'

export default {
  data() {
    return {
      expenseIds: [],
      expenses: [],
      advances: [],
      selectedAdvanceIds: [],
      requestId: '',
      pendingVerifyPayload: null,
      advancePermissionNotice: '',
      verificationReady: false,
      loading: true,
      loadError: '',
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
      this.loadError = ''
      this.verificationReady = false
      try {
        this.expenses = await Promise.all(this.expenseIds.map(async (id) => {
          const response = await request({ url: `/finance/expense/${id}/verificationCandidate`, method: 'GET', silent: true })
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
        try {
          const response = await request({ url: '/finance/expense/unverifiedAdvances', method: 'GET', silent: true })
          const data = response.data || response
          this.advances = Array.isArray(data) ? data : (data.rows || [])
        } catch (permErr) {
          const permissionDenied = Number(permErr?.code) === 403 || /权限|无权/.test(String(permErr?.msg || permErr?.message || ''))
          if (!permissionDenied) throw permErr
          this.advances = []
          this.advancePermissionNotice = '暂无借支单查看权限，可不选择借支单直接核销。'
        }
        const availableAdvanceIds = new Set(this.advances.map((item) => Number(item.advanceId)))
        this.selectedAdvanceIds = this.selectedAdvanceIds.filter((id) => availableAdvanceIds.has(Number(id)))
        this.verificationReady = true
      } catch (error) {
        this.loadError = error?.message || error?.msg || '加载核销数据失败'
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
      this.pendingVerifyPayload = this.pendingVerifyPayload || {
        expenseIds: [...this.expenseIds],
        advanceIds: [...this.selectedAdvanceIds],
        requestId: this.requestId
      }
      this.submitting = true
      try {
        await request({
          url: '/finance/expense/batchVerify',
          method: 'PUT',
          data: this.pendingVerifyPayload,
          silent: true
        })
        this.pendingVerifyPayload = null
        uni.showToast({ title: '核销成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (error) {
        if (isUnknownWriteOutcome(error)) {
          const modal = await new Promise((resolve) => uni.showModal({
            title: '确认核销结果',
            content: '网络中断，无法确认核销结果。再次确认将复用同一请求编号，不会重复核销。',
            confirmText: '确认结果',
            cancelText: '返回核对',
            success: resolve,
            fail: () => resolve({ confirm: false })
          }))
          if (modal.confirm) {
            this.submitting = false
            await this.submitVerify()
            return
          }
          uni.navigateBack()
          return
        }
        this.pendingVerifyPayload = null
        this.requestId = this.createRequestId()
        uni.showToast({ title: error?.msg || error?.message || '核销失败', icon: 'none' })
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
  background: #E8EEF5;
  color: #1A2332;
  box-sizing: border-box;
}

.load-state,
.load-error {
  margin-top: 120rpx;
  padding: 48rpx 32rpx;
  border: 1rpx solid #D5E0EC;
  border-radius: 8rpx;
  background: #FFFFFF;
  text-align: center;
}

.load-error-title,
.load-error-message {
  display: block;
}

.load-error-title {
  color: #1A2332;
  font-size: 30rpx;
  font-weight: 700;
}

.load-error-message {
  margin-top: 14rpx;
  color: #64748B;
  font-size: 25rpx;
  line-height: 1.5;
}

.retry {
  width: 220rpx;
  height: 72rpx;
  margin: 28rpx auto 0;
  border: 1rpx solid #B8D4F0;
  border-radius: 8rpx;
  background: #E8F3FF;
  color: #087CF0;
  font-size: 26rpx;
  line-height: 72rpx;
}

.summary-card {
  background: linear-gradient(135deg, #123F73 0%, #087CF0 72%, #5AA9E8 100%);
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
  border: 1rpx solid #D5E0EC;
  box-shadow: 0 4rpx 14rpx rgba(45, 72, 98, 0.06);
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
  color: #087CF0;
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
  border: 2rpx solid #087CF0;
  background: #F0F7FA;
}

.card.selectable.selected .check {
  color: #FFFFFF;
  background: #087CF0;
  border-color: #087CF0;
}

.submit {
  position: fixed;
  bottom: 40rpx;
  left: 28rpx;
  right: 28rpx;
  height: 88rpx;
  line-height: 88rpx;
  color: #fff;
  background: #087CF0;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 24rpx rgba(8, 124, 240, 0.3);
}

.submit[disabled] {
  opacity: 0.55;
  box-shadow: none;
}
</style>
