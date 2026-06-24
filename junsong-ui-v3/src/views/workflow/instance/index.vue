<template>
  <div class="workflow-runtime-page app-container">
    <section class="runtime-overview">
      <el-card v-for="card in overviewCards" :key="card.key" class="runtime-overview-card" shadow="hover">
        <div class="runtime-overview-card__label">{{ card.label }}</div>
        <div class="runtime-overview-card__value">{{ card.value }}</div>
        <div class="runtime-overview-card__hint">{{ card.hint }}</div>
      </el-card>
    </section>

    <el-card class="runtime-table-card" shadow="never">
      <el-form
        ref="queryFormRef"
        :model="queryParams"
        :inline="true"
        label-width="88px"
        v-show="showSearch"
      >
        <el-form-item label="流程标识" prop="processKey">
          <el-input
            v-model="queryParams.processKey"
            placeholder="请输入流程标识"
            clearable
            @keyup.enter="getList"
          />
        </el-form-item>
        <el-form-item label="业务键" prop="businessKey">
          <el-input
            v-model="queryParams.businessKey"
            placeholder="请输入业务键"
            clearable
            @keyup.enter="getList"
          />
        </el-form-item>
        <el-form-item label="实例状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 140px">
            <el-option label="全部" value="all" />
            <el-option label="运行中" value="running" />
            <el-option label="已结束" value="finished" />
          </el-select>
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
        <el-button type="success" :icon="Refresh" @click="getList" v-hasPermi="['workflow:instance:list']">
          刷新
        </el-button>
      </RightToolbar>

      <el-table v-loading="loading" :data="filteredList">
        <el-table-column label="流程名称" min-width="220">
          <template #default="{ row }">
            <div class="runtime-title">
              <el-button link type="primary" @click="openDetail(row)">
                {{ row.processDefinitionName || row.processDefinitionKey || '未命名流程' }}
              </el-button>
              <div class="runtime-title__sub">{{ row.processInstanceId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="流程标识" prop="processDefinitionKey" min-width="160" />
        <el-table-column label="业务键" prop="businessKey" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button v-if="canOpenBusiness(row)" link type="primary" @click="goToBusiness(row)">
              {{ safeWorkflowText(row.businessKey) }}
            </el-button>
            <span v-else>{{ safeWorkflowText(row.businessKey) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发起人" prop="startUserId" width="120">
          <template #default="{ row }">
            {{ safeWorkflowText(row.startUserId || row.initiator) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="mapWorkflowInstanceStatus(row).type">
              {{ mapWorkflowInstanceStatus(row).label }}
            </el-tag>
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
        <el-table-column label="耗时" width="130">
          <template #default="{ row }">
            {{ formatWorkflowDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="isRunning(row)"
              link
              type="primary"
              @click="openRunningTasks(row)"
              v-hasPermi="['workflow:instance:list']"
            >
              运行任务
            </el-button>
            <el-button link type="info" @click="goToHistory(row)" v-hasPermi="['workflow:history:list']">
              历史
            </el-button>
            <el-button v-if="canOpenBusiness(row)" link type="primary" @click="goToBusiness(row)">业务单</el-button>
            <el-button
              v-if="isRunning(row)"
              link
              type="danger"
              @click="handleTerminate(row)"
              v-hasPermi="['workflow:instance:remove']"
            >
              终止
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filteredList.length" description="暂无流程实例数据" />
    </el-card>

    <el-drawer v-model="detailDrawer.visible" title="流程实例详情" size="680px">
      <el-skeleton :loading="detailDrawer.loading" animated :rows="8">
        <template #default>
          <el-descriptions v-if="detailDrawer.data" :column="2" border>
            <el-descriptions-item label="流程名称">
              {{ safeWorkflowText(detailDrawer.data.instance.processDefinitionName) }}
            </el-descriptions-item>
            <el-descriptions-item label="流程标识">
              {{ safeWorkflowText(detailDrawer.data.instance.processDefinitionKey) }}
            </el-descriptions-item>
            <el-descriptions-item label="实例 ID" :span="2">
              {{ detailDrawer.data.instance.processInstanceId }}
            </el-descriptions-item>
            <el-descriptions-item label="定义 ID" :span="2">
              {{ safeWorkflowText(detailDrawer.data.instance.processDefinitionId) }}
            </el-descriptions-item>
            <el-descriptions-item label="业务键">
              <el-button
                v-if="canOpenBusiness(detailDrawer.data.instance)"
                link
                type="primary"
                @click="goToBusiness(detailDrawer.data.instance)"
              >
                {{ safeWorkflowText(detailDrawer.data.instance.businessKey) }}
              </el-button>
              <span v-else>{{ safeWorkflowText(detailDrawer.data.instance.businessKey) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="发起人">
              {{ safeWorkflowText(detailDrawer.data.instance.startUserId) }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="mapWorkflowInstanceStatus(detailDrawer.data.instance).type">
                {{ mapWorkflowInstanceStatus(detailDrawer.data.instance).label }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">
              {{ formatWorkflowDateTime(detailDrawer.data.instance.startTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ formatWorkflowDateTime(detailDrawer.data.instance.endTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="耗时">
              {{ formatWorkflowDuration(detailDrawer.data.instance.durationMs) }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">运行中任务</el-divider>
          <div v-if="detailDrawer.tasks.length" class="runtime-task-list">
            <div v-for="task in detailDrawer.tasks" :key="task.taskId" class="runtime-task-item">
              <div class="runtime-task-item__title">{{ safeWorkflowText(task.taskName) }}</div>
              <div class="runtime-task-item__meta">
                <span>处理人：{{ safeWorkflowText(task.assignee) }}</span>
                <span>创建时间：{{ formatWorkflowDateTime(task.createTime) }}</span>
              </div>
              <div class="runtime-task-item__desc">{{ safeWorkflowText(task.description) }}</div>
            </div>
          </div>
          <el-empty v-else description="当前实例暂无运行中任务" :image-size="72" />

          <div class="runtime-drawer-actions">
            <el-button
              v-if="canOpenBusiness(detailDrawer.data?.instance)"
              type="primary"
              @click="goToBusiness(detailDrawer.data?.instance)"
            >
              查看业务单
            </el-button>
            <el-button type="primary" plain @click="goToHistory(detailDrawer.data?.instance)">查看历史记录</el-button>
          </div>
        </template>
      </el-skeleton>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import { resetForm as resetFormUtil } from '@/utils/junsong'
import {
  type WorkflowInstanceDetail,
  type WorkflowInstanceRow,
  type WorkflowRunningTask,
  getWorkflowInstanceDetail,
  listWorkflowInstances,
  listWorkflowRunningTasks,
  terminateWorkflowInstance,
} from '@/api/workflow/instance'
import { listWorkflowHistoryInstances, type WorkflowHistoryInstanceRow } from '@/api/workflow/history'
import {
  formatWorkflowDateTime,
  formatWorkflowDuration,
  mapWorkflowInstanceStatus,
  resolveWorkflowBusinessTarget,
  safeWorkflowText,
} from '../shared/runtime'

type WorkflowRuntimeRow = WorkflowInstanceRow & { source: 'running' | 'finished'; initiator?: string | null }

interface DetailDrawerState {
  visible: boolean
  loading: boolean
  data: WorkflowInstanceDetail | null
  tasks: WorkflowRunningTask[]
}

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const showSearch = ref(true)
const queryFormRef = ref()
const rawList = ref<WorkflowRuntimeRow[]>([])
const detailDrawer = reactive<DetailDrawerState>({
  visible: false,
  loading: false,
  data: null,
  tasks: [],
})

const queryParams = reactive({
  processKey: '',
  businessKey: '',
  status: 'all',
  dateRange: [] as string[],
})

const filteredList = computed(() => {
  return rawList.value.filter((item) => {
    const matchesStatus =
      queryParams.status === 'all'
      || (queryParams.status === 'running' && isRunning(item))
      || (queryParams.status === 'finished' && !isRunning(item))
    if (!matchesStatus) return false

    if (queryParams.dateRange?.length === 2) {
      const time = item.startTime ? new Date(item.startTime).getTime() : NaN
      const start = new Date(`${queryParams.dateRange[0]} 00:00:00`).getTime()
      const end = new Date(`${queryParams.dateRange[1]} 23:59:59`).getTime()
      if (!Number.isNaN(time) && (time < start || time > end)) return false
    }

    return true
  })
})

const overviewCards = computed(() => {
  const items = filteredList.value
  const now = Date.now()
  const recentWindow = 1000 * 60 * 60 * 24 * 7
  return [
    {
      key: 'total',
      label: '当前查询实例数',
      value: items.length,
      hint: '基于当前筛选结果汇总',
    },
    {
      key: 'running',
      label: '运行中实例',
      value: items.filter((item) => isRunning(item)).length,
      hint: '可继续跟踪当前节点',
    },
    {
      key: 'finished',
      label: '已结束实例',
      value: items.filter((item) => !isRunning(item)).length,
      hint: '含已完成或已终止数据',
    },
    {
      key: 'recent',
      label: '近 7 天发起',
      value: items.filter((item) => {
        if (!item.startTime) return false
        return now - new Date(item.startTime).getTime() <= recentWindow
      }).length,
      hint: '用于快速观察近期活跃度',
    },
  ]
})

function isRunning(row: { running?: boolean; endTime?: string | null }) {
  return row.running === true || !row.endTime
}

function normalizeFinishedRow(row: WorkflowHistoryInstanceRow): WorkflowRuntimeRow {
  return {
    ...row,
    startUserId: row.initiator,
    running: false,
    source: 'finished',
  }
}

function normalizeRunningRow(row: WorkflowInstanceRow): WorkflowRuntimeRow {
  return {
    ...row,
    running: true,
    source: 'running',
  }
}

async function getList() {
  loading.value = true
  try {
    const [runningRes, finishedRes] = await Promise.all([
      listWorkflowInstances({
        processKey: queryParams.processKey || undefined,
        businessKey: queryParams.businessKey || undefined,
      }),
      listWorkflowHistoryInstances({
        processKey: queryParams.processKey || undefined,
        limit: 100,
      }),
    ])

    const runningRows = (runningRes.data || []).map(normalizeRunningRow)
    const finishedRows = (finishedRes.data || []).map(normalizeFinishedRow)
    const mergedMap = new Map<string, WorkflowRuntimeRow>()
    runningRows.forEach((item) => mergedMap.set(item.processInstanceId, item))
    finishedRows.forEach((item) => {
      if (!mergedMap.has(item.processInstanceId)) {
        mergedMap.set(item.processInstanceId, item)
      }
    })
    rawList.value = Array.from(mergedMap.values()).filter((item) => {
      const processKeyMatch = !queryParams.processKey || item.processDefinitionKey?.includes(queryParams.processKey)
      const businessKeyMatch = !queryParams.businessKey || String(item.businessKey || '').includes(queryParams.businessKey)
      return processKeyMatch && businessKeyMatch
    })
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.processKey = ''
  queryParams.businessKey = ''
  queryParams.status = 'all'
  queryParams.dateRange = []
  resetFormUtil(queryFormRef.value)
  getList()
}

async function openDetail(row: WorkflowRuntimeRow) {
  detailDrawer.visible = true
  detailDrawer.loading = true
  detailDrawer.tasks = []
  try {
    const detailRes = await getWorkflowInstanceDetail(row.processInstanceId)
    detailDrawer.data = detailRes.data
    if (detailRes.data?.running) {
      const tasksRes = await listWorkflowRunningTasks(row.processInstanceId)
      detailDrawer.tasks = tasksRes.data || []
    }
  } finally {
    detailDrawer.loading = false
  }
}

async function openRunningTasks(row: WorkflowRuntimeRow) {
  await openDetail(row)
}

function goToHistory(row?: WorkflowRuntimeRow | WorkflowInstanceRow | null) {
  if (!row?.processInstanceId) return
  router.push({
    path: '/workflow/history',
    query: { processInstanceId: row.processInstanceId },
  })
}

function canOpenBusiness(row?: Partial<WorkflowRuntimeRow | WorkflowInstanceRow> | null) {
  return !!resolveWorkflowBusinessTarget(row?.processDefinitionKey, row?.businessKey)
}

function goToBusiness(row?: Partial<WorkflowRuntimeRow | WorkflowInstanceRow> | null) {
  const target = resolveWorkflowBusinessTarget(row?.processDefinitionKey, row?.businessKey)
  if (!target) return
  router.push(target)
}

async function handleTerminate(row: WorkflowRuntimeRow) {
  await ElMessageBox.confirm(
    `确认终止流程实例「${row.processDefinitionName || row.processInstanceId}」吗？该操作会结束当前流程。`,
    '终止确认',
    {
      type: 'warning',
      confirmButtonText: '确认终止',
      cancelButtonText: '取消',
    },
  )
  await terminateWorkflowInstance(row.processInstanceId, '前端手动终止')
  ElMessage.success('流程实例已终止')
  detailDrawer.visible = false
  getList()
}

onMounted(() => {
  queryParams.processKey = typeof route.query.processKey === 'string' ? route.query.processKey : ''
  queryParams.businessKey = typeof route.query.businessKey === 'string' ? route.query.businessKey : ''
  getList()
})
</script>

<style scoped>
.workflow-runtime-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.runtime-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.runtime-overview-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.runtime-overview-card__value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.runtime-overview-card__hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
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

.runtime-task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.runtime-task-item {
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
}

.runtime-task-item__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.runtime-task-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.runtime-task-item__desc {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.runtime-drawer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1200px) {
  .runtime-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .runtime-overview {
    grid-template-columns: 1fr;
  }
}
</style>
