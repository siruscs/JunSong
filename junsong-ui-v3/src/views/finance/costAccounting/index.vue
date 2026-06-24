<template>
  <div class="app-container">
    <el-card class="mb20 cost-summary-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">当前核算周期</span>
          <div class="real-time-actions">
            <el-button link type="primary" @click="refreshRealTime">刷新</el-button>
          </div>
        </div>
      </template>
      <div v-if="realTimeStats.periodNo" class="period-line">
        <el-tag size="small" :type="getPeriodStatusType(realTimeStats.periodStatus)">
          {{ getPeriodStatusText(realTimeStats.periodStatus) }}
        </el-tag>
        <span>周期编号：{{ realTimeStats.periodNo }}</span>
        <span>起始时间：{{ parseTime(realTimeStats.periodStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
      </div>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-item">
            <div class="stat-label">投资来源金额</div>
            <div class="stat-value success">¥{{ money(realTimeStats.totalInvestAmount) }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-item">
            <div class="stat-label">总缴款金额</div>
            <div class="stat-value success">¥{{ money(realTimeStats.totalPaymentAmount) }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-item">
            <div class="stat-label">总花销费用</div>
            <div class="stat-value danger">¥{{ money(realTimeStats.totalExpenseAmount) }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-item">
            <div class="stat-label">总进货金额</div>
            <div class="stat-value danger">¥{{ money(realTimeStats.totalPurchaseAmount) }}</div>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mt20">
        <el-col :xs="24" :md="12">
          <div class="stat-item">
            <div class="stat-label">当前借支（未核销）</div>
            <div class="stat-value danger">¥{{ money(realTimeStats.currentAdvance) }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="stat-item">
            <div class="stat-label">净利（销售缴款 - 已核销费用 - 进货款 - 借支未核销）</div>
            <div class="stat-value" :class="Number(realTimeStats.profit || 0) >= 0 ? 'success' : 'danger'">
              ¥{{ money(realTimeStats.profit) }}
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="核算单号" prop="accountingNo">
        <el-input
          v-model="queryParams.accountingNo"
          placeholder="请输入核算单号"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          :icon="Plus"
          @click="handleAdd"
          v-hasPermi="['finance:costAccounting:add']"
        >
          新增核算
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList" />
    </el-row>

    <el-table v-loading="loading" :data="accountingList">
      <el-table-column label="核算单号" align="center" prop="accountingNo" width="180" />
      <el-table-column label="开始日期" align="center" prop="startDate" width="120" />
      <el-table-column label="结束日期" align="center" prop="endDate" width="120" />
      <el-table-column label="总花销费用" align="center" prop="totalExpense">
        <template #default="scope">
          <span class="money-danger">¥{{ money(scope.row.totalExpense) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总进货金额" align="center" prop="totalPurchase">
        <template #default="scope">
          <span class="money-danger">¥{{ money(scope.row.totalPurchase) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总销售金额" align="center" prop="totalSale">
        <template #default="scope">
          <span class="money-success">¥{{ money(scope.row.totalSale) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="投资来源金额" align="center" prop="totalInvest">
        <template #default="scope">
          <span class="money-primary">¥{{ money(scope.row.totalInvest) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前借支" align="center" prop="currentAdvance">
        <template #default="scope">
          <span class="money-danger">¥{{ money(scope.row.currentAdvance) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总缴款金额" align="center" prop="totalPayment">
        <template #default="scope">
          <span class="money-success">¥{{ money(scope.row.totalPayment) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="净利" align="center" prop="returnSituation">
        <template #default="scope">
          <span :class="Number(scope.row.returnSituation || 0) >= 0 ? 'money-success' : 'money-danger'">
            ¥{{ money(scope.row.returnSituation) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" :icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:costAccounting:remove']"
          >
            删除
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

    <el-dialog v-model="open" :title="title" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="form.startDate"
            type="datetime"
            placeholder="选择开始日期时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="datetime"
            placeholder="选择结束日期时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="2" />
        </el-form-item>
      </el-form>

      <el-divider v-if="previewStats.startDate">预览数据</el-divider>
      <div v-if="previewStats.startDate" class="preview-stats">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="preview-item">
              <div class="preview-label">总花销费用</div>
              <div class="preview-value danger">¥{{ money(previewStats.totalExpenseAmount) }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="preview-item">
              <div class="preview-label">总进货金额</div>
              <div class="preview-value danger">¥{{ money(previewStats.totalPurchaseAmount) }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="preview-item">
              <div class="preview-label">总销售金额</div>
              <div class="preview-value success">¥{{ money(previewStats.totalSaleAmount) }}</div>
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="20" class="mt15">
          <el-col :span="8">
            <div class="preview-item">
              <div class="preview-label">总缴款金额</div>
              <div class="preview-value success">¥{{ money(previewStats.totalPaymentAmount) }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="preview-item">
              <div class="preview-label">投资来源金额</div>
              <div class="preview-value success">¥{{ money(previewStats.totalInvestAmount) }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="preview-item">
              <div class="preview-label">当前借支</div>
              <div class="preview-value danger">¥{{ money(previewStats.currentAdvance) }}</div>
            </div>
          </el-col>
        </el-row>
        <el-row class="mt15">
          <el-col :span="24">
            <div class="preview-item text-center">
              <div class="preview-label">净利</div>
              <div class="preview-value" :class="Number(previewStats.profit || 0) >= 0 ? 'success' : 'danger'">
                ¥{{ money(previewStats.profit) }}
              </div>
              <div class="preview-formula">（总缴款 - 总花销 - 总进货 - 当前借支）</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :disabled="!previewStats.startDate" @click="submitForm">确认保存</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="viewOpen" title="详情" width="800px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="核算单号">{{ viewForm.accountingNo }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ viewForm.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ viewForm.endDate }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime, '{y}-{m}-{d} {h}:{i}') }}</el-descriptions-item>
        <el-descriptions-item label="总花销费用"><span class="money-danger strong">¥{{ money(viewForm.totalExpense) }}</span></el-descriptions-item>
        <el-descriptions-item label="总进货金额"><span class="money-danger strong">¥{{ money(viewForm.totalPurchase) }}</span></el-descriptions-item>
        <el-descriptions-item label="总销售金额"><span class="money-success strong">¥{{ money(viewForm.totalSale) }}</span></el-descriptions-item>
        <el-descriptions-item label="投资来源金额"><span class="money-primary strong">¥{{ money(viewForm.totalInvest) }}</span></el-descriptions-item>
        <el-descriptions-item label="当前借支"><span class="money-danger strong">¥{{ money(viewForm.currentAdvance) }}</span></el-descriptions-item>
        <el-descriptions-item label="总缴款金额"><span class="money-success strong">¥{{ money(viewForm.totalPayment) }}</span></el-descriptions-item>
        <el-descriptions-item label="净利" :span="2">
          <span :class="Number(viewForm.returnSituation || 0) >= 0 ? 'money-success strong-lg' : 'money-danger strong-lg'">
            ¥{{ money(viewForm.returnSituation) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewForm.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/junsong'
import {
  addCostAccounting,
  checkUnverifiedExpense,
  delCostAccounting,
  getCostAccounting,
  getRealTimeStats,
  listCostAccounting,
  previewCostAccounting,
} from '@/api/finance/costAccounting'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const accountingList = ref<any[]>([])
const deptOptions = ref<any[]>([])
const open = ref(false)
const viewOpen = ref(false)
const title = ref('')
const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  accountingNo: undefined as string | undefined,
})

const realTimeStats = reactive<any>({})
const previewStats = reactive<any>({})
const viewForm = reactive<any>({})
const form = reactive<any>({
  accountingId: undefined,
  accountingNo: undefined,
  startDate: undefined,
  endDate: undefined,
  remark: undefined,
})

const rules = {
  startDate: [{ required: true, message: '开始日期不能为空', trigger: 'change' }],
  endDate: [{ required: true, message: '结束日期不能为空', trigger: 'change' }],
}

function money(value: any) {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed.toFixed(2) : '0.00'
}

function assignReactive(target: any, source: any) {
  Object.keys(target).forEach((key) => delete target[key])
  Object.assign(target, source || {})
}

function resetForm(target: any, defaults: any) {
  assignReactive(target, defaults)
}

function getDeptOptions() {
  deptOptions.value = userStore.depts || []
}

function getList() {
  loading.value = true
  listCostAccounting({ ...queryParams, deptId: userStore.currentDeptId })
    .then((res: any) => {
      accountingList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function refreshRealTime() {
  getRealTimeStats({ deptId: userStore.currentDeptId }).then((res: any) => {
    assignReactive(realTimeStats, res.data || {})
  })
}

function reset() {
  resetForm(form, {
    accountingId: undefined,
    accountingNo: undefined,
    startDate: undefined,
    endDate: undefined,
    remark: undefined,
  })
  resetForm(previewStats, {})
  nextTick(() => {
    formRef.value?.clearValidate?.()
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryFormRef.value?.resetFields?.()
  queryParams.accountingNo = undefined
  handleQuery()
}

function handleAdd() {
  checkUnverifiedExpense({ deptId: userStore.currentDeptId })
    .then((res: any) => {
      const data = res.data || {}
      const hasUnverified = data.hasUnverified || data.hasUnverified === 'true'
      if (hasUnverified) {
        ElMessage.error('有未核销费用，请先核销后再进行成本核算')
        return
      }
      reset()
      open.value = true
      title.value = '新增成本核算'
    })
    .catch(() => {
      reset()
      open.value = true
      title.value = '新增成本核算'
    })
}

function handleDateChange() {
  if (!form.startDate || !form.endDate) {
    resetForm(previewStats, {})
    return
  }
  previewCostAccounting(form.startDate, form.endDate, userStore.currentDeptId).then((res: any) => {
    assignReactive(previewStats, res.data || {})
    previewStats.startDate = form.startDate
  })
}

function handleView(row: any) {
  getCostAccounting(row.accountingId).then((res: any) => {
    assignReactive(viewForm, res.data || {})
    viewOpen.value = true
  })
}

function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除该成本核算？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => delCostAccounting(row.accountingId))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
      refreshRealTime()
    })
    .catch(() => {})
}

function submitForm() {
  if (!formRef.value) return
  formRef.value.validate((valid: boolean) => {
    if (!valid) return
    const data = { ...form, deptId: userStore.currentDeptId }
    addCostAccounting(data).then(() => {
      ElMessage.success('核算成功')
      open.value = false
      getList()
      refreshRealTime()
    })
  })
}

function cancel() {
  open.value = false
  reset()
}

function getPeriodStatusText(status: any) {
  const statusMap: Record<string, string> = { '0': '进行中', '1': '已回本待结转', '2': '已结转' }
  return statusMap[String(status)] || status || '当前周期'
}

function getPeriodStatusType(status: any) {
  const typeMap: Record<string, 'warning' | 'success' | 'info'> = { '0': 'warning', '1': 'success', '2': 'info' }
  return typeMap[String(status)] || 'info'
}

onMounted(() => {
  getDeptOptions()
  getList()
  refreshRealTime()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cost-summary-card {
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
}

.real-time-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dept-select {
  width: 210px;
}

.period-line {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  color: #606266;
  font-size: 13px;
}

.stat-item {
  padding: 15px;
  text-align: center;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-label {
  margin-bottom: 10px;
  color: #606266;
  font-size: 14px;
  font-weight: 700;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.success,
.money-success {
  color: #67c23a;
}

.danger,
.money-danger {
  color: #f56c6c;
}

.money-primary {
  color: #409eff;
}

.strong {
  font-weight: 700;
}

.strong-lg {
  font-size: 18px;
  font-weight: 700;
}

.preview-stats {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.preview-item {
  padding: 10px;
  text-align: center;
}

.preview-label {
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
  font-weight: 700;
}

.preview-value {
  font-size: 20px;
  font-weight: 700;
}

.preview-formula {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}

.mt15 {
  margin-top: 15px;
}

.mt20 {
  margin-top: 20px;
}
</style>
