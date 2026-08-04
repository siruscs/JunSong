<template>
  <view class="page inventory-workbench">
    <view class="workspace-header">
      <text class="workspace-eyebrow">库存工作台 · 只读查询</text>
      <text class="workspace-title">库存情况</text>
      <text class="workspace-note">{{ currentDeptName || '当前授权门店' }} · 查看库存数量、成本和金额</text>
    </view>
    <view class="inventory-summary" v-if="report">
      <view class="summary-item summary-primary"><text>库存金额</text><strong>¥{{ money(report.closingAmount) }}</strong><small>期末金额</small></view>
      <view class="summary-item"><text>库存数量</text><strong>{{ report.closingQuantity || 0 }}</strong><small>期末数量</small></view>
      <view class="summary-item"><text>商品数</text><strong>{{ items.length }}</strong><small>当前部门</small></view>
      <view class="summary-item summary-muted"><text>销售成本</text><strong>¥{{ money(report.saleCost) }}</strong><small>辅助指标</small></view>
    </view>
    <scroll-view scroll-y class="workspace-scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="refresh">
      <view v-if="stateStatus === 'normal'" class="inventory-list">
        <view class="inventory-card" v-for="item in items" :key="`${item.deptId}-${item.productId}`" @tap="openLedger(item)">
          <view class="inventory-card-head"><view><text class="inventory-card-kicker">库存商品</text><text class="product-name">{{ item.productName || '-' }}</text></view><text class="dept">{{ item.deptName || item.deptId || '-' }}</text></view>
          <view class="inventory-card-main"><view><text>库存数量</text><strong>{{ item.closingQuantity || 0 }}</strong></view><view><text>平均成本</text><strong>¥{{ money(item.avgUnitCost, 6) }}</strong></view><view class="amount-cell"><text>库存金额</text><strong>¥{{ money(item.closingAmount) }}</strong></view></view>
          <view class="inventory-card-foot"><text>进入明细查看变动</text><text class="ledger-link">查看库存流水 <text class="arrow">→</text></text></view>
        </view>
      </view>
      <StateView v-else :status="stateStatus" :message="loadError" @retry="load" />
    </scroll-view>
  </view>
</template>

<script>
import { getStockValueReport } from '@/api/stock.js'
import { requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'
import StateView from '@/components/StateView.vue'

export default {
  components: { StateView },
  data() { return { report: null, items: [], loading: false, refreshing: false, loadError: '', currentDeptName: '' } },
  computed: { stateStatus() { if (this.loading && !this.items.length) return 'loading'; if (this.loadError && !this.items.length) return 'error'; if (!this.items.length) return 'empty'; return 'normal' } },
  onLoad() { this.currentDeptName = workContext.snapshot().currentDept?.name || ''; if (requireModulePermission('stockCost')) this.load() },
  methods: {
    money(value, digits = 2) { return Number(value || 0).toFixed(digits) },
    load() {
      this.loading = true
      this.loadError = ''
      getStockValueReport({ startDate: '', endDate: '', pageNum: 1, pageSize: 200 })
        .then(res => { const data = res.data || {}; this.report = data; const scope = workContext.snapshot(); this.currentDeptName = scope.currentDept?.name || ''; this.items = (data.items || []).filter(item => String(item.deptId) === String(scope.currentDeptId)) })
        .catch(e => { this.loadError = e.msg || '库存加载失败'; uni.showToast({ title: this.loadError, icon: 'none' }) })
        .finally(() => { this.loading = false; this.refreshing = false })
    },
    refresh() { this.refreshing = true; this.load() }
    ,openLedger(item) { uni.navigateTo({ url: `/pages/stock-ledger/index?productId=${item.productId}&productName=${encodeURIComponent(item.productName || '')}&deptId=${item.deptId}` }) }
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#f4f7fb;color:#1e293b}.workspace-header{padding:34rpx 28rpx 48rpx;background:#087cf0;color:#fff}.workspace-eyebrow{display:block;font-size:22rpx;letter-spacing:1rpx;opacity:.78}.workspace-title{display:block;margin-top:12rpx;font-size:42rpx;font-weight:700;letter-spacing:-1rpx}.workspace-note{display:block;margin-top:12rpx;font-size:23rpx;line-height:34rpx;opacity:.86}.inventory-summary{display:flex;margin:-22rpx 24rpx 22rpx;padding:22rpx 8rpx;background:#fff;border:1rpx solid #e8eef6;border-radius:20rpx;box-shadow:0 10rpx 28rpx rgba(15,23,42,.07)}.summary-item{flex:1;min-width:0;padding:0 8rpx;color:#64748b;font-size:21rpx;text-align:center;border-right:1rpx solid #eef2f7}.summary-item:last-child{border-right:0}.summary-item strong{display:block;margin-top:10rpx;color:#0f172a;font-size:27rpx;font-weight:700;font-variant-numeric:tabular-nums;white-space:nowrap}.summary-item small{display:block;margin-top:6rpx;color:#94a3b8;font-size:18rpx}.summary-primary strong{color:#087cf0}.summary-muted strong{color:#64748b;font-size:23rpx}.workspace-scroll{height:calc(100vh - 270rpx);padding:0 24rpx 30rpx;box-sizing:border-box}.inventory-list{padding-bottom:20rpx}.inventory-card{margin-bottom:16rpx;padding:24rpx;background:#fff;border:1rpx solid #e8eef6;border-radius:18rpx;box-shadow:0 5rpx 18rpx rgba(15,23,42,.035)}.inventory-card-head{display:flex;justify-content:space-between;align-items:flex-start;gap:16rpx}.inventory-card-kicker{display:block;margin-bottom:6rpx;color:#94a3b8;font-size:19rpx}.product-name{display:block;color:#0f172a;font-size:29rpx;font-weight:650}.dept{padding:6rpx 12rpx;border-radius:8rpx;background:#f1f6fd;color:#5b7ea8;font-size:20rpx;white-space:nowrap}.inventory-card-main{display:flex;margin-top:24rpx;padding:18rpx 0;border-top:1rpx solid #f0f3f7;border-bottom:1rpx solid #f0f3f7}.inventory-card-main>view{flex:1;min-width:0;color:#64748b;font-size:21rpx}.inventory-card-main>view+view{padding-left:14rpx}.inventory-card-main strong{display:block;margin-top:8rpx;color:#1e293b;font-size:27rpx;font-weight:700;font-variant-numeric:tabular-nums;white-space:nowrap}.amount-cell strong{color:#087cf0}.inventory-card-foot{display:flex;justify-content:space-between;align-items:center;margin-top:18rpx;color:#94a3b8;font-size:21rpx}.ledger-link{color:#087cf0;font-size:22rpx;font-weight:600}.arrow{margin-left:5rpx;font-size:27rpx}.state-view{padding-top:50rpx}
</style>
