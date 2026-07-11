<template>
  <div class="app-container overview-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">会员管理概览</h2>
        <p>聚焦会员增长、活跃、积分消耗、活动履约和经营贡献，先用真实数据判断会员运营是否健康。</p>
      </div>
      <el-button type="primary" icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <div class="store-filter-panel">
      <el-form label-position="top" class="store-filter-form">
        <el-form-item label="门店">
          <el-select
            v-model="selectedDeptIds"
            placeholder="请选择门店"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            class="store-select"
            @change="handleDeptChange"
          >
            <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
          </el-select>
        </el-form-item>
      </el-form>
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
        <span>会员总数</span>
        <strong>{{ stats.totalMembers ?? 0 }}</strong>
        <p>今日新增 {{ stats.todayMembers ?? 0 }}，近 30 天活跃 {{ stats.activeMembers ?? 0 }}</p>
      </div>
      <div class="metric-card success">
        <span>会员销售</span>
        <strong>{{ money(stats.totalSale) }}</strong>
        <p>今日销售 {{ money(stats.todaySale) }}</p>
      </div>
      <div class="metric-card warning">
        <span>积分发放</span>
        <strong>{{ number(stats.totalPointsIssued) }}</strong>
        <p>已使用 {{ number(stats.totalPointsUsed) }}，兑换 {{ stats.totalExchanges ?? 0 }} 笔</p>
      </div>
      <div class="metric-card danger">
        <span>待核销金额</span>
        <strong>{{ money(unverifiedTotal) }}</strong>
        <p>费用 {{ money(stats.unverifiedExpense) }}，借支 {{ money(stats.unverifiedAdvance) }}</p>
      </div>
    </div>

    <div class="metric-grid">
      <div class="metric-card" :class="opActiveRateClass">
        <span>30 天活跃率</span>
        <strong>{{ operation.activeRate30d ?? 0 }}%</strong>
        <p>活跃会员 {{ operation.activeMembers30d ?? 0 }} / {{ operation.totalMembers ?? 0 }}</p>
      </div>
      <div class="metric-card" :class="opPointsUseRateClass">
        <span>积分使用率</span>
        <strong>{{ operation.pointsUseRate ?? 0 }}%</strong>
        <p>已使用 {{ number(operation.pointsUsed) }} / 已发放 {{ number(operation.pointsIssued) }}</p>
      </div>
      <div class="metric-card" :class="{ danger: (operation.pendingRefundCount ?? 0) > 0 }">
        <span>待处理退款</span>
        <strong>{{ operation.pendingRefundCount ?? 0 }}</strong>
        <p>{{ (operation.pendingRefundCount ?? 0) > 0 ? '存在待处理退款单，请及时跟进' : '当前无待处理退款' }}</p>
      </div>
      <div class="metric-card success">
        <span>秒杀活动</span>
        <strong>{{ operation.seckillActiveCount ?? 0 }}</strong>
        <p>参与人数 {{ operation.seckillParticipantCount ?? 0 }}</p>
      </div>
    </div>

    <div class="metric-grid ops-grid">
      <div class="metric-card">
        <span>会员增长（30日）</span>
        <strong>{{ stats.newMemberCount30d ?? 0 }}</strong>
        <p>近 30 天新增会员数</p>
      </div>
      <div class="metric-card" :class="opActiveMemberClass">
        <span>30 日活跃</span>
        <strong>{{ stats.activeMemberCount30d ?? 0 }}</strong>
        <p>近 30 天有积分变动的会员</p>
      </div>
      <div class="metric-card" :class="opRepeatRateClass">
        <span>90 日复购率</span>
        <strong>{{ stats.repeatRate90d ?? 0 }}%</strong>
        <p>复购会员 {{ stats.repeatMemberCount90d ?? 0 }} 人</p>
      </div>
      <div class="metric-card warning">
        <span>积分负债</span>
        <strong>{{ money(stats.pointsLiability) }}</strong>
        <p>已兑换成本 {{ money(stats.pointsRedeemedCost) }}</p>
      </div>
      <div class="metric-card success">
        <span>活动贡献</span>
        <strong>{{ money(stats.activityContributionAmount) }}</strong>
        <p>{{ stats.activityRoiText || 'ROI 暂不可算 / 缺少活动成本' }}</p>
      </div>
      <div class="metric-card success">
        <span>会员销售贡献</span>
        <strong>{{ money(stats.memberSalesAmount) }}</strong>
        <p>销售订单 {{ stats.memberSalesOrderCount ?? 0 }} 单</p>
      </div>
    </div>

    <div v-if="opWarnings.length > 0" class="op-warnings">
      <el-alert
        v-for="(warn, idx) in opWarnings"
        :key="idx"
        :title="warn"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 8px"
      />
    </div>

    <div v-if="segmentRows.length > 0" class="segment-grid">
      <div
        v-for="row in segmentRows"
        :key="row.segmentType"
        class="metric-card clickable"
        :class="segmentClass(row.segmentType)"
        @click="goToSegment(row.segmentType)"
      >
        <span>{{ row.segmentName }}</span>
        <strong>{{ row.memberCount ?? 0 }}</strong>
        <p>占比 {{ row.ratio ?? 0 }}%</p>
      </div>
    </div>

    <el-card v-if="suggestions.length > 0" class="section-card suggestion-card">
      <template #header><span>经营建议</span></template>
      <el-alert
        v-for="(s, idx) in suggestions"
        :key="idx"
        :title="s.title"
        :type="suggestionAlertType(s.severity)"
        show-icon
        :closable="false"
        style="margin-bottom: 10px"
      >
        <div class="suggestion-body">
          <p>{{ s.reason }}</p>
          <p><strong>建议：</strong>{{ s.suggestion }}</p>
        </div>
      </el-alert>
    </el-card>

    <el-card v-if="memberActionItems.length > 0" class="section-card action-items-card">
      <template #header>
        <div class="card-head">
          <span>会员经营动作</span>
          <el-tag type="warning">{{ memberActionItems.length }} 项</el-tag>
        </div>
      </template>
      <div class="action-item-list">
        <div v-for="(item, idx) in memberActionItems" :key="idx" class="action-item" :class="item.level?.toLowerCase()">
          <div class="action-item-head">
            <el-tag :type="actionLevelTagType(item.level)" size="small">{{ item.level }}</el-tag>
            <strong>{{ item.title }}</strong>
          </div>
          <p class="action-reason">{{ item.reason }}</p>
          <p class="action-suggestion"><strong>建议：</strong>{{ item.suggestion }}</p>
          <div class="action-go">
            <router-link :to="item.targetRoute" class="action-link">去处理 &rarr;</router-link>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="section-card growth-action-card">
      <template #header>
        <div class="card-head">
          <span>增长动作</span>
          <el-tag v-if="growthAction.pressureFallbackUsed" type="warning" size="small">压力 fallback</el-tag>
        </div>
      </template>
      <div class="growth-action-body">
        <div class="growth-action-metric">
          <span>待执行动作</span>
          <strong>{{ growthAction.pendingActionCount ?? 0 }}</strong>
        </div>
        <div class="growth-action-metric">
          <span>待触达会员</span>
          <strong>{{ growthAction.pendingMemberCount ?? 0 }}</strong>
        </div>
        <div class="growth-action-metric">
          <span>有效率</span>
          <strong>{{ ratePercent(growthAction.effectRate) }}%</strong>
        </div>
        <div class="growth-action-metric">
          <span>优先分层</span>
          <strong class="growth-segment">{{ growthSegmentLabel(growthAction.topSegmentType) }}</strong>
        </div>
        <div class="growth-action-go">
          <router-link to="/member/growthAction" class="action-link">查看动作台 &rarr;</router-link>
        </div>
      </div>
    </el-card>

    <div class="content-grid">
      <el-card class="section-card">
        <template #header>
          <div class="card-head">
            <span>近 7 天会员趋势</span>
            <el-tag type="info">{{ trendRows.length }} 天</el-tag>
          </div>
        </template>
        <el-table :data="trendRows" stripe style="width: 100%" empty-text="暂无趋势数据" max-height="320">
          <el-table-column prop="date" label="日期" width="100" />
          <el-table-column prop="newMembers" label="新增会员" width="110" />
          <el-table-column prop="dailySale" label="销售额" min-width="120">
            <template #default="{ row }">{{ money(row.dailySale) }}</template>
          </el-table-column>
          <el-table-column prop="consumeAmount" label="积分消费金额" min-width="130">
            <template #default="{ row }">{{ money(row.consumeAmount) }}</template>
          </el-table-column>
          <el-table-column prop="pointsChange" label="积分变动" min-width="110" />
        </el-table>
      </el-card>

      <el-card class="section-card">
        <template #header><span>运营信号</span></template>
        <div class="signal-list">
          <div class="signal-item" :class="opActiveRateClass">
            <strong>会员活跃度</strong>
            <p>近 30 天活跃 {{ operation.activeMembers30d ?? 0 }} / 总会员 {{ operation.totalMembers ?? 0 }}，活跃率 {{ operation.activeRate30d ?? 0 }}%。</p>
          </div>
          <div class="signal-item" :class="{ danger: (operation.pendingRefundCount ?? 0) > 0 }">
            <strong>退款风险</strong>
            <p>{{ (operation.pendingRefundCount ?? 0) > 0 ? '当前有 ' + operation.pendingRefundCount + ' 笔待处理退款，请及时跟进。' : '当前无待处理退款。' }}</p>
          </div>
          <div class="signal-item" :class="opPointsUseRateClass">
            <strong>积分一致性</strong>
            <p>积分已发放 {{ number(operation.pointsIssued) }}，已使用 {{ number(operation.pointsUsed) }}，使用率 {{ operation.pointsUseRate ?? 0 }}%。兑换 {{ operation.exchangeCount ?? 0 }} 笔。</p>
          </div>
          <div class="signal-item">
            <strong>活动履约</strong>
            <p>秒杀活动 {{ operation.seckillActiveCount ?? 0 }} 个进行中，参与 {{ operation.seckillParticipantCount ?? 0 }} 人次。会员销售 {{ money(operation.memberSalesAmount) }}（{{ operation.memberSaleOrderCount ?? 0 }} 单，均单 {{ money(operation.avgMemberSaleAmount) }}）。</p>
          </div>
          <div class="signal-item" :class="{ danger: unverifiedTotal > 0 }">
            <strong>未核销风险</strong>
            <p>{{ unverifiedTotal > 0 ? '存在待核销费用或借支，需要进入财务链路处理。' : '当前未核销金额为 0。' }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <div class="content-grid">
      <el-card class="section-card">
        <template #header><span>高积分会员排行</span></template>
        <el-table :data="ranking" stripe style="width: 100%" empty-text="暂无排行数据" max-height="320">
          <el-table-column type="index" label="#" width="56" />
          <el-table-column prop="member_no" label="会员编号" min-width="130" show-overflow-tooltip />
          <el-table-column prop="member_name" label="会员姓名" min-width="120" show-overflow-tooltip />
          <el-table-column prop="balance" label="积分余额" min-width="110" />
        </el-table>
      </el-card>

      <el-card class="section-card">
        <template #header><span>常用运营入口</span></template>
        <div class="quick-grid">
          <router-link v-for="link in quickLinks" :key="link.to" :to="link.to" class="quick-link">
            <span>{{ link.group }}</span>
            <strong>{{ link.title }}</strong>
            <p>{{ link.desc }}</p>
          </router-link>
        </div>
      </el-card>
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardOperation, getDashboardRanking, getDashboardStats, getDashboardTrend } from '@/api/member/dashboard'
import { getPointsOperationSummary } from '@/api/member/pointsOperation'
import { getGrowthActionDashboard } from '@/api/member/growthAction'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const permissionDenied = ref(false)
const stats = ref<Record<string, any>>({})
const trend = ref<Record<string, any>>({})
const ranking = ref<Record<string, any>[]>([])
const operation = ref<Record<string, any>>({})
const pointsSummary = ref<Record<string, any>>({})
const growthAction = ref<Record<string, any>>({})

