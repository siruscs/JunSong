<template>
  <view class="page">
    <view class="hero"><view><text class="eyebrow">库存管理</text><text class="hero-title">库存流水</text></view></view>
    <view class="work-scope"><view class="work-scope-mark"></view><view class="work-scope-copy"><text class="work-scope-label">当前部门 · </text><text class="work-scope-name">{{ currentDeptName || '当前部门' }}</text></view></view>
    <view class="section-card filters-card">
      <view class="section-header"><view class="section-dot" style="background:#087CF0"></view><text class="section-title">筛选流水</text><text class="section-link">共 {{ total }} 条</text></view>
      <view class="filter-inline"><picker class="filter-type-picker" :range="typeOptions" range-key="label" @change="changeType"><view class="filter-picker">{{ selectedTypeLabel }}<text class="filter-chevron">⌄</text></view></picker><view class="date-controls"><picker mode="date" :value="startDate" @change="e => changeDate('startDate', e.detail.value)"><view class="filter-date">{{ startDate }}</view></picker><text class="date-separator">至</text><picker mode="date" :value="endDate" @change="e => changeDate('endDate', e.detail.value)"><view class="filter-date">{{ endDate }}</view></picker><button class="filter-button" @tap="reload">查询</button></view></view>
    </view>
    <scroll-view scroll-y class="scroll" @scrolltolower="loadMore">
      <view class="section-card ledger-list-card"><view class="section-header"><view class="section-dot" style="background:#10B981"></view><text class="section-title">流水明细</text></view>
      <view class="record-card" v-for="row in rows" :key="row.ledgerId || row.id">
        <view class="card-bar" :style="{ background: changeDirection(row) === '入库' ? 'linear-gradient(180deg,#1687F5,#5AA9E8)' : 'linear-gradient(180deg,#F59E0B,#FCD34D)' }"></view>
        <view class="card-body">
          <view class="card-header">
            <text class="record-title">
              <text class="direction-tag" :class="changeDirection(row) === '入库' ? 'is-in' : 'is-out'">{{ changeDirection(row) === '入库' ? '入' : '出' }}</text>
              {{ changeTypeText(row.changeType || row.change_type) }}
            </text>
            <text class="record-id" :class="changeDirection(row) === '入库' ? 'is-in' : 'is-out'">{{ changeQuantity(row) }}</text>
          </view>
          <view class="summary-grid summary-grid-3">
            <view class="summary-item"><text class="summary-label">变动后余额</text><text class="summary-value">{{ row.afterQuantity ?? row.after_quantity ?? 0 }}</text></view>
            <view class="summary-item"><text class="summary-label">单位成本</text><text class="summary-value">¥{{ rowUnitCost(row) }}</text></view>
            <view class="summary-item"><text class="summary-label">变动金额</text><text class="summary-value tone-money">¥{{ rowChangeAmount(row) }}</text></view>
          </view>
          <view class="card-footer">
            <text class="meta-text">{{ formatDateTime(row.createTime || row.create_time) }} · {{ row.remark || '系统记录' }}</text>
          </view>
        </view>
      </view>
      <StateView v-if="stateStatus !== 'normal'" :status="stateStatus" message="" @retry="reload" />
      <view v-else-if="finished" class="finished">已加载全部流水（{{ total }} 条）</view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { queryStockLedger } from '@/api/stocktake.js'
import { request } from '@/api/index.js'
import { workContext } from '@/utils/workContext.js'
import { dictCache } from '@/utils/dictCache.js'
import StateView from '@/components/StateView.vue'
import { getStatusBarHeight } from '@/utils/systemInfo.js'

const FALLBACK_TYPES = { PURCHASE_IN: '采购入库', PURCHASE_REVERSE: '采购冲销', SALE_OUT: '销售出库', SALE_REVERSE: '销售冲销', STOCK_INIT: '期初库存', OPENING_STOCK: '期初库存', HISTORY_REPLENISH: '历史数据补录', TRIAL_CONSUMPTION: '试用消耗', STORE_USE: '店面自用', DAMAGE_LOSS: '报损', OTHER: '其他', STOCK_ADJUSTMENT: '库存调整', ADJUSTMENT_IN: '库存调整入库', ADJUSTMENT_OUT: '库存调整出库', STOCK_TAKE_GAIN: '盘点盘盈', STOCK_TAKE_LOSS: '盘点盘亏', STOCK_TAKE_REVERSE: '盘点冲销' }

