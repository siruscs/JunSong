<template>
  <div class="app-container stocktake-list-page">
    <!-- 状态统计卡片 -->
    <el-row :gutter="12" class="stat-row">
      <el-col v-for="item in statusOptions" :key="item.value" :xs="12" :sm="8" :md="6" :lg="3">
        <div class="stat-card" :class="'stat-' + item.value.toLowerCase()">
          <span>{{ item.label }}</span>
          <strong>{{ statusCounts[item.value] || 0 }}</strong>
        </div>
      </el-col>
    </el-row>

    <!-- 查询栏 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="82px" class="query-form">
      <el-form-item label="盘点单号" prop="takeNo">
        <el-input
          v-model="queryParams.takeNo"
          placeholder="请输入盘点单号"
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
      <el-form-item label="盘点人" prop="counterUserId">
        <el-input
          v-model="counterUserIdInput"
          placeholder="盘点人ID"
          clearable
          style="width: 140px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['finance:stocktake:add']">创建</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          size="small"
          :loading="exportLoading"
          @click="handleExport"
          v-hasPermi="['finance:stocktake:export']"
        >导出</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="盘点单号" prop="takeNo" min-width="160" />
      <el-table-column label="门店" min-width="140">
        <template #default="scope">{{ getDeptName(scope.row.deptId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="盘点人" min-width="120">
        <template #default="scope">{{ scope.row.counterUserName || '-' }}</template>
      </el-table-column>
      <el-table-column label="复盘人" min-width="120">
        <template #default="scope">{{ scope.row.recountUserName || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</template>
      </el-table-column>
      <el-table-column label="提交时间" width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.submittedTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</template>
      </el-table-column>
      <el-table-column label="过账时间" width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.postedTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right" align="center">
        <template #default="scope">
          <el-button
            link
            type="primary"
            size="small"
            @click="handleDetail(scope.row)"
            v-hasPermi="['finance:stocktake:query']"
          >详情</el-button>
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
    <el-dialog title="创建盘点任务" v-model="createOpen" width="560px" append-to-body>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="盘点单号" prop="takeNo">
          <el-input v-model="createForm.takeNo" placeholder="请输入盘点单号" />
        </el-form-item>
        <el-form-item label="门店" prop="deptId">
          <el-select v-model="createForm.deptId" placeholder="选择门店" filterable style="width: 100%">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点范围" prop="scopeType">
          <el-input model-value="指定商品" disabled />
        </el-form-item>
        <el-form-item label="商品" prop="productIds">
          <el-select
            v-model="createForm.productIds"
            multiple
            filterable
            placeholder="选择商品"
            style="width: 100%"
          >
            <el-option
              v-for="p in productOptions"
              :key="p.productId"
              :label="p.productName"
              :value="p.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点人" prop="counterUserId">
          <el-input v-model="counterUserInput" placeholder="盘点人用户ID" />
        </el-form-item>
        <el-form-item label="复盘人" prop="recountUserId">
          <el-input v-model="recountUserInput" placeholder="复盘人用户ID（可选）" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取 消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { saveAs } from 'file-saver'
import { parseTime } from '@/utils/junsong'
import { useUserStore } from '@/stores/user'
import Pagination from '@/components/Pagination/index.vue'
import {
  createStocktake,
  exportStocktakes,
  listStocktakes,
  type StocktakeQuery,
} from '@/api/finance/stocktake'
import { listProductSelector } from '@/api/finance/product'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const exportLoading = ref(false)
const createLoading = ref(false)
const createOpen = ref(false)
const total = ref(0)
const list = ref<any[]>([])
const productOptions = ref<any[]>([])
const deptOptions = ref<any[]>([])
const dateRange = ref<string[]>([])
const counterUserIdInput = ref('')
const counterUserInput = ref('')
const recountUserInput = ref('')
const statusCounts = reactive<Record<string, number>>({})

