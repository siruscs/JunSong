<template>
  <div class="app-container stocktake-detail-page" v-loading="loading">
    <!-- 顶部 -->
    <div class="detail-header">
      <el-button :icon="ArrowLeft" @click="handleBack">返回</el-button>
      <div class="header-title">
        <span class="take-no">{{ detail?.takeNo || '-' }}</span>
        <el-tag v-if="detail" :type="statusTagType(detail.status)" size="default">{{ statusLabel(detail.status) }}</el-tag>
      </div>
    </div>

    <template v-if="detail">
      <!-- 操作按钮区 -->
      <div class="action-bar">
        <el-button
          v-if="detail.status === 'DRAFT' || detail.status === 'COUNTING'"
          type="primary"
          size="small"
          @click="openAssignDialog"
          v-hasPermi="['finance:stocktake:assign']"
        >分配</el-button>
        <el-button
          v-if="detail.status === 'DRAFT'"
          type="success"
          size="small"
          :loading="actionLoading"
          @click="handleStart"
        >启动盘点</el-button>
        <el-button
          v-if="detail.status === 'COUNTING'"
          type="success"
          size="small"
          :loading="actionLoading"
          @click="handleSubmit"
          v-hasPermi="['finance:stocktake:submit']"
        >提交</el-button>
        <el-button
          v-if="detail.status === 'SUBMITTED' || detail.status === 'RECOUNTING'"
          type="warning"
          size="small"
          @click="openApproveDialog"
          v-hasPermi="['finance:stocktake:approve']"
        >审批</el-button>
        <el-button
          v-if="detail.status === 'APPROVED'"
          type="success"
          size="small"
          :loading="actionLoading"
          @click="handlePost"
          v-hasPermi="['finance:stocktake:post']"
        >过账</el-button>
        <el-button
          v-if="canCancel"
          type="danger"
          plain
          size="small"
          :loading="actionLoading"
          @click="handleCancel"
          v-hasPermi="['finance:stocktake:add']"
        >取消</el-button>
        <el-button
          v-if="detail.status === 'POSTED'"
          type="danger"
          size="small"
          @click="openReverseDialog"
          v-hasPermi="['finance:stocktake:reverse']"
        >冲销</el-button>
      </div>

      <!-- 头部信息卡片 -->
      <el-card shadow="never" class="info-card">
        <el-alert
          v-if="detail.status === 'DRAFT' || detail.status === 'COUNTING'"
          title="盘点流程：先启动盘点，再在下方“实际数量”中录入现场盘点结果；保存后提交，审批通过后点击过账，系统才会正式生成库存变更流水。"
          type="info"
          :closable="false"
          show-icon
          class="stocktake-flow-tip"
        />
        <el-descriptions :column="3" border>
          <el-descriptions-item label="门店">{{ getDeptName(detail.deptId) }}</el-descriptions-item>
          <el-descriptions-item label="盘点人">{{ detail.counterUserName || userLabel(detail.counterUserId) }}</el-descriptions-item>
          <el-descriptions-item label="复盘人">{{ detail.recountUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="冻结时间">{{ formatTime(detail.freezeTime) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(detail.submittedTime) }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ formatTime(detail.approvedTime) }}</el-descriptions-item>
          <el-descriptions-item label="过账时间">{{ formatTime(detail.postedTime) }}</el-descriptions-item>
          <el-descriptions-item label="冲销时间">{{ formatTime(detail.reversedTime) }}</el-descriptions-item>
          <el-descriptions-item label="冲销原因">{{ detail.reversalReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 行项目表格 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <span class="card-title">行项目</span>
        </template>
        <stocktake-items-table
          ref="itemsTableRef"
          :items="detail.items"
          :hide-expected="detail.hideExpected"
          :status="detail.status"
          :stocktake-id="detail.stocktakeId"
          @count="handleCount"
          @recount="handleRecount"
        />
      </el-card>

      <!-- 历史时间线 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <span class="card-title">历史记录</span>
        </template>
        <el-timeline v-if="detail.histories && detail.histories.length > 0">
          <el-timeline-item
            v-for="h in detail.histories"
            :key="h.historyId"
            :timestamp="formatTime(h.createTime)"
            placement="top"
          >
            <div class="history-item">
              <span class="history-action">{{ h.action }}</span>
              <span class="history-transition" v-if="h.fromStatus || h.toStatus">
                {{ statusLabel(h.fromStatus || '') }} → {{ statusLabel(h.toStatus) }}
              </span>
              <span class="history-operator">操作人：{{ h.operator || '-' }}</span>
              <div class="history-comment" v-if="h.comment">备注：{{ h.comment }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无历史记录" :image-size="80" />
      </el-card>
    </template>

    <!-- 分配对话框 -->
    <el-dialog title="分配盘点人/复盘人" v-model="assignOpen" width="480px" append-to-body>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="100px">
        <el-form-item label="盘点人" prop="counterUserIdInput">
          <el-select v-model="assignForm.counterUserIdInput" filterable placeholder="选择盘点人" style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.userId" :label="userOptionLabel(u)" :value="String(u.userId)" />
          </el-select>
        </el-form-item>
        <el-form-item label="复盘人" prop="recountUserIdInput">
          <el-select v-model="assignForm.recountUserIdInput" filterable clearable placeholder="选择复盘人（超阈值时必填）" style="width: 100%">
            <el-option v-for="u in independentRecountUsers" :key="u.userId" :label="userOptionLabel(u)" :value="String(u.userId)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignOpen = false">取 消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitAssign">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog title="审批盘点" v-model="approveOpen" width="480px" append-to-body>
      <el-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-width="100px">
        <el-form-item label="审批决定" prop="decision">
          <el-radio-group v-model="approveForm.decision">
            <el-radio value="APPROVE">通过</el-radio>
            <el-radio value="REJECT">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="comment">
          <el-input v-model="approveForm.comment" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveOpen = false">取 消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitApprove">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 冲销对话框 -->
    <el-dialog title="整单冲销" v-model="reverseOpen" width="520px" append-to-body>
      <el-alert type="warning" :closable="false" show-icon class="reverse-alert">
        冲销将生成红字库存与成本台账，撤回本次盘点过账影响。该操作不可撤销。
      </el-alert>
      <el-form ref="reverseFormRef" :model="reverseForm" :rules="reverseRules" label-width="100px">
        <el-form-item label="冲销原因" prop="reason">
          <el-input v-model="reverseForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入冲销原因（必填）" />
        </el-form-item>
        <el-form-item label="幂等键" prop="idempotencyKey">
          <el-input v-model="reverseForm.idempotencyKey" placeholder="幂等键（必填，重复提交时复用同一值）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reverseOpen = false">取 消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="submitReverse">确认冲销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/junsong'
import { useUserStore } from '@/stores/user'
import { listUser } from '@/api/system/user'
import StocktakeItemsTable from './components/StocktakeItemsTable.vue'
import {
  approveStocktake,
  assignCounter,
  cancelStocktake,
  countItem,
  getStocktakeDetail,
  postStocktake,
  recountItem,
  reverseStocktake,
  startStocktake,
  submitStocktake,
  type StocktakeApprovalRequest,
  type StocktakeAssignRequest,
  type StocktakeCountRequest,
  type StocktakeDetailVO,
  type StocktakeRecountRequest,
  type StocktakeReverseRequest,
} from '@/api/finance/stocktake'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const actionLoading = ref(false)
const detail = ref<StocktakeDetailVO | null>(null)
const userOptions = ref<any[]>([])
const independentRecountUsers = computed(() => userOptions.value.filter((u) => String(u.userId) !== String(detail.value?.counterUserId)))
const itemsTableRef = ref<InstanceType<typeof StocktakeItemsTable>>()

const assignOpen = ref(false)
const approveOpen = ref(false)
const reverseOpen = ref(false)

const assignFormRef = ref<FormInstance>()
const approveFormRef = ref<FormInstance>()
const reverseFormRef = ref<FormInstance>()

const stocktakeId = computed(() => Number(route.params.id))

const deptOptions = computed(() => userStore.depts || [])

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '盘点中', value: 'COUNTING' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '复盘中', value: 'RECOUNTING' },
  { label: '已审批', value: 'APPROVED' },
  { label: '已过账', value: 'POSTED' },
  { label: '已冲销', value: 'REVERSED' },
  { label: '已取消', value: 'CANCELLED' },
]

const assignForm = reactive({
  counterUserIdInput: '',
  recountUserIdInput: '',
})
const assignRules = {
  counterUserIdInput: [{ required: true, message: '请输入盘点人ID', trigger: 'blur' }],
}

const approveForm = reactive<{ decision: string; comment: string }>({
  decision: 'APPROVE',
  comment: '',
})
const approveRules = {
  decision: [{ required: true, message: '请选择审批决定', trigger: 'change' }],
}

const reverseForm = reactive<{ reason: string; idempotencyKey: string }>({
  reason: '',
  idempotencyKey: '',
})
const reverseRules = {
  reason: [{ required: true, message: '请输入冲销原因', trigger: 'blur' }],
  idempotencyKey: [{ required: true, message: '请输入幂等键', trigger: 'blur' }],
}

const canCancel = computed(() => {
  const s = detail.value?.status
  return s === 'DRAFT' || s === 'COUNTING' || s === 'SUBMITTED' || s === 'RECOUNTING' || s === 'APPROVED'
})

function statusLabel(value: string) {
  return statusOptions.find((o) => o.value === value)?.label || value || '-'
}

function statusTagType(value: string) {
  const map: Record<string, string> = {
    DRAFT: 'info',
    COUNTING: 'warning',
    SUBMITTED: '',
    RECOUNTING: 'warning',
    APPROVED: 'success',
    POSTED: 'success',
    REVERSED: 'danger',
    CANCELLED: 'info',
  }
  return map[value] || 'info'
}

function getDeptName(deptId: number) {
  const dept = deptOptions.value.find((d: any) => d.deptId === deptId)
  return dept ? dept.deptName : deptId || '-'
}

function formatTime(value: string | null) {
  if (!value) return '-'
  return parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-'
}

function createIdempotencyKey() {
  if (globalThis.crypto && typeof globalThis.crypto.randomUUID === 'function') {
    return globalThis.crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function loadDetail() {
  if (!stocktakeId.value || Number.isNaN(stocktakeId.value)) {
    ElMessage.error('无效的盘点任务ID')
    return
  }
  loading.value = true
    getStocktakeDetail(stocktakeId.value)
    .then((res: any) => {
      // 后端详情返回 { stocktake, items, history, hideExpected }，
      // 页面使用扁平头表对象；这里统一转换，避免头部字段和状态为空。
      const payload = res.data?.data || res.data || {}
      const header = payload.stocktake || payload
      detail.value = {
        ...header,
        items: payload.items || header.items || [],
        histories: payload.history || payload.histories || header.histories || [],
        hideExpected: Boolean(payload.hideExpected),
      } as StocktakeDetailVO
    })
    .finally(() => {
      loading.value = false
    })
}

function userOptionLabel(user: any) {
  return user.nickName ? `${user.nickName}（${user.userName}）` : user.userName || `用户 ${user.userId}`
}

function userLabel(userId: number | null | undefined) {
  if (!userId) return '-'
  const user = userOptions.value.find((u) => String(u.userId) === String(userId))
  if (user) return userOptionLabel(user)
  if (String(userStore.id) === String(userId)) return `${userStore.nickName || userStore.name}（${userStore.name}）`
  return `用户 ${userId}`
}

function loadUserOptions() {
  listUser({ pageNum: 1, pageSize: 200 })
    .then((res: any) => {
      userOptions.value = res.rows || res.data?.rows || []
    })
    .catch(() => {
      userOptions.value = []
    })
}

function handleBack() {
  router.push('/finance/stocktake/index')
}

// ---- 行录入 ----
function handleCount(payload: { itemId: number; request: StocktakeCountRequest }) {
  countItem(stocktakeId.value, payload.itemId, payload.request)
    .then(() => {
      ElMessage.success('行录入已保存')
      return loadDetail()
    })
    .catch(() => {
      itemsTableRef.value?.clearSaving(payload.itemId)
    })
}

function handleRecount(payload: { itemId: number; request: StocktakeRecountRequest }) {
  recountItem(stocktakeId.value, payload.itemId, payload.request)
    .then(() => {
      ElMessage.success('复盘录入已保存')
      return loadDetail()
    })
    .catch(() => {
      itemsTableRef.value?.clearSaving(payload.itemId)
    })
}

// ---- 状态流转操作 ----
function handleStart() {
  if (!detail.value) return
  ElMessageBox.confirm('确认启动盘点？启动后将冻结库存快照。')
    .then(() => {
      actionLoading.value = true
      return startStocktake(stocktakeId.value, detail.value!.version)
    })
    .then(() => {
      ElMessage.success('盘点已启动')
      return loadDetail()
    })
    .catch(() => {})
    .finally(() => {
      actionLoading.value = false
    })
}

function handleSubmit() {
  if (!detail.value) return
  ElMessageBox.confirm('确认提交盘点？提交后将生成方差并可能触发复盘。')
    .then(() => {
      actionLoading.value = true
      return submitStocktake(stocktakeId.value, detail.value!.version)
    })
    .then(() => {
      ElMessage.success('盘点已提交')
      return loadDetail()
    })
    .catch(() => {})
    .finally(() => {
      actionLoading.value = false
    })
}

function handlePost() {
  if (!detail.value) return
  ElMessageBox.confirm('确认过账？过账将原子更新库存数量与移动平均成本。')
    .then(() => {
      actionLoading.value = true
      return postStocktake(stocktakeId.value, detail.value!.version)
    })
    .then(() => {
      ElMessage.success('过账成功')
      return loadDetail()
    })
    .catch(() => {})
    .finally(() => {
      actionLoading.value = false
    })
}

function handleCancel() {
  if (!detail.value) return
  ElMessageBox.confirm('确认取消该盘点任务？取消后不可恢复。')
    .then(() => {
      actionLoading.value = true
      return cancelStocktake(stocktakeId.value, detail.value!.version)
    })
    .then(() => {
      ElMessage.success('盘点任务已取消')
      return loadDetail()
    })
    .catch(() => {})
    .finally(() => {
      actionLoading.value = false
    })
}

// ---- 分配 ----
function openAssignDialog() {
  assignForm.counterUserIdInput = detail.value ? String(detail.value.counterUserId || '') : ''
  assignForm.recountUserIdInput = detail.value && detail.value.recountUserId ? String(detail.value.recountUserId) : ''
  assignOpen.value = true
}

function submitAssign() {
  if (!detail.value) return
  assignFormRef.value?.validate((valid) => {
    if (!valid) return
    const counterUserId = Number(assignForm.counterUserIdInput)
    if (!counterUserId || Number.isNaN(counterUserId)) {
      ElMessage.warning('请输入有效的盘点人ID')
      return
    }
    const payload: StocktakeAssignRequest = {
      counterUserId,
      recountUserId: assignForm.recountUserIdInput ? Number(assignForm.recountUserIdInput) : undefined,
      version: detail.value!.version,
    }
    actionLoading.value = true
    assignCounter(stocktakeId.value, payload)
      .then(() => {
        ElMessage.success('分配成功')
        assignOpen.value = false
        return loadDetail()
      })
      .finally(() => {
        actionLoading.value = false
      })
  })
}

// ---- 审批 ----
function openApproveDialog() {
  approveForm.decision = 'APPROVE'
  approveForm.comment = ''
  approveOpen.value = true
}

function submitApprove() {
  if (!detail.value) return
  approveFormRef.value?.validate((valid) => {
    if (!valid) return
    const payload: StocktakeApprovalRequest = {
      decision: approveForm.decision,
      comment: approveForm.comment || undefined,
      version: detail.value!.version,
    }
    actionLoading.value = true
    approveStocktake(stocktakeId.value, payload)
      .then(() => {
        ElMessage.success(approveForm.decision === 'APPROVE' ? '审批通过' : '已驳回')
        approveOpen.value = false
        return loadDetail()
      })
      .finally(() => {
        actionLoading.value = false
      })
  })
}

// ---- 冲销 ----
function openReverseDialog() {
  reverseForm.reason = ''
  reverseForm.idempotencyKey = createIdempotencyKey()
  reverseOpen.value = true
}

function submitReverse() {
  if (!detail.value) return
  reverseFormRef.value?.validate((valid) => {
    if (!valid) return
    const payload: StocktakeReverseRequest = {
      reason: reverseForm.reason,
      idempotencyKey: reverseForm.idempotencyKey,
      version: detail.value!.version,
    }
    actionLoading.value = true
    reverseStocktake(stocktakeId.value, payload)
      .then(() => {
        ElMessage.success('冲销成功')
        reverseOpen.value = false
        return loadDetail()
      })
      .finally(() => {
        actionLoading.value = false
      })
  })
}

onMounted(() => {
  loadUserOptions()
  loadDetail()
})
</script>

<style scoped>
.stocktake-detail-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.take-no {
  font-size: 18px;
  font-weight: 700;
  color: #18202f;
}
.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.info-card {
  border: 1px solid #e5e9f2;
  margin-bottom: 12px;
}
.card-title {
  font-weight: 600;
  color: #1f2d3d;
}
.history-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.6;
}
.history-action {
  font-weight: 600;
  color: #1f2d3d;
}
.history-transition {
  color: #606266;
  font-size: 13px;
}
.history-operator {
  color: #909399;
  font-size: 13px;
}
.history-comment {
  color: #606266;
  font-size: 13px;
}
.reverse-alert {
  margin-bottom: 16px;
}
</style>
