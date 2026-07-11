<template>
  <div class="store-dashboard">
    <section class="workbench-header">
      <div class="header-copy">
        <span class="eyebrow">STORE REVIEW WORKBENCH</span>
        <h2>门店经营复盘工作台</h2>
        <p>{{ scopeText }} · {{ todayLabel }}</p>
      </div>
      <div class="header-actions">
        <el-select
          v-if="deptOptions.length > 1"
          v-model="selectedDeptId"
          class="dept-select"
          placeholder="选择门店"
          filterable
          @change="handleDeptChange"
        >
          <el-option
            v-for="dept in deptOptions"
            :key="dept.deptId"
            :label="dept.deptName"
            :value="dept.deptId"
          />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </section>

    <section class="period-finance-grid" v-loading="loading">
      <article class="period-hero">
        <div>
          <span class="panel-kicker">ACCOUNTING OVERVIEW</span>
          <h3>核算总览</h3>
          <strong :class="amountClass(currentPeriod.netProfit)">
            {{ money(currentPeriod.netProfit) }}
          </strong>
          <p>
            {{ periodStatusText }}
            <span v-if="currentPeriod.periodNo || currentPeriod.periodName">
              · {{ currentPeriod.periodNo || currentPeriod.periodName }}
            </span>
          </p>
        </div>
        <div class="break-even-ring" :class="scoreClass(periodReturnRate)">
          <span>{{ periodReturnRate }}%</span>
          <small>盈亏平衡</small>
        </div>
      </article>

      <article class="period-kpi-strip">
        <div v-for="item in periodFinanceCards" :key="item.key" class="period-kpi-item">
          <span>{{ item.label }}</span>
          <strong :class="item.className">{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </article>
    </section>

    <section class="top-grid" v-loading="loading">
      <article class="status-panel">
        <div class="status-main">
          <span class="panel-kicker">今日状态</span>
          <strong>{{ mainStatusTitle }}</strong>
          <p>{{ mainStatusDesc }}</p>
        </div>
        <div class="status-score" :class="scoreClass(portfolioHealthScore)">
          <span>{{ portfolioHealthScore }}</span>
          <small>健康分</small>
        </div>
      </article>

      <article class="metric-strip">
        <div class="metric-item">
          <span>今日销售</span>
          <strong>{{ money(dailyReview.salesAmount) }}</strong>
        </div>
        <div class="metric-item">
          <span>今日费用</span>
          <strong>{{ money(dailyReview.expenseAmount) }}</strong>
        </div>
        <div class="metric-item">
          <span>净现金流</span>
          <strong :class="amountClass(dailyReview.netCashflowAmount)">
            {{ money(dailyReview.netCashflowAmount) }}
          </strong>
        </div>
        <div class="metric-item">
          <span>待处理复盘</span>
          <strong>{{ pendingTaskCount }}</strong>
        </div>
      </article>
    </section>

    <section class="action-grid">
      <article class="panel priority-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">NEXT ACTION</span>
            <h3>今日待办与优先处理</h3>
          </div>
          <el-button text type="primary" @click="goReviewTask">查看复盘任务</el-button>
        </div>
        <div v-if="priorityItems.length" class="priority-list">
          <div v-for="item in priorityItems" :key="item.key" class="priority-row" :class="item.level">
            <div class="priority-marker">{{ item.badge }}</div>
            <div class="priority-content">
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
            <span class="priority-tag">{{ item.label }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无待办，今日经营状态平稳" :image-size="76" />
      </article>

      <!-- R13-F: 应收待跟进 -->
      <div class="card" v-if="receivableFollowUp.length > 0" style="margin-bottom: 20px;">
        <div class="card-header">
          <span style="font-weight: 600; font-size: 15px;">应收待跟进</span>
          <el-tag type="warning" size="small">{{ receivableFollowUp.length }} 笔</el-tag>
        </div>
        <div style="display: flex; gap: 12px; margin-top: 12px;">
          <div class="mini-metric">
            <span>待跟进应收</span>
            <strong>&yen;{{ receivablePressure.endingReceivableAmount.toFixed(2) }}</strong>
          </div>
          <div class="mini-metric">
            <span>逾期应收</span>
            <strong>{{ receivablePressure.overdueReceivableCount }} 笔</strong>
          </div>
          <div class="mini-metric">
            <span>今日催收</span>
            <strong>承诺回款 / 逾期承诺</strong>
          </div>
        </div>
        <div style="margin-top: 12px;">
          <div v-for="item in receivableFollowUp" :key="item.saleId"
               style="display: flex; align-items: center; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f0f0f0;">
            <div>
              <span style="font-weight: 500; color: #303133;">{{ item.saleNo }}</span>
              <span style="margin-left: 12px; color: #909399; font-size: 13px;">
                ¥{{ ((item.saleAmount || 0) - (item.paidAmount || 0)).toFixed(2) }}
              </span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 12px; color: #E6A23C;">{{ computeAgeDays(item.saleDate) }}天</span>
              <el-button type="primary" link size="small" @click="$router.push('/finance/sale?tab=receivable')">去缴款</el-button>
            </div>
          </div>
        </div>
      </div>

      <article class="panel health-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">STORE HEALTH</span>
            <h3>门店健康矩阵</h3>
          </div>
          <div class="health-summary">
            <span><b>{{ riskStoreCount }}</b> 风险门店</span>
            <span><b>{{ watchStoreCount }}</b> 观察门店</span>
          </div>
        </div>
        <div v-if="healthRows.length" class="health-list">
          <div v-for="store in healthRows" :key="store.deptId || store.deptName" class="health-row">
            <div class="store-name">
              <strong>{{ store.deptName || '未命名门店' }}</strong>
              <span>{{ healthLevelText(store.healthLevel) }}</span>
            </div>
            <div class="health-meter">
              <i :style="{ width: `${clampScore(store.healthScore)}%` }" :class="scoreClass(store.healthScore)" />
            </div>
            <div class="health-score">{{ number(store.healthScore, 0) }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无授权门店健康数据" :image-size="76" />
      </article>
    </section>

    <section class="review-grid">
      <article class="panel weekly-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">WEEK REVIEW</span>
            <h3>本周复盘</h3>
          </div>
          <span class="period-text">{{ weeklyBoard.weekStart || '-' }} 至 {{ weeklyBoard.weekEnd || '-' }}</span>
        </div>
        <div class="weekly-stats">
          <div>
            <span>本周销售</span>
            <strong>{{ money(weeklyBoard.salesAmount) }}</strong>
            <em :class="rateClass(weeklyBoard.salesChangeRate)">环比 {{ rate(weeklyBoard.salesChangeRate) }}</em>
          </div>
          <div>
            <span>本周费用</span>
            <strong>{{ money(weeklyBoard.expenseAmount) }}</strong>
            <em :class="rateClass(-Number(weeklyBoard.expenseChangeRate || 0))">
              环比 {{ rate(weeklyBoard.expenseChangeRate) }}
            </em>
          </div>
          <div>
            <span>净现金流</span>
            <strong :class="amountClass(weeklyBoard.netCashflowAmount)">
              {{ money(weeklyBoard.netCashflowAmount) }}
            </strong>
            <em :class="rateClass(weeklyBoard.cashflowChangeRate)">环比 {{ rate(weeklyBoard.cashflowChangeRate) }}</em>
          </div>
        </div>
        <div class="weekly-note">
          <strong>下周重点</strong>
          <p>{{ weeklyBoard.nextWeekFocus || '保持复盘节奏，优先处理高风险门店与未闭环任务。' }}</p>
        </div>
      </article>

      <article class="panel memo-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">WEEKLY MEMO</span>
            <h3>周经营纪要</h3>
          </div>
          <span class="memo-score" :class="scoreClass(weeklyMemo.reviewQualityScore)">
            {{ number(weeklyMemo.reviewQualityScore, 0) }}
          </span>
        </div>
        <div class="memo-content">
          <p>{{ weeklyMemo.headline || weeklyMemo.summary || weeklyBoard.weeklySummary || '本周纪要待生成，先从每日复盘任务闭环开始。' }}</p>
          <div v-if="memoHighlights.length" class="memo-tags">
            <span v-for="item in memoHighlights" :key="item">{{ item }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="lower-grid">
      <article class="panel trend-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">7 DAYS</span>
            <h3>近 7 日经营趋势</h3>
          </div>
        </div>
        <div v-if="trendRows.length" class="trend-list">
          <div v-for="row in trendRows" :key="row.label" class="trend-row">
            <span>{{ row.label }}</span>
            <div class="trend-bars">
              <i class="sales" :style="{ width: `${row.salesWidth}%` }" />
              <i class="expense" :style="{ width: `${row.expenseWidth}%` }" />
            </div>
            <strong>{{ money(row.sales) }}</strong>
          </div>
        </div>
        <el-empty v-else description="暂无趋势数据" :image-size="70" />
      </article>

      <article class="panel member-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">MEMBER ACTION</span>
            <h3>会员经营动作</h3>
          </div>
        </div>
        <div v-if="memberActions.length" class="member-actions">
          <div v-for="action in memberActions" :key="action.title || action.name" class="member-action">
            <strong>{{ action.title || action.name }}</strong>
            <p>{{ action.description || action.desc || action.suggestion || '关注会员活跃、复购与积分成本变化。' }}</p>
          </div>
        </div>
        <div v-else class="quiet-tip">暂无会员动作建议，建议持续关注新会员首购与复购会员变化。</div>
      </article>

      <article class="panel period-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">ACCOUNTING</span>
            <h3>当前核算周期</h3>
          </div>
        </div>
        <div class="period-body">
          <strong>{{ currentPeriod.periodNo || currentPeriod.periodName || '未初始化' }}</strong>
          <p>{{ currentPeriod.startTime || '-' }} 至 {{ currentPeriod.endTime || '-' }}</p>
          <span :class="periodStatusClass">{{ periodStatusText }}</span>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getDailyReviewBoard, getWeeklyMemo, getWeeklyReviewBoard } from '@/api/finance/dailyReview'
import { getAuthorizedStorePortfolio } from '@/api/finance/storeReport'
import { listReviewTasks } from '@/api/finance/reviewTask'
import { getCurrentAccountingPeriod } from '@/api/finance/accountingPeriod'
import { getDashboardOperation, getDashboardTrend } from '@/api/member/dashboard'
import { listReceivable } from '@/api/finance/sale'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const selectedDeptId = ref<number | null>(userStore.currentDeptId)
const reviewTasks = ref<any[]>([])
const portfolio = ref<any>({})
const memberOperation = ref<any>({})
const memberTrend = ref<any>({})
const currentPeriod = ref<any>({})
const receivableFollowUp = ref<any[]>([])

const dailyReview = reactive<any>({
  salesAmount: 0,
  expenseAmount: 0,
  cashInAmount: 0,
  netCashflowAmount: 0,
  pendingTaskCount: 0,
  highPriorityTaskCount: 0,
  focusItems: [],
  suggestions: [],
})

const weeklyBoard = reactive<any>({
  weekStart: '',
  weekEnd: '',
  salesAmount: 0,
  expenseAmount: 0,
  netCashflowAmount: 0,
  salesChangeRate: 0,
  expenseChangeRate: 0,
  cashflowChangeRate: 0,
  weeklySummary: '',
  nextWeekFocus: '',
})

const weeklyMemo = reactive<any>({
  reviewQualityScore: null,
  headline: '',
  summary: '',
  keyChanges: [],
  completedActions: [],
  unresolvedRisks: [],
  highlights: [],
  nextActions: [],
  nextWeekFocus: [],
})

const deptOptions = computed<any[]>(() => userStore.depts || [])
const todayLabel = computed(() => new Date().toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', weekday: 'long' }))
const scopeText = computed(() => {
  if (deptOptions.value.length > 1) {
    return `授权 ${deptOptions.value.length} 家门店，当前查看 ${currentDeptName.value}`
  }
  return currentDeptName.value || '当前门店'
})
const currentDeptName = computed(() => {
  const dept = deptOptions.value.find((item) => Number(item.deptId) === Number(selectedDeptId.value))
  return dept?.deptName || userStore.currentDeptName || '未选择门店'
})
const selectedDeptIds = computed(() => selectedDeptId.value ? [Number(selectedDeptId.value)] : [])

const healthRows = computed<any[]>(() => {
  const rows = portfolio.value?.storeRows || portfolio.value?.stores || portfolio.value?.rows || []
  return [...rows]
    .sort((a, b) => Number(a.healthScore || 0) - Number(b.healthScore || 0))
    .slice(0, 6)
})
const riskStoreCount = computed(() => Number(portfolio.value?.riskStoreCount ?? healthRows.value.filter((row) => Number(row.healthScore || 0) < 60).length))
const watchStoreCount = computed(() => Number(portfolio.value?.watchStoreCount ?? healthRows.value.filter((row) => Number(row.healthScore || 0) >= 60 && Number(row.healthScore || 0) < 80).length))
const pendingTaskCount = computed(() => {
  const fromList = reviewTasks.value.filter((task) => !['DONE', 'IGNORED'].includes(String(task.status || '').toUpperCase())).length
  return Math.max(fromList, Number(dailyReview.pendingTaskCount || 0))
})
const portfolioHealthScore = computed(() => {
  if (portfolio.value?.averageHealthScore !== undefined) return number(portfolio.value.averageHealthScore, 0)
  if (!healthRows.value.length) return 0
  return number(healthRows.value.reduce((sum, row) => sum + Number(row.healthScore || 0), 0) / healthRows.value.length, 0)
})
const mainStatusTitle = computed(() => {
  if (riskStoreCount.value > 0) return '存在风险门店，需要优先复盘'
  if (pendingTaskCount.value > 0) return '经营平稳，复盘任务待闭环'
  return '经营状态健康，保持节奏'
})
const mainStatusDesc = computed(() => {
  if (riskStoreCount.value > 0) return `当前有 ${riskStoreCount.value} 家门店健康分偏低，建议先处理费用、销售下滑或复盘超时问题。`
  if (pendingTaskCount.value > 0) return `当前还有 ${pendingTaskCount.value} 个复盘任务未完成，建议今天闭环。`
  return '暂无高风险事项，继续观察销售、费用和会员活跃变化。'
})
const periodReturnRate = computed(() => {
  const salePayment = Number(currentPeriod.value?.totalSalePayment || 0)
  const costTotal = Number(currentPeriod.value?.totalVerifiedExpense || 0)
    + Number(currentPeriod.value?.totalPurchase || 0)
    + Number(currentPeriod.value?.totalUnverifiedAdvance || 0)
  if (!costTotal) return 0
  return Math.min(100, Math.max(0, Math.round((salePayment / costTotal) * 100)))
})
const periodFinanceCards = computed(() => {
  const period = currentPeriod.value || {}
  const netProfit = Number(period.netProfit || 0)
  return [
    {
      key: 'salePayment',
      label: '销售缴款',
      value: money(period.totalSalePayment),
      hint: '当前周期累计',
      className: 'amount-blue',
    },
    {
      key: 'expense',
      label: '已核销费用',
      value: money(period.totalVerifiedExpense),
      hint: '当前周期累计',
      className: 'amount-gold',
    },
    {
      key: 'purchase',
      label: '进货款',
      value: money(period.totalPurchase),
      hint: '当前周期累计',
      className: 'amount-cyan',
    },
    {
      key: 'advance',
      label: '借支未核销',
      value: money(period.totalUnverifiedAdvance),
      hint: '当前周期累计',
      className: 'amount-negative',
    },
    {
      key: 'profit',
      label: '净利润',
      value: money(period.netProfit),
      hint: netProfit >= 0 ? '盈利' : '亏损',
      className: amountClass(netProfit),
    },
  ]
})

const priorityItems = computed(() => {
  const tasks = reviewTasks.value
    .filter((task) => !['DONE', 'IGNORED'].includes(String(task.status || '').toUpperCase()))
    .map((task, index) => ({
      key: `task-${task.taskId || index}`,
      title: task.taskTitle || task.title || '复盘任务待处理',
      desc: task.reason || task.taskReason || task.description || '请进入复盘任务查看处理依据。',
      label: task.priority || task.level || '待处理',
      badge: '复',
      level: priorityLevel(task.priority || task.level),
    }))

  const focus = (dailyReview.focusItems || []).map((item: any, index: number) => ({
    key: `focus-${index}`,
    title: item.title || item.name || '今日经营关注项',
    desc: item.description || item.desc || item.suggestion || String(item),
    label: item.level || '关注',
    badge: '今',
    level: priorityLevel(item.level),
  }))

  return [...tasks, ...focus].slice(0, 6)
})

const memoHighlights = computed(() => {
  const source = weeklyMemo.keyChanges?.length
    ? weeklyMemo.keyChanges
    : weeklyMemo.unresolvedRisks?.length
      ? weeklyMemo.unresolvedRisks
      : weeklyMemo.nextWeekFocus?.length
        ? weeklyMemo.nextWeekFocus
        : weeklyMemo.highlights?.length
          ? weeklyMemo.highlights
          : weeklyMemo.nextActions
  return (source || []).map((item: any) => item.title || item.name || item).slice(0, 4)
})

const memberActions = computed<any[]>(() => {
  const actions = memberOperation.value?.actionItems || memberOperation.value?.memberActionItems || memberOperation.value?.suggestions || []
  return actions.slice(0, 3)
})

const trendRows = computed(() => {
  const labels = memberTrend.value?.dates || memberTrend.value?.dateList || []
  const sales = memberTrend.value?.dailySale || memberTrend.value?.salesAmounts || memberTrend.value?.salesAmountList || memberTrend.value?.sales || memberTrend.value?.consumeAmounts || []
  const expenses = memberTrend.value?.dailyExpense || memberTrend.value?.expenseAmounts || memberTrend.value?.expenseAmountList || memberTrend.value?.expenses || []
  const rows = labels.slice(-7).map((label: string, index: number) => ({
    label,
    sales: Number(sales[sales.length - labels.slice(-7).length + index] || 0),
    expense: Number(expenses[expenses.length - labels.slice(-7).length + index] || 0),
  }))
  const maxValue = Math.max(1, ...rows.flatMap((row: any) => [row.sales, row.expense]))
  return rows.map((row: any) => ({
    ...row,
    salesWidth: Math.max(4, Math.round((row.sales / maxValue) * 100)),
    expenseWidth: Math.max(4, Math.round((row.expense / maxValue) * 100)),
  }))
})

const periodStatusText = computed(() => {
  const status = String(currentPeriod.value?.status ?? '')
  if (status === '2' || status === 'LOCKED') return '已锁账'
  if (status === '1' || status === 'CLOSED') return '已结转'
  if (currentPeriod.value?.periodNo || currentPeriod.value?.periodName) return '进行中'
  return '待初始化'
})
const periodStatusClass = computed(() => {
  if (periodStatusText.value === '已锁账') return 'period-locked'
  if (periodStatusText.value === '已结转') return 'period-closed'
  if (periodStatusText.value === '进行中') return 'period-active'
  return 'period-empty'
})

const receivablePressure = computed(() => {
  const rows = receivableFollowUp.value || []
  const endingReceivableAmount = rows.reduce((sum, item) => {
    const unpaid = Number(item.unpaidAmount ?? (Number(item.saleAmount || 0) - Number(item.paidAmount || 0)))
    return sum + Math.max(0, unpaid)
  }, 0)
  const overdueReceivableCount = rows.filter((item) => computeAgeDays(item.saleDate) > 30).length
  return { endingReceivableAmount, overdueReceivableCount }
})

onMounted(() => {
  loadData()
})

async function handleDeptChange(value: number) {
  selectedDeptId.value = value
  await userStore.switchDept(value).catch(() => undefined)
  loadData()
}

async function loadData() {
  loading.value = true
  const deptId = selectedDeptId.value
  const deptPayload = deptId ? { deptId } : {}
  const deptIds = selectedDeptIds.value

  try {
    const results = await Promise.allSettled([
      getDailyReviewBoard(deptPayload),
      getWeeklyReviewBoard(deptPayload),
      getWeeklyMemo(deptPayload),
      getAuthorizedStorePortfolio({ deptIds, timeType: 'day' }),
      listReviewTasks({ pageNum: 1, pageSize: 8, deptId }),
      getDashboardOperation(deptIds),
      getDashboardTrend(deptIds),
      deptId ? getCurrentAccountingPeriod(deptId) : Promise.resolve({ data: {} }),
    ])

    assignReactive(dailyReview, dataOf(results[0]))
    assignReactive(weeklyBoard, dataOf(results[1]))
    assignReactive(weeklyMemo, dataOf(results[2]))
    portfolio.value = dataOf(results[3])
    reviewTasks.value = rowsOf(results[4])
    memberOperation.value = dataOf(results[5])
    memberTrend.value = dataOf(results[6])
    currentPeriod.value = dataOf(results[7])

    listReceivable({ pageNum: 1, pageSize: 5, deptId, minAgeDays: 7 }).then((r: any) => {
      receivableFollowUp.value = r.data?.rows || r.rows || []
    }).catch(() => { receivableFollowUp.value = [] })
  } catch (error) {
    ElMessage.error('加载店长首页失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function dataOf(result: PromiseSettledResult<any>) {
  if (result.status !== 'fulfilled') return {}
  return result.value?.data || {}
}

function rowsOf(result: PromiseSettledResult<any>) {
  if (result.status !== 'fulfilled') return []
  return result.value?.rows || result.value?.data?.rows || result.value?.data || []
}

function assignReactive(target: any, source: any) {
  Object.keys(target).forEach((key) => {
    if (source[key] !== undefined) target[key] = source[key]
  })
}

function goReviewTask() {
  router.push('/finance/reviewTask')
}

function number(value: any, digits = 2) {
  const n = Number(value || 0)
  return Number(n.toFixed(digits))
}

function money(value: any) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function rate(value: any) {
  const n = Number(value || 0)
  return `${n > 0 ? '+' : ''}${n.toFixed(1)}%`
}

function clampScore(value: any) {
  return Math.min(100, Math.max(0, Number(value || 0)))
}

function scoreClass(value: any) {
  const score = Number(value || 0)
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-watch'
  return 'score-risk'
}

function amountClass(value: any) {
  return Number(value || 0) >= 0 ? 'amount-positive' : 'amount-negative'
}

function rateClass(value: any) {
  return Number(value || 0) >= 0 ? 'rate-up' : 'rate-down'
}

function healthLevelText(level: any) {
  const value = String(level || '').toUpperCase()
  if (value === 'RISK') return '风险'
  if (value === 'WATCH') return '观察'
  if (value === 'GOOD') return '健康'
  return '待评估'
}

function priorityLevel(level: any) {
  const value = String(level || '').toUpperCase()
  if (['HIGH', 'URGENT', 'P0', 'P1'].includes(value)) return 'high'
  if (['MEDIUM', 'P2'].includes(value)) return 'medium'
  return 'normal'
}

function computeAgeDays(saleDate: string) {
  if (!saleDate) return 0
  const diff = Date.now() - new Date(saleDate).getTime()
  return Math.floor(diff / (1000 * 60 * 60 * 24))
}
</script>

<style scoped>
.store-dashboard {
  min-height: 100%;
  padding: 22px;
  background: #f5f7fb;
  color: #172033;
}

.workbench-header,
.panel,
.period-hero,
.period-kpi-strip,
.status-panel,
.metric-strip {
  border: 1px solid #dfe6f1;
  border-radius: 8px;
  background: #fff;
}

.workbench-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
}

.header-copy h2 {
  margin: 6px 0 8px;
  font-size: 26px;
  line-height: 1.2;
  letter-spacing: 0;
}

.header-copy p {
  margin: 0;
  color: #637083;
}

.eyebrow,
.panel-kicker {
  color: #2667c9;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dept-select {
  width: 220px;
}

.period-finance-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.78fr) minmax(560px, 1.4fr);
  gap: 14px;
  margin-top: 14px;
}

.period-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  padding: 20px 22px;
  background: #fff;
}

.period-hero h3 {
  margin: 6px 0 8px;
  font-size: 18px;
  letter-spacing: 0;
}

.period-hero strong {
  display: block;
  font-size: 30px;
  line-height: 1.1;
  word-break: break-word;
}

.period-hero p {
  margin: 10px 0 0;
  color: #637083;
  line-height: 1.5;
}

.break-even-ring {
  display: grid;
  place-items: center;
  width: 92px;
  height: 92px;
  flex: 0 0 auto;
  border: 1px solid currentColor;
  border-radius: 50%;
}

.break-even-ring span {
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}

.break-even-ring small {
  margin-top: 4px;
  font-size: 12px;
}

.period-kpi-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  overflow: hidden;
}

.period-kpi-item {
  min-width: 0;
  padding: 19px 16px;
  border-right: 1px solid #edf1f7;
  background: #fff;
}

.period-kpi-item:last-child {
  border-right: 0;
}

.period-kpi-item span {
  display: block;
  color: #637083;
  font-size: 13px;
}

.period-kpi-item strong {
  display: block;
  margin-top: 10px;
  font-size: 20px;
  line-height: 1.2;
  word-break: break-word;
}

.period-kpi-item em {
  display: block;
  margin-top: 8px;
  color: #8a96a8;
  font-size: 12px;
  font-style: normal;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.95fr) minmax(420px, 1.4fr);
  gap: 14px;
  margin-top: 14px;
}

