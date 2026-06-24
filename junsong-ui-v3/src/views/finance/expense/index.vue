<template>
  <div class="app-container">
    <el-row :gutter="20" class="expense-stat-row">
      <el-col :span="6">
        <div class="stat-card stat-card-1">
          <div class="stat-label">未核销借支金额</div>
          <div class="stat-value">¥{{ summary.unverifiedAdvanceAmount || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-2">
          <div class="stat-label">未核销费用总金额</div>
          <div class="stat-value">¥{{ summary.unverifiedExpenseAmount || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-3">
          <div class="stat-label">借支余额</div>
          <div class="stat-value" :class="(summary.advanceBalance || 0) >= 0 ? 'positive' : 'negative'">¥{{ summary.advanceBalance || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-4">
          <div class="stat-label">当前核销周期总费用</div>
          <div class="stat-value">¥{{ summary.totalExpenseAmount || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="费用单号" prop="expenseNo">
        <el-input v-model="queryParams.expenseNo" placeholder="请输入费用单号" clearable style="width: 180px;" />
      </el-form-item>
      <el-form-item label="费用类别" prop="expenseType">
        <el-select v-model="queryParams.expenseType" placeholder="请选择费用类别" clearable style="width: 160px;">
          <el-option v-for="dict in dict.type.finance_expense_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="费用内容" prop="expenseContent">
        <el-input v-model="queryParams.expenseContent" placeholder="请输入费用内容" clearable style="width: 180px;" />
      </el-form-item>
      <el-form-item label="费用日期" prop="expenseDate">
        <el-date-picker v-model="queryParams.expenseDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" clearable style="width: 160px;" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px;">
          <el-option label="未核销" value="0" />
          <el-option label="已核销" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['finance:expense:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain size="small" @click="handleImport" v-hasPermi="['finance:expense:import']">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain size="small" @click="handleExport" v-hasPermi="['finance:expense:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain size="small" :disabled="deleteDisabled" @click="handleDelete" v-hasPermi="['finance:expense:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain size="small" :disabled="multiple" @click="handleBatchVerify" v-hasPermi="['finance:expense:edit']">批量核销</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="expenseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="费用单号" align="center" prop="expenseNo" width="180" />
      <el-table-column label="费用日期" align="center" prop="expenseDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expenseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="费用类别" align="center" prop="expenseType">
        <template #default="scope">
          <el-tag :type="getExpenseTypeColor(scope.row.expenseType)">{{ expenseTypeFormat(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="费用金额" align="center" prop="expenseAmount">
        <template #default="scope">
          <span :style="scope.row.expenseType === '收入' ? 'color: #67C23A;' : 'color: #F56C6C;'">
            ¥{{ scope.row.expenseAmount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="花销内容" align="center" prop="expenseContent" min-width="180" show-overflow-tooltip />
      <el-table-column label="付款方式" align="center" prop="paymentMethod" width="120">
        <template #default="scope">
          <span>{{ paymentMethodFormat(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag :type="scope.row.status === '1' ? 'success' : 'warning'">{{ scope.row.status === '1' ? '已核销' : '未核销' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleView(scope.row)">查看</el-button>
          <el-button size="small" type="text" @click="handleUpdate(scope.row)" v-hasPermi="['finance:expense:edit']">修改</el-button>
          <el-button size="small" type="text" @click="handleVerify(scope.row)" v-if="scope.row.status !== '1'" v-hasPermi="['finance:expense:edit']">核销</el-button>
          <el-button size="small" type="text" @click="handleDelete(scope.row)" v-if="scope.row.status !== '1'" v-hasPermi="['finance:expense:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="费用日期" prop="expenseDate">
          <el-date-picker v-model="form.expenseDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="费用类别" prop="expenseType">
          <el-select v-model="form.expenseType" placeholder="请选择费用类别" style="width: 100%;" @change="handleExpenseTypeChange">
            <el-option v-for="dict in dict.type.finance_expense_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="花销内容" prop="expenseContent">
          <el-input v-model="form.expenseContent" type="textarea" placeholder="请输入花销内容" :rows="3" />
        </el-form-item>
        <el-form-item label="费用金额" prop="expenseAmount">
          <el-input v-model="form.expenseAmount" placeholder="请输入费用金额" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="付款方式" prop="paymentMethod">
          <el-select v-model="form.paymentMethod" placeholder="请选择付款方式" style="width: 100%;">
            <el-option v-for="dict in dict.type.finance_payment_method" :key="dict.value" :label="dict.label" :value="dict.label" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
      </template>
    </el-dialog>

    <el-dialog title="详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="费用单号">{{ viewForm.expenseNo }}</el-descriptions-item>
        <el-descriptions-item label="费用日期">{{ parseTime(viewForm.expenseDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="费用类别">
          <el-tag :type="getExpenseTypeColor(viewForm.expenseType)">{{ expenseTypeFormat(viewForm) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="费用金额">
          <span :style="viewForm.expenseType === '收入' ? 'color: #67C23A; font-weight: bold;' : 'color: #F56C6C; font-weight: bold;'">
            ¥{{ viewForm.expenseAmount }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ viewForm.paymentMethod }}</el-descriptions-item>
        <el-descriptions-item label="关联借支">{{ viewForm.advanceNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewForm.status === '1' ? 'success' : 'warning'">{{ viewForm.status === '1' ? '已核销' : '未核销' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime, '{y}-{m}-{d} {h}:{i}') }}</el-descriptions-item>
        <el-descriptions-item label="花销内容" :span="2">{{ viewForm.expenseContent }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewForm.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
      </template>
    </el-dialog>

    <el-dialog :title="verifyDialogTitle" v-model="batchVerifyOpen" width="600px" append-to-body>
      <el-descriptions :column="1" border class="mb20">
        <el-descriptions-item label="已选择费用记录数">
          <span class="highlight">{{ selectedExpenses.length }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="总核销金额">
          <span class="highlight amount">¥{{ totalVerifyAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedAdvances.length > 0" label="总借支金额">
          <span class="highlight amount">¥{{ totalAdvanceAmount }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form ref="batchVerifyForm" :model="batchVerifyForm" :rules="batchVerifyRules" label-width="100px">
        <el-form-item label="选择借支" prop="advanceIds">
          <el-select v-model="batchVerifyForm.advanceIds" multiple placeholder="请选择借支记录" style="width: 100%;" @change="handleAdvanceChange">
            <el-option v-for="item in unverifiedAdvances" :key="item.advanceId" :label="item.advanceNo + ' (¥' + item.advanceAmount + ', 借款人: ' + item.borrower + ')'" :value="item.advanceId" />
          </el-select>
        </el-form-item>
        <el-descriptions v-if="selectedAdvances.length > 0" :column="1" border class="mb20">
          <el-descriptions-item label="已选借支单号">{{ selectedAdvances.map(a => a.advanceNo).join(', ') }}</el-descriptions-item>
          <el-descriptions-item label="差额" :label-class-name="getDiffLabelClass()">
            <span :class="getDiffClass()">
              ¥{{ verifyDiff }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="操作说明">{{ getOperationDesc() }}</el-descriptions-item>
        </el-descriptions>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitBatchVerify">确 认</el-button>
        <el-button @click="batchVerifyOpen = false">取 消</el-button>
      </div>
      </template>
    </el-dialog>

    <ExcelImportDialog
      ref="importExpenseRef"
      title="费用记录导入"
      action="/finance/expense/importData"
      template-action="/finance/expense/importTemplate"
      template-file-name="expense_template"
      update-support-label="是否更新已经存在的费用数据"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script>
import { useDict } from '@/composables/useDict'
import { ElMessage, ElMessageBox } from 'element-plus'
import { parseTime, selectDictLabel } from '@/utils/junsong'
import { listExpense, getExpense, delExpense, addExpense, updateExpense, verifyExpense, getExpenseSummary, batchVerifyExpense, listUnverifiedAdvances } from '@/api/finance/expense'
import ExcelImportDialog from '@/components/ExcelImportDialog/index.vue'
import { useUserStore } from '@/stores/user'

const dict = useDict('finance_expense_type', 'finance_payment_method')
const userStore = useUserStore()

export default {
  name: "Expense",
  components: {
    ExcelImportDialog
  },
  data() {
    return {
      dict,
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      expenseList: [],
      unverifiedAdvances: [],
      summary: {},
      title: "",
      open: false,
      viewOpen: false,
      viewForm: {},
      batchVerifyOpen: false,
      verifyDialogTitle: "批量核销",
      selectedExpenses: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        expenseNo: undefined,
        expenseType: undefined,
        expenseContent: undefined,
        expenseDate: undefined,
        status: undefined
      },
      form: {},
      batchVerifyForm: {
        expenseIds: [],
        advanceIds: []
      },
      rules: {
        expenseDate: [{ required: true, message: "费用日期不能为空", trigger: "change" }],
        expenseType: [{ required: true, message: "费用类别不能为空", trigger: "change" }],
        expenseContent: [{ required: true, message: "花销内容不能为空", trigger: "blur" }],
        expenseAmount: [{ required: true, message: "费用金额不能为空", trigger: "blur" }],
        paymentMethod: [{ required: true, message: "付款方式不能为空", trigger: "change" }]
      },
      batchVerifyRules: {
      },
      importExpenseRef: null
    }
  },
  computed: {
    totalVerifyAmount() {
      return this.selectedExpenses.reduce((sum, item) => sum + Number(item.expenseAmount || 0), 0).toFixed(2)
    },
    totalAdvanceAmount() {
      return this.selectedAdvances.reduce((sum, item) => sum + Number(item.advanceAmount || 0), 0).toFixed(2)
    },
    verifyDiff() {
      if (!this.selectedAdvances.length === 0) return 0
      return (Number(this.totalVerifyAmount) - Number(this.totalAdvanceAmount)).toFixed(2)
    },
    selectedAdvances() {
      return this.batchVerifyForm.advanceIds.map(advanceId =>
        this.unverifiedAdvances.find(item => item.advanceId === advanceId) || null
      ).filter(item => item !== null)
    },
    deleteDisabled() {
      return this.multiple || this.selectedExpenses.some(item => item.status === '1')
    }
  },
  created() {
    this.getList()
    this.getSummary()
  },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.queryParams, deptId: userStore.currentDeptId }
      if (this.queryParams.expenseDate) {
        params.params = params.params || {}
        params.params.beginTime = this.queryParams.expenseDate
        params.params.endTime = this.queryParams.expenseDate
      }
      listExpense(params).then(response => {
        this.expenseList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getSummary() {
      const deptId = userStore.currentDeptId
      getExpenseSummary(deptId).then(response => {
        this.summary = response.data
      })
    },
    getUnverifiedAdvances() {
      const deptId = userStore.currentDeptId
      listUnverifiedAdvances(deptId).then(response => {
        this.unverifiedAdvances = response.rows || []
      })
    },
    getExpenseTypeColor(type) {
      const colorMap = { '预支': 'warning', '开支': 'danger', '收入': 'success' }
      return colorMap[type] || 'info'
    },
    expenseTypeFormat(row) {
      return selectDictLabel(this.dict.type.finance_expense_type, row.expenseType)
    },
    paymentMethodFormat(row) {
      return selectDictLabel(this.dict.type.finance_payment_method, row.paymentMethod) || row.paymentMethod || '-'
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        expenseId: undefined,
        expenseNo: undefined,
        expenseDate: new Date().toISOString().split('T')[0],
        expenseType: undefined,
        expenseContent: undefined,
        expenseAmount: 0,
        paymentMethod: undefined,
        advanceId: undefined,
        advanceNo: undefined,
        status: "0",
        remark: undefined
      }
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.expenseId)
      this.single = selection.length != 1
      this.multiple = !selection.length
      this.selectedExpenses = selection
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加费用记录"
    },
    handleImport() {
      this.$refs.importExpenseRef.open()
    },
    handleImportSuccess() {
      this.getList()
      this.getSummary()
    },
    handleExport() {
      this.download('finance/expense/export', {
        ...this.queryParams
      }, `expense_${new Date().getTime()}.xlsx`)
    },
    handleUpdate(row) {
      this.reset()
      const expenseId = row.expenseId || this.ids
      getExpense(expenseId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改费用记录"
      })
    },
    handleView(row) {
      getExpense(row.expenseId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    handleVerify(row) {
      if (row.status === '1') return
      this.getUnverifiedAdvances()
      this.selectedExpenses = [row]
      this.batchVerifyForm = {
        expenseIds: [row.expenseId],
        advanceIds: []
      }
      this.verifyDialogTitle = "费用核销"
      this.batchVerifyOpen = true
    },
    handleExpenseTypeChange(type) {
      if (type === '收入') {
        this.form.paymentMethod = '收入'
        // 如果金额自动转负数
        if (this.form.expenseAmount !== undefined && this.form.expenseAmount !== null) {
          let amount = parseFloat(this.form.expenseAmount)
          if (!isNaN(amount) && amount > 0) {
            this.form.expenseAmount = -amount
          }
        }
      }
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.expenseId != undefined) {
            updateExpense(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
              this.getSummary()
            })
          } else {
            addExpense(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
              this.getSummary()
            })
          }
        }
      })
    },
    handleDelete(row = {}) {
      if (row.status === '1') {
        ElMessage.warning('已核销费用不能删除')
        return
      }
      if (!row.expenseId && this.selectedExpenses.some(item => item.status === '1')) {
        ElMessage.warning('已核销费用不能删除')
        return
      }
      const expenseIds = row.expenseId || this.ids
      ElMessageBox.confirm('是否确认删除费用记录编号为"' + expenseIds + '"的数据项？').then(() => {
        return delExpense(expenseIds)
      }).then(() => {
        this.getList()
        this.getSummary()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleBatchVerify() {
      // 检查是否选择了未核销的记录
      const hasVerified = this.selectedExpenses.some(item => item.status === '1')
      if (hasVerified) {
        ElMessage.error("请只选择未核销的费用记录进行核销")
        return
      }
      this.getUnverifiedAdvances()
      this.batchVerifyForm = {
        expenseIds: this.ids,
        advanceIds: []
      }
      this.verifyDialogTitle = "批量核销"
      this.batchVerifyOpen = true
    },
    handleAdvanceChange(advanceIds) {
      // 不需要额外处理，selectedAdvances 是计算属性
    },
    getDiffLabelClass() {
      if (this.selectedAdvances.length === 0) return ''
      const diff = Number(this.verifyDiff)
      if (diff === 0) return ''
      return diff > 0 ? 'text-danger' : 'text-success'
    },
    getDiffClass() {
      if (this.selectedAdvances.length === 0) return ''
      const diff = Number(this.verifyDiff)
      if (diff === 0) return 'highlight amount'
      return diff > 0 ? 'highlight amount text-danger' : 'highlight amount text-success'
    },
    getOperationDesc() {
      if (this.selectedAdvances.length === 0) return ''
      const diff = Number(this.verifyDiff)
      if (diff === 0) {
        return '核销总金额等于借支金额，将同时核销选中的费用记录和所有选择的借支记录'
      } else if (diff > 0) {
        return `核销总金额大于借支金额，将同时：1.核销选中的费用记录；2.核销所有选择的借支记录；3.生成新的已核销借支记录(金额: ¥${Math.abs(diff)}, 备注:核销补费用)`
      } else {
        return `核销总金额小于借支金额，将同时：1.核销选中的费用记录；2.核销所有选择的借支记录；3.生成新的未核销借支记录(金额: ¥${Math.abs(diff)}, 备注:核销节余)`
      }
    },
    submitBatchVerify() {
      ElMessageBox.confirm('是否确认批量核销选中的费用记录？').then(() => {
        return batchVerifyExpense(this.batchVerifyForm)
      }).then(() => {
        this.batchVerifyOpen = false
        this.getList()
        this.getSummary()
        ElMessage.success("批量核销成功")
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.expense-stat-row {
  margin-bottom: 12px;
}

.stat-card {
  background: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-primary-light) 100%);
  padding: 10px 16px;
  border-radius: 6px;
  color: white;
  text-align: center;
  box-shadow: 0 4px 12px rgba(var(--theme-primary-rgb), 0.2);
}
.stat-card-2 {
  background: linear-gradient(135deg, var(--theme-primary-dark) 0%, var(--theme-primary) 100%);
}
.stat-card-3 {
  background: linear-gradient(135deg, var(--theme-primary-light) 0%, var(--theme-primary) 100%);
}
.stat-card-4 {
  background: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-primary-dark) 100%);
}
.stat-label {
  font-size: 12px;
  font-weight: 500;
  opacity: 0.9;
  margin-bottom: 4px;
}
.stat-value {
  font-size: 20px;
  font-weight: bold;
}
.stat-value.positive {
  color: #67C23A;
}
.stat-value.negative {
  color: #F56C6C;
}
.highlight {
  font-weight: bold;
  color: #409EFF;
}
.highlight.amount {
  font-size: 18px;
}
.text-danger {
  color: #F56C6C !important;
}
.text-success {
  color: #67C23A !important;
}
</style>
