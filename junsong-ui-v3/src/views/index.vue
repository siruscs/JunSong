<template>
  <StoreDashboard v-if="!isAdmin" />
  <div v-else class="admin-dashboard">
    <div class="bg-grid"></div>

    <header class="dashboard-header">
      <div class="header-copy">
        <span class="dashboard-kicker">ADMIN DATA VISUALIZATION</span>
        <h2 class="dashboard-title">{{ welcomeText }}</h2>
        <span class="dashboard-date">{{ currentTime }}</span>
      </div>
      <div class="header-status" :class="healthLevelClass">
        <span class="status-dot"></span>
        <strong>{{ healthStatusText }}</strong>
      </div>
    </header>

    <section class="hero-panel">
      <div class="hero-main">
        <span class="panel-label">管理资产总览</span>
        <div class="hero-value">{{ totalAssets }}</div>
        <div class="hero-meta">
          <span>组织、权限、配置等核心管理对象</span>
          <strong>{{ stats.users }} 名用户</strong>
        </div>
        <div class="hero-line"></div>
      </div>

      <div class="hero-kpis">
        <div v-for="item in primaryCards" :key="item.key" class="stat-card">
          <span class="stat-label">{{ item.label }}</span>
          <strong class="stat-value" :style="{ color: item.color }">{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </div>
    </section>

    <div class="visual-grid">
      <section class="chart-card org-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">组织规模</span>
            <h3>用户、角色、部门、岗位分布</h3>
          </div>
        </div>
        <div ref="orgChartRef" class="chart-body"></div>
      </section>

      <section class="chart-card health-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">系统健康度</span>
            <h3>服务器真实资源状态</h3>
          </div>
          <span class="refresh-time">{{ healthUpdateText }}</span>
        </div>
        <div v-loading="healthLoading" class="health-grid">
          <div class="health-score">
            <div>
              <strong>{{ dashboardHealth.overallScore }}</strong>
              <span>综合评分</span>
            </div>
            <em>{{ dashboardHealth.hostName || '当前服务器' }}</em>
          </div>
          <div v-for="item in healthItems" :key="item.key" class="health-item">
            <div class="health-copy">
              <strong>{{ item.label }}</strong>
              <span>{{ item.value }}%</span>
            </div>
            <el-progress :percentage="item.value" :stroke-width="9" :color="item.color" />
          </div>
        </div>
      </section>

      <section class="chart-card service-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">服务运行情况</span>
            <h3>{{ dashboardHealth.upServiceCount }} / {{ dashboardHealth.serviceCount }} 个服务在线</h3>
          </div>
          <span class="service-summary" :class="healthLevelClass">{{ dashboardHealth.downServiceCount }} 异常</span>
        </div>
        <div v-loading="healthLoading" class="service-grid">
          <div v-for="service in dashboardHealth.services" :key="service.code" class="service-card" :class="service.status === 'UP' ? 'is-up' : 'is-down'">
            <div class="service-topline">
              <span class="service-light"></span>
              <strong>{{ service.name }}</strong>
              <em>{{ service.status }}</em>
            </div>
            <div class="service-meta">
              <span>{{ service.code }}</span>
              <span>{{ service.responseTime || 0 }}ms</span>
            </div>
            <p>{{ service.message }}</p>
          </div>
          <el-empty v-if="dashboardHealth.services.length === 0 && !healthLoading" description="暂无服务状态" />
        </div>
      </section>

      <section class="chart-card asset-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">系统资产</span>
            <h3>字典、参数、通知占比</h3>
          </div>
        </div>
        <div ref="assetChartRef" class="chart-body asset-chart"></div>
      </section>

      <section class="chart-card workbench-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">统一工作台</span>
            <h3>经营待办任务</h3>
          </div>
          <el-tag :type="workbenchTasks.length > 0 ? 'warning' : 'success'" size="small">
            {{ workbenchTasks.length > 0 ? workbenchTasks.length + ' 项待办' : '暂无待办' }}
          </el-tag>
        </div>
        <div class="workbench-tabs">
          <button
            v-for="tab in workbenchTabs"
            :key="tab.key"
            class="workbench-tab"
            :class="{ active: workbenchFilter === tab.key }"
            @click="workbenchFilter = tab.key"
          >
            {{ tab.label }}
            <em v-if="tab.count > 0">{{ tab.count }}</em>
          </button>
        </div>
        <div class="workbench-list">
          <div v-for="(task, idx) in filteredWorkbenchTasks" :key="idx" class="workbench-item">
            <div class="workbench-top">
              <el-tag size="small" effect="plain">{{ task.sourceModule }}</el-tag>
              <el-tag :type="severityTagType(task.severity)" size="small" effect="dark">{{ task.severity }}</el-tag>
              <strong>{{ task.title }}</strong>
              <el-tag v-if="task.status" :type="statusTagType(task.status)" size="small" effect="light">{{ statusLabel(task.status) }}</el-tag>
            </div>
            <p class="workbench-reason">{{ task.reason }}</p>
            <div class="workbench-foot">
              <span>{{ task.suggestion }}</span>
              <span v-if="task.impactAmount" class="impact-amount">影响金额: {{ money(task.impactAmount) }}</span>
              <router-link
                v-if="task.targetRoute"
                :to="resolveTaskRoute(task)"
                class="more-link"
              >{{ task.status === 'VIEW_ONLY' ? '查看' : '去处理' }}</router-link>
            </div>
          </div>
          <el-empty v-if="filteredWorkbenchTasks.length === 0" description="当前暂无待处理经营任务" />
        </div>
      </section>

      <!-- R8-B: 今日复盘区块 -->
      <section class="chart-card daily-review-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">今日复盘</span>
            <h3>{{ dailyReview.reviewDate }} 经营快照</h3>
          </div>
          <el-tag :type="dailyReview.highPriorityTaskCount > 0 ? 'danger' : 'success'" size="small">
            {{ dailyReview.highPriorityTaskCount > 0 ? dailyReview.highPriorityTaskCount + ' 项高优' : '无高优待办' }}
          </el-tag>
        </div>
        <div v-loading="dailyReviewLoading" class="daily-review-body">
          <div class="daily-review-kpis">
            <div class="dr-kpi">
              <span class="dr-label">销售额</span>
              <strong>¥{{ formatAmount(dailyReview.salesAmount) }}</strong>
            </div>
            <div class="dr-kpi">
              <span class="dr-label">实收现金</span>
              <strong>¥{{ formatAmount(dailyReview.cashInAmount) }}</strong>
            </div>
            <div class="dr-kpi">
              <span class="dr-label">费用支出</span>
              <strong>¥{{ formatAmount(dailyReview.expenseAmount) }}</strong>
            </div>
            <div class="dr-kpi" :class="{ negative: isNegativeCashflow }">
              <span class="dr-label">净现金流</span>
              <strong>¥{{ formatAmount(dailyReview.netCashflowAmount) }}</strong>
            </div>
          </div>
          <div v-if="dailyReview.focusItems.length > 0" class="daily-review-focus">
            <span class="panel-label">本日关注项 Top 3</span>
            <div v-for="(item, idx) in dailyReview.focusItems" :key="idx" class="focus-item">
              <el-tag type="danger" size="small" effect="dark">{{ item.itemType }}</el-tag>
              <strong>{{ item.title }}</strong>
              <router-link v-if="item.targetRoute" :to="item.targetRoute" class="more-link">去处理</router-link>
            </div>
          </div>
          <div v-if="dailyReview.suggestions.length > 0" class="daily-review-suggestions">
            <span v-for="(s, idx) in dailyReview.suggestions" :key="idx" class="suggestion-text">{{ s }}</span>
          </div>
          <el-empty v-if="!dailyReviewLoading && dailyReview.focusItems.length === 0 && dailyReview.suggestions.length === 0" description="暂无复盘数据" />
          <div v-if="dailyReviewError" class="dr-error">{{ dailyReviewError }}</div>
        </div>
      </section>

      <!-- R9-A: 周复盘看板 -->
      <section class="chart-card weekly-board-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">周复盘</span>
            <h3>{{ weeklyBoard.weekStart }} ~ {{ weeklyBoard.weekEnd }} {{ weeklyBoard.deptName }}</h3>
          </div>
          <div class="wb-task-summary">
            <el-tag type="success" size="small">{{ weeklyBoard.completedTaskCount }} 已完成</el-tag>
            <el-tag v-if="weeklyBoard.pendingTaskCount > 0" type="warning" size="small">{{ weeklyBoard.pendingTaskCount }} 待处理</el-tag>
          </div>
        </div>
        <div v-loading="weeklyBoardLoading" class="weekly-board-body">
          <div class="wb-metrics-grid">
            <div class="wb-metric">
              <span class="wb-label">本周销售</span>
              <strong>¥{{ formatAmount(weeklyBoard.salesAmount) }}</strong>
              <em :class="rateClass(weeklyBoard.salesChangeRate)">{{ formatRate(weeklyBoard.salesChangeRate) }}</em>
            </div>
            <div class="wb-metric">
              <span class="wb-label">费用支出</span>
              <strong>¥{{ formatAmount(weeklyBoard.expenseAmount) }}</strong>
              <em :class="rateClass(weeklyBoard.expenseChangeRate)">{{ formatRate(weeklyBoard.expenseChangeRate) }}</em>
            </div>
            <div class="wb-metric">
              <span class="wb-label">实收现金</span>
              <strong>¥{{ formatAmount(weeklyBoard.cashInAmount) }}</strong>
            </div>
            <div class="wb-metric" :class="{ negative: Number(weeklyBoard.netCashflowAmount) < 0 }">
              <span class="wb-label">净现金流</span>
              <strong>¥{{ formatAmount(weeklyBoard.netCashflowAmount) }}</strong>
              <em :class="rateClass(weeklyBoard.cashflowChangeRate)">{{ formatRate(weeklyBoard.cashflowChangeRate) }}</em>
            </div>
          </div>
          <div v-if="weeklyBoard.weeklySummary" class="wb-summary">
            <span class="panel-label">本周总结</span>
            <p>{{ weeklyBoard.weeklySummary }}</p>
          </div>
          <div v-if="weeklyBoard.nextWeekFocus" class="wb-focus">
            <span class="panel-label">下周重点</span>
            <p>{{ weeklyBoard.nextWeekFocus }}</p>
          </div>
          <el-empty v-if="!weeklyBoardLoading && !weeklyBoard.weekStart" description="暂无周复盘数据" />
          <div v-if="weeklyBoardError" class="dr-error">{{ weeklyBoardError }}</div>
        </div>
      </section>

      <!-- R10-F: 周经营纪要 -->
      <section class="chart-card weekly-memo-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">周经营纪要</span>
            <h3>{{ weeklyMemo.weekStart }} ~ {{ weeklyMemo.weekEnd }}</h3>
          </div>
        </div>
        <div v-loading="weeklyMemoLoading" class="weekly-memo-body">
          <p v-if="weeklyMemo.headline" class="memo-headline">{{ weeklyMemo.headline }}</p>
          <div v-if="weeklyMemo.reviewQualityScore !== null && weeklyMemo.reviewQualityScore !== undefined" class="memo-section memo-score-section">
            <span class="memo-label">复盘质量分</span>
            <span class="memo-score" :class="scoreClass(weeklyMemo.reviewQualityScore)">{{ Number(weeklyMemo.reviewQualityScore).toFixed(0) }}</span>
          </div>
          <div v-if="weeklyMemo.keyChanges.length > 0" class="memo-section">
            <span class="memo-label">关键变化</span>
            <div v-for="(c, i) in weeklyMemo.keyChanges" :key="i" class="memo-item">{{ c }}</div>
          </div>
          <div v-if="weeklyMemo.completedActions.length > 0" class="memo-section">
            <span class="memo-label">已完成动作</span>
            <div v-for="(a, i) in weeklyMemo.completedActions" :key="i" class="memo-item">{{ a }}</div>
          </div>
          <div v-if="weeklyMemo.unresolvedRisks.length > 0" class="memo-section">
            <span class="memo-label">未解决风险</span>
            <div v-for="(r, i) in weeklyMemo.unresolvedRisks" :key="i" class="memo-item memo-risk">{{ r }}</div>
          </div>
          <div v-if="weeklyMemo.nextWeekFocus.length > 0" class="memo-section">
            <span class="memo-label">下周重点</span>
            <div v-for="(f, i) in weeklyMemo.nextWeekFocus" :key="i" class="memo-item">{{ f }}</div>
          </div>
          <div v-if="weeklyMemo.riskStoreCount || weeklyMemo.watchStoreCount || weeklyMemo.goodStoreCount" class="memo-section">
            <span class="memo-label">本周门店健康分布</span>
            <div class="memo-health-dist">
              <span v-if="weeklyMemo.goodStoreCount" class="memo-health-tag good">良好 {{ weeklyMemo.goodStoreCount }}</span>
              <span v-if="weeklyMemo.watchStoreCount" class="memo-health-tag watch">关注 {{ weeklyMemo.watchStoreCount }}</span>
              <span v-if="weeklyMemo.riskStoreCount" class="memo-health-tag risk">风险 {{ weeklyMemo.riskStoreCount }}</span>
            </div>
          </div>
          <div v-if="weeklyMemo.storeHealthHighlights && weeklyMemo.storeHealthHighlights.length > 0" class="memo-section">
            <span class="memo-label">重点门店</span>
            <div v-for="(h, i) in weeklyMemo.storeHealthHighlights" :key="i" class="memo-item memo-risk">{{ h }}</div>
          </div>
          <div v-if="weeklyMemo.reusableKnowledgeHints && weeklyMemo.reusableKnowledgeHints.length > 0" class="memo-section">
            <span class="memo-label">可复用动作</span>
            <div v-for="(k, i) in weeklyMemo.reusableKnowledgeHints" :key="i" class="memo-item memo-knowledge">{{ k }}</div>
          </div>
          <el-empty v-if="!weeklyMemoLoading && !weeklyMemo.headline" description="暂无纪要数据" />
        </div>
      </section>

      <!-- R12-G: 动作成效 -->
      <section class="chart-card effect-summary-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">动作成效</span>
            <h3>已完成动作效果评估</h3>
          </div>
          <el-tag v-if="effectSummary.evaluatedTaskCount > 0"
                  :type="effectSummary.averageEffectScore >= 60 ? 'success' : effectSummary.averageEffectScore >= 40 ? 'warning' : 'danger'"
                  size="small">
            均分 {{ effectSummary.averageEffectScore }}
          </el-tag>
          <el-tag v-else type="info" size="small">暂无评估</el-tag>
        </div>
        <div v-loading="effectSummaryLoading" class="effect-summary-body">
          <template v-if="effectSummary.evaluatedTaskCount > 0">
            <div class="effect-kpis">
              <div class="effect-kpi">
                <span class="effect-label">已评估</span>
                <strong>{{ effectSummary.evaluatedTaskCount }}</strong>
              </div>
              <div class="effect-kpi">
                <span class="effect-label">改善明显</span>
                <strong style="color:#0ea573;">{{ effectSummary.goodEffectCount }}</strong>
              </div>
              <div class="effect-kpi">
                <span class="effect-label">观察中</span>
                <strong style="color:#d4940a;">{{ effectSummary.watchEffectCount }}</strong>
              </div>
              <div class="effect-kpi">
                <span class="effect-label">未改善</span>
                <strong style="color:#d4456a;">{{ effectSummary.noImprovementCount }}</strong>
              </div>
            </div>
            <div v-if="effectSummary.reopenCandidates.length > 0" class="effect-reopen">
              <span class="panel-label">待重开 Top {{ effectSummary.reopenCandidates.length }}</span>
              <div v-for="rc in effectSummary.reopenCandidates" :key="rc.taskId" class="effect-reopen-item">
                <el-tag size="small" effect="plain">{{ rc.taskType }}</el-tag>
                <strong>{{ rc.title }}</strong>
                <span class="effect-dept">{{ rc.deptName }}</span>
              </div>
            </div>
          </template>
          <el-empty v-else-if="!effectSummaryLoading" description="暂无已评估的动作成效" />
        </div>
      </section>

      <section class="chart-card notice-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">信息流</span>
            <h3>最新通知</h3>
          </div>
          <router-link to="/system/notice" class="more-link">查看更多</router-link>
        </div>
        <div class="notice-list">
          <div v-for="notice in noticeList" :key="notice.noticeId || notice.noticeTitle" class="notice-item">
            <el-tag :type="getNoticeTagType(notice.noticeType)" size="small" effect="light">
              {{ getNoticeTypeName(notice.noticeType) }}
            </el-tag>
            <div class="notice-copy">
              <strong>{{ notice.noticeTitle }}</strong>
              <span>{{ notice.createTime }}</span>
            </div>
          </div>
          <el-empty v-if="noticeList.length === 0" description="暂无通知" />
        </div>
      </section>

      <section class="chart-card matrix-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">管理入口</span>
            <h3>常用模块概览</h3>
          </div>
        </div>
        <div class="metric-matrix">
          <router-link v-for="item in moduleCards" :key="item.key" :to="item.to" class="matrix-card">
            <div class="matrix-icon" :style="{ '--glow': item.glow }">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import {
  Document,
  Menu,
  OfficeBuilding,
  Setting,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { listNotice } from '@/api/system/notice'
import { getDashboardHealth, getWorkbenchTasks } from '@/api/system/dashboard'
import { getDailyReviewBoard, getWeeklyReviewBoard, getWeeklyMemo } from '@/api/finance/dailyReview'
import { getEffectSummary } from '@/api/finance/reviewTask'
import { useUserStore } from '@/stores/user'
import StoreDashboard from '@/views/dashboard/StoreDashboard.vue'

interface ServiceHealth {
  name: string
  code: string
  url: string
  status: 'UP' | 'DOWN'
  message: string
  responseTime: number
}

interface DashboardHealth {
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  availableProcessors: number
  totalMemoryGb: number
  hostName: string
  osName: string
  overallScore: number
  level: 'EXCELLENT' | 'GOOD' | 'WARN'
  serviceCount: number
  upServiceCount: number
  downServiceCount: number
  updateTime: number
  services: ServiceHealth[]
}

const userStore = useUserStore()

const currentTime = ref('')
const noticeList = ref<any[]>([])
const workbenchTasks = ref<any[]>([])
const workbenchFilter = ref('ALL')
const orgChartRef = ref<HTMLElement>()
const assetChartRef = ref<HTMLElement>()
const healthLoading = ref(false)
const dailyReviewLoading = ref(false)
const dailyReviewError = ref('')
let orgChart: echarts.ECharts | null = null
let assetChart: echarts.ECharts | null = null
let timer: number | null = null
let healthTimer: number | null = null

interface DailyReviewFocusItem {
  itemType: string
  title: string
  reason?: string
  suggestion?: string
  targetRoute?: string
  impactAmount?: number
}

interface DailyReviewBoard {
  reviewDate: string
  deptId: number | null
  deptName: string
  salesAmount: number
  cashInAmount: number
  expenseAmount: number
  netCashflowAmount: number
  pendingTaskCount: number
  highPriorityTaskCount: number
  focusItems: DailyReviewFocusItem[]
  suggestions: string[]
}

const dailyReview = reactive<DailyReviewBoard>({
  reviewDate: '',
  deptId: null,
  deptName: '',
  salesAmount: 0,
  cashInAmount: 0,
  expenseAmount: 0,
  netCashflowAmount: 0,
  pendingTaskCount: 0,
  highPriorityTaskCount: 0,
  focusItems: [],
  suggestions: [],
})

interface WeeklyReviewBoard {
  weekStart: string
  weekEnd: string
  deptId: number | null
  deptName: string
  salesAmount: number
  expenseAmount: number
  cashInAmount: number
  netCashflowAmount: number
  previousWeekSalesAmount: number
  previousWeekExpenseAmount: number
  previousWeekNetCashflowAmount: number
  salesChangeRate: number
  expenseChangeRate: number
  cashflowChangeRate: number
  completedTaskCount: number
  pendingTaskCount: number
  weeklySummary: string
  nextWeekFocus: string
}

const weeklyBoardLoading = ref(false)
const weeklyBoardError = ref('')

const weeklyBoard = reactive<WeeklyReviewBoard>({
  weekStart: '',
  weekEnd: '',
  deptId: null,
  deptName: '',
  salesAmount: 0,
  expenseAmount: 0,
  cashInAmount: 0,
  netCashflowAmount: 0,
  previousWeekSalesAmount: 0,
  previousWeekExpenseAmount: 0,
  previousWeekNetCashflowAmount: 0,
  salesChangeRate: 0,
  expenseChangeRate: 0,
  cashflowChangeRate: 0,
  completedTaskCount: 0,
  pendingTaskCount: 0,
  weeklySummary: '',
  nextWeekFocus: '',
})

interface WeeklyMemo {
  weekStart: string
  weekEnd: string
  headline: string
  keyChanges: string[]
  completedActions: string[]
  unresolvedRisks: string[]
  nextWeekFocus: string[]
  storeHealthHighlights: string[]
  reusableKnowledgeHints: string[]
  riskStoreCount: number
  watchStoreCount: number
  goodStoreCount: number
  reviewQualityScore: number | null
}

const weeklyMemoLoading = ref(false)
const weeklyMemo = reactive<WeeklyMemo>({
  weekStart: '',
  weekEnd: '',
  headline: '',
  keyChanges: [],
  completedActions: [],
  unresolvedRisks: [],
  nextWeekFocus: [],
  storeHealthHighlights: [],
  reusableKnowledgeHints: [],
  riskStoreCount: 0,
  watchStoreCount: 0,
  goodStoreCount: 0,
  reviewQualityScore: null,
})

interface EffectSummary {
  evaluatedTaskCount: number
  goodEffectCount: number
  watchEffectCount: number
  noImprovementCount: number
  averageEffectScore: number
  reopenCandidates: Array<{
    taskId: number
    title: string
    taskType: string
    deptName: string
    archiveTime: string
    reopenCount: number
  }>
}

const effectSummaryLoading = ref(false)
const effectSummary = reactive<EffectSummary>({
  evaluatedTaskCount: 0,
  goodEffectCount: 0,
  watchEffectCount: 0,
  noImprovementCount: 0,
  averageEffectScore: 0,
  reopenCandidates: [],
})

const isNegativeCashflow = computed(() => {
  return Number(dailyReview.netCashflowAmount || 0) < 0
})

const stats = reactive({
  users: 1568,
  roles: 12,
  depts: 28,
  posts: 45,
  dicts: 156,
  configs: 89,
  notices: 36,
})

const dashboardHealth = reactive<DashboardHealth>({
  cpuUsage: 0,
  memoryUsage: 0,
  diskUsage: 0,
  availableProcessors: 0,
  totalMemoryGb: 0,
  hostName: '',
  osName: '',
  overallScore: 0,
  level: 'WARN',
  serviceCount: 0,
  upServiceCount: 0,
  downServiceCount: 0,
  updateTime: 0,
  services: [],
})

const palette = {
  blue: '#2563eb',
  green: '#0ea573',
  gold: '#d4940a',
  red: '#d4456a',
  cyan: '#0891b2',
  violet: '#6c63d9',
}

const welcomeText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了，注意休息'
  if (hour < 9) return '早上好，欢迎回来'
  if (hour < 12) return '上午好，欢迎回来'
  if (hour < 14) return '中午好，别忘了休息'
  if (hour < 18) return '下午好，工作愉快'
  if (hour < 22) return '晚上好，辛苦了'
  return '夜深了，注意休息'
})

