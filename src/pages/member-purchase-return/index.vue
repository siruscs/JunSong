<template>
  <view class="page" v-if="authorized">
    <view class="hero"><text class="eyebrow">会员服务</text><text class="hero-title">退货/退款</text></view>
    <view class="work-scope"><view class="work-scope-mark"></view><view class="work-scope-copy"><text class="work-scope-label">当前部门 · </text><text class="work-scope-name">{{ currentDeptName || '当前部门' }}</text></view></view>

    <view class="section-card filters-card">
      <view class="filter-row filter-row-merged">
        <picker class="filter-type-picker" :range="statusFilters" range-key="label" :value="statusFilterIndex" @change="selectStatusFilter"><view class="filter-picker">{{ statusFilters[statusFilterIndex]?.label || '全部状态' }}<text class="filter-chevron">⌄</text></view></picker>
        <input class="filter-kw" v-model="keyword" placeholder="顾客姓名或手机号" confirm-type="search" @confirm="load" />
        <button class="filter-button" @tap="load">查询</button>
        <button class="filter-button filter-button-ghost" @tap="resetFilters">重置</button>
      </view>
    </view>

    <view class="summary-bar" v-if="summary"><view><text class="summary-value">{{ summary.returnCount || 0 }}</text><text class="summary-label">退货单数</text></view><view><text class="summary-value warning">¥{{ money(summary.refundAmount) }}</text><text class="summary-label">应退金额</text></view><view><text class="summary-value success">¥{{ money(summary.refundedAmount) }}</text><text class="summary-label">已退金额</text></view></view>

    <view class="scroll-pad"></view>
    <view class="bottom-bar">
      <button v-if="can('add')" class="add-button" @tap="openCreate">＋ 新增</button>
    </view>

    <scroll-view scroll-y class="scroll" @scrolltolower="nextPage">
      <view class="record-card" v-for="row in rows" :key="row.returnId" @tap="openDetail(row)">
        <view class="card-bar"></view>
        <view class="card-body compact-body">
          <view class="compact-row1">
            <text class="compact-title">{{ row.customerName || '未登记顾客' }}</text>
            <view class="compact-qty-group">
              <text class="compact-qty-label">原买</text><text class="compact-qty-value">{{ row.purchaseQuantity || 0 }}</text>
              <text class="compact-qty-label">退</text><text class="compact-qty-value">{{ row.totalReturnQuantity || 0 }}</text>
            </view>
            <text class="compact-amount">¥{{ money(row.refundAmount) }}</text>
          </view>
          <view class="compact-row2">
            <text class="compact-meta date">{{ dateText(row.returnDate) }}</text>
            <text class="compact-meta paid">已退 ¥{{ money(row.refundedAmount) }}</text>
            <text class="compact-status" :class="statusClass(row.status)">{{ statusText(row.status) }}</text>
          </view>
        </view>
      </view>
      <view class="section-card list-card state-card" v-if="!rows.length">
        <view class="empty">暂无退货记录</view>
      </view>
    </scroll-view>

    <!-- ════════ 详情面板（参考 member-purchase detail-page） ════════ -->
    <view class="overlay-mask" v-if="panel === 'detail'" @tap="closePanel">
      <view class="detail-page" @tap.stop>
        <view class="detail-hero">
          <view class="detail-hero-bg"></view>
          <view class="detail-hero-content">
            <view class="detail-hero-eyebrow">购买退货 · {{ statusText(detail.status) }}</view>
            <view class="detail-hero-title">{{ detail.returnNo || `退货单 #${detail.returnId}` }}</view>
            <view class="detail-hero-value">¥{{ money(detail.refundAmount) }}</view>
            <view class="detail-hero-meta">退货日期 {{ dateText(detail.returnDate) }}</view>
          </view>
        </view>

        <view class="detail-section">
          <view class="detail-section-title">概要信息</view>
          <view class="detail-highlight-grid">
            <view class="detail-highlight-item"><view class="detail-highlight-label">顾客姓名</view><view class="detail-highlight-value">{{ detail.customerName || '未登记顾客' }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">手机号</view><view class="detail-highlight-value">{{ detail.customerPhone || '-' }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">退货状态</view><view class="detail-highlight-value" :class="statusClass(detail.status)">{{ statusText(detail.status) }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">原购买单</view><view class="detail-highlight-value">{{ detail.purchaseId }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">应退金额</view><view class="detail-highlight-value tone-warning">¥{{ money(detail.refundAmount) }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">已退金额</view><view class="detail-highlight-value tone-success">¥{{ money(detail.refundedAmount) }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">退货办理周期</view><view class="detail-highlight-value">{{ detail.returnPeriodId || '-' }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">原核算周期</view><view class="detail-highlight-value">{{ detail.originalPeriodId || '-' }}</view></view>
          </view>
        </view>

        <view class="detail-section" v-if="(detail.items || []).length">
          <view class="detail-section-title">退货明细</view>
          <view class="detail-item" v-for="(item, idx) in detail.items || []" :key="idx">
            <view class="detail-item-header"><text class="detail-item-title">{{ item.productNameSnapshot || `商品${idx + 1}` }}</text></view>
            <view class="detail-row"><text class="detail-label">退正品数量</text><text class="detail-value-text">{{ quantity(item.returnSaleQuantity) }}</text></view>
            <view class="detail-row"><text class="detail-label">退赠品数量</text><text class="detail-value-text">{{ quantity(item.returnGiftQuantity) }}</text></view>
            <view class="detail-row"><text class="detail-label">退货总数</text><text class="detail-value-text">{{ quantity(item.returnTotalQuantity) }}</text></view>
            <view class="detail-row"><text class="detail-label">退款单价</text><text class="detail-value-text">¥{{ money(item.refundUnitPrice) }}</text></view>
            <view class="detail-row"><text class="detail-label">退款金额</text><text class="detail-value-text amount">¥{{ money(item.refundAmount) }}</text></view>
          </view>
          <view class="detail-summary">
            <view class="detail-summary-row"><text class="detail-summary-label">应退总额</text><text class="detail-summary-value">¥{{ money(detail.refundAmount) }}</text></view>
            <view class="detail-summary-row"><text class="detail-summary-label">已退总额</text><text class="detail-summary-value">¥{{ money(detail.refundedAmount) }}</text></view>
          </view>
        </view>

        <view class="detail-section" v-if="detail.reason">
          <view class="detail-section-title">退货原因</view>
          <view class="detail-remark">{{ detail.reason }}</view>
        </view>

        <view class="detail-section" v-if="detail.remark">
          <view class="detail-section-title">备注</view>
          <view class="detail-remark">{{ detail.remark }}</view>
        </view>

        <view class="detail-footer-placeholder"></view>
        <view class="detail-footer-bar" v-if="detail.status === 'DRAFT' && can('complete')">
          <button class="detail-action-btn primary-btn" @tap="complete(detail)">完成退货</button>
        </view>
      </view>
    </view>

    <!-- ════════ 新建退货单面板（参考 member-purchase form-page） ════════ -->
    <view class="overlay-mask" v-if="panel === 'create'" @tap="closePanel">
      <view class="form-page" @tap.stop>
        <view class="form-hero">
          <view class="form-hero-icon">＋</view>
          <view class="form-hero-info">
            <view class="form-hero-title">新增退货单</view>
            <view class="form-hero-meta">会员服务 · 请完善必要信息后保存</view>
          </view>
          <view class="form-hero-close" @tap="closePanel">✕</view>
        </view>

        <view class="form-section-card">
          <view class="form-section-header"><view class="form-section-dot required"></view><text class="form-section-title">必填信息</text></view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">原购买单</text><text class="form-required-tag">*</text></view>
            <picker :range="purchaseOptions" range-key="label" :value="purchaseIndex" @change="selectPurchase" :disabled="!!fixedPurchaseId"><view class="form-control picker" :class="{ 'has-value': form.purchaseId, 'is-fixed': !!fixedPurchaseId }"><text class="form-picker-text">{{ form.purchaseId ? purchaseOptions[purchaseIndex]?.label : '请选择原购买单' }}</text><text class="form-picker-arrow" v-if="!fixedPurchaseId">›</text></view></picker>
          </view>
          <view class="form-item">
            <view class="form-label-row"><text class="form-label">退货办理核算周期</text><text class="form-required-tag">*</text></view>
            <picker :range="periods" range-key="label" :value="periodIndex" @change="selectPeriod"><view class="form-control picker" :class="{ 'has-value': form.returnPeriodId }"><text class="form-picker-text">{{ form.returnPeriodId ? periods[periodIndex]?.label : '请选择当前机构核算周期' }}</text><text class="form-picker-arrow">›</text></view></picker>
          </view>
        </view>

        <view class="form-section-card" v-if="returnItems.length">
          <view class="form-section-header"><view class="form-section-dot required"></view><text class="form-section-title">退货明细</text><text class="form-section-count">{{ returnItems.length }}项</text></view>
          <view class="form-item" v-for="item in returnItems" :key="item.itemId">
            <view class="form-label-row"><text class="form-label">{{ item.productNameSnapshot || `商品${item.itemId}` }}</text></view>
            <view class="return-item-limits">原正品 {{ quantity(item.purchaseQuantity) }} · 原赠品 {{ quantity(item.giftQuantity) }} · 单价 ¥{{ money(item.unitPrice) }}</view>
            <view class="form-grid-2col">
              <view class="form-item-sub">
                <view class="form-label-row"><text class="form-label">退正品</text></view>
                <input class="form-control input" v-model="item.returnSaleQuantity" type="digit" placeholder="0.000" @input="limit(item,'returnSaleQuantity',$event.detail.value,3)" />
              </view>
              <view class="form-item-sub">
                <view class="form-label-row"><text class="form-label">退赠品</text></view>
                <input class="form-control input" v-model="item.returnGiftQuantity" type="digit" placeholder="0.000" @input="limit(item,'returnGiftQuantity',$event.detail.value,3)" />
              </view>
            </view>
          </view>
        </view>

        <view class="form-section-card">
          <view class="form-section-header collapsible" @tap="remarkCollapsed = !remarkCollapsed"><view class="form-section-dot"></view><text class="form-section-title">其他信息</text><text class="form-section-count">1项</text><text class="form-collapse-arrow" :class="{ collapsed: remarkCollapsed }">›</text></view>
          <view class="form-item" v-if="!remarkCollapsed">
            <view class="form-label-row"><text class="form-label">退货原因</text></view>
            <textarea class="form-control textarea" v-model="form.reason" placeholder="请填写退货原因，可不填" />
          </view>
        </view>

        <view class="form-footer-placeholder"></view>
        <view class="form-footer">
          <button class="form-btn-secondary" @tap="closePanel">取消</button>
          <button class="form-btn-primary" @tap="submit">保存退货单</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { createMemberPurchaseReturn, listMemberPurchaseReturns, getMemberPurchaseReturn } from '@/api/memberPurchaseReturn.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'

const newReturnForm = () => ({ purchaseId: '', returnPeriodId: '', reason: '' })

export default {
  data() {
    return {
      authorized: false, currentDeptId: '', currentDeptName: '',
      rows: [], loading: false, keyword: '', panel: '',
      detail: {}, form: newReturnForm(),
      periods: [], purchaseOptions: [], purchaseIndex: 0, periodIndex: 0, returnItems: [],
      filters: { status: '' }, summary: null,
      pageNum: 1, pageSize: 20, total: 0,
      remarkCollapsed: false, fixedPurchaseId: ''
    }
  },
  computed: {
    statusFilters() { return [{ label: '全部', value: '' }, { label: '草稿', value: 'DRAFT' }, { label: '已完成', value: 'COMPLETED' }, { label: '已作废', value: 'CANCELLED' }] },
    statusFilterIndex() { const i = this.statusFilters.findIndex(x => x.value === this.filters.status); return i < 0 ? 0 : i },
    totalPages() { return Math.max(1, Math.ceil(Number(this.total || 0) / Number(this.pageSize || 1))) }
  },
  onLoad(options) {
    this.authorized = requireModulePermission('memberPurchaseReturn')
    const scope = workContext.snapshot()
    this.currentDeptId = scope.currentDeptId
    this.currentDeptName = scope.currentDept?.name || scope.currentDept?.deptName || '未选择机构'
    this.fixedPurchaseId = options?.purchaseId || ''
    if (this.authorized) {
      this.loadOptions(options?.purchaseId)
      this.load()
    }
  },
  methods: {
    can(action) { return hasActionPermission('memberPurchaseReturn', action) },
    unwrap(res) { return res?.rows || res?.data?.rows || res?.data || [] },
    today() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` },
    dateText(v) { return v ? String(v).replace('T',' ').slice(0,19) : '-' },
    money(v) { return Number(v || 0).toFixed(2) },
    quantity(v) { return Number(v || 0).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1') },
    returnQuantity(row) {
      if (!row) return '-'
      if (row.returnTotalQuantity != null && Number(row.returnTotalQuantity) !== 0) return this.quantity(row.returnTotalQuantity)
      if (row.totalQuantity != null && Number(row.totalQuantity) !== 0) return this.quantity(row.totalQuantity)
      if (row.returnQuantity != null && Number(row.returnQuantity) !== 0) return this.quantity(row.returnQuantity)
      if (Array.isArray(row.items) && row.items.length) {
        const s = row.items.reduce((acc, it) => acc + Number(it.returnTotalQuantity || it.totalQuantity || it.returnSaleQuantity || 0) + Number(it.returnGiftQuantity || 0), 0)
        return s ? this.quantity(s) : '-'
      }
      return '-'
    },
    statusText(v) { return ({ DRAFT: '草稿', PENDING: '待审核', APPROVED: '已批准', COMPLETED: '已完成', REFUNDED: '已退款', CANCELLED: '已作废' })[v] || v || '-' },
    statusClass(v) { const s = String(v ?? ''); return s === 'COMPLETED' || s === 'REFUNDED' ? 'status-ok' : s === 'CANCELLED' ? 'status-danger' : s === 'DRAFT' ? 'status-warn' : 'status-info' },
    periodLabel(v) { return `${v.periodNo || v.periodId}（${this.dateText(v.startTime)} 至 ${v.endTime ? this.dateText(v.endTime) : '当前'}）` },
    limit(item, key, value, precision) { const s = String(value || '').replace(/[^\d.]/g,'').replace(/\.(?=.*\.)/g,''); item[key] = s.includes('.') ? s.split('.')[0] + '.' + s.split('.')[1].slice(0, precision) : s },

    async load() {
      this.loading = true
      try {
        const res = await listMemberPurchaseReturns(this.listParams())
        this.rows = this.unwrap(res)
        this.total = Number(res?.total ?? 0) || 0
      } finally { this.loading = false }
      this.loadStatistics()
    },
    listParams() {
      const params = {
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        deptId: this.currentDeptId
      }
      if (this.filters.status) params.status = this.filters.status
      if (this.keyword) params.customerName = this.keyword
      return params
    },
    async loadStatistics() {
      try {
        const res = await listMemberPurchaseReturns({ pageNum: 1, pageSize: 200, deptId: this.currentDeptId, silent: true })
        const rows = this.unwrap(res)
        this.summary = {
          returnCount: rows.length,
          refundAmount: rows.reduce((s, r) => s + Number(r.refundAmount || 0), 0),
          refundedAmount: rows.reduce((s, r) => s + Number(r.refundedAmount || 0), 0)
        }
      } catch (e) { this.summary = null }
    },
    async loadOptions(purchaseId) {
      const [periods, purchases] = await Promise.all([
        request({ url: '/finance/accountingPeriod/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId: this.currentDeptId } }),
        request({ url: '/member/purchase/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId: this.currentDeptId } })
      ])
      this.periods = this.unwrap(periods).filter(x => String(x.deptId) === String(this.currentDeptId)).map(x => ({ ...x, label: this.periodLabel(x) }))
      this.purchaseOptions = this.unwrap(purchases).map(x => ({ ...x, label: `${x.purchaseNo || x.purchaseId} · ${x.customerName || '未登记顾客'}` }))
      
      // 自动选中第一个周期
      if (this.periods.length && !this.form.returnPeriodId) {
        this.periodIndex = 0
        this.form.returnPeriodId = this.periods[0].periodId
      }
      
      if (purchaseId) {
        const i = this.purchaseOptions.findIndex(x => String(x.purchaseId) === String(purchaseId))
        if (i >= 0) { this.purchaseIndex = i; await this.selectPurchase({ detail: { value: i } }) }
      }
    },

    selectStatusFilter(e) { this.filters.status = this.statusFilters[Number(e.detail.value)].value; this.pageNum = 1; this.load() },
    resetFilters() { this.keyword = ''; this.filters = { status: '' }; this.pageNum = 1; this.load() },
    prevPage() { if (this.pageNum <= 1) return; this.pageNum--; this.load() },
    nextPage() { if (this.pageNum >= this.totalPages) return; this.pageNum++; this.load() },

    async selectPurchase(e) {
      this.purchaseIndex = Number(e.detail.value)
      const p = this.purchaseOptions[this.purchaseIndex]
      this.form.purchaseId = p?.purchaseId || ''
      if (!p) { this.returnItems = []; return }
      const r = await request({ url: `/member/purchase/${p.purchaseId}`, method: 'GET' })
      const d = r.data || r
      this.returnItems = (d.items || []).map(x => ({ ...x, returnSaleQuantity: '', returnGiftQuantity: '' }))
    },
    selectPeriod(e) { this.periodIndex = Number(e.detail.value); this.form.returnPeriodId = this.periods[this.periodIndex]?.periodId || '' },

    openCreate() { this.fixedPurchaseId = ''; this.form = newReturnForm(); this.periodIndex = 0; this.purchaseIndex = 0; this.returnItems = []; if (this.periods.length) { this.form.returnPeriodId = this.periods[0].periodId } this.panel = 'create' },
    async openDetail(row) {
      try {
        const res = await getMemberPurchaseReturn(row.returnId)
        this.detail = res.data || res
      } catch (e) { this.detail = row }
      this.panel = 'detail'
    },
    closePanel() { this.panel = '' },

    async submit() {
      const items = this.returnItems
        .filter(x => Number(x.returnSaleQuantity || 0) > 0 || Number(x.returnGiftQuantity || 0) > 0)
        .map(x => ({ itemId: x.itemId, returnSaleQuantity: Number(x.returnSaleQuantity || 0), returnGiftQuantity: Number(x.returnGiftQuantity || 0) }))
      if (!this.form.purchaseId || !this.form.returnPeriodId || !items.length) {
        return uni.showToast({ title: '请选择原购买单、办理周期并填写退货数量', icon: 'none' })
      }
      await createMemberPurchaseReturn({ ...this.form, items, idempotencyKey: `mp-return-${Date.now()}` })
      uni.showToast({ title: '退货单已保存', icon: 'success' })
      this.closePanel()
      this.load()
    },
    async complete(row) {
      const ok = await new Promise(resolve => uni.showModal({
        title: '操作确认',
        content: `确认完成退货单 ${row.returnNo || row.returnId} 吗？`,
        success: r => resolve(r.confirm)
      }))
      if (!ok) return
      try {
        await request({ url: `/member/purchase-return/${row.returnId}/complete`, method: 'PUT' })
        uni.showToast({ title: '退货单已完成', icon: 'success' })
        this.closePanel()
        this.load()
      } catch (e) {}
    }
  }
}
</script>

<style scoped>
/* ──────────────────────────────────────────────
 * 通用业务页皮肤：与会员购买记录/销售记录保持一致
 * ────────────────────────────────────────────── */
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:#e7eff7;color:#1e293b;box-sizing:border-box;overflow:hidden}

/* ── 顶部标题栏 ── */
.hero{margin:22rpx 30rpx 0;padding:28rpx 30rpx 30rpx;border-left:5rpx solid #1687f5;border-radius:20rpx;background:linear-gradient(110deg,#d9eaff,#f7faff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08)}
.eyebrow{display:block;color:#1687f5;font-size:24rpx;font-weight:600}
.hero-title{display:block;margin-top:10rpx;color:#1e293b;font-size:38rpx;font-weight:700}

/* ── 部门范围条 ── */
.work-scope{display:flex;align-items:center;margin:20rpx 30rpx 0;min-height:44rpx}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:16rpx;border-radius:50%;background:#1687f5}
.work-scope-copy{display:flex;align-items:baseline;color:#8192a6;font-size:24rpx}
.work-scope-name{margin-left:4rpx;color:#26384d;font-size:27rpx;font-weight:700}

/* ── 通用卡片容器 ── */
.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-top:24rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box;overflow:hidden}
.section-header{display:flex;align-items:center;gap:12rpx;margin-bottom:18rpx}
.section-dot{width:12rpx;height:12rpx;border-radius:50%;flex-shrink:0}
.section-title{font-size:28rpx;font-weight:700;color:#1A2332;flex:1}
.section-link{font-size:22rpx;color:#94A3B8}

/* ── 筛选区 ── */
.filters-card{margin:16rpx 30rpx 0!important;padding:22rpx 24rpx!important}
.filter-row{display:flex;align-items:center;gap:10rpx;min-height:66rpx;width:100%;box-sizing:border-box}
.filter-row+.filter-row{margin-top:10rpx}
.filter-row-tools{gap:12rpx;margin-top:14rpx}
.filter-row-merged{gap:10rpx}
.filter-row-merged .filter-type-picker{flex:0 0 180rpx}
.filter-row-merged .filter-kw{flex:1}
.filter-type-picker{flex:1;min-width:0}
.filter-picker{box-sizing:border-box!important;padding:16rpx 14rpx;height:64rpx;line-height:32rpx;border:1rpx solid #D5E0EC;border-radius:12rpx;background:#F8FBFD;color:#5A6B7F;font-size:23rpx;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;width:100%;display:flex;align-items:center;justify-content:space-between}
.filter-kw{box-sizing:border-box!important;height:64rpx;line-height:64rpx;padding:0 14rpx;border:1rpx solid #D5E0EC;border-radius:12rpx;background:#F8FBFD;color:#1A2332;font-size:23rpx;width:100%}
.filter-chevron{color:#94a3b8;font-size:25rpx;margin-left:8rpx}
.filter-kw{flex:2}
.filter-button{flex:none;margin:0;padding:0 16rpx;height:64rpx;line-height:64rpx;border:0;border-radius:32rpx;background:#087CF0;color:#fff;font-size:22rpx;white-space:nowrap}
.filter-button-ghost{background:#EEF3F8;color:#334155}

/* ── 汇总条 ── */
.summary-bar{display:flex;margin:16rpx 30rpx 0;padding:18rpx 8rpx;background:#fff;border-radius:18rpx;border:1rpx solid #dbe6f1;box-sizing:border-box}
.summary-bar>view{flex:1;text-align:center;border-right:1rpx solid #edf1f5;min-width:0}
.summary-bar>view:last-child{border-right:0}
.summary-value{display:block;color:#1687f5;font-size:28rpx;font-weight:700;font-variant-numeric:tabular-nums;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.summary-value.success{color:#10B981}
.summary-value.warning{color:#F59E0B}
.summary-label{display:block;margin-top:6rpx;color:#98a9ba;font-size:20rpx}

/* ── 操作行 ── */
/* ── 浮动底部操作栏 ── */
.scroll-pad{height:16rpx;margin:16rpx 0 0}
.bottom-bar{position:fixed;left:0;right:0;bottom:0;display:flex;justify-content:center;gap:16rpx;padding:20rpx 24rpx;padding-bottom:calc(20rpx + env(safe-area-inset-bottom));background:rgba(255,255,255,.96);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-top:1rpx solid #E2E8F0;z-index:10}
.bottom-bar .add-button{width:320rpx;height:84rpx;line-height:84rpx;background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#FFF;font-size:28rpx;border-radius:999rpx;text-align:center;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25);border:0;padding:0}
.bottom-bar .add-button::after{border:none}
.scroll{padding-bottom:160rpx!important}

/* ── 滚动列表区 ── */
.scroll{flex:1;width:100%;min-height:0;padding:0 30rpx 160rpx!important;box-sizing:border-box;overflow-x:hidden}
.list-card{margin-top:16rpx!important;padding:20rpx 28rpx!important}
.state-card{padding:20rpx 28rpx 28rpx!important}

/* ── 标准记录卡片 ── */
.record-card{display:flex;margin-bottom:16rpx;background:#FFFFFF;border-radius:20rpx;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);overflow:hidden}
.card-bar{width:4rpx;background:linear-gradient(180deg,#087CF0,#A8C7E5);flex-shrink:0}
.card-body{flex:1;padding:24rpx 28rpx;box-sizing:border-box}
.card-header{display:flex;align-items:flex-start;justify-content:space-between;gap:20rpx}
.record-title{flex:1;font-size:30rpx;line-height:42rpx;font-weight:700;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}
.record-id{padding:4rpx 14rpx;background:#E8EEF5;color:#5A6B7F;font-size:20rpx;border-radius:999rpx;flex-shrink:0;font-variant-numeric:tabular-nums}
.summary-grid{display:grid;grid-template-columns:1fr 1fr;gap:12rpx;margin-top:16rpx}
.summary-item{display:flex;flex-direction:column;gap:6rpx}
.summary-label{font-size:22rpx;color:#94A3B8}
.summary-value{font-size:26rpx;color:#1A2332;font-weight:500;font-variant-numeric:tabular-nums}
.summary-value.tone-money{color:#B45309;font-weight:700}
.summary-value.tone-danger{color:#B91C1C;font-weight:700}
.summary-value.status-ok{color:#047857;font-weight:700}
.summary-value.status-warn{color:#B45309;font-weight:700}
.card-footer{display:flex;justify-content:space-between;align-items:center;margin-top:16rpx;padding-top:14rpx;border-top:1rpx solid #E8EEF5}
.meta-text{font-size:24rpx;color:#94A3B8}
.arrow-icon{font-size:36rpx;color:#CBD5E1;font-weight:300;line-height:1}

/* ── 紧凑2行卡片样式 ── */
.compact-body{flex:1;padding:20rpx 24rpx}
.compact-title{font-size:33rpx;line-height:44rpx;font-weight:700;color:#1A2332;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.compact-row1{display:grid;grid-template-columns:1fr 180rpx 140rpx;align-items:center;gap:8rpx}
.compact-row2{display:grid;grid-template-columns:1fr 180rpx 140rpx;align-items:center;gap:8rpx;margin-top:8rpx}
.compact-row2 .date{text-align:left}
.compact-row2 .paid{text-align:left}
.compact-row1 .compact-qty-group{justify-self:start}
.compact-row2 .compact-status{justify-self:end}
.compact-qty-group{display:inline-flex;align-items:baseline;gap:2rpx;white-space:nowrap}
.compact-qty-label{font-size:22rpx;color:#94A3B8;font-weight:400}
.compact-qty-value{font-size:30rpx;color:#1A2332;font-weight:700;margin-right:8rpx}
.compact-amount{font-size:33rpx;color:#DC2626;font-weight:700;text-align:right;white-space:nowrap}
.compact-meta{flex-shrink:0;font-size:23rpx;line-height:32rpx;color:#94A3B8}
.compact-meta.date{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.compact-meta.paid{color:#059669;font-weight:600}
.compact-id{flex-shrink:0;font-size:21rpx;color:#5A6B7F;background:#E8EEF5;padding:2rpx 12rpx;border-radius:999rpx}
.compact-status{flex-shrink:0;padding:4rpx 14rpx;border-radius:999rpx;font-size:21rpx;line-height:30rpx;background:#E8EEF5;color:#5A6B7F}
.compact-status.status-ok{background:#D1FAE5;color:#065F46}
.compact-status.status-warn{background:#FEF3C7;color:#92400E}
.compact-status.status-danger{background:#FEE2E2;color:#991B1B}
.pagination-card{margin-top:4rpx}

/* ── 空状态 / 分页 ── */
.empty{text-align:center;color:#94a3b8;padding:56rpx 0;font-size:23rpx}
.pagination{display:flex;align-items:center;justify-content:center;gap:18rpx;padding:24rpx 0 0;color:#64748b;font-size:24rpx}
.pagination button{border:0;border-radius:12rpx;padding:10rpx 28rpx;background:#eef3f8;color:#334155;margin:0}
.pagination button[disabled]{opacity:.5}
.page-info{min-width:140rpx;text-align:center}

/* ════════════════════════════════════════════════
 * 详情/表单面板样式（与 member-purchase 一致）
 * ════════════════════════════════════════════════ */
.overlay-mask{position:fixed;inset:0;background:rgba(15,23,42,.45);z-index:50;overflow:hidden}

/* ────────────────── 详情页 ────────────────── */
.detail-page{height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box;overflow-y:auto;-webkit-overflow-scrolling:touch}
.detail-hero{position:relative;margin:24rpx 28rpx;border-radius:20rpx;overflow:hidden}
.detail-hero-bg{position:absolute;inset:0;background:linear-gradient(135deg,#087CF0,#5AA9E8,#A8C7E5);border-radius:20rpx}
.detail-hero-content{position:relative;z-index:1;padding:40rpx 36rpx}
.detail-hero-eyebrow{font-size:22rpx;color:rgba(255,255,255,.7);margin-bottom:12rpx;letter-spacing:2rpx}
.detail-hero-title{font-size:36rpx;font-weight:600;color:#fff;margin-bottom:16rpx;line-height:1.4;overflow-wrap:anywhere;word-break:break-word}
.detail-hero-value{font-size:52rpx;font-weight:700;color:#fff;margin-bottom:12rpx}
.detail-hero-meta{font-size:24rpx;color:rgba(255,255,255,.7)}
.detail-close{position:absolute;top:24rpx;right:24rpx;width:56rpx;height:56rpx;line-height:56rpx;text-align:center;border-radius:50%;background:rgba(0,0,0,.2);color:#fff;font-size:30rpx;z-index:2}

.detail-section{margin:20rpx 28rpx 0;background:#fff;border-radius:20rpx;padding:28rpx 32rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,.06);box-sizing:border-box}
.detail-section-title{font-size:28rpx;font-weight:600;color:#1A2332;margin-bottom:20rpx;padding-left:16rpx;border-left:4rpx solid #087CF0}

.detail-highlight-grid{display:flex;flex-wrap:wrap;gap:12rpx}
.detail-highlight-item{flex:1;min-width:45%;background:#F5F8FA;border-radius:12rpx;padding:18rpx 20rpx;box-sizing:border-box}
.detail-highlight-label{font-size:22rpx;color:#94A3B8;margin-bottom:6rpx}
.detail-highlight-value{font-size:28rpx;font-weight:600;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}
.detail-highlight-value.status-ok{display:inline-block;background:#D1FAE5;color:#065F46;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-warn{display:inline-block;background:#FEF3C7;color:#92400E;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-danger{display:inline-block;background:#FEE2E2;color:#991B1B;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-info{display:inline-block;background:#E0F2FE;color:#075985;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.tone-success{color:#059669}
.detail-highlight-value.tone-warning{color:#C26A1B}

.detail-item{background:#F5F8FA;border-radius:16rpx;padding:24rpx;margin-bottom:20rpx;box-sizing:border-box}
.detail-item:last-child{margin-bottom:0}
.detail-item-header{display:flex;align-items:center;justify-content:space-between;padding-bottom:16rpx;border-bottom:1rpx solid #E2E8F0;margin-bottom:16rpx}
.detail-item-title{font-size:28rpx;font-weight:700;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}
.detail-row{display:flex;align-items:center;min-height:72rpx;padding:16rpx 0;box-sizing:border-box;line-height:34rpx}
.detail-label{width:140rpx;font-size:26rpx;color:#64748B;flex-shrink:0}
.detail-value-text{font-size:26rpx;color:#1A2332;flex:1;overflow-wrap:anywhere;word-break:break-word}
.detail-value-text.amount{font-weight:700;color:#087CF0}

.detail-summary{margin-top:24rpx;padding-top:24rpx;border-top:2rpx solid #E2E8F0}
.detail-summary-row{display:flex;align-items:center;padding:12rpx 0}
.detail-summary-label{flex:1;font-size:28rpx;color:#1A2332;font-weight:500}
.detail-summary-value{font-size:32rpx;color:#087CF0;font-weight:700}

.detail-remark{font-size:26rpx;color:#475569;line-height:1.6;white-space:pre-wrap;word-break:break-word;background:#F5F8FA;border-radius:12rpx;padding:20rpx}

.detail-footer-placeholder{height:140rpx}
.detail-footer-bar{position:fixed;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;gap:16rpx;padding:16rpx 24rpx;padding-bottom:calc(16rpx + env(safe-area-inset-bottom));background:#fff;box-shadow:0 -2rpx 16rpx rgba(8,124,240,.06);z-index:100}
.detail-action-btn{flex:1;height:76rpx;line-height:76rpx;font-size:28rpx;font-weight:500;border-radius:16rpx;text-align:center;border:none;margin:0;padding:0}
.detail-action-btn::after{border:none}
.detail-action-btn.edit-btn{background:#E8EEF5;color:#087CF0}
.detail-action-btn.primary-btn{background:#087CF0;color:#fff}

/* ────────────────── 表单页 ────────────────── */
.form-page{min-height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box}
.form-hero{display:flex;align-items:center;gap:24rpx;padding:36rpx 28rpx;background:linear-gradient(135deg,#C7DCF2 0%,#EAF3FC 100%);border-top:1rpx solid #B7D1EB;border-radius:0 0 24rpx 24rpx;position:relative}
.form-hero-icon{width:72rpx;height:72rpx;line-height:72rpx;text-align:center;background:rgba(255,255,255,.15);border-radius:18rpx;font-size:36rpx;color:#fff;flex-shrink:0;display:flex;align-items:center;justify-content:center}
.form-hero-info{flex:1;min-width:0}
.form-hero-title{font-size:34rpx;font-weight:700;color:#1F2D3D}
.form-hero-meta{margin-top:6rpx;font-size:22rpx;color:#6E8197}
.form-hero-close{position:absolute;top:24rpx;right:24rpx;width:56rpx;height:56rpx;line-height:56rpx;text-align:center;border-radius:50%;background:rgba(255,255,255,.3);color:#5A6B7F;font-size:30rpx}

.form-section-card{margin:20rpx 28rpx 0;padding:28rpx;background:#fff;border-radius:20rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box}
.form-section-header{display:flex;align-items:center;gap:10rpx;margin-bottom:20rpx}
.form-section-header.collapsible{padding:4rpx 0}
.form-section-dot{width:8rpx;height:8rpx;border-radius:50%;background:#087CF0;flex-shrink:0}
.form-section-dot.required{background:#EF4444}
.form-section-title{font-size:28rpx;font-weight:600;color:#1A2332;flex:1}
.form-section-count{margin-left:auto;font-size:22rpx;color:#94A3B8}
.form-collapse-arrow{font-size:32rpx;color:#94A3B8;transform:rotate(90deg);transition:transform .2s;margin-left:8rpx}
.form-collapse-arrow.collapsed{transform:rotate(0deg)}

.form-item{padding-top:20rpx;border-top:1rpx solid #E8EEF5}
.form-item + .form-item{border-top:1rpx solid #E8EEF5;padding-top:20rpx}
.form-item:first-child{border-top:0;padding-top:0}
.form-item-sub{padding-top:0;border-top:0}
.form-label-row{display:flex;align-items:center;gap:8rpx;margin-bottom:12rpx}
.form-label{font-size:24rpx;color:#5A6B7F;font-weight:500}
.form-required-tag{color:#EF4444;font-size:26rpx;font-weight:700}

.form-control{width:100%;min-height:84rpx;padding:0 24rpx;background:#F5F8FA;border:2rpx solid #E2E8F0;border-radius:14rpx;font-size:28rpx;color:#1A2332;box-sizing:border-box!important;transition:border-color .2s}
.form-control.input{display:block;width:100%;height:84rpx;line-height:84rpx}
.form-control.textarea{height:170rpx;padding-top:20rpx;line-height:1.5}
.form-control.picker{display:flex;align-items:center;justify-content:space-between;line-height:84rpx}
.form-control.picker.is-fixed{background:#EEF2F7;border-color:#D5DFE9;color:#475569}
.form-picker-text{flex:1;color:#1A2332;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.form-control.picker:not(.has-value) .form-picker-text{color:#94A3B8}
.form-picker-arrow{font-size:32rpx;color:#CBD5E1;font-weight:300;margin-left:8rpx;flex-shrink:0}

.form-grid-2col{display:grid;grid-template-columns:1fr 1fr;gap:18rpx;box-sizing:border-box}
.form-grid-2col .form-item-sub{border-top:0!important;padding-top:0!important}
.return-item-limits{font-size:22rpx;color:#64748b;margin-bottom:12rpx;line-height:1.5}

.form-footer-placeholder{height:140rpx}
.form-footer{position:fixed;left:0;right:0;bottom:0;display:flex;gap:16rpx;padding:18rpx 24rpx;padding-bottom:calc(18rpx + env(safe-area-inset-bottom));background:rgba(255,255,255,.96);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-top:1rpx solid #E2E8F0;z-index:100}
.form-btn-primary,.form-btn-secondary{flex:1;height:88rpx;line-height:88rpx;font-size:28rpx;border-radius:999rpx;text-align:center;border:none;margin:0;padding:0}
.form-btn-primary{background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#fff;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25)}
.form-btn-secondary{background:#fff;color:#5A6B7F;border:1rpx solid #E2E8F0}
.form-btn-primary::after,.form-btn-secondary::after{border:none}
</style>
