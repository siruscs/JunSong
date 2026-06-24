<template>
  <div class="store-opening-page app-container">
    <section class="store-opening-overview">
      <el-card v-for="card in overviewCards" :key="card.key" shadow="hover">
        <div class="overview-card__label">{{ card.label }}</div>
        <div class="overview-card__value">{{ card.value }}</div>
        <div class="overview-card__hint">{{ card.hint }}</div>
      </el-card>
    </section>

    <el-card shadow="never">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="88px" v-show="showSearch">
        <el-form-item label="申请单号" prop="orderNo">
          <el-input v-model="queryParams.orderNo" placeholder="请输入申请单号" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="门店名称" prop="storeName">
          <el-input v-model="queryParams.storeName" placeholder="请输入门店名称" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="所属区域" prop="regionName">
          <el-input v-model="queryParams.regionName" placeholder="请输入所属区域" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="流程状态" prop="workflowStatus">
          <el-select v-model="queryParams.workflowStatus" placeholder="请选择流程状态" clearable style="width: 220px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <RightToolbar v-model:showSearch="showSearch" @query="getList">
        <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:storeOpening:add']">
          新增申请
        </el-button>
        <el-button type="success" :icon="Refresh" @click="getList" v-hasPermi="['system:storeOpening:list']">
          刷新
        </el-button>
      </RightToolbar>

      <el-table v-loading="loading" :data="rows">
        <el-table-column label="申请单" min-width="220">
          <template #default="{ row }">
            <div class="table-title">
              <el-button link type="primary" @click="openDetail(row)">{{ row.storeName || '-' }}</el-button>
              <div class="table-title__sub">{{ row.orderNo || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="门店简称" prop="storeShortName" min-width="120" show-overflow-tooltip />
        <el-table-column label="所属区域" prop="regionName" min-width="120" show-overflow-tooltip />
        <el-table-column label="预计开业日期" min-width="140">
          <template #default="{ row }">{{ formatDate(row.expectedOpeningDate) }}</template>
        </el-table-column>
        <el-table-column label="当前节点" prop="currentTaskName" min-width="140" show-overflow-tooltip />
        <el-table-column label="流程状态" width="160" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.workflowStatus).type">
              {{ statusMeta(row.workflowStatus).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交人" prop="submitter" width="120" />
        <el-table-column label="更新时间" min-width="168">
          <template #default="{ row }">{{ formatDateTime(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="360" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="canEdit(row)"
              link
              type="primary"
              @click="handleEdit(row)"
              v-hasPermi="['system:storeOpening:edit']"
            >
              编辑
            </el-button>
            <el-button
              v-if="canSubmit(row)"
              link
              type="success"
              @click="handleSubmit(row)"
              v-hasPermi="['system:storeOpening:submit']"
            >
              提交审批
            </el-button>
            <el-button
              v-if="canWithdraw(row)"
              link
              type="warning"
              @click="handleWithdraw(row)"
              v-hasPermi="['system:storeOpening:withdraw']"
            >
              撤回
            </el-button>
            <el-button
              v-if="row.processInstanceId"
              link
              type="info"
              @click="goToWorkflow(row.processInstanceId)"
              v-hasPermi="['workflow:history:list']"
            >
              查看流程
            </el-button>
            <el-button
              v-if="canEdit(row)"
              link
              type="danger"
              @click="handleDelete(row)"
              v-hasPermi="['system:storeOpening:remove']"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="960px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px" :disabled="formDialog.mode === 'detail'">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="申请单号">
              <el-input :model-value="form.orderNo || '保存后自动生成'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流程状态">
              <el-tag :type="statusMeta(form.workflowStatus).type">
                {{ statusMeta(form.workflowStatus).label }}
              </el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="门店名称" prop="storeName">
              <el-input v-model="form.storeName" maxlength="128" placeholder="请输入门店名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门店简称" prop="storeShortName">
              <el-input v-model="form.storeShortName" maxlength="64" placeholder="请输入门店简称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="门店类型" prop="storeType">
              <el-select v-model="form.storeType" placeholder="请选择门店类型" clearable style="width: 100%">
                <el-option label="直营店" value="DIRECT" />
                <el-option label="加盟店" value="FRANCHISE" />
                <el-option label="旗舰店" value="FLAGSHIP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计开业日期" prop="expectedOpeningDate">
              <el-date-picker
                v-model="form.expectedOpeningDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择预计开业日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="所属区域" prop="regionName">
              <el-input v-model="form.regionName" maxlength="64" placeholder="如：华东一区" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区域负责人" prop="regionLeader">
              <el-input v-model="form.regionLeader" maxlength="64" placeholder="请输入区域负责人账号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总经理账号" prop="generalManager">
              <el-input v-model="form.generalManager" maxlength="64" placeholder="请输入总经理账号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="省" prop="province">
              <el-input v-model="form.province" maxlength="64" placeholder="请输入省" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市" prop="city">
              <el-input v-model="form.city" maxlength="64" placeholder="请输入市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区/县" prop="district">
              <el-input v-model="form.district" maxlength="64" placeholder="请输入区/县" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" prop="addressDetail">
          <el-input v-model="form.addressDetail" maxlength="255" placeholder="请输入详细地址" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="门店负责人" prop="storeManagerName">
              <el-input v-model="form.storeManagerName" maxlength="64" placeholder="请输入门店负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="场地面积" prop="siteArea">
              <el-input-number v-model="form.siteArea" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="场地类型" prop="siteMode">
              <el-select v-model="form.siteMode" placeholder="请选择场地类型" clearable style="width: 100%">
                <el-option label="租赁" value="LEASE" />
                <el-option label="自有" value="OWNED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="预计编制人数" prop="plannedStaffCount">
              <el-input-number v-model="form.plannedStaffCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="首期投入(元)" prop="initialInvestmentAmount">
              <el-input-number v-model="form.initialInvestmentAmount" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计月营收" prop="estimatedMonthlyRevenue">
              <el-input-number v-model="form.estimatedMonthlyRevenue" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开业说明" prop="openingReason">
          <el-input v-model="form.openingReason" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>

        <template v-if="formDialog.mode === 'detail'">
          <el-divider content-position="left">流程信息</el-divider>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="流程实例 ID" :span="2">{{ form.processInstanceId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="流程定义 Key">{{ form.processDefinitionKey || '-' }}</el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ form.currentTaskName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交人">{{ form.submitter || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDateTime(form.submitTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后更新">{{ formatDateTime(form.updateTime || form.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="formDialog.visible = false">关闭</el-button>
        <el-button
          v-if="formDialog.mode !== 'detail'"
          type="primary"
          :loading="formDialog.submitting"
          @click="submitForm"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import { resetForm as resetFormUtil, parseTime } from '@/utils/junsong'
import {
  addStoreOpening,
  delStoreOpening,
  getStoreOpening,
  getStoreOpeningByOrderNo,
  listStoreOpening,
  submitStoreOpening,
  updateStoreOpening,
  withdrawStoreOpening,
  type StoreOpeningFormData,
} from '@/api/system/storeOpening'
import { startWorkflowInstance, terminateWorkflowInstance } from '@/api/workflow/instance'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const queryFormRef = ref()
const formRef = ref()
const rows = ref<StoreOpeningFormData[]>([])

const statusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '待区域负责人审批', value: 'PENDING_REGION_APPROVAL', type: 'warning' },
  { label: '待总经理审批', value: 'PENDING_GM_APPROVAL', type: 'warning' },
  { label: '已通过', value: 'APPROVED', type: 'success' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
  { label: '已撤回', value: 'WITHDRAWN', type: 'info' },
]

const queryParams = reactive({
  orderNo: '',
  storeName: '',
  regionName: '',
  workflowStatus: '',
})

const formDialog = reactive({
  visible: false,
  title: '新增门店开业申请',
  mode: 'add' as 'add' | 'edit' | 'detail',
  submitting: false,
})

const form = reactive<StoreOpeningFormData>({
  id: undefined,
  orderNo: '',
  storeName: '',
  storeShortName: '',
  storeType: '',
  expectedOpeningDate: '',
  regionName: '',
  regionLeader: '',
  generalManager: '',
  storeManagerName: '',
  province: '',
  city: '',
  district: '',
  addressDetail: '',
  siteArea: null,
  siteMode: '',
  plannedStaffCount: null,
  initialInvestmentAmount: null,
  estimatedMonthlyRevenue: null,
  openingReason: '',
  remark: '',
  processInstanceId: '',
  processDefinitionKey: '',
  workflowStatus: 'DRAFT',
  currentTaskName: '',
  submitter: '',
  submitTime: '',
  createTime: '',
  updateTime: '',
})

const rules = {
  storeName: [{ required: true, message: '门店名称不能为空', trigger: 'blur' }],
  expectedOpeningDate: [{ required: true, message: '预计开业日期不能为空', trigger: 'change' }],
  regionName: [{ required: true, message: '所属区域不能为空', trigger: 'blur' }],
  regionLeader: [{ required: true, message: '区域负责人账号不能为空', trigger: 'blur' }],
  generalManager: [{ required: true, message: '总经理账号不能为空', trigger: 'blur' }],
  openingReason: [{ required: true, message: '开业说明不能为空', trigger: 'blur' }],
}

const overviewCards = computed(() => {
  const all = rows.value
  return [
    { key: 'all', label: '申请单总数', value: all.length, hint: '当前筛选结果内的业务单数量' },
    {
      key: 'pending',
      label: '审批中',
      value: all.filter((item) => ['PENDING_REGION_APPROVAL', 'PENDING_GM_APPROVAL'].includes(item.workflowStatus || '')).length,
      hint: '等待流程节点处理',
    },
    {
      key: 'approved',
      label: '已通过',
      value: all.filter((item) => item.workflowStatus === 'APPROVED').length,
      hint: '已走完全部审批节点',
    },
    {
      key: 'draft',
      label: '草稿/撤回',
      value: all.filter((item) => ['DRAFT', 'WITHDRAWN'].includes(item.workflowStatus || '')).length,
      hint: '可继续编辑并再次提交流程',
    },
  ]
})

function statusMeta(status?: string) {
  return statusOptions.find((item) => item.value === status) || { label: status || '未设置', type: 'info' }
}

function formatDate(value?: string) {
  return value ? parseTime(value, '{y}-{m}-{d}') : '-'
}

function formatDateTime(value?: string) {
  return value ? parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
}

function resetFormData() {
  Object.assign(form, {
    id: undefined,
    orderNo: '',
    storeName: '',
    storeShortName: '',
    storeType: '',
    expectedOpeningDate: '',
    regionName: '',
    regionLeader: '',
    generalManager: '',
    storeManagerName: '',
    province: '',
    city: '',
    district: '',
    addressDetail: '',
    siteArea: null,
    siteMode: '',
    plannedStaffCount: null,
    initialInvestmentAmount: null,
    estimatedMonthlyRevenue: null,
    openingReason: '',
    remark: '',
    processInstanceId: '',
    processDefinitionKey: '',
    workflowStatus: 'DRAFT',
    currentTaskName: '',
    submitter: '',
    submitTime: '',
    createTime: '',
    updateTime: '',
  })
}

function fillForm(data?: StoreOpeningFormData | null) {
  resetFormData()
  if (!data) return
  Object.assign(form, data, {
    siteArea: data.siteArea ?? null,
    plannedStaffCount: data.plannedStaffCount ?? null,
    initialInvestmentAmount: data.initialInvestmentAmount ?? null,
    estimatedMonthlyRevenue: data.estimatedMonthlyRevenue ?? null,
    expectedOpeningDate: data.expectedOpeningDate ? parseTime(data.expectedOpeningDate, '{y}-{m}-{d}') : '',
  })
}

async function getList() {
  loading.value = true
  try {
    const res: any = await listStoreOpening({
      orderNo: queryParams.orderNo || undefined,
      storeName: queryParams.storeName || undefined,
      regionName: queryParams.regionName || undefined,
      workflowStatus: queryParams.workflowStatus || undefined,
    })
    rows.value = res.rows || []
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.orderNo = ''
  queryParams.storeName = ''
  queryParams.regionName = ''
  queryParams.workflowStatus = ''
  resetFormUtil(queryFormRef.value)
  getList()
}

function canEdit(row: StoreOpeningFormData) {
  return ['DRAFT', 'WITHDRAWN', 'REJECTED', '', undefined].includes(row.workflowStatus as any)
}

function canSubmit(row: StoreOpeningFormData) {
  return ['DRAFT', 'WITHDRAWN', 'REJECTED', '', undefined].includes(row.workflowStatus as any)
}

function canWithdraw(row: StoreOpeningFormData) {
  return ['PENDING_REGION_APPROVAL', 'PENDING_GM_APPROVAL'].includes(row.workflowStatus || '') && !!row.processInstanceId
}

function handleAdd() {
  resetFormData()
  formDialog.visible = true
  formDialog.mode = 'add'
  formDialog.title = '新增门店开业申请'
  resetFormUtil(formRef.value)
}

async function handleEdit(row: StoreOpeningFormData) {
  const res: any = await getStoreOpening(row.id as number)
  fillForm(res.data)
  formDialog.visible = true
  formDialog.mode = 'edit'
  formDialog.title = '编辑门店开业申请'
}

async function openDetail(row: StoreOpeningFormData) {
  const res: any = await getStoreOpening(row.id as number)
  fillForm(res.data)
  formDialog.visible = true
  formDialog.mode = 'detail'
  formDialog.title = '门店开业申请详情'
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate()
  formDialog.submitting = true
  try {
    const isEdit = !!form.id
    const payload = { ...form }
    const res: any = isEdit ? await updateStoreOpening(payload) : await addStoreOpening(payload)
    fillForm(res.data)
    ElMessage.success(isEdit ? '保存成功' : '新增成功')
    formDialog.visible = false
    await getList()
  } finally {
    formDialog.submitting = false
  }
}

async function ensureSaved(row: StoreOpeningFormData) {
  if (row.id) {
    const res: any = await getStoreOpening(row.id)
    return res.data as StoreOpeningFormData
  }
  return row
}

async function handleSubmit(row: StoreOpeningFormData) {
  const current = await ensureSaved(row)
  if (!current?.id || !current.orderNo) {
    ElMessage.warning('请先保存申请单后再提交流程')
    return
  }
  await ElMessageBox.confirm(
    `确认提交门店开业申请「${current.storeName}」进入审批流吗？`,
    '提交审批确认',
    {
      type: 'warning',
      confirmButtonText: '确认提交',
      cancelButtonText: '取消',
    },
  )
  const startRes = await startWorkflowInstance({
    processKey: 'store_opening_apply',
    businessKey: current.orderNo,
    variables: {
      formId: current.id,
      orderNo: current.orderNo,
      storeName: current.storeName,
      regionName: current.regionName,
      regionLeader: current.regionLeader,
      generalManager: current.generalManager,
      expectedOpeningDate: current.expectedOpeningDate,
      openingReason: current.openingReason,
    },
  })
  await submitStoreOpening(current.id, {
    processInstanceId: (startRes as any).data.processInstanceId,
    processDefinitionKey: (startRes as any).data.processDefinitionKey,
    currentTaskName: '区域负责人审批',
  })
  ElMessage.success('流程已发起，已进入区域负责人审批')
  await getList()
}

async function handleWithdraw(row: StoreOpeningFormData) {
  if (!row.id || !row.processInstanceId) return
  await ElMessageBox.confirm(
    '撤回后会终止当前流程实例，并将业务单状态改为“已撤回”，确认继续吗？',
    '撤回确认',
    {
      type: 'warning',
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
    },
  )
  await terminateWorkflowInstance(row.processInstanceId, '业务单撤回')
  await withdrawStoreOpening(row.id)
  ElMessage.success('申请单已撤回')
  await getList()
}

async function handleDelete(row: StoreOpeningFormData) {
  if (!row.id) return
  await ElMessageBox.confirm(`确认删除申请单「${row.storeName}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
  })
  await delStoreOpening(row.id)
  ElMessage.success('删除成功')
  await getList()
}

function goToWorkflow(processInstanceId: string) {
  router.push({
    path: '/workflow/history',
    query: { processInstanceId },
  })
}

async function loadFromRoute() {
  const orderNo = typeof route.query.orderNo === 'string' ? route.query.orderNo.trim() : ''
  if (!orderNo) return
  queryParams.orderNo = orderNo
  const res: any = await getStoreOpeningByOrderNo(orderNo)
  if (res.data?.id) {
    await getList()
    await openDetail(res.data)
    return
  }
  await getList()
}

onMounted(async () => {
  await getList()
  await loadFromRoute()
})
</script>

<style scoped>
.store-opening-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.store-opening-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.overview-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.overview-card__value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 600;
}

.overview-card__hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.table-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.table-title__sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 1200px) {
  .store-opening-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .store-opening-overview {
    grid-template-columns: 1fr;
  }
}
</style>
