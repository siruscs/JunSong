<template>
  <div class="app-container review-task-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">复盘任务管理</h2>
      </div>
      <el-button type="primary" :icon="MagicStick" :loading="generateLoading" @click="handleGenerate">生成复盘任务</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
        <el-form-item label="门店" prop="deptIds">
          <el-select
            v-model="queryParams.deptIds"
            placeholder="请选择门店"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            style="width: 280px"
          >
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围" prop="dateRange">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待处理" name="PENDING" />
        <el-tab-pane label="处理中" name="IN_PROGRESS" />
        <el-tab-pane label="已完成" name="DONE" />
        <el-tab-pane label="已忽略" name="IGNORED" />
      </el-tabs>

      <el-table v-loading="loading" :data="taskList" stripe border style="width: 100%" empty-text="暂无复盘任务">
        <el-table-column label="门店" align="center" prop="deptName" min-width="120" />
        <el-table-column label="任务类型" align="center" prop="taskType" width="130">
          <template #default="scope">
            <span>{{ taskTypeLabel(scope.row.taskType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="严重级别" align="center" prop="severity" width="100">
          <template #default="scope">
            <el-tag :type="severityTagType(scope.row.severity)" size="small">
              {{ severityLabel(scope.row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="影响金额" align="center" prop="impactAmount" width="120">
          <template #default="scope">
            <span class="money-danger">&yen;{{ formatMoney(scope.row.impactAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="触发原因" align="center" prop="reason" min-width="180" show-overflow-tooltip />
        <el-table-column label="建议动作" align="center" prop="suggestion" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="small">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" align="center" prop="handlerName" width="100" />
        <el-table-column label="更新时间" align="center" prop="updateTime" width="170">
          <template #default="scope">
            <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" fixed="right" width="220">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'PENDING'"
              link
              type="primary"
              size="small"
              @click="handleMarkInProgress(scope.row)"
            >
              处理中
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING' || scope.row.status === 'IN_PROGRESS'"
              link
              type="success"
              size="small"
              @click="handleOpenDoneDialog(scope.row)"
            >
              完成
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING' || scope.row.status === 'IN_PROGRESS'"
              link
              type="info"
              size="small"
              @click="handleOpenIgnoreDialog(scope.row)"
            >
              忽略
            </el-button>
            <el-button
              v-if="scope.row.targetRoute"
              link
              type="warning"
              size="small"
              @click="handleNavigate(scope.row)"
            >
              跳转
            </el-button>
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
    </el-card>

    <el-dialog v-model="doneDialogVisible" title="标记完成" width="500px" append-to-body>
      <el-form ref="doneFormRef" :model="doneForm" :rules="doneRules" label-width="90px">
        <el-form-item label="处理说明" prop="handlerNote">
          <el-input
            v-model="doneForm.handlerNote"
            type="textarea"
            placeholder="请输入处理说明（至少5个字符）"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="doneLoading" @click="submitDone">确 定</el-button>
          <el-button @click="doneDialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="ignoreDialogVisible" title="标记忽略" width="500px" append-to-body>
      <el-form ref="ignoreFormRef" :model="ignoreForm" :rules="ignoreRules" label-width="90px">
        <el-form-item label="忽略原因" prop="ignoreReason">
          <el-input
            v-model="ignoreForm.ignoreReason"
            type="textarea"
            placeholder="请输入忽略原因（至少5个字符）"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="ignoreLoading" @click="submitIgnore">确 定</el-button>
          <el-button @click="ignoreDialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Refresh, Search } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/junsong'
import {
  generateReviewTasks,
  listReviewTasks,
  markDone,
  markIgnored,
  markInProgress,
} from '@/api/finance/reviewTask'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const generateLoading = ref(false)
const total = ref(0)
const taskList = ref<any[]>([])
const deptOptions = ref<any[]>([])
const activeTab = ref('')
const dateRange = ref<string[]>([])

const queryFormRef = ref()
const doneFormRef = ref()
const ignoreFormRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  deptIds: [] as number[],
  status: undefined as string | undefined,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
})

const doneDialogVisible = ref(false)
const doneLoading = ref(false)
const currentDoneTask = ref<any>(null)
const doneForm = reactive({
  handlerNote: '',
})

const ignoreDialogVisible = ref(false)
const ignoreLoading = ref(false)
const currentIgnoreTask = ref<any>(null)
const ignoreForm = reactive({
  ignoreReason: '',
})

const taskTypeMap: Record<string, string> = {
  SALES_DROP: '销售下滑',
  EXPENSE_SPIKE: '费用异常',
  PROFIT_RATE_DROP: '利润率偏低',
  PENDING_VERIFY: '待核销费用偏高',
  PROFIT_SHARE_EXCEPTION: '分润异常',
  MEMBER_CONTRIBUTION_DROP: '会员贡献下降',
}

const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待处理', type: 'warning' },
  IN_PROGRESS: { label: '处理中', type: '' },
  DONE: { label: '已完成', type: 'success' },
  IGNORED: { label: '已忽略', type: 'info' },
}

const severityMap: Record<string, { label: string; type: string }> = {
  HIGH: { label: '高', type: 'danger' },
  MEDIUM: { label: '中', type: 'warning' },
  LOW: { label: '低', type: 'info' },
}

const doneRules = {
  handlerNote: [
    { required: true, message: '请输入处理说明', trigger: 'blur' },
    { min: 5, message: '处理说明至少5个字符', trigger: 'blur' },
  ],
}

const ignoreRules = {
  ignoreReason: [
    { required: true, message: '请输入忽略原因', trigger: 'blur' },
    { min: 5, message: '忽略原因至少5个字符', trigger: 'blur' },
  ],
}

function taskTypeLabel(type: string) {
  return taskTypeMap[type] || type || '-'
}

function statusLabel(status: string) {
  return statusMap[status]?.label || status || '-'
}

function statusTagType(status: string) {
  return (statusMap[status]?.type || 'info') as any
}

function severityLabel(severity: string) {
  return severityMap[severity]?.label || severity || '-'
}

function severityTagType(severity: string) {
  return (severityMap[severity]?.type || 'info') as any
}

function formatMoney(value: any) {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed.toFixed(2) : '0.00'
}

function getDeptOptions() {
  deptOptions.value = userStore.depts || []
}

function handleDateRangeChange(val: string[] | null) {
  if (val && val.length === 2) {
    queryParams.startTime = val[0]
    queryParams.endTime = val[1]
  } else {
    queryParams.startTime = undefined
    queryParams.endTime = undefined
  }
}

function getList() {
  loading.value = true
  const params: Record<string, any> = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  if (queryParams.deptIds.length > 0) {
    params.deptIds = queryParams.deptIds.join(',')
  }
  if (queryParams.status) {
    params.status = queryParams.status
  }
  if (queryParams.startTime) {
    params.startTime = queryParams.startTime
  }
  if (queryParams.endTime) {
    params.endTime = queryParams.endTime
  }
  listReviewTasks(params)
    .then((res: any) => {
      taskList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryFormRef.value?.resetFields?.()
  dateRange.value = []
  queryParams.deptIds = []
  queryParams.status = undefined
  queryParams.startTime = undefined
  queryParams.endTime = undefined
  activeTab.value = ''
  handleQuery()
}

function handleTabChange(tab: string) {
  queryParams.status = tab || undefined
  queryParams.pageNum = 1
  getList()
}

function handleMarkInProgress(row: any) {
  ElMessageBox.confirm('是否将该任务标记为处理中？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => markInProgress(row.taskId))
    .then(() => {
      ElMessage.success('已标记为处理中')
      getList()
    })
    .catch(() => {})
}

function handleOpenDoneDialog(row: any) {
  currentDoneTask.value = row
  doneForm.handlerNote = ''
  doneDialogVisible.value = true
}

function submitDone() {
  if (!doneFormRef.value || !currentDoneTask.value) return
  doneFormRef.value.validate((valid: boolean) => {
    if (!valid) return
    doneLoading.value = true
    markDone(currentDoneTask.value.taskId, { handlerNote: doneForm.handlerNote })
      .then(() => {
        ElMessage.success('已标记为完成')
        doneDialogVisible.value = false
        getList()
      })
      .finally(() => {
        doneLoading.value = false
      })
  })
}

function handleOpenIgnoreDialog(row: any) {
  currentIgnoreTask.value = row
  ignoreForm.ignoreReason = ''
  ignoreDialogVisible.value = true
}

function submitIgnore() {
  if (!ignoreFormRef.value || !currentIgnoreTask.value) return
  ignoreFormRef.value.validate((valid: boolean) => {
    if (!valid) return
    ignoreLoading.value = true
    markIgnored(currentIgnoreTask.value.taskId, { ignoreReason: ignoreForm.ignoreReason })
      .then(() => {
        ElMessage.success('已标记为忽略')
        ignoreDialogVisible.value = false
        getList()
      })
      .finally(() => {
        ignoreLoading.value = false
      })
  })
}

function handleNavigate(row: any) {
  if (row.targetRoute) {
    try {
      const query = row.targetParams ? JSON.parse(row.targetParams) : {}
      router.push({ path: row.targetRoute, query })
    } catch {
      router.push({ path: row.targetRoute })
    }
  }
}

function handleGenerate() {
  ElMessageBox.confirm('是否根据当前筛选条件生成复盘任务？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      generateLoading.value = true
      const data: Record<string, any> = {}
      if (queryParams.deptIds.length > 0) {
        data.deptIds = queryParams.deptIds
      }
      if (queryParams.startTime) {
        data.startTime = queryParams.startTime
      }
      if (queryParams.endTime) {
        data.endTime = queryParams.endTime
      }
      return generateReviewTasks(data)
    })
    .then(() => {
      ElMessage.success('复盘任务生成成功')
      getList()
    })
    .catch(() => {})
    .finally(() => {
      generateLoading.value = false
    })
}

onMounted(() => {
  getDeptOptions()
  getList()
})
</script>

<style scoped>
.review-task-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 6px;
  color: #18202f;
  font-size: 22px;
  font-weight: 700;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.filter-card :deep(.el-card__body) {
  padding: 14px 16px 2px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 12px;
}

.table-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.table-card :deep(.el-card__body) {
  padding: 0 16px 16px;
}

.table-card :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.table-card :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.money-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
