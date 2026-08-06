<template>
  <view class="page">
    <!-- 顶部渐变区域 -->
    <view class="header">
      <view class="header-bg"></view>
      <view class="header-content" :style="headerContentStyle">
        <view class="header-row">
          <view class="header-left">
            <text class="header-title">松·云助手</text>
            <text class="header-sub">{{ greeting }}，{{ nickName }}</text>
          </view>
          <view class="header-right">
            <view v-if="currentDeptName" class="dept-switch-inline" @tap="openDeptPicker">
              <text class="dept-name-inline">{{ currentDeptName }}</text>
              <text class="work-view-label">{{ workView.label }}</text>
              <text v-if="canSwitchDept" class="dept-arrow-inline">▼</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 核心指标卡片行（浮在波浪上） -->
    <view class="kpi-row-wrap">
      <view class="kpi-row fade-in-up" style="animation-delay:0.05s">
        <view class="kpi-card">
          <text class="kpi-value primary">{{ fmtMoney(period?.totalSalePayment) }}</text>
          <text class="kpi-label">本周期实际缴款</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-value" :class="netProfitClass">{{ fmtMoney(period?.netProfit) }}</text>
          <text class="kpi-label">本周期净利</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-value warning">{{ fmtMoney(periodBreakEvenGap) }}</text>
          <text class="kpi-label">距离回本</text>
        </view>
        <view class="kpi-card">
          <text class="kpi-value info">{{ fmtMoney(periodTotalCost) }}</text>
          <text class="kpi-label">周期总成本</text>
        </view>
      </view>
    </view>

    <view v-if="periodStale" class="stale-banner">当前显示最近一次成功的核算周期数据，正在等待最新数据</view>
    <view v-if="!period && !periodLoading" class="period-empty section-card">
      <text class="section-title">暂无当前核算周期</text>
      <text class="empty-sub">请先初始化核算周期，或确认当前部门权限。</text>
      <text class="section-link" @tap="go('/pages/detail/index?type=accountingPeriod')">去查看核算周期 ›</text>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <!-- 店面回本情况 -->
      <view class="section-card fade-in-up" style="animation-delay:0.12s" v-if="period || periodFallback">
        <view class="section-header">
          <view class="section-dot" style="background:#087CF0"></view>
          <text class="section-title">{{ periodAudienceLabel }}回本情况</text>
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
      <view class="section-card fade-in-up" style="animation-delay:0.19s" v-if="showMemberSection" @tap="openModule('member')">
        <view class="section-header">
          <view class="section-dot" style="background:#10B981"></view>
          <text class="section-title">会员增长</text>
          <text class="section-link">查看 ›</text>
        </view>
        <view class="member-stats-row">
          <view class="ms-item">
            <text class="ms-value primary">{{ overviewMember.todayMembers || 0 }}</text>
            <text class="ms-label">今日新增</text>
          </view>
          <view class="ms-item">
            <text class="ms-value">{{ overviewMember.totalMembers || 0 }}</text>
            <text class="ms-label">总会员数</text>
          </view>
          <view class="ms-item">
            <text class="ms-value success">{{ overviewMember.activeMembers || 0 }}</text>
            <text class="ms-label">活跃会员</text>
          </view>
          <view class="ms-item">
            <text class="ms-value warning">{{ overviewMember.silentMembers || 0 }}</text>
            <text class="ms-label">沉默会员</text>
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

      <!-- 成长体系 -->
      <view class="section-card fade-in-up" style="animation-delay:0.22s" v-if="showGrowthSection" @tap="goMemberGrowth">
        <view class="section-header">
          <view class="section-dot" style="background:#8B5CF6"></view>
          <text class="section-title">成长体系</text>
          <text class="section-link">查看 ›</text>
        </view>
        <view class="member-stats-row">
          <view class="ms-item">
            <text class="ms-value primary">{{ overviewGrowth.todaySignInCount || 0 }}</text>
            <text class="ms-label">今日签到</text>
          </view>
          <view class="ms-item">
            <text class="ms-value">{{ overviewGrowth.avgGrowthValue || 0 }}</text>
            <text class="ms-label">平均成长值</text>
          </view>
        </view>
        <view class="bar-chart" v-if="showLevelDistribution">
          <text class="bar-chart-title">会员等级分布</text>
          <view class="bar-row" v-for="(item, idx) in overviewLevelDistribution" :key="idx">
            <text class="bar-name">{{ item.name }}</text>
            <view class="bar-track">
              <view class="bar-fill" :style="{ width: item.percent + '%', background: barColors[idx % barColors.length] }"></view>
            </view>
            <text class="bar-val">{{ item.count }}</text>
          </view>
        </view>
      </view>

      <!-- 增长动作 -->
      <view class="section-card fade-in-up" style="animation-delay:0.24s" v-if="showGrowthActionsSection" @tap="goMemberActions">
        <view class="section-header">
          <view class="section-dot" style="background:#0EA5E9"></view>
          <text class="section-title">增长动作</text>
          <text class="section-link">查看 ›</text>
        </view>
        <view class="member-stats-row">
          <view class="ms-item">
            <text class="ms-value warning">{{ overviewGrowthActions.pending }}</text>
            <text class="ms-label">待执行动作</text>
          </view>
          <view class="ms-item">
            <text class="ms-value success">{{ overviewGrowthActions.completed }}</text>
            <text class="ms-label">已完成动作</text>
          </view>
          <view class="ms-item">
            <text class="ms-value primary">{{ overviewGrowthActions.effectRate }}%</text>
            <text class="ms-label">完成率</text>
          </view>
        </view>
      </view>

      <!-- 积分运营 -->
      <view class="section-card fade-in-up" style="animation-delay:0.26s" v-if="showPointsSection" @tap="openModule('pointsExchange')">
        <view class="section-header">
          <view class="section-dot" style="background:#F59E0B"></view>
          <text class="section-title">积分运营</text>
          <text class="section-link">查看 ›</text>
        </view>
        <view class="member-stats-row">
          <view class="ms-item">
            <text class="ms-value warning">{{ overviewPoints.pendingExchangeCount || 0 }}</text>
            <text class="ms-label">待领取兑换</text>
          </view>
          <view class="ms-item">
            <text class="ms-value success">{{ overviewPoints.todayPointsIssued || 0 }}</text>
            <text class="ms-label">今日发放积分</text>
          </view>
          <view class="ms-item">
            <text class="ms-value primary">{{ overviewPoints.todayPointsConsumed || 0 }}</text>
            <text class="ms-label">今日消耗积分</text>
          </view>
        </view>
      </view>

      <!-- 分层洞察 -->
      <view class="section-card fade-in-up" style="animation-delay:0.28s" v-if="showSegmentSection" @tap="goMemberDashboard">
        <view class="section-header">
          <view class="section-dot" style="background:#EC4899"></view>
          <text class="section-title">分层洞察</text>
          <text class="section-link">查看 ›</text>
        </view>
        <view class="member-stats-row">
          <view class="ms-item" v-for="(item, idx) in overviewSegmentDistribution" :key="idx">
            <text class="ms-value" :class="segmentTone(item.name)">{{ item.count }}</text>
            <text class="ms-label">{{ item.name }}</text>
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

      <!-- 经营任务快捷入口（角色优先：店长/财务/管理员可见） -->
      <view class="section-card fade-in-up" style="animation-delay:0.33s" v-if="canViewOperatingTask" @tap="openOperatingTask">
        <view class="section-header">
          <view class="section-dot" style="background:#6366F1"></view>
          <text class="section-title">经营任务</text>
          <text class="section-badge warn" v-if="operatingTaskCount > 0">{{ operatingTaskCount }} 待办</text>
        </view>
        <view class="alert-row">
          <text class="alert-label">点击查看待处理任务、逾期提醒和优先事项</text>
          <text class="alert-value primary">›</text>
        </view>
      </view>

      <!-- 核算周期综合数据 -->
      <view class="section-card fade-in-up" style="animation-delay:0.32s" v-if="expenseSummary">
        <view class="section-header">
          <view class="section-dot" style="background:#F59E0B"></view>
          <text class="section-title">核算周期综合数据</text>
        </view>
        <view class="finance-grid">
          <view class="finance-item">
            <text class="finance-label">未核销借支</text>
            <text class="finance-value warn">{{ fmtMoneyWithSign(expenseSummary.unverifiedAdvanceAmount) }}</text>
          </view>
          <view class="finance-item">
            <text class="finance-label">未核销费用</text>
            <text class="finance-value warn">{{ fmtMoneyWithSign(expenseSummary.unverifiedExpenseAmount) }}</text>
          </view>
          <view class="finance-item">
            <text class="finance-label">借支余额</text>
            <text class="finance-value" :class="getBalanceClass(expenseSummary.advanceBalance)">{{ fmtMoneyWithSign(expenseSummary.advanceBalance) }}</text>
          </view>
          <view class="finance-item">
            <text class="finance-label">总费用</text>
            <text class="finance-value">{{ fmtMoneyWithSign(expenseSummary.totalExpenseAmount) }}</text>
          </view>
        </view>
      </view>

      <view class="empty" v-if="!stats && !loading">
        <text class="empty-title">暂无数据</text>
        <text class="empty-sub">请检查网络或重新登录</text>
      </view>
    </scroll-view>

    <!-- 部门选择弹窗（复用登录页逻辑） -->
    <view v-if="showDeptPicker" class="dept-modal-mask" @tap="closeDeptPicker">
      <view class="dept-modal" @tap.stop>
        <view class="dept-modal-head">
          <text class="dept-modal-title">选择门店</text>
          <text class="dept-modal-sub">请选择要切换的部门</text>
        </view>
        <scroll-view scroll-y class="dept-list">
          <view
            v-for="(dept, idx) in allDepts"
            :key="dept.deptId || dept.id || idx"
            class="dept-list-item"
            :class="{ active: String(dept.deptId || dept.id) === String(pendingDeptId) }"
            @tap="pickDept(dept)"
          >
            <view class="dept-item-mark"><text class="dept-item-mark-text">店</text></view>
            <view class="dept-item-body">
              <text class="dept-item-name">{{ dept.deptName || dept.name }}</text>
              <text v-if="dept.leader" class="dept-item-meta">{{ dept.leader }}</text>
            </view>
            <view v-if="String(dept.deptId || dept.id) === String(pendingDeptId)" class="dept-item-check">✓</view>
          </view>
        </scroll-view>
        <view class="dept-modal-foot">
          <button class="dept-btn-cancel" @tap="closeDeptPicker">取消</button>
          <button class="dept-btn-confirm" :disabled="!pendingDeptId" @tap="confirmDeptSwitch">确认切换</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import miniProgramShare from '@/mixins/miniProgramShare.js'