const isAdmin = computed(() => {
  return userStore.roles.includes('admin') || userStore.permissions.includes('*:*:*')
})

const totalAssets = computed(() => {
  return number(stats.users + stats.roles + stats.depts + stats.posts + stats.dicts + stats.configs + stats.notices)
})

const primaryCards = computed(() => [
  panelCard('users', '用户总数', number(stats.users), `${stats.roles} 个角色参与授权`, palette.blue),
  panelCard('org', '组织节点', number(stats.depts + stats.posts), `${stats.depts} 部门 / ${stats.posts} 岗位`, palette.green),
  panelCard('services', '在线服务', `${dashboardHealth.upServiceCount}/${dashboardHealth.serviceCount}`, `${dashboardHealth.downServiceCount} 个异常服务`, dashboardHealth.downServiceCount > 0 ? palette.red : palette.cyan),
  panelCard('health', '健康评分', `${dashboardHealth.overallScore || '--'}`, `${dashboardHealth.availableProcessors || 0} 核 / ${dashboardHealth.totalMemoryGb || 0}GB`, palette.violet),
])

const healthItems = computed(() => [
  { key: 'cpu', label: 'CPU 使用率', value: normalizePercent(dashboardHealth.cpuUsage), color: palette.blue },
  { key: 'memory', label: '内存使用率', value: normalizePercent(dashboardHealth.memoryUsage), color: palette.gold },
  { key: 'disk', label: '磁盘使用率', value: normalizePercent(dashboardHealth.diskUsage), color: palette.green },
])

