<template>
  <div class="app-container stockinit-list-page">
    <!-- 状态统计卡片 -->
    <el-row :gutter="12" class="stat-row">
      <el-col v-for="item in statusOptions" :key="item.value" :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" :class="'stat-' + item.value.toLowerCase()">
          <span>{{ item.label }}</span>
          <strong>{{ statusCounts[item.value] || 0 }}</strong>
        </div>
      </el-col>
    </el-row>

    <!-- 查询栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="82px" class="query-form">
      <el-form-item label="批次号" prop="batchNo">
        <el-input
          v-model="queryParams.batchNo"
          placeholder="请输入批次号"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="门店" prop="deptId">
        <el-select v-model="queryParams.deptId" placeholder="选择门店" clearable filterable style="width: 180px">
          <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="选择状态" clearable style="width: 140px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="dateRange">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['finance:stockInit:add']">创建</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          size="small"
          :loading="exportLoading"
          @click="handleExport"
          v-hasPermi="['finance:stockInit:export']"
        >导出</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="批次号" prop="batchNo" min-width="160" />
      <el-table-column label="门店" min-width="140">
        <template #default="scope">{{ getDeptName(scope.row.deptId) }}</template>
      </el-table-column>
      <el-table-column label="调整日历" width="120" align="center">
        <template #default="scope">{{ formatDate(scope.row.initDate) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建人" prop="createBy" min-width="120">
        <template #default="scope">{{ scope.row.createBy || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleDetail(scope.row)" v-hasPermi="['finance:stockInit:query']">查看</el-button>
          <el-button
            v-if="scope.row.status === 'DRAFT'"
            link
            type="primary"
            size="small"
            :loading="actionLoadingKey === `validate-${scope.row.batchId}`"
            @click="handleValidate(scope.row)"
            v-hasPermi="['finance:stockInit:add']"
          >验证</el-button>
          <el-button
            v-if="scope.row.status === 'VALIDATED'"
            link
            type="primary"
            size="small"
            :loading="actionLoadingKey === `submit-${scope.row.batchId}`"
            @click="handleSubmit(scope.row)"
            v-hasPermi="['finance:stockInit:add']"
          >提交</el-button>
          <el-button
            v-if="scope.row.status === 'SUBMITTED'"
            link
            type="warning"
            size="small"
            @click="openApproveFromList(scope.row)"
            v-hasPermi="['finance:stockInit:approve']"
          >审批</el-button>
          <el-button
            v-if="scope.row.status === 'APPROVED'"
            link
            type="success"
            size="small"
            :loading="actionLoadingKey === `post-${scope.row.batchId}`"
            @click="handlePostFromList(scope.row)"
            v-hasPermi="['finance:stockInit:post']"
          >过账</el-button>
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

    <!-- 创建对话框 -->
    <el-dialog title="创建库存调整" v-model="createOpen" width="920px" append-to-body>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="批次号" prop="batchNo">
              <el-input model-value="" readonly placeholder="保存时由服务端生成">
                <template #suffix><span class="auto-field-tag">系统生成</span></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门店" prop="deptId">
              <el-select v-model="createForm.deptId" placeholder="选择门店" filterable :disabled="deptOptions.length <= 1" style="width: 100%">
                <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="调整日历" prop="initDate">
              <el-date-picker
                v-model="createForm.initDate"
                type="date"
                placeholder="选择调整日历"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调整类型" prop="adjustmentType">
              <el-select v-model="createForm.adjustmentType" style="width: 100%" @change="onAdjustmentTypeChange">
                <el-option label="期初库存录入" value="OPENING_STOCK" />
                <el-option label="历史数据补录" value="HISTORY_REPLENISH" />
                <el-option label="试用消耗" value="TRIAL_CONSUMPTION" />
                <el-option label="店面自用" value="STORE_USE" />
                <el-option label="报损" value="DAMAGE_LOSS" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="createForm.adjustmentType === 'OTHER'" :span="12">
            <el-form-item label="库存方向" prop="adjustmentDirection">
              <el-radio-group v-model="createForm.adjustmentDirection">
                <el-radio value="INCREASE">增加库存</el-radio>
                <el-radio value="DECREASE">减少库存</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建人">
              <el-input :model-value="creatorLabel" readonly>
                <template #suffix><span class="auto-field-tag">当前登录用户</span></template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="商品明细" required>
          <div class="items-toolbar">
            <el-button type="primary" plain size="small" @click="addItemRow">添加行</el-button>
            <span class="items-hint">数量填写正数；{{ adjustmentHint }}；金额 = 数量 × 单位成本</span>
          </div>
          <el-table :data="createForm.items" border stripe size="small" style="width: 100%">
            <el-table-column label="商品" min-width="220">
              <template #default="scope">
                <el-select
                  v-model="scope.row.productId"
                  filterable
                  placeholder="选择商品"
                  :loading="productLoading"
                  no-data-text="暂无可选商品"
                  style="width: 100%"
                  @change="onProductChange(scope.$index)"
                >
                  <el-option
                    v-for="p in productOptions"
                    :key="p.productId"
                    :label="p.productName"
                    :value="p.productId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="160" align="center">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.quantity"
                  :min="0"
                  :precision="3"
                  :step="1"
                  size="small"
                  controls-position="right"
                  style="width: 140px"
                />
              </template>
            </el-table-column>
            <el-table-column label="单位成本" width="170" align="center">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.unitCost"
                  :min="0"
                  :precision="2"
                  :step="0.01"
                  size="small"
                  controls-position="right"
                  style="width: 150px"
                />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="120" align="right">
              <template #default="scope">
                <span class="amount-cell">¥{{ rowAmount(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="scope">
                <el-button link type="danger" size="small" @click="removeItemRow(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="items-summary">
            <span>合计金额：</span>
            <strong>¥{{ totalAmount }}</strong>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取 消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 列表页内联审批对话框 -->
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { saveAs } from 'file-saver'
import { parseTime } from '@/utils/junsong'
import { useUserStore } from '@/stores/user'
import { useSubmitLock } from '@/composables/useSubmitLock'
import Pagination from '@/components/Pagination/index.vue'
import {
  approveStockInit,
  createStockInit,
  exportStockInit,
  listStockInit,
  postStockInit,
  submitStockInit,
  validateStockInit,
  type StockInitApproveRequest,
  type StockInitQuery,
} from '@/api/finance/stockInit'
import { listProductSelector } from '@/api/finance/product'

interface CreateItemRow {
  productId: number | undefined
  quantity: number
  unitCost: number
}

const router = useRouter()
const userStore = useUserStore()
const { buildIdempotencyKey } = useSubmitLock()

const loading = ref(false)
const exportLoading = ref(false)
const createLoading = ref(false)
const createOpen = ref(false)
const approveOpen = ref(false)
const total = ref(0)
const list = ref<any[]>([])
const productOptions = ref<any[]>([])
const deptOptions = ref<any[]>([])
const productLoading = ref(false)
const dateRange = ref<string[]>([])
const actionLoadingKey = ref('')
const creatorLabel = computed(() => userStore.nickName || userStore.name || '当前登录用户')
const statusCounts = reactive<Record<string, number>>({})

const createFormRef = ref<FormInstance>()
const approveFormRef = ref<FormInstance>()
const approveTarget = ref<{ batchId: number; version: number } | null>(null)

const createForm = reactive<{
  deptId: number | undefined
  initDate: string
  adjustmentType: string
  adjustmentDirection: 'INCREASE' | 'DECREASE' | undefined
  items: CreateItemRow[]
  remark: string
}>({
  deptId: undefined,
  initDate: '',
  adjustmentType: 'OPENING_STOCK',
  adjustmentDirection: undefined,
  items: [],
  remark: '',
})
const createRules = {
  deptId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  initDate: [{ required: true, message: '请选择期初日期', trigger: 'change' }],
  adjustmentType: [{ required: true, message: '请选择调整类型', trigger: 'change' }],
}

const approveForm = reactive<{ decision: 'APPROVE' | 'REJECT'; comment: string }>({
  decision: 'APPROVE',
  comment: '',
})
const approveRules = {
  decision: [{ required: true, message: '请选择审批决定', trigger: 'change' }],
}

const queryParams = reactive<StockInitQuery>({
  pageNum: 1,
  pageSize: 10,
  batchNo: '',
  deptId: undefined,
  status: '',
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已校验', value: 'VALIDATED' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已审批', value: 'APPROVED' },
  { label: '已过账', value: 'POSTED' },
]

const totalAmount = computed(() => {
  return createForm.items
    .reduce((sum, row) => sum + (Number(row.quantity) || 0) * (Number(row.unitCost) || 0), 0)
    .toFixed(2)
})

const adjustmentHint = computed(() => {
  if (createForm.adjustmentType === 'OTHER') return createForm.adjustmentDirection === 'DECREASE' ? '库存将减少' : '库存将增加'
  return ['OPENING_STOCK', 'HISTORY_REPLENISH'].includes(createForm.adjustmentType) ? '库存将增加' : '库存将减少'
})

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

function formatDate(value: string | null) {
  if (!value) return '-'
  return parseTime(value, '{y}-{m}-{d}') || '-'
}

function rowAmount(row: CreateItemRow) {
  return safeMul(Number(row.quantity) || 0, Number(row.unitCost) || 0).toFixed(2)
}

function loadStats() {
  return listStockInit({ pageNum: 1, pageSize: 1000 })
    .then((res: any) => {
      const rows = res.rows || []
      const counts: Record<string, number> = {}
      statusOptions.forEach((o) => (counts[o.value] = 0))
      rows.forEach((r: any) => {
        if (counts[r.status] !== undefined) counts[r.status]++
      })
      Object.keys(counts).forEach((k) => (statusCounts[k] = counts[k]))
    })
    .catch(() => {})
}

function getList() {
  loading.value = true
  const params: StockInitQuery = { ...queryParams }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  listStockInit(params)
    .then((res: any) => {
      list.value = res.rows || []
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
  queryParams.batchNo = ''
  queryParams.deptId = undefined
  queryParams.status = ''
  dateRange.value = []
  queryParams.pageNum = 1
  getList()
}

function handleDetail(row: any) {
  const batchId = row.batchId
  if (!batchId || Number.isNaN(Number(batchId))) {
    ElMessage.error('期初库存批次缺少有效ID，无法打开详情，请刷新列表后重试')
    return
  }
  router.push(`/finance/stockInit/detail/${Number(batchId)}`)
}

function handleAdd() {
  resetCreateForm()
  createOpen.value = true
  if (productOptions.value.length === 0) {
    loadProducts()
  }
}

function resetCreateForm() {
  createForm.deptId = userStore.currentDeptId || undefined
  createForm.initDate = ''
  createForm.adjustmentType = 'OPENING_STOCK'
  createForm.adjustmentDirection = undefined
  createForm.items = [{ productId: undefined, quantity: 0, unitCost: 0 }]
  createForm.remark = ''
}

function addItemRow() {
  createForm.items.push({ productId: undefined, quantity: 0, unitCost: 0 })
}

function removeItemRow(index: number) {
  createForm.items.splice(index, 1)
}

function onAdjustmentTypeChange() {
  if (createForm.adjustmentType !== 'OTHER') createForm.adjustmentDirection = undefined
}

function onProductChange(index: number) {
  const product = productOptions.value.find((p: any) => p.productId === createForm.items[index].productId)
  if (product && (createForm.items[index].unitCost === 0 || createForm.items[index].unitCost == null)) {
    createForm.items[index].unitCost = Number(Number(product.purchasePrice || 0).toFixed(2))
  }
}

function loadProducts() {
  productLoading.value = true
  listProductSelector()
    .then((res: any) => {
      productOptions.value = res.data || res.rows || []
    })
    .catch(() => {
      productOptions.value = []
      ElMessage.error('商品列表加载失败，请稍后重试')
    })
    .finally(() => {
      productLoading.value = false
    })
}

function validateCreateItems(): boolean {
  if (!createForm.items || createForm.items.length === 0) {
    ElMessage.warning('请至少添加一行商品明细')
    return false
  }
  const seenProductIds = new Set<number>()
  for (let i = 0; i < createForm.items.length; i++) {
    const row = createForm.items[i]
    if (!row.productId) {
      ElMessage.warning(`第 ${i + 1} 行未选择商品`)
      return false
    }
    if (seenProductIds.has(row.productId)) {
      ElMessage.warning(`第 ${i + 1} 行商品重复，同一批次不允许同一商品多行`)
      return false
    }
    seenProductIds.add(row.productId)
    if (row.quantity === null || row.quantity === undefined || Number(row.quantity) <= 0) {
      ElMessage.warning(`第 ${i + 1} 行数量必须大于 0`)
      return false
    }
    if (row.unitCost === null || row.unitCost === undefined || Number(row.unitCost) < 0) {
      ElMessage.warning(`第 ${i + 1} 行单位成本不能为负`)
      return false
    }
    if (Number(row.quantity).toFixed(3) !== String(Number(row.quantity))) { ElMessage.warning(`第 ${i + 1} 行数量最多三位小数`); return false }
    if (Number(row.unitCost).toFixed(2) !== String(Number(row.unitCost))) { ElMessage.warning(`第 ${i + 1} 行单位成本最多两位小数`); return false }
  }
  return true
}

function submitCreate() {
  createFormRef.value?.validate((valid) => {
    if (!valid) return
    if (!validateCreateItems()) return
    const payload = {
      deptId: createForm.deptId as number,
      initDate: createForm.initDate,
      adjustmentDate: createForm.initDate,
      adjustmentType: createForm.adjustmentType,
      adjustmentDirection: createForm.adjustmentDirection,
      items: createForm.items.map((row) => ({
        productId: row.productId as number,
        quantity: row.quantity,
        unitCost: row.unitCost,
      })),
      remark: createForm.remark || undefined,
    }
    createLoading.value = true
    createStockInit(payload)
      .then(() => {
        ElMessage.success('期初库存批次已创建')
        createOpen.value = false
        getList()
        loadStats()
      })
      .catch((error: any) => {
        const message = error?.response?.data?.msg || error?.message || '保存失败，请检查门店、商品与金额后重试'
        ElMessage.error(message)
      })
      .finally(() => {
        createLoading.value = false
      })
  })
}

function handleValidate(row: any) {
  ElMessageBox.confirm('确认校验该批次？校验通过后状态将变为“已校验”。')
    .then(() => {
      actionLoadingKey.value = `validate-${row.batchId}`
      return validateStockInit(row.batchId, row.version)
    })
    .then(() => {
      ElMessage.success('校验成功')
      getList()
      loadStats()
    })
    .catch(() => {})
    .finally(() => {
      actionLoadingKey.value = ''
    })
}

function handleSubmit(row: any) {
  ElMessageBox.confirm('确认提交该批次？提交后将进入审批环节。')
    .then(() => {
      actionLoadingKey.value = `submit-${row.batchId}`
      return submitStockInit(row.batchId, row.version)
    })
    .then(() => {
      ElMessage.success('提交成功')
      getList()
      loadStats()
    })
    .catch(() => {})
    .finally(() => {
      actionLoadingKey.value = ''
    })
}

function openApproveFromList(row: any) {
  approveTarget.value = { batchId: row.batchId, version: row.version }
  approveForm.decision = 'APPROVE'
  approveForm.comment = ''
  approveOpen.value = true
}

function submitApprove() {
  if (!approveTarget.value) return
  approveFormRef.value?.validate((valid) => {
    if (!valid) return
    const payload: StockInitApproveRequest = {
      decision: approveForm.decision,
      comment: approveForm.comment || undefined,
      version: approveTarget.value!.version,
    }
    actionLoadingKey.value = 'approve-submit'
    approveStockInit(approveTarget.value.batchId, payload)
      .then(() => {
        ElMessage.success(approveForm.decision === 'APPROVE' ? '审批通过' : '已驳回')
        approveOpen.value = false
        getList()
        loadStats()
      })
      .finally(() => {
        actionLoadingKey.value = ''
      })
  })
}

function handlePostFromList(row: any) {
  ElMessageBox.confirm('确认过账？过账将原子写入库存数量与移动平均成本。')
    .then(() => {
      actionLoadingKey.value = `post-${row.batchId}`
      const idempotencyKey = buildIdempotencyKey('stockInit-post', row.batchId)
      return postStockInit(row.batchId, {
        postIdempotencyKey: idempotencyKey,
        version: row.version,
      })
    })
    .then(() => {
      ElMessage.success('过账成功')
      getList()
      loadStats()
    })
    .catch(() => {})
    .finally(() => {
      actionLoadingKey.value = ''
    })
}

function handleExport() {
  exportLoading.value = true
  const params: StockInitQuery = { ...queryParams }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  exportStockInit(params)
    .then((data: any) => {
      const blob = new Blob([data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      saveAs(blob, `stockInit_${new Date().getTime()}.xlsx`)
      ElMessage.success('导出成功')
    })
    .finally(() => {
      exportLoading.value = false
    })
}

// 简单的浮点乘法（避免精度丢失过度，保留两位小数显示）
function safeMul(a: number, b: number): number {
  const aStr = String(a)
  const bStr = String(b)
  const aDec = aStr.includes('.') ? aStr.split('.')[1].length : 0
  const bDec = bStr.includes('.') ? bStr.split('.')[1].length : 0
  const factor = Math.pow(10, aDec + bDec)
  return (Number(aStr.replace('.', '')) * Number(bStr.replace('.', ''))) / factor
}

onMounted(() => {
  deptOptions.value = userStore.depts || []
  getList()
  loadStats()
})
</script>

<style scoped>
.stockinit-list-page .stat-row {
  margin-bottom: 12px;
}
.stat-card {
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
  min-height: 78px;
}
.stat-card span {
  display: block;
  margin-bottom: 8px;
  color: #667085;
  font-size: 13px;
}
.stat-card strong {
  color: #18202f;
  font-size: 22px;
}
.stat-posted,
.stat-approved {
  border-color: #b3e19d;
}
.stat-submitted {
  border-color: #f5dab1;
}
.query-form {
  margin-top: 12px;
}
.mb8 {
  margin-bottom: 8px;
}
.auto-field-tag {
  color: #94a3b8;
  font-size: 12px;
}
.items-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.items-hint {
  color: #94a3b8;
  font-size: 12px;
}
.amount-cell {
  font-weight: 600;
  color: #18202f;
}
.items-summary {
  margin-top: 8px;
  text-align: right;
  color: #18202f;
}
.items-summary strong {
  margin-left: 4px;
  font-size: 16px;
}
</style>