import { groups, modules } from '@/config/modules.js'
import { request, getToken, getBaseUrl } from '@/api/index.js'
import { filterAuthorizedGroups, hasModulePermission, hasExactPermission, isAdmin } from '@/utils/permission.js'
import { applySeckillStats } from '@/utils/seckillStats.js'
import { SERVICE_STATUS_TARGETS, buildSystemHealthItems, normalizeDeptOptions, resolveCurrentDept, normalizeServerStatus, isSystemAdminUser } from '@/utils/homeControl.js'
import { getStatusBarHeight } from '@/utils/systemInfo.js'
import { resolveDeptCollection, workContext } from '@/utils/workContext.js'
import { refreshForegroundSession } from '@/utils/foregroundSession.js'
import { deriveWorkView } from '@/utils/workView.js'
import { recordRecent } from '@/utils/workbenchPersonalization.js'

const MODULE_BG = {
  member: 'rgba(8, 124, 240,0.08)', pointsGoods: 'rgba(59,130,246,0.08)', pointsRecord: 'rgba(8, 124, 240,0.08)',
  pointsExchange: 'rgba(139,92,246,0.08)', seckill: 'rgba(249,115,22,0.08)', seckillRecord: 'rgba(234,88,12,0.08)',
  expense: 'rgba(239,68,68,0.08)', advance: 'rgba(139,92,246,0.08)', product: 'rgba(59,130,246,0.08)',
  supplier: 'rgba(107,114,128,0.08)', purchase: 'rgba(249,115,22,0.08)', sale: 'rgba(16,185,129,0.08)',
  investorPayment: 'rgba(236,72,153,0.08)', investor: 'rgba(14,165,233,0.08)', investRecord: 'rgba(16,185,129,0.08)',
  deptProfitConfig: 'rgba(107,114,128,0.08)', accountingPeriod: 'rgba(245,158,11,0.08)', profitShare: 'rgba(244,63,94,0.08)',
  costAccounting: 'rgba(6,182,212,0.08)', userManage: 'rgba(99,102,241,0.08)',
  stockCost: 'rgba(14,165,233,0.08)', stockAdjustment: 'rgba(245,158,11,0.08)', stockLedger: 'rgba(6,182,212,0.08)', stocktake: 'rgba(99,102,241,0.08)',
  memberPurchase: 'rgba(8,124,240,0.08)', memberPurchaseReturn: 'rgba(239,68,68,0.08)',
  configSync: 'rgba(107,114,128,0.08)', campaignPolicy: 'rgba(249,115,22,0.08)', memberLevel: 'rgba(139,92,246,0.08)'
}