const healthLevelClass = computed(() => {
  return {
    'is-excellent': dashboardHealth.level === 'EXCELLENT',
    'is-good': dashboardHealth.level === 'GOOD',
    'is-warn': dashboardHealth.level === 'WARN',
  }
})

const healthStatusText = computed(() => {
  if (dashboardHealth.downServiceCount > 0) return '存在异常服务'
  if (dashboardHealth.level === 'EXCELLENT') return '系统运行良好'
  if (dashboardHealth.level === 'GOOD') return '系统运行稳定'
  return '资源负载偏高'
})

const healthUpdateText = computed(() => {
  if (!dashboardHealth.updateTime) return '等待采集'
  return `更新 ${new Date(dashboardHealth.updateTime).toLocaleTimeString('zh-CN', { hour12: false })}`
})

const moduleCards = computed(() => [
  moduleCard('user', '用户管理', '账号、部门、角色维护', '/system/user', User, 'rgba(37,99,235,0.18)'),
  moduleCard('role', '角色权限', '菜单权限与数据范围', '/system/role', UserFilled, 'rgba(14,165,115,0.18)'),
  moduleCard('dept', '组织架构', '部门树与业务归属', '/system/dept', OfficeBuilding, 'rgba(8,145,178,0.18)'),
  moduleCard('dict', '字典数据', '默认值与枚举维护', '/system/dict', Document, 'rgba(212,148,10,0.2)'),
  moduleCard('menu', '菜单管理', '路由与按钮权限', '/system/menu', Menu, 'rgba(108,99,217,0.18)'),
  moduleCard('config', '参数配置', '系统开关与策略配置', '/system/config', Setting, 'rgba(212,69,106,0.18)'),
])

