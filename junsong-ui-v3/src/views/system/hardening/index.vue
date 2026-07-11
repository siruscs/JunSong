<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <span class="page-title">企业级硬化与长期运维</span>
      <el-button type="warning" :icon="Refresh" style="margin-left: 12px" @click="refreshAll">刷新</el-button>
    </div>

    <!-- 指标卡片 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>高危审计数</span></template>
          <div class="metric-value danger">{{ dashboard.highRiskAuditCount }}</div>
          <div class="metric-desc">近7天 HIGH/CRITICAL 审计快照数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>归档候选数</span></template>
          <div class="metric-value warning">{{ dashboard.archiveCandidateCount }}</div>
          <div class="metric-desc">最近一次归档预览候选数量</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>未关闭CRITICAL告警</span></template>
          <div class="metric-value danger">{{ dashboard.openCriticalAlertCount }}</div>
          <div class="metric-desc">状态为 OPEN 的 CRITICAL 告警数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 审计 Tab -->
      <el-tab-pane label="审计" name="audit">
        <el-form :model="auditQuery" ref="auditFormRef" :inline="true" label-width="90px">
          <el-form-item label="业务类型" prop="bizType">
            <el-input
              v-model="auditQuery.bizType"
              placeholder="请输入业务类型"
              clearable
              style="width: 200px"
              @keyup.enter="handleAuditQuery"
            />
          </el-form-item>
          <el-form-item label="风险级别" prop="riskLevel">
            <el-select v-model="auditQuery.riskLevel" placeholder="请选择" clearable style="width: 160px">
              <el-option label="低" value="LOW" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="高" value="HIGH" />
              <el-option label="严重" value="CRITICAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="auditDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleAuditQuery">搜索</el-button>
            <el-button :icon="Refresh" @click="resetAuditQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="auditLoading" :data="auditList" stripe border>
          <el-table-column label="业务类型" align="center" prop="bizType" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="业务ID" align="center" prop="bizId" min-width="160" :show-overflow-tooltip="true" />
          <el-table-column label="操作" align="center" prop="operation" width="100" />
          <el-table-column label="风险级别" align="center" prop="riskLevel" width="100">
            <template #default="{ row }">
              <el-tag :type="riskLevelTagType(row.riskLevel)">{{ riskLevelLabel(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作人" align="center" prop="operatorName" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
        </el-table>

        <pagination
          v-show="auditTotal > 0"
          :total="auditTotal"
          v-model:page="auditQuery.pageNum"
          v-model:limit="auditQuery.pageSize"
          @pagination="getAuditList"
        />
      </el-tab-pane>

      <!-- 归档 Tab -->
      <el-tab-pane label="归档" name="archive">
        <el-form :inline="true" label-width="90px">
          <el-form-item label="目标表">
            <el-select v-model="archiveForm.tableName" placeholder="请选择归档目标表" style="width: 320px">
              <el-option label="操作日志 (sys_oper_log)" value="sys_oper_log" />
              <el-option label="预测样本 (finance_prediction_sample)" value="finance_prediction_sample" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="handlePreviewArchive"
              v-hasPermi="['system:hardening:archive']"
            >预览归档候选</el-button>
            <!-- R25 边界：前端归档页面只暴露 dry-run=true 按钮，不暴露真实归档（dryRun:false）按钮。
                 后端 SysDataArchiveServiceImpl 默认 dry-run，不物理删除业务表行。 -->
            <el-button
              type="warning"
              @click="handleRunArchiveDry"
              v-hasPermi="['system:hardening:archive']"
            >执行归档(dry-run)</el-button>
          </el-form-item>
        </el-form>

        <el-card v-if="archivePreview" shadow="never" class="archive-preview">
          <template #header>
            <span>归档预览结果</span>
            <el-tag type="warning" size="small" style="margin-left: 8px">dry-run</el-tag>
          </template>
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="metric-label">目标表</div>
              <div>{{ archivePreview.tableName }}</div>
            </el-col>
            <el-col :span="6">
              <div class="metric-label">归档模式</div>
              <div>{{ archivePreview.archiveMode }}</div>
            </el-col>
            <el-col :span="6">
              <div class="metric-label">候选数量</div>
              <div class="metric-value warning">{{ archivePreview.candidateCount }}</div>
            </el-col>
            <el-col :span="6">
              <div class="metric-label">截止时间</div>
              <div>{{ archivePreview.cutoffTime }}</div>
            </el-col>
          </el-row>
        </el-card>

        <el-alert
          v-if="archiveRunResult"
          :title="archiveRunResultTitle"
          :type="archiveRunResult.success ? 'success' : 'error'"
          :description="archiveRunResult.message"
          show-icon
          :closable="false"
          style="margin-top: 16px"
        />
      </el-tab-pane>

      <!-- 告警 Tab -->
      <el-tab-pane label="告警" name="alert">
        <el-form :model="alertQuery" ref="alertFormRef" :inline="true" label-width="90px">
          <el-form-item label="严重级别" prop="severity">
            <el-select v-model="alertQuery.severity" placeholder="请选择" clearable style="width: 160px">
              <el-option label="低" value="LOW" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="高" value="HIGH" />
              <el-option label="严重" value="CRITICAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="alertQuery.status" placeholder="请选择" clearable style="width: 160px">
              <el-option label="未处理" value="OPEN" />
              <el-option label="已确认" value="ACKED" />
              <el-option label="已解决" value="RESOLVED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleAlertQuery">搜索</el-button>
            <el-button :icon="Refresh" @click="resetAlertQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="alertLoading" :data="alertList" stripe border>
          <el-table-column label="严重级别" align="center" prop="severity" width="100">
            <template #default="{ row }">
              <el-tag :type="severityTagType(row.severity)">{{ severityLabel(row.severity) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="标题" align="left" prop="title" min-width="220" :show-overflow-tooltip="true" />
          <el-table-column label="来源" align="center" prop="sourceType" width="160" :show-overflow-tooltip="true" />
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template #default="{ row }">
              <el-tag :type="alertStatusTagType(row.status)">{{ alertStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="命中次数" align="center" prop="hitCount" width="100" />
          <el-table-column label="最近触发时间" align="center" prop="lastSeenTime" width="180" />
          <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'OPEN'"
                link
                type="primary"
                @click="handleAck(row)"
                v-hasPermi="['system:hardening:alert']"
              >确认</el-button>
              <el-button
                v-if="row.status === 'OPEN' || row.status === 'ACKED'"
                link
                type="success"
                @click="handleResolve(row)"
                v-hasPermi="['system:hardening:alert']"
              >解决</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="alertTotal > 0"
          :total="alertTotal"
          v-model:page="alertQuery.pageNum"
          v-model:limit="alertQuery.pageSize"
          @pagination="getAlertList"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getEnterpriseHardeningDashboard,
  listAuditSnapshots,
  previewArchive,
  runArchive,
  listAlertEvents,
  ackAlert,
  resolveAlert
} from '@/api/system/hardening'

interface DashboardData {
  highRiskAuditCount: number
  archiveCandidateCount: number
  openCriticalAlertCount: number
  basis?: string
}

interface ArchivePreview {
  tableName: string
  archiveMode: string
  dryRun: string
  candidateCount: number
  cutoffTime: string
}

const showSearch = ref(true)
const activeTab = ref('audit')

// Dashboard
const dashboard = reactive<DashboardData>({
  highRiskAuditCount: 0,
  archiveCandidateCount: 0,
  openCriticalAlertCount: 0,
  basis: ''
})

// Audit tab
const auditLoading = ref(false)
const auditList = ref<any[]>([])
const auditTotal = ref(0)
const auditFormRef = ref()
const auditDateRange = ref<[string, string] | []>([])
const auditQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  bizType: '',
  riskLevel: '',
  beginTime: '',
  endTime: ''
})

// Archive tab
const archiveForm = reactive({ tableName: 'sys_oper_log' })
const archivePreview = ref<ArchivePreview | null>(null)
const archiveRunResult = ref<{ success: boolean; message: string } | null>(null)

// Alert tab
const alertLoading = ref(false)
const alertList = ref<any[]>([])
const alertTotal = ref(0)
const alertFormRef = ref()
const alertQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  severity: '',
  status: ''
})

const archiveRunResultTitle = computed(() => {
  if (!archiveRunResult.value) return ''
  return archiveRunResult.value.success ? '归档执行完成（dry-run）' : '归档执行失败'
})

function riskLevelTagType(level: string) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

function riskLevelLabel(level: string) {
  const map: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' }
  return map[level] || level
}

function severityTagType(severity: string) {
  if (severity === 'CRITICAL') return 'danger'
  if (severity === 'HIGH') return 'danger'
  if (severity === 'MEDIUM') return 'warning'
  return 'info'
}

function severityLabel(severity: string) {
  const map: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' }
  return map[severity] || severity
}

function alertStatusTagType(status: string) {
  if (status === 'OPEN') return 'danger'
  if (status === 'ACKED') return 'warning'
  return 'success'
}

function alertStatusLabel(status: string) {
  const map: Record<string, string> = { OPEN: '未处理', ACKED: '已确认', RESOLVED: '已解决' }
  return map[status] || status
}

async function fetchDashboard() {
  try {
    const res: any = await getEnterpriseHardeningDashboard()
    const data = res.data || res
    dashboard.highRiskAuditCount = data?.highRiskAuditCount ?? 0
    dashboard.archiveCandidateCount = data?.archiveCandidateCount ?? 0
    dashboard.openCriticalAlertCount = data?.openCriticalAlertCount ?? 0
    dashboard.basis = data?.basis || ''
  } catch (e: any) {
    ElMessage.error(e?.message || '加载硬化看板失败')
  }
}

async function getAuditList() {
  auditLoading.value = true
  try {
    if (auditDateRange.value && auditDateRange.value.length === 2) {
      auditQuery.beginTime = auditDateRange.value[0]
      auditQuery.endTime = auditDateRange.value[1]
    } else {
      auditQuery.beginTime = ''
      auditQuery.endTime = ''
    }
    const params: Record<string, any> = {
      pageNum: auditQuery.pageNum,
      pageSize: auditQuery.pageSize
    }
    if (auditQuery.bizType) params.bizType = auditQuery.bizType
    if (auditQuery.riskLevel) params.riskLevel = auditQuery.riskLevel
    if (auditQuery.beginTime) params.beginTime = auditQuery.beginTime
    if (auditQuery.endTime) params.endTime = auditQuery.endTime
    const res: any = await listAuditSnapshots(params)
    const rows = res.rows || res.data?.rows || res.data || []
    auditList.value = rows
    auditTotal.value = res.total || res.data?.total || (Array.isArray(rows) ? rows.length : 0)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载审计列表失败')
  } finally {
    auditLoading.value = false
  }
}

function handleAuditQuery() {
  auditQuery.pageNum = 1
  getAuditList()
}

function resetAuditQuery() {
  auditFormRef.value?.resetFields?.()
  auditDateRange.value = []
  auditQuery.bizType = ''
  auditQuery.riskLevel = ''
  auditQuery.beginTime = ''
  auditQuery.endTime = ''
  handleAuditQuery()
}

async function handlePreviewArchive() {
  if (!archiveForm.tableName) {
    ElMessage.warning('请选择归档目标表')
    return
  }
  try {
    const res: any = await previewArchive({ tableName: archiveForm.tableName })
    const data = res.data || res
    archivePreview.value = {
      tableName: data?.tableName || archiveForm.tableName,
      archiveMode: data?.archiveMode || '',
      dryRun: data?.dryRun ?? '1',
      candidateCount: data?.candidateCount ?? 0,
      cutoffTime: data?.cutoffTime || ''
    }
    archiveRunResult.value = null
    ElMessage.success('预览完成')
  } catch (e: any) {
    ElMessage.error(e?.message || '归档预览失败')
  }
}

async function handleRunArchiveDry() {
  if (!archiveForm.tableName) {
    ElMessage.warning('请选择归档目标表')
    return
  }
  try {
    // R25 边界：前端归档页面只调用 dry-run=true，不暴露真实归档（dryRun:false）按钮。
    // 后端 SysDataArchiveServiceImpl 默认 dry-run，不物理删除业务表行。
    const res: any = await runArchive({ tableName: archiveForm.tableName, dryRun: true })
    const data = res.data || res
    archiveRunResult.value = {
      success: true,
      message: `归档任务已记录（dry-run），候选数量: ${data?.candidateCount ?? 0}，截止时间: ${data?.cutoffTime || '-'}`
    }
    ElMessage.success('归档执行完成（dry-run）')
  } catch (e: any) {
    archiveRunResult.value = {
      success: false,
      message: e?.message || '归档执行失败'
    }
    ElMessage.error(e?.message || '归档执行失败')
  }
}

async function getAlertList() {
  alertLoading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: alertQuery.pageNum,
      pageSize: alertQuery.pageSize
    }
    if (alertQuery.severity) params.severity = alertQuery.severity
    if (alertQuery.status) params.status = alertQuery.status
    const res: any = await listAlertEvents(params)
    const rows = res.rows || res.data?.rows || res.data || []
    alertList.value = rows
    alertTotal.value = res.total || res.data?.total || (Array.isArray(rows) ? rows.length : 0)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载告警列表失败')
  } finally {
    alertLoading.value = false
  }
}

