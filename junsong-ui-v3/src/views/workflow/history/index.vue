<template>
  <div class="workflow-history-page app-container">
    <el-card class="runtime-table-card" shadow="never">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="88px" v-show="showSearch">
        <el-form-item label="流程标识" prop="processKey">
          <el-input v-model="queryParams.processKey" placeholder="请输入流程标识" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="实例 ID" prop="processInstanceId">
          <el-input v-model="queryParams.processInstanceId" placeholder="请输入实例 ID" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="业务键" prop="businessKey">
          <el-input v-model="queryParams.businessKey" placeholder="请输入业务键" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <RightToolbar v-model:showSearch="showSearch" @query="getList">
        <el-button type="success" :icon="Refresh" @click="getList" v-hasPermi="['workflow:history:list']">
          刷新
        </el-button>
      </RightToolbar>

      <div class="history-layout">
        <div class="history-layout__table">
          <el-table v-loading="loading" :data="filteredRows" highlight-current-row @current-change="handleCurrentChange">
            <el-table-column label="流程名称" min-width="220">
              <template #default="{ row }">
                <div class="runtime-title">
                  <el-button link type="primary" @click="selectRow(row)">
                    {{ safeWorkflowText(row.processDefinitionName || row.processDefinitionKey) }}
                  </el-button>
                  <div class="runtime-title__sub">{{ row.processInstanceId }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="流程标识" prop="processDefinitionKey" min-width="150" />
            <el-table-column label="业务键" prop="businessKey" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <el-button v-if="canOpenBusiness(row)" link type="primary" @click="goToBusiness(row)">
                  {{ safeWorkflowText(row.businessKey) }}
                </el-button>
                <span v-else>{{ safeWorkflowText(row.businessKey) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="发起人" prop="initiator" width="120">
              <template #default="{ row }">
                {{ safeWorkflowText(row.initiator) }}
              </template>
            </el-table-column>
            <el-table-column label="开始时间" min-width="168">
              <template #default="{ row }">
                {{ formatWorkflowDateTime(row.startTime) }}
              </template>
            </el-table-column>
            <el-table-column label="结束时间" min-width="168">
              <template #default="{ row }">
                {{ formatWorkflowDateTime(row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column label="总耗时" width="130">
              <template #default="{ row }">
                {{ formatWorkflowDuration(row.durationMs) }}
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && !filteredRows.length" description="暂无历史流程数据" />
        </div>

        <div class="history-layout__detail">
          <el-card shadow="never" class="history-detail-card">
            <template #header>
              <div class="history-detail-card__header">
                <span>历史轨迹</span>
                <div class="history-detail-card__header-tags" v-if="selectedSummary">
                  <el-tag size="small" :type="selectedSummary.running ? 'success' : 'info'">
                    {{ selectedSummary.running ? '运行中' : '已结束' }}
                  </el-tag>
                  <el-tag size="small" type="info">{{ selectedSummary.processInstanceId }}</el-tag>
                </div>
              </div>
            </template>

            <template v-if="selectedSummary">
              <el-descriptions :column="1" border class="history-summary">
                <el-descriptions-item label="流程名称">
                  {{ safeWorkflowText(selectedSummary.processDefinitionName || selectedSummary.processDefinitionKey) }}
                </el-descriptions-item>
                <el-descriptions-item label="业务键">
                  <el-button v-if="canOpenBusiness(selectedSummary)" link type="primary" @click="goToBusiness(selectedSummary)">
                    {{ safeWorkflowText(selectedSummary.businessKey) }}
                  </el-button>
                  <span v-else>{{ safeWorkflowText(selectedSummary.businessKey) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="发起人">
                  {{ safeWorkflowText(selectedSummary.initiator) }}
                </el-descriptions-item>
              </el-descriptions>

              <el-divider content-position="left">流程跟踪图</el-divider>
              <WorkflowTrackingViewer
                :xml="trackingXml"
                :completed-activity-ids="completedActivityIds"
                :active-activity-ids="activeActivityIds"
                :loading="detailLoading"
              />

              <el-divider content-position="left">节点轨迹</el-divider>
              <el-timeline v-loading="detailLoading">
                <el-timeline-item
                  v-for="item in activityRows"
                  :key="`${item.activityId}-${item.startTime}`"
                  :timestamp="`${formatWorkflowDateTime(item.startTime)} → ${formatWorkflowDateTime(item.endTime)}`"
                  placement="top"
                >
                  <div class="history-entry-title">{{ safeWorkflowText(item.activityName || item.activityId) }}</div>
                  <div class="history-entry-meta">
                    <span>类型：{{ safeWorkflowText(item.activityType) }}</span>
                    <span>处理人：{{ safeWorkflowText(item.assignee) }}</span>
                    <span>耗时：{{ formatWorkflowDuration(item.durationMs) }}</span>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="!detailLoading && !activityRows.length" description="暂无节点轨迹" :image-size="72" />

              <el-divider content-position="left">审批意见</el-divider>
              <div v-if="commentRows.length" class="history-comment-list">
                <div v-for="item in commentRows" :key="item.id" class="history-comment-item">
                  <div class="history-comment-item__head">
                    <span>{{ mapWorkflowCommentType(item.type) }}</span>
                    <span>{{ safeWorkflowText(item.userId) }}</span>
                    <span>{{ formatWorkflowDateTime(item.time) }}</span>
                  </div>
                  <div class="history-comment-item__body">{{ safeWorkflowText(item.message) }}</div>
                </div>
              </div>
              <el-empty v-else-if="!detailLoading" description="暂无审批意见" :image-size="72" />
            </template>
            <el-empty v-else description="请选择左侧一条流程实例查看历史轨迹" :image-size="88" />
          </el-card>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import WorkflowTrackingViewer from './WorkflowTrackingViewer.vue'
import { resetForm as resetFormUtil } from '@/utils/junsong'
import { getWorkflowDefinitionXml } from '@/api/workflow/definition'
import { getWorkflowInstanceDetail, type WorkflowInstanceRow } from '@/api/workflow/instance'
import {
  listWorkflowHistoryActivities,
  listWorkflowHistoryComments,
  listWorkflowHistoryInstances,
  type WorkflowHistoryActivityRow,
  type WorkflowHistoryCommentRow,
  type WorkflowHistoryInstanceRow,
} from '@/api/workflow/history'
import {
  formatWorkflowDateTime,
  formatWorkflowDuration,
  mapWorkflowCommentType,
  resolveWorkflowBusinessTarget,
  safeWorkflowText,
} from '../shared/runtime'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detailLoading = ref(false)
const showSearch = ref(true)
const queryFormRef = ref()
const rows = ref<WorkflowHistoryInstanceRow[]>([])
const selectedRow = ref<WorkflowHistoryInstanceRow | null>(null)
const selectedSummary = ref<(WorkflowInstanceRow & { initiator?: string | null; running?: boolean }) | null>(null)
const activityRows = ref<WorkflowHistoryActivityRow[]>([])
const commentRows = ref<WorkflowHistoryCommentRow[]>([])
const trackingXml = ref('')
const activeActivityIds = ref<string[]>([])

const queryParams = reactive({
  processKey: '',
  processInstanceId: '',
  businessKey: '',
  dateRange: [] as string[],
})

const filteredRows = computed(() => {
  return rows.value.filter((item) => {
    if (queryParams.processInstanceId && !item.processInstanceId.includes(queryParams.processInstanceId)) {
      return false
    }
    if (queryParams.businessKey && !String(item.businessKey || '').includes(queryParams.businessKey)) {
      return false
    }
    if (queryParams.dateRange?.length === 2) {
      const time = item.startTime ? new Date(item.startTime).getTime() : NaN
      const start = new Date(`${queryParams.dateRange[0]} 00:00:00`).getTime()
      const end = new Date(`${queryParams.dateRange[1]} 23:59:59`).getTime()
      if (!Number.isNaN(time) && (time < start || time > end)) return false
    }
    return true
  })
})

async function getList() {
  loading.value = true
  try {
    const res = await listWorkflowHistoryInstances({
      processKey: queryParams.processKey || undefined,
      limit: 100,
    })
    rows.value = res.data || []
    const queryInstanceId = String(route.query.processInstanceId || '').trim()
    if (queryInstanceId) {
      const nextRow = rows.value.find((item) => item.processInstanceId === queryInstanceId)
      if (nextRow) {
        await selectRow(nextRow)
      } else {
        await loadByInstanceId(queryInstanceId)
      }
      return
    }
    const nextRow = rows.value[0]
    if (nextRow) {
      await selectRow(nextRow)
      return
    }
    clearDetail()
  } finally {
    loading.value = false
  }
}

function clearDetail() {
  selectedRow.value = null
  selectedSummary.value = null
  activityRows.value = []
  commentRows.value = []
  trackingXml.value = ''
  activeActivityIds.value = []
}

function canOpenBusiness(row?: Partial<WorkflowHistoryInstanceRow & WorkflowInstanceRow> | null) {
  return !!resolveWorkflowBusinessTarget(row?.processDefinitionKey, row?.businessKey)
}

function goToBusiness(row?: Partial<WorkflowHistoryInstanceRow & WorkflowInstanceRow> | null) {
  const target = resolveWorkflowBusinessTarget(row?.processDefinitionKey, row?.businessKey)
  if (!target) return
  router.push(target)
}

function resetQuery() {
  queryParams.processKey = ''
  queryParams.processInstanceId = ''
  queryParams.businessKey = ''
  queryParams.dateRange = []
  resetFormUtil(queryFormRef.value)
  getList()
}

function toHistoryRow(summary: WorkflowInstanceRow & { initiator?: string | null; running?: boolean }): WorkflowHistoryInstanceRow {
  return {
    processInstanceId: summary.processInstanceId,
    processDefinitionId: summary.processDefinitionId,
    processDefinitionKey: summary.processDefinitionKey,
    processDefinitionName: summary.processDefinitionName,
    businessKey: summary.businessKey,
    startTime: summary.startTime,
    endTime: summary.endTime,
    durationMs: summary.durationMs,
    initiator: summary.initiator,
    running: summary.running,
  }
}

function uniqueActivityIds(values: Array<string | null | undefined>) {
  return Array.from(new Set(values.filter((item): item is string => Boolean(item && item.trim()))))
}

async function loadDetail(processInstanceId: string) {
  detailLoading.value = true
  try {
    const [detailRes, activitiesRes, commentsRes] = await Promise.all([
      getWorkflowInstanceDetail(processInstanceId),
      listWorkflowHistoryActivities(processInstanceId),
      listWorkflowHistoryComments(processInstanceId),
    ])
    activityRows.value = activitiesRes.data || []
    commentRows.value = commentsRes.data || []
    const detail = detailRes.data
    const summary = detail?.instance
      ? {
          ...detail.instance,
          initiator: detail.instance.startUserId,
          running: detail.running,
        }
      : null
    selectedSummary.value = summary
    activeActivityIds.value = uniqueActivityIds(
      activityRows.value.filter((item) => !item.endTime).map((item) => item.activityId),
    )
    const definitionId = summary?.processDefinitionId
    if (definitionId) {
      const xmlRes = await getWorkflowDefinitionXml(definitionId)
      trackingXml.value = xmlRes.data?.xml || ''
    } else {
      trackingXml.value = ''
    }
  } finally {
    detailLoading.value = false
  }
}

async function selectRow(row: WorkflowHistoryInstanceRow) {
  selectedRow.value = row
  await loadDetail(row.processInstanceId)
}

async function loadByInstanceId(processInstanceId: string) {
  await loadDetail(processInstanceId)
  if (selectedSummary.value) {
    selectedRow.value = toHistoryRow(selectedSummary.value)
  }
}

function handleCurrentChange(row?: WorkflowHistoryInstanceRow) {
  if (row) {
    selectRow(row)
  }
}

const completedActivityIds = computed(() =>
  uniqueActivityIds(activityRows.value.map((item) => item.activityId)).filter((id) => !activeActivityIds.value.includes(id)),
)

onMounted(() => {
  const queryInstanceId = String(route.query.processInstanceId || '').trim()
  if (queryInstanceId) {
    queryParams.processInstanceId = queryInstanceId
  }
  getList()
})
</script>

<style scoped>
.workflow-history-page {
  display: flex;
  flex-direction: column;
}

.history-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(360px, 0.9fr);
  gap: 16px;
}

.history-detail-card {
  min-height: 100%;
}

.history-detail-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-summary {
  margin-bottom: 12px;
}

.history-detail-card__header-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.runtime-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.runtime-title__sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.history-entry-title {
  font-size: 14px;
  font-weight: 600;
}

.history-entry-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.history-comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-comment-item {
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
}

.history-comment-item__head {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.history-comment-item__body {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
}

@media (max-width: 1200px) {
  .history-layout {
    grid-template-columns: 1fr;
  }
}
</style>