.status-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
}

.status-main strong {
  display: block;
  margin: 8px 0;
  font-size: 20px;
}

.status-main p {
  margin: 0;
  color: #637083;
  line-height: 1.6;
}

.status-score {
  display: grid;
  place-items: center;
  width: 92px;
  height: 92px;
  border: 1px solid currentColor;
  border-radius: 50%;
  flex: 0 0 auto;
}

.status-score span {
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.status-score small {
  margin-top: 4px;
  font-size: 12px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
}

.metric-item {
  min-width: 0;
  padding: 22px 18px;
  background: #fff;
}

.metric-item span {
  display: block;
  color: #637083;
  font-size: 13px;
}

.metric-item strong {
  display: block;
  margin-top: 10px;
  font-size: 22px;
  line-height: 1.2;
  word-break: break-word;
}

.action-grid,
.review-grid,
.lower-grid {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.action-grid {
  grid-template-columns: minmax(420px, 1.1fr) minmax(360px, 0.9fr);
}

.review-grid {
  grid-template-columns: minmax(420px, 1.15fr) minmax(340px, 0.85fr);
}

.lower-grid {
  grid-template-columns: minmax(380px, 1.1fr) minmax(300px, 0.9fr) minmax(260px, 0.7fr);
}

.panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 4px 0 0;
  font-size: 17px;
  letter-spacing: 0;
}

.priority-list,
.health-list,
.trend-list,
.member-actions {
  display: grid;
  gap: 10px;
}

.priority-row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e4eaf3;
  border-left: 4px solid #2d6cdf;
  border-radius: 8px;
  background: #fbfcff;
}

