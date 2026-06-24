<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="商品编码" prop="productCode">
        <el-input
          v-model="queryParams.productCode"
          placeholder="请输入商品编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品名称" prop="productName">
        <el-input
          v-model="queryParams.productName"
          placeholder="请输入商品名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="商品状态" clearable>
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
          v-hasPermi="['finance:product:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
         
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['finance:product:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
         
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['finance:product:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
         
          size="small"
          @click="handleExport"
          v-hasPermi="['finance:product:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="商品编码" align="center" prop="productCode" width="120" />
      <el-table-column label="商品名称" align="center" prop="productName" />
      <el-table-column label="计量单位" align="center" prop="unit" width="100" />
      <el-table-column label="进货价格" align="center" prop="purchasePrice" width="120">
        <template #default="scope">
          <span style="color: #409EFF;">¥{{ scope.row.purchasePrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="销售价格" align="center" prop="salePrice" width="120">
        <template #default="scope">
          <span style="color: #67C23A;">¥{{ scope.row.salePrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="库存数量" align="center" prop="stockNum" width="100" />
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
            v-hasPermi="['finance:product:edit']"
          >修改</el-button>
          <el-button
            size="small"
            type="text"
           
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:product:remove']"
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

    <!-- 添加或修改商品对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body class="product-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品编码">
              <el-input v-model="form.productCode" placeholder="系统自动生成" disabled>
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择" style="width: 100%;">
                <el-option
                  v-for="dict in dict.type.finance_product_unit"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="进货价格" prop="purchasePrice">
              <el-input-number v-model="form.purchasePrice" :precision="2" :step="0.1" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售价格" prop="salePrice">
              <el-input-number v-model="form.salePrice" :precision="2" :step="0.1" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存数量" prop="stockNum">
              <el-input-number v-model="form.stockNum" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低库存预警" prop="minStock">
              <el-input-number v-model="form.minStock" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品状态" prop="status">
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
    <el-dialog title="商品详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品编码">{{ viewForm.productCode }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ viewForm.productName }}</el-descriptions-item>
        <el-descriptions-item label="计量单位">{{ viewForm.unit }}</el-descriptions-item>
        <el-descriptions-item label="进货价格">
          <span style="color: #409EFF;">¥{{ viewForm.purchasePrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="销售价格">
          <span style="color: #67C23A;">¥{{ viewForm.salePrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="库存数量">{{ viewForm.stockNum }}</el-descriptions-item>
        <el-descriptions-item label="最低库存预警">{{ viewForm.minStock }}</el-descriptions-item>
        <el-descriptions-item label="商品状态">
          <dict-tag :options="dict.type.sys_normal_disable" :value="viewForm.status"/>
        </el-descriptions-item>
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
import { listProduct, getProduct, delProduct, addProduct, updateProduct } from '@/api/finance/product'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: "Product",
  setup() {
    const dict = useDict('sys_normal_disable', 'finance_product_unit')
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
      // 商品表格数据
      productList: [],
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
        productCode: undefined,
        productName: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 查看表单
      viewForm: {},
      // 表单校验
      rules: {
        productName: [
          { required: true, message: "商品名称不能为空", trigger: "blur" }
        ],
        unit: [
          { required: true, message: "计量单位不能为空", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询商品列表 */
    getList() {
      this.loading = true
      listProduct({ ...this.queryParams, deptId: userStore.currentDeptId }).then(response => {
        this.productList = response.rows
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
        productId: undefined,
        productCode: undefined,
        productName: undefined,
        categoryId: undefined,
        unit: undefined,
        purchasePrice: undefined,
        salePrice: undefined,
        stockNum: 0,
        minStock: 0,
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
      this.ids = selection.map(item => item.productId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加商品"
    },
    /** 查看详情 */
    handleView(row) {
      getProduct(row.productId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const productId = row.productId || this.ids
      getProduct(productId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改商品"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.productId != undefined) {
            updateProduct(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            // 新增时设置当前店面deptId，后端会自动生成商品编码
            this.form.deptId = userStore.currentDeptId
            addProduct(this.form).then(() => {
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
      const productIds = row.productId || this.ids
      ElMessageBox.confirm('是否确认删除商品编号为"' + productIds + '"的数据项？').then(function() {
        return delProduct(productIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/product/export', {
        ...this.queryParams
      }, `product_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style lang="scss" scoped>
.product-dialog {
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