const MODULE_LETTER = {
  member: '👤', pointsGoods: '🎁', pointsRecord: '📝', pointsExchange: '🔄',
  seckill: '⚡', seckillRecord: '🏃', expense: '💰', advance: '💵',
  product: '📦', supplier: '🏪', purchase: '🛒', sale: '📈',
  investorPayment: '💸', investor: '🤝', investRecord: '💎',
  deptProfitConfig: '⚙️', accountingPeriod: '📅', profitShare: '📊',
  costAccounting: '🧮', userManage: '👥',
  stockCost: '📋', stockAdjustment: '⚖️', stockLedger: '📒', stocktake: '🔢',
  memberPurchase: '🛍️', memberPurchaseReturn: '↩️',
  configSync: '🔄', campaignPolicy: '🎯', memberLevel: '🎖️'
}

const MODULE_ICON_COLOR = {
  member: '#087CF0', pointsGoods: '#3B82F6', pointsRecord: '#087CF0',
  pointsExchange: '#8B5CF6', seckill: '#F97316', seckillRecord: '#EA580C',
  expense: '#EF4444', advance: '#8B5CF6', product: '#3B82F6',
  supplier: '#6B7280', purchase: '#F97316', sale: '#10B981',
  investorPayment: '#EC4899', investor: '#0EA5E9', investRecord: '#10B981',
  deptProfitConfig: '#6B7280', accountingPeriod: '#F59E0B', profitShare: '#F43F5E',
  costAccounting: '#06B6D4', userManage: '#6366F1',
  stockCost: '#0EA5E9', stockAdjustment: '#F59E0B', stockLedger: '#06B6D4', stocktake: '#6366F1',
  memberPurchase: '#087CF0', memberPurchaseReturn: '#EF4444',
  configSync: '#6B7280', campaignPolicy: '#F97316', memberLevel: '#8B5CF6'
}