.priority-row.high {
  border-left-color: #d9534f;
}

.priority-row.medium {
  border-left-color: #d99118;
}

.priority-marker {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #eaf2ff;
  color: #2667c9;
  font-weight: 800;
}

.priority-content {
  min-width: 0;
}

.priority-content strong,
.store-name strong,
.member-action strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.priority-content p,
.member-action p,
.weekly-note p,
.memo-content p,
.period-body p {
  margin: 4px 0 0;
  color: #667386;
  line-height: 1.55;
}

.priority-tag {
  color: #4a5a70;
  font-size: 12px;
  white-space: nowrap;
}

.health-summary {
  display: flex;
  gap: 12px;
  color: #647286;
  font-size: 12px;
}

.health-summary b {
  color: #172033;
}

.health-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(120px, 1.1fr) 42px;
  align-items: center;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px solid #edf1f7;
}

.health-row:last-child {
  border-bottom: 0;
}

.store-name span {
  color: #7a8798;
  font-size: 12px;
}

.health-meter {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #e8edf5;
}

.health-meter i {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.health-score {
  text-align: right;
  font-weight: 800;
}

.weekly-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.weekly-stats div {
  min-width: 0;
  padding: 14px;
  border-radius: 8px;
  background: #f7f9fd;
}

.weekly-stats span,
.period-text {
  color: #687589;
  font-size: 12px;
}

.weekly-stats strong {
  display: block;
  margin: 8px 0 6px;
  font-size: 18px;
  word-break: break-word;
}

.weekly-stats em {
  font-style: normal;
  font-size: 12px;
}

.weekly-note {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #edf1f7;
}

.memo-score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 46px;
  height: 30px;
  padding: 0 10px;
  border-radius: 8px;
  font-weight: 800;
}

