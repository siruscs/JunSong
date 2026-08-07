<template>
  <view class="page" v-if="authorized">
    <!-- 顶部标题栏（渐变背景 + 左边框，与销售记录页一致） -->
    <view class="hero">
      <text class="eyebrow">会员营销</text>
      <text class="hero-title">销售政策</text>
    </view>

    <!-- 部门范围条 -->
    <view class="work-scope">
      <view class="work-scope-mark"></view>
      <view class="work-scope-copy">
        <text class="work-scope-label">当前部门 · </text>
        <text class="work-scope-name">{{ deptName }}</text>
      </view>
    </view>

    <!-- 筛选区卡片 -->
    <view class="section-card filters-card">
      <!-- 筛选合并为一行：状态 + 关键字 + 查询/重置 -->
      <view class="filter-row filter-row-inline">
        <picker class="filter-type-picker" :range="statusFilters" range-key="label" :value="statusFilterIndex" @change="selectStatusFilter">
          <view class="filter-picker">{{ statusFilters[statusFilterIndex]?.label || '全部状态' }}<text class="filter-chevron">⌄</text></view>
        </picker>
        <input class="filter-kw" v-model="keyword" placeholder="政策名称、编号或商品" confirm-type="search" @confirm="applyFilter" />
        <button class="filter-button" @tap="applyFilter">查询</button>
        <button class="filter-button filter-button-ghost" @tap="resetFilters">重置</button>
      </view>
    </view>

    <view class="scroll-pad"></view>
    <view class="bottom-bar">
      <button v-if="can('add')" class="add-button" @tap="add">＋ 新增</button>
    </view>

    <!-- 滚动列表区 -->
    <scroll-view scroll-y class="scroll">
      <!-- 政策列表 -->
      <view class="record-card" v-for="row in filteredRows" :key="row.policyId" @tap="openDetail(row)">
        <view class="card-bar"></view>
        <view class="card-body">
          <view class="card-header">
            <view class="record-title">{{ row.policyName }}</view>
            <view class="record-id">NO. {{ row.policyNo || '-' }}</view>
          </view>
          <!-- 套餐档位：单独一行显示 -->
          <view class="packages-row">
            <text class="packages-label">套餐档位</text>
            <view class="packages-tags">
              <text class="package-tag" v-for="(p, idx) in (row.packages || [])" :key="idx">
                买{{ quantity(p.purchaseQuantity) }}送{{ quantity(p.giftQuantity) }} / {{ p.packagePrice ? money(p.packagePrice) : '按单价' }}
              </text>
              <text class="packages-empty" v-if="!(row.packages || []).length">无套餐</text>
            </view>
          </view>
          <view class="summary-grid summary-grid-2col">
            <view class="summary-item">
              <text class="summary-label">商品名称</text>
              <text class="summary-value">{{ productName(row.productId) }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">状态</text>
              <text class="summary-value" :class="row.status === '1' ? 'tone-ok' : 'tone-warn'">{{ row.status === '1' ? '已启用' : '已停用' }}</text>
            </view>
          </view>
        </view>
      </view>
      <!-- 空状态 -->
      <view class="section-card list-card state-card" v-if="!filteredRows.length">
        <view class="empty">暂无销售政策</view>
      </view>
      <view class="list-total" v-if="filteredRows.length">共 {{ filteredRows.length }} 条</view>
    </scroll-view>

    <!-- ════════ 详情面板（支持右滑返回） ════════ -->
    <view class="overlay-mask" v-if="panel === 'detail'" @tap="closePanel"
          @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd" @touchcancel="onTouchEnd">
      <view class="detail-page" :class="{ 'swipe-closing': swipeClosing }" @tap.stop
            :style="{ transform: swipeOffset ? `translateX(${swipeOffset}px)` : 'none', transition: swiping ? 'none' : 'transform 0.3s ease' }">
        <!-- hero区 -->
        <view class="detail-hero">
          <view class="detail-hero-bg"></view>
          <view class="detail-hero-content">
            <view class="detail-hero-eyebrow">销售政策 · {{ detail.status === '1' ? '启用中' : '已停用' }}</view>
            <view class="detail-hero-title">{{ detail.policyName }}</view>
            <view class="detail-hero-value">{{ (detail.packages || []).length }} 档套餐</view>
            <view class="detail-hero-meta">{{ productName(detail.productId) }}</view>
          </view>
        </view>
        <!-- 概要信息 -->
        <view class="detail-section">
          <view class="detail-section-title">概要信息</view>
          <view class="detail-highlight-grid">
            <view class="detail-highlight-item"><view class="detail-highlight-label">政策编号</view><view class="detail-highlight-value">{{ detail.policyNo || '-' }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">商品名称</view><view class="detail-highlight-value">{{ productName(detail.productId) }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">套餐档数</view><view class="detail-highlight-value">{{ (detail.packages || []).length }}</view></view>
            <view class="detail-highlight-item"><view class="detail-highlight-label">状态</view><view class="detail-highlight-value">{{ detail.status === '1' ? '启用中' : '已停用' }}</view></view>
          </view>
        </view>
        <!-- 套餐明细 -->
        <view class="detail-section" v-if="(detail.packages || []).length">
          <view class="detail-section-title">套餐明细</view>
          <view class="detail-item" v-for="(p, idx) in detail.packages || []" :key="idx">
            <view class="detail-item-header"><text class="detail-item-title">{{ p.packageName || `套餐${idx+1}` }}</text></view>
            <view class="detail-row"><text class="detail-label">购买数量</text><text class="detail-value-text">{{ quantity(p.purchaseQuantity) }}</text></view>
            <view class="detail-row"><text class="detail-label">赠送数量</text><text class="detail-value-text">{{ quantity(p.giftQuantity) }}</text></view>
            <view class="detail-row"><text class="detail-label">套餐价格</text><text class="detail-value-text amount">¥{{ money(p.packagePrice) }}</text></view>
          </view>
        </view>
        <!-- 底部占位（给固定按钮留空间） -->
        <view class="detail-footer-placeholder"></view>
      </view>
      <!-- 底部操作按钮：固定在overlay-mask内，独立于滚动容器 -->
      <view class="detail-footer-bar">
        <button v-if="can('edit')" class="detail-action-btn primary-btn" @tap="edit(detail)">编辑</button>
        <button v-if="can('remove')" class="detail-action-btn danger-btn" @tap="deletePolicy(detail)">删除</button>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'

export default {
  data() {
    return {
      authorized: false,
      rows: [],
      products: [],
      deptName: '',
      deptId: '',
      keyword: '',
      statusFilter: '',
      panel: '',
      detail: {},
      // 右滑返回相关
      swiping: false,
      swipeOffset: 0,
      swipeClosing: false,
      touchStartX: 0,
      touchStartY: 0,
      statusFilters: [
        { label: '全部状态', value: '' },
        { label: '启用', value: '1' },
        { label: '停用', value: '0' }
      ]
    }
  },
  computed: {
    // 状态筛选 picker 当前索引
    statusFilterIndex() {
      const i = this.statusFilters.findIndex(x => x.value === this.statusFilter)
      return i < 0 ? 0 : i
    },
    // 关键字 + 状态联合筛选结果
    filteredRows() {
      const kw = (this.keyword || '').trim().toLowerCase()
      const st = this.statusFilter
      return (this.rows || []).filter(row => {
        if (st && String(row.status) !== String(st)) return false
        if (!kw) return true
        const hay = `${row.policyName || ''} ${row.policyNo || ''} ${row.productName || ''} ${row.productId || ''}`.toLowerCase()
        return hay.includes(kw)
      })
    }
  },
  onLoad() {
    this.authorized = requireModulePermission('campaignPolicy')
    const s = workContext.snapshot()
    this.deptId = s.currentDeptId
    this.deptName = s.currentDept?.name || s.currentDept?.deptName || '未选择机构'
    if (this.authorized) this.init()
  },
  onShow() {
    if (this.authorized) this.load()
  },
  methods: {
    can(a) { return hasActionPermission('campaignPolicy', a) },
    quantity(v) { return Number(v || 0).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1') },
    money(v) { return Number(v || 0).toFixed(2) },
    productName(productId) {
      if (!productId) return '-'
      const p = this.products.find(x => String(x.productId) === String(productId))
      return p?.productName || productId
    },
    totalPurchaseQuantity(packages) {
      const list = packages || []
      const total = list.reduce((sum, p) => sum + Number(p.purchaseQuantity || 0), 0)
      return this.quantity(total)
    },
    unwrap(r) { return r?.rows || r?.data?.rows || r?.data || [] },
    async init() {
      try {
        const r = await request({ url: '/finance/product/list', method: 'GET', data: { pageNum: 1, pageSize: 500, deptId: this.deptId } })
        this.products = this.unwrap(r)
      } catch (e) {
        this.products = []
      }
      this.load()
    },
    async load() {
      const r = await request({ url: '/member/campaign/policy/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId: this.deptId } })
      this.rows = r.rows || r.data || []
    },
    selectStatusFilter(e) { this.statusFilter = this.statusFilters[Number(e.detail.value)].value },
    applyFilter() { /* 计算属性自动响应 */ },
    resetFilters() { this.keyword = ''; this.statusFilter = '' },
    add() { uni.navigateTo({ url: '/pages/campaign-policy/form' }) },
    openDetail(row) { this.detail = row; this.panel = 'detail' },
    closePanel() {
      this.swipeClosing = false
      this.swipeOffset = 0
      this.panel = ''
    },
    // 右滑返回相关方法
    onTouchStart(e) {
      const touch = e.touches[0]
      this.touchStartX = touch.clientX
      this.touchStartY = touch.clientY
      this.swiping = false
      this.swipeOffset = 0
    },
    onTouchMove(e) {
      const touch = e.touches[0]
      const dx = touch.clientX - this.touchStartX
      const dy = touch.clientY - this.touchStartY
      const screenWidth = uni.getSystemInfoSync().windowWidth || 375
      // 从左边缘开始，或水平距离大于垂直距离且向右滑超过30px
      if ((this.touchStartX < 50 || Math.abs(dx) > Math.abs(dy)) && dx > 30) {
        this.swiping = true
        this.swipeOffset = Math.min(dx, screenWidth * 0.8)
      }
    },
    onTouchEnd() {
      const screenWidth = uni.getSystemInfoSync().windowWidth || 375
      // 右滑超过屏幕宽度的30%即关闭
      if (this.swiping && this.swipeOffset > screenWidth * 0.3) {
        this.swipeClosing = true
        this.swipeOffset = screenWidth
        setTimeout(() => {
          this.closePanel()
        }, 280)
      } else {
        this.swipeOffset = 0
        this.swiping = false
      }
    },
    edit(r) { uni.navigateTo({ url: `/pages/campaign-policy/form?id=${r.policyId}` }) },
    async deletePolicy(r) {
      const confirmed = await new Promise((resolve) => {
        uni.showModal({
          title: '删除确认',
          content: `确认删除销售政策"${r.policyName}"吗？删除后不可恢复。`,
          confirmColor: '#dc2626',
          success: (res) => resolve(res.confirm)
        })
      })
      if (!confirmed) return
      try {
        await request({ url: `/member/campaign/policy/${r.policyId}`, method: 'DELETE' })
        uni.showToast({ title: '已删除', icon: 'success' })
        this.closePanel()
        await this.load()
      } catch (e) {
        uni.showToast({ title: e?.message || '删除失败', icon: 'none' })
      }
    }
  }
}
</script>

<style scoped>
/* ──────────────────────────────────────────────
 * 通用业务页皮肤：与销售记录/库存流水保持一致
 * 蓝色主题 #087CF0，背景 #e7eff7
 * ────────────────────────────────────────────── */
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:#e7eff7;color:#1e293b;box-sizing:border-box;overflow:hidden}

/* ── 顶部标题栏（左边框 + 浅蓝渐变） ── */
.hero{margin:22rpx 30rpx 0;padding:28rpx 30rpx 30rpx;border-left:5rpx solid #1687f5;border-radius:20rpx;background:linear-gradient(110deg,#d9eaff,#f7faff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08)}
.eyebrow{display:block;color:#1687f5;font-size:24rpx;font-weight:600}
.hero-title{display:block;margin-top:10rpx;color:#1e293b;font-size:38rpx;font-weight:700}

/* ── 部门范围条 ── */
.work-scope{display:flex;align-items:center;margin:20rpx 30rpx 0;min-height:44rpx}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:16rpx;border-radius:50%;background:#1687f5}
.work-scope-copy{display:flex;align-items:baseline;color:#8192a6;font-size:24rpx}
.work-scope-name{margin-left:4rpx;color:#26384d;font-size:27rpx;font-weight:700}

/* ── 通用卡片容器（section-card） ── */
.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin-top:24rpx;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box;overflow:hidden}
.section-header{display:flex;align-items:center;gap:12rpx;margin-bottom:18rpx}
.section-dot{width:12rpx;height:12rpx;border-radius:50%;flex-shrink:0}
.section-title{font-size:28rpx;font-weight:700;color:#1A2332;flex:1}
.section-link{font-size:22rpx;color:#94A3B8}

/* ── 筛选区卡片 ── */
.filters-card{margin:16rpx 30rpx 0!important;padding:22rpx 24rpx!important}
.filter-row{display:flex;align-items:center;gap:10rpx;min-height:66rpx;width:100%;box-sizing:border-box}
.filter-row+.filter-row{margin-top:10rpx}
.filter-row-inline{flex-wrap:nowrap}
.filter-row-tools{gap:12rpx;margin-top:14rpx}
.filter-type-picker{flex:1;min-width:0}
.filter-picker,.filter-kw{box-sizing:border-box!important;padding:16rpx 14rpx;height:64rpx;line-height:32rpx;border:1rpx solid #D5E0EC;border-radius:12rpx;background:#F8FBFD;color:#5A6B7F;font-size:22rpx;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.filter-picker{display:flex;align-items:center;justify-content:space-between;flex:1;min-width:0}
.filter-chevron{color:#94a3b8;font-size:25rpx;margin-left:8rpx}
.filter-kw{flex:2;min-width:0}
.filter-button{flex:none;margin:0;padding:0 20rpx;height:64rpx;line-height:64rpx;border:0;border-radius:32rpx;background:#087CF0;color:#fff;font-size:22rpx;white-space:nowrap}
.filter-button-ghost{background:#EEF3F8;color:#334155}

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

/* ── 标准 record-card 结构 ── */
.record-card{display:flex;margin-bottom:16rpx;background:#FFFFFF;border-radius:20rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,.06);overflow:hidden}
.card-bar{width:4rpx;background:linear-gradient(180deg,#087CF0,#A8C7E5);flex-shrink:0}
.card-body{flex:1;padding:24rpx 28rpx}
.card-header{display:flex;align-items:flex-start;justify-content:space-between;gap:20rpx}
.record-title{flex:1;font-size:30rpx;line-height:42rpx;font-weight:700;color:#1A2332;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0}
.record-id{padding:4rpx 14rpx;background:#E8EEF5;color:#5A6B7F;font-size:20rpx;border-radius:999rpx;flex-shrink:0}

/* ── 套餐档位行 ── */
.packages-row{display:flex;align-items:flex-start;gap:12rpx;margin-top:14rpx;padding-top:14rpx;border-top:1rpx solid #E8EEF5}
.packages-label{font-size:22rpx;color:#94A3B8;flex-shrink:0;margin-top:4rpx}
.packages-tags{display:flex;flex-wrap:wrap;gap:10rpx;flex:1}
.package-tag{padding:6rpx 14rpx;background:#EFF6FF;color:#087CF0;font-size:22rpx;font-weight:500;border-radius:8rpx;border:1rpx solid #BFDBFE}
.packages-empty{font-size:22rpx;color:#94A3B8}

/* ── 商品名行（summary-grid） ── */
.summary-grid{display:grid;grid-template-columns:1fr 1fr;gap:12rpx;margin-top:14rpx}
.summary-item{display:flex;flex-direction:column;gap:6rpx}
.summary-label{font-size:22rpx;color:#94A3B8}
.summary-value{font-size:26rpx;color:#1A2332;font-weight:500}
.summary-value.tone-ok{color:#047857;font-weight:700}
.summary-value.tone-warn{color:#B45309;font-weight:700}

/* ── 列表总数 ── */
.list-total{text-align:center;padding:20rpx 0 40rpx;font-size:24rpx;color:#94A3B8}

/* ── 空状态 / 分页 ── */
.empty{text-align:center;color:#94a3b8;padding:56rpx 0;font-size:23rpx}

/* ════════════════════════════════════════════════
 * 详情面板样式（与 member-purchase-return 一致）
 * ════════════════════════════════════════════════ */
.overlay-mask{position:fixed;inset:0;background:rgba(15,23,42,.45);z-index:50;overflow:hidden}
.detail-page{position:absolute;inset:0;bottom:0;background:#E8EEF5;padding-bottom:calc(120rpx + env(safe-area-inset-bottom));box-sizing:border-box;overflow-y:auto;-webkit-overflow-scrolling:touch;will-change:transform}
.detail-page.swipe-closing{pointer-events:none}
.detail-hero{position:relative;margin:24rpx 28rpx;border-radius:20rpx;overflow:hidden}
.detail-hero-bg{position:absolute;inset:0;background:linear-gradient(135deg,#087CF0,#5AA9E8,#A8C7E5);border-radius:20rpx}
.detail-hero-content{position:relative;z-index:1;padding:40rpx 36rpx}
.detail-hero-eyebrow{font-size:22rpx;color:rgba(255,255,255,.7);margin-bottom:12rpx;letter-spacing:2rpx}
.detail-hero-title{font-size:36rpx;font-weight:600;color:#fff;margin-bottom:16rpx;line-height:1.4}
.detail-hero-value{font-size:52rpx;font-weight:700;color:#fff;margin-bottom:12rpx}
.detail-hero-meta{font-size:24rpx;color:rgba(255,255,255,.7)}
.detail-section{margin:20rpx 28rpx 0;background:#fff;border-radius:20rpx;padding:28rpx 32rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,.06);box-sizing:border-box}
.detail-section-title{font-size:28rpx;font-weight:600;color:#1A2332;margin-bottom:20rpx;padding-left:16rpx;border-left:4rpx solid #087CF0}
.detail-highlight-grid{display:flex;flex-wrap:wrap;gap:12rpx}
.detail-highlight-item{flex:1;min-width:45%;background:#F5F8FA;border-radius:12rpx;padding:18rpx 20rpx;box-sizing:border-box}
.detail-highlight-label{font-size:22rpx;color:#94A3B8;margin-bottom:6rpx}
.detail-highlight-value{font-size:28rpx;font-weight:600;color:#1A2332}
.detail-item{background:#F5F8FA;border-radius:16rpx;padding:24rpx;margin-bottom:20rpx}
.detail-item:last-child{margin-bottom:0}
.detail-item-header{display:flex;align-items:center;justify-content:space-between;padding-bottom:16rpx;border-bottom:1rpx solid #E2E8F0;margin-bottom:16rpx}
.detail-item-title{font-size:28rpx;font-weight:700;color:#1A2332}
.detail-row{display:flex;align-items:center;min-height:72rpx;padding:16rpx 0}
.detail-label{width:140rpx;font-size:26rpx;color:#64748B;flex-shrink:0}
.detail-value-text{font-size:26rpx;color:#1A2332;flex:1}
.detail-value-text.amount{font-weight:700;color:#087CF0}
.detail-footer-placeholder{height:140rpx}
.detail-footer-bar{position:absolute;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;gap:16rpx;padding:16rpx 24rpx;padding-bottom:calc(16rpx + env(safe-area-inset-bottom));background:#fff;box-shadow:0 -2rpx 16rpx rgba(8,124,240,.06);z-index:60}
.detail-action-btn{flex:1;height:76rpx;line-height:76rpx;font-size:28rpx;font-weight:500;border-radius:16rpx;text-align:center;border:none;margin:0;padding:0;min-width:0}
.detail-action-btn::after{border:none}
.detail-action-btn.edit-btn{background:#E8EEF5;color:#087CF0}
.detail-action-btn.primary-btn{background:#087CF0;color:#fff}
.detail-action-btn.danger-btn{background:#FECACA;color:#7F1D1D}
</style>