function panelCard(key: string, label: string, value: string, hint: string, color: string) {
  return { key, label, value, hint, color }
}

function moduleCard(key: string, title: string, desc: string, to: string, icon: any, glow: string) {
  return { key, title, desc, to, icon, glow }
}

function number(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function normalizePercent(value: number) {
  return Math.max(0, Math.min(100, Math.round(Number(value || 0))))
}

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    weekday: 'long',
  })
}

function getNoticeTagType(type: string): string {
  const typeMap: Record<string, string> = { '1': 'primary', '2': 'success', '3': 'warning' }
  return typeMap[type] || 'info'
}

function getNoticeTypeName(type: string): string {
  const typeMap: Record<string, string> = { '1': '通知', '2': '公告', '3': '资讯' }
  return typeMap[type] || '通知'
}

function fetchNotice() {
  listNotice({ pageNum: 1, pageSize: 5 })
    .then((res: any) => {
      noticeList.value = res.rows || []
    })
    .catch(() => {
      noticeList.value = []
    })
}

function fetchWorkbenchTasks() {
  getWorkbenchTasks()
    .then((res: any) => {
      workbenchTasks.value = res.data || []
    })
    .catch(() => {
      workbenchTasks.value = []
    })
}

function fetchDailyReview() {
  dailyReviewLoading.value = true
  dailyReviewError.value = ''
  getDailyReviewBoard({})
    .then((res: any) => {
      const data = res.data || {}
      Object.assign(dailyReview, {
        reviewDate: data.reviewDate || '',
        deptId: data.deptId ?? null,
        deptName: data.deptName || '',
        salesAmount: Number(data.salesAmount || 0),
        cashInAmount: Number(data.cashInAmount || 0),
        expenseAmount: Number(data.expenseAmount || 0),
        netCashflowAmount: Number(data.netCashflowAmount || 0),
        pendingTaskCount: Number(data.pendingTaskCount || 0),
        highPriorityTaskCount: Number(data.highPriorityTaskCount || 0),
        focusItems: Array.isArray(data.focusItems) ? data.focusItems : [],
        suggestions: Array.isArray(data.suggestions) ? data.suggestions : [],
      })
    })
    .catch((err: any) => {
      dailyReviewError.value = err?.message || '复盘数据加载失败'
    })
    .finally(() => {
      dailyReviewLoading.value = false
    })
}