.memo-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.memo-tags span {
  padding: 5px 9px;
  border-radius: 6px;
  background: #eef4ff;
  color: #2667c9;
  font-size: 12px;
}

.trend-row {
  display: grid;
  grid-template-columns: 74px minmax(120px, 1fr) 92px;
  align-items: center;
  gap: 10px;
  color: #59677a;
  font-size: 13px;
}

.trend-bars {
  display: grid;
  gap: 4px;
}

.trend-bars i {
  display: block;
  height: 7px;
  border-radius: 999px;
}

.trend-bars .sales {
  background: #2d6cdf;
}

.trend-bars .expense {
  background: #d99118;
}

.trend-row strong {
  text-align: right;
  color: #172033;
  font-size: 13px;
}

.member-action,
.quiet-tip {
  padding: 12px;
  border-radius: 8px;
  background: #f7f9fd;
}

.period-body strong {
  display: block;
  font-size: 22px;
  word-break: break-word;
}

.period-body span {
  display: inline-flex;
  margin-top: 14px;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
}

.score-good,
.amount-positive,
.rate-up {
  color: #138a5b;
}

.amount-blue {
  color: #2667c9;
}

.amount-gold {
  color: #b7791f;
}

.amount-cyan {
  color: #087f95;
}

.score-watch {
  color: #b7791f;
}

