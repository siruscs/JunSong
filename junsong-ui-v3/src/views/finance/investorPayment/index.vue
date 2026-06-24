<template>
  <div class="app-container">
    <!-- 统计数据 -->
    <el-row :gutter="20" class="summary-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card primary">
          <div class="stat-label">投资来源金额</div>
          <div class="stat-value">¥{{ summary.totalInvestAmount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card warning">
          <div class="stat-label">总返款金额</div>
          <div class="stat-value">¥{{ summary.totalReturnAmount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card info">
          <div class="stat-label">备用金</div>
          <div class="stat-value">¥{{ summary.reserveFund || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card success">
          <div class="stat-label">费用余额</div>
          <div class="stat-value">¥{{ summary.expenseBalance || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px" class="payment-query-form">
      <el-form-item label="机构" prop="deptId">
        <el-select v-model="queryParams.deptId" placeholder="请选择机构" filterable clearable style="width: 180px;">
          <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item label="返款单号" prop="paymentNo">
        <el-input v-model="queryParams.paymentNo" placeholder="请输入返款单号" clearable style="width: 180px;" />
      </el-form-item>
      <el-form-item label="投资人" prop="investorName">
        <el-input v-model="queryParams.investorName" placeholder="请输入投资人" clearable style="width: 160px;" />
      </el-form-item>
      <el-form-item label="类型" prop="paymentType">
        <el-select v-model="queryParams.paymentType" placeholder="请选择类型" clearable style="width: 160px;">
          <el-option label="返款" value="return" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="请选择来源" clearable style="width: 160px;">
          <el-option label="手工返款" value="0" />
          <el-option label="结转自动返款" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="paymentStatus">
        <el-select v-model="queryParams.paymentStatus" placeholder="请选择状态" clearable style="width: 140px;">
          <el-option label="待返款" value="0" />
          <el-option label="已返款" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['finance:investorPayment:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['finance:investorPayment:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="paymentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="返款单号" align="center" prop="paymentNo" width="180" />
      <el-table-column label="机构" align="center" width="130">
        <template #default="scope">{{ getDeptName(scope.row.deptId) }}</template>
      </el-table-column>
      <el-table-column label="日期" align="center" prop="paymentDate" width="120" />
      <el-table-column label="类型" align="center" prop="paymentType">
        <template #default="scope">
          <el-tag type="danger">{{ scope.row.paymentType === 'return' ? '返款' : scope.row.paymentType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="投资人" align="center" prop="investorName" width="120" />
      <el-table-column label="金额" align="center" prop="amount">
        <template #default="scope">
          <span style="color: #F56C6C; font-weight: bold;">
            ¥{{ scope.row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="来源" align="center" prop="sourceType" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.sourceType === '1' ? 'success' : 'info'">{{ getSourceText(scope.row.sourceType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="paymentStatus" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.paymentStatus === '1' ? 'success' : 'warning'">{{ getPaymentStatusText(scope.row.paymentStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="投资占比" align="center" prop="investRatio" width="100">
        <template #default="scope">{{ formatRate(scope.row.investRatio) }}</template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button size="small" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-if="scope.row.sourceType !== '1'" v-hasPermi="['finance:investorPayment:edit']">修改</el-button>
          <el-button size="small" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-if="scope.row.sourceType !== '1'" v-hasPermi="['finance:investorPayment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="机构" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择机构" filterable style="width: 100%;" @change="handleFormDeptChange">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="paymentDate">
          <el-date-picker v-model="form.paymentDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="类型" prop="paymentType">
          <el-input value="返款" disabled />
        </el-form-item>
        <el-form-item label="投资人" prop="investorName">
          <el-select v-model="form.investorId" placeholder="请选择投资人" filterable clearable style="width: 100%;" @change="handleInvestorChange">
            <el-option v-for="investor in filteredInvestors" :key="investor.investorId" :label="investor.investorName" :value="investor.investorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :precision="2" :step="0.1" :min="0.01" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <el-dialog title="详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="返款单号">{{ viewForm.paymentNo }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ viewForm.paymentDate }}</el-descriptions-item>
        <el-descriptions-item label="机构">{{ getDeptName(viewForm.deptId) }}</el-descriptions-item>
        <el-descriptions-item label="周期ID">{{ viewForm.periodId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag type="danger">{{ viewForm.paymentType === 'return' ? '返款' : viewForm.paymentType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="投资人">{{ viewForm.investorName }}</el-descriptions-item>
        <el-descriptions-item label="来源">
          <el-tag :type="viewForm.sourceType === '1' ? 'success' : 'info'">{{ getSourceText(viewForm.sourceType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewForm.paymentStatus === '1' ? 'success' : 'warning'">{{ getPaymentStatusText(viewForm.paymentStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="金额">
          <span style="color: #F56C6C; font-weight: bold;">
            ¥{{ viewForm.amount }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="投资占比">{{ formatRate(viewForm.investRatio) }}</el-descriptions-item>
        <el-descriptions-item label="分润单ID">{{ viewForm.shareId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分润明细ID">{{ viewForm.shareDetailId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime, '{y}-{m}-{d} {h}:{i}') }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewForm.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from "element-plus"
import { parseTime } from "@/utils/junsong"
import { listInvestorPayment, getInvestorPayment, delInvestorPayment, addInvestorPayment, updateInvestorPayment, getInvestorPaymentSummary } from "@/api/finance/investorPayment"
import { listInvestor } from "@/api/finance/investor"
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: "InvestorPayment",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      paymentList: [],
      selectedPayments: [],
      deptOptions: [],
      investorOptions: [],
      summary: {},
      title: "",
      open: false,
      viewOpen: false,
      viewForm: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deptId: undefined,
        paymentNo: undefined,
        investorName: undefined,
        paymentType: 'return',
        sourceType: undefined,
        paymentStatus: undefined
      },
      form: {},
      rules: {
        paymentDate: [{ required: true, message: "日期不能为空", trigger: "change" }],
        paymentType: [{ required: true, message: "类型不能为空", trigger: "change" }],
        investorName: [{ required: true, message: "投资人不能为空", trigger: "blur" }],
        amount: [{ required: true, message: "金额不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getDeptOptions()
    this.getInvestorOptions()
    this.getList()
    this.getSummary()
  },
  computed: {
    filteredInvestors() {
      if (!this.form.deptId) {
        return this.investorOptions
      }
      return this.investorOptions.filter(item => item.deptId === this.form.deptId && item.status !== '1')
    }
  },
  methods: {
    parseTime,
    resetForm(formName) {
      this.$refs[formName]?.resetFields?.()
    },
    getDeptOptions() {
      this.deptOptions = userStore.depts || []
    },
    getInvestorOptions() {
      listInvestor({ pageNum: 1, pageSize: 9999, status: '0' }).then(response => {
        this.investorOptions = response.rows || []
      })
    },
    getList() {
      this.loading = true
      listInvestorPayment({ ...this.queryParams, deptId: userStore.currentDeptId }).then(response => {
        this.paymentList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getSummary() {
      getInvestorPaymentSummary({ deptId: userStore.currentDeptId }).then(response => {
        this.summary = response.data
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        paymentId: undefined,
        paymentNo: undefined,
        deptId: undefined,
        investorId: undefined,
        paymentDate: new Date().toISOString().split('T')[0],
        paymentType: 'return',
        investorName: undefined,
        amount: 0.01,
        sourceType: '0',
        paymentStatus: '1',
        remark: undefined
      }
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
      this.getSummary()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.selectedPayments = selection
      this.ids = selection.map(item => item.paymentId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增投资人返款"
    },
    handleUpdate(row) {
      if (row.sourceType === '1') {
        ElMessage.warning("结转自动生成的返款记录不允许手工修改")
        return
      }
      this.reset()
      const paymentId = row.paymentId || this.ids
      getInvestorPayment(paymentId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改投资人返款"
      })
    },
    handleFormDeptChange() {
      this.form.investorId = undefined
      this.form.investorName = undefined
    },
    handleInvestorChange(investorId) {
      const investor = this.investorOptions.find(item => item.investorId === investorId)
      if (investor) {
        this.form.investorName = investor.investorName
        this.form.deptId = investor.deptId
      }
    },
    handleView(row) {
      getInvestorPayment(row.paymentId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.paymentId != undefined) {
            updateInvestorPayment(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
              this.getSummary()
            })
          } else {
            this.form.deptId = userStore.currentDeptId
            addInvestorPayment(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
              this.getSummary()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const checkedRows = row.paymentId ? [row] : this.selectedPayments
      if (checkedRows.some(item => item.sourceType === '1')) {
        ElMessage.warning("结转自动生成的返款记录不允许手工删除")
        return
      }
      const paymentIds = row.paymentId || this.ids
      ElMessageBox.confirm('是否确认删除该数据项？').then(() => {
        return delInvestorPayment(paymentIds)
      }).then(() => {
        this.getList()
        this.getSummary()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    getDeptName(deptId) {
      const dept = this.deptOptions.find(item => item.deptId === deptId)
      return dept ? dept.deptName : deptId || '-'
    },
    getSourceText(sourceType) {
      return sourceType === '1' ? '结转自动返款' : '手工返款'
    },
    getPaymentStatusText(status) {
      return status === '0' ? '待返款' : '已返款'
    },
    formatRate(rate) {
      if (rate === undefined || rate === null || rate === '') {
        return '-'
      }
      const value = Number(rate || 0)
      return value <= 1 ? (value * 100).toFixed(2) + '%' : value.toFixed(2) + '%'
    }
  }
}
</script>

<style scoped>
.summary-row {
  margin-bottom: 18px;
}

.payment-query-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 12px 16px;
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.payment-query-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.stat-card {
  position: relative;
  min-height: 86px;
  padding: 14px 16px;
  border: 1px solid rgba(var(--theme-primary-rgb), 0.14);
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(var(--theme-primary-rgb), 0.12), rgba(var(--theme-primary-rgb), 0.04));
  color: #303133;
  box-shadow: 0 8px 22px rgba(var(--theme-primary-rgb), 0.08);
}

.stat-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--theme-primary);
}

.stat-card::after {
  content: "";
  position: absolute;
  right: -34px;
  top: -44px;
  width: 110px;
  height: 110px;
  border-radius: 999px;
  background: rgba(var(--theme-primary-rgb), 0.1);
}

.stat-card.primary {
  --stat-tint: rgba(var(--theme-primary-rgb), 0.12);
}

.stat-card.warning {
  --stat-tint: rgba(var(--theme-primary-rgb), 0.09);
}

.stat-card.info {
  --stat-tint: rgba(var(--theme-primary-rgb), 0.08);
}

.stat-card.success {
  --stat-tint: rgba(var(--theme-primary-rgb), 0.1);
}

.stat-label {
  position: relative;
  z-index: 1;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.stat-value {
  position: relative;
  z-index: 1;
  color: var(--theme-primary-dark);
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