function fetchWeeklyBoard() {
  weeklyBoardLoading.value = true
  weeklyBoardError.value = ''
  getWeeklyReviewBoard({})
    .then((res: any) => {
      const data = res.data || {}
      Object.assign(weeklyBoard, {
        weekStart: data.weekStart || '',
        weekEnd: data.weekEnd || '',
        deptId: data.deptId ?? null,
        deptName: data.deptName || '',
        salesAmount: Number(data.salesAmount || 0),
        expenseAmount: Number(data.expenseAmount || 0),
        cashInAmount: Number(data.cashInAmount || 0),
        netCashflowAmount: Number(data.netCashflowAmount || 0),
        previousWeekSalesAmount: Number(data.previousWeekSalesAmount || 0),
        previousWeekExpenseAmount: Number(data.previousWeekExpenseAmount || 0),
        previousWeekNetCashflowAmount: Number(data.previousWeekNetCashflowAmount || 0),
        salesChangeRate: Number(data.salesChangeRate || 0),
        expenseChangeRate: Number(data.expenseChangeRate || 0),
        cashflowChangeRate: Number(data.cashflowChangeRate || 0),
        completedTaskCount: Number(data.completedTaskCount || 0),
        pendingTaskCount: Number(data.pendingTaskCount || 0),
        weeklySummary: data.weeklySummary || '',
        nextWeekFocus: data.nextWeekFocus || '',
      })
    })
    .catch((err: any) => {
      weeklyBoardError.value = err?.message || '周复盘数据加载失败'
    })
    .finally(() => {
      weeklyBoardLoading.value = false
    })
}

function fetchWeeklyMemo() {
  weeklyMemoLoading.value = true
  getWeeklyMemo({})
    .then((res: any) => {
      const data = res.data || {}
      Object.assign(weeklyMemo, {
        weekStart: data.weekStart || '',
        weekEnd: data.weekEnd || '',
        headline: data.headline || '',
        keyChanges: Array.isArray(data.keyChanges) ? data.keyChanges : [],
        completedActions: Array.isArray(data.completedActions) ? data.completedActions : [],
        unresolvedRisks: Array.isArray(data.unresolvedRisks) ? data.unresolvedRisks : [],
        nextWeekFocus: Array.isArray(data.nextWeekFocus) ? data.nextWeekFocus : [],
        storeHealthHighlights: Array.isArray(data.storeHealthHighlights) ? data.storeHealthHighlights : [],
        reusableKnowledgeHints: Array.isArray(data.reusableKnowledgeHints) ? data.reusableKnowledgeHints : [],
        riskStoreCount: Number(data.riskStoreCount || 0),
        watchStoreCount: Number(data.watchStoreCount || 0),
        goodStoreCount: Number(data.goodStoreCount || 0),
        reviewQualityScore: Number(data.reviewQualityScore || 0),
      })
    })
    .catch(() => {
      // silent fail for memo
    })
    .finally(() => {
      weeklyMemoLoading.value = false
    })
}