export default {
  mixins: [miniProgramShare],
  data() {
    return {
      stats: {},
      overview: null,
      period: null,
      periodLoading: false,
      expenseSummary: null,
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
      barColors: ['#087CF0', '#3B82F6', '#8B5CF6', '#10B981', '#F59E0B', '#EC4899'],
      showDeptPicker: false,
      allDepts: [],
      pendingDeptId: null,
      operatingTaskCount: 0,
      lowFrequencyAt: { seckill: 0, expense: 0 }
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
    workView() {
      const context = workContext.snapshot()
      const authorizedModules = this.modules.filter(key => hasModulePermission(key, this.modules))
      return deriveWorkView({
        depts: context.depts,
        modules: authorizedModules
      })
    },
    adminUser() {
      return isAdmin(this.modules) || isSystemAdminUser(this.userInfo, this.systemPermissions)
    },
    canViewOperatingTask() {
      // 后端权限是权威：system:operatingTask:list
      // 角色优先：店长(workView.management)、财务、管理员可见
      return hasExactPermission('system:operatingTask:list')
    },
    canViewServerStatus() {
      return isSystemAdminUser(this.userInfo, this.systemPermissions)
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
    isInvestorView() {
      return hasExactPermission('finance:investor:list') || hasModulePermission('investor', this.modules)
    },
    isSupervisorView() {
      return this.canSwitchDept && !this.isInvestorView
    },
    periodAudienceLabel() {
      if (this.isInvestorView) return '投资范围周期'
      if (this.isSupervisorView) return '授权门店周期'
      return '店面'
    },
    periodStale() {
      return Boolean(this.period?.stale)
    },
    canSwitchDept() {
      // 基于登录页获取的部门列表实时判断（openDeptPicker 会重新请求）
      const depts = this.allDepts.length > 0 ? this.allDepts : (this.userInfo?.depts || this.deptList || [])
      return Array.isArray(depts) && depts.length > 1
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
    headerContentStyle() {
      const top = this.menuButton?.bottom ? this.menuButton.bottom + 8 : this.statusBarH + 48
      return {
        paddingTop: top + 'px'
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
      const advance = Number(p.totalUnverifiedAdvance) || 0
      const totalCost = expense + purchase + advance
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
    periodTotalCost() {
      if (!this.period) return 0
      return (Number(this.period.totalVerifiedExpense) || 0)
        + (Number(this.period.totalPurchase) || 0)
        + (Number(this.period.totalUnverifiedAdvance) || 0)
    },
    periodBreakEvenGap() {
      return Math.max(this.periodTotalCost - (Number(this.period?.totalSalePayment) || 0), 0)
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
    },
    // ===== 新版聚合看板计算属性（R1-R25 同步） =====
    // 会员增长分组：优先使用 overview，回退 stats
    overviewMember() {
      if (this.overview && this.overview.member) {
        return this.overview.member
      }
      // 回退到旧版 stats
      return {
        totalMembers: this.stats?.totalMembers || 0,
        todayMembers: this.stats?.todayMembers || 0,
        activeMembers: this.stats?.activeMembers || 0,
        silentMembers: 0
      }
    },
    // 成长体系分组
    overviewGrowth() {
      return this.overview?.growth || null
    },
    // 增长动作分组
    overviewGrowthActions() {
      const g = this.overviewGrowth
      if (!g) return null
      return {
        pending: g.pendingGrowthActions || 0,
        completed: g.completedGrowthActions || 0,
        effectRate: g.growthActionEffectRate || 0
      }
    },
    // 积分运营分组
    overviewPoints() {
      return this.overview?.points || null
    },
    // 等级分布
    overviewLevelDistribution() {
      const dist = this.overview?.level?.distribution
      if (!Array.isArray(dist) || !dist.length) return []
      const maxCount = Math.max(...dist.map(d => Number(d.count) || 0), 1)
      return dist.map(d => ({
        name: d.levelName || '未分级',
        count: Number(d.count) || 0,
        percent: Math.round(((Number(d.count) || 0) / maxCount) * 100)
      }))
    },
    // 分层洞察分布
    overviewSegmentDistribution() {
      const dist = this.overview?.segment?.distribution
      if (!Array.isArray(dist) || !dist.length) return []
      return dist.map(d => ({
        name: d.segmentName || '未知',
        count: Number(d.count) || 0
      }))
    },
    // 活动表现分组：优先使用 overview，回退 seckillList
    overviewActivity() {
      if (this.overview && this.overview.activity) {
        return this.overview.activity
      }
      return {
        activeSeckillCount: this.seckillList?.length || 0,
        todayActivityMembers: 0,
        todayActivityAmount: 0
      }
    },
    // 今日经营分组：优先使用 overview，回退 stats
    overviewFinance() {
      if (this.overview && this.overview.finance) {
        return this.overview.finance
      }
      return {
        todaySale: this.stats?.todaySale || 0,
        todayExpense: this.stats?.todayExpense || 0,
        unverifiedExpense: this.stats?.unverifiedExpense || 0,
        unverifiedAdvance: this.stats?.unverifiedAdvance || 0
      }
    },
    // 是否展示会员增长分组（需要 member 模块权限）
    showMemberSection() {
      return hasModulePermission('member', this.modules)
    },
    // 是否展示成长体系分组（需要 member 模块权限 + overview 数据）
    showGrowthSection() {
      return this.showMemberSection && this.overviewGrowth
    },
    // 是否展示增长动作分组（需要 member 模块权限 + overview 数据）
    showGrowthActionsSection() {
      return this.showMemberSection && this.overviewGrowthActions
    },
    // 是否展示积分运营分组（需要 pointsRecord 或 pointsExchange 权限）
    showPointsSection() {
      return (hasModulePermission('pointsRecord', this.modules) || hasModulePermission('pointsExchange', this.modules)) && this.overviewPoints
    },
    // 是否展示分层洞察分组（需要 member 模块权限 + 分层数据）
    showSegmentSection() {
      return this.showMemberSection && this.overviewSegmentDistribution.length > 0
    },
    // 是否展示等级分布（需要 member 模块权限 + 等级数据）
    showLevelDistribution() {
      return this.showMemberSection && this.overviewLevelDistribution.length > 0
    }
  },
  async onShow() {
    if (!getToken()) {
      uni.reLaunch({ url: '/pages/login/index' })
      return
    }
    try {
      await refreshForegroundSession()
    } catch (_) {
      if (!getToken()) return
    }
    this.statusBarH = getStatusBarHeight()
    try {
      this.menuButton = uni.getMenuButtonBoundingClientRect()
    } catch (e) {
      this.menuButton = null
    }
    const context = workContext.snapshot()
    if (context.user || context.depts.length) {
      this.applyWorkContext(context)
    } else {
      const userInfo = uni.getStorageSync('userInfo') || {}
      this.userInfo = userInfo
      this.nickName = userInfo.nickName || userInfo.username || ''
      this.currentDeptId = userInfo.currentDeptId || userInfo.deptId || null
      this.deptList = normalizeDeptOptions(userInfo.depts || [])
    }
    this.modules = uni.getStorageSync('modules') || []
    const safe = (p) => p.catch(e => this.logRequestFailure('onShow request failed', e))
    safe(this.loadUserContext())
    safe(this.loadDashboard())
    setTimeout(() => {
      safe(this.loadOverview())
      safe(this.loadPeriod())
    }, 200)
    setTimeout(() => {
      safe(this.refreshModules())
      safe(this.loadSeckill())
      safe(this.loadExpenseSummary())
    }, 600)
  },
  methods: {
    applyWorkContext(context) {
      const user = context.user || {}
      this.userInfo = {
        ...this.userInfo,
        ...user,
        depts: context.depts,
        deptId: context.currentDeptId,
        currentDeptId: context.currentDeptId,
        deptName: context.currentDept?.name || user.deptName || this.userInfo.deptName || ''
      }
      this.nickName = user.nickName || user.userName || user.username || this.nickName
      this.currentDeptId = context.currentDeptId
      this.deptList = normalizeDeptOptions(context.depts)
      uni.setStorageSync('userInfo', this.userInfo)
    },
    logRequestFailure(label, error) {
      const message = error?.msg || error?.errMsg || error?.message || String(error || '')
      console.warn(label + ': ' + message)
    },
    go(url) {
      uni.navigateTo({ url })
    },
    openOperatingTask() {
      uni.navigateTo({ url: '/pages/operating-task/index' })
    },
    async loadOperatingTaskCount() {
      if (!this.canViewOperatingTask) {
        this.operatingTaskCount = 0
        return
      }
      try {
        const res = await request({
          url: '/operatingTask/pendingCount',
          method: 'GET',
          silent: true
        })
        this.operatingTaskCount = Number(res?.data ?? res ?? 0)
      } catch {
        this.operatingTaskCount = 0
      }
    },
    openModule(key) {
      if (!hasModulePermission(key, this.modules)) {
        uni.showToast({ title: '暂无该功能权限', icon: 'none' })
        return
      }
      const recent = uni.getStorageSync('miniProgramRecent') || []
      uni.setStorageSync('miniProgramRecent', recordRecent(recent, key, this.modules))
      if (key === 'userManage') {
        uni.navigateTo({ url: '/pages/user/index' })
      } else {
        uni.navigateTo({ url: '/pages/list/index?module=' + key })
      }
    },
    // 跳转会员运营看板
    goMemberDashboard() {
      if (!hasModulePermission('member', this.modules)) {
        uni.showToast({ title: '暂无该功能权限', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/member/dashboard' })
    },
    // 跳转会员成长页
    goMemberGrowth() {
      if (!hasModulePermission('member', this.modules)) {
        uni.showToast({ title: '暂无该功能权限', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/member/growth' })
    },
    // 跳转增长动作任务列表
    goMemberActions() {
      if (!hasModulePermission('member', this.modules)) {
        uni.showToast({ title: '暂无该功能权限', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/member/actions' })
    },
    // 跳转积分流水页
    goMemberPoints() {
      if (!hasModulePermission('pointsRecord', this.modules) && !hasModulePermission('pointsExchange', this.modules)) {
        uni.showToast({ title: '暂无该功能权限', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/member/points' })
    },
    // 分层名称对应的色调
    segmentTone(name) {
      if (!name) return ''
      if (name.indexOf('高价值') >= 0) return 'primary'
      if (name.indexOf('活跃') >= 0) return 'success'
      if (name.indexOf('沉默') >= 0 || name.indexOf('待唤醒') >= 0) return 'warning'
      return ''
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
    fmtMoney(val) {
      if (!val && val !== 0) return '0'
      const n = Number(val)
      if (isNaN(n)) return '0'
      if (n >= 10000) return (n / 10000).toFixed(1) + '万'
      return n.toFixed(n % 1 === 0 ? 0 : 2)
    },
    fmtMoneyWithSign(val) {
      return '¥' + this.fmtMoney(val)
    },
    getBalanceClass(val) {
      return Number(val) < 0 ? 'danger' : 'success'
    },
    seckillProgress(item) {
      return Number(item.claimProgress) || 0
    },
    shouldRefreshLowFrequency(key, force = false) {
      const now = Date.now()
      if (!force && now - (this.lowFrequencyAt[key] || 0) < 60000) return false
      this.lowFrequencyAt[key] = now
      return true
    },
    async loadDashboard() {
      this.loading = true
      try {
        const params = {}
        if (this.currentDeptId !== null && this.currentDeptId !== undefined) {
          params.deptId = this.currentDeptId
        }
        const res = await request({
          url: '/member/mp/dashboard/stats',
          method: 'GET',
          data: params,
          silent: true,
          timeout: 12000,
          withContextMeta: true
        })
        if (res.contextMeta?.staleContext) return
        const data = res.data || res
        this.stats = data && typeof data === 'object' ? data : {}
      } catch (e) {
        this.logRequestFailure('dashboard load failed', e)
        this.stats = {
          todayMembers: 0, totalMembers: 0, activeMembers: 0,
          todaySale: 0, todayExpense: 0, totalSale: 0, totalExpense: 0,
          totalPurchase: 0, pointsExchangeCount: 0
        }
      } finally {
        this.loading = false
      }
    },
    /**
     * 加载新版聚合看板接口（R1-R25 同步）。
     * 单模块失败时后端返回空数据，小程序不重复弹 toast；
     * 整体失败时保留上一次成功数据，仅提示"数据更新失败"。
     */
    async loadOverview() {
      try {
        const params = {}
        if (this.currentDeptId !== null && this.currentDeptId !== undefined) {
          params.deptId = this.currentDeptId
        }
        const res = await request({
          url: '/member/mp/dashboard/overview',
          method: 'GET',
          data: params,
          silent: true,
          timeout: 12000,
          withContextMeta: true
        })
        if (res.contextMeta?.staleContext) return
        const data = res.data || res
        if (data && typeof data === 'object') {
          this.overview = data
        }
      } catch (e) {
        this.logRequestFailure('overview load failed', e)
      }
    },
    async loadPeriod() {
      this.periodLoading = true
      try {
        const params = {}
        if (this.currentDeptId !== null && this.currentDeptId !== undefined) {
          params.deptId = this.currentDeptId
        }
        const res = await request({
          url: '/finance/accountingPeriod/current',
          method: 'GET',
          data: params,
          silent: true,
          timeout: 12000,
          withContextMeta: true
        })
        if (res.contextMeta?.staleContext) return
        this.period = res.data || res || null
        if (this.period && this.currentDeptId) {
          uni.setStorageSync(`periodSummary:${this.currentDeptId}`, this.period)
        }
      } catch (e) {
        this.logRequestFailure('period load failed', e)
        const cached = this.currentDeptId
          ? uni.getStorageSync(`periodSummary:${this.currentDeptId}`)
          : null
        this.period = cached && typeof cached === 'object'
          ? { ...cached, stale: true }
          : null
      } finally {
        this.periodLoading = false
      }
    },
    async loadExpenseSummary() {
      if (!this.shouldRefreshLowFrequency('expense', arguments[0] || false)) return
      try {
        const params = {}
        if (this.currentDeptId !== null && this.currentDeptId !== undefined) {
          params.deptId = this.currentDeptId
        }
        const res = await request({
          url: '/finance/expense/summary',
          method: 'GET',
          data: params,
          silent: true,
          timeout: 12000,
          withContextMeta: true
        })
        if (res.contextMeta?.staleContext) return
        this.expenseSummary = res.data || res || null
      } catch (e) {
        this.logRequestFailure('expense summary load failed', e)
        this.expenseSummary = null
      }
    },
    async loadSeckill() {
      if (!this.shouldRefreshLowFrequency('seckill', arguments[0] || false)) return
      try {
        const params = { status: '0' }
        if (this.currentDeptId !== null && this.currentDeptId !== undefined) {
          params.deptId = this.currentDeptId
        }
        const res = await request({
          url: '/member/seckill/list',
          method: 'GET',
          data: params,
          silent: true,
          timeout: 12000,
          withContextMeta: true
        })
        if (res.contextMeta?.staleContext) return
        const list = res.data || res || []
        const activities = Array.isArray(list) ? list : (list.rows || [])
        if (!activities.length) {
          this.seckillList = []
          return
        }
        const statsRes = await request({
          url: '/member/seckillRecord/statistics/batch',
          silent: true,
          timeout: 12000,
          method: 'GET',
          data: { seckillIds: activities.map((item) => item.seckillId).join(','), deptId: this.currentDeptId },
          withContextMeta: true
        })
        if (statsRes.contextMeta?.staleContext) return null
        const results = [res, statsRes]
        if (results.some(item => item === null)) return
        const statsRows = statsRes.data || statsRes || []
        const statsMap = (Array.isArray(statsRows) ? statsRows : []).reduce((map, row) => {
          map[String(row.seckillId)] = row
          return map
        }, {})
        this.seckillList = activities.map((item) => applySeckillStats(item, statsMap[String(item.seckillId)] || {}))
      } catch (e) {
        this.logRequestFailure('seckill load failed', e)
        this.seckillList = []
      }
    },
    async loadServerStatus() {
      if (!this.canViewServerStatus) return
      this.serverStatusLoading = true
      const token = getToken()
      const baseUrl = getBaseUrl()
      const timeoutMs = 5000
      const services = await Promise.all(SERVICE_STATUS_TARGETS.map(async (target) => {
        try {
          const res = await Promise.race([
            new Promise((resolve, reject) => {
              try {
                uni.request({
                  url: baseUrl + target.url,
                  method: 'GET',
                  header: { Authorization: 'Bearer ' + token },
                  timeout: timeoutMs,
                  success: (r) => resolve(r),
                  fail: (e) => reject(e)
                })
              } catch (syncErr) {
                reject(syncErr)
              }
            }),
            new Promise((_, reject) => {
              setTimeout(() => reject(new Error('health check guard timeout')), timeoutMs + 500)
            })
          ])
          const statusCode = res.statusCode
          const data = res.data || {}
          if (statusCode === 403) {
            return { ...target, status: 'UP', ok: true }
          }
          return { ...target, status: data.status || data.code || 'OK', ok: data.status === 'UP' || data.code === 200 || data.status === undefined }
        } catch (e) {
          return { ...target, status: 'DOWN', ok: false }
        }
      }))
      this.serverStatus = normalizeServerStatus(services)
      this.serverStatusLoading = false
    },
    clearModuleAccess() {
      this.modules = []
      uni.setStorageSync('modules', [])
    },
    async refreshModules() {
      try {
        const res = await request({
          url: '/member/mp/modules',
          method: 'GET',
          silent: true,
          timeout: 12000,
          withContextMeta: true
        })
        if (res.contextMeta?.staleContext) return
        const modules = res.data || res || []
        const moduleList = Array.isArray(modules) ? modules : []
        this.modules = moduleList.length || !this.adminUser ? moduleList : ['member', 'seckillRecord', 'expense', 'userManage']
        uni.setStorageSync('modules', this.modules)
      } catch (e) {
        this.logRequestFailure('modules refresh failed', e)
      }
    },
    async loadUserContext() {
      try {
        const res = await request({
          url: '/system/user/getInfo',
          method: 'GET',
          noRedirect: true,
          silent: true,
          timeout: 12000,
          withContextMeta: true
        })
        if (res.contextMeta?.staleContext) return
        const user = res.user || {}
        const depts = resolveDeptCollection(res, this.userInfo)
        this.systemPermissions = res.permissions || []
        workContext.hydrate({
          user,
          depts,
          currentDeptId: res.currentDeptId ?? user.currentDeptId ?? user.deptId ?? this.currentDeptId
        })
        this.applyWorkContext(workContext.snapshot())
        if (this.adminUser && (!this.modules || this.modules.length === 0)) {
          this.modules = ['member', 'seckillRecord', 'expense', 'userManage']
          uni.setStorageSync('modules', this.modules)
        }
        if (this.adminUser) {
          await this.loadAllDepts()
        }
        const current = resolveCurrentDept(this.deptList, this.currentDeptId)
        if (current) this.currentDeptId = current.id
      } catch (e) {
        const cached = this.userInfo || {}
        this.deptList = normalizeDeptOptions(cached.depts || [])
      }
    },
    async loadAllDepts() {
      try {
        const res = await request({
          url: '/system/user/deptTree',
          method: 'GET',
          noRedirect: true,
          silent: true,
          timeout: 12000
        })
        const list = normalizeDeptOptions(res.data || res.depts || [])
        if (list.length) this.deptList = list
      } catch (e) {
        this.logRequestFailure('dept tree load failed', e)
      }
    },
    async onDeptChange(e) {
      const index = Number(e.detail.value)
      const target = this.deptList[index]
      if (!target || String(target.id) === String(this.currentDeptId) || this.switchingDept) return
      this.switchingDept = true
      uni.showLoading({ title: '切换中' })
      try {
        await request({
          url: `/system/user/switchDept/${target.id}`,
          method: 'POST',
          silent: true,
          timeout: 12000
        })
        workContext.selectDept(target.id)
        this.applyWorkContext(workContext.snapshot())
        this.clearModuleAccess()
        await this.refreshModules()
        this.lowFrequencyAt = { seckill: 0, expense: 0 }
        await Promise.all([this.loadDashboard(), this.loadOverview(), this.loadPeriod(), this.loadSeckill(), this.loadExpenseSummary(), this.loadOperatingTaskCount()])
        uni.showToast({ title: '已切换部门', icon: 'success' })
      } catch (err) {
        uni.showToast({ title: err?.msg || err?.message || '部门切换失败', icon: 'none' })
      } finally {
        this.switchingDept = false
        uni.hideLoading()
      }
    },
    async openDeptPicker() {
      // 已知只有1个部门或更少，直接返回不弹任何提示
      const knownDepts = this.allDepts.length > 0 ? this.allDepts : (this.userInfo?.depts || this.deptList || [])
      if (knownDepts.length <= 1) {
        // 再确认一次：如果缓存明确只有1个部门，不再请求
        if (knownDepts.length === 1) return
        // 缓存为空时才请求确认
      }
      try {
        uni.showLoading({ title: '加载部门...' })
        const res = await request({
          url: '/system/user/getInfo',
          method: 'GET',
          noRedirect: true,
          silent: true,
          timeout: 12000
        })
        const deptList = (res.depts || res.data?.depts || res.user?.depts || [])
        uni.hideLoading()
        if (deptList.length <= 1) {
          // 更新缓存，下次点击直接返回
          this.allDepts = deptList
          return
        }
        this.allDepts = deptList
        this.pendingDeptId = this.currentDeptId
        this.showDeptPicker = true
      } catch (e) {
        uni.hideLoading()
        this.logRequestFailure('获取部门列表失败', e)
        uni.showToast({ title: '加载部门失败，请重试', icon: 'none' })
      }
    },
    pickDept(dept) {
      this.pendingDeptId = dept.deptId || dept.id
    },
    closeDeptPicker() {
      this.showDeptPicker = false
      this.allDepts = []
      this.pendingDeptId = null
    },
    async confirmDeptSwitch() {
      if (!this.pendingDeptId || String(this.pendingDeptId) === String(this.currentDeptId)) {
        this.closeDeptPicker()
        return
      }
      if (this.switchingDept) return
      this.switchingDept = true
      uni.showLoading({ title: '切换中' })
      try {
        await request({
          url: `/system/user/switchDept/${this.pendingDeptId}`,
          method: 'POST',
          silent: true,
          timeout: 12000
        })
        workContext.selectDept(this.pendingDeptId)
        this.applyWorkContext(workContext.snapshot())
        this.closeDeptPicker()
        this.clearModuleAccess()
        await this.refreshModules()
        this.lowFrequencyAt = { seckill: 0, expense: 0 }
        await Promise.all([this.loadDashboard(), this.loadOverview(), this.loadPeriod(), this.loadSeckill(), this.loadExpenseSummary(), this.loadOperatingTaskCount()])
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
      const safe = (p) => p.catch(e => this.logRequestFailure('refresh request failed', e))
      Promise.all([
        safe(this.loadUserContext()),
        safe(this.loadDashboard())
      ]).then(() => {
        return Promise.all([
          safe(this.loadOverview()),
          safe(this.loadPeriod())
        ])
      }).then(() => {
        return Promise.all([
          safe(this.refreshModules()),
          safe(this.loadSeckill(true)),
          safe(this.loadExpenseSummary(true))
        ])
      }).finally(() => {
        this.refreshing = false
      })
    }
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  max-width: 750rpx;
  margin: 0 auto;
  background: linear-gradient(180deg, #E6EEF6 0%, #F3F6FA 42%, #E8EEF5 100%);
  box-sizing: border-box;
  overflow: hidden;
}

.stale-banner {
  margin: 16rpx 24rpx 0;
  padding: 14rpx 18rpx;
  border: 1rpx solid #f5d08a;
  border-radius: 12rpx;
  background: #fff8e6;
  color: #946200;
  font-size: 22rpx;
}

.period-empty {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin: 18rpx 24rpx;
  padding: 28rpx;
}

/* ===== 头部（参照我的页面：relative，非fixed） ===== */
.header {
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(180deg, #C7DCF2 0%, #E1ECF8 100%);
  border-bottom: 2rpx solid #AFCBE7;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,.58) 0%, rgba(202,224,246,.9) 100%);
  border-bottom: 1rpx solid rgba(8,124,240,.08);
}

.header-bg::before {
  content: '';
  position: absolute;
  width: 300rpx;
  height: 300rpx;
  right: -160rpx;
  top: -170rpx;
  border: 22rpx solid rgba(8,124,240,.05);
  border-radius: 50%;
}

.header-bg::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2rpx;
  background: #A8C7E5;
  border-radius: 0;
}

.header-content {
  position: relative;
  z-index: 2;
  padding: 0 30rpx 42rpx;
  box-sizing: border-box;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.header-left {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.header-title {
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.2;
  color: #1F2D3D;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-sub {
  font-size: 24rpx;
  line-height: 1.4;
  color: #8190A1;
  margin-top: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-shrink: 0;
}

.dept-switch-inline,
.dept-static-inline {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 24rpx;
  min-height: 60rpx;
  border-radius: 16rpx;
  background: #F1F6FF;
  border: 1rpx solid #CFE0F8;
  box-shadow: none;
}

.dept-switch-inline {
  cursor: pointer;
}

.dept-switch-inline:active {
  transform: scale(0.96);
  background: rgba(255, 255, 255, 0.32);
}

.dept-no-switch {
  background: transparent;
  border: none;
  padding: 16rpx 0;
}

.dept-name-inline {
  font-size: 26rpx;
  font-weight: 600;
  color: #087CF0;
  margin-right: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160rpx;
}

.dept-arrow-inline {
  font-size: 20rpx;
  color: #6C8DB4;
  margin-left: 2rpx;
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
  color: #087CF0;
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

/* ===== KPI卡片行（浮在波浪上） ===== */
.kpi-row-wrap {
  padding: 0 28rpx;
  margin-top: -16rpx;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.kpi-row {
  display: flex;
  gap: 16rpx;
  width: 100%;
  box-sizing: border-box;
}

/* ===== 滚动区 ===== */
.scroll {
  flex: 1;
  width: 100%;
  min-height: 0;
  padding: 18rpx 28rpx 40rpx;
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

.kpi-card {
  flex: 1;
  min-width: 0;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1rpx solid #D5E0EC;
  box-shadow: 0 5rpx 18rpx rgba(45, 72, 98, 0.08);
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

.kpi-value.primary { color: #087CF0; }
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
  border: 1rpx solid #D5E0EC;
  box-shadow: 0 5rpx 18rpx rgba(45, 72, 98, 0.07);
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
  color: #087CF0;
  background: rgba(8, 124, 240, 0.1);
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

.section-link {
  font-size: 22rpx;
  color: #94A3B8;
  font-weight: 500;
  margin-left: auto;
  flex-shrink: 0;
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
  border-top-color: #087CF0;
  border-right-color: #087CF0;
  transform: rotate(calc(var(--progress) * 3.6deg - 90deg));
  transition: transform 0.8s ease-out;
}

/* 用 conic-gradient 实现精确进度 */
.ring-outer {
  background: conic-gradient(
    #087CF0 calc(var(--progress) * 3.6deg),
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
  color: #087CF0;
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
  color: #087CF0;
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
  background: #E8EEF5;
  border-radius: 8rpx;
  overflow: hidden;
}

.mini-bar-fill {
  height: 100%;
  border-radius: 8rpx;
  transition: width 0.8s ease-out;
}

.mini-bar-fill.sale {
  background: linear-gradient(90deg, #087CF0, #A8C7E5);
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

.ms-value.primary { color: #087CF0; }
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
  background: #E8EEF5;
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
  color: #087CF0;
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
  background: #E8EEF5;
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
  box-shadow: 0 12rpx 32rpx rgba(8, 124, 240, 0.08);
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
  background: linear-gradient(135deg, rgba(8, 124, 240, 0.06), rgba(248, 250, 252, 0.96));
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

/* ===== 核算周期综合数据 ===== */
.finance-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.finance-item {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  padding: 16rpx;
  border-radius: 16rpx;
  background: #F8FAFC;
}

.finance-label {
  font-size: 22rpx;
  color: #94A3B8;
}

.finance-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #1A2332;
}

.finance-value.warn {
  color: #D97706;
}

.finance-value.success {
  color: #059669;
}

.finance-value.danger {
  color: #EF4444;
}

.work-view-label {
  flex-shrink: 0;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
  background: #DCEBFA;
  color: #35658F;
  font-size: 20rpx;
  line-height: 1.3;
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

/* ===== 部门选择弹窗 ===== */
.dept-modal-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.5);
}

.dept-modal {
  width: 100%;
  border-radius: 32rpx 32rpx 0 0;
  background: #FFFFFF;
  padding: 0 0 env(safe-area-inset-bottom);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.dept-modal-head {
  padding: 32rpx 36rpx 20rpx;
  text-align: center;
}

.dept-modal-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1A2332;
  display: block;
}

.dept-modal-sub {
  font-size: 24rpx;
  color: #94A3B8;
  margin-top: 8rpx;
  display: block;
}

.dept-list {
  max-height: 50vh;
  padding: 0 36rpx 0 20rpx;
  box-sizing: border-box;
}

.dept-list-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 40rpx 24rpx 20rpx;
  border-radius: 16rpx;
  margin-bottom: 8rpx;
  border: 2rpx solid transparent;
  transition: all 0.15s;
  box-sizing: border-box;
  overflow: visible;
}

.dept-list-item.active {
  background: #EAF4F8;
  border: 2rpx solid #087CF0;
}

.dept-item-mark {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dept-item-mark-text {
  font-size: 26rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.dept-item-body {
  flex: 1;
  min-width: 0;
}

.dept-item-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A2332;
  display: block;
}

.dept-item-meta {
  font-size: 22rpx;
  color: #94A3B8;
  margin-top: 4rpx;
  display: block;
}

.dept-item-check {
  font-size: 32rpx;
  color: #087CF0;
  font-weight: 700;
  flex-shrink: 0;
  width: 44rpx;
  margin-right: 4rpx;
  text-align: center;
  line-height: 1;
}

.dept-modal-foot {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 28rpx 28rpx;
  border-top: 1rpx solid #F1F5F9;
}

.dept-btn-cancel,
.dept-btn-confirm {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  margin: 0;
  padding: 0;
}

.dept-btn-cancel {
  background: #F1F5F9;
  color: #475569;
}

.dept-btn-confirm {
  background: linear-gradient(135deg, #087CF0, #5AA9E8);
  color: #FFFFFF;
}

.dept-btn-cancel::after,
.dept-btn-confirm::after {
  border: none;
}
</style>