// store multi-select filter
const depts = computed(() =>
  (userStore.depts || []).map((dept: any) => ({
    id: dept.deptId,
    label: dept.deptName,
  }))
)
const selectedDeptIds = ref<number[]>([])

const quickLinks = [
  { group: '会员', title: '会员详情', desc: '查询会员档案、状态和积分概况。', to: '/member/member' },
  { group: '报表', title: '会员报表', desc: '查看会员增长、活跃和贡献。', to: '/member/report/member' },
  { group: '积分', title: '积分规则', desc: '维护积分获取、消耗和兑换规则。', to: '/member/pointsRule' },
  { group: '积分', title: '兑换记录', desc: '追踪积分兑换和履约状态。', to: '/member/pointsExchange' },
  { group: '活动', title: '秒杀活动', desc: '管理活动、时间窗和库存。', to: '/member/seckill' },
  { group: '活动', title: '秒杀记录', desc: '查看活动参与和领取记录。', to: '/member/seckill/record' },
  { group: '售后', title: '退款管理', desc: '处理退款申请和审核流转。', to: '/member/refund' },
  { group: '移动端', title: '小程序权限', desc: '配置小程序可访问能力。', to: '/member/mpPerm' },
]

const unverifiedTotal = computed(() => Number(stats.value.unverifiedExpense ?? 0) + Number(stats.value.unverifiedAdvance ?? 0))
const activeRate = computed(() => {
  const total = Number(stats.value.totalMembers ?? 0)
  if (total <= 0) return 0
  return Number(((Number(stats.value.activeMembers ?? 0) / total) * 100).toFixed(1))
})
const activeRateClass = computed(() => {
  if (activeRate.value >= 30) return 'success'
  if (activeRate.value >= 10) return 'warning'
  return 'danger'
})
const trendRows = computed(() => {
  const dates = trend.value.dates || []
  return dates.map((date: string, index: number) => ({
    date,
    newMembers: trend.value.newMembers?.[index] ?? 0,
    consumeAmount: trend.value.consumeAmounts?.[index] ?? 0,
    pointsChange: trend.value.pointsChanges?.[index] ?? 0,
    dailySale: trend.value.dailySale?.[index] ?? 0,
  }))
})

