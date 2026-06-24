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
import { getDashboardHealth } from '@/api/system/dashboard'
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
const orgChartRef = ref<HTMLElement>()
const assetChartRef = ref<HTMLElement>()
const healthLoading = ref(false)
let orgChart: echarts.ECharts | null = null
let assetChart: echarts.ECharts | null = null
let timer: number | null = null
let healthTimer: number | null = null

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

@media (max-width: 1200px) {
  .hero-panel,
  .visual-grid {
    grid-template-columns: 1fr;
  }

  .hero-kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .service-panel,
  .matrix-panel {
    grid-column: span 1;
  }

  .service-grid {
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
  .service-grid {
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
</style>
