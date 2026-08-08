<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="eyebrow">财务管理</text>
        <text class="hero-title">库存查询</text>
      </view>
    </view>
    <view class="work-scope" :class="{ 'work-scope-disabled': !switchable }" :hover-class="switchable ? 'work-scope-hover' : ''" hover-stay-time="80" hover-start-time="30" @tap="openDeptSwitcher"><view class="work-scope-mark" :class="{ 'work-scope-mark-disabled': !switchable }"></view><view class="work-scope-copy"><text class="work-scope-label">{{ scopeLabel }}</text><text class="work-scope-name">{{ currentDeptName || '未选择部门' }}</text></view></view>
    <view class="summary-bar" v-if="items.length"><view><text class="summary-value">¥{{ money(totalClosingAmount) }}</text><text class="summary-label">库存金额</text></view><view><text class="summary-value">{{ totalClosingQuantity }}</text><text class="summary-label">库存数量</text></view><view><text class="summary-value">{{ items.length }}</text><text class="summary-label">商品数</text></view></view>
    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="refresh">
      <view v-if="stateStatus === 'normal'" class="record-list">
        <view class="record-card" v-for="item in items" :key="`${item.deptId}-${item.productId}`" @tap="openLedger(item)">
          <view class="card-bar"></view>
          <view class="card-body">
            <view class="card-header">
              <text class="record-title">{{ item.productName || '-' }}</text>
              <text class="record-quantity">{{ item.closingQuantity || 0 }}</text>
            </view>
            <view class="summary-grid">
              <view class="summary-item"><text class="summary-label">平均成本</text><text class="summary-value">¥{{ money(item.avgUnitCost) }}</text></view>
              <view class="summary-item"><text class="summary-label">库存金额</text><text class="summary-value tone-money">¥{{ money(item.closingAmount) }}</text></view>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="section-card state-card"><StateView :status="stateStatus" :message="loadError" @retry="load" /></view>
    </scroll-view>
    <dept-switcher
      v-model:visible="showDeptSwitcher"
      :current-dept-id="currentDeptId"
      :request-fn="request"
      @change="onDeptSwitcherChanged"
    />
  </view>
</template>