const opActiveRateClass = computed(() => {
  const rate = Number(operation.value.activeRate30d ?? 0)
  if (rate >= 30) return 'success'
  if (rate >= 10) return 'warning'
  return 'danger'
})

const opPointsUseRateClass = computed(() => {
  const rate = Number(operation.value.pointsUseRate ?? 0)
  if (rate >= 30) return 'success'
  if (rate >= 10) return 'warning'
  return 'danger'
})

const opActiveMemberClass = computed(() => {
  const count = Number(stats.value.activeMemberCount30d ?? 0)
  const total = Number(stats.value.totalMembers ?? 0)
  if (total <= 0) return 'danger'
  const rate = (count / total) * 100
  if (rate >= 30) return 'success'
  if (rate >= 10) return 'warning'
  return 'danger'
})

const opRepeatRateClass = computed(() => {
  const rate = Number(stats.value.repeatRate90d ?? 0)
  if (rate >= 30) return 'success'
  if (rate >= 10) return 'warning'
  return 'danger'
})

const opWarnings = computed(() => {
  const warnings = operation.value.operationWarnings
  return Array.isArray(warnings) ? warnings : []
})

const segmentRows = computed(() => {
  const rows = operation.value.segmentRows
  return Array.isArray(rows) ? rows : []
})