function fetchEffectSummary() {
  effectSummaryLoading.value = true
  getEffectSummary({ windowDays: 7 })
    .then((res: any) => {
      const data = res.data || {}
      Object.assign(effectSummary, {
        evaluatedTaskCount: Number(data.evaluatedTaskCount || 0),
        goodEffectCount: Number(data.goodEffectCount || 0),
        watchEffectCount: Number(data.watchEffectCount || 0),
        noImprovementCount: Number(data.noImprovementCount || 0),
        averageEffectScore: Number(data.averageEffectScore || 0),
        reopenCandidates: Array.isArray(data.reopenCandidates) ? data.reopenCandidates : [],
      })
    })
    .catch(() => {
      // silent fail
    })
    .finally(() => {
      effectSummaryLoading.value = false
    })
}

function formatRate(value: any): string {
  const num = Number(value || 0)
  const sign = num > 0 ? '+' : ''
  return sign + num.toFixed(1) + '%'
}

function rateClass(value: any): string {
  const num = Number(value || 0)
  if (num > 0) return 'rate-up'
  if (num < 0) return 'rate-down'
  return 'rate-flat'
}

function scoreClass(score: any): string {
  const num = Number(score || 0)
  if (num >= 80) return 'score-good'
  if (num >= 60) return 'score-watch'
  return 'score-risk'
}

function formatAmount(value: any): string {
  const num = Number(value || 0)
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function severityTagType(severity: string): string {
  const map: Record<string, string> = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[severity] || 'info'
}

const workbenchTabs = computed(() => {
  const sources = ['ALL', 'FINANCE', 'MEMBER', 'STOCK', 'SYSTEM']
  const labels: Record<string, string> = { ALL: '全部', FINANCE: '财务', MEMBER: '会员', STOCK: '库存', SYSTEM: '系统' }
  return sources.map(s => ({
    key: s,
    label: labels[s] || s,
    count: s === 'ALL' ? workbenchTasks.value.length : workbenchTasks.value.filter(t => t.sourceModule === s).length,
  }))
})

const filteredWorkbenchTasks = computed(() => {
  if (workbenchFilter.value === 'ALL') return workbenchTasks.value
  return workbenchTasks.value.filter(t => t.sourceModule === workbenchFilter.value)
})

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理', IN_PROGRESS: '处理中', DONE: '已完成', IGNORED: '已忽略',
    VIEW_ONLY: '查看', OPEN: '待处理', TODO: '待处理', PROCESSING: '处理中',
  }
  return map[status] || status
}

function statusTagType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning', IN_PROGRESS: 'primary', DONE: 'success', IGNORED: 'info',
    VIEW_ONLY: 'info', OPEN: 'warning', TODO: 'warning', PROCESSING: 'primary',
  }
  return map[status] || 'info'
}

function resolveTaskRoute(task: any): string {
  if (task.bizId && task.bizId.startsWith('FINANCE:')) {
    const taskId = task.bizId.split(':')[1]
    const base = task.targetRoute || '/finance/reviewTask'
    return base + (base.includes('?') ? '&' : '?') + 'taskId=' + taskId
  }
  return task.targetRoute || '/'
}

function fetchDashboardHealth() {
  healthLoading.value = true
  getDashboardHealth()
    .then((res: any) => {
      const data = res.data || res
      Object.assign(dashboardHealth, {
        ...data,
        services: data.services || [],
      })
    })
    .catch(() => {
      Object.assign(dashboardHealth, {
        level: 'WARN',
        overallScore: 0,
        serviceCount: 0,
        upServiceCount: 0,
        downServiceCount: 0,
        services: [],
        updateTime: Date.now(),
      })
    })
    .finally(() => {
      healthLoading.value = false
    })
}

function renderOrgChart() {
  if (!orgChartRef.value) return
  if (!orgChart) orgChart = echarts.init(orgChartRef.value)
  orgChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: 'rgba(37,99,235,0.16)',
      textStyle: { color: '#303133', fontSize: 12 },
    },
    grid: { left: 42, right: 22, top: 24, bottom: 28 },
    xAxis: {
      type: 'category',
      data: ['用户', '角色', '部门', '岗位'],
      axisLine: { lineStyle: { color: 'rgba(37,99,235,0.12)' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(37,99,235,0.07)', type: 'dashed' } },
    },
    series: [{
      type: 'bar',
      data: [stats.users, stats.roles, stats.depts, stats.posts],
      barWidth: 26,
      itemStyle: {
        borderRadius: [5, 5, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: palette.blue },
          { offset: 1, color: 'rgba(37,99,235,0.16)' },
        ]),
      },
    }],
  })
}

function renderAssetChart() {
  if (!assetChartRef.value) return
  if (!assetChart) assetChart = echarts.init(assetChartRef.value)
  assetChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: 'rgba(37,99,235,0.16)',
      textStyle: { color: '#303133', fontSize: 12 },
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#64748b', fontSize: 12 },
    },
    series: [{
      type: 'pie',
      radius: ['48%', '70%'],
      center: ['50%', '43%'],
      avoidLabelOverlap: true,
      label: { color: '#475569', fontSize: 12 },
      itemStyle: {
        borderColor: '#fff',
        borderWidth: 3,
        borderRadius: 6,
      },
      data: [
        { name: '字典', value: stats.dicts, itemStyle: { color: palette.blue } },
        { name: '参数', value: stats.configs, itemStyle: { color: palette.green } },
        { name: '通知', value: stats.notices, itemStyle: { color: palette.gold } },
      ],
    }],
  })
}

function handleResize() {
  orgChart?.resize()
  assetChart?.resize()
}