<script>
import { getStockValueReport } from '@/api/stock.js'
import { requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'
import { applyWorkScopeToPage, openDeptSwitcher, handleDeptChanged } from '@/utils/listWorkScope.js'
import { request } from '@/api/index.js'
import StateView from '@/components/StateView.vue'
import DeptSwitcher from '@/components/DeptSwitcher.vue'
import { getStatusBarHeight } from '@/utils/systemInfo.js'

export default {
  components: { StateView, DeptSwitcher },
  data() { return { report: null, items: [], loading: false, refreshing: false, loadError: '', showDeptSwitcher: false, scopeLabel: '暂无可用数据范围', contextVersion: 0, currentDeptId: null, currentDeptName: '未选择部门', switchable: false, deptCount: 0, statusBarH: 0, menuButton: null } },
  computed: {
    stateStatus() { if (this.loading && !this.items.length) return 'loading'; if (this.loadError && !this.items.length) return 'error'; if (!this.items.length) return 'empty'; return 'normal' },
    headerContentStyle() { const top = this.menuButton?.bottom ? this.menuButton.bottom + 8 : this.statusBarH + 48; return { paddingTop: top + 'px' } },
    totalClosingAmount() { return this.items.reduce((sum, item) => sum + Number(item.closingAmount || 0), 0) },
    totalClosingQuantity() { return this.items.reduce((sum, item) => sum + Number(item.closingQuantity || 0), 0) }
  },
  onLoad() { this.statusBarH = getStatusBarHeight(); try { this.menuButton = uni.getMenuButtonBoundingClientRect() } catch (_) { this.menuButton = null }; applyWorkScopeToPage(this); if (requireModulePermission('stockCost')) this.load() },
  onShow() { const { departmentChanged } = applyWorkScopeToPage(this); if (departmentChanged) this.refresh() },
  methods: {
    openDeptSwitcher() { return openDeptSwitcher(this) },
    onDeptSwitcherChanged() { return handleDeptChanged(this, () => this.refresh()) },
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
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:#e7eff7;box-sizing:border-box;overflow:hidden}
.hero{margin:22rpx 30rpx 0;padding:28rpx 30rpx 30rpx;border-left:5rpx solid #1687f5;border-radius:20rpx;background:linear-gradient(110deg,#d9eaff,#f7faff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08);color:#1e293b}
.eyebrow{display:block;color:#1687f5;font-size:24rpx;font-weight:600}
.hero-title{display:block;margin-top:10rpx;color:#1e293b;font-size:38rpx;font-weight:700}
.work-scope{display:flex;align-items:center;margin:20rpx 30rpx 0;min-height:44rpx;padding:4rpx 12rpx;border-radius:12rpx;box-sizing:border-box}
.work-scope-hover{background:#eaf3ff;border-radius:12rpx}
.work-scope-disabled{opacity:1;background:#F1F5F9}
.work-scope-disabled .work-scope-copy{color:#475569}
.work-scope-disabled .work-scope-name{color:#1E293B;font-weight:600}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:16rpx;border-radius:50%;background:#1687f5}
.work-scope-mark-disabled{background:#475569}
.work-scope-copy{display:flex;align-items:baseline;color:#8192a6;font-size:24rpx}
.work-scope-name{margin-left:4rpx;color:#26384d;font-size:27rpx;font-weight:700}
.summary-bar{display:flex;margin:16rpx 30rpx 0;padding:18rpx 8rpx;background:#fff;border-radius:18rpx;border:1rpx solid #dbe6f1}
.summary-bar>view{flex:1;text-align:center;border-right:1rpx solid #edf1f5}
.summary-bar>view:last-child{border-right:0}
.summary-bar .summary-value{display:block;color:#1687f5;font-size:28rpx;font-weight:700}
.summary-bar .summary-label{display:block;margin-top:6rpx;color:#98a9ba;font-size:20rpx}
.scroll{flex:1;width:100%;min-height:0;padding:16rpx 30rpx 34rpx;box-sizing:border-box;overflow-x:hidden}
.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-top:24rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);width:100%;box-sizing:border-box;overflow:hidden}
.state-card{padding:0 28rpx 28rpx;margin:16rpx 0 0}
.state-card .state-view{padding-top:20rpx}
.record-list{margin-top:0;padding-top:8rpx}
.record-card{display:flex;margin-bottom:16rpx;background:#fff;border-radius:20rpx;box-shadow:0 6rpx 16rpx rgba(46,82,120,.08);overflow:hidden}
.card-bar{width:4rpx;flex-shrink:0;background:linear-gradient(180deg,#1687f5 0%,#58b0ff 100%);border-radius:4rpx 0 0 4rpx}
.card-body{flex:1;min-width:0;padding:24rpx 26rpx 22rpx 22rpx;display:flex;flex-direction:column;gap:18rpx}
.card-header{display:flex;justify-content:space-between;align-items:flex-start;gap:16rpx}
.record-title{flex:1;min-width:0;color:#1A2332;font-size:29rpx;font-weight:700;line-height:1.35;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.record-quantity{flex-shrink:0;color:#1687f5;font-size:30rpx;font-weight:800;font-variant-numeric:tabular-nums;white-space:nowrap;padding-left:16rpx}
.summary-grid{display:grid;grid-template-columns:1fr 1fr;gap:14rpx 24rpx}
.summary-item{display:flex;flex-direction:column;gap:6rpx;min-width:0}
.summary-grid .summary-label{color:#8A9BB0;font-size:20rpx;line-height:1}
.summary-grid .summary-value{color:#1A2332;font-size:26rpx;font-weight:700;font-variant-numeric:tabular-nums;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tone-money{color:#1687f5!important}
.tone-warn{color:#F59E0B!important;font-size:22rpx!important;font-weight:600!important}
</style>
