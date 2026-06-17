<template>
  <view class="page">
    <!-- 顶部渐变区域 -->
    <view class="header">
      <view class="header-bg"></view>
      <view class="header-content" :style="headerContentStyle">
        <view class="header-row">
          <view class="header-left">
            <text class="header-title">JunSong 运营</text>
            <text class="header-sub">{{ greeting }}，{{ nickName }}</text>
          </view>
        </view>
        <picker v-if="canSwitchDept" :range="deptNames" :value="deptIndex" @change="onDeptChange">
          <view class="dept-switch" hover-class="dept-switch--active">
            <view class="dept-mark"><text class="dept-mark-text">店</text></view>
            <view class="dept-copy">
              <text class="dept-label">当前部门</text>
              <text class="dept-name">{{ currentDeptName || '选择部门' }}</text>
            </view>
            <text class="dept-arrow">›</text>
          </view>
        </picker>
        <view v-else-if="currentDeptName" class="dept-static">
          <text class="dept-static-label">当前部门</text>
          <text class="dept-static-name">{{ currentDeptName }}</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="scroll" :style="scrollStyle" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

      <!-- 核心指标卡片行 -->
      <view class="kpi-row fade-in-up" style="animation-delay:0.05s">
        <view class="kpi-card">
          <text class="kpi-value primary">{{ stats.todayMembers || 0 }}</text>
          <text class="kpi-label">今日新增</text>
          <view class="kpi-trend" v-if="stats.yesterdayMembers !== undefined">
            <text class="trend-tag" :class="stats.todayMembers >= stats.yesterdayMembers ? 'up' : 'down'">
              {{ stats.todayMembers >= stats.yesterdayMembers ? '↑' : '↓' }}
            </text>
          </view>
        </view>
        <view class="kpi-card">
          <text class="kpi-value success">{{ fmtMoney(stats.todaySale) }}</text>
          <text class="kpi-label">今日销售</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-value warning">{{ fmtMoney(stats.todayExpense) }}</text>
          <text class="kpi-label">今日费用</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-value info">{{ stats.totalMembers || 0 }}</text>
          <text class="kpi-label">总会员</text>
        </view>
      </view>

      <!-- 店面回本情况 -->
      <view class="section-card fade-in-up" style="animation-delay:0.12s" v-if="period || periodFallback">
        <view class="section-header">
          <view class="section-dot" style="background:#2A6F97"></view>
          <text class="section-title">店面回本情况</text>
          <text class="section-badge" v-if="periodStatusText" :class="periodStatusClass">{{ periodStatusText }}</text>
        </view>
        <view class="breakeven-body">
          <!-- 环形进度 -->
          <view class="ring-wrap">
            <view class="ring-outer">
              <view class="ring-track"></view>
              <view class="ring-progress" :style="{ '--progress': breakevenProgress }"></view>
            </view>
            <view class="ring-inner">
              <text class="ring-percent">{{ breakevenProgress }}%</text>
              <text class="ring-label">回本进度</text>
            </view>
          </view>
          <!-- 关键数据 -->
          <view class="breakeven-data">
            <view class="be-item">
              <text class="be-label">回本时间</text>
              <text class="be-value">{{ period.breakEvenTime || '进行中' }}</text>
            </view>
            <view class="be-item">
              <text class="be-label">净利</text>
              <text class="be-value" :class="netProfitClass">{{ fmtMoney(period.netProfit) }}</text>
            </view>
            <view class="be-item">
              <text class="be-label">店长分润</text>
              <text class="be-value accent">{{ fmtMoney(period.managerProfitAmount) }}</text>
            </view>
            <view class="be-item">
              <text class="be-label">投资人返款</text>
              <text class="be-value accent">{{ fmtMoney(period.investorProfitAmount) }}</text>
            </view>
          </view>
        </view>
        <!-- 迷你对比条 -->
        <view class="mini-bar-row">
          <view class="mini-bar-item">
            <text class="mini-bar-label">销售缴款</text>
            <view class="mini-bar-track"><view class="mini-bar-fill sale" :style="{ width: saleBarWidth }"></view></view>
            <text class="mini-bar-val">{{ fmtMoney(period.totalSalePayment) }}</text>
          </view>
          <view class="mini-bar-item">
            <text class="mini-bar-label">已核销费用</text>
            <view class="mini-bar-track"><view class="mini-bar-fill expense" :style="{ width: expenseBarWidth }"></view></view>
            <text class="mini-bar-val">{{ fmtMoney(period.totalVerifiedExpense) }}</text>
          </view>
        </view>
      </view>

      <!-- 会员情况 -->
      <view class="section-card fade-in-up" style="animation-delay:0.19s" v-if="stats">
        <view class="section-header">
          <view class="section-dot" style="background:#10B981"></view>
          <text class="section-title">会员情况</text>
        </view>
        <view class="member-stats-row">
          <view class="ms-item">
            <text class="ms-value primary">{{ stats.todayMembers || 0 }}</text>
            <text class="ms-label">今日新增</text>
          </view>
          <view class="ms-item">
            <text class="ms-value">{{ stats.totalMembers || 0 }}</text>
            <text class="ms-label">总会员数</text>
          </view>
          <view class="ms-item">
            <text class="ms-value success">{{ stats.activeMembers || 0 }}</text>
            <text class="ms-label">活跃会员</text>
          </view>
          <view class="ms-item">
            <text class="ms-value warning">{{ stats.pointsExchangeCount || 0 }}</text>
            <text class="ms-label">积分兑换</text>
          </view>
        </view>
        <!-- 会员卡类型分布 横向柱状图 -->
        <view class="bar-chart" v-if="cardTypeDistribution.length">
          <text class="bar-chart-title">会员卡类型分布</text>
          <view class="bar-row" v-for="(item, idx) in cardTypeDistribution" :key="idx">
            <text class="bar-name">{{ item.name }}</text>
            <view class="bar-track">
              <view class="bar-fill" :style="{ width: item.percent + '%', background: barColors[idx % barColors.length] }"></view>
            </view>
            <text class="bar-val">{{ item.count }}</text>
          </view>
        </view>
      </view>

      <!-- 秒杀情况 -->
      <view class="section-card fade-in-up" style="animation-delay:0.26s" v-if="seckillList.length">
        <view class="section-header">
          <view class="section-dot" style="background:#F97316"></view>
          <text class="section-title">秒杀活动</text>
          <text class="section-badge seckill">{{ seckillList.length }} 进行中</text>
        </view>
        <view class="seckill-list">
          <view class="seckill-item" v-for="item in seckillList" :key="item.seckillId" @tap="openModule('seckill')">
            <view class="sk-top">
              <text class="sk-name">{{ item.seckillName }}</text>
              <text class="sk-type">{{ item.seckillType || '秒杀' }}</text>
            </view>
            <view class="sk-info-grid">
              <view class="sk-info-cell">
                <text class="sk-info-label">秒杀单价</text>
                <text class="sk-info-value price">¥{{ item.seckillPrice || 0 }}</text>
              </view>
              <view class="sk-info-cell">
                <text class="sk-info-label">已秒杀份额</text>
                <text class="sk-info-value">{{ item.soldShares || 0 }}份</text>
              </view>
              <view class="sk-info-cell">
                <text class="sk-info-label">秒杀人数</text>
                <text class="sk-info-value">{{ item.seckillCount || 0 }}人</text>
              </view>
              <view class="sk-info-cell">
                <text class="sk-info-label">总金额</text>
                <text class="sk-info-value accent">¥{{ fmtMoney(item.totalAmount || (item.seckillPrice * (item.soldShares || 0))) }}</text>
              </view>
            </view>
            <view class="sk-footer">
              <view class="sk-progress-row">
                <view class="sk-progress-track">
                  <view class="sk-progress-fill" :style="{ width: seckillProgress(item) + '%' }"></view>
                </view>
                <text class="sk-progress-text">已领{{ seckillProgress(item) }}%</text>
              </view>
              <text class="sk-date" v-if="item.seckillDate || item.startDate">{{ item.seckillDate || item.startDate }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 待处理提醒 -->
      <view class="alert-card fade-in-up" style="animation-delay:0.30s" v-if="stats && (stats.unverifiedExpense || stats.unverifiedAdvance)">
        <view class="alert-header">
          <view class="alert-icon-wrap"><text class="alert-icon-text">!</text></view>
          <text class="alert-title">待处理</text>
        </view>
        <view class="alert-row" v-if="stats.unverifiedExpense">
          <text class="alert-label">未核销费用</text>
          <text class="alert-value warn">¥{{ fmtMoney(stats.unverifiedExpense) }}</text>
        </view>
        <view class="alert-row" v-if="stats.unverifiedAdvance">
          <text class="alert-label">未核销借支</text>
          <text class="alert-value warn">¥{{ fmtMoney(stats.unverifiedAdvance) }}</text>
        </view>
      </view>

      <view v-if="adminUser" class="server-card fade-in-up" style="animation-delay:0.32s">
        <view class="server-head">
          <view>
            <text class="server-title">服务器状态</text>
            <text class="server-sub">核心服务运行概览</text>
          </view>
          <view class="server-pill" :class="serverStatusClass">
            <text class="server-pill-text">{{ serverStatusText }}</text>
          </view>
        </view>
        <view class="health-grid" v-if="serverStatus && serverStatus.summary.total">
          <view class="health-item" v-for="item in systemHealthItems" :key="item.key">
            <view class="health-copy">
              <text class="health-label">{{ item.label }}</text>
              <text class="health-value">{{ item.value }}%</text>
            </view>
            <view class="health-track">
              <view class="health-fill" :style="{ width: item.value + '%', background: item.color }"></view>
            </view>
          </view>
        </view>
        <view class="server-grid" v-if="serverStatus && serverStatus.services.length">
          <view class="server-item" v-for="item in serverStatus.services" :key="item.key || item.name">
            <view class="server-dot" :class="{ 'server-dot--ok': item.ok }"></view>
            <view class="server-info">
              <text class="server-name">{{ item.name || item.key }}</text>
              <text class="server-desc">{{ item.statusText }}</text>
            </view>
          </view>
        </view>
        <view class="server-empty" v-else>
          <text class="server-empty-text">{{ serverStatusLoading ? '正在检测服务状态' : '暂无状态数据' }}</text>
        </view>
      </view>

      <view class="quick-section fade-in-up" style="animation-delay:0.34s" v-if="filteredQuickActions.length">
        <text class="section-label">常用操作</text>
        <view class="quick-grid">
          <view class="quick-item" hover-class="quick-item--active" v-for="action in filteredQuickActions" :key="action.key" @tap="openModule(action.key)">
            <view class="quick-icon" :style="{ background: getQuickBg(action.key) }">
              <text class="quick-icon-text" :style="{ color: getQuickColor(action.key) }">{{ getQuickLetter(action.key) }}</text>
            </view>
            <text class="quick-name">{{ action.name }}</text>
          </view>
        </view>
      </view>

      <view class="empty" v-if="!stats && !loading">
        <text class="empty-title">暂无数据</text>
        <text class="empty-sub">请检查网络或重新登录</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { groups, modules } from '@/config/modules.js'
import { request, getToken } from '@/api/index.js'
import { filterAuthorizedGroups, hasModulePermission, isAdmin } from '@/utils/permission.js'
import { applySeckillStats } from '@/utils/seckillStats.js'
import { SERVICE_STATUS_TARGETS, buildSystemHealthItems, normalizeDeptOptions, resolveCurrentDept, normalizeServerStatus, isSystemAdminUser } from '@/utils/homeControl.js'

const MODULE_BG = {
  member: 'rgba(42,111,151,0.08)', pointsGoods: 'rgba(59,130,246,0.08)', pointsRecord: 'rgba(42,111,151,0.08)',
  pointsExchange: 'rgba(139,92,246,0.08)', seckill: 'rgba(249,115,22,0.08)', seckillRecord: 'rgba(234,88,12,0.08)',
  expense: 'rgba(239,68,68,0.08)', advance: 'rgba(139,92,246,0.08)', product: 'rgba(59,130,246,0.08)',
  supplier: 'rgba(107,114,128,0.08)', purchase: 'rgba(249,115,22,0.08)', sale: 'rgba(16,185,129,0.08)',
  investorPayment: 'rgba(236,72,153,0.08)', investor: 'rgba(14,165,233,0.08)', investRecord: 'rgba(16,185,129,0.08)',
  deptProfitConfig: 'rgba(107,114,128,0.08)', accountingPeriod: 'rgba(245,158,11,0.08)', profitShare: 'rgba(244,63,94,0.08)',
  costAccounting: 'rgba(6,182,212,0.08)', userManage: 'rgba(99,102,241,0.08)'
}

const MODULE_LETTER = {
  member: '会', pointsGoods: '品', pointsRecord: '记', pointsExchange: '兑',
  seckill: '秒', seckillRecord: '录', expense: '费', advance: '借',
  product: '商', supplier: '供', purchase: '进', sale: '销',
  investorPayment: '返', investor: '投', investRecord: '款',
  deptProfitConfig: '配', accountingPeriod: '核', profitShare: '润',
  costAccounting: '成', userManage: '管'
}

const MODULE_ICON_COLOR = {
  member: '#2A6F97', pointsGoods: '#3B82F6', pointsRecord: '#2A6F97',
  pointsExchange: '#8B5CF6', seckill: '#F97316', seckillRecord: '#EA580C',
  expense: '#EF4444', advance: '#8B5CF6', product: '#3B82F6',
  supplier: '#6B7280', purchase: '#F97316', sale: '#10B981',
  investorPayment: '#EC4899', investor: '#0EA5E9', investRecord: '#10B981',
  deptProfitConfig: '#6B7280', accountingPeriod: '#F59E0B', profitShare: '#F43F5E',
  costAccounting: '#06B6D4', userManage: '#6366F1'
}

const QUICK_ACTIONS = [
  { key: 'member', name: '会员记录' },
  { key: 'seckillRecord', name: '秒杀记录' },
  { key: 'expense', name: '费用记录' }
]

export default {
  data() {
    return {
      stats: null,
      period: null,
      seckillList: [],
      serverStatus: null,
      serverStatusLoading: false,
      modules: [],
      refreshing: false,
      loading: false,
      nickName: '',
      userInfo: {},
      systemPermissions: [],
      deptList: [],
      currentDeptId: null,
      switchingDept: false,
      statusBarH: 0,
      menuButton: null,
      barColors: ['#2A6F97', '#3B82F6', '#8B5CF6', '#10B981', '#F59E0B', '#EC4899']
    }
  },
  computed: {
    greeting() {
      const h = new Date().getHours()
      if (h < 6) return '凌晨好'
      if (h < 12) return '上午好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    },
    filteredQuickActions() {
      return QUICK_ACTIONS.filter(a => hasModulePermission(a.key, this.modules))
    },
    adminUser() {
      return isAdmin(this.modules) || isSystemAdminUser(this.userInfo, this.systemPermissions)
    },
    deptNames() {
      return this.deptList.map(d => d.displayName || d.name)
    },
    deptIndex() {
      const idx = this.deptList.findIndex(d => String(d.id) === String(this.currentDeptId))
      return idx >= 0 ? idx : 0
    },
    currentDept() {
      return resolveCurrentDept(this.deptList, this.currentDeptId)
    },
    currentDeptName() {
      return this.currentDept?.name || this.userInfo.deptName || ''
    },
    canSwitchDept() {
      return this.deptList.length > 1 && (this.adminUser || this.deptList.length > 1)
    },
    serverStatusText() {
      if (this.serverStatusLoading) return '检测中'
      if (!this.serverStatus || !this.serverStatus.summary.total) return '未知'
      return this.serverStatus.summary.abnormal ? '异常' : '在线'
    },
    serverStatusClass() {
      if (this.serverStatusLoading) return 'server-pill--loading'
      if (!this.serverStatus || !this.serverStatus.summary.total) return 'server-pill--unknown'
      return this.serverStatus.summary.abnormal ? 'server-pill--warn' : 'server-pill--ok'
    },
    systemHealthItems() {
      return buildSystemHealthItems(this.serverStatus)
    },
    headerHeight() {
      const top = this.menuButton?.bottom ? this.menuButton.bottom + 10 : this.statusBarH + 42
      return top + uni.upx2px(this.currentDeptName ? 236 : 156)
    },
    headerContentStyle() {
      const top = this.menuButton?.bottom ? this.menuButton.bottom + 10 : this.statusBarH + 42
      return {
        paddingTop: top + 'px',
        paddingRight: '32rpx'
      }
    },
    scrollStyle() {
      return {
        paddingTop: (this.headerHeight + uni.upx2px(18)) + 'px',
        height: '100vh'
      }
    },
    // 回本进度百分比
    breakevenProgress() {
      if (!this.period) return 0
      const p = this.period
      if (p.status === '1' || p.status === '2') return 100
      const sale = Number(p.totalSalePayment) || 0
      const expense = Number(p.totalVerifiedExpense) || 0
      const purchase = Number(p.totalPurchase) || 0
      const totalCost = expense + purchase
      if (totalCost <= 0) return sale > 0 ? 100 : 0
      const progress = Math.min(Math.round((sale / totalCost) * 100), 100)
      return Math.max(progress, 0)
    },
    // 核算周期状态文本
    periodStatusText() {
      if (!this.period) return ''
      const s = this.period.status
      if (s === '0') return '进行中'
      if (s === '1') return '已回本'
      if (s === '2') return '已结转'
      return ''
    },
    periodStatusClass() {
      if (!this.period) return ''
      const s = this.period.status
      if (s === '0') return 'running'
      if (s === '1') return 'done'
      if (s === '2') return 'closed'
      return ''
    },
    netProfitClass() {
      if (!this.period) return ''
      const n = Number(this.period.netProfit) || 0
      return n >= 0 ? 'positive' : 'negative'
    },
    // 销售缴款对比条宽度
    saleBarWidth() {
      if (!this.period) return '0%'
      const sale = Number(this.period.totalSalePayment) || 0
      const expense = Number(this.period.totalVerifiedExpense) || 0
      const max = Math.max(sale, expense, 1)
      return Math.round((sale / max) * 100) + '%'
    },
    expenseBarWidth() {
      if (!this.period) return '0%'
      const sale = Number(this.period.totalSalePayment) || 0
      const expense = Number(this.period.totalVerifiedExpense) || 0
      const max = Math.max(sale, expense, 1)
      return Math.round((expense / max) * 100) + '%'
    },
    // 会员卡类型分布
    cardTypeDistribution() {
      if (!this.stats || !this.stats.cardTypeStats) return []
      const data = this.stats.cardTypeStats
      if (!Array.isArray(data) || !data.length) return []
      const maxCount = Math.max(...data.map(d => d.count || 0), 1)
      return data.map(d => ({
        name: d.name || d.cardType || '未知',
        count: d.count || 0,
        percent: Math.round(((d.count || 0) / maxCount) * 100)
      }))
    },
    // 占位数据：当 period API 失败时使用
    periodFallback() {
      return this.period
    }
  },
  onShow() {
    if (!getToken()) {
      uni.reLaunch({ url: '/pages/login/index' })
      return
    }
    const systemInfo = uni.getSystemInfoSync()
    this.statusBarH = systemInfo.statusBarHeight || 20
    try {
      this.menuButton = uni.getMenuButtonBoundingClientRect()
    } catch (e) {
      this.menuButton = null
    }
    const userInfo = uni.getStorageSync('userInfo') || {}
    this.userInfo = userInfo
    this.nickName = userInfo.nickName || userInfo.username || ''
    this.currentDeptId = userInfo.currentDeptId || userInfo.deptId || null
    this.modules = uni.getStorageSync('modules') || []
    this.refreshModules()
    this.loadUserContext()
    this.loadDashboard()
    this.loadPeriod()
    this.loadSeckill()
  },
  methods: {
    go(url) {
      uni.navigateTo({ url })
    },
    openModule(key) {
      if (!hasModulePermission(key, this.modules)) {
        uni.showToast({ title: '暂无该功能权限', icon: 'none' })
        return
      }
      if (key === 'userManage') {
        uni.navigateTo({ url: '/pages/user/index' })
      } else {
        uni.navigateTo({ url: '/pages/list/index?module=' + key })
      }
    },
    getModuleBg(key) {
      return MODULE_BG[key] || 'rgba(148,163,184,0.08)'
    },
    getModuleLetter(key) {
      return MODULE_LETTER[key] || key.charAt(0).toUpperCase()
    },
    getModuleColor(key) {
      return MODULE_ICON_COLOR[key] || '#94A3B8'
    },
    getQuickBg(key) {
      return MODULE_BG[key] || 'rgba(148,163,184,0.08)'
    },
    getQuickLetter(key) {
      return MODULE_LETTER[key] || key.charAt(0).toUpperCase()
    },
    getQuickColor(key) {
      return MODULE_ICON_COLOR[key] || '#94A3B8'
    },
    fmtMoney(val) {
      if (!val && val !== 0) return '0'
      const n = Number(val)
      if (isNaN(n)) return '0'
      if (n >= 10000) return (n / 10000).toFixed(1) + '万'
      return n.toFixed(n % 1 === 0 ? 0 : 2)
    },
    seckillProgress(item) {
      return Number(item.claimProgress) || 0
    },
    async loadDashboard() {
      this.loading = true
      try {
        const res = await request({ url: '/member/mp/dashboard/stats', method: 'GET' })
        this.stats = res.data || res
      } catch (e) {
        console.log('dashboard load failed', e)
        // 占位数据
        if (!this.stats) {
          this.stats = {
            todayMembers: 0, totalMembers: 0, activeMembers: 0,
            todaySale: 0, todayExpense: 0, totalSale: 0, totalExpense: 0,
            totalPurchase: 0, pointsExchangeCount: 0
          }
        }
      } finally {
        this.loading = false
      }
    },
    async loadPeriod() {
      try {
        const res = await request({ url: '/finance/accountingPeriod/current', method: 'GET' })
        this.period = res.data || res || null
      } catch (e) {
        console.log('period load failed', e)
        // 占位数据
        this.period = {
          status: '0', totalSalePayment: 0, totalVerifiedExpense: 0,
          totalPurchase: 0, netProfit: 0, managerProfitAmount: 0,
          investorProfitAmount: 0, breakEvenTime: ''
        }
      }
    },
    async loadSeckill() {
      try {
        const res = await request({ url: '/member/seckill/list', method: 'GET', data: { status: '0' } })
        const list = res.data || res || []
        const activities = Array.isArray(list) ? list : (list.rows || [])
        this.seckillList = await Promise.all(activities.map(async (item) => {
          try {
            const statsRes = await request({ url: '/member/seckillRecord/statistics', method: 'GET', data: { seckillId: item.seckillId } })
            return applySeckillStats(item, statsRes.data || statsRes || {})
          } catch (e) {
            console.log('seckill statistics load failed', e)
            return applySeckillStats(item, {})
          }
        }))
      } catch (e) {
        console.log('seckill load failed', e)
        this.seckillList = []
      }
    },
    async loadServerStatus() {
      if (!this.adminUser) return
      this.serverStatusLoading = true
      const services = await Promise.all(SERVICE_STATUS_TARGETS.map(async (target) => {
        try {
          const res = await request({ url: target.url, method: 'GET', noRedirect: true, silent: true, timeout: 3000 })
          return { ...target, status: res.status || res.code || 'OK', ok: res.status === 'UP' || res.code === 200 || res.status === undefined }
        } catch (e) {
          return { ...target, status: 'DOWN', ok: false }
        }
      }))
      this.serverStatus = normalizeServerStatus(services)
      this.serverStatusLoading = false
    },
    async refreshModules() {
      try {
        const res = await request({ url: '/member/mp/modules', method: 'GET' })
        const modules = res.data || res || []
        const moduleList = Array.isArray(modules) ? modules : []
        this.modules = moduleList.length || !this.adminUser ? moduleList : ['member', 'seckillRecord', 'expense', 'userManage']
        uni.setStorageSync('modules', this.modules)
      } catch (e) {
        console.log('modules refresh failed', e)
      }
    },
    async loadUserContext() {
      try {
        const res = await request({ url: '/system/user/getInfo', method: 'GET', noRedirect: true })
        const user = res.user || {}
        this.systemPermissions = res.permissions || []
        this.userInfo = { ...this.userInfo, ...user, depts: res.depts || [], currentDeptId: res.currentDeptId }
        this.nickName = user.nickName || user.userName || user.username || this.nickName
        this.currentDeptId = res.currentDeptId || user.deptId || this.currentDeptId
        if (this.adminUser && (!this.modules || this.modules.length === 0)) {
          this.modules = ['member', 'seckillRecord', 'expense', 'userManage']
          uni.setStorageSync('modules', this.modules)
        }
        uni.setStorageSync('userInfo', this.userInfo)
        this.deptList = normalizeDeptOptions(res.depts || [])
        if (this.adminUser) {
          await this.loadAllDepts()
        }
        const current = resolveCurrentDept(this.deptList, this.currentDeptId)
        if (current) this.currentDeptId = current.id
        this.loadServerStatus()
      } catch (e) {
        const cached = this.userInfo || {}
        this.deptList = normalizeDeptOptions(cached.depts || [])
      }
    },
    async loadAllDepts() {
      try {
        const res = await request({ url: '/system/user/deptTree', method: 'GET', noRedirect: true })
        const list = normalizeDeptOptions(res.data || res.depts || [])
        if (list.length) this.deptList = list
      } catch (e) {
        console.log('dept tree load failed', e)
      }
    },
    async onDeptChange(e) {
      const index = Number(e.detail.value)
      const target = this.deptList[index]
      if (!target || String(target.id) === String(this.currentDeptId) || this.switchingDept) return
      this.switchingDept = true
      uni.showLoading({ title: '切换中' })
      try {
        await request({ url: `/system/user/switchDept/${target.id}`, method: 'POST' })
        this.currentDeptId = target.id
        this.userInfo = { ...this.userInfo, deptId: target.id, currentDeptId: target.id, deptName: target.name }
        uni.setStorageSync('userInfo', this.userInfo)
        await Promise.all([this.loadDashboard(), this.loadPeriod(), this.loadSeckill()])
        uni.showToast({ title: '已切换部门', icon: 'success' })
      } catch (err) {
        uni.showToast({ title: err?.msg || err?.message || '部门切换失败', icon: 'none' })
      } finally {
        this.switchingDept = false
        uni.hideLoading()
      }
    },
    onRefresh() {
      this.refreshing = true
      Promise.all([this.refreshModules(), this.loadUserContext(), this.loadDashboard(), this.loadPeriod(), this.loadSeckill(), this.loadServerStatus()]).finally(() => {
        this.refreshing = false
      })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  width: 100vw;
  max-width: 100vw;
  background: #F0F4F8;
  overflow: hidden;
  box-sizing: border-box;
}

/* ===== 头部 ===== */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 20;
  overflow: hidden;
  width: 100%;
  max-width: 750rpx;
  margin: 0 auto;
  box-sizing: border-box;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #173B57 0%, #2A6F97 40%, #3A8DB8 80%, #8EC8D2 100%);
}

.header-bg::after {
  content: '';
  position: absolute;
  bottom: -38rpx;
  left: -8%;
  right: -8%;
  height: 88rpx;
  background: #F0F4F8;
  border-radius: 50% 50% 0 0;
}

.header-content {
  position: relative;
  min-height: 168rpx;
  padding: 28rpx 32rpx 48rpx;
  box-sizing: border-box;
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.header-left {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.header-title {
  font-size: 42rpx;
  font-weight: 700;
  line-height: 1.18;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-sub {
  font-size: 25rpx;
  line-height: 1.35;
  color: rgba(255, 255, 255, 0.72);
  margin-top: 14rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dept-switch,
.dept-static {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
  min-height: 72rpx;
  padding: 12rpx 18rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.14);
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  box-sizing: border-box;
}

.dept-switch--active {
  transform: scale(0.98);
  opacity: 0.9;
}

.dept-mark {
  width: 44rpx;
  height: 44rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dept-mark-text {
  font-size: 22rpx;
  font-weight: 900;
  color: #2A6F97;
}

.dept-copy {
  flex: 1;
  min-width: 0;
  margin-left: 14rpx;
}

.dept-label,
.dept-static-label {
  display: block;
  font-size: 20rpx;
  line-height: 28rpx;
  color: rgba(255, 255, 255, 0.62);
}

.dept-name,
.dept-static-name {
  display: block;
  margin-top: 2rpx;
  font-size: 27rpx;
  line-height: 36rpx;
  font-weight: 800;
  color: #FFFFFF;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dept-arrow {
  margin-left: 12rpx;
  font-size: 38rpx;
  line-height: 44rpx;
  color: rgba(255, 255, 255, 0.72);
  transform: rotate(90deg);
}

.dept-static {
  justify-content: space-between;
  gap: 18rpx;
}

.dept-static-name {
  flex: 1;
  text-align: right;
}

/* ===== 滚动区 ===== */
.scroll {
  width: 100%;
  padding: 0 28rpx 40rpx;
  box-sizing: border-box;
  overflow-x: hidden;
}

/* ===== 入场动画 ===== */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(32rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-in-up {
  animation: fadeInUp 0.45s ease-out both;
}

/* ===== 核心指标卡片行 ===== */
.kpi-row {
  display: flex;
  gap: 16rpx;
  margin-top: 0;
  position: relative;
  z-index: 21;
  width: 100%;
  box-sizing: border-box;
}

.kpi-card {
  flex: 1;
  min-width: 0;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
  position: relative;
}

.kpi-value {
  font-size: 34rpx;
  font-weight: 700;
  color: #1A2332;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kpi-value.primary { color: #2A6F97; }
.kpi-value.success { color: #10B981; }
.kpi-value.warning { color: #F59E0B; }
.kpi-value.info { color: #3B82F6; }

.kpi-label {
  font-size: 20rpx;
  color: #94A3B8;
  margin-top: 8rpx;
}

.kpi-trend {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
}

.trend-tag {
  font-size: 18rpx;
  font-weight: 700;
  padding: 2rpx 8rpx;
  border-radius: 8rpx;
}

.trend-tag.up {
  color: #10B981;
  background: rgba(16, 185, 129, 0.1);
}

.trend-tag.down {
  color: #EF4444;
  background: rgba(239, 68, 68, 0.1);
}

/* ===== 通用区块卡片 ===== */
.section-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-top: 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(42, 111, 151, 0.06);
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.section-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
  flex: 1;
}

.section-badge {
  font-size: 20rpx;
  font-weight: 600;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
}

.section-badge.running {
  color: #2A6F97;
  background: rgba(42, 111, 151, 0.1);
}

.section-badge.done {
  color: #10B981;
  background: rgba(16, 185, 129, 0.1);
}

.section-badge.closed {
  color: #94A3B8;
  background: rgba(148, 163, 184, 0.1);
}

.section-badge.seckill {
  color: #F97316;
  background: rgba(249, 115, 22, 0.1);
}

/* ===== 店面回本 - 环形进度 ===== */
.breakeven-body {
  display: flex;
  align-items: center;
  gap: 32rpx;
}

.ring-wrap {
  position: relative;
  width: 200rpx;
  height: 200rpx;
  flex-shrink: 0;
}

.ring-outer {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  position: relative;
}

.ring-track {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 16rpx solid #E8F0F6;
}

.ring-progress {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 16rpx solid transparent;
  border-top-color: #2A6F97;
  border-right-color: #2A6F97;
  transform: rotate(calc(var(--progress) * 3.6deg - 90deg));
  transition: transform 0.8s ease-out;
}

/* 用 conic-gradient 实现精确进度 */
.ring-outer {
  background: conic-gradient(
    #2A6F97 calc(var(--progress) * 3.6deg),
    #E8F0F6 calc(var(--progress) * 3.6deg)
  );
  border-radius: 50%;
  -webkit-mask: radial-gradient(circle, transparent 70rpx, #000 72rpx);
  mask: radial-gradient(circle, transparent 70rpx, #000 72rpx);
}

.ring-track {
  display: none;
}

.ring-progress {
  display: none;
}

.ring-inner {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-percent {
  font-size: 44rpx;
  font-weight: 800;
  color: #2A6F97;
  line-height: 1;
}

.ring-label {
  font-size: 20rpx;
  color: #94A3B8;
  margin-top: 6rpx;
}

/* 回本关键数据 */
.breakeven-data {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx 24rpx;
}

.be-item {
  width: calc(50% - 12rpx);
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.be-label {
  font-size: 22rpx;
  color: #94A3B8;
}

.be-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
  margin-top: 4rpx;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.be-value.accent {
  color: #2A6F97;
}

.be-value.positive {
  color: #10B981;
}

.be-value.negative {
  color: #EF4444;
}

/* 迷你对比条 */
.mini-bar-row {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.mini-bar-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.mini-bar-label {
  font-size: 22rpx;
  color: #94A3B8;
  width: 120rpx;
  flex-shrink: 0;
}

.mini-bar-track {
  flex: 1;
  min-width: 0;
  height: 16rpx;
  background: #F0F4F8;
  border-radius: 8rpx;
  overflow: hidden;
}

.mini-bar-fill {
  height: 100%;
  border-radius: 8rpx;
  transition: width 0.8s ease-out;
}

.mini-bar-fill.sale {
  background: linear-gradient(90deg, #2A6F97, #8EC8D2);
}

.mini-bar-fill.expense {
  background: linear-gradient(90deg, #F59E0B, #FCD34D);
}

.mini-bar-val {
  font-size: 22rpx;
  font-weight: 600;
  color: #5A6B7F;
  width: 80rpx;
  text-align: right;
  flex-shrink: 0;
}

/* ===== 会员情况 ===== */
.member-stats-row {
  display: flex;
  gap: 12rpx;
  width: 100%;
  box-sizing: border-box;
}

.ms-item {
  flex: 1;
  min-width: 0;
  background: #F8FBFD;
  border-radius: 16rpx;
  padding: 20rpx 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.ms-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A2332;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ms-value.primary { color: #2A6F97; }
.ms-value.success { color: #10B981; }
.ms-value.warning { color: #F59E0B; }

.ms-label {
  font-size: 20rpx;
  color: #94A3B8;
  margin-top: 6rpx;
}

/* 横向柱状图 */
.bar-chart {
  margin-top: 24rpx;
}

.bar-chart-title {
  font-size: 24rpx;
  font-weight: 600;
  color: #5A6B7F;
  margin-bottom: 16rpx;
  display: block;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 14rpx;
}

.bar-name {
  font-size: 22rpx;
  color: #5A6B7F;
  width: 100rpx;
  flex-shrink: 0;
  text-align: right;
}

.bar-track {
  flex: 1;
  min-width: 0;
  height: 24rpx;
  background: #F0F4F8;
  border-radius: 12rpx;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 12rpx;
  transition: width 0.8s ease-out;
  min-width: 8rpx;
}

.bar-val {
  font-size: 22rpx;
  font-weight: 600;
  color: #1A2332;
  width: 60rpx;
  text-align: right;
  flex-shrink: 0;
}

/* ===== 秒杀情况 ===== */
.seckill-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.seckill-item {
  background: #FFFAF5;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  border: 1rpx solid rgba(249, 115, 22, 0.1);
}

.sk-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.sk-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #1A2332;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sk-type {
  font-size: 20rpx;
  font-weight: 600;
  color: #F97316;
  background: rgba(249, 115, 22, 0.1);
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
}

.sk-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12rpx 24rpx;
  margin-bottom: 16rpx;
}

.sk-info-cell {
  display: flex;
  flex-direction: column;
}

.sk-info-label {
  font-size: 20rpx;
  color: #94A3B8;
}

.sk-info-value {
  font-size: 26rpx;
  font-weight: 700;
  color: #1A2332;
  margin-top: 4rpx;
}

.sk-info-value.price {
  color: #F97316;
}

.sk-info-value.accent {
  color: #2A6F97;
}

.sk-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.sk-progress-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  min-width: 0;
}

.sk-progress-track {
  flex: 1;
  min-width: 0;
  height: 12rpx;
  background: #F0F4F8;
  border-radius: 6rpx;
  overflow: hidden;
}

.sk-progress-fill {
  height: 100%;
  border-radius: 6rpx;
  background: linear-gradient(90deg, #F97316, #FCD34D);
  transition: width 0.8s ease-out;
}

.sk-progress-text {
  font-size: 20rpx;
  font-weight: 600;
  color: #F97316;
  flex-shrink: 0;
}

.sk-date {
  font-size: 20rpx;
  color: #94A3B8;
  flex-shrink: 0;
}

/* ===== 待处理提醒 ===== */
.alert-card {
  background: #FFFBEB;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  margin-top: 24rpx;
  border: 1rpx solid #FEF3C7;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.alert-icon-wrap {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: #F59E0B;
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-icon-text {
  font-size: 22rpx;
  font-weight: 700;
  color: #ffffff;
}

.alert-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #92400E;
}

.alert-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
}

.alert-label {
  font-size: 26rpx;
  color: #5A6B7F;
}

.alert-value {
  font-size: 26rpx;
  font-weight: 600;
  color: #92400E;
}

.alert-value.warn {
  color: #D97706;
}

/* ===== 服务器状态 ===== */
.server-card {
  margin-top: 28rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: #FFFFFF;
  box-shadow: 0 12rpx 32rpx rgba(42, 111, 151, 0.08);
  border: 1rpx solid rgba(226, 232, 240, 0.92);
}

.server-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.server-title {
  display: block;
  font-size: 30rpx;
  line-height: 40rpx;
  font-weight: 900;
  color: #102A3A;
}

.server-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  line-height: 32rpx;
  color: #708196;
}

.server-pill {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.server-pill-text {
  font-size: 22rpx;
  line-height: 30rpx;
  font-weight: 800;
}

.server-pill--ok {
  background: #D1FAE5;
}

.server-pill--ok .server-pill-text {
  color: #047857;
}

.server-pill--warn {
  background: #FEE2E2;
}

.server-pill--warn .server-pill-text {
  color: #B91C1C;
}

.server-pill--unknown,
.server-pill--loading {
  background: #E0F2FE;
}

.server-pill--unknown .server-pill-text,
.server-pill--loading .server-pill-text {
  color: #075985;
}

.health-grid {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 24rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, rgba(42, 111, 151, 0.06), rgba(248, 250, 252, 0.96));
  box-sizing: border-box;
}

.health-copy {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 10rpx;
}

.health-label {
  font-size: 24rpx;
  font-weight: 700;
  color: #334E68;
}

.health-value {
  font-size: 23rpx;
  font-weight: 800;
  color: #64748B;
}

.health-track {
  height: 14rpx;
  border-radius: 999rpx;
  background: #E8F0F6;
  overflow: hidden;
}

.health-fill {
  height: 100%;
  border-radius: inherit;
}

.server-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 24rpx;
}

.server-item {
  display: flex;
  align-items: center;
  width: calc(50% - 7rpx);
  min-width: 0;
  padding: 18rpx 14rpx;
  border-radius: 20rpx;
  background: #F8FAFC;
  box-sizing: border-box;
}

.server-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #EF4444;
  flex-shrink: 0;
  box-shadow: 0 0 0 6rpx rgba(239, 68, 68, 0.1);
}

.server-dot--ok {
  background: #10B981;
  box-shadow: 0 0 0 6rpx rgba(16, 185, 129, 0.12);
}

.server-info {
  min-width: 0;
  margin-left: 14rpx;
}

.server-name,
.server-desc {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.server-name {
  font-size: 24rpx;
  line-height: 32rpx;
  font-weight: 800;
  color: #102A3A;
}

.server-desc {
  margin-top: 2rpx;
  font-size: 20rpx;
  line-height: 28rpx;
  color: #708196;
}

.server-empty {
  margin-top: 24rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  background: #F8FAFC;
  text-align: center;
}

.server-empty-text {
  font-size: 24rpx;
  color: #708196;
}

/* ===== 常用操作 ===== */
.quick-section {
  margin-top: 28rpx;
}

.section-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #5A6B7F;
  margin-bottom: 20rpx;
  padding-left: 4rpx;
  display: block;
}

.quick-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  width: 100%;
  box-sizing: border-box;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  max-width: 100%;
  padding: 0 22rpx;
  height: 80rpx;
  background: #ffffff;
  border-radius: 999rpx;
  box-shadow: 0 2rpx 8rpx rgba(42, 111, 151, 0.04);
  box-sizing: border-box;
}

.quick-item--active {
  transform: scale(0.96);
  opacity: 0.8;
}

.quick-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-icon-text {
  font-size: 22rpx;
  font-weight: 700;
}

.quick-name {
  font-size: 26rpx;
  color: #1A2332;
  font-weight: 500;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 空状态 ===== */
.empty {
  padding: 60rpx 30rpx;
  text-align: center;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A2332;
  display: block;
}

.empty-sub {
  font-size: 24rpx;
  color: #94A3B8;
  margin-top: 12rpx;
  display: block;
}
</style>