function handleAlertQuery() {
  alertQuery.pageNum = 1
  getAlertList()
}

function resetAlertQuery() {
  alertFormRef.value?.resetFields?.()
  alertQuery.severity = ''
  alertQuery.status = ''
  handleAlertQuery()
}

async function handleAck(row: any) {
  try {
    await ackAlert(row.eventId)
    ElMessage.success('告警已确认')
    getAlertList()
    fetchDashboard()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function handleResolve(row: any) {
  try {
    await resolveAlert(row.eventId)
    ElMessage.success('告警已解决')
    getAlertList()
    fetchDashboard()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function handleTabChange(name: string | number) {
  if (name === 'audit') {
    getAuditList()
  } else if (name === 'alert') {
    getAlertList()
  }
}

function refreshAll() {
  fetchDashboard()
  if (activeTab.value === 'audit') {
    getAuditList()
  } else if (activeTab.value === 'alert') {
    getAlertList()
  }
}

onMounted(() => {
  fetchDashboard()
  getAuditList()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
}
.metric-row {
  margin-bottom: 20px;
}
.metric-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.4;
}
.metric-value.danger {
  color: #f56c6c;
}
.metric-value.warning {
  color: #e6a23c;
}
.metric-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.metric-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.archive-preview {
  margin-top: 16px;
}
</style>
