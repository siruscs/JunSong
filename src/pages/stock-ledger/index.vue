<template>
  <view class="page">
    <view class="hero"><text class="eyebrow">库存与成本</text><text class="title">{{ productName || '商品' }} · 库存流水</text><text class="note">{{ currentDeptName }}</text></view>
    <view class="filters">
      <picker :range="typeOptions" range-key="label" @change="changeType"><view class="filter-picker">{{ selectedTypeLabel }}⌄</view></picker>
      <picker mode="date" :value="startDate" @change="e => changeDate('startDate', e.detail.value)"><view class="filter-date">{{ startDate }}</view></picker>
      <picker mode="date" :value="endDate" @change="e => changeDate('endDate', e.detail.value)"><view class="filter-date">{{ endDate }}</view></picker>
      <button class="filter-button" @tap="reload">查询</button>
    </view>
    <scroll-view scroll-y class="scroll" @scrolltolower="loadMore">
      <view v-for="row in rows" :key="row.ledgerId || row.id" class="card">
        <view class="head"><text>{{ changeTypeText(row.changeType || row.change_type) }}</text><text :class="Number(row.changeQuantity ?? row.change_quantity ?? 0) >= 0 ? 'in' : 'out'">{{ row.changeQuantity ?? row.change_quantity ?? 0 }}</text></view>
        <view class="meta"><text>{{ row.createTime || row.create_time || '-' }}</text><text>余额 {{ row.afterQuantity ?? row.after_quantity ?? 0 }}</text></view>
        <view class="remark">{{ row.remark || row.referenceNo || row.reference_no || row.referenceType || row.reference_type || '-' }}</view>
      </view>
      <view v-if="loading" class="empty">加载中</view>
      <view v-else-if="!rows.length" class="empty">暂无库存流水</view>
      <view v-else-if="finished" class="finished">已加载全部流水（{{ total }} 条）</view>
    </scroll-view>
  </view>
</template>

<script>
import { queryStockLedger } from '@/api/stocktake.js'
import { request } from '@/api/index.js'
import { workContext } from '@/utils/workContext.js'
import { dictCache } from '@/utils/dictCache.js'

const FALLBACK_TYPES = { PURCHASE_IN: '采购入库', PURCHASE_REVERSE: '采购冲销', SALE_OUT: '销售出库', SALE_REVERSE: '销售冲销', STOCK_INIT: '期初库存', OPENING_STOCK: '期初库存', HISTORY_REPLENISH: '历史数据补录', TRIAL_CONSUMPTION: '试用消耗', STORE_USE: '店面自用', DAMAGE_LOSS: '报损', OTHER: '其他', STOCK_ADJUSTMENT: '库存调整', ADJUSTMENT_IN: '库存调整入库', ADJUSTMENT_OUT: '库存调整出库', STOCK_TAKE_GAIN: '盘点盘盈', STOCK_TAKE_LOSS: '盘点盘亏', STOCK_TAKE_REVERSE: '盘点冲销' }

export default {
  data() {
    const today = new Date().toISOString().slice(0, 10)
    return { productId: null, productName: '', rows: [], loading: false, loadingMore: false, finished: false, total: 0, currentDeptId: null, currentDeptName: '', pageNum: 1, pageSize: 30, startDate: '2000-01-01', endDate: today, selectedChangeType: '', typeOptions: [{ label: '全部变动类型', value: '' }], adjustmentDict: [] }
  },
  computed: { selectedTypeLabel() { return this.typeOptions.find(x => x.value === this.selectedChangeType)?.label || '全部变动类型' } },
  onLoad(options = {}) {
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
    async reload() { this.pageNum = 1; this.rows = []; this.total = 0; this.finished = false; await this.load() },
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
        this.rows = this.pageNum === 1 ? pageRows : this.rows.concat(pageRows)
        this.total = Number(data.total ?? data.totalCount ?? this.rows.length) || 0
        this.finished = pageRows.length < this.pageSize || (this.total > 0 && this.rows.length >= this.total)
      } catch (e) { uni.showToast({ title: e.msg || '库存流水加载失败', icon: 'none' }) } finally { this.loading = false; this.loadingMore = false }
    },
    loadMore() { if (!this.finished && !this.loading && !this.loadingMore) { this.pageNum += 1; this.load() } },
    changeTypeText(type) { const item = this.adjustmentDict.find(x => String(x.dictValue ?? x.value) === String(type)); return item?.dictLabel || item?.label || FALLBACK_TYPES[type] || type || '库存变动' }
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f7fb}.hero{padding:34rpx 28rpx;background:#087cf0;color:#fff}.eyebrow{display:block;font-size:23rpx;opacity:.8}.title{display:block;margin-top:10rpx;font-size:36rpx;font-weight:700}.note{display:block;margin-top:10rpx;font-size:23rpx;opacity:.85}.filters{display:flex;gap:10rpx;align-items:center;padding:18rpx 20rpx;background:#fff;box-shadow:0 2rpx 8rpx rgba(15,23,42,.06)}.filter-picker,.filter-date{padding:16rpx 12rpx;border:1rpx solid #dbe4ef;border-radius:10rpx;background:#f8fafc;color:#475569;font-size:22rpx;white-space:nowrap}.filter-picker{width:190rpx}.filter-date{width:190rpx}.filter-button{margin:0;padding:0 18rpx;height:66rpx;line-height:66rpx;border:0;border-radius:10rpx;background:#087cf0;color:#fff;font-size:23rpx}.scroll{height:calc(100vh - 280rpx);padding:22rpx 24rpx;box-sizing:border-box}.card{background:#fff;border-radius:16rpx;padding:24rpx;margin-bottom:16rpx}.head,.meta{display:flex;justify-content:space-between}.head{font-size:28rpx;font-weight:600}.in{color:#16834b}.out{color:#c2410c}.meta,.remark{margin-top:14rpx;color:#718096;font-size:23rpx}.empty,.finished{text-align:center;padding:70rpx 0;color:#94a3b8;font-size:26rpx}
</style>
