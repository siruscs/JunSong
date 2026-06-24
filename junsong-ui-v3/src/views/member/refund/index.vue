<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form :model="queryParams" :inline="true" label-width="88px" v-show="showSearch">
        <el-form-item label="退款单号">
          <el-input v-model="queryParams.refundNo" placeholder="请输入退款单号" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="会员姓名">
          <el-input v-model="queryParams.memberName" placeholder="请输入会员姓名" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="门店">
          <el-input v-model="queryParams.storeName" placeholder="请输入门店" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="流程状态">
          <el-select v-model="queryParams.workflowStatus" clearable placeholder="请选择流程状态" style="width: 220px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <RightToolbar v-model:showSearch="showSearch" @query="getList">
        <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['member:refund:add']">新增申请</el-button>
        <el-button type="success" :icon="Refresh" @click="getList" v-hasPermi="['member:refund:list']">刷新</el-button>
      </RightToolbar>

      <el-table v-loading="loading" :data="rows">
        <el-table-column label="退款单号" prop="refundNo" min-width="170" />
        <el-table-column label="会员姓名" prop="memberName" min-width="120" />
        <el-table-column label="联系电话" prop="memberPhone" min-width="130" />
        <el-table-column label="门店" prop="storeName" min-width="140" />
        <el-table-column label="退款金额" min-width="120">
          <template #default="{ row }">¥{{ Number(row.refundAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="当前节点" prop="currentTaskName" min-width="140" />
        <el-table-column label="流程状态" width="160">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.workflowStatus).type">{{ statusMeta(row.workflowStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="168">
          <template #default="{ row }">{{ formatDateTime(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)" v-if="canEdit(row)" v-hasPermi="['member:refund:edit']">编辑</el-button>
            <el-button link type="success" @click="handleSubmit(row)" v-if="canSubmit(row)" v-hasPermi="['member:refund:submit']">提交审批</el-button>
            <el-button link type="warning" @click="handleWithdraw(row)" v-if="canWithdraw(row)" v-hasPermi="['member:refund:withdraw']">撤回</el-button>
            <el-button link type="info" @click="goToWorkflow(row.processInstanceId)" v-if="row.processInstanceId" v-hasPermi="['workflow:history:list']">查看流程</el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-if="canEdit(row)" v-hasPermi="['member:refund:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="退款单号"><el-input :model-value="form.refundNo || '保存后自动生成'" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="流程状态"><el-tag :type="statusMeta(form.workflowStatus).type">{{ statusMeta(form.workflowStatus).label }}</el-tag></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="会员姓名" prop="memberName"><el-input v-model="form.memberName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话" prop="memberPhone"><el-input v-model="form.memberPhone" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="门店" prop="storeName"><el-input v-model="form.storeName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="退款金额" prop="refundAmount"><el-input-number v-model="form.refundAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="门店负责人" prop="storeApprover"><el-input v-model="form.storeApprover" placeholder="请输入账号" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="财务复核人" prop="financeApprover"><el-input v-model="form.financeApprover" placeholder="大额退款时使用" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="退款原因" prop="refundReason"><el-input v-model="form.refundReason" maxlength="255" /></el-form-item>
        <el-form-item label="退款备注" prop="refundRemark"><el-input v-model="form.refundRemark" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import { parseTime, resetForm as resetFormUtil } from '@/utils/junsong'
import { startWorkflowInstance, terminateWorkflowInstance } from '@/api/workflow/instance'
import { addRefund, delRefund, getRefund, getRefundByRefundNo, listRefund, submitRefund, type RefundFormData, updateRefund, withdrawRefund } from '@/api/member/refund'

const { proxy } = getCurrentInstance() as any
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const showSearch = ref(true)
const rows = ref<RefundFormData[]>([])
const formRef = ref<FormInstance>()

const queryParams = reactive({ refundNo: '', memberName: '', storeName: '', workflowStatus: '' })
const dialog = reactive({ visible: false, title: '', submitting: false })
const form = reactive<RefundFormData>({
  memberName: '',
  memberPhone: '',
  storeName: '',
  refundAmount: null,
  refundReason: '',
  refundRemark: '',
  storeApprover: '',
  financeApprover: '',
  workflowStatus: 'DRAFT',
})

const rules: FormRules = {
  memberName: [{ required: true, message: '请输入会员姓名', trigger: 'blur' }],
  refundAmount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }],
  storeApprover: [{ required: true, message: '请输入门店负责人账号', trigger: 'blur' }],
}

const statusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '待门店负责人审批', value: 'PENDING_STORE_APPROVAL', type: 'warning' },
  { label: '待财务复核', value: 'PENDING_FINANCE_APPROVAL', type: 'warning' },
  { label: '审批通过', value: 'APPROVED', type: 'success' },
  { label: '审批驳回', value: 'REJECTED', type: 'danger' },
  { label: '已撤回', value: 'WITHDRAWN', type: 'info' },
] as const

