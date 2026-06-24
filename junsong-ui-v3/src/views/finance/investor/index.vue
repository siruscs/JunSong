<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="机构" prop="deptId">
        <el-select v-model="queryParams.deptId" placeholder="请选择机构" filterable clearable style="width: 200px;">
          <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item label="投资人" prop="investorName">
        <el-input v-model="queryParams.investorName" placeholder="请输入投资人姓名" clearable style="width: 180px;" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 140px;">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['finance:investor:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['finance:investor:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['finance:investor:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="investorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="投资人" align="center" prop="investorName" min-width="140" />
      <el-table-column label="机构" align="center" min-width="160">
        <template #default="scope">{{ getDeptName(scope.row.deptId) }}</template>
      </el-table-column>
      <el-table-column label="联系电话" align="center" prop="phone" width="140" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="150">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleUpdate(scope.row)" v-hasPermi="['finance:investor:edit']">修改</el-button>
          <el-button size="small" type="text" @click="handleDelete(scope.row)" v-hasPermi="['finance:investor:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="机构" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择机构" filterable style="width: 100%;">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="投资人" prop="investorName">
          <el-input v-model="form.investorName" placeholder="请输入投资人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
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
import { listInvestor, getInvestor, addInvestor, updateInvestor, delInvestor } from '@/api/finance/investor'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: 'Investor',
  data() {
    return {
      loading: false,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      investorList: [],
      deptOptions: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deptId: undefined,
        investorName: undefined,
        status: undefined
      },
      form: {},
      rules: {
        deptId: [{ required: true, message: '机构不能为空', trigger: 'change' }],
        investorName: [{ required: true, message: '投资人姓名不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getDeptOptions()
    this.getList()
  },
  methods: {
    getDeptOptions() {
      this.deptOptions = userStore.depts || []
    },
    getList() {
      this.loading = true
      listInvestor({ ...this.queryParams, deptId: userStore.currentDeptId }).then(response => {
        this.investorList = response.rows || []
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
        investorId: undefined,
        deptId: undefined,
        investorName: undefined,
        phone: undefined,
        status: '0',
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
      this.ids = selection.map(item => item.investorId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增投资人'
    },
    handleUpdate(row) {
      this.reset()
      const investorId = row.investorId || this.ids
      getInvestor(investorId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改投资人'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (!this.form.investorId) {
          this.form.deptId = userStore.currentDeptId
        }
        const request = this.form.investorId ? updateInvestor(this.form) : addInvestor(this.form)
        request.then(() => {
          ElMessage.success(this.form.investorId ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const investorIds = row.investorId || this.ids
      ElMessageBox.confirm('是否确认删除选中的投资人？').then(() => delInvestor(investorIds)).then(() => {
        this.getList()
        ElMessage.success('删除成功')
      }).catch(() => {})
    },
    getDeptName(deptId) {
      const dept = this.deptOptions.find(item => item.deptId === deptId)
      return dept ? dept.deptName : deptId || '-'
    }
  }
}
</script>
