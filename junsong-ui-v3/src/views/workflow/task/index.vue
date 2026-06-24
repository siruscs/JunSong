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
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待办任务" name="todo" />
        <el-tab-pane label="已办任务" name="done" />
        <el-tab-pane label="我发起的" name="applied" />
      </el-tabs>

      <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="88px" v-show="showSearch">
        <el-form-item :label="activeTab === 'applied' ? '流程名称' : '任务名称'" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            :placeholder="activeTab === 'applied' ? '请输入流程名称/标识' : '请输入任务名称'"
            clearable
            @keyup.enter="applyFilter"
          />
        </el-form-item>
        <el-form-item :label="activeTab === 'applied' ? '业务键' : '处理人'" prop="operator">
          <el-input
            v-model="queryParams.operator"
            :placeholder="activeTab === 'applied' ? '请输入业务键' : '请输入处理人'"
            clearable
            @keyup.enter="applyFilter"
          />
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
          <el-button type="primary" @click="applyFilter">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <RightToolbar v-model:showSearch="showSearch" @query="loadCurrentTab">
        <el-button type="success" :icon="Refresh" @click="loadCurrentTab" v-hasPermi="['workflow:task:list']">
          刷新
        </el-button>
      </RightToolbar>

      <el-table v-loading="loading" :data="filteredRows">
        <template v-if="activeTab !== 'applied'">
          <el-table-column label="任务名称" min-width="220">
            <template #default="{ row }">
              <div class="runtime-title">
                <el-button link type="primary" @click="openTaskDetail(row)">{{ safeWorkflowText(row.taskName) }}</el-button>
                <div class="runtime-title__sub">{{ row.taskId }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="流程实例 ID" prop="processInstanceId" min-width="200" show-overflow-tooltip />
          <el-table-column label="处理人" width="120">
            <template #default="{ row }">
              {{ safeWorkflowText(row.assignee || row.owner) }}
            </template>
          </el-table-column>
          <el-table-column :label="activeTab === 'todo' ? '创建时间' : '完成时间'" min-width="168">
            <template #default="{ row }">
              {{ formatWorkflowDateTime(activeTab === 'todo' ? row.createTime : row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="时长" width="130">
            <template #default="{ row }">
              {{ formatWorkflowDuration(row.durationMs) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="mapWorkflowTaskTabStatus(activeTab, row).type">
                {{ mapWorkflowTaskTabStatus(activeTab, row).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="320" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTaskDetail(row)">详情</el-button>
              <el-button
                v-if="canOpenBusiness(row)"
                link
                type="primary"
                @click="goToBusiness(row)"
              >
                业务单
              </el-button>
              <el-button
                v-if="activeTab === 'todo' && canClaim(row)"
                link
                type="primary"
                @click="handleClaim(row)"
                v-hasPermi="['workflow:task:approve']"
              >
                认领
              </el-button>
              <el-button
                v-if="activeTab === 'todo'"
                link
                type="success"
                @click="openApproveDialog(row)"
                v-hasPermi="['workflow:task:approve']"
              >
                审批通过
              </el-button>
              <el-button
                v-if="activeTab === 'todo'"
                link
                type="danger"
                @click="openRejectDialog(row)"
                v-hasPermi="['workflow:task:reject']"
              >
                驳回
              </el-button>
              <el-button
                v-if="activeTab === 'todo'"
                link
                type="warning"
                @click="openTransferDialog(row)"
                v-hasPermi="['workflow:task:delegate']"
              >
                转办
              </el-button>
              <el-button link type="info" @click="goToHistory(row.processInstanceId)" v-hasPermi="['workflow:history:list']">
                历史
              </el-button>
            </template>
          </el-table-column>
        </template>

        <template v-else>
          <el-table-column label="流程名称" min-width="220">
            <template #default="{ row }">
              <div class="runtime-title">
                <el-button link type="primary" @click="goToHistory(row.processInstanceId)">
                  {{ safeWorkflowText(row.processDefinitionName || row.processDefinitionKey) }}
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
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="mapWorkflowTaskTabStatus(activeTab, row).type">
                {{ mapWorkflowTaskTabStatus(activeTab, row).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goToHistory(row.processInstanceId)">历史</el-button>
              <el-button v-if="canOpenBusiness(row)" link type="primary" @click="goToBusiness(row)">业务单</el-button>
            </template>
          </el-table-column>
        </template>
      </el-table>

      <el-empty v-if="!loading && !filteredRows.length" description="当前分页暂无任务数据" />
    </el-card>

    <el-drawer v-model="detailDrawer.visible" title="任务详情" size="720px">
      <el-skeleton :loading="detailDrawer.loading" animated :rows="10">
        <template #default>
          <template v-if="detailDrawer.data">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="任务名称">
                {{ safeWorkflowText(detailDrawer.data.taskName) }}
              </el-descriptions-item>
              <el-descriptions-item label="处理人">
                {{ safeWorkflowText(detailDrawer.data.assignee || detailDrawer.data.owner) }}
              </el-descriptions-item>
              <el-descriptions-item label="任务 ID" :span="2">
                {{ detailDrawer.data.taskId }}
              </el-descriptions-item>
              <el-descriptions-item label="流程实例 ID" :span="2">
                {{ detailDrawer.data.processInstanceId }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatWorkflowDateTime(detailDrawer.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="到期时间">
                {{ formatWorkflowDateTime(detailDrawer.data.dueDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="说明" :span="2">
                {{ safeWorkflowText(detailDrawer.data.description) }}
              </el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">流程变量摘要</el-divider>
            <el-descriptions :column="1" border v-if="taskVariableEntries.length">
              <el-descriptions-item v-for="entry in taskVariableEntries" :key="entry.key" :label="entry.key">
                {{ entry.value }}
              </el-descriptions-item>
            </el-descriptions>
            <el-empty v-else description="当前任务暂无流程变量" :image-size="72" />

            <el-divider content-position="left">原始变量 JSON</el-divider>
            <el-input :model-value="rawVariablesText" type="textarea" :rows="8" readonly />

            <div class="runtime-drawer-actions">
              <el-button
                v-if="canOpenBusiness(detailDrawer.data)"
                type="primary"
                @click="goToBusiness(detailDrawer.data)"
              >
                查看业务单
              </el-button>
              <el-button type="primary" plain @click="goToHistory(detailDrawer.data.processInstanceId)">
                查看历史记录
              </el-button>
            </div>
          </template>
        </template>
      </el-skeleton>
    </el-drawer>

    <el-dialog v-model="approveDialog.visible" title="审批通过" width="560px">
      <el-form label-width="88px">
        <el-form-item label="审批意见">
          <el-input v-model="approveDialog.form.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
        </el-form-item>
        <el-form-item label="变量 JSON">
          <el-input
            v-model="approveDialog.form.variablesText"
            type="textarea"
            :rows="6"
            placeholder='如需传变量，请输入 JSON，例如：{"approved":true}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialog.visible" title="驳回任务" width="520px">
      <el-alert
        title="当前后端驳回逻辑会终止整个流程实例，请确认后再执行。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form label-width="88px" class="runtime-form-margin">
        <el-form-item label="驳回意见">
          <el-input v-model="rejectDialog.form.comment" type="textarea" :rows="4" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferDialog.visible" title="转办任务" width="480px">
      <el-form label-width="88px">
        <el-form-item label="目标用户">
          <el-input v-model="transferDialog.form.toUser" placeholder="请输入接收任务的用户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确认转办</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import { resetForm as resetFormUtil } from '@/utils/junsong'
import {
  approveWorkflowTask,
  claimWorkflowTask,
  getWorkflowTaskDetail,
  listAppliedWorkflowTasks,
  listDoneWorkflowTasks,
  listTodoWorkflowTasks,
  rejectWorkflowTask,
  transferWorkflowTask,
  type WorkflowAppliedTaskRow,
  type WorkflowDoneTaskRow,
  type WorkflowTaskDetail,
  type WorkflowTodoTaskRow,
} from '@/api/workflow/task'
import { formatWorkflowDateTime, formatWorkflowDuration, mapWorkflowTaskTabStatus, resolveWorkflowBusinessTarget, safeWorkflowText } from '../shared/runtime'

type TaskTab = 'todo' | 'done' | 'applied'
type TaskRow = (WorkflowTodoTaskRow | WorkflowDoneTaskRow | WorkflowAppliedTaskRow) & Record<string, any>

const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const queryFormRef = ref()
const activeTab = ref<TaskTab>('todo')
const rows = ref<TaskRow[]>([])

const detailDrawer = reactive({
  visible: false,
  loading: false,
  data: null as WorkflowTaskDetail | null,
})

const approveDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  form: {
    comment: '',
    variablesText: '',
  },
})

const rejectDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  form: {
    comment: '',
  },
})

const transferDialog = reactive({
  visible: false,
  row: null as WorkflowTodoTaskRow | null,
  form: {
    toUser: '',
  },
})

const queryParams = reactive({
  keyword: '',
  operator: '',
  dateRange: [] as string[],
})

const filteredRows = computed(() => {
  return rows.value.filter((item) => {
    const keyword = queryParams.keyword.trim()
    const operator = queryParams.operator.trim()

    if (keyword) {
      const matched =
        String(item.taskName || item.processDefinitionName || '').includes(keyword)
        || String(item.processDefinitionKey || '').includes(keyword)
      if (!matched) return false
    }

    if (operator) {
      const matched =
        String(item.assignee || item.owner || '').includes(operator)
        || String(item.businessKey || '').includes(operator)
      if (!matched) return false
    }

    if (queryParams.dateRange?.length === 2) {
      const sourceTime = item.createTime || item.startTime || item.endTime
      const time = sourceTime ? new Date(sourceTime).getTime() : NaN
      const start = new Date(`${queryParams.dateRange[0]} 00:00:00`).getTime()
      const end = new Date(`${queryParams.dateRange[1]} 23:59:59`).getTime()
      if (!Number.isNaN(time) && (time < start || time > end)) return false
    }

    return true
  })
})

const overviewCards = computed(() => {
  const items = filteredRows.value
  return [
    {
      key: 'total',
      label: activeTab.value === 'applied' ? '我发起的流程数' : '当前任务数',
      value: items.length,
      hint: '基于当前 Tab 与筛选结果汇总',
    },
    {
      key: 'processable',
      label: activeTab.value === 'todo' ? '待处理任务' : activeTab.value === 'done' ? '已处理任务' : '运行中流程',
      value:
        activeTab.value === 'todo'
          ? items.length
          : activeTab.value === 'done'
            ? items.length
            : items.filter((item) => item.running === true || !item.endTime).length,
      hint: activeTab.value === 'todo' ? '可直接审批处理' : '用于快速定位进展',
    },
    {
      key: 'finished',
      label: activeTab.value === 'applied' ? '已结束流程' : '已分配任务',
      value:
        activeTab.value === 'applied'
          ? items.filter((item) => item.endTime).length
          : items.filter((item) => item.assignee || item.owner).length,
      hint: '帮助判断当前工作分布',
    },
    {
      key: 'recent',
      label: '近 7 天记录',
      value: items.filter((item) => {
        const sourceTime = item.createTime || item.startTime || item.endTime
        if (!sourceTime) return false
        return Date.now() - new Date(sourceTime).getTime() <= 1000 * 60 * 60 * 24 * 7
      }).length,
      hint: '观察最近一周流转活跃度',
    },
  ]
})

const taskVariableEntries = computed(() => {
  const variables = detailDrawer.data?.variables || {}
  return Object.entries(variables).map(([key, value]) => ({
    key,
    value: typeof value === 'object' ? JSON.stringify(value) : safeWorkflowText(value as any),
  }))
})

const rawVariablesText = computed(() => JSON.stringify(detailDrawer.data?.variables || {}, null, 2))

function canClaim(row: WorkflowTodoTaskRow) {
  return !row.assignee
}

function applyFilter() {
  rows.value = [...rows.value]
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.operator = ''
  queryParams.dateRange = []
  resetFormUtil(queryFormRef.value)
  applyFilter()
}

async function loadCurrentTab() {
  loading.value = true
  try {
    if (activeTab.value === 'todo') {
      const res = await listTodoWorkflowTasks()
      rows.value = res.data || []
      return
    }
    if (activeTab.value === 'done') {
      const res = await listDoneWorkflowTasks()
      rows.value = res.data || []
      return
    }
    const res = await listAppliedWorkflowTasks()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  resetQuery()
  loadCurrentTab()
}

async function openTaskDetail(row: TaskRow) {
  detailDrawer.visible = true
  detailDrawer.loading = true
  try {
    detailDrawer.data = await getWorkflowTaskDetail(row.taskId).then((res) => res.data)
  } finally {
    detailDrawer.loading = false
  }
}

function goToHistory(processInstanceId?: string) {
  if (!processInstanceId) return
  router.push({
    path: '/workflow/history',
    query: { processInstanceId },
  })
}

function canOpenBusiness(row?: Partial<TaskRow> | null) {
  return !!resolveWorkflowBusinessTarget(row?.processDefinitionKey, row?.businessKey)
}

function goToBusiness(row?: Partial<TaskRow> | null) {
  const target = resolveWorkflowBusinessTarget(row?.processDefinitionKey, row?.businessKey)
  if (!target) return
  router.push(target)
}

async function handleClaim(row: WorkflowTodoTaskRow) {
  await claimWorkflowTask(row.taskId)
  ElMessage.success('任务已认领')
  loadCurrentTab()
}

function openApproveDialog(row: WorkflowTodoTaskRow) {
  approveDialog.visible = true
  approveDialog.row = row
  approveDialog.form.comment = ''
  approveDialog.form.variablesText = ''
}

function openRejectDialog(row: WorkflowTodoTaskRow) {
  rejectDialog.visible = true
  rejectDialog.row = row
  rejectDialog.form.comment = ''
}

function openTransferDialog(row: WorkflowTodoTaskRow) {
  transferDialog.visible = true
  transferDialog.row = row
  transferDialog.form.toUser = ''
}

function parseVariablesInput(raw: string) {
  const text = raw.trim()
  if (!text) return undefined
  try {
    return JSON.parse(text)
  } catch {
    throw new Error('变量 JSON 格式不正确')
  }
}

async function submitApprove() {
  if (!approveDialog.row) return
  const variables = parseVariablesInput(approveDialog.form.variablesText)
  await approveWorkflowTask(approveDialog.row.taskId, {
    comment: approveDialog.form.comment || undefined,
    variables,
  })
  approveDialog.visible = false
  ElMessage.success('任务已审批通过')
  loadCurrentTab()
}

async function submitReject() {
  if (!rejectDialog.row) return
  await ElMessageBox.confirm(
    '当前后端驳回逻辑会终止整个流程实例，确认继续吗？',
    '驳回确认',
    {
      type: 'warning',
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
    },
  )
  await rejectWorkflowTask(rejectDialog.row.taskId, {
    comment: rejectDialog.form.comment || undefined,
  })
  rejectDialog.visible = false
  ElMessage.success('任务已驳回')
  loadCurrentTab()
}

async function submitTransfer() {
  if (!transferDialog.row) return
  if (!transferDialog.form.toUser.trim()) {
    ElMessage.warning('请输入目标用户名')
    return
  }
  await transferWorkflowTask(transferDialog.row.taskId, transferDialog.form.toUser.trim())
  transferDialog.visible = false
  ElMessage.success('任务已转办')
  loadCurrentTab()
}

onMounted(() => {
  loadCurrentTab()
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

.runtime-drawer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.runtime-form-margin {
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