const statusMeta = (status?: string) => statusOptions.find((item) => item.value === status) || statusOptions[0]
const canEdit = (row: RefundFormData) => ['DRAFT', 'REJECTED'].includes(row.workflowStatus || 'DRAFT')
const canSubmit = (row: RefundFormData) => ['DRAFT', 'REJECTED'].includes(row.workflowStatus || 'DRAFT')
const canWithdraw = (row: RefundFormData) => ['PENDING_STORE_APPROVAL', 'PENDING_FINANCE_APPROVAL'].includes(row.workflowStatus || '')

function formatDateTime(value?: string) {
  return value ? parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-' : '-'
}

function resetForm() {
  Object.assign(form, {
    id: undefined, refundNo: '', memberId: undefined, memberName: '', memberPhone: '', storeId: undefined, storeName: '',
    refundAmount: null, refundReason: '', refundRemark: '', storeApprover: '', financeApprover: '',
    processInstanceId: '', processDefinitionKey: '', workflowStatus: 'DRAFT', currentTaskName: '',
    submitter: '', submitTime: '', createTime: '', updateTime: '',
  })
  resetFormUtil(formRef.value)
}

async function getList() {
  loading.value = true
  try {
    const res: any = await listRefund(queryParams)
    rows.value = res.rows || res.data || []
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.refundNo = ''
  queryParams.memberName = ''
  queryParams.storeName = ''
  queryParams.workflowStatus = ''
  getList()
}

function handleAdd() {
  resetForm()
  dialog.title = '新增退款申请'
  dialog.visible = true
}

async function handleEdit(row: RefundFormData) {
  if (!row.id) return
  const res: any = await getRefund(row.id)
  Object.assign(form, res.data)
  dialog.title = '编辑退款申请'
  dialog.visible = true
}

async function handleSave() {
  await formRef.value?.validate()
  dialog.submitting = true
  try {
    const payload = { ...form }
    const res: any = form.id ? await updateRefund(payload) : await addRefund(payload)
    Object.assign(form, res.data)
    dialog.visible = false
    ElMessage.success(form.id ? '保存成功' : '新增成功')
    await getList()
  } finally {
    dialog.submitting = false
  }
}

async function ensureSaved(row: RefundFormData) {
  if (row.id) {
    const res: any = await getRefund(row.id)
    return res.data as RefundFormData
  }
  return row
}

async function handleSubmit(row: RefundFormData) {
  const current = await ensureSaved(row)
  if (!current?.id || !current.refundNo) {
    ElMessage.warning('请先保存退款申请后再提交流程')
    return
  }
  await ElMessageBox.confirm(`确认提交退款申请「${current.refundNo}」进入审批流吗？`, '提交审批确认', { type: 'warning' })
  const startRes: any = await startWorkflowInstance({
    processKey: 'refund_apply',
    businessKey: current.refundNo,
    variables: {
      formId: current.id,
      refundNo: current.refundNo,
      businessTitle: `退款申请-${current.memberName}`,
      initiator: proxy?.$auth?.getUserName?.(),
      refundAmount: current.refundAmount,
      storeApprover: current.storeApprover,
      financeApprover: current.financeApprover,
    },
  })
  await submitRefund(current.id, {
    processInstanceId: startRes.data.processInstanceId,
    processDefinitionKey: startRes.data.processDefinitionKey,
    currentTaskName: '门店负责人审批',
  })
  ElMessage.success('流程已发起，已进入门店负责人审批')
  await getList()
}

async function handleWithdraw(row: RefundFormData) {
  if (!row.id || !row.processInstanceId) return
  await ElMessageBox.confirm('撤回后会终止当前流程实例，并将退款申请状态改为“已撤回”，确认继续吗？', '撤回确认', { type: 'warning' })
  await terminateWorkflowInstance(row.processInstanceId, '退款申请撤回')
  await withdrawRefund(row.id)
  ElMessage.success('退款申请已撤回')
  await getList()
}

async function handleDelete(row: RefundFormData) {
  if (!row.id) return
  await ElMessageBox.confirm(`确认删除退款申请「${row.refundNo}」吗？`, '删除确认', { type: 'warning' })
  await delRefund(row.id)
  ElMessage.success('删除成功')
  await getList()
}

function goToWorkflow(processInstanceId?: string) {
  if (!processInstanceId) return
  router.push({ path: '/workflow/history', query: { processInstanceId } })
}

async function loadFromRoute() {
  const refundNo = typeof route.query.refundNo === 'string' ? route.query.refundNo.trim() : ''
  if (!refundNo) return
  queryParams.refundNo = refundNo
  const res: any = await getRefundByRefundNo(refundNo)
  if (res.data?.id) await getList()
}

onMounted(async () => {
  await getList()
  await loadFromRoute()
})
</script>
