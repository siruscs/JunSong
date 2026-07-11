<template>
  <div class="app-container cashflow-forecast">
    <div class="page-head">
      <div>
        <h2>现金流预测</h2>
        <p>基于R15承诺回款预测未来7/14/30天回款，量化现金压力并复盘预测偏差。</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="loadDashboard">刷新</el-button>
        <el-button type="primary" :icon="Camera" :loading="snapshotLoading" @click="handleSnapshot" v-hasPermi="['finance:cashflowForecast:snapshot']">生成预测快照</el-button>
      </div>
    </div>

    <el-row :gutter="12" class="window-row">
      <el-col :xs="24" :sm="24" :md="8" v-for="window in dashboard.windows || []" :key="window.windowDays">
        <div class="window-card">
          <div class="window-title">{{ windowLabels[window.windowDays] || window.windowLabel || ('未来' + window.windowDays + '天') }}</div>
          <div class="window-amount">&yen;{{ money(window.forecastReceivableAmount) }}</div>
          <div class="window-meta">
            <span>承诺回款: &yen;{{ money(window.promisedAmount) }}</span>
            <span>实际回款: &yen;{{ money(window.actualReceivableAmount) }}</span>
          </div>
          <div class="window-deviation">
            <el-tag :type="deviationTag(window.deviationAmount)" size="small">
              偏差: &yen;{{ money(window.deviationAmount) }} ({{ ratePercent(window.deviationRate) }}%)
            </el-tag>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>现金压力指数</span>
          <el-tag :type="pressureTag(dashboard.pressure)" size="small">
            {{ pressureLabel(dashboard.pressure) }} {{ dashboard.pressure?.pressureScore || 0 }}分
          </el-tag>
        </div>
      </template>
      <div class="pressure-body">
        <div class="pressure-score" :class="pressureClass(dashboard.pressure)">
          {{ dashboard.pressure?.pressureScore || 0 }}
        </div>
        <div class="pressure-detail">
          <div class="pressure-row">
            <span>总未缴金额</span><strong>&yen;{{ money(dashboard.pressure?.totalUnpaidAmount) }}</strong>
          </div>
          <div class="pressure-row">
            <span>逾期承诺金额</span><strong>&yen;{{ money(dashboard.pressure?.overduePromiseAmount) }}</strong>
          </div>
          <div class="pressure-row">
            <span>30天以上应收</span><strong>&yen;{{ money(dashboard.pressure?.age30PlusAmount) }}</strong>
          </div>
          <div class="pressure-row">
            <span>近7天实收</span><strong>&yen;{{ money(dashboard.pressure?.recentCashInAmount) }}</strong>
          </div>
          <div class="pressure-row">
            <span>近7天费用</span><strong>&yen;{{ money(dashboard.pressure?.recentExpenseAmount) }}</strong>
          </div>
        </div>
      </div>
      <div v-if="dashboard.pressure?.reasons && dashboard.pressure.reasons.length > 0" class="pressure-reasons">
        <el-tag v-for="reason in dashboard.pressure.reasons" :key="reason" type="danger" size="small" effect="plain" style="margin-right: 8px;">
          {{ reason }}
        </el-tag>
      </div>
    </el-card>

    <el-card class="section-card">
      <template #header><span>预测偏差</span></template>
      <el-table :data="dashboard.forecastDeviation || []" stripe border style="width: 100%" empty-text="暂无预测快照，点击生成预测快照">
        <el-table-column prop="forecastDate" label="预测日期" min-width="120">
          <template #default="scope">{{ formatDate(scope.row.forecastDate) }}</template>
        </el-table-column>
        <el-table-column prop="windowDays" label="窗口" width="80">
          <template #default="scope">未来{{ scope.row.windowDays }}天</template>
        </el-table-column>
        <el-table-column prop="forecastReceivableAmount" label="预计回款" min-width="120">
          <template #default="scope">&yen;{{ money(scope.row.forecastReceivableAmount) }}</template>
        </el-table-column>
        <el-table-column prop="actualReceivableAmount" label="实际回款" min-width="120">
          <template #default="scope">&yen;{{ money(scope.row.actualReceivableAmount) }}</template>
        </el-table-column>
        <el-table-column prop="deviationAmount" label="偏差金额" min-width="120">
          <template #default="scope">&yen;{{ money(scope.row.deviationAmount) }}</template>
        </el-table-column>
        <el-table-column prop="deviationRate" label="偏差率" width="100">
          <template #default="scope">{{ ratePercent(scope.row.deviationRate) }}%</template>
        </el-table-column>
        <el-table-column prop="pressureLevel" label="压力等级" width="100">
          <template #default="scope">
            <el-tag :type="levelTag(scope.row.pressureLevel)" size="small">{{ levelLabel(scope.row.pressureLevel) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section-card">
      <template #header><span>周经营节奏</span></template>
      <div class="rhythm-body">
        <div class="rhythm-item">
          <span>本周预计回款</span>
          <strong>&yen;{{ money(dashboard.weeklyRhythm?.weeklyForecastAmount) }}</strong>
        </div>
        <div class="rhythm-item">
          <span>本周逾期承诺</span>
          <strong>&yen;{{ money(dashboard.weeklyRhythm?.weeklyOverduePromiseAmount) }}</strong>
        </div>
        <div class="rhythm-item">
          <span>本周现金压力等级</span>
          <el-tag :type="levelTag(dashboard.weeklyRhythm?.weeklyPressureLevel)" size="small">
            {{ levelLabel(dashboard.weeklyRhythm?.weeklyPressureLevel) }}
          </el-tag>
        </div>
        <div class="rhythm-action">
          <span>推荐动作</span>
          <p>{{ dashboard.weeklyRhythm?.recommendedAction || '暂无建议' }}</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Refresh } from '@element-plus/icons-vue'
import { createCashflowForecastSnapshot, getCashflowForecastDashboard } from '@/api/finance/cashflowForecast'

const loading = ref(false)
const snapshotLoading = ref(false)
const dashboard = ref<any>({})

const windowLabels: Record<number, string> = {
  7: '未来7天',
  14: '未来14天',
  30: '未来30天',
}

function money(value: any) {
  return Number(value || 0).toFixed(2)
}

function ratePercent(value: any) {
  return Number(value || 0).toFixed(2)
}

function formatDate(value: any) {
  if (!value) return '-'
  return String(value).substring(0, 10)
}

function pressureLabel(pressure: any) {
  const level = pressure?.pressureLevel || 'LOW'
  return levelLabel(level)
}

function pressureTag(pressure: any) {
  return levelTag(pressure?.pressureLevel)
}

function pressureClass(pressure: any) {
  return 'score-' + (pressure?.pressureLevel || 'LOW').toLowerCase()
}

function levelLabel(level: string) {
  const labels: Record<string, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '严重',
  }
  return labels[level] || level || '-'
}

