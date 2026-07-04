<template>
  <div class="app-container predictive-ops">
    <div class="page-head">
      <div>
        <h2>预测辅助 V2</h2>
        <p>可解释规则：基于 R16 现金流快照、R15 应收数据、R17 会员动作和库存健康生成预测，提供 what-if 模拟（只读，不写业务表）。</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
        <el-button type="primary" :icon="Camera" :loading="snapshotLoading" @click="handleSnapshot" v-hasPermi="['finance:predictiveOps:snapshot']">生成预测快照</el-button>
      </div>
    </div>

    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>基线现金压力</span>
          <el-tag :type="levelTag(dashboard.basePressureLevel)" size="small">
            {{ levelLabel(dashboard.basePressureLevel) }} {{ dashboard.basePressureScore || 0 }} 分
          </el-tag>
        </div>
      </template>
      <div class="baseline">
        <div class="baseline-score" :class="levelClass(dashboard.basePressureLevel)">
          {{ dashboard.basePressureScore || 0 }}
        </div>
        <div class="baseline-meta">
          <div>预测窗口：{{ dashboard.windowDays || 7 }} 天</div>
          <div>预测金额合计：&yen;{{ money(dashboard.totalForecastAmount) }}</div>
          <div class="muted">基线取现金流和应收预测分数的最大值；what-if 模拟可在此基础上叠加变化</div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="12" class="prediction-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="risk in predictionCards" :key="risk.key">
        <div class="risk-card">
          <div class="risk-header">
            <span class="risk-label">{{ risk.label }}</span>
            <el-tag :type="levelTag(risk.value?.level)" size="small">
              {{ levelLabel(risk.value?.level) }} {{ risk.value?.score || 0 }} 分
            </el-tag>
          </div>
          <div class="risk-score" :class="levelClass(risk.value?.level)">
            {{ risk.value?.score || 0 }}
          </div>
          <div class="risk-basis">{{ risk.value?.basis }}</div>
          <div class="risk-recommendation">
            <strong>建议：</strong>{{ risk.value?.recommendation || '无建议' }}
          </div>
          <div class="risk-factors" v-if="risk.value?.factors?.length">
            <el-tag v-for="factor in risk.value.factors" :key="factor.factorCode" size="small" effect="plain" class="factor-tag">
              {{ factor.factorName }} ({{ factor.factorValue }}) - {{ factor.factorWeight }} 分
            </el-tag>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card class="section-card">
      <template #header><span>what-if 模拟（只读，不修改任何业务表）</span></template>
      <el-form :inline="true" :model="simulationForm" class="what-if-form">
        <el-form-item label="门店/部门">
          <el-input-number v-model="simulationForm.deptId" :min="0" :step="1" controls-position="right" placeholder="留空为全量" />
        </el-form-item>
        <el-form-item label="窗口天数">
          <el-input-number v-model="simulationForm.windowDays" :min="1" :max="30" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="预计回款变化">
          <el-input-number v-model="simulationForm.expectedCollectionDelta" :step="1000" controls-position="right" />
        </el-form-item>
        <el-form-item label="预计费用变化">
          <el-input-number v-model="simulationForm.expectedExpenseDelta" :step="1000" controls-position="right" />
        </el-form-item>
        <el-form-item label="催收完成数">
          <el-input-number v-model="simulationForm.completedCollectionActions" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="会员动作完成数">
          <el-input-number v-model="simulationForm.completedMemberActions" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="库存补货调整">
          <el-input-number v-model="simulationForm.stockReplenishmentDelta" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="DataAnalysis" :loading="simulating" @click="handleWhatIf" v-hasPermi="['finance:predictiveOps:simulate']">运行模拟</el-button>
          <el-button @click="resetWhatIf">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-if="simulationResult" class="what-if-result">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="基线压力分">{{ simulationResult.basePressureScore }}</el-descriptions-item>
          <el-descriptions-item label="基线等级">
            <el-tag :type="levelTag(simulationResult.basePressureLevel)" size="small">{{ levelLabel(simulationResult.basePressureLevel) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模拟后压力分">{{ simulationResult.simulatedPressureScore }}</el-descriptions-item>
          <el-descriptions-item label="模拟后等级">
            <el-tag :type="levelTag(simulationResult.simulatedPressureLevel)" size="small">{{ levelLabel(simulationResult.simulatedPressureLevel) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="变化分">{{ simulationResult.deltaScore }}</el-descriptions-item>
          <el-descriptions-item label="变化方向">
            <el-tag :type="deltaTag(simulationResult.deltaLevel)" size="small">{{ deltaLabel(simulationResult.deltaLevel) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="what-if-bases">
          <p><strong>口径：</strong>{{ simulationResult.basis }}</p>
          <p><strong>建议：</strong>{{ simulationResult.recommendation }}</p>
        </div>

        <div class="what-if-factors" v-if="simulationResult.factors?.length">
          <el-tag v-for="factor in simulationResult.factors" :key="factor.factorCode" :type="factorTag(factor.factorWeight)" size="small" class="factor-tag" effect="plain">
            {{ factor.factorName }} ({{ factor.factorValue }}): {{ factor.factorWeight }} 分 - {{ factor.explanation }}
          </el-tag>
        </div>

        <div class="what-if-areas" v-if="simulationResult.impactAreas?.length">
          <strong>影响范围：</strong>
          <el-tag v-for="area in simulationResult.impactAreas" :key="area" size="small" type="info" class="area-tag">
            {{ area }}
          </el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, DataAnalysis, Refresh } from '@element-plus/icons-vue'
import {
  createPredictiveOpsSnapshot,
  getPredictiveOpsDashboard,
  simulatePredictiveOpsWhatIf,
} from '@/api/finance/predictiveOps'

const loading = ref(false)
const snapshotLoading = ref(false)
const simulating = ref(false)
const dashboard = ref<any>({})
const simulationResult = ref<any>(null)

const simulationForm = reactive({
  deptId: undefined as number | undefined,
  windowDays: 7,
  expectedCollectionDelta: 0,
  expectedExpenseDelta: 0,
  completedCollectionActions: 0,
  completedMemberActions: 0,
  stockReplenishmentDelta: 0,
})

const predictionCards = computed(() => [
  { key: 'cashflow', label: '现金流预测', value: dashboard.value.cashflow },
  { key: 'receivable', label: '应收兑现风险', value: dashboard.value.receivable },
  { key: 'memberAction', label: '会员动作转化', value: dashboard.value.memberAction },
  { key: 'stock', label: '库存风险', value: dashboard.value.stock },
])

function money(value: any) {
  return Number(value || 0).toFixed(2)
}

function levelLabel(level: string) {
  const labels: Record<string, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '严重',
    UNCHANGED: '不变',
  }
  return labels[level] || level || '-'
}

function levelTag(level: string) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'UNCHANGED') return 'info'
  return 'success'
}

function levelClass(level: string) {
  return 'score-' + (level || 'LOW').toLowerCase()
}

function deltaLabel(level: string) {
  if (level === 'DECREASE') return '压力下降'
  if (level === 'INCREASE') return '压力上升'
  return '基本不变'
}

function deltaTag(level: string) {
  if (level === 'DECREASE') return 'success'
  if (level === 'INCREASE') return 'danger'
  return 'info'
}

function factorTag(weight: number) {
  if (weight < 0) return 'success'
  if (weight > 30) return 'danger'
  if (weight > 0) return 'warning'
  return 'info'
}

function loadDashboard() {
  loading.value = true
  return getPredictiveOpsDashboard({})
    .then((res: any) => {
      dashboard.value = res.data || {}
    })
    .finally(() => {
      loading.value = false
    })
}

function handleSnapshot() {
  snapshotLoading.value = true
  createPredictiveOpsSnapshot({})
    .then((res: any) => {
      ElMessage.success('预测快照已生成：' + (res.data ?? 0) + ' 条')
      return loadDashboard()
    })
    .finally(() => {
      snapshotLoading.value = false
    })
}

function handleWhatIf() {
  simulating.value = true
  simulatePredictiveOpsWhatIf(simulationForm)
    .then((res: any) => {
      simulationResult.value = res.data || null
      ElMessage.success('what-if 模拟完成（仅模拟）')
    })
    .finally(() => {
      simulating.value = false
    })
}

function resetWhatIf() {
  simulationResult.value = null
  Object.assign(simulationForm, {
    deptId: undefined,
    windowDays: 7,
    expectedCollectionDelta: 0,
    expectedExpenseDelta: 0,
    completedCollectionActions: 0,
    completedMemberActions: 0,
    stockReplenishmentDelta: 0,
  })
}

onMounted(loadDashboard)
</script>

<style scoped>
.predictive-ops {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-head h2 {
  margin: 0 0 4px 0;
  font-size: 22px;
  color: #1f2d3d;
}

.page-head p {
  margin: 0;
  color: #606266;
}

.section-card {
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.baseline {
  display: flex;
  align-items: center;
  gap: 24px;
}

.baseline-score {
  font-size: 56px;
  font-weight: 700;
  color: #67c23a;
}

.baseline-score.score-medium {
  color: #e6a23c;
}

.baseline-score.score-high,
.baseline-score.score-critical {
  color: #f56c6c;
}

.baseline-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #303133;
}

.muted {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.prediction-row {
  margin-bottom: 16px;
}

.risk-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}

.risk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.risk-label {
  font-weight: 600;
  color: #1f2d3d;
}

.risk-score {
  font-size: 32px;
  font-weight: 700;
  color: #67c23a;
}

.risk-score.score-medium {
  color: #e6a23c;
}

.risk-score.score-high,
.risk-score.score-critical {
  color: #f56c6c;
}

.risk-basis {
  margin: 8px 0;
  font-size: 12px;
  color: #606266;
}

.risk-recommendation {
  font-size: 12px;
  color: #303133;
  margin-bottom: 8px;
}

.risk-factors,
.what-if-factors,
.what-if-areas {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.factor-tag,
.area-tag {
  margin: 2px;
}

.what-if-form {
  margin-bottom: 12px;
}

.what-if-result {
  border-top: 1px dashed #ebeef5;
  padding-top: 12px;
}

.what-if-bases {
  margin: 8px 0;
  color: #303133;
  font-size: 13px;
}
</style>