onMounted(async () => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
  if (isAdmin.value) {
    fetchNotice()
    fetchDashboardHealth()
    fetchWorkbenchTasks()
    fetchDailyReview()
    fetchWeeklyBoard()
    fetchWeeklyMemo()
    fetchEffectSummary()
    healthTimer = window.setInterval(fetchDashboardHealth, 30000)
  }
  await nextTick()
  if (isAdmin.value) {
    renderOrgChart()
    renderAssetChart()
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  if (healthTimer) {
    clearInterval(healthTimer)
    healthTimer = null
  }
  window.removeEventListener('resize', handleResize)
  orgChart?.dispose()
  assetChart?.dispose()
})
</script>

<style scoped>
.admin-dashboard {
  position: relative;
  min-height: calc(100vh - 84px);
  padding: 18px;
  overflow: hidden;
  color: #0f172a;
  background:
    radial-gradient(circle at 12% 10%, rgba(var(--theme-primary-rgb, 37, 99, 235), 0.12), transparent 28%),
    radial-gradient(circle at 92% 8%, rgba(14, 165, 115, 0.12), transparent 24%),
    linear-gradient(135deg, #f8fafc 0%, #eef4fb 48%, #f6f9fc 100%);
}

.bg-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.045) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.75), transparent 78%);
}

.dashboard-header,
.hero-panel,
.visual-grid {
  position: relative;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dashboard-kicker,
.panel-label {
  color: var(--theme-primary, #2563eb);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}

.dashboard-title {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 700;
}

.dashboard-date,
.refresh-time,
.service-summary {
  color: #64748b;
  font-size: 13px;
}

.header-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(var(--theme-primary-rgb, 37, 99, 235), 0.18);
  border-radius: 8px;
  color: #1e293b;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0ea573;
  box-shadow: 0 0 0 5px rgba(14, 165, 115, 0.12);
}

.is-warn .status-dot,
.header-status.is-warn .status-dot,
.service-summary.is-warn {
  color: #d4940a;
  background: #d4940a;
  box-shadow: 0 0 0 5px rgba(212, 148, 10, 0.14);
}

.hero-panel {
  display: grid;
  grid-template-columns: 1.05fr 2fr;
  gap: 14px;
  margin-bottom: 14px;
}

.hero-main,
.stat-card,
.chart-card {
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.07);
  backdrop-filter: blur(14px);
}

.hero-main {
  position: relative;
  min-height: 154px;
  padding: 22px;
  overflow: hidden;
}

.hero-main::after {
  position: absolute;
  right: -40px;
  bottom: -58px;
  width: 180px;
  height: 180px;
  content: '';
  border: 28px solid rgba(var(--theme-primary-rgb, 37, 99, 235), 0.08);
  border-radius: 50%;
}

.hero-value {
  margin-top: 12px;
  color: #0f172a;
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  color: #64748b;
  font-size: 13px;
}

.hero-meta strong {
  color: #0ea573;
}

.hero-line {
  position: absolute;
  right: 22px;
  bottom: 20px;
  left: 22px;
  height: 3px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
}

.hero-line::before {
  display: block;
  width: 68%;
  height: 100%;
  content: '';
  border-radius: inherit;
  background: linear-gradient(90deg, var(--theme-primary, #2563eb), #0ea573);
}

.hero-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  min-width: 0;
  padding: 18px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover,
.matrix-card:hover,
.service-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.1);
}

.stat-label {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.stat-value {
  display: block;
  margin-top: 10px;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.15;
  overflow-wrap: anywhere;
}

.stat-card em {
  display: block;
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}

.visual-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.9fr);
  grid-auto-rows: minmax(294px, auto);
  gap: 14px;
}

.chart-card {
  min-width: 0;
}

.service-panel,
.matrix-panel {
  grid-column: span 2;
}

.chart-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 0;
}

.chart-header h3 {
  margin: 4px 0 0;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.more-link {
  color: var(--theme-primary, #2563eb);
  font-size: 13px;
  text-decoration: none;
}

.chart-body {
  height: 296px;
  padding: 8px 10px 14px;
}

.asset-chart {
  height: 300px;
}

.health-grid {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.health-score {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border: 1px solid rgba(37, 99, 235, 0.12);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(14, 165, 115, 0.06));
}

.health-score strong {
  display: block;
  color: #0f172a;
  font-size: 30px;
  line-height: 1;
}

.health-score span,
.health-score em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.health-copy {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.health-copy strong {
  color: #334155;
  font-size: 13px;
}

.health-copy span {
  color: #64748b;
  font-size: 12px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 18px 20px 20px;
}

.service-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95), rgba(255, 255, 255, 0.78));
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.service-card.is-down {
  border-color: rgba(212, 69, 106, 0.26);
  background: linear-gradient(135deg, rgba(212, 69, 106, 0.08), rgba(255, 255, 255, 0.88));
}

.service-topline,
.service-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.service-topline strong {
  flex: 1;
  color: #1e293b;
  font-size: 14px;
}

.service-topline em {
  color: #0ea573;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.service-card.is-down .service-topline em {
  color: #d4456a;
}

.service-light {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #0ea573;
  box-shadow: 0 0 0 5px rgba(14, 165, 115, 0.12);
}

.service-card.is-down .service-light {
  background: #d4456a;
  box-shadow: 0 0 0 5px rgba(212, 69, 106, 0.12);
}

.service-meta {
  justify-content: space-between;
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
}

.service-card p {
  margin: 10px 0 0;
  overflow: hidden;
  color: #94a3b8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-list {
  display: grid;
  gap: 12px;
  padding: 18px 20px 20px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.8);
}

.notice-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.notice-copy strong {
  overflow: hidden;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-copy span {
  color: #94a3b8;
  font-size: 12px;
}

.workbench-list {
  display: grid;
  gap: 12px;
  padding: 0 20px 20px;
}

.workbench-tabs {
  display: flex;
  gap: 4px;
  padding: 12px 20px 0;
  flex-wrap: wrap;
}

.workbench-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 6px;
  background: rgba(248, 250, 252, 0.6);
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.workbench-tab:hover {
  border-color: rgba(37, 99, 235, 0.3);
  color: #2563eb;
}

.workbench-tab.active {
  border-color: var(--theme-primary, #2563eb);
  background: rgba(var(--theme-primary-rgb, 37, 99, 235), 0.08);
  color: var(--theme-primary, #2563eb);
  font-weight: 600;
}

.workbench-tab em {
  font-style: normal;
  font-size: 11px;
  padding: 0 5px;
  border-radius: 8px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

.impact-amount {
  color: #d4456a;
  font-weight: 600;
}

.workbench-item {
  padding: 12px 14px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.8);
}

.workbench-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.workbench-top strong {
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
}

.workbench-reason {
  margin: 8px 0 6px;
  color: #64748b;
  font-size: 12px;
}

.workbench-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #94a3b8;
  font-size: 12px;
}

.metric-matrix {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 18px 20px 20px;
}

.matrix-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  color: inherit;
  text-decoration: none;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95), rgba(255, 255, 255, 0.78));
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.matrix-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 8px;
  color: var(--theme-primary, #2563eb);
  background: rgba(var(--theme-primary-rgb, 37, 99, 235), 0.08);
  box-shadow: 0 0 14px var(--glow, rgba(37, 99, 235, 0.18));
}

