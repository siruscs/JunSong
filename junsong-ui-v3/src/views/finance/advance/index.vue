<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="借支单号" prop="advanceNo">
        <el-input v-model="queryParams.advanceNo" placeholder="请输入借支单号" clearable style="width: 180px;" />
      </el-form-item>
      <el-form-item label="借款人" prop="borrower">
        <el-input v-model="queryParams.borrower" placeholder="请输入借款人" clearable style="width: 160px;" />
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
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['finance:advance:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['finance:advance:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="advanceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="借支单号" align="center" prop="advanceNo" width="180" />
      <el-table-column label="借支日期" align="center" prop="advanceDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.advanceDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="借支金额" align="center" prop="advanceAmount">
        <template #default="scope">
          <span style="color: #F56C6C; font-weight: bold;">¥{{ scope.row.advanceAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="借款人" align="center" prop="borrower" width="120" />
      <el-table-column label="借支用途" align="center" prop="advancePurpose" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag :type="scope.row.status === '1' ? 'success' : 'warning'">{{ scope.row.status === '1' ? '已核销' : '未核销' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleView(scope.row)">查看</el-button>
          <el-button size="small" type="text" @click="handleUpdate(scope.row)" v-hasPermi="['finance:advance:edit']" v-if="scope.row.status !== '1'">修改</el-button>
          <el-button size="small" type="text" @click="handleDelete(scope.row)" v-hasPermi="['finance:advance:remove']" v-if="scope.row.status !== '1'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="借支日期" prop="advanceDate">
          <el-date-picker v-model="form.advanceDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="借支金额" prop="advanceAmount">
          <el-input-number v-model="form.advanceAmount" :precision="2" :step="0.1" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="借款人" prop="borrower">
          <el-select v-model="form.borrower" placeholder="请选择借款人" filterable style="width: 100%;">
            <el-option v-for="item in staffOptions" :key="item.userId" :label="item.nickName" :value="item.nickName" />
          </el-select>
        </el-form-item>
        <el-form-item label="借支用途" prop="advancePurpose">
          <el-input v-model="form.advancePurpose" type="textarea" placeholder="请输入借支用途" :rows="3" />
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
        <el-descriptions-item label="借支单号">{{ viewForm.advanceNo }}</el-descriptions-item>
        <el-descriptions-item label="借支日期">{{ parseTime(viewForm.advanceDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="借支金额">
          <span style="color: #F56C6C; font-weight: bold;">¥{{ viewForm.advanceAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="借款人">{{ viewForm.borrower }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewForm.status === '1' ? 'success' : 'warning'">{{ viewForm.status === '1' ? '已核销' : '未核销' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime, '{y}-{m}-{d} {h}:{i}') }}</el-descriptions-item>
        <el-descriptions-item label="借支用途" :span="2">{{ viewForm.advancePurpose }}</el-descriptions-item>
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

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdvance, getAdvance, delAdvance, addAdvance, updateAdvance } from '@/api/finance/advance'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: "Advance",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      advanceList: [],
      title: "",
      open: false,
      viewOpen: false,
      viewForm: {},
      staffOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        advanceNo: undefined,
        borrower: undefined,
        status: undefined
      },
      form: {},
      rules: {
        advanceDate: [{ required: true, message: "借支日期不能为空", trigger: "change" }],
        advanceAmount: [{ required: true, message: "借支金额不能为空", trigger: "blur" }],
        borrower: [{ required: true, message: "借款人不能为空", trigger: "change" }],
        advancePurpose: [{ required: true, message: "借支用途不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
    this.getStaffOptions()
  },
  methods: {
    getStaffOptions() {
      const deptId = userStore.currentDeptId
      request({ url: '/system/userDept/staff/' + deptId, method: 'get' }).then(response => {
        this.staffOptions = response.data || []
      })
    },
    getList() {
      this.loading = true
      listAdvance({ ...this.queryParams, deptId: userStore.currentDeptId }).then(response => {
        this.advanceList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        advanceId: undefined,
        advanceNo: undefined,
        advanceDate: new Date().toISOString().split('T')[0],
        advanceAmount: 0,
        borrower: undefined,
        advancePurpose: undefined,
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
      this.ids = selection.map(item => item.advanceId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加借支记录"
    },
    handleUpdate(row) {
      this.reset()
      const advanceId = row.advanceId || this.ids
      getAdvance(advanceId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改借支记录"
      })
    },
    handleView(row) {
      getAdvance(row.advanceId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.advanceId != undefined) {
            updateAdvance(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            this.form.deptId = userStore.currentDeptId
            addAdvance(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const advanceIds = row.advanceId || this.ids
      ElMessageBox.confirm('是否确认删除借支记录编号为"' + advanceIds + '"的数据项？').then(() => {
        return delAdvance(advanceIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