export default {
  components: { StateView },
  data() {
    const today = new Date().toISOString().slice(0, 10)
    return { productId: null, productName: '', rows: [], loading: false, loadingMore: false, loadError: '', finished: false, total: 0, currentDeptId: null, currentDeptName: '', statusBarH: 0, menuButton: null, pageNum: 1, pageSize: 30, startDate: '2000-01-01', endDate: today, selectedChangeType: '', typeOptions: [{ label: '全部变动类型', value: '' }], adjustmentDict: [] }
  },
  computed: { selectedTypeLabel() { return this.typeOptions.find(x => x.value === this.selectedChangeType)?.label || '全部变动类型' }, stateStatus() { if (this.loading && !this.rows.length) return 'loading'; if (this.loadError && !this.rows.length) return 'error'; if (!this.rows.length) return 'empty'; return 'normal' }, headerContentStyle() { const top = this.menuButton?.bottom ? this.menuButton.bottom + 8 : this.statusBarH + 48; return { paddingTop: top + 'px' } } },
  onLoad(options = {}) {
    this.statusBarH = getStatusBarHeight()
    try { this.menuButton = uni.getMenuButtonBoundingClientRect() } catch (_) { this.menuButton = null }
    const s = workContext.snapshot()
    this.productId = options.productId || ''
    this.productName = decodeURIComponent(options.productName || '')
    this.currentDeptId = s.currentDeptId
    this.currentDeptName = s.currentDept?.name || ''
    if (!this.productId || !this.currentDeptId) return uni.showToast({ title: '缺少商品或部门信息', icon: 'none' })
    if (options.deptId && String(options.deptId) !== String(this.currentDeptId)) return uni.showToast({ title: '部门范围已变化', icon: 'none' })
    this.load()
  },
  onShow() {
    const s = workContext.snapshot()
    if (this.currentDeptId && String(this.currentDeptId) !== String(s.currentDeptId)) {
      this.currentDeptId = s.currentDeptId
      this.currentDeptName = s.currentDept?.name || ''
      this.reload()
    }
  },
  methods: {
    async loadDict() {
      try {
        this.adjustmentDict = await dictCache.get('finance_stock_adjustment_type', async () => { const res = await request({ url: '/system/dict/data/type/finance_stock_adjustment_type', method: 'GET' }); return res.data || res.rows || [] })
        this.typeOptions = [{ label: '全部变动类型', value: '' }, ...this.adjustmentDict.map(x => ({ label: x.dictLabel || x.label, value: x.dictValue || x.value }))]
      } catch (_) { this.typeOptions = [{ label: '全部变动类型', value: '' }] }
    },
    changeType(e) { this.selectedChangeType = this.typeOptions[Number(e.detail.value)]?.value || '' },
    changeDate(key, value) { this[key] = value },
    async reload() { this.pageNum = 1; this.rows = []; this.total = 0; this.loadError = ''; this.finished = false; await this.load() },
    async load() {
      if (!this.currentDeptId || this.loading || this.loadingMore) return
      this.loading = this.pageNum === 1
      this.loadingMore = this.pageNum > 1
      const requestDeptId = this.currentDeptId
      try {
        if (!this.adjustmentDict.length) await this.loadDict()
        const res = await queryStockLedger({ deptId: requestDeptId, productId: this.productId, startDate: this.startDate, endDate: this.endDate, changeType: this.selectedChangeType || undefined, pageNum: this.pageNum, pageSize: this.pageSize })
        if (String(workContext.snapshot().currentDeptId) !== String(requestDeptId)) return
        const data = res.data || res
        const pageRows = data.rows || data.records || data.items || []
        this.loadError = ''
        this.rows = this.pageNum === 1 ? pageRows : this.rows.concat(pageRows)
        this.total = Number(data.total ?? data.totalCount ?? this.rows.length) || 0
        this.finished = pageRows.length < this.pageSize || (this.total > 0 && this.rows.length >= this.total)
      } catch (e) { this.loadError = e.msg || '库存流水加载失败'; uni.showToast({ title: this.loadError, icon: 'none' }) } finally { this.loading = false; this.loadingMore = false }
    },
    loadMore() { if (!this.finished && !this.loading && !this.loadingMore) { this.pageNum += 1; this.load() } },
    changeDirection(row) { const quantity = Number(row.changeQuantity ?? row.change_quantity ?? 0); return quantity >= 0 ? '入库' : '出库' },
    changeQuantity(row) { const quantity = Number(row.changeQuantity ?? row.change_quantity ?? 0); return (quantity >= 0 ? '+' : '') + (row.changeQuantity ?? row.change_quantity ?? 0) },
    changeTypeText(type) { const item = this.adjustmentDict.find(x => String(x.dictValue ?? x.value) === String(type)); return item?.dictLabel || item?.label || FALLBACK_TYPES[type] || type || '库存变动' },
    rowUnitCost(row) { return Number(row.unitCost ?? row.unit_cost ?? 0).toFixed(2) },
    rowChangeAmount(row) { const q = Number(row.changeQuantity ?? row.change_quantity ?? 0); const c = Number(row.unitCost ?? row.unit_cost ?? 0); return Math.abs(q * c).toFixed(2) },
    formatDateTime(val) {
      if (!val) return '-'
      let s = String(val)
      if (s.includes('T')) s = s.replace('T', ' ')
      s = s.replace(/\.\d+Z?$/, '').replace(/Z$/, '')
      if (s.length > 19) s = s.slice(0, 19)
      return s
    }
  }
}
</script>