function levelTag(level: string) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}

function deviationTag(amount: any) {
  return Number(amount || 0) >= 0 ? 'success' : 'danger'
}

function loadDashboard() {
  loading.value = true
  return getCashflowForecastDashboard({})
    .then((res: any) => {
      dashboard.value = res.data || {}
    })
    .finally(() => {
      loading.value = false
    })
}

function handleSnapshot() {
  snapshotLoading.value = true
  createCashflowForecastSnapshot({})
    .then(() => {
      ElMessage.success('预测快照已生成')
      return loadDashboard()
    })
    .finally(() => {
      snapshotLoading.value = false
    })
}

onMounted(loadDashboard)
</script>

<style scoped>
.cashflow-forecast {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.page-head,
.window-card,
.section-card {
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fff;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  margin-bottom: 12px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.page-head p {
  margin: 0;
  color: #667085;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.window-row {
  margin-bottom: 12px;
}

.window-card {
  min-height: 130px;
  padding: 16px;
}

.window-title {
  margin-bottom: 8px;
  color: #667085;
  font-size: 14px;
  font-weight: 600;
}

.window-amount {
  margin-bottom: 8px;
  color: #18202f;
  font-size: 26px;
  font-weight: 800;
}

.window-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
  color: #909399;
  font-size: 12px;
}

.window-deviation {
  margin-top: 4px;
}

.section-card {
  margin-bottom: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pressure-body {
  display: flex;
  align-items: center;
  gap: 24px;
}

.pressure-score {
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  background: #67C23A;
}

.pressure-score.score-medium {
  background: #E6A23C;
}

.pressure-score.score-high {
  background: #F56C6C;
}

.pressure-score.score-critical {
  background: #d92d20;
}

.pressure-detail {
  flex: 1;
}

.pressure-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  border-bottom: 1px solid #f0f2f5;
  font-size: 13px;
  color: #606266;
}

.pressure-row strong {
  color: #18202f;
}

.pressure-reasons {
  margin-top: 12px;
}

.rhythm-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.rhythm-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  background: #fafbfc;
  border-radius: 6px;
}

.rhythm-item span {
  color: #909399;
  font-size: 13px;
}

.rhythm-item strong {
  color: #18202f;
  font-size: 18px;
}

.rhythm-action {
  grid-column: 1 / -1;
  padding: 12px;
  background: #fafbfc;
  border-radius: 6px;
}

.rhythm-action span {
  color: #909399;
  font-size: 13px;
}

.rhythm-action p {
  margin: 6px 0 0;
  color: #18202f;
  font-weight: 600;
}
</style>
