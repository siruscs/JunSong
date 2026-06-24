<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="供应商编码" prop="supplierCode">
        <el-input
          v-model="queryParams.supplierCode"
          placeholder="请输入供应商编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商名称" prop="supplierName">
        <el-input
          v-model="queryParams.supplierName"
          placeholder="请输入供应商名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="供应商状态" clearable>
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
         
          size="small"
          @click="handleAdd"
          v-hasPermi="['finance:supplier:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
         
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['finance:supplier:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
         
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['finance:supplier:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
         
          size="small"
          @click="handleExport"
          v-hasPermi="['finance:supplier:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="supplierList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="供应商编码" align="center" prop="supplierCode" width="120" />
      <el-table-column label="供应商名称" align="center" prop="supplierName" />
      <el-table-column label="联系人" align="center" prop="contactPerson" width="100" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" width="130" />
      <el-table-column label="地址" align="center" prop="address" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-button
            size="small"
            type="text"
           
            @click="handleView(scope.row)"
          >查看</el-button>
          <el-button
            size="small"
            type="text"
           
            @click="handleUpdate(scope.row)"
            v-hasPermi="['finance:supplier:edit']"
          >修改</el-button>
          <el-button
            size="small"
            type="text"
           
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:supplier:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改供应商对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body class="supplier-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商编码">
              <el-input v-model="form.supplierCode" placeholder="系统自动生成" disabled>
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="供应商名称" prop="supplierName">
          <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" type="textarea" placeholder="请输入地址" :rows="3" />
        </el-form-item>
        <el-form-item label="供应商状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog title="供应商详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="供应商编码">{{ viewForm.supplierCode }}</el-descriptions-item>
        <el-descriptions-item label="供应商名称">{{ viewForm.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ viewForm.contactPerson }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ viewForm.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="dict.type.sys_normal_disable" :value="viewForm.status"/>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-if="viewForm.address" style="margin-top: 20px;" :column="1" border>
        <el-descriptions-item label="地址">{{ viewForm.address }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-if="viewForm.remark" style="margin-top: 20px;" :column="1" border>
        <el-descriptions-item label="备注">{{ viewForm.remark }}</el-descriptions-item>
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
import { useDict } from '@/composables/useDict'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSupplier, getSupplier, delSupplier, addSupplier, updateSupplier } from '@/api/finance/supplier'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: "Supplier",
  setup() {
    const dict = useDict('sys_normal_disable')
    return { dict, userStore }
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 供应商表格数据
      supplierList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查看详情弹出层
      viewOpen: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        supplierCode: undefined,
        supplierName: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 查看表单
      viewForm: {},
      // 表单校验
      rules: {
        supplierName: [
          { required: true, message: "供应商名称不能为空", trigger: "blur" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询供应商列表 */
    getList() {
      this.loading = true
      listSupplier({ ...this.queryParams, deptId: userStore.currentDeptId }).then(response => {
        this.supplierList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        supplierId: undefined,
        supplierCode: undefined,
        supplierName: undefined,
        contactPerson: undefined,
        contactPhone: undefined,
        address: undefined,
        status: "0",
        remark: undefined
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.supplierId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加供应商"
    },
    /** 查看详情 */
    handleView(row) {
      getSupplier(row.supplierId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const supplierId = row.supplierId || this.ids
      getSupplier(supplierId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改供应商"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.supplierId != undefined) {
            updateSupplier(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            // 新增时设置当前店面deptId，后端会自动生成供应商编码
            this.form.deptId = userStore.currentDeptId
            addSupplier(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const supplierIds = row.supplierId || this.ids
      ElMessageBox.confirm('是否确认删除供应商编号为"' + supplierIds + '"的数据项？').then(function() {
        return delSupplier(supplierIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/supplier/export', {
        ...this.queryParams
      }, `supplier_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style lang="scss" scoped>
.supplier-dialog {
  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 20px;
    .el-dialog__title {
      color: white;
      font-weight: bold;
    }
  }
}
</style>