.score-risk,
.amount-negative,
.rate-down {
  color: #c43d3d;
}

.health-meter .score-good {
  background: #22a06b;
}

.health-meter .score-watch {
  background: #d99118;
}

.health-meter .score-risk {
  background: #d9534f;
}

.period-active {
  background: #e8f7ef;
  color: #138a5b;
}

.period-closed {
  background: #eef4ff;
  color: #2667c9;
}

.period-locked {
  background: #fff4e5;
  color: #b7791f;
}

.period-empty {
  background: #f0f3f8;
  color: #69788c;
}

.card {
  border: 1px solid #dfe6f1;
  border-radius: 8px;
  background: #fff;
  padding: 18px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.mini-metric {
  flex: 1;
  padding: 8px 12px;
  border-radius: 6px;
  background: #f7f9fd;
}

.mini-metric span {
  display: block;
  color: #637083;
  font-size: 12px;
}

.mini-metric strong {
  display: block;
  margin-top: 4px;
  font-size: 16px;
  color: #172033;
}

@media (max-width: 1180px) {
  .period-finance-grid,
  .top-grid,
  .action-grid,
  .review-grid,
  .lower-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .store-dashboard {
    padding: 12px;
  }

  .workbench-header,
  .status-panel,
  .panel-head {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    align-items: stretch;
  }

  .dept-select {
    width: 100%;
  }

  .metric-strip,
  .period-kpi-strip,
  .weekly-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .period-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .priority-row,
  .health-row,
  .trend-row {
    grid-template-columns: 1fr;
  }

  .trend-row strong,
  .health-score {
    text-align: left;
  }
}
</style>