const createFormRef = ref<FormInstance>()
const createForm = reactive<{
  takeNo: string
  deptId: number | undefined
  scopeType: string
  productIds: number[]
  remark: string
}>({
  takeNo: '',
  deptId: undefined,
  scopeType: 'SELECTED_PRODUCTS',
  productIds: [],
  remark: '',
})
const createRules = {
  takeNo: [{ required: true, message: '请输入盘点单号', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  productIds: [{ required: true, type: 'array' as const, min: 1, message: '请选择商品', trigger: 'change' }],
}

const queryParams = reactive<StocktakeQuery>({
  pageNum: 1,
  pageSize: 10,
  takeNo: '',
  deptId: undefined,
  status: '',
  counterUserId: undefined,
})

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

function loadStats() {
  // 全局状态汇总（不受筛选影响）
  return listStocktakes({ pageNum: 1, pageSize: 1000 })
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
  const params: StocktakeQuery = { ...queryParams }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  if (counterUserIdInput.value) {
    params.counterUserId = Number(counterUserIdInput.value)
  } else {
    params.counterUserId = undefined
  }
  listStocktakes(params)
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
  queryParams.takeNo = ''
  queryParams.deptId = undefined
  queryParams.status = ''
  queryParams.counterUserId = undefined
  counterUserIdInput.value = ''
  dateRange.value = []
  queryParams.pageNum = 1
  getList()
}

function handleDetail(row: any) {
  router.push(`/finance/stocktake/detail/${row.stocktakeId}`)
}

function handleAdd() {
  resetCreateForm()
  createOpen.value = true
  if (productOptions.value.length === 0) {
    loadProducts()
  }
}

function resetCreateForm() {
  createForm.takeNo = ''
  createForm.deptId = userStore.currentDeptId || undefined
  createForm.scopeType = 'SELECTED_PRODUCTS'
  createForm.productIds = []
  createForm.remark = ''
  counterUserInput.value = ''
  recountUserInput.value = ''
}

function loadProducts() {
  listProductSelector()
    .then((res: any) => {
      productOptions.value = res.data || res.rows || []
    })
    .catch(() => {})
}

function submitCreate() {
  createFormRef.value?.validate((valid) => {
    if (!valid) return
    const counterUserId = Number(counterUserInput.value)
    if (!counterUserId || Number.isNaN(counterUserId)) {
      ElMessage.warning('请输入有效的盘点人ID')
      return
    }
    const payload = {
      takeNo: createForm.takeNo,
      deptId: createForm.deptId as number,
      scopeType: 'SELECTED_PRODUCTS',
      productIds: createForm.productIds,
      counterUserId,
      recountUserId: recountUserInput.value ? Number(recountUserInput.value) : undefined,
      remark: createForm.remark || undefined,
    }
    createLoading.value = true
    createStocktake(payload)
      .then(() => {
        ElMessage.success('盘点任务已创建')
        createOpen.value = false
        getList()
        loadStats()
      })
      .finally(() => {
        createLoading.value = false
      })
  })
}

function handleExport() {
  exportLoading.value = true
  const params: StocktakeQuery = { ...queryParams }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  if (counterUserIdInput.value) {
    params.counterUserId = Number(counterUserIdInput.value)
  }
  exportStocktakes(params)
    .then((data: any) => {
      const blob = new Blob([data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      saveAs(blob, `stocktake_${new Date().getTime()}.xlsx`)
      ElMessage.success('导出成功')
    })
    .finally(() => {
      exportLoading.value = false
    })
}

onMounted(() => {
  deptOptions.value = userStore.depts || []
  getList()
  loadStats()
})
</script>

<style scoped>
.stocktake-list-page .stat-row {
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
.stat-reversed {
  border-color: #fbc4c4;
}
.stat-counting,
.stat-recounting {
  border-color: #f5dab1;
}
.query-form {
  margin-top: 12px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
