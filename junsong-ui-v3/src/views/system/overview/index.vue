<template>
  <div class="app-container overview-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">系统管理概览</h2>
        <p>聚焦账号、组织、权限、菜单和服务健康，帮助管理员先发现治理风险，再进入对应页面处理。</p>
      </div>
      <el-button type="primary" icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <el-result v-if="permissionDenied" icon="warning" title="暂无权限" sub-title="暂无权限查看该概览数据，请联系管理员开通相应权限。">
      <template #extra>
        <el-button type="primary" @click="loadData">重试</el-button>
      </template>
    </el-result>

    <template v-else>
    <el-alert v-if="loadError" :title="loadError" type="error" show-icon closable style="margin-bottom: 14px" @close="loadError = ''" />

    <div class="metric-grid">
      <div class="metric-card">
        <span>系统用户</span>
        <strong>{{ stats.userCount ?? 0 }}</strong>
        <p>{{ stats.roleCount ?? 0 }} 个角色，{{ stats.deptCount ?? 0 }} 个部门/门店</p>
      </div>
      <div class="metric-card">
        <span>菜单权限</span>
        <strong>{{ stats.menuCount ?? 0 }}</strong>
        <p>菜单、路由、按钮权限需要保持同步</p>
      </div>
      <div class="metric-card">
        <span>通知与集成</span>
        <strong>{{ (Number(stats.noticeCount ?? 0) + Number(stats.webhookCount ?? 0)) }}</strong>
        <p>{{ stats.noticeCount ?? 0 }} 条通知，{{ stats.webhookCount ?? 0 }} 个 Webhook</p>
      </div>
      <div class="metric-card" :class="healthClass">
        <span>系统健康</span>
        <strong>{{ stats.overallScore ?? health.overallScore ?? '-' }}</strong>
        <p>{{ healthLevelText }}，{{ stats.upServiceCount ?? health.upServiceCount ?? 0 }}/{{ stats.serviceCount ?? health.serviceCount ?? 0 }} 服务在线</p>
      </div>
    </div>

    <div class="content-grid">
      <el-card class="section-card">
        <template #header>
          <div class="card-head">
            <span>服务健康</span>
            <el-tag :type="healthTagType">{{ healthLevelText }}</el-tag>
          </div>
        </template>
        <div class="health-bars">
          <div class="bar-row">
            <div><strong>CPU</strong><span>{{ formatPercent(health.cpuUsage) }}</span></div>
            <el-progress :percentage="normalizePercent(health.cpuUsage)" :stroke-width="10" />
          </div>
          <div class="bar-row">
            <div><strong>内存</strong><span>{{ formatPercent(health.memoryUsage) }}</span></div>
            <el-progress :percentage="normalizePercent(health.memoryUsage)" :stroke-width="10" />
          </div>
          <div class="bar-row">
            <div><strong>磁盘</strong><span>{{ formatPercent(health.diskUsage) }}</span></div>
            <el-progress :percentage="normalizePercent(health.diskUsage)" :stroke-width="10" />
          </div>
        </div>
        <el-table :data="health.services || []" stripe style="width: 100%; margin-top: 14px" empty-text="暂无服务健康数据" max-height="280">
          <el-table-column prop="name" label="服务" min-width="160" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="String(row.status).toUpperCase() === 'UP' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card class="section-card">
        <template #header><span>治理风险</span></template>
        <div class="risk-list">
          <div class="risk-item" :class="{ danger: Number(stats.downServiceCount ?? health.downServiceCount ?? 0) > 0 }">
            <strong>服务可用性</strong>
            <p>{{ Number(stats.downServiceCount ?? health.downServiceCount ?? 0) > 0 ? '存在离线服务，需要优先排查网关、注册中心和模块进程。' : '服务在线情况正常。' }}</p>
          </div>
          <div class="risk-item">
            <strong>租户初始化</strong>
            <p>新租户开通后需确认管理员账号、默认角色和基础菜单已正确初始化，避免登录后菜单缺失。</p>
          </div>
          <div class="risk-item">
            <strong>菜单路由健康</strong>
            <p>检查菜单组件路径是否存在、路由是否可加载、按钮权限码是否与后端一致，防止初始化用户菜单失败。</p>
          </div>
          <div class="risk-item">
            <strong>平台权限边界</strong>
            <p>新增平台运营类菜单默认只授权超级管理员，普通租户角色按需开通，不得批量放开经营决策权限。</p>
          </div>
          <div class="risk-item">
            <strong>组织数据边界</strong>
            <p>用户、部门、角色和数据范围是财务与会员报表授权门店的基础。</p>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="section-card governance-section">
      <template #header>
        <div class="card-head">
          <span>治理体检</span>
          <el-tag :type="governanceWarnings.length > 0 ? 'warning' : 'success'" size="small">
            {{ governanceWarnings.length > 0 ? governanceWarnings.length + ' 项待关注' : '状态良好' }}
          </el-tag>
        </div>
      </template>
      <div class="governance-grid">
        <div class="governance-card" :class="{ risk: Number(governance.emptyMenuCount ?? 0) > 0 }">
          <span>空组件菜单</span>
          <strong>{{ governance.emptyMenuCount ?? 0 }}</strong>
          <p>菜单类型C缺少组件路径</p>
          <router-link to="/system/menu" class="gov-link">检查菜单</router-link>
        </div>
        <div class="governance-card" :class="{ risk: Number(governance.disabledUserCount ?? 0) > 0 }">
          <span>停用用户</span>
          <strong>{{ governance.disabledUserCount ?? 0 }}</strong>
          <p>状态为停用的用户账号</p>
          <router-link to="/system/user" class="gov-link">查看用户</router-link>
        </div>
        <div class="governance-card" :class="{ risk: Number(governance.roleWithoutUserCount ?? 0) > 0 }">
          <span>空闲角色</span>
          <strong>{{ governance.roleWithoutUserCount ?? 0 }}</strong>
          <p>未分配给任何用户的角色</p>
          <router-link to="/system/role" class="gov-link">检查角色</router-link>
        </div>
        <div class="governance-card" :class="{ risk: Number(governance.menuWithoutRoleCount ?? 0) > 0 }">
          <span>未授权菜单</span>
          <strong>{{ governance.menuWithoutRoleCount ?? 0 }}</strong>
          <p>未关联角色的菜单/按钮</p>
          <router-link to="/system/role" class="gov-link">授权检查</router-link>
        </div>
        <div class="governance-card" :class="{ risk: Number(governance.recentLoginFailCount ?? 0) > 0 }">
          <span>登录失败(24h)</span>
          <strong>{{ governance.recentLoginFailCount ?? 0 }}</strong>
          <p>近24小时登录失败次数</p>
          <router-link to="/monitor/logininfor" class="gov-link">查看日志</router-link>
        </div>
      </div>
      <div v-if="governanceWarnings.length > 0" class="governance-warnings">
        <el-alert
          v-for="w in governanceWarnings"
          :key="w.key"
          :title="w.message"
          :type="w.level === 'danger' ? 'error' : w.level === 'warning' ? 'warning' : 'info'"
          show-icon
          :closable="false"
          style="margin-bottom: 8px"
        />
      </div>
    </el-card>

    <el-card class="section-card">
      <template #header><span>常用治理入口</span></template>
      <div class="quick-grid">
        <router-link v-for="link in quickLinks" :key="link.to" :to="link.to" class="quick-link">
          <span>{{ link.group }}</span>
          <strong>{{ link.title }}</strong>
          <p>{{ link.desc }}</p>
        </router-link>
      </div>
    </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDashboardHealth, getDashboardStats, getDashboardGovernance } from '@/api/system/dashboard'

