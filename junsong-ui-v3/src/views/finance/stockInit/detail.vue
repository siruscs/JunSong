<template>
  <div class="app-container stockinit-detail-page" v-loading="loading">
    <!-- 顶部 -->
    <div class="detail-header">
      <el-button :icon="ArrowLeft" @click="handleBack">返回</el-button>
      <div class="header-title">
        <span class="batch-no">{{ detail?.batch?.batchNo || '系统生成' }}</span>
        <el-tag v-if="detail" :type="statusTagType(detail.batch.status)" size="default">{{ statusLabel(detail.batch.status) }}</el-tag>
      </div>
    </div>

    <template v-if="detail">
      <!-- 操作按钮区 -->
      <div class="action-bar">
        <el-button
          v-if="detail.batch.status === 'DRAFT'"
          type="primary"
          size="small"
          :loading="actionLoadingKey === 'validate'"
          @click="handleValidate"
          v-hasPermi="['finance:stockInit:add']"
        >验证</el-button>
        <el-button
          v-if="detail.batch.status === 'VALIDATED'"
          type="primary"
          size="small"
          :loading="actionLoadingKey === 'submit'"
          @click="handleSubmit"
          v-hasPermi="['finance:stockInit:add']"
        >提交</el-button>
        <el-button
          v-if="detail.batch.status === 'SUBMITTED'"
          type="warning"
          size="small"
          @click="openApproveDialog"
          v-hasPermi="['finance:stockInit:approve']"
        >审批</el-button>
        <el-button
          v-if="detail.batch.status === 'APPROVED'"
          type="success"
          size="small"
          @click="openPostDialog"
          v-hasPermi="['finance:stockInit:post']"
        >过账</el-button>
      </div>

      <!-- 头部信息卡片 -->
      <el-card shadow="never" class="info-card">
        <el-alert
          v-if="detail.batch.status === 'DRAFT' || detail.batch.status === 'VALIDATED'"
          title="期初库存流程：先校验批次（系统会校验商品归属与金额），校验通过后提交审批；审批通过后点击过账，系统才会正式生成库存与成本台账流水。"
          type="info"
          :closable="false"
          show-icon
          class="stockinit-flow-tip"
        />
        <el-descriptions :column="3" border>
          <el-descriptions-item label="批次号">
            <span class="readonly-field">{{ detail.batch.batchNo || '-' }}</span>
            <span class="auto-field-tag" v-if="detail.batch.batchNo">系统生成</span>
          </el-descriptions-item>
          <el-descriptions-item label="门店">{{ getDeptName(detail.batch.deptId) }}</el-descriptions-item>
          <el-descriptions-item label="期初日期">{{ formatDate(detail.batch.initDate) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.batch.status)" size="small">{{ statusLabel(detail.batch.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.batch.createBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ detail.batch.submittedBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ detail.batch.approvedBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="过账人">{{ detail.batch.postedBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(detail.batch.submittedTime) }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ formatTime(detail.batch.approvedTime) }}</el-descriptions-item>
          <el-descriptions-item label="过账时间">{{ formatTime(detail.batch.postedTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ detail.batch.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 行项目表格 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <span class="card-title">行项目</span>
        </template>
        <el-table :data="detail.items" border stripe>
          <el-table-column label="商品名称" prop="productName" min-width="160" show-overflow-tooltip fixed="left" />
          <el-table-column label="调整数量" width="130" align="right">
            <template #default="scope">{{ formatNum(scope.row.quantity) }}</template>
          </el-table-column>
          <el-table-column label="单位成本" width="130" align="right">
            <template #default="scope">{{ formatMoney(scope.row.unitCost) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="140" align="right">
            <template #default="scope">
              <span class="amount-cell">{{ formatMoney(scope.row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="库存流水ID" prop="stockLedgerId" width="130" align="center">
            <template #default="scope">{{ scope.row.stockLedgerId || '-' }}</template>
          </el-table-column>
          <el-table-column label="成本流水ID" prop="costLedgerId" width="130" align="center">
            <template #default="scope">{{ scope.row.costLedgerId || '-' }}</template>
          </el-table-column>
        </el-table>
        <div class="items-summary">
          <span>合计金额：</span>
          <strong>¥{{ totalAmount }}</strong>
        </div>
      </el-card>
    </template>

    <!-- 审批对话框 -->
    <el-dialog title="审批期初库存" v-model="approveOpen" width="480px" append-to-body>
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
        <el-button type="primary" :loading="actionLoadingKey === 'approve-submit'" @click="submitApprove">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 过账对话框 -->
    <el-dialog title="过账期初库存" v-model="postOpen" width="520px" append-to-body>
      <el-alert type="warning" :closable="false" show-icon class="post-alert">
        过账将原子写入库存数量与移动平均成本，生成库存与成本台账流水。该操作不可撤销。
      </el-alert>
      <el-form ref="postFormRef" :model="postForm" :rules="postRules" label-width="100px">
        <el-form-item label="幂等键" prop="postIdempotencyKey">
          <el-input v-model="postForm.postIdempotencyKey" readonly>
            <template #suffix><span class="auto-field-tag">系统生成</span></template>
          </el-input>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input-number v-model="postForm.version" :min="0" :step="1" controls-position="right" disabled style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="postOpen = false">取 消</el-button>
        <el-button type="success" :loading="actionLoadingKey === 'post-submit'" @click="submitPost">确认过账</el-button>
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
import { useSubmitLock } from '@/composables/useSubmitLock'
import {
  approveStockInit,
  getStockInitDetail,
  postStockInit,
  submitStockInit,
  validateStockInit,
  type StockInitApproveRequest,
  type StockInitDetailVO,
  type StockInitPostRequest,
} from '@/api/finance/stockInit'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { buildIdempotencyKey } = useSubmitLock()

const loading = ref(false)
const actionLoadingKey = ref('')
const detail = ref<StockInitDetailVO | null>(null)

const approveOpen = ref(false)
const postOpen = ref(false)

const approveFormRef = ref<FormInstance>()
const postFormRef = ref<FormInstance>()

const batchId = computed(() => Number(route.params.id))
const deptOptions = computed(() => userStore.depts || [])
const totalAmount = computed(() => {
  if (!detail.value || !detail.value.items) return '0.00'
  return detail.value.items
    .reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
    .toFixed(2)
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已校验', value: 'VALIDATED' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已审批', value: 'APPROVED' },
  { label: '已过账', value: 'POSTED' },
]

const approveForm = reactive<{ decision: 'APPROVE' | 'REJECT'; comment: string }>({
  decision: 'APPROVE',
  comment: '',
})
const approveRules = {
  decision: [{ required: true, message: '请选择审批决定', trigger: 'change' }],
}

const postForm = reactive<{ postIdempotencyKey: string; version: number }>({
  postIdempotencyKey: '',
  version: 0,
})
const postRules = {
  postIdempotencyKey: [{ required: true, message: '幂等键不能为空', trigger: 'blur' }],
  version: [{ required: true, message: '版本号不能为空', trigger: 'blur' }],
}

function statusLabel(value: string) {
  return statusOptions.find((o) => o.value === value)?.label || value || '-'
}

function statusTagType(value: string) {
  const map: Record<string, string> = {
    DRAFT: 'info',
    VALIDATED: '',
    SUBMITTED: 'warning',
    APPROVED: 'success',
    POSTED: 'success',
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

function formatDate(value: string | null) {
  if (!value) return '-'
  return parseTime(value, '{y}-{m}-{d}') || '-'
}

function formatNum(value: number | null | undefined) {
  if (value === null || value === undefined) return '-'
  return Number(value).toFixed(2)
}

function formatMoney(value: number | null | undefined) {
  if (value === null || value === undefined) return '-'
  return '¥' + Number(value).toFixed(2)
}

function loadDetail() {
  if (!batchId.value || Number.isNaN(batchId.value)) {
    ElMessage.error('无效的期初库存批次ID')
    return
  }
  loading.value = true
  getStockInitDetail(batchId.value)
    .then((res: any) => {
      const payload = res.data?.data || res.data || {}
      detail.value = {
        batch: payload.batch || {},
        items: payload.items || [],
      } as StockInitDetailVO
    })
    .finally(() => {
      loading.value = false
    })
}

function handleBack() {
  router.push('/finance/stockInit/index')
}

// ---- 状态流转操作 ----
function handleValidate() {
  if (!detail.value) return
  ElMessageBox.confirm('确认校验该批次？校验通过后状态将变为“已校验”。')
    .then(() => {
      actionLoadingKey.value = 'validate'
      return validateStockInit(batchId.value, detail.value!.batch.version)
    })
    .then(() => {
      ElMessage.success('校验成功')
      return loadDetail()
    })
    .catch(() => {})
    .finally(() => {
      actionLoadingKey.value = ''
    })
}

function handleSubmit() {
  if (!detail.value) return
  ElMessageBox.confirm('确认提交该批次？提交后将进入审批环节。')
    .then(() => {
      actionLoadingKey.value = 'submit'
      return submitStockInit(batchId.value, detail.value!.batch.version)
    })
    .then(() => {
      ElMessage.success('提交成功')
      return loadDetail()
    })
    .catch(() => {})
    .finally(() => {
      actionLoadingKey.value = ''
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
    const payload: StockInitApproveRequest = {
      decision: approveForm.decision,
      comment: approveForm.comment || undefined,
      version: detail.value!.batch.version,
    }
    actionLoadingKey.value = 'approve-submit'
    approveStockInit(batchId.value, payload)
      .then(() => {
        ElMessage.success(approveForm.decision === 'APPROVE' ? '审批通过' : '已驳回')
        approveOpen.value = false
        return loadDetail()
      })
      .finally(() => {
        actionLoadingKey.value = ''
      })
  })
}

// ---- 过账 ----
function openPostDialog() {
  if (!detail.value) return
  postForm.postIdempotencyKey = buildIdempotencyKey('stockInit-post', batchId.value)
  postForm.version = detail.value.batch.version
  postOpen.value = true
}

function submitPost() {
  if (!detail.value) return
  postFormRef.value?.validate((valid) => {
    if (!valid) return
    const payload: StockInitPostRequest = {
      postIdempotencyKey: postForm.postIdempotencyKey,
      version: postForm.version,
    }
    actionLoadingKey.value = 'post-submit'
    postStockInit(batchId.value, payload)
      .then(() => {
        ElMessage.success('过账成功')
        postOpen.value = false
        return loadDetail()
      })
      .finally(() => {
        actionLoadingKey.value = ''
      })
  })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.stockinit-detail-page {
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
.batch-no {
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
.readonly-field {
  margin-right: 8px;
}
.auto-field-tag {
  color: #94a3b8;
  font-size: 12px;
}
.amount-cell {
  font-weight: 600;
  color: #18202f;
}
.items-summary {
  margin-top: 12px;
  text-align: right;
  color: #18202f;
}
.items-summary strong {
  margin-left: 4px;
  font-size: 16px;
}
.post-alert {
  margin-bottom: 16px;
}
</style>
