<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">库存与成本</text>
      <text class="hero-title">库存情况</text>
      <text class="hero-note">只读查看当前授权门店的库存数量与金额</text>
    </view>
    <view class="summary" v-if="report">
      <view class="summary-item"><text>期末数量</text><strong>{{ report.closingQuantity || 0 }}</strong></view>
      <view class="summary-item"><text>期末金额</text><strong>¥{{ money(report.closingAmount) }}</strong></view>
      <view class="summary-item"><text>销售成本</text><strong>¥{{ money(report.saleCost) }}</strong></view>
    </view>
    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="refresh">
      <view class="stock-card" v-for="item in items" :key="`${item.deptId}-${item.productId}`" @tap="openLedger(item)">
        <view class="stock-card-head"><text class="product-name">{{ item.productName || '-' }}</text><text class="dept">{{ item.deptName || item.deptId || '-' }}</text></view>
        <view class="stock-card-grid">
          <view><text>库存数量</text><strong>{{ item.closingQuantity || 0 }}</strong></view>
          <view><text>平均成本</text><strong>¥{{ money(item.avgUnitCost, 6) }}</strong></view>
          <view><text>库存金额</text><strong>¥{{ money(item.closingAmount) }}</strong></view>
        </view>
        <text class="ledger-link">查看库存流水 ›</text>
      </view>
      <view class="empty" v-if="!loading && !items.length">暂无库存数据</view>
      <view class="loading" v-if="loading">加载中</view>
    </scroll-view>
  </view>
</template>

<script>
import { getStockValueReport } from '@/api/stock.js'
import { requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'

export default {
  data() { return { report: null, items: [], loading: false, refreshing: false } },
  onLoad() { if (requireModulePermission('stockCost')) this.load() },
  methods: {
    money(value, digits = 2) { return Number(value || 0).toFixed(digits) },
    load() {
      this.loading = true
      getStockValueReport({ startDate: '', endDate: '', pageNum: 1, pageSize: 200 })
        .then(res => { const data = res.data || {}; this.report = data; const deptId = workContext.snapshot().currentDeptId; this.items = (data.items || []).filter(item => String(item.deptId) === String(deptId)) })
        .catch(() => uni.showToast({ title: '库存加载失败', icon: 'none' }))
        .finally(() => { this.loading = false; this.refreshing = false })
    },
    refresh() { this.refreshing = true; this.load() }
    ,openLedger(item) { uni.navigateTo({ url: `/pages/stock-ledger/index?productId=${item.productId}&productName=${encodeURIComponent(item.productName || '')}&deptId=${item.deptId}` }) }
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f7fb}.hero{padding:42rpx 32rpx 34rpx;background:#087cf0;color:#fff}.eyebrow{display:block;font-size:24rpx;opacity:.8}.hero-title{display:block;margin-top:12rpx;font-size:44rpx;font-weight:700}.hero-note{display:block;margin-top:14rpx;font-size:24rpx;opacity:.85}.summary{display:flex;margin:-18rpx 24rpx 20rpx;padding:24rpx 10rpx;background:#fff;border-radius:18rpx;box-shadow:0 8rpx 24rpx rgba(15,23,42,.08)}.summary-item{flex:1;text-align:center;color:#64748b;font-size:22rpx}.summary-item strong{display:block;margin-top:10rpx;color:#0f172a;font-size:30rpx}.scroll{height:calc(100vh - 260rpx);padding:0 24rpx;box-sizing:border-box}.stock-card{margin-bottom:18rpx;padding:24rpx;background:#fff;border-radius:16rpx}.stock-card-head{display:flex;justify-content:space-between;align-items:center}.product-name{font-size:30rpx;font-weight:600;color:#0f172a}.dept{font-size:22rpx;color:#64748b}.stock-card-grid{display:flex;margin-top:22rpx}.stock-card-grid view{flex:1;color:#64748b;font-size:22rpx}.stock-card-grid strong{display:block;margin-top:8rpx;color:#0f172a;font-size:26rpx}.ledger-link{display:block;margin-top:18rpx;color:#087cf0;font-size:23rpx}.empty,.loading{text-align:center;padding:80rpx;color:#94a3b8;font-size:26rpx}
</style>
