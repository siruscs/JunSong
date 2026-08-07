<template>
  <view class="page" v-if="config">
    <view class="hero">
      <view>
        <view class="eyebrow">{{ config.group }}</view>
        <view class="hero-title">{{ config.title }}</view>
      </view>
    </view>

    <view class="work-scope">
      <view class="work-scope-mark"></view>
      <view class="work-scope-copy">
        <text class="work-scope-label">{{ scopeLabel }}</text>
        <text class="work-scope-name">{{ currentDeptName }}</text>
      </view>
    </view>

    <view class="filter-wrap" v-if="moduleKey === 'seckillRecord'">
      <picker class="filter-picker" :range="seckillOptions" range-key="seckillName" :value="selectedSeckillIndex" @change="onSeckillChange">
        <view class="filter-picker-box">
          <text class="filter-picker-label">秒杀活动</text>
          <text class="filter-picker-value">{{ selectedSeckill?.seckillName || '选择活动' }}</text>
          <text class="filter-picker-arrow">›</text>
        </view>
      </picker>
      <button class="batch-all-btn" v-if="canBatchAll" @tap="openBatchAll">⚡ 全员秒杀</button>
    </view>

    <view class="stats-bar" v-if="moduleKey === 'expense' && expenseSummary">
      <view class="stat-item">
        <text class="stat-value">{{ expenseSummary.totalExpenseAmount || '0' }}</text>
        <text class="stat-label">总费用</text>
      </view>
      <view class="stat-item">
        <text class="stat-value warn">{{ expenseSummary.unverifiedExpenseAmount || '0' }}</text>
        <text class="stat-label">未核销</text>
      </view>
      <view class="stat-item">
        <text class="stat-value" :class="parseFloat(expenseSummary.advanceBalance) < 0 ? 'danger' : 'ok'">{{ expenseSummary.advanceBalance || '0' }}</text>
        <text class="stat-label">借支余额</text>
      </view>
    </view>

    <view class="search-wrap">
      <input class="search" v-model="queryValue" :placeholder="searchPlaceholder" confirm-type="search" @confirm="refresh" />
      <button class="search-button" @tap="refresh">查询</button>
      <button v-if="moduleKey === 'expense' && canVerifyExpenses" class="search-button batch-verify-btn" @tap="toggleBatchSelection">{{ batchSelecting ? '取消批量' : '批量核销' }}</button>
    </view>
    <view v-if="moduleKey === 'expense' && canVerifyExpenses && batchSelecting" class="expense-batch-tools">
      <button class="chip-button primary" :disabled="selectedExpenseIds.length === 0" @tap="continueBatchVerify">下一步（{{ selectedExpenseIds.length }}笔 / ¥{{ selectedExpenseTotal.toFixed(2) }}）</button>
    </view>

    <view class="page-actions" v-if="authorizedPageActions.length">
      <button class="chip-button" v-for="action in authorizedPageActions" :key="action.name" @tap="runPageAction(action)">
        {{ action.name }}
      </button>
    </view>

    <view class="member-overview" v-if="moduleKey === 'member'">
      <view class="member-overview-main">
        <text class="member-overview-value">{{ totalRecords }}</text>
        <text class="member-overview-label">会员档案</text>
      </view>
      <view class="member-overview-side">
        <text class="member-overview-sub">{{ memberActiveCount }} 位正常</text>
        <text class="member-overview-hint">按姓名或编号快速检索</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="refresh" @scrolltolower="loadMore">

      <view v-if="moduleKey === 'member'">
        <view class="member-card" hover-class="member-card--active" v-for="item in rows" :key="item[config.idKey]" @tap="openDetail(item)">
          <view class="member-card-top">
            <view class="member-avatar" :class="statusClass(item.status)">
              <text>{{ memberInitial(item) }}</text>
            </view>
            <view class="member-title-block">
              <view class="member-title-line">
                <text class="member-name">{{ item.memberName || '-' }}</text>
                <text class="member-status-text" :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
                <text class="member-card-type">{{ memberCardType(item) }}</text>
              </view>
              <text class="member-no">{{ memberNoLine(item) }}</text>
              <view class="member-contact-line">
                <text class="member-phone">{{ item.phone || '-' }}</text>
                <text class="member-seckill">秒杀{{ seckillCount(item) }}次</text>
              </view>
            </view>
            <text class="member-arrow">›</text>
          </view>
        </view>
      </view>

      <view v-if="moduleKey === 'seckillRecord'">
        <view class="seckill-empty-select" v-if="!selectedSeckillId">
          <text>请先选择秒杀活动</text>
        </view>
        <view class="seckill-record-card" v-for="item in rows" :key="item[config.idKey]">
          <view class="seckill-record-main" @tap="openDetail(item)">
            <view class="seckill-record-title">
              <text class="seckill-member">{{ item.memberName || '-' }}</text>
              <text class="seckill-status" :class="seckillRecordStatusClass(item.status)">{{ seckillRecordStatusText(item.status) }}</text>
            </view>
            <text class="seckill-member-no">{{ item.memberNo || '-' }}</text>
            <view class="seckill-progress-grid">
              <view class="seckill-progress-item">
                <text class="seckill-progress-value">{{ item.shares || 0 }}</text>
                <text class="seckill-progress-label">总份额</text>
              </view>
              <view class="seckill-progress-item">
                <text class="seckill-progress-value claimed">{{ item.claimedShares || 0 }}</text>
                <text class="seckill-progress-label">已领取</text>
              </view>
              <view class="seckill-progress-item">
                <text class="seckill-progress-value remain">{{ item.remainingShares ?? item.shares ?? 0 }}</text>
                <text class="seckill-progress-label">待领取</text>
              </view>
            </view>
          </view>
          <view class="seckill-record-actions">
            <text class="seckill-record-date">{{ item.seckillDate || '-' }}</text>
            <button class="claim-button" v-if="canClaim(item)" @tap.stop="openClaim(item)">领取</button>
          </view>
        </view>

        <view class="claim-history" v-if="selectedSeckillId">
          <view class="claim-history-head">
            <text class="claim-history-title">领取记录</text>
            <text class="claim-history-count">{{ claimRows.length }}条</text>
          </view>
          <view class="claim-row" v-for="claim in claimRows" :key="claim.claimId">
            <view>
              <text class="claim-member">{{ claim.memberName || '-' }}</text>
              <text class="claim-meta">{{ claim.memberNo || '-' }}  {{ claim.claimTime || '-' }}</text>
            </view>
            <text class="claim-shares">{{ claim.claimShares || 0 }}份</text>
          </view>
          <view class="claim-empty" v-if="!claimLoading && claimRows.length === 0">暂无领取记录</view>
        </view>
      </view>

      <view v-if="moduleKey === 'expense'">
        <view class="expense-item" :class="{ disabled: batchSelecting && !isExpenseSelectable(item), selected: isExpenseSelected(item) }" hover-class="expense-item--active" v-for="item in rows" :key="item[config.idKey]" @tap="handleExpenseTap(item)">
          <view class="expense-checkbox" v-if="batchSelecting">{{ isExpenseSelected(item) ? '✓' : '' }}</view>
          <view class="expense-bar"></view>
          <view class="expense-body">
            <view class="expense-row1">
              <text class="expense-content">{{ item.expenseContent || displayField('expenseType', item.expenseType) || '-' }}</text>
              <text class="expense-amount" :class="{ income: isIncomeExpense(item) }">{{ expenseAmountText(item) }}</text>
            </view>
            <view class="expense-row2">
              <text class="expense-meta">{{ item.expenseType || '-' }}</text>
              <text class="expense-meta">{{ displayField('paymentMethod', item.paymentMethod) || '-' }}</text>
              <text class="expense-meta date">{{ item.expenseDate || '-' }}</text>
              <text class="expense-status" :class="expenseStatusClass(item.status)">{{ expenseStatusText(item.status) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="moduleKey === 'advance'">
        <view class="record-card" v-for="item in rows" :key="item[config.idKey]" @tap="openDetail(item)">
          <view class="card-bar"></view>
          <view class="card-body">
            <view class="card-header">
              <view class="record-title">{{ recordTitle(item) }}</view>
              <view class="advance-amount">{{ displayField('advanceAmount', item.advanceAmount) }}</view>
            </view>
            <view class="advance-subrow">
              <text class="advance-date">{{ displayField('advanceDate', item.advanceDate) || '-' }}</text>
              <text class="advance-status" :class="displayField('status', item.status) === '已核销' ? 'tone-ok' : 'tone-warn'">{{ displayField('status', item.status) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="moduleKey === 'product'">
        <view class="record-card" v-for="item in rows" :key="item[config.idKey]" @tap="openDetail(item)">
          <view class="card-bar"></view>
          <view class="card-body">
            <view class="card-header">
              <view class="record-title">{{ recordTitle(item) }}</view>
              <view class="product-status" :class="displayField('status', item.status) === '正常' ? 'tone-ok' : 'tone-warn'">{{ displayField('status', item.status) }}</view>
            </view>
            <view class="summary-grid">
              <view class="summary-item" v-for="entry in pillEntries(item)" :key="entry.key">
                <text class="summary-label">{{ entry.label }}</text>
                <text class="summary-value" :class="entry.tone">{{ entry.value }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-if="moduleKey === 'purchase'">
        <view class="record-card" v-for="item in rows" :key="item[config.idKey]" @tap="openDetail(item)">
          <view class="card-bar"></view>
          <view class="card-body">
            <view class="card-header">
              <view class="record-title">{{ displayField('purchaseDate', item.purchaseDate) || displayField('createTime', item.createTime) || '-' }}</view>
              <view class="purchase-total-amount">{{ displayField('totalAmount', item.totalAmount) }}</view>
            </view>
            <view class="purchase-summary-grid">
              <view class="summary-item">
                <text class="summary-label">供应商名称</text>
                <text class="summary-value">{{ displayField('supplierName', item.supplierName) }}</text>
              </view>
              <view class="summary-item">
                <text class="summary-label">进货单号</text>
                <text class="summary-value">{{ item.purchaseNo || '-' }}</text>
              </view>
            </view>
            <view class="purchase-product-names" v-if="item.productNames">
              <text>{{ item.productNames }}</text>
              <text class="purchase-status-tag" :class="'purchase-status-' + (item.status || '')">{{ displayField('status', item.status) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="moduleKey !== 'member' && moduleKey !== 'expense' && moduleKey !== 'seckillRecord' && moduleKey !== 'advance' && moduleKey !== 'product' && moduleKey !== 'purchase'">
        <view class="record-card" v-for="item in rows" :key="item[config.idKey]" @tap="openDetail(item)">
          <view class="card-bar"></view>
          <view class="card-body">
            <view class="card-header">
              <view class="record-title">{{ recordTitle(item) }}</view>
              <view class="record-id">NO. {{ item[config.idKey] }}</view>
            </view>

            <view class="summary-grid">
              <view class="summary-item" v-for="entry in pillEntries(item)" :key="entry.key">
                <text class="summary-label">{{ entry.label }}</text>
                <text class="summary-value" :class="entry.tone">{{ entry.value }}</text>
              </view>
            </view>

            <view class="card-footer" v-if="moduleKey === 'sale' || metaText(item)" :class="moduleKey === 'sale' ? `sale-footer-${saleStatusClass(item)}` : ''">
              <view v-if="moduleKey === 'sale'" class="sale-footer-meta">
                <text class="sale-footer-date">{{ saleDateText(item) }}</text>
                <text class="sale-footer-status">{{ saleStatusText(item) }}</text>
              </view>
              <text v-else class="meta-text">{{ metaText(item) }}</text>
              <text class="arrow-icon">›</text>
            </view>
          </view>
        </view>
      </view>

      <view class="load-error" v-if="listState === 'error'">
        <text class="empty-title">加载失败</text>
        <text class="empty-subtitle">{{ loadError }}</text>
        <button class="retry-button" @tap="refresh">重新加载</button>
      </view>
      <view class="empty" v-if="listState === 'empty'">
        <view class="empty-title">暂无{{ config.title }}</view>
        <view class="empty-subtitle">{{ canAdd ? '点击右下角新增一条记录' : '暂无可查看数据' }}</view>
      </view>
      <view class="loading" v-if="listState === 'loading'">加载中</view>
      <view class="loading" v-if="finished && rows.length > 0">没有更多了</view>
    </scroll-view>

    <view class="bottom-bar" v-if="canAdd">
      <button class="add-button" @tap="addItem">＋ 新增</button>
    </view>

    <view class="claim-mask" v-if="claimPanelOpen" @tap="closeClaim">
      <view class="claim-panel" @tap.stop>
        <view class="claim-panel-title">领取份额</view>
        <view class="claim-panel-sub">{{ activeClaimRecord?.memberName || '-' }}，剩余 {{ activeClaimRecord?.remainingShares || 0 }} 份</view>
        <input class="claim-input" v-model="claimForm.claimShares" type="digit" placeholder="0.000" />
        <view class="claim-time-row">
          <picker mode="date" :value="claimForm.claimDate" @change="onClaimDateChange">
            <view class="claim-time-picker">{{ claimForm.claimDate }}</view>
          </picker>
          <picker mode="time" :value="claimForm.claimTime" @change="onClaimTimeChange">
            <view class="claim-time-picker">{{ claimForm.claimTime }}</view>
          </picker>
        </view>
        <textarea class="claim-remark" v-model="claimForm.remark" placeholder="备注，可不填" />
        <view class="claim-panel-actions">
          <button class="claim-cancel" @tap="closeClaim">取消</button>
          <button class="claim-confirm" :disabled="claimSubmitting" @tap="submitClaim">{{ claimSubmitting ? '领取中' : '确认领取' }}</button>
        </view>
      </view>
    </view>

    <view class="claim-mask" v-if="batchAllOpen" @tap="closeBatchAll">
      <view class="claim-panel" @tap.stop>
        <view class="claim-panel-title">⚡ 全员秒杀</view>
        <view class="claim-panel-sub">为当前部门所有有效会员批量生成秒杀记录</view>
        <view class="batch-all-form">
          <view class="batch-all-row">
            <text class="batch-all-label">秒杀活动</text>
            <picker :range="seckillOptions" range-key="seckillName" :value="batchAllSeckillIndex" @change="onBatchAllSeckillChange">
              <view class="batch-all-picker">{{ batchAllSeckill?.seckillName || '请选择活动' }}</view>
            </picker>
          </view>
          <view class="batch-all-row">
            <text class="batch-all-label">秒杀日期</text>
            <picker mode="date" :value="batchAllForm.seckillDate" @change="onBatchAllDateChange">
              <view class="batch-all-picker">{{ batchAllForm.seckillDate || '请选择日期' }}</view>
            </picker>
          </view>
          <view class="batch-all-row">
            <text class="batch-all-label">收款方式</text>
            <picker :range="paymentMethodOptions" range-key="label" :value="batchAllPaymentIndex" @change="onBatchAllPaymentChange">
              <view class="batch-all-picker">{{ batchAllPaymentLabel || '请选择' }}</view>
            </picker>
          </view>
          <view class="batch-all-row">
            <text class="batch-all-label">每人份额</text>
            <input class="batch-all-input" v-model="batchAllForm.shares" type="number" placeholder="1" />
          </view>
          <textarea class="claim-remark" v-model="batchAllForm.remark" placeholder="备注，可不填" />
        </view>
        <view class="claim-panel-actions">
          <button class="claim-cancel" @tap="closeBatchAll">取消</button>
          <button class="claim-confirm" @tap="submitBatchAll" :disabled="batchAllLoading">{{ batchAllLoading ? '生成中' : '确认生成' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import miniProgramShare from '@/mixins/miniProgramShare.js'
import { getModule, formatDisplayValue, getValueTone } from '@/config/modules.js'
import { listData, request } from '@/api/index.js'
import { hasActionPermission, requireModulePermission } from '@/utils/permission.js'
import { resolveListState } from '@/utils/operationState.js'
import { resolveMemberSearchField } from '@/utils/memberWorkflow.js'
import { workContext } from '@/utils/workContext.js'
import { canRequestListScope, resolveListWorkScope, shouldRestoreListPage } from '@/utils/listWorkScope.js'

export default {
  mixins: [miniProgramShare],
  data() {
    return {
      moduleKey: '',
      config: null,
      queryValue: '',
      pageNum: 1,
      pageSize: 10,
      rows: [],
      loading: false,
      loadError: '',
      refreshPending: false,
      refreshing: false,
      finished: false,
      expenseSummary: null,
      totalRecords: 0,
      dictCache: {},
      seckillOptions: [],
      selectedSeckillId: '',
      claimRows: [],
      claimLoading: false,
      claimSubmitting: false,
      claimPanelOpen: false,
      activeClaimRecord: null,
      claimForm: {
        claimShares: '',
        claimDate: '',
        claimTime: '',
        remark: ''
      },
      batchAllOpen: false,
      batchAllLoading: false,
      batchAllForm: {
        seckillId: '',
        seckillDate: '',
        paymentMethod: '',
        shares: 1,
        remark: ''
      },
      paymentMethodOptions: [],
      paymentMethodDictLoaded: false,
      currentDeptId: null,
      currentDeptName: '未选择部门',
      scopeLabel: '暂无可用数据范围',
      contextVersion: 0,
      batchSelecting: false,
      selectedExpenseIds: []
    }
  },
  computed: {
    listState() {
      return resolveListState({ loading: this.loading, error: this.loadError, rows: this.rows })
    },
    canAdd() {
      if (this.moduleKey === 'seckillRecord') return hasActionPermission(this.moduleKey, 'add')
      return hasActionPermission(this.moduleKey, 'add') && !this.config?.addOnly
    },
    canVerifyExpenses() {
      return this.moduleKey === 'expense' && hasActionPermission(this.moduleKey, 'verify')
    },
    selectedExpenseTotal() {
      return this.rows.filter((item) => this.selectedExpenseIds.includes(Number(item.expenseId)))
        .reduce((total, item) => total + Number(item.expenseAmount || 0), 0)
    },
    authorizedPageActions() {
      return (this.config?.pageActions || []).filter((action) => hasActionPermission(this.moduleKey, action.action || 'view'))
    },
    memberActiveCount() {
      return this.rows.filter((item) => String(item.status ?? '0') === '0').length
    },
    selectedSeckillIndex() {
      const index = this.seckillOptions.findIndex((item) => String(item.seckillId) === String(this.selectedSeckillId))
      return index < 0 ? 0 : index
    },
    selectedSeckill() {
      return this.seckillOptions[this.selectedSeckillIndex] || null
    },
    searchPlaceholder() {
      if (this.moduleKey === 'expense') return '搜索花销内容'
      if (this.moduleKey === 'member') return '输入姓名、编号或手机号'
      if (this.config?.searchKeys && this.config.searchKeys.length > 1) return '输入姓名或编号搜索'
      return '搜索' + (this.config?.title || '')
    },
    canBatchAll() {
      return this.moduleKey === 'seckillRecord' && hasActionPermission(this.moduleKey, 'add')
    },
    batchAllSeckillIndex() {
      const idx = this.seckillOptions.findIndex((item) => String(item.seckillId) === String(this.batchAllForm.seckillId))
      return idx < 0 ? 0 : idx
    },
    batchAllSeckill() {
      return this.seckillOptions[this.batchAllSeckillIndex] || null
    },
    batchAllPaymentIndex() {
      const idx = this.paymentMethodOptions.findIndex((item) => String(item.value) === String(this.batchAllForm.paymentMethod))
      return idx < 0 ? 0 : idx
    },
    batchAllPaymentLabel() {
      const hit = this.paymentMethodOptions.find((item) => String(item.value) === String(this.batchAllForm.paymentMethod))
      return hit ? hit.label : ''
    }
  },
  async onLoad(options) {
    this.moduleKey = options.module
    this.config = getModule(this.moduleKey)
    this.syncWorkScope()
    if (this.config && requireModulePermission(this.moduleKey)) {
      uni.setNavigationBarTitle({ title: this.config.title })
      await this.loadDictOptions()
      if (this.moduleKey === 'seckillRecord') await this.loadSeckillOptions(options.seckillId)
      this.refresh()
      if (this.moduleKey === 'expense') this.loadExpenseSummary()
    }
  },
  onShow() {
    if (this.config && this.moduleKey) {
      this.syncWorkScope()
      this.resetBatchSelection()
      this.refresh()
      if (this.moduleKey === 'expense') this.loadExpenseSummary()
    }
  },
  methods: {
    syncWorkScope() {
      const scope = resolveListWorkScope(workContext.snapshot(), this.currentDeptId)
      if (scope.departmentChanged) {
        this.rows = []
        this.totalRecords = 0
        this.finished = false
        this.expenseSummary = null
        this.resetBatchSelection()
      }
      this.currentDeptId = scope.currentDeptId
      this.currentDeptName = scope.currentDeptName
      this.scopeLabel = scope.scopeLabel
      this.contextVersion = scope.contextVersion
      return scope
    },
    resetBatchSelection() {
      this.batchSelecting = false
      this.selectedExpenseIds = []
    },
    async loadExpenseSummary() {
      if (!canRequestListScope(this.currentDeptId)) {
        this.expenseSummary = null
        return false
      }
      const contextVersion = workContext.captureVersion()
      const requestDeptId = this.currentDeptId
      try {
        const params = {}
        if (this.currentDeptId !== null && this.currentDeptId !== undefined) {
          params.deptId = this.currentDeptId
        }
        const res = await request({ url: '/finance/expense/summary', method: 'GET', data: params })
        if (!workContext.isCurrent(contextVersion) || String(this.currentDeptId) !== String(requestDeptId)) return false
        this.expenseSummary = res.data || res || null
        return true
      } catch (e) {
        if (!workContext.isCurrent(contextVersion) || String(this.currentDeptId) !== String(requestDeptId)) return false
        console.log('加载费用统计失败', e)
        return false
      }
    },
    statusText(val) {
      const map = { '0': '正常', '1': '无效', '2': '已退卡' }
      return map[val] || '未知'
    },
    statusClass(val) {
      const map = { '0': 'status-ok', '1': 'status-disabled', '2': 'status-expired' }
      return map[val] || 'status-ok'
    },
    seckillRecordStatusText(val) {
      const map = { '0': '待领取', '1': '已领取', '2': '已取消', '3': '部分领取' }
      return map[String(val ?? '0')] || '待领取'
    },
    seckillRecordStatusClass(val) {
      const map = { '0': 'status-waiting', '1': 'status-done', '2': 'status-cancelled', '3': 'status-partial' }
      return map[String(val ?? '0')] || 'status-waiting'
    },
    expenseStatusText(val) {
      const map = { '0': '未核销', '1': '已核销' }
      return map[String(val ?? '0')] || '未核销'
    },
    expenseStatusClass(val) {
      return String(val ?? '0') === '1' ? 'status-verified' : 'status-pending'
    },
    expenseAmountText(item) {
      const amount = Number(item.expenseAmount) || 0
      return `¥${amount.toFixed(2)}`
    },
    isIncomeExpense(item) {
      return item.expenseType === '收入' || Number(item.expenseAmount) < 0
    },
    canClaim(item) {
      const remaining = Number(item.remainingShares ?? item.shares ?? 0)
      return hasActionPermission(this.moduleKey, 'receive') && String(item.status) !== '2' && remaining > 0
    },
    todayStr() {
      const d = new Date()
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    },
    nowTimeStr() {
      const d = new Date()
      return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
    },
    memberInitial(item) {
      const name = item.memberName || item.memberNo || '会'
      return String(name).slice(0, 1).toUpperCase()
    },
    memberNoLine(item) {
      const memberNo = item.memberNo || '未生成编号'
      return item.joinDate ? `${memberNo}（${item.joinDate}）` : memberNo
    },
    memberCardType(item) {
      return this.displayField('cardType', item.cardType) || '-'
    },
    seckillCount(item) {
      return item.seckillCount ?? item.seckillRecordCount ?? item.buyCount ?? item.seckillTimes ?? 0
    },
    async loadDictOptions() {
      const dictFields = (this.config?.fields || []).filter((field) => field.dictType)
      for (const field of dictFields) {
        if (this.dictCache[field.dictType]) {
          field.options = this.dictCache[field.dictType]
          continue
        }
        try {
          const res = await request({ url: '/system/dict/data/type/' + field.dictType, method: 'GET' })
          const options = (res.data || []).map((item) => ({ label: item.dictLabel, value: item.dictValue }))
          this.dictCache[field.dictType] = options
          field.options = options
        } catch (e) {
          console.log('加载字典失败:', field.dictType, e)
        }
      }
    },
    async loadSeckillOptions(defaultSeckillId) {
      try {
        const res = await request({ url: '/member/seckill/list', method: 'GET', data: { pageNum: 1, pageSize: 100 } })
        const list = res.rows || res.data || []
        this.seckillOptions = Array.isArray(list) ? list : []
        this.selectedSeckillId = defaultSeckillId || this.seckillOptions[0]?.seckillId || ''
      } catch (e) {
        console.log('加载秒杀活动失败', e)
        this.seckillOptions = []
      }
      if (this.moduleKey === 'seckillRecord' && !this.paymentMethodDictLoaded) {
        await this.loadPaymentMethodDict()
      }
    },
    async loadPaymentMethodDict() {
      try {
        const res = await request({ url: '/system/dict/data/type/finance_payment_method', method: 'GET' })
        const list = res.data || []
        this.paymentMethodOptions = list.map((item) => ({ label: item.dictLabel, value: item.dictValue, raw: item }))
        this.paymentMethodDictLoaded = true
      } catch (e) {
        console.log('加载付款方式字典失败', e)
        this.paymentMethodOptions = []
      }
    },
    getDictDefaultValue(options, fallback) {
      const defaultItem = options.find((item) => item.raw?.isDefault === 'Y')
      return defaultItem?.value ?? fallback
    },
    onBatchAllSeckillChange(e) {
      const item = this.seckillOptions[Number(e.detail.value)]
      this.batchAllForm.seckillId = item?.seckillId || ''
    },
    onBatchAllDateChange(e) {
      this.batchAllForm.seckillDate = e.detail.value
    },
    onBatchAllPaymentChange(e) {
      const item = this.paymentMethodOptions[Number(e.detail.value)]
      this.batchAllForm.paymentMethod = item?.value || ''
    },
    openBatchAll() {
      const defaultPayment = this.getDictDefaultValue(this.paymentMethodOptions, this.paymentMethodOptions[0]?.value || '')
      this.batchAllForm = {
        seckillId: this.selectedSeckillId || this.seckillOptions[0]?.seckillId || '',
        seckillDate: this.todayStr(),
        paymentMethod: defaultPayment,
        shares: 1,
        remark: ''
      }
      this.batchAllOpen = true
    },
    closeBatchAll() {
      this.batchAllOpen = false
    },
    async submitBatchAll() {
      if (this.batchAllLoading) return
      if (!this.batchAllForm.seckillId) {
        uni.showToast({ title: '请选择秒杀活动', icon: 'none' })
        return
      }
      if (!this.batchAllForm.seckillDate) {
        uni.showToast({ title: '请选择秒杀日期', icon: 'none' })
        return
      }
      this.batchAllLoading = true
      try {
        const res = await request({
          url: '/member/seckillRecord/batch',
          method: 'POST',
          data: {
            seckillId: this.batchAllForm.seckillId,
            seckillDate: this.batchAllForm.seckillDate,
            paymentMethod: this.batchAllForm.paymentMethod,
            shares: Number(this.batchAllForm.shares) || 1,
            remark: this.batchAllForm.remark
          }
        })
        uni.showToast({ title: res.msg || '生成成功', icon: 'success' })
        this.closeBatchAll()
        this.refresh()
      } catch (e) {
        console.error('全员秒杀失败', e)
      } finally {
        this.batchAllLoading = false
      }
    },
    onSeckillChange(e) {
      const item = this.seckillOptions[Number(e.detail.value)]
      this.selectedSeckillId = item?.seckillId || ''
      this.refresh()
    },
    // 根据 key 查找字段定义并展示值
    fieldOf(key) {
      return this.config.fields.find((field) => field.key === key) || { key, label: key }
    },
    // 展示某个字段的值（处理 select 类型）
    displayField(key, value, item) {
      if (this.moduleKey === 'sale' && key === 'saleQuantity') {
        const quantity = Number(value || 0)
        const giftQuantity = Number(item?.giftQuantity || 0)
        return giftQuantity > 0 ? `${quantity}+${giftQuantity}(赠)` : String(quantity)
      }
      const field = this.fieldOf(key)
      if (typeof field.formatter === 'function') return field.formatter(item || {}) || '-'
      return formatDisplayValue(field, value, item)
    },
    fieldTone(key, value) {
      return getValueTone(this.fieldOf(key), value)
    },
    // 列表记录标题
    recordTitle(item) {
      const searchKey = this.config.searchKey
      if (searchKey && item[searchKey]) {
        return String(item[searchKey])
      }
      // 回退到 summary 第一个有值的字段
      const summaryKeys = this.config.summary || []
      for (const key of summaryKeys) {
        if (item[key] !== undefined && item[key] !== null && item[key] !== '') {
          return this.displayField(key, item[key])
        }
      }
      return this.config.title
    },
    buildQuery() {
      const query = { pageNum: this.pageNum, pageSize: this.pageSize }
      if (this.currentDeptId !== null && this.currentDeptId !== undefined) {
        query.deptId = this.currentDeptId
      }
      if (this.moduleKey === 'seckillRecord' && this.selectedSeckillId) {
        query.seckillId = this.selectedSeckillId
      }
      if (this.queryValue) {
        const val = this.queryValue.trim()
        if (this.moduleKey === 'member') {
          query[resolveMemberSearchField(val)] = val
        } else if (this.config.searchKeys && this.config.searchKeys.length > 1) {
          const isNo = /^[A-Za-z0-9]/.test(val)
          query[isNo ? this.config.searchKeys[1] : this.config.searchKeys[0]] = val
        } else if (this.config.searchKey) {
          query[this.config.searchKey] = this.queryValue
        }
      }
      return query
    },
    pillEntries(item) {
      return this.config.summary.slice(0, 4).map((key) => ({
        key,
        label: this.fieldOf(key).label,
        value: this.displayField(key, item[key], item),
        tone: this.fieldTone(key, item[key])
      })).filter((entry) => entry.value !== '-')
    },
    metaText(item) {
      const dateKey = this.config.fields.find((field) => field.type === 'date' && item[field.key])?.key
      const statusKey = this.config.fields.find((field) => field.key === 'status')?.key
      if (dateKey && statusKey) return `${this.displayField(dateKey, item[dateKey])}  ${this.displayField(statusKey, item[statusKey])}`
      if (dateKey) return this.displayField(dateKey, item[dateKey])
      if (statusKey) return this.displayField(statusKey, item[statusKey])
      return ''
    },
    saleDateText(item) {
      const dateKey = this.config.fields.find((field) => field.type === 'date' && item[field.key])?.key
      return dateKey ? this.displayField(dateKey, item[dateKey], item) : '-'
    },
    saleStatusText(item) {
      return this.displayField('status', item.status, item)
    },
    saleStatusClass(item) {
      const status = String(item?.status ?? '')
      if (status === '2') return 'status-ok'
      if (status === '1') return 'status-info'
      return 'status-warn'
    },
    async refresh() {
      if (this.loading) {
        this.refreshPending = true
        return
      }
      this.resetBatchSelection()
      this.pageNum = 1
      this.finished = false
      this.refreshing = true
      try {
        await this.fetchList(true)
      } finally {
        this.refreshing = false
      }
    },
    async loadMore() {
      if (this.loading || this.finished) return
      const contextVersion = workContext.captureVersion()
      const previousPage = this.pageNum
      this.pageNum += 1
      const loaded = await this.fetchList(false)
      if (shouldRestoreListPage(loaded, workContext.isCurrent(contextVersion))) this.pageNum = previousPage
    },
    async fetchList(reset) {
      if (!canRequestListScope(this.currentDeptId)) {
        this.rows = []
        this.totalRecords = 0
        this.finished = true
        if (reset) this.loadError = '暂无可用部门，请返回首页刷新'
        return false
      }
      const contextVersion = workContext.captureVersion()
      const requestDeptId = this.currentDeptId
      this.loading = true
      if (reset) this.loadError = ''
      try {
        if (this.moduleKey === 'seckillRecord' && !this.selectedSeckillId) {
          this.rows = []
          this.totalRecords = 0
          this.finished = true
          this.claimRows = []
          return true
        }
        const query = this.buildQuery()
        let res = await listData(this.config.path, query)
        if (!workContext.isCurrent(contextVersion) || String(this.currentDeptId) !== String(requestDeptId)) return false
        if (this.refreshPending) return true
        let list = res.rows || res.data || []
        // 核算周期必须与当前登录时选定的部门一致；即使后端返回了越权/跨部门数据，前端也不展示。
        if (this.moduleKey === 'accountingPeriod') {
          list = list.filter((item) => String(item.deptId) === String(requestDeptId))
        }
        if (this.moduleKey === 'member' && query.phone && list.length === 0) {
          const fallbackQuery = { ...query }
          delete fallbackQuery.phone
          fallbackQuery.memberNo = query.phone
          res = await listData(this.config.path, fallbackQuery)
          if (!workContext.isCurrent(contextVersion) || String(this.currentDeptId) !== String(requestDeptId)) return false
          if (this.refreshPending) return true
          list = res.rows || res.data || []
          if (this.moduleKey === 'accountingPeriod') {
            list = list.filter((item) => String(item.deptId) === String(requestDeptId))
          }
        }
        this.rows = reset ? list : this.rows.concat(list)
        this.totalRecords = Number(res.total ?? this.rows.length) || 0
        this.finished = list.length < this.pageSize
        if (reset && this.moduleKey === 'seckillRecord') this.loadClaimRows()
        return true
      } catch (error) {
        if (!workContext.isCurrent(contextVersion) || String(this.currentDeptId) !== String(requestDeptId)) return false
        if (this.refreshPending) return true
        if (reset) this.loadError = error?.msg || error?.message || '请检查网络后重试'
        else uni.showToast({ title: '加载更多失败，请重试', icon: 'none' })
        return false
      } finally {
        this.loading = false
        if (this.refreshPending) {
          this.refreshPending = false
          await this.refresh()
        }
      }
    },
    async loadClaimRows() {
      if (!this.selectedSeckillId) return
      this.claimLoading = true
      try {
        const data = { pageNum: 1, pageSize: 100, seckillId: this.selectedSeckillId }
        const searchValue = String(this.queryValue || '').trim()
        if (searchValue) {
          data[/^[A-Za-z0-9]/.test(searchValue) ? 'memberNo' : 'memberName'] = searchValue
        }
        const res = await request({ url: '/member/seckillRecord/claim/list', method: 'GET', data })
        this.claimRows = res.rows || res.data || []
      } finally {
        this.claimLoading = false
      }
    },
    addItem() {
      if (!this.canAdd) {
        uni.showToast({ title: '暂无新增权限', icon: 'none' })
        return
      }
      if (this.moduleKey === 'seckillRecord') {
        if (!this.selectedSeckillId) {
          uni.showToast({ title: '请先选择秒杀活动', icon: 'none' })
          return
        }
        const selected = this.selectedSeckill || {}
        uni.navigateTo({ url: `/pages/form/index?module=${this.moduleKey}&seckillId=${this.selectedSeckillId}&seckillDate=${selected.seckillDate || ''}&seckillPrice=${selected.seckillPrice || ''}` })
        return
      }
      uni.navigateTo({ url: '/pages/form/index?module=' + this.moduleKey })
    },
    openClaim(item) {
      this.activeClaimRecord = item
      this.claimForm = { claimShares: String(item.remainingShares || 1), claimDate: this.todayStr(), claimTime: this.nowTimeStr(), remark: '' }
      this.claimPanelOpen = true
    },
    closeClaim() {
      this.claimPanelOpen = false
      this.activeClaimRecord = null
      this.claimForm = { claimShares: '', claimDate: '', claimTime: '', remark: '' }
    },
    onClaimDateChange(e) {
      this.claimForm.claimDate = e.detail.value
    },
    onClaimTimeChange(e) {
      this.claimForm.claimTime = e.detail.value
    },
    async submitClaim() {
      if (this.claimSubmitting) return
      const record = this.activeClaimRecord
      const claimShares = Number(this.claimForm.claimShares)
      if (!record || !claimShares || claimShares <= 0) {
        uni.showToast({ title: '请输入领取数量', icon: 'none' })
        return
      }
      this.claimSubmitting = true
      try {
        await request({
          url: '/member/seckillRecord/claim/' + record.recordId,
          method: 'PUT',
          data: {
            claimShares,
            claimTime: `${this.claimForm.claimDate} ${this.claimForm.claimTime}:00`,
            remark: this.claimForm.remark
          }
        })
        uni.showToast({ title: '领取成功' })
        this.closeClaim()
        this.refresh()
      } catch (e) {
        console.error('领取失败', e)
      } finally {
        this.claimSubmitting = false
      }
    },
    openDetail(item) {
      uni.navigateTo({ url: `/pages/detail/index?module=${this.moduleKey}&id=${item[this.config.idKey]}` })
    },
    isExpenseSelectable(item) {
      return String(item.status ?? '0') !== '1'
    },
    isExpenseSelected(item) {
      return this.selectedExpenseIds.includes(Number(item.expenseId))
    },
    handleExpenseTap(item) {
      if (!this.batchSelecting) return this.openDetail(item)
      if (!this.isExpenseSelectable(item)) return
      const id = Number(item.expenseId)
      this.selectedExpenseIds = this.isExpenseSelected(item)
        ? this.selectedExpenseIds.filter((value) => value !== id)
        : [...this.selectedExpenseIds, id]
    },
    toggleBatchSelection() {
      this.batchSelecting = !this.batchSelecting
      this.selectedExpenseIds = []
    },
    continueBatchVerify() {
      if (!this.selectedExpenseIds.length) return
      uni.navigateTo({ url: `/pages/expense-verify/index?expenseIds=${this.selectedExpenseIds.join(',')}` })
    },
    async runPageAction(action) {
      if (this.moduleKey === 'costAccounting') {
        uni.navigateTo({ url: '/pages/form/index?module=costAccounting&mode=preview' })
        return
      }
      try {
        await request({ url: action.url, method: action.method || 'GET' })
        uni.showToast({ title: '操作成功' })
        this.refresh()
      } catch (e) {
        console.error('页面操作失败', e)
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #E8EEF5;
  width: 100vw;
  max-width: 100vw;
  overflow-x: hidden;
  box-sizing: border-box;
}

.hero {
  padding: 36rpx 28rpx 24rpx;
  background: linear-gradient(135deg, #D4E5F7 0%, #F7FAFD 100%);
  border-left: 6rpx solid #087CF0;
  border-top: 1rpx solid #C1D8EF;
  margin: 24rpx 28rpx 0;
  border-radius: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(45, 72, 98, 0.08);
}

.hero--seckill-record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 28rpx 24rpx;
  border-left-color: #EA580C;
}

.eyebrow {
  font-size: 22rpx;
  color: #087CF0;
  font-weight: 600;
}

.hero-title {
  margin-top: 6rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #1A2332;
}

.work-scope {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin: 0 28rpx;
  padding: 18rpx 4rpx 4rpx;
}

.work-scope-mark {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #087CF0;
  flex-shrink: 0;
}

.work-scope-copy {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  min-width: 0;
}

.work-scope-label {
  color: #708196;
  font-size: 22rpx;
  flex-shrink: 0;
}

.work-scope-name {
  color: #1F2D3D;
  font-size: 25rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-wrap {
  margin: 16rpx 28rpx 0;
  display: flex;
  gap: 14rpx;
  align-items: stretch;
}

.filter-picker {
  flex: 1;
  min-width: 0;
}

.batch-all-btn {
  flex-shrink: 0;
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 24rpx;
  background: linear-gradient(135deg, #EA580C, #F97316);
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 700;
  border-radius: 999rpx;
  box-shadow: 0 4rpx 14rpx rgba(234, 88, 12, 0.25);
}

.filter-picker-box {
  display: flex;
  align-items: center;
  gap: 14rpx;
  height: 80rpx;
  padding: 0 28rpx;
  background: #FFFFFF;
  border: 2rpx solid #E2E8F0;
  border-radius: 999rpx;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
  box-sizing: border-box;
}

.filter-picker-label {
  flex-shrink: 0;
  font-size: 24rpx;
  font-weight: 700;
  color: #087CF0;
}

.filter-picker-value {
  min-width: 0;
  flex: 1;
  font-size: 26rpx;
  color: #1A2332;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-picker-arrow {
  flex-shrink: 0;
  font-size: 34rpx;
  color: #94A3B8;
  line-height: 1;
}

.stats-bar {
  margin: 16rpx 28rpx 0;
  padding: 24rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  display: flex;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}

.stat-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A2332;
}

.stat-value.warn {
  color: #d97706;
}

.stat-value.danger {
  color: #EF4444;
}

.stat-value.ok {
  color: #0F766E;
}

.stat-label {
  font-size: 22rpx;
  color: #94A3B8;
}

.search-wrap {
  display: flex;
  gap: 12rpx;
  margin: 16rpx 28rpx 0;
  padding: 0;
}

.search {
  flex: 1;
  min-width: 0;
  height: 80rpx;
  padding: 0 28rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.search-button {
  width: 108rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  font-size: 26rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.batch-verify-btn {
  width: auto;
  padding: 0 28rpx;
  background: linear-gradient(135deg, #E07B39, #F59E0B);
}

.expense-batch-tools {
  display: flex;
  gap: 16rpx;
  margin: 12rpx 28rpx 0;
  flex-wrap: wrap;
}

.page-actions {
  display: flex;
  gap: 14rpx;
  margin: 16rpx 28rpx 0;
}

.chip-button {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 28rpx;
  background: #FFFFFF;
  color: #087CF0;
  font-size: 26rpx;
  font-weight: 500;
  border: 2rpx solid #E2E8F0;
  border-radius: 16rpx;
}

.chip-button.primary {
  background: #087CF0;
  color: #FFFFFF;
  border-color: #087CF0;
}

.chip-button[disabled] {
  opacity: 0.5;
}

.scroll {
  width: 100%;
  height: calc(100vh - 300rpx);
  padding: 20rpx 28rpx 150rpx;
  box-sizing: border-box;
  overflow-x: hidden;
}

.member-overview {
  margin: 16rpx 28rpx 0;
  padding: 24rpx 28rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #123F73, #087CF0);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  box-shadow: 0 14rpx 36rpx rgba(8, 124, 240, 0.18);
  box-sizing: border-box;
}

.member-overview-main {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.member-overview-value {
  font-size: 44rpx;
  font-weight: 800;
  color: #FFFFFF;
  line-height: 1;
}

.member-overview-label {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.68);
}

.member-overview-side {
  flex: 1;
  min-width: 0;
  text-align: right;
}

.member-overview-sub {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #FFFFFF;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-overview-hint {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.64);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ====== 会员列表卡片模式 ====== */
.member-card {
  padding: 24rpx 24rpx 24rpx 26rpx;
  margin-bottom: 18rpx;
  background: #FFFFFF;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 28rpx rgba(8, 124, 240, 0.07);
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-sizing: border-box;
  overflow: hidden;
}

.member-card--active {
  transform: scale(0.98);
  opacity: 0.88;
}

.member-card-top {
  display: flex;
  align-items: flex-start;
  gap: 18rpx;
}

.member-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 32rpx;
  font-weight: 800;
}

.member-avatar.status-ok {
  background: #E0F2FE;
  color: #075985;
}

.member-avatar.status-disabled {
  background: #FEE2E2;
  color: #991B1B;
}

.member-avatar.status-expired {
  background: #FEF3C7;
  color: #92400E;
}

.member-title-block {
  flex: 1;
  min-width: 0;
}

.member-title-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 0;
}

.member-name {
  font-size: 38rpx;
  line-height: 48rpx;
  font-weight: 900;
  color: #102A3A;
  min-width: 0;
  max-width: 180rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-status-text {
  font-size: 21rpx;
  line-height: 34rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.member-status-text.status-ok {
  color: #0F766E;
}

.member-status-text.status-disabled {
  color: #B91C1C;
}

.member-status-text.status-expired {
  color: #B45309;
}

.member-card-type {
  min-width: 0;
  flex: 1;
  font-size: 22rpx;
  line-height: 34rpx;
  color: #708196;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-no {
  display: block;
  margin-top: 8rpx;
  font-size: 30rpx;
  line-height: 40rpx;
  font-weight: 800;
  color: #334E68;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-contact-line {
  display: flex;
  align-items: center;
  gap: 28rpx;
  margin-top: 8rpx;
  min-width: 0;
}

.member-phone {
  min-width: 0;
  max-width: 220rpx;
  font-size: 25rpx;
  line-height: 34rpx;
  color: #102A3A;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-seckill {
  flex-shrink: 0;
  font-size: 25rpx;
  line-height: 34rpx;
  color: #EA580C;
  font-weight: 700;
}

.member-arrow {
  padding-top: 14rpx;
  font-size: 38rpx;
  line-height: 1;
  color: #CBD5E1;
  flex-shrink: 0;
}

/* ====== 秒杀记录活动台账 ====== */
.seckill-empty-select {
  padding: 60rpx 24rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 26rpx;
}

.seckill-record-card {
  margin-bottom: 18rpx;
  padding: 24rpx;
  background: #FFFFFF;
  border-radius: 24rpx;
  border: 1rpx solid rgba(254, 215, 170, 0.9);
  box-shadow: 0 8rpx 28rpx rgba(234, 88, 12, 0.07);
  box-sizing: border-box;
  overflow: hidden;
}

.seckill-record-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.seckill-member {
  flex: 1;
  min-width: 0;
  font-size: 32rpx;
  line-height: 42rpx;
  font-weight: 800;
  color: #102A3A;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seckill-status {
  flex-shrink: 0;
  padding: 5rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 700;
}

.seckill-status.status-waiting {
  background: #FEF3C7;
  color: #92400E;
}

.seckill-status.status-partial {
  background: #FFEDD5;
  color: #C2410C;
}

.seckill-status.status-done {
  background: #D1FAE5;
  color: #065F46;
}

.seckill-status.status-cancelled {
  background: #FEE2E2;
  color: #991B1B;
}

.seckill-member-no {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #708196;
}

.seckill-progress-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10rpx;
  margin-top: 18rpx;
}

.seckill-progress-item {
  min-width: 0;
  padding: 16rpx 12rpx;
  border-radius: 16rpx;
  background: #F8FAFC;
  text-align: center;
  box-sizing: border-box;
}

.seckill-progress-value {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  color: #1A2332;
}

.seckill-progress-value.claimed {
  color: #059669;
}

.seckill-progress-value.remain {
  color: #EA580C;
}

.seckill-progress-label {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  color: #94A3B8;
}

.seckill-record-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #F1F5F9;
}

.seckill-record-date {
  min-width: 0;
  font-size: 22rpx;
  color: #94A3B8;
}

.claim-button {
  width: 132rpx;
  height: 62rpx;
  line-height: 62rpx;
  border-radius: 999rpx;
  background: #EA580C;
  color: #FFFFFF;
  font-size: 24rpx;
}

.claim-history {
  margin-top: 28rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  box-sizing: border-box;
}

.claim-history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.claim-history-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #1A2332;
}

.claim-history-count {
  font-size: 22rpx;
  color: #94A3B8;
}

.claim-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 16rpx 0;
  border-top: 1rpx solid #F1F5F9;
}

.claim-member {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
}

.claim-meta {
  display: block;
  margin-top: 4rpx;
  font-size: 21rpx;
  color: #94A3B8;
}

.claim-shares {
  flex-shrink: 0;
  font-size: 30rpx;
  font-weight: 800;
  color: #EA580C;
}

.claim-empty {
  padding: 24rpx 0 4rpx;
  text-align: center;
  font-size: 24rpx;
  color: #94A3B8;
}

.claim-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.45);
}

.claim-panel {
  width: 100%;
  padding: 30rpx 28rpx calc(30rpx + env(safe-area-inset-bottom));
  border-radius: 28rpx 28rpx 0 0;
  background: #FFFFFF;
  box-sizing: border-box;
}

.claim-panel-title {
  font-size: 34rpx;
  font-weight: 800;
  color: #1A2332;
}

.claim-panel-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #708196;
}

.claim-input,
.claim-remark {
  width: 100%;
  margin-top: 18rpx;
  padding: 0 20rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 18rpx;
  box-sizing: border-box;
  font-size: 26rpx;
}

.claim-input {
  height: 78rpx;
}

.claim-time-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 18rpx;
}

.claim-time-picker {
  min-width: 0;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 18rpx;
  border-radius: 18rpx;
  background: #FFF7ED;
  color: #9A3412;
  font-size: 25rpx;
  font-weight: 700;
  text-align: center;
  box-sizing: border-box;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.claim-remark {
  height: 136rpx;
  padding-top: 18rpx;
}

.claim-panel-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}

/* ====== 全员秒杀弹窗表单 ====== */
.batch-all-form {
  margin-top: 18rpx;
}

.batch-all-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.batch-all-row > picker {
  display: block;
  flex: 1;
  width: 0;
  min-width: 0;
}

.batch-all-label {
  flex-shrink: 0;
  width: 140rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #5A6B7F;
}

.batch-all-picker {
  display: block;
  width: 100%;
  flex: 1;
  min-width: 0;
  height: 84rpx;
  line-height: 84rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  color: #1A2332;
  font-size: 28rpx;
  box-sizing: border-box;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-all-input {
  display: block;
  flex: 1;
  min-width: 0;
  width: 100%;
  height: 84rpx;
  line-height: 84rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: #F5F8FA;
  border: 2rpx solid #E2E8F0;
  color: #1A2332;
  font-size: 28rpx;
  box-sizing: border-box;
}

.batch-all-input:focus {
  border-color: #087CF0;
  background: #FFFFFF;
}

.claim-cancel,
.claim-confirm {
  flex: 1;
  height: 78rpx;
  line-height: 78rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.claim-cancel {
  background: #F1F5F9;
  color: #475569;
}

.claim-confirm {
  background: #EA580C;
  color: #FFFFFF;
}

/* ====== 费用列表两行模式 ====== */
.expense-item {
  display: flex;
  margin-bottom: 18rpx;
  background: #FFFFFF;
  border-radius: 22rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 8rpx 26rpx rgba(8, 124, 240, 0.06);
  overflow: hidden;
}

.expense-item--active {
  transform: scale(0.98);
  opacity: 0.9;
}

.expense-bar {
  width: 6rpx;
  background: linear-gradient(180deg, #C65A4A, #F2A88D);
  flex-shrink: 0;
}

.expense-body {
  flex: 1;
  min-width: 0;
  padding: 22rpx 24rpx;
  box-sizing: border-box;
}

.expense-row1 {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.expense-content {
  flex: 1;
  min-width: 0;
  font-size: 29rpx;
  line-height: 40rpx;
  font-weight: 800;
  color: #102A3A;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expense-amount {
  font-size: 29rpx;
  line-height: 40rpx;
  font-weight: 800;
  color: #C65A4A;
  flex-shrink: 0;
}

.expense-amount.income {
  color: #0F766E;
}

.expense-row2 {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 12rpx;
  min-width: 0;
}

.expense-meta {
  flex-shrink: 0;
  font-size: 23rpx;
  line-height: 32rpx;
  color: #708196;
  font-weight: 600;
}

.expense-meta.date {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expense-status {
  flex-shrink: 0;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  line-height: 30rpx;
  font-weight: 500;
}

.expense-status.status-verified {
  background: #E0F2FE;
  color: #075985;
}

.expense-status.status-pending {
  background: #FEF3C7;
  color: #92400E;
}

/* ====== 其他模块卡片模式 ====== */
.record-card {
  display: flex;
  margin-bottom: 16rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(8, 124, 240, 0.06);
  overflow: hidden;
}

.card-bar {
  width: 4rpx;
  background: linear-gradient(180deg, #087CF0, #A8C7E5);
  flex-shrink: 0;
}

.card-body {
  flex: 1;
  padding: 24rpx 28rpx;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.record-title {
  flex: 1;
  font-size: 30rpx;
  line-height: 42rpx;
  font-weight: 700;
  color: #1A2332;
}

.record-id {
  padding: 4rpx 14rpx;
  background: #E8EEF5;
  color: #5A6B7F;
  font-size: 20rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.advance-amount {
  font-size: 30rpx;
  font-weight: 700;
  color: #B86620;
  flex-shrink: 0;
  margin-left: 12rpx;
}

.advance-subrow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14rpx;
}

.advance-date {
  font-size: 24rpx;
  color: #5A6B7F;
}

.advance-status {
  font-size: 24rpx;
  font-weight: 500;
}

.advance-status.tone-ok {
  color: #1F7F4C;
}

.advance-status.tone-warn {
  color: #B86620;
}

.product-status {
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
  margin-left: 12rpx;
}

.product-status.tone-ok {
  color: #1F7F4C;
  background: #E6F5EC;
}

.product-status.tone-warn {
  color: #B86620;
  background: #FCEFE0;
}

.purchase-total-amount {
  font-size: 30rpx;
  font-weight: 700;
  color: #B86620;
  flex-shrink: 0;
  margin-left: 12rpx;
}

.purchase-summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  margin-top: 16rpx;
}

.purchase-product-names {
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #EEF2F7;
  color: #1A2332;
  font-size: 28rpx;
  font-weight: 600;
  line-height: 1.5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.purchase-product-names > text:first-child {
  flex: 1;
  min-width: 0;
}

.purchase-status-tag {
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
}

.purchase-status-0 {
  color: #5A6B7F;
  background: #E8EEF5;
}

.purchase-status-1 {
  background: #FEF3C7;
  color: #92400E;
}

.purchase-status-2 {
  background: #E0F2FE;
  color: #075985;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  margin-top: 16rpx;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.summary-label {
  font-size: 22rpx;
  color: #94A3B8;
}

.summary-value {
  font-size: 26rpx;
  color: #1A2332;
  font-weight: 500;
}

.summary-value.tone-money {
  color: #B45309;
  font-weight: 700;
}

.summary-value.tone-points {
  color: #087CF0;
  font-weight: 700;
}

.summary-value.tone-percent {
  color: #7C3AED;
  font-weight: 700;
}

.summary-value.tone-danger,
.summary-value.status-danger {
  color: #B91C1C;
  font-weight: 700;
}

.summary-value.status-ok {
  color: #047857;
  font-weight: 700;
}

.summary-value.status-warn {
  color: #B45309;
  font-weight: 700;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid #E8EEF5;
}

.meta-text {
  font-size: 24rpx;
  color: #94A3B8;
}

.sale-footer-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10rpx;
  flex: 1;
}

.sale-footer-date,
.sale-footer-status {
  font-size: 22rpx;
  line-height: 1.3;
  font-weight: 600;
}

.sale-footer-status {
  margin-left: auto;
}

.card-footer.sale-footer-status-ok {
  padding: 12rpx 16rpx;
  border-top: none;
  border-radius: 12rpx;
  background: #D1FAE5;
  color: #065F46;
}

.card-footer.sale-footer-status-warn {
  padding: 12rpx 16rpx;
  border-top: none;
  border-radius: 12rpx;
  background: #FEF3C7;
  color: #92400E;
}

.card-footer.sale-footer-status-info {
  padding: 12rpx 16rpx;
  border-top: none;
  border-radius: 12rpx;
  background: #E0F2FE;
  color: #075985;
}

.arrow-icon {
  font-size: 36rpx;
  color: #CBD5E1;
  font-weight: 300;
  line-height: 1;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1rpx solid #E2E8F0;
}

.add-button {
  width: 320rpx;
  height: 84rpx;
  line-height: 84rpx;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
  font-size: 28rpx;
  border-radius: 999rpx;
  text-align: center;
  box-shadow: 0 6rpx 20rpx rgba(8, 124, 240, 0.25);
}

.empty,
.load-error,
.loading {
  padding: 60rpx;
  text-align: center;
  color: #94A3B8;
  font-size: 26rpx;
}

.retry-button {
  width: 220rpx;
  height: 72rpx;
  margin: 28rpx auto 0;
  padding: 0;
  border: 1rpx solid #B8D4F0;
  border-radius: 8rpx;
  background: #E8F3FF;
  color: #087CF0;
  font-size: 26rpx;
  line-height: 72rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A2332;
}

.empty-subtitle {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #94A3B8;
}
</style>
