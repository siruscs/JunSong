<template>
  <view class="page">
    <view class="header"><view class="header-bg"></view><view class="header-content" :style="headerContentStyle"><view class="header-row"><view class="header-left"><text class="header-title">{{ productName || '商品' }}</text><text class="header-sub">库存流水 · {{ currentDeptName || '当前部门' }}</text></view><view class="dept-static-inline"><text class="dept-name-inline">{{ currentDeptName || '当前部门' }}</text></view></view></view></view>
    <view class="section-card filters-card">
      <view class="section-header"><view class="section-dot" style="background:#087CF0"></view><text class="section-title">筛选流水</text><text class="section-link">共 {{ total }} 条</text></view>
      <view class="filter-row"><text class="filter-label">变动类型</text><picker class="filter-control-wide" :range="typeOptions" range-key="label" @change="changeType"><view class="filter-picker">{{ selectedTypeLabel }}<text class="filter-chevron">⌄</text></view></picker></view>
      <view class="filter-row filter-date-row"><text class="filter-label">日期范围</text><view class="date-controls"><picker mode="date" :value="startDate" @change="e => changeDate('startDate', e.detail.value)"><view class="filter-date">{{ startDate }}</view></picker><text class="date-separator">至</text><picker mode="date" :value="endDate" @change="e => changeDate('endDate', e.detail.value)"><view class="filter-date">{{ endDate }}</view></picker><button class="filter-button" @tap="reload">查询</button></view></view>
    </view>
    <scroll-view scroll-y class="scroll" @scrolltolower="loadMore">
      <view class="section-card ledger-list-card"><view class="section-header"><view class="section-dot" style="background:#10B981"></view><text class="section-title">流水明细</text></view>
      <view v-for="row in rows" :key="row.ledgerId || row.id" class="ledger-card">
        <view class="ledger-card-head"><view class="ledger-type"><text class="direction-mark" :class="changeDirection(row) === '入库' ? 'is-in' : 'is-out'">{{ changeDirection(row) === '入库' ? '入' : '出' }}</text><view><text class="ledger-kicker">{{ changeDirection(row) }}</text><text class="ledger-title">{{ changeTypeText(row.changeType || row.change_type) }}</text></view></view><text class="ledger-delta" :class="changeDirection(row) === '入库' ? 'is-in' : 'is-out'">{{ Number(row.changeQuantity ?? row.change_quantity ?? 0) >= 0 ? '+' : '' }}{{ row.changeQuantity ?? row.change_quantity ?? 0 }}</text></view>
        <view class="ledger-card-meta"><text>{{ row.createTime || row.create_time || '-' }}</text><text>变动后余额 {{ row.afterQuantity ?? row.after_quantity ?? 0 }}</text></view>
        <view class="ledger-card-source"><text class="source-label">来源</text><text class="remark">{{ row.remark || row.referenceNo || row.reference_no || row.referenceType || row.reference_type || '系统记录' }}</text></view>
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
    changeTypeText(type) { const item = this.adjustmentDict.find(x => String(x.dictValue ?? x.value) === String(type)); return item?.dictLabel || item?.label || FALLBACK_TYPES[type] || type || '库存变动' }
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#f4f7fb;color:#1e293b}.workspace-header{padding:34rpx 28rpx 44rpx;background:#087cf0;color:#fff}.workspace-eyebrow{display:block;font-size:22rpx;letter-spacing:1rpx;opacity:.78}.workspace-title{display:block;margin-top:12rpx;font-size:40rpx;font-weight:700;letter-spacing:-1rpx}.workspace-note{display:block;margin-top:10rpx;font-size:23rpx;line-height:34rpx;opacity:.86}.ledger-filters{padding:18rpx 24rpx 20rpx;background:#fff;border-bottom:1rpx solid #e8eef6}.filter-row{display:flex;align-items:center;gap:16rpx;min-height:70rpx}.filter-row+.filter-row{margin-top:10rpx}.filter-label{width:108rpx;color:#64748b;font-size:22rpx}.filter-control-wide{flex:1;min-width:0}.filter-picker,.filter-date{box-sizing:border-box;padding:16rpx 18rpx;border:1rpx solid #dbe4ef;border-radius:12rpx;background:#f8fafc;color:#334155;font-size:22rpx;white-space:nowrap}.filter-picker{display:flex;justify-content:space-between}.filter-chevron{color:#94a3b8;font-size:25rpx}.date-controls{display:flex;align-items:center;flex:1;min-width:0;gap:8rpx}.date-controls picker{flex:1;min-width:0}.filter-date{overflow:hidden;text-align:center}.date-separator{color:#94a3b8;font-size:21rpx}.filter-button{flex:none;margin:0;padding:0 18rpx;height:66rpx;line-height:66rpx;border:0;border-radius:11rpx;background:#087cf0;color:#fff;font-size:23rpx}.workspace-scroll{height:calc(100vh - 320rpx);padding:22rpx 24rpx 30rpx;box-sizing:border-box}.ledger-card{margin-bottom:16rpx;padding:24rpx;background:#fff;border:1rpx solid #e8eef6;border-radius:18rpx;box-shadow:0 5rpx 18rpx rgba(15,23,42,.035)}.ledger-card-head{display:flex;justify-content:space-between;align-items:flex-start}.ledger-type{display:flex;align-items:center;gap:14rpx;min-width:0}.direction-mark{display:flex;align-items:center;justify-content:center;width:48rpx;height:48rpx;border-radius:14rpx;font-size:24rpx;font-weight:700}.direction-mark.is-in{background:#eaf8ef;color:#16834b}.direction-mark.is-out{background:#fff2e9;color:#c2410c}.ledger-kicker{display:block;color:#94a3b8;font-size:19rpx}.ledger-title{display:block;margin-top:5rpx;color:#1e293b;font-size:28rpx;font-weight:650}.ledger-delta{font-size:30rpx;font-weight:700;font-variant-numeric:tabular-nums}.ledger-delta.is-in{color:#16834b}.ledger-delta.is-out{color:#c2410c}.ledger-card-meta{display:flex;justify-content:space-between;gap:16rpx;margin-top:20rpx;padding-top:16rpx;border-top:1rpx solid #f0f3f7;color:#64748b;font-size:21rpx}.ledger-card-source{display:flex;align-items:flex-start;gap:14rpx;margin-top:12rpx;color:#94a3b8;font-size:21rpx}.source-label{flex:none;color:#a0aec0}.remark{margin:0;color:#64748b;line-height:32rpx;word-break:break-all}.finished{text-align:center;padding:56rpx 0;color:#94a3b8;font-size:23rpx}
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:linear-gradient(180deg,#E6EEF6 0%,#F3F6FA 42%,#E8EEF5 100%);box-sizing:border-box;overflow:hidden}.header{position:relative;overflow:hidden;flex-shrink:0;background:linear-gradient(180deg,#C7DCF2 0%,#E1ECF8 100%);border-bottom:2rpx solid #AFCBE7}.header-bg{position:absolute;inset:0;background:linear-gradient(135deg,rgba(255,255,255,.58) 0%,rgba(202,224,246,.9) 100%)}.header-content{position:relative;z-index:2;padding:0 30rpx 42rpx}.header-row{display:flex;align-items:center;justify-content:space-between;gap:16rpx}.header-left{display:flex;flex-direction:column;flex:1;min-width:0;overflow:hidden}.header-title{font-size:36rpx;font-weight:700;color:#1F2D3D}.header-sub{font-size:24rpx;color:#8190A1;margin-top:10rpx;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.dept-static-inline{display:flex;align-items:center;justify-content:center;padding:16rpx 24rpx;min-height:60rpx;border-radius:16rpx;background:#F1F6FF;border:1rpx solid #CFE0F8}.dept-name-inline{font-size:26rpx;font-weight:600;color:#087CF0;max-width:160rpx;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.scroll{flex:1;width:100%;min-height:0;padding:18rpx 28rpx 40rpx;box-sizing:border-box}.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-top:24rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box;overflow:hidden}.filters-card{margin-top:-18rpx;position:relative;z-index:2;padding:24rpx 28rpx}.ledger-list-card{margin-top:0;padding:28rpx}.section-header{display:flex;align-items:center;gap:12rpx;margin-bottom:18rpx}.section-dot{width:12rpx;height:12rpx;border-radius:50%;flex-shrink:0}.section-title{font-size:28rpx;font-weight:700;color:#1A2332;flex:1}.section-link{font-size:22rpx;color:#94A3B8}.filter-row{min-height:66rpx}.filter-row+.filter-row{margin-top:10rpx}.filter-picker,.filter-date{border-color:#D5E0EC;background:#F8FBFD;color:#5A6B7F}.filter-button{background:#087CF0}.ledger-card{margin-bottom:14rpx;padding:20rpx 0;border:0;border-radius:0;box-shadow:none;border-bottom:1rpx solid #E8EEF5}.ledger-card:last-of-type{border-bottom:0}.ledger-card-head{align-items:center}.ledger-card-meta{border-top:0;padding-top:14rpx}.finished{padding:24rpx 0 0}
</style>
