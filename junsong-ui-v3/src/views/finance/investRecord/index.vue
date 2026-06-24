<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="机构" prop="deptId">
        <el-select v-model="queryParams.deptId" placeholder="请选择机构" filterable clearable style="width: 200px;" @change="handleQuery">
          <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item label="投资人" prop="investorName">
        <el-input v-model="queryParams.investorName" placeholder="请输入投资人姓名" clearable style="width: 180px;" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['finance:investRecord:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['finance:investRecord:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['finance:investRecord:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="机构" align="center" min-width="160">
        <template #default="scope">{{ getDeptName(scope.row.deptId) }}</template>
      </el-table-column>
      <el-table-column label="投资人" align="center" prop="investorName" min-width="140" />
      <el-table-column label="投资来源金额" align="center" prop="investAmount" width="130">
        <template #default="scope">
          <span class="amount-text">¥{{ formatMoney(scope.row.investAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="投资时间" align="center" prop="investTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.investTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
      </el-table-column>
      <el-table-column label="周期ID" align="center" prop="periodId" width="90" />
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="150">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleUpdate(scope.row)" v-hasPermi="['finance:investRecord:edit']">修改</el-button>
          <el-button size="small" type="text" @click="handleDelete(scope.row)" v-hasPermi="['finance:investRecord:remove']">删除</el-button>
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
        <el-form-item label="投资人" prop="investorId">
          <el-select v-model="form.investorId" placeholder="请选择投资人" filterable style="width: 100%;" @change="handleInvestorChange">
            <el-option v-for="investor in filteredInvestors" :key="investor.investorId" :label="investor.investorName" :value="investor.investorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="投资来源金额" prop="investAmount">
          <el-input-number v-model="form.investAmount" :precision="2" :step="1000" :min="0.01" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="投资时间" prop="investTime">
          <el-date-picker v-model="form.investTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择投资时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="所属周期">
          <el-input :value="form.periodId || '保存时自动绑定当前核算周期'" disabled />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { listInvestor } from '@/api/finance/investor'
import { listInvestRecord, getInvestRecord, addInvestRecord, updateInvestRecord, delInvestRecord } from '@/api/finance/investRecord'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: 'InvestRecord',
  data() {
    return {
      loading: false,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      recordList: [],
      deptOptions: [],
      investorOptions: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deptId: undefined,
        investorName: undefined
      },
      form: {},
      rules: {
        deptId: [{ required: true, message: '机构不能为空', trigger: 'change' }],
        investorId: [{ required: true, message: '投资人不能为空', trigger: 'change' }],
        investAmount: [{ required: true, message: '投资来源金额不能为空', trigger: 'blur' }],
        investTime: [{ required: true, message: '投资时间不能为空', trigger: 'change' }]
      }
    }
  },
  computed: {
    filteredInvestors() {
      if (!this.form.deptId) {
        return this.investorOptions
      }
      return this.investorOptions.filter(item => item.deptId === this.form.deptId && item.status !== '1')
    }
  },
  created() {
    this.getDeptOptions()
    this.getInvestorOptions()
    this.getList()
  },
  methods: {
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
      listInvestRecord({ ...this.queryParams, deptId: userStore.currentDeptId }).then(response => {
        this.recordList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        investId: undefined,
        deptId: undefined,
        periodId: undefined,
        investorId: undefined,
        investorName: undefined,
        investAmount: 0.01,
        investTime: this.parseTime(new Date(), '{y}-{m}-{d} {h}:{i}:{s}'),
        remark: undefined
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.investId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增投资来源记录'
    },
    handleUpdate(row) {
      this.reset()
      const investId = row.investId || this.ids
      getInvestRecord(investId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改投资来源记录'
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
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (!this.form.investId) {
          this.form.deptId = userStore.currentDeptId
        }
        const request = this.form.investId ? updateInvestRecord(this.form) : addInvestRecord(this.form)
        request.then(() => {
          ElMessage.success(this.form.investId ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const investIds = row.investId || this.ids
      ElMessageBox.confirm('是否确认删除选中的投资来源记录？').then(() => delInvestRecord(investIds)).then(() => {
        this.getList()
        ElMessage.success('删除成功')
      }).catch(() => {})
    },
    getDeptName(deptId) {
      const dept = this.deptOptions.find(item => item.deptId === deptId)
      return dept ? dept.deptName : deptId || '-'
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>

<style scoped>
.amount-text {
  color: #409EFF;
  font-weight: 700;
}
</style>
