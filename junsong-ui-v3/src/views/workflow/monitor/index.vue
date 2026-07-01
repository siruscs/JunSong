<template>
  <div class="workflow-monitor-page app-container" v-loading="loading">
    <div class="monitor-header">
      <span class="monitor-title">流程监控仪表盘</span>
      <div class="monitor-header__extra">
        <el-tag type="success" effect="plain" size="small">每 60 秒自动刷新</el-tag>
        <el-button :icon="Refresh" size="small" @click="loadAll">立即刷新</el-button>
      </div>
    </div>

    <section class="monitor-stats">
      <el-card v-for="card in statCards" :key="card.key" class="monitor-stat-card" shadow="hover">
        <div class="monitor-stat-card__label">{{ card.label }}</div>
        <div class="monitor-stat-card__value" :style="{ color: card.color }">{{ card.value }}</div>
        <div class="monitor-stat-card__hint">{{ card.hint }}</div>
      </el-card>
    </section>

    <el-card shadow="never" class="monitor-chart-card">
      <template #header>近 7 天流程发起趋势</template>
      <div ref="trendChartRef" class="monitor-chart" />
    </el-card>

    <el-row :gutter="16" class="monitor-chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="monitor-chart-card">
          <template #header>各流程类型在途实例数</template>
          <div ref="typeChartRef" class="monitor-chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="monitor-chart-card">
          <template #header>审批效率（按流程类型平均审批时长 / 分钟）</template>
          <div ref="efficiencyChartRef" class="monitor-chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { listWorkflowInstances, type WorkflowInstanceRow } from '@/api/workflow/instance'
import { listTodoWorkflowTasks, type WorkflowTodoTaskRow } from '@/api/workflow/task'

const loading = ref(false)
const instances = ref<WorkflowInstanceRow[]>([])
const todoTasks = ref<WorkflowTodoTaskRow[]>([])

const trendChartRef = ref<HTMLDivElement>()
const typeChartRef = ref<HTMLDivElement>()
const efficiencyChartRef = ref<HTMLDivElement>()

let trendChart: echarts.ECharts | null = null
let typeChart: echarts.ECharts | null = null
let efficiencyChart: echarts.ECharts | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

function isRunningInstance(row: WorkflowInstanceRow) {
  return row.running === true || !row.endTime
}

function toDateKey(value?: string | null) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function isToday(value?: string | null) {
  return toDateKey(value) === toDateKey(new Date().toISOString())
}

function last7Days(): string[] {
  const days: string[] = []
  const today = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    days.push(toDateKey(d.toISOString()))
  }
  return days
}

const runningInstances = computed(() => instances.value.filter(isRunningInstance))

const statCards = computed(() => {
  const todayNew = instances.value.filter((i) => isToday(i.startTime)).length
  const timeoutCount = todoTasks.value.filter(
    (t) => t.dueDate && new Date(t.dueDate).getTime() < Date.now(),
  ).length
  return [
    {
      key: 'running',
      label: '在途流程总数',
      value: runningInstances.value.length,
      hint: '尚未结束的流程实例',
      color: '#409eff',
    },
    {
      key: 'todayNew',
      label: '今日新增实例',
      value: todayNew,
      hint: '今日发起的流程实例',
      color: '#67c23a',
    },
    {
      key: 'todo',
      label: '待办任务总数',
      value: todoTasks.value.length,
      hint: '当前待处理审批任务',
      color: '#e6a23c',
    },
    {
      key: 'timeout',
      label: '超时任务数',
      value: timeoutCount,
      hint: '已超过到期时间的待办',
      color: '#f56c6c',
    },
  ]
})

function buildTrendOption() {
  const days = last7Days()
  const counts = days.map((day) => {
    return instances.value.filter((i) => toDateKey(i.startTime) === day).length
  })
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: days },
    yAxis: { type: 'value', minInterval: 1, name: '实例数' },
    series: [
      {
        name: '发起实例',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#409eff' },
        data: counts,
        label: { show: true, position: 'top' },
      },
    ],
  }
}

function buildTypeOption() {
  const map = new Map<string, number>()
  runningInstances.value.forEach((i) => {
    const name = i.processDefinitionName || i.processDefinitionKey || '未知流程'
    map.set(name, (map.get(name) || 0) + 1)
  })
  const data = Array.from(map.entries()).map(([name, value]) => ({ name, value }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 10, top: 'middle' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
        },
        data,
      },
    ],
  }
}

function buildEfficiencyOption() {
  const map = new Map<string, { total: number; count: number }>()
  instances.value
    .filter((i) => i.durationMs != null && i.endTime)
    .forEach((i) => {
      const name = i.processDefinitionName || i.processDefinitionKey || '未知流程'
      const cur = map.get(name) || { total: 0, count: 0 }
      cur.total += Number(i.durationMs) || 0
      cur.count += 1
      map.set(name, cur)
    })
  const entries = Array.from(map.entries()).map(([name, v]) => ({
    name,
    avgMinutes: v.count ? Math.round(v.total / v.count / 60000) : 0,
  }))
  entries.sort((a, b) => b.avgMinutes - a.avgMinutes)
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v: any) => `${v} 分钟` },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: entries.map((e) => e.name),
      axisLabel: { rotate: 30, interval: 0 },
    },
    yAxis: { type: 'value', name: '分钟' },
    series: [
      {
        type: 'bar',
        data: entries.map((e) => e.avgMinutes),
        itemStyle: { color: '#67c23a' },
        label: { show: true, position: 'top', formatter: '{c}分' },
      },
    ],
  }
}

function renderCharts() {
  if (trendChartRef.value) {
    trendChart?.dispose()
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption(buildTrendOption())
  }
  if (typeChartRef.value) {
    typeChart?.dispose()
    typeChart = echarts.init(typeChartRef.value)
    typeChart.setOption(buildTypeOption())
  }
  if (efficiencyChartRef.value) {
    efficiencyChart?.dispose()
    efficiencyChart = echarts.init(efficiencyChartRef.value)
    efficiencyChart.setOption(buildEfficiencyOption())
  }
}

function handleResize() {
  trendChart?.resize()
  typeChart?.resize()
  efficiencyChart?.resize()
}

async function loadAll() {
  loading.value = true
  try {
    const [instanceRes, todoRes]: any = await Promise.all([
      listWorkflowInstances(),
      listTodoWorkflowTasks(),
    ])
    instances.value = instanceRes.data || []
    todoTasks.value = todoRes.data || []
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(loadAll, 60 * 1000)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadAll()
  startAutoRefresh()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  typeChart?.dispose()
  efficiencyChart?.dispose()
})
</script>

<style scoped>
.workflow-monitor-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.monitor-title {
  font-size: 18px;
  font-weight: 600;
}

.monitor-header__extra {
  display: flex;
  align-items: center;
  gap: 12px;
}

.monitor-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.monitor-stat-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.monitor-stat-card__value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 600;
}

.monitor-stat-card__hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.monitor-chart-card {
  margin: 0;
}

.monitor-chart {
  height: 340px;
  width: 100%;
}

.monitor-chart-row {
  margin-top: 0;
}

@media (max-width: 1200px) {
  .monitor-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .monitor-stats {
    grid-template-columns: 1fr;
  }
}
</style>
