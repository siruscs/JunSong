<template>
  <div class="app-container operating-task-page">
    <!-- 待办计数卡 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--pending" @click="filterByStatus('PENDING')">
          <div class="stat-card__inner">
            <el-icon :size="28"><Bell /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ pendingCount }}</div>
              <div class="stat-card__label">我的待办</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--in-progress" @click="filterByStatus('IN_PROGRESS')">
          <div class="stat-card__inner">
            <el-icon :size="28"><Loading /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ inProgressCount }}</div>
              <div class="stat-card__label">处理中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--overdue" @click="filterByOverdue()">
          <div class="stat-card__inner">
            <el-icon :size="28"><Warning /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ overdueCount }}</div>
              <div class="stat-card__label">逾期任务</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--done" @click="filterByStatus('DONE')">
          <div class="stat-card__inner">
            <el-icon :size="28"><CircleCheck /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ doneCount }}</div>
              <div class="stat-card__label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选表单 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="门店" prop="deptId">
        <el-select
          v-model="queryParams.deptId"
          placeholder="全部门店"
          clearable
          style="width: 200px"
          :disabled="deptOptions.length === 0"
        >
          <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 160px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="IN_PROGRESS" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已重开" value="REOPENED" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-select v-model="queryParams.priority" placeholder="全部优先级" clearable style="width: 160px">
          <el-option label="紧急" value="URGENT" />
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
      </el-form-item>
      <el-form-item label="负责人" prop="assigneeId">
        <el-select v-model="queryParams.assigneeId" placeholder="全部负责人" clearable filterable style="width: 180px">
          <el-option v-for="u in assigneeOptions" :key="u.userId" :label="u.nickName || u.userName" :value="u.userId" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源模块" prop="sourceModule">
        <el-select v-model="queryParams.sourceModule" placeholder="全部来源" clearable style="width: 160px">
          <el-option label="财务" value="FINANCE" />
          <el-option label="库存" value="STOCK" />
          <el-option label="会员" value="MEMBER" />
          <el-option label="工作流" value="WORKFLOW" />
          <el-option label="系统" value="SYSTEM" />
        </el-select>
      </el-form-item>
      <el-form-item label="截止时间" prop="dueRange">
        <el-date-picker
          v-model="dueRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button :icon="Refresh" @click="getList">刷新</el-button>
    </RightToolbar>

    <!-- 任务列表 -->
    <el-table v-loading="loading" :data="taskList" stripe border style="width: 100%" empty-text="暂无经营任务">
      <el-table-column label="标题" min-width="240" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <span class="task-title" :class="{ 'task-overdue': isOverdue(row) }">{{ row.title }}</span>
          <el-tag v-if="isOverdue(row)" type="danger" size="small" effect="dark" style="margin-left: 6px">逾期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="门店" align="center" prop="deptName" min-width="120" />
      <el-table-column label="来源" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="sourceTagType(row.sourceModule)" size="small">{{ sourceLabel(row.sourceModule) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" align="center" width="90">
        <template #default="{ row }">
          <el-tag :type="priorityTagType(row.priority)" size="small" effect="dark">
            {{ priorityLabel(row.priority) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="负责人" align="center" prop="assigneeName" width="120">
        <template #default="{ row }">
          {{ row.assigneeName || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="截止时间" align="center" width="160">
        <template #default="{ row }">
          <span :class="{ 'task-overdue': isOverdue(row) }">{{ formatTime(row.dueTime) || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="影响金额" align="center" width="120">
        <template #default="{ row }">
          <span v-if="row.impactAmount != null" class="money-danger">&yen;{{ formatMoney(row.impactAmount) }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center" class-name="small-padding fixed-width" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button
            v-if="canClaim(row) && hasPermi('system:operatingTask:claim')"
            link
            type="success"
            @click="handleClaim(row)"
          >认领</el-button>
          <el-button
            v-if="canComplete(row) && hasPermi('system:operatingTask:complete')"
            link
            type="primary"
            @click="handleComplete(row)"
          >完成</el-button>
          <el-button
            v-if="canReject(row) && hasPermi('system:operatingTask:reject')"
            link
            type="warning"
            @click="handleReject(row)"
          >驳回</el-button>
          <el-button
            v-if="canReopen(row) && hasPermi('system:operatingTask:reopen')"
            link
            type="primary"
            @click="handleReopen(row)"
          >重开</el-button>
          <el-button
            v-if="row.sourceRoute"
            link
            type="info"
            @click="handleViewSource(row)"
          >查看来源</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="任务详情" size="600px" direction="rtl">
      <div v-loading="detailLoading">
        <div v-if="currentTask">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="任务ID">{{ currentTask.taskId }}</el-descriptions-item>
            <el-descriptions-item label="标题">{{ currentTask.title }}</el-descriptions-item>
            <el-descriptions-item label="来源">
              {{ sourceLabel(currentTask.sourceModule) }} / {{ currentTask.sourceType }}
            </el-descriptions-item>
            <el-descriptions-item label="门店">{{ currentTask.deptName || currentTask.deptId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="priorityTagType(currentTask.priority)" size="small" effect="dark">
                {{ priorityLabel(currentTask.priority) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(currentTask.status)" size="small">{{ statusLabel(currentTask.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="负责人">{{ currentTask.assigneeName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="发生时间">{{ formatTime(currentTask.occurTime) || '—' }}</el-descriptions-item>
            <el-descriptions-item label="截止时间">
              <span :class="{ 'task-overdue': isOverdue(currentTask) }">
                {{ formatTime(currentTask.dueTime) || '—' }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="影响金额">
              <span v-if="currentTask.impactAmount != null" class="money-danger">
                &yen;{{ formatMoney(currentTask.impactAmount) }}
              </span>
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="currentTask.handlerNote" label="处理备注">
              {{ currentTask.handlerNote }}
            </el-descriptions-item>
            <el-descriptions-item v-if="currentTask.rejectReason" label="驳回原因">
              {{ currentTask.rejectReason }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(currentTask.createTime) || '—' }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-actions">
            <el-button
              v-if="canClaim(currentTask) && hasPermi('system:operatingTask:claim')"
              type="success"
              @click="handleClaim(currentTask)"
            >认领</el-button>
            <el-button
              v-if="canComplete(currentTask) && hasPermi('system:operatingTask:complete')"
              type="primary"
              @click="handleComplete(currentTask)"
            >完成</el-button>
            <el-button
              v-if="canReject(currentTask) && hasPermi('system:operatingTask:reject')"
              type="warning"
              @click="handleReject(currentTask)"
            >驳回</el-button>
            <el-button
              v-if="canReopen(currentTask) && hasPermi('system:operatingTask:reopen')"
              type="primary"
              @click="handleReopen(currentTask)"
            >重开</el-button>
            <el-button
              v-if="currentTask.sourceRoute"
              type="info"
              @click="handleViewSource(currentTask)"
            >查看来源单据</el-button>
          </div>

          <!-- 操作日志 -->
          <h4 class="log-title">操作轨迹</h4>
          <el-timeline v-if="taskLogs.length > 0">
            <el-timeline-item
              v-for="log in taskLogs"
              :key="log.logId"
              :timestamp="formatTime(log.createTime)"
              :type="actionTagType(log.action)"
            >
              <strong>{{ actionLabel(log.action) }}</strong>
              <span v-if="log.operatorName"> · {{ log.operatorName }}</span>
              <div v-if="log.note" class="log-note">{{ log.note }}</div>
              <div v-if="log.oldStatus && log.newStatus" class="log-status">
                {{ statusLabel(log.oldStatus) }} → {{ statusLabel(log.newStatus) }}
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无操作记录" :image-size="80" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Loading, Warning, CircleCheck, Search, Refresh } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/junsong'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { listUser } from '@/api/system/user'
import {
  listOperatingTask,
  getOperatingTask,
  listOperatingTaskLogs,
  getOperatingTaskPendingCount,
  claimOperatingTask,
  completeOperatingTask,
  rejectOperatingTask,
  reopenOperatingTask,
  type OperatingTask,
  type OperatingTaskLog,
} from '@/api/system/operatingTask'

const router = useRouter()
const userStore = useUserStore()
const { hasPermi } = useAuth()

const loading = ref(false)
const taskList = ref<OperatingTask[]>([])
const total = ref(0)
const showSearch = ref(true)
const deptOptions = ref<any[]>([])
const assigneeOptions = ref<any[]>([])
const dueRange = ref<string[]>([])

const pendingCount = ref(0)
const inProgressCount = ref(0)
const overdueCount = ref(0)
const doneCount = ref(0)

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentTask = ref<OperatingTask | null>(null)
const taskLogs = ref<OperatingTaskLog[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  assigneeId: undefined as number | undefined,
  sourceModule: '',
  sourceType: '',
  priority: '',
  deptId: undefined as number | undefined,
})

const now = Date.now()

/** 逾期判定：dueTime 已过且状态为待办/处理中/重开 */
function isOverdue(row: OperatingTask): boolean {
  if (!row.dueTime) return false
  const dueMs = new Date(row.dueTime).getTime()
  if (isNaN(dueMs) || dueMs >= now) return false
  return ['PENDING', 'IN_PROGRESS', 'REOPENED'].includes(row.status)
}

function canClaim(row: OperatingTask): boolean {
  return row.status === 'PENDING'
}
function canComplete(row: OperatingTask): boolean {
  return ['IN_PROGRESS', 'REOPENED'].includes(row.status)
}
function canReject(row: OperatingTask): boolean {
  return ['IN_PROGRESS', 'REOPENED'].includes(row.status)
}
function canReopen(row: OperatingTask): boolean {
  return ['DONE', 'REJECTED'].includes(row.status)
}

async function getList() {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] == null) delete params[k]
    })
    // 截止时间范围筛选（前端组装为单个时间字段，后端如不支持仅作展示用，不影响查询）
    if (dueRange.value && dueRange.value.length === 2) {
      params.dueTimeStart = dueRange.value[0]
      params.dueTimeEnd = dueRange.value[1]
    }
    const res: any = await listOperatingTask(params)
    taskList.value = res.rows || []
    total.value = res.total || 0
    refreshCounts()
  } finally {
    loading.value = false
  }
}

/** 刷新统计卡片：基于当前列表数据 + 后端 pendingCount */
async function refreshCounts() {
  try {
    const cntRes: any = await getOperatingTaskPendingCount()
    pendingCount.value = Number(cntRes.data ?? cntRes ?? 0)
  } catch {
    pendingCount.value = 0
  }
  inProgressCount.value = taskList.value.filter((t) => t.status === 'IN_PROGRESS').length
  doneCount.value = taskList.value.filter((t) => t.status === 'DONE').length
  overdueCount.value = taskList.value.filter((t) => isOverdue(t)).length
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.status = ''
  queryParams.assigneeId = undefined
  queryParams.sourceModule = ''
  queryParams.sourceType = ''
  queryParams.priority = ''
  queryParams.deptId = undefined
  dueRange.value = []
  handleQuery()
}

function filterByStatus(status: string) {
  queryParams.status = status
  handleQuery()
}

function filterByOverdue() {
  // 逾期不能直接通过后端筛选，回到全部待办状态
  queryParams.status = 'PENDING'
  handleQuery()
}

async function handleDetail(row: OperatingTask) {
  detailVisible.value = true
  detailLoading.value = true
  currentTask.value = row
  taskLogs.value = []
  try {
    const [detailRes, logsRes] = await Promise.all([
      getOperatingTask(row.taskId),
      listOperatingTaskLogs(row.taskId),
    ])
    currentTask.value = (detailRes as any).data || row
    taskLogs.value = (logsRes as any).data || []
  } catch (e: any) {
    // 403/404 等错误由 request 拦截器统一提示
  } finally {
    detailLoading.value = false
  }
}

async function handleClaim(row: OperatingTask) {
  try {
    await ElMessageBox.confirm(`确认认领任务「${row.title}」？`, '认领确认', {
      confirmButtonText: '认领',
      cancelButtonText: '取消',
      type: 'info',
    })
  } catch {
    return
  }
  try {
    await claimOperatingTask(row.taskId)
    ElMessage.success('认领成功')
    await getList()
    if (currentTask.value && currentTask.value.taskId === row.taskId) {
      await handleDetail(currentTask.value)
    }
  } catch (e: any) {
    // 错误由拦截器处理；状态冲突提示后端返回的「任务状态已变更，请刷新」
  }
}

async function handleComplete(row: OperatingTask) {
  let handlerNote = ''
  try {
    const result = await ElMessageBox.prompt('请输入处理备注', '完成任务', {
      confirmButtonText: '完成',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入处理说明（必填）',
      inputValidator: (val) => (val && val.trim().length > 0) || '处理备注不能为空',
    })
    handlerNote = result.value
  } catch {
    return
  }
  try {
    await completeOperatingTask(row.taskId, handlerNote)
    ElMessage.success('任务已完成')
    await getList()
    if (currentTask.value && currentTask.value.taskId === row.taskId) {
      await handleDetail(currentTask.value)
    }
  } catch (e: any) {
    // 状态冲突/重复提交由拦截器提示
  }
}

async function handleReject(row: OperatingTask) {
  let rejectReason = ''
  try {
    const result = await ElMessageBox.prompt('请输入驳回原因', '驳回任务', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入驳回原因（必填）',
      inputValidator: (val) => (val && val.trim().length > 0) || '驳回原因不能为空',
    })
    rejectReason = result.value
  } catch {
    return
  }
  try {
    await rejectOperatingTask(row.taskId, rejectReason)
    ElMessage.success('任务已驳回')
    await getList()
    if (currentTask.value && currentTask.value.taskId === row.taskId) {
      await handleDetail(currentTask.value)
    }
  } catch (e: any) {
    // 错误由拦截器处理
  }
}

async function handleReopen(row: OperatingTask) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('请输入重开原因', '重开任务', {
      confirmButtonText: '重开',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入重开原因（必填）',
      inputValidator: (val) => (val && val.trim().length > 0) || '重开原因不能为空',
    })
    reason = result.value
  } catch {
    return
  }
  try {
    await reopenOperatingTask(row.taskId, reason)
    ElMessage.success('任务已重开')
    await getList()
    if (currentTask.value && currentTask.value.taskId === row.taskId) {
      await handleDetail(currentTask.value)
    }
  } catch (e: any) {
    // 错误由拦截器处理
  }
}

function handleViewSource(row: OperatingTask) {
  if (!row.sourceRoute) {
    ElMessage.warning('该任务未配置来源路由')
    return
  }
  // 来源路由已由后端权威返回，前端直接跳转
  router.push(row.sourceRoute)
}

function formatTime(time?: string) {
  return time ? parseTime(new Date(time), '{y}-{m}-{d} {h}:{i}') : ''
}

function formatMoney(val: any) {
  const n = Number(val || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function priorityLabel(p: string): string {
  const map: Record<string, string> = { URGENT: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[p] || p || '—'
}
function priorityTagType(p: string): any {
  const map: Record<string, string> = { URGENT: 'danger', HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[p] || 'info'
}
function statusLabel(s: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理', IN_PROGRESS: '处理中', DONE: '已完成', REJECTED: '已驳回', REOPENED: '已重开',
  }
  return map[s] || s || '—'
}
function statusTagType(s: string): any {
  const map: Record<string, string> = {
    PENDING: 'warning', IN_PROGRESS: 'primary', DONE: 'success', REJECTED: 'info', REOPENED: 'warning',
  }
  return map[s] || 'info'
}
function sourceLabel(s: string): string {
  const map: Record<string, string> = {
    FINANCE: '财务', STOCK: '库存', MEMBER: '会员', WORKFLOW: '工作流', SYSTEM: '系统',
  }
  return map[s] || s || '—'
}
function sourceTagType(s: string): any {
  const map: Record<string, string> = {
    FINANCE: 'warning', STOCK: 'danger', MEMBER: 'success', WORKFLOW: 'primary', SYSTEM: 'info',
  }
  return map[s] || 'info'
}
function actionLabel(a: string): string {
  const map: Record<string, string> = {
    CREATE: '创建', CLAIM: '认领', COMPLETE: '完成', REJECT: '驳回', REOPEN: '重开',
  }
  return map[a] || a
}
function actionTagType(a: string): any {
  const map: Record<string, string> = {
    CREATE: 'primary', CLAIM: 'success', COMPLETE: 'success', REJECT: 'warning', REOPEN: 'warning',
  }
  return map[a] || 'info'
}

async function loadDeptOptions() {
  // 门店选项来自用户授权门店列表（后端权威），前端不做租户/部门过滤
  deptOptions.value = userStore.depts || []
}

async function loadAssigneeOptions() {
  // 负责人候选来自用户列表（后端权限过滤），仅在需要时加载
  try {
    const res: any = await listUser({ pageNum: 1, pageSize: 100 })
    assigneeOptions.value = res.rows || []
  } catch {
    assigneeOptions.value = []
  }
}

onMounted(() => {
  loadDeptOptions()
  loadAssigneeOptions()
  getList()
})
</script>

<style scoped>
.stat-cards { margin-bottom: 16px; }
.stat-card { cursor: pointer; transition: transform 0.15s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-card__inner { display: flex; align-items: center; gap: 12px; }
.stat-card__value { font-size: 24px; font-weight: 600; line-height: 1.2; }
.stat-card__label { font-size: 13px; color: #909399; }
.stat-card--pending { background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%); }
.stat-card--in-progress { background: linear-gradient(135deg, #f0f9ff 0%, #c6e2ff 100%); }
.stat-card--overdue { background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%); }
.stat-card--done { background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%); }

.task-title { font-weight: 500; }
.task-overdue { color: #f56c6c; font-weight: 600; }
.money-danger { color: #f56c6c; font-weight: 600; }

.detail-actions { margin-top: 20px; padding-top: 16px; border-top: 1px solid #ebeef5; display: flex; gap: 8px; flex-wrap: wrap; }
.log-title { margin-top: 24px; margin-bottom: 12px; color: #303133; }
.log-note { margin-top: 4px; color: #606266; font-size: 13px; }
.log-status { margin-top: 2px; color: #909399; font-size: 12px; }
</style>