const loading = ref(false)
const loadError = ref('')
const permissionDenied = ref(false)
const stats = ref<Record<string, any>>({})
const health = ref<Record<string, any>>({})
const governance = ref<Record<string, any>>({})

const governanceWarnings = computed(() => {
  return Array.isArray(governance.value.governanceWarnings) ? governance.value.governanceWarnings : []
})

const quickLinks = [
  { group: '租户', title: '租户管理', desc: '管理租户主体、管理员和开通状态。', to: '/system/tenant' },
  { group: '组织', title: '用户管理', desc: '维护账号、部门归属和角色。', to: '/system/user' },
  { group: '组织', title: '部门管理', desc: '维护组织、门店和数据边界。', to: '/system/dept' },
  { group: '权限', title: '角色管理', desc: '维护菜单权限和数据范围。', to: '/system/role' },
  { group: '权限', title: '菜单管理', desc: '检查菜单路由、组件和权限码。', to: '/system/menu' },
  { group: '消息', title: '通知中心', desc: '查看系统通知和阅读情况。', to: '/system/notification' },
  { group: '配置', title: '字典管理', desc: '维护业务状态和枚举项。', to: '/system/dict' },
  { group: '集成', title: 'Webhook', desc: '管理开放事件订阅与回调。', to: '/system/webhook' },
]