.matrix-card strong {
  display: block;
  color: #1e293b;
  font-size: 14px;
  font-weight: 800;
}

.matrix-card span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

/* ── R8-B: 今日复盘区块 ── */
.daily-review-panel {
  grid-column: span 2;
}

.daily-review-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px 20px 20px;
}

.daily-review-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.dr-kpi {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95), rgba(255, 255, 255, 0.78));
}

.dr-kpi.negative {
  border-color: rgba(212, 69, 106, 0.26);
  background: linear-gradient(135deg, rgba(212, 69, 106, 0.08), rgba(255, 255, 255, 0.88));
}

.dr-label {
  color: #64748b;
  font-size: 12px;
}

.dr-kpi strong {
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.15;
}

.dr-kpi.negative strong {
  color: #d4456a;
}

.daily-review-focus {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.focus-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 6px;
  background: rgba(248, 250, 252, 0.8);
}

.focus-item strong {
  flex: 1;
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
}

.daily-review-suggestions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.suggestion-text {
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
  padding-left: 12px;
  border-left: 2px solid rgba(37, 99, 235, 0.2);
}

.dr-error {
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(212, 69, 106, 0.08);
  color: #d4456a;
  font-size: 12px;
}

/* ── R9-A: 周复盘看板 ── */
.weekly-board-panel {
  grid-column: span 2;
}

.weekly-board-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px 20px 20px;
}

.wb-task-summary {
  display: flex;
  gap: 6px;
}

.wb-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.wb-metric {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95), rgba(255, 255, 255, 0.78));
}

.wb-metric.negative {
  border-color: rgba(212, 69, 106, 0.26);
  background: linear-gradient(135deg, rgba(212, 69, 106, 0.08), rgba(255, 255, 255, 0.88));
}

.wb-label {
  color: #64748b;
  font-size: 12px;
}

.wb-metric strong {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.15;
}

.wb-metric.negative strong {
  color: #d4456a;
}

.wb-metric em {
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
}

.rate-up {
  color: #0ea573;
}

.rate-down {
  color: #d4456a;
}

.rate-flat {
  color: #94a3b8;
}

.wb-summary,
.wb-focus {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wb-summary p,
.wb-focus p {
  margin: 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
  padding-left: 12px;
  border-left: 2px solid rgba(37, 99, 235, 0.2);
}

@media (max-width: 1200px) {
  .hero-panel,
  .visual-grid {
    grid-template-columns: 1fr;
  }

  .hero-kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .service-panel,
  .matrix-panel,
  .daily-review-panel,
  .weekly-board-panel,
  .effect-summary-panel {
    grid-column: span 1;
  }

  .service-grid,
  .daily-review-kpis,
  .wb-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .admin-dashboard {
    padding: 12px;
  }

  .dashboard-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .dashboard-title {
    font-size: 20px;
  }

  .hero-kpis,
  .metric-matrix,
  .service-grid,
  .daily-review-kpis,
  .wb-metrics-grid,
  .effect-kpis {
    grid-template-columns: 1fr;
  }

  .hero-main {
    min-height: 140px;
    padding: 18px;
  }

  .hero-value {
    font-size: 28px;
  }

  .chart-body,
  .asset-chart {
    height: 260px;
  }
}

/* ── R10-F/R11-G: 周经营纪要 ── */
.weekly-memo-panel {
  grid-column: span 2;
}

.weekly-memo-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px 20px 20px;
}

.memo-headline {
  margin: 0;
  color: #1e293b;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
}

.memo-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.memo-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.memo-item {
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
  padding-left: 10px;
  border-left: 2px solid rgba(37, 99, 235, 0.15);
}

.memo-risk {
  color: #d4456a;
  border-left-color: rgba(212, 69, 106, 0.3);
}

.memo-knowledge {
  color: #0ea573;
  border-left-color: rgba(14, 165, 115, 0.3);
}

.memo-health-dist {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.memo-health-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.memo-health-tag.good {
  color: #0ea573;
  background: rgba(14, 165, 115, 0.1);
}

.memo-health-tag.watch {
  color: #d97706;
  background: rgba(217, 119, 6, 0.1);
}

.memo-health-tag.risk {
  color: #d4456a;
  background: rgba(212, 69, 106, 0.1);
}

/* R10-FIX-F: 复盘质量分 */
.memo-score-section {
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.memo-score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 28px;
  padding: 0 10px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 700;
}

.memo-score.score-good {
  color: #0ea573;
  background: rgba(14, 165, 115, 0.12);
}

.memo-score.score-watch {
  color: #d97706;
  background: rgba(217, 119, 6, 0.12);
}

.memo-score.score-risk {
  color: #d4456a;
  background: rgba(212, 69, 106, 0.12);
}

/* ── R12-G: 动作成效面板 ── */
.effect-summary-panel {
  grid-column: span 2;
}

.effect-summary-body {
  padding: 18px 20px 20px;
}

.effect-kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.effect-kpi {
  text-align: center;
  padding: 8px 4px;
  background: rgba(37, 99, 235, 0.04);
  border-radius: 6px;
}

.effect-kpi strong {
  display: block;
  font-size: 20px;
  margin-top: 2px;
}

.effect-label {
  font-size: 12px;
  color: #64748b;
}

.effect-reopen {
  margin-top: 8px;
}

.effect-reopen-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid rgba(37, 99, 235, 0.06);
}

.effect-reopen-item:last-child {
  border-bottom: none;
}

.effect-dept {
  margin-left: auto;
  font-size: 12px;
  color: #94a3b8;
}
</style>