const suggestions = computed(() => {
  const list = operation.value.suggestions
  return Array.isArray(list) ? list : []
})

const memberActionItems = computed(() => {
  const items = stats.value.memberActionItems
  return Array.isArray(items) ? items : []
})

function segmentClass(type: string) {
  switch (type) {
    case 'NEW': return 'success'
    case 'ACTIVE': return 'success'
    case 'SILENT': return 'danger'
    case 'HIGH_VALUE': return 'warning'
    default: return ''
  }
}

function suggestionAlertType(severity: string) {
  switch (severity) {
    case 'HIGH': return 'error'
    case 'MEDIUM': return 'warning'
    case 'LOW': return 'info'
    default: return 'info'
  }
}

function actionLevelTagType(level: string) {
  switch (level) {
    case 'HIGH': return 'danger'
    case 'MEDIUM': return 'warning'
    case 'LOW': return 'info'
    default: return 'info'
  }
}

function money(value: any) {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return '¥0.00'
  return `¥${num.toFixed(2)}`
}

function number(value: any) {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return '0'
  return num.toLocaleString()
}

function ratePercent(value: any) {
  return Number(value ?? 0).toFixed(2)
}

function growthSegmentLabel(type: string) {
  const labels: Record<string, string> = {
    SLEEPING_HIGH_VALUE: '高价值沉睡',
    NEAR_LEVEL_UP: '临门升级',
    RECENT_ACTIVE_NO_REPEAT: '活跃未复购',
    PRESSURE_STORE_RECALL: '压力门店召回',
  }
  return labels[type || ''] || '暂无'
}

