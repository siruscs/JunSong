<template>
  <div class="store-dashboard">
    <div class="bg-grid"></div>

    <header class="dashboard-header">
      <div class="header-copy">
        <span class="dashboard-kicker">WEB DATA VISUALIZATION</span>
        <h2 class="dashboard-title">门店运营数据中心</h2>
        <span class="dashboard-date">{{ currentDate }}</span>
      </div>
      <el-button class="refresh-btn" circle :loading="loading" @click="loadData">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </header>

    <section class="hero-panel">
      <div class="hero-main">
        <span class="panel-label">核算总览</span>
        <div class="hero-value">¥{{ money(currentPeriod.netProfit) }}</div>
        <div class="hero-meta">
          <span v-if="currentPeriod.status !== undefined">{{ periodStatusText }}</span>
          <strong v-if="currentPeriod.status !== undefined">盈亏平衡 {{ returnRate }}%</strong>
        </div>
        <div class="hero-line"></div>
      </div>
      <div class="hero-kpis">
        <div v-for="item in primaryCards" :key="item.key" class="stat-card">
          <span class="stat-label">{{ item.label }}</span>
          <strong class="stat-value" :style="{ color: item.color }">{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <div class="visual-grid">
      <section class="chart-card trend-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">近7日趋势</span>
            <h3>会员增长与收支走势</h3>
          </div>
        </div>
        <div ref="trendChartRef" class="chart-body trend-chart"></div>
      </section>

      <section class="chart-card side-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">运营健康度</span>
            <h3>盈亏平衡度</h3>
          </div>
        </div>
        <div ref="healthChartRef" class="health-chart"></div>
        <div class="health-summary">
          <strong>{{ returnRate }}%</strong>
          <span>缴款 / 成本</span>
        </div>
      </section>

      <section class="chart-card rank-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">TOP10</span>
            <h3>会员积分排行</h3>
          </div>
        </div>
        <div ref="rankChartRef" class="chart-body rank-chart"></div>
      </section>

      <section class="chart-card matrix-panel">
        <div class="chart-header">
          <div>
            <span class="panel-label">关键指标</span>
            <h3>运营数据矩阵</h3>
          </div>
        </div>
        <div class="metric-matrix">
          <div v-for="item in matrixCards" :key="item.key" class="matrix-card">
            <div class="stat-icon" :style="{ '--glow': item.glow }">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <div>
              <strong :style="{ color: item.color }">{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import {
  Coin,
  DocumentChecked,
  DocumentDelete,
  GoodsFilled,
  Refresh,
  User,
  UserFilled,
  Van,
  Wallet,
} from '@element-plus/icons-vue'
import { getDashboardRanking, getDashboardStats, getDashboardTrend } from '@/api/member/dashboard'
import { getCurrentAccountingPeriod } from '@/api/finance/accountingPeriod'
import { useUserStore } from '@/stores/user'

const loading = ref(false)
const userStore = useUserStore()
const trendChartRef = ref<HTMLElement>()
const rankChartRef = ref<HTMLElement>()
const healthChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let rankChart: echarts.ECharts | null = null
let healthChart: echarts.ECharts | null = null

const stats = reactive<Record<string, any>>({})
const currentPeriod = reactive<Record<string, any>>({})
const trend = reactive<Record<string, any[]>>({
  dates: [],
  newMembers: [],
  consumeAmounts: [],
  pointsChanges: [],
  dailyExpense: [],
  dailySale: [],
})
const ranking = ref<any[]>([])

const palette = {
  blue: '#2563eb',
  green: '#0ea573',
  gold: '#d4940a',
  red: '#d4456a',
  cyan: '#0891b2',
  slate: '#475569',
}

const currentDate = computed(() => {
  const d = new Date()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekdays[d.getDay()]}`
})

const returnRate = computed(() => {
  const salePayment = Number.parseFloat(currentPeriod.totalSalePayment) || 0
  const costTotal = (Number.parseFloat(currentPeriod.totalVerifiedExpense) || 0)
    + (Number.parseFloat(currentPeriod.totalPurchase) || 0)
    + (Number.parseFloat(currentPeriod.totalUnverifiedAdvance) || 0)
  if (!costTotal) return 0
  return Math.min(100, Math.max(0, Math.round((salePayment / costTotal) * 100)))
})

const periodStatusText = computed(() => {
  const status = currentPeriod.status
  if (status === 0) return '进行中'
  if (status === 2) return '已结转'
  return '未初始化'
})

const primaryCards = computed(() => {
  const period = currentPeriod
  const netProfit = Number(period.netProfit || 0)
  return [
    panelCard('salePayment', '销售缴款', `¥${money(period.totalSalePayment)}`, '当前周期累计', palette.blue),
    panelCard('expense', '已核销费用', `¥${money(period.totalVerifiedExpense)}`, '当前周期累计', palette.gold),
    panelCard('purchase', '进货款', `¥${money(period.totalPurchase)}`, '当前周期累计', palette.cyan),
    panelCard('advance', '借支未核销', `¥${money(period.totalUnverifiedAdvance)}`, '当前周期累计', palette.red),
    panelCard('profit', '净利润', `¥${money(period.netProfit)}`, netProfit >= 0 ? '盈利' : '亏损', netProfit >= 0 ? palette.green : palette.red),
  ]
})

const matrixCards = computed(() => [
  card('todayMembers', '今日新增会员', number(stats.todayMembers), UserFilled, palette.green, 'rgba(14,165,115,0.18)'),
  card('activeMembers', '近30日活跃会员', number(stats.activeMembers), User, palette.blue, 'rgba(37,99,235,0.18)'),
  card('totalPurchase', '累计进货', `¥${money(stats.totalPurchase)}`, Van, palette.cyan, 'rgba(8,145,178,0.18)'),
  card('totalExpense', '累计费用', `¥${money(stats.totalExpense)}`, DocumentDelete, palette.gold, 'rgba(212,148,10,0.2)'),
  card('unverifiedExpense', '未核销费用', `¥${money(stats.unverifiedExpense)}`, DocumentChecked, palette.red, 'rgba(212,69,106,0.18)'),
  card('unverifiedAdvance', '未核销预支', `¥${money(stats.unverifiedAdvance)}`, Wallet, palette.red, 'rgba(212,69,106,0.18)'),
  card('pointsIssued', '累计发放积分', number(stats.totalPointsIssued), Coin, palette.green, 'rgba(14,165,115,0.18)'),
  card('pointsUsed', '累计使用积分', number(stats.totalPointsUsed), GoodsFilled, palette.slate, 'rgba(71,85,105,0.16)'),
])

function card(key: string, label: string, value: string, icon: any, color: string, glow: string) {
  return { key, label, value, icon, color, glow }
}

function panelCard(key: string, label: string, value: string, hint: string, color: string) {
  return { key, label, value, hint, color }
}

function number(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function money(value: any) {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed.toFixed(2) : '0.00'
}

function assignReactive(target: Record<string, any>, source: Record<string, any>) {
  Object.keys(target).forEach((key) => {
    delete target[key]
  })
  Object.assign(target, source || {})
}

async function loadData() {
  loading.value = true
  try {
    const [statsRes, trendRes, rankRes, periodRes]: any[] = await Promise.all([
      getDashboardStats(),
      getDashboardTrend(),
      getDashboardRanking(),
      getCurrentAccountingPeriod(userStore.currentDeptId).catch(() => ({ code: 0, data: {} })),
    ])
    if (statsRes.code === 200) assignReactive(stats, statsRes.data || {})
    if (trendRes.code === 200) assignReactive(trend, trendRes.data || {})
    if (rankRes.code === 200) ranking.value = rankRes.data || []
    if (periodRes.code === 200) assignReactive(currentPeriod, periodRes.data || {})
  } finally {
    loading.value = false
    await nextTick()
    renderTrendChart()
    renderRankChart()
    renderHealthChart()
  }
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: 'rgba(37,99,235,0.16)',
      textStyle: { color: '#303133', fontSize: 12 },
    },
    legend: {
      data: ['新增会员', '销售金额', '支出金额'],
      top: 4,
      right: 16,
      icon: 'roundRect',
      itemWidth: 10,
      itemHeight: 6,
      textStyle: { fontSize: 12, color: '#64748b' },
    },
    grid: { left: 42, right: 42, top: 48, bottom: 28 },
    xAxis: {
      type: 'category',
      data: trend.dates || [],
      axisLine: { lineStyle: { color: 'rgba(37,99,235,0.12)' } },
      axisLabel: { fontSize: 11, color: '#64748b' },
      axisTick: { show: false },
    },
    yAxis: [
      {
        type: 'value',
        name: '人数',
        nameTextStyle: { color: '#64748b', fontSize: 11 },
        axisLabel: { fontSize: 11, color: '#64748b' },
        splitLine: { lineStyle: { color: 'rgba(37,99,235,0.07)', type: 'dashed' } },
      },
      {
        type: 'value',
        name: '金额(元)',
        nameTextStyle: { color: '#64748b', fontSize: 11 },
        axisLabel: { fontSize: 11, color: '#64748b' },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '新增会员',
        type: 'bar',
        data: trend.newMembers || [],
        barWidth: 18,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: palette.blue },
            { offset: 1, color: 'rgba(37,99,235,0.16)' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
      },
      {
        name: '销售金额',
        type: 'line',
        yAxisIndex: 1,
        data: trend.dailySale || [],
        smooth: true,
        showSymbol: true,
        symbolSize: 5,
        lineStyle: { color: palette.green, width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14,165,115,0.22)' },
            { offset: 1, color: 'rgba(14,165,115,0.01)' },
          ]),
        },
      },
      {
        name: '支出金额',
        type: 'line',
        yAxisIndex: 1,
        data: trend.dailyExpense || [],
        smooth: true,
        showSymbol: true,
        symbolSize: 5,
        lineStyle: { color: palette.red, width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(212,69,106,0.18)' },
            { offset: 1, color: 'rgba(212,69,106,0.01)' },
          ]),
        },
      },
    ],
  })
}

function renderRankChart() {
  if (!rankChartRef.value) return
  if (!rankChart) rankChart = echarts.init(rankChartRef.value)
  const list = [...(ranking.value || [])].reverse()
  rankChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: 'rgba(15,23,42,0.08)',
      textStyle: { color: '#303133', fontSize: 12 },
    },
    grid: { left: 86, right: 24, top: 12, bottom: 18 },
    xAxis: {
      type: 'value',
      axisLabel: { fontSize: 11, color: '#64748b' },
      splitLine: { lineStyle: { color: 'rgba(37,99,235,0.07)', type: 'dashed' } },
    },
    yAxis: {
      type: 'category',
      data: list.map((item) => item.memberName || item.memberNo || '-'),
      axisLabel: { fontSize: 11, width: 64, overflow: 'truncate', color: '#475569' },
      axisTick: { show: false },
    },
    series: [{
      type: 'bar',
      data: list.map((item) => item.balance || 0),
      barWidth: 14,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: 'rgba(37,99,235,0.45)' },
          { offset: 1, color: palette.blue },
        ]),
        borderRadius: [0, 4, 4, 0],
      },
      showBackground: true,
      backgroundStyle: {
        color: 'rgba(15,23,42,0.04)',
        borderRadius: [0, 4, 4, 0],
      },
    }],
  })
}

function renderHealthChart() {
  if (!healthChartRef.value) return
  if (!healthChart) healthChart = echarts.init(healthChartRef.value)
  healthChart.setOption({
    series: [
      {
        type: 'gauge',
        startAngle: 205,
        endAngle: -25,
        min: 0,
        max: 100,
        radius: '96%',
        center: ['50%', '58%'],
        progress: {
          show: true,
          roundCap: true,
          width: 14,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: palette.green },
              { offset: 1, color: palette.blue },
            ]),
          },
        },
        axisLine: {
          roundCap: true,
          lineStyle: {
            width: 14,
            color: [[1, 'rgba(37,99,235,0.1)']],
          },
        },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: { show: false },
        data: [{ value: returnRate.value }],
      },
    ],
  })
}

function handleResize() {
  trendChart?.resize()
  rankChart?.resize()
  healthChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  rankChart?.dispose()
  healthChart?.dispose()
})
</script>

<style scoped>
.store-dashboard {
  position: relative;
  min-height: calc(100vh - 84px);
  padding: 18px;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 10%, rgba(var(--theme-primary-rgb, 37, 99, 235), 0.12), transparent 28%),
    radial-gradient(circle at 90% 6%, rgba(14, 165, 115, 0.12), transparent 24%),
    linear-gradient(135deg, #f8fafc 0%, #eef4fb 48%, #f6f9fc 100%);
  color: #0f172a;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.045) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.75), transparent 78%);
  pointer-events: none;
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
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.dashboard-date {
  font-size: 13px;
  color: #64748b;
}

.refresh-btn {
  width: 38px;
  height: 38px;
  border-color: rgba(var(--theme-primary-rgb, 37, 99, 235), 0.22);
  color: var(--theme-primary, #2563eb);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
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
  border-radius: 50%;
  border: 28px solid rgba(var(--theme-primary-rgb, 37, 99, 235), 0.08);
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
  font-weight: 700;
}

.hero-line {
  position: absolute;
  left: 22px;
  right: 22px;
  bottom: 20px;
  height: 3px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
}

.hero-line::before {
  display: block;
  width: 64%;
  height: 100%;
  content: '';
  border-radius: inherit;
  background: linear-gradient(90deg, var(--theme-primary, #2563eb), #0ea573);
}

.hero-kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  min-width: 0;
  padding: 18px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
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
  margin-top: 8px;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  grid-template-columns: minmax(0, 1.55fr) minmax(300px, 0.9fr);
  grid-auto-rows: minmax(294px, auto);
  gap: 14px;
}

.trend-panel {
  min-width: 0;
}

.side-panel,
.rank-panel,
.matrix-panel {
  min-width: 0;
}

.matrix-panel {
  grid-column: span 2;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 0;
}

.chart-header h3 {
  margin: 4px 0 0;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.chart-body {
  height: 296px;
  padding: 8px 10px 14px;
}

.trend-chart {
  height: 326px;
}

.rank-chart {
  height: 300px;
}

.health-chart {
  height: 172px;
  margin: 8px 8px 0;
}

.health-summary {
  padding: 0 20px 20px;
  text-align: center;
}

.health-summary strong {
  display: block;
  color: var(--theme-primary, #2563eb);
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
}

.health-summary span {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.metric-matrix {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95), rgba(255, 255, 255, 0.78));
}

.stat-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 8px;
  color: var(--theme-primary, #2563eb);
  background: rgba(var(--theme-primary-rgb, 37, 99, 235), 0.08);
  border: 1px solid rgba(148, 163, 184, 0.1);
  box-shadow: 0 0 14px var(--glow, rgba(37, 99, 235, 0.18));
}

.matrix-card strong {
  display: block;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.matrix-card span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .hero-panel,
  .visual-grid {
    grid-template-columns: 1fr;
  }

  .hero-kpis,
  .metric-matrix {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .matrix-panel {
    grid-column: span 1;
  }
}

@media (max-width: 640px) {
  .store-dashboard {
    padding: 12px;
  }

  .dashboard-title {
    font-size: 20px;
  }

  .hero-main {
    min-height: 140px;
    padding: 18px;
  }

  .hero-value {
    font-size: 28px;
  }

  .hero-kpis,
  .metric-matrix {
    grid-template-columns: 1fr;
  }

  .chart-body,
  .trend-chart,
  .rank-chart {
    height: 260px;
  }
}
</style>