const healthLevel = computed(() => String(stats.value.healthLevel || health.value.level || '').toUpperCase())
const healthLevelText = computed(() => {
  if (healthLevel.value === 'EXCELLENT') return '优秀'
  if (healthLevel.value === 'GOOD') return '良好'
  if (healthLevel.value === 'WARN') return '需关注'
  if (healthLevel.value === 'RISK') return '有风险'
  return healthLevel.value || '暂无数据'
})
const healthTagType = computed(() => {
  if (healthLevel.value === 'EXCELLENT' || healthLevel.value === 'GOOD') return 'success'
  if (healthLevel.value === 'RISK') return 'danger'
  return 'warning'
})
const healthClass = computed(() => {
  if (healthLevel.value === 'EXCELLENT' || healthLevel.value === 'GOOD') return 'success'
  if (healthLevel.value === 'RISK') return 'danger'
  return 'warning'
})

function normalizePercent(value: any) {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return 0
  return Math.max(0, Math.min(100, Number(num.toFixed(2))))
}

function formatPercent(value: any) {
  return `${normalizePercent(value)}%`
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  permissionDenied.value = false
  try {
    const [statsRes, healthRes, govRes] = await Promise.all([
      getDashboardStats(),
      getDashboardHealth(),
      getDashboardGovernance()
    ])
    stats.value = statsRes.data || {}
    health.value = healthRes.data || {}
    governance.value = govRes.data || {}
  } catch (e: any) {
    if (e?.response?.status === 403 || e?.message?.includes('403')) {
      permissionDenied.value = true
    } else {
      loadError.value = e?.message || '加载概览数据失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.overview-page {
  min-height: calc(100vh - 84px);
  background: #f5f7fb;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  .page-title {
    margin: 0 0 6px;
    color: #18202f;
    font-size: 22px;
    font-weight: 700;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 13px;
  }
}

.metric-grid,
.content-grid,
.quick-grid {
  display: grid;
  gap: 14px;
}

.metric-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 14px;
}

.metric-card {
  min-height: 108px;
  padding: 18px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fff;

  span {
    color: #7a879c;
    font-size: 13px;
    font-weight: 600;
  }

  strong {
    display: block;
    margin: 10px 0 8px;
    color: #18202f;
    font-size: 28px;
    line-height: 1;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 12px;
    line-height: 1.5;
  }

  &.success strong { color: #239b63; }
  &.warning strong { color: #b7791f; }
  &.danger strong { color: #c24136; }
}

.content-grid {
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  margin-bottom: 14px;
}

.section-card {
  border-radius: 8px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.health-bars {
  display: grid;
  gap: 14px;
}

.bar-row > div {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  color: #445065;
  font-size: 13px;
}

.risk-list {
  display: grid;
  gap: 12px;
}

.risk-item {
  padding: 12px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #f8fafc;

  strong {
    display: block;
    margin-bottom: 6px;
    color: #24324b;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 13px;
    line-height: 1.6;
  }

  &.danger {
    border-color: #f4b4ad;
    background: #fff7f6;
  }
}

.governance-section {
  margin-bottom: 14px;
}

.governance-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.governance-card {
  padding: 14px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #f8fafc;

  span {
    color: #7a879c;
    font-size: 12px;
    font-weight: 600;
  }

  strong {
    display: block;
    margin: 8px 0 6px;
    color: #18202f;
    font-size: 24px;
    line-height: 1;
  }

  p {
    margin: 0 0 8px;
    color: #667085;
    font-size: 12px;
    line-height: 1.5;
  }

  .gov-link {
    color: #4080ff;
    font-size: 12px;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  &.risk {
    border-color: #f5c882;
    background: #fffaf2;

    strong {
      color: #b7791f;
    }
  }
}

.governance-warnings {
  margin-top: 4px;
}

.quick-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.quick-link {
  min-height: 96px;
  padding: 14px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  color: inherit;
  text-decoration: none;
  background: #fff;

  span {
    color: #7a879c;
    font-size: 12px;
  }

  strong {
    display: block;
    margin: 8px 0 6px;
    color: #24324b;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 12px;
    line-height: 1.5;
  }
}

@media (max-width: 1100px) {
  .metric-grid,
  .content-grid,
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .governance-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head {
    flex-direction: column;
  }

  .metric-grid,
  .content-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .governance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