function handleDeptChange() {
  loadData()
}

function goToSegment(segmentType: string) {
  router.push({ path: '/member/segment', query: { segmentType } })
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  permissionDenied.value = false
  const deptIds = selectedDeptIds.value.length > 0 ? selectedDeptIds.value : undefined
  try {
    const [statsRes, trendRes, rankingRes, operationRes, pointsSummaryRes] = await Promise.all([
      getDashboardStats(deptIds),
      getDashboardTrend(deptIds),
      getDashboardRanking(deptIds),
      getDashboardOperation(deptIds),
      getPointsOperationSummary(deptIds),
    ])
    stats.value = statsRes.data || {}
    trend.value = trendRes.data || {}
    ranking.value = rankingRes.data || []
    operation.value = operationRes.data || {}
    pointsSummary.value = pointsSummaryRes.data || {}

    // R17: 增长动作轻量卡片，best-effort 加载，无权限或失败不影响概览主流程
    try {
      const growthRes = await getGrowthActionDashboard({})
      growthAction.value = growthRes.data || {}
    } catch {
      growthAction.value = {}
    }
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

onMounted(() => {
  // default-select all authorized stores
  selectedDeptIds.value = (userStore.depts || []).map((d: any) => d.deptId)
  loadData()
})
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
  padding: 12px 20px 4px;
  margin-bottom: 0;

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

.store-filter-panel {
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.store-filter-form {
  display: flex;
  align-items: end;
  gap: 18px;
}

.store-filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.store-filter-form :deep(.el-form-item__label) {
  justify-content: flex-start;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 600;
  line-height: 1.2;
}

.store-select {
  min-width: 320px;
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

.op-warnings {
  margin-bottom: 14px;
}

.ops-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.segment-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  }
}

.suggestion-card {
  margin-bottom: 14px;
}

.suggestion-body {
  p {
    margin: 4px 0;
    line-height: 1.6;
  }
}

.action-items-card {
  margin-bottom: 14px;
}

.growth-action-card {
  margin-bottom: 14px;
}

.growth-action-body {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  align-items: center;
}

.growth-action-metric {
  padding: 10px 14px;
  border: 1px solid #e5e9f2;
  border-radius: 6px;
  background: #fafbfc;

  span {
    display: block;
    color: #7a879c;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 6px;
  }

  strong {
    display: block;
    color: #18202f;
    font-size: 22px;
    line-height: 1.1;
  }

  .growth-segment {
    font-size: 15px;
  }
}

.growth-action-go {
  text-align: right;
}

@media (max-width: 900px) {
  .growth-action-body {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .growth-action-go {
    grid-column: 1 / -1;
    text-align: left;
  }
}

.action-item-list {
  display: grid;
  gap: 12px;
}

.action-item {
  padding: 14px 16px;
  border: 1px solid #e5e9f2;
  border-radius: 6px;
  border-left: 3px solid #909399;

  &.high { border-left-color: #f56c6c; }
  &.medium { border-left-color: #e6a23c; }
  &.low { border-left-color: #909399; }
}

.action-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  strong {
    color: #18202f;
    font-size: 14px;
  }
}

.action-reason,
.action-suggestion {
  margin: 4px 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.action-go {
  margin-top: 8px;
}

.action-link {
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.content-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
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
    font-size: 26px;
    line-height: 1.1;
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

.section-card {
  border-radius: 8px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.signal-list {
  display: grid;
  gap: 12px;
}

.signal-item {
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

  &.success {
    border-color: #b8e4cf;
    background: #f3fbf7;
  }

  &.warning {
    border-color: #f2d39a;
    background: #fffaf0;
  }

  &.danger {
    border-color: #f4b4ad;
    background: #fff7f6;
  }
}

.quick-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.quick-link {
  min-height: 92px;
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
  .segment-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head {
    flex-direction: column;
  }

  .metric-grid,
  .content-grid,
  .quick-grid,
  .segment-grid {
    grid-template-columns: 1fr;
  }

  .store-select {
    min-width: 100%;
  }
}
</style>