<style scoped>
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:#e7eff7;box-sizing:border-box;overflow:hidden}
.hero{margin:22rpx 30rpx 0;padding:28rpx 30rpx 30rpx;border-left:5rpx solid #1687f5;border-radius:20rpx;background:linear-gradient(110deg,#d9eaff,#f7faff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08);color:#1e293b}
.eyebrow{display:block;color:#1687f5;font-size:24rpx;font-weight:600}
.hero-title{display:block;margin-top:10rpx;color:#1e293b;font-size:38rpx;font-weight:700}
.work-scope{display:flex;align-items:center;margin:20rpx 30rpx 0;min-height:44rpx}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:16rpx;border-radius:50%;background:#1687f5}
.work-scope-copy{display:flex;align-items:baseline;color:#8192a6;font-size:24rpx}
.work-scope-name{margin-left:4rpx;color:#26384d;font-size:27rpx;font-weight:700}
.scroll{flex:1;width:100%;min-height:0;padding:0 30rpx 34rpx!important;box-sizing:border-box}
.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-top:24rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box;overflow:hidden}
.filters-card{margin:16rpx 30rpx 0!important;padding:22rpx 24rpx!important;position:relative;z-index:2}
.ledger-list-card{margin-top:16rpx!important;padding:20rpx 28rpx!important}
.section-header{display:flex;align-items:center;gap:12rpx;margin-bottom:18rpx}
.section-dot{width:12rpx;height:12rpx;border-radius:50%;flex-shrink:0}
.section-title{font-size:28rpx;font-weight:700;color:#1A2332;flex:1}
.section-link{font-size:22rpx;color:#94A3B8}
.filter-inline{display:flex;align-items:center;gap:10rpx;width:100%;min-width:0}
.filter-type-picker{flex:1.05;min-width:0}
.filter-inline .filter-picker{height:64rpx;line-height:32rpx;padding:16rpx 14rpx;overflow:hidden;text-overflow:ellipsis;box-sizing:border-box;border:1rpx solid #D5E0EC;border-radius:12rpx;background:#F8FBFD;color:#5A6B7F;font-size:22rpx;white-space:nowrap;display:flex;justify-content:space-between}
.filter-chevron{color:#94a3b8;font-size:25rpx}
.filter-inline .date-controls{flex:1.95;gap:6rpx;min-width:0;display:flex;align-items:center}
.filter-inline .filter-date{height:64rpx;line-height:32rpx;padding:16rpx 8rpx;font-size:20rpx;box-sizing:border-box;border:1rpx solid #D5E0EC;border-radius:12rpx;background:#F8FBFD;color:#5A6B7F;overflow:hidden;text-align:center}
.filter-inline .date-separator{flex:none;font-size:19rpx;color:#94a3b8}
.filter-inline .filter-button{padding:0 14rpx;white-space:nowrap;font-size:22rpx;height:64rpx;line-height:64rpx;border:0;border-radius:32rpx;background:#087CF0;color:#fff;margin:0}

.record-card{display:flex;margin-bottom:16rpx;background:#FFFFFF;border-radius:20rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,0.06);overflow:hidden}
.card-bar{width:4rpx;background:linear-gradient(180deg,#087CF0,#A8C7E5);flex-shrink:0}
.card-body{flex:1;padding:24rpx 28rpx}
.card-header{display:flex;align-items:flex-start;justify-content:space-between;gap:20rpx}
.record-title{flex:1;font-size:30rpx;line-height:42rpx;font-weight:700;color:#1A2332;display:flex;align-items:center;gap:14rpx}
.direction-tag{display:inline-flex;align-items:center;justify-content:center;width:48rpx;height:48rpx;border-radius:14rpx;font-size:24rpx;font-weight:700;flex-shrink:0}
.direction-tag.is-in{background:#E0F2FE;color:#075985}
.direction-tag.is-out{background:#FEF3C7;color:#92400E}
.record-id{padding:6rpx 18rpx;font-size:24rpx;border-radius:999rpx;flex-shrink:0;font-weight:700;font-variant-numeric:tabular-nums}
.record-id.is-in{background:#E0F2FE;color:#075985}
.record-id.is-out{background:#FEF3C7;color:#92400E}
.summary-grid{display:grid;grid-template-columns:1fr 1fr;gap:12rpx;margin-top:16rpx}
.summary-grid.summary-grid-3{grid-template-columns:1fr 1fr 1fr;gap:12rpx 10rpx}
.summary-item{display:flex;flex-direction:column;gap:6rpx}
.summary-label{font-size:22rpx;color:#94A3B8}
.summary-value{font-size:26rpx;color:#1A2332;font-weight:500}
.summary-value.tone-money{color:#1687f5;font-weight:700}
.card-footer{display:flex;justify-content:flex-start;align-items:center;margin-top:16rpx;padding-top:14rpx;border-top:1rpx solid #E8EEF5}
.meta-text{font-size:24rpx;color:#94A3B8;flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.finished{text-align:center;padding:24rpx 0 0;color:#94a3b8;font-size:23rpx}
</style>
