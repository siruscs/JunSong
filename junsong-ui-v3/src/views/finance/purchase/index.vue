<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="进货单号" prop="purchaseNo">
        <el-input
          v-model="queryParams.purchaseNo"
          placeholder="请输入进货单号"
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
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option label="草稿" value="0" />
          <el-option label="已确认" value="1" />
          <el-option label="已完成" value="2" />
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
          v-hasPermi="['finance:purchase:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
         
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['finance:purchase:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
         
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['finance:purchase:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
         
          size="small"
          @click="handleExport"
          v-hasPermi="['finance:purchase:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="purchaseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="进货单号" align="center" prop="purchaseNo" width="180" />
      <el-table-column label="供应商名称" align="center" prop="supplierName" />
      <el-table-column label="进货日期" align="center" prop="purchaseDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.purchaseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总金额" align="center" prop="totalAmount">
        <template #default="scope">
          <span style="color: #409EFF; font-weight: bold;">¥{{ scope.row.totalAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总数量" align="center" prop="totalQuantity" />
      <el-table-column label="付款方式" align="center" prop="paymentMethod">
        <template #default="scope">
          <dict-tag :options="dict.type.finance_payment_method" :value="scope.paymentMethod" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="statusOptions" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="收货人姓名" align="center" prop="receiverName" width="100">
        <template #default="scope">
          <span>{{ scope.row.receiverName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收货人电话" align="center" prop="receiverPhone" width="120">
        <template #default="scope">
          <span>{{ scope.row.receiverPhone || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
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
            v-hasPermi="['finance:purchase:edit']"
          >修改</el-button>
          <el-button
            size="small"
            type="text"
           
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:purchase:remove']"
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

    <!-- 添加或修改进货单对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body :close-on-click-modal="false" class="purchase-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="进货单号" prop="purchaseNo">
              <el-input v-model="form.purchaseNo" placeholder="自动生成" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商" style="width: 100%;" @change="handleSupplierChange">
                <el-option
                  v-for="item in supplierOptions"
                  :key="item.supplierId"
                  :label="item.supplierName"
                  :value="item.supplierId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="进货日期" prop="purchaseDate">
              <el-date-picker v-model="form.purchaseDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式" prop="paymentMethod">
          <el-select v-model="form.paymentMethod" placeholder="请选择付款方式" style="width: 100%;">
            <el-option v-for="dict in dict.type.finance_payment_method" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="总金额" prop="totalAmount">
              <el-input-number v-model="form.totalAmount" :precision="2" :step="0.1" :min="0" style="width: 100%;" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已付金额" prop="paidAmount">
              <el-input-number v-model="form.paidAmount" :precision="2" :step="0.1" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="总数量" prop="totalQuantity">
              <el-input-number v-model="form.totalQuantity" :min="0" style="width: 100%;" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%;">
                <el-option label="草稿" value="0" />
                <el-option label="已确认" value="1" />
                <el-option label="已完成" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 收货人信息 -->
        <el-divider content-position="left">收货人信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="收货人姓名" prop="receiverName">
              <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货人电话" prop="receiverPhone">
              <el-input v-model="form.receiverPhone" placeholder="请输入收货人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货人地址" prop="receiverAddress">
              <el-input v-model="form.receiverAddress" placeholder="请输入收货人地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 商品明细 -->
        <el-form-item label="商品明细">
          <div class="detail-section">
            <el-button type="primary" size="small" @click="handleAddDetail">添加商品</el-button>
            <el-table :data="form.details" border style="width: 100%; margin-top: 10px;">
              <el-table-column label="商品" min-width="180">
                <template #default="scope">
                  <el-select v-model="scope.row.productId" placeholder="请选择商品" style="width: 100%;" @change="(val) => handleProductChange(val, scope.$index)">
                    <el-option
                      v-for="item in productOptions"
                      :key="item.productId"
                      :label="item.productName"
                      :value="item.productId"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="单位" width="80" align="center">
                <template #default="scope">{{ scope.row.unit }}</template>
              </el-table-column>
              <el-table-column label="数量" width="150" align="center">
                <template #default="scope">
                  <el-input-number v-model="scope.row.quantity" :min="-99999" :step="1" size="medium" style="width: 100%;" @change="calculateAmount(scope.$index)" />
                </template>
              </el-table-column>
              <el-table-column label="单价" width="160" align="center">
                <template #default="scope">
                  <el-input-number v-model="scope.row.price" :precision="2" :min="0" :step="1" size="medium" style="width: 100%;" @change="calculateAmount(scope.$index)" :disabled="scope.row.isGift === '1'" />
                </template>
              </el-table-column>
              <el-table-column label="金额" width="120" align="center">
                <template #default="scope">
                  <span :style="{ color: scope.row.isGift === '1' ? '#909399' : '#409EFF' }">{{ scope.row.isGift === '1' ? '赠品' : '¥' + scope.row.amount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="赠品" width="80" align="center">
                <template #default="scope">
                  <el-checkbox v-model="scope.row.isGift" true-value="1" false-value="0" @change="calculateAmount(scope.$index)" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60" align="center">
                <template #default="scope">
                  <el-button type="danger" size="small" circle @click="handleDeleteDetail(scope.$index)" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="3" />
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
    <el-dialog title="进货单详情" v-model="viewOpen" width="900px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="进货单号">{{ viewForm.purchaseNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ viewForm.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="进货日期">{{ parseTime(viewForm.purchaseDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">
          <dict-tag :options="dict.type.finance_payment_method" :value="viewForm.paymentMethod" />
        </el-descriptions-item>
        <el-descriptions-item label="总金额">
          <span style="color: #409EFF; font-weight: bold;">¥{{ viewForm.totalAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="已付金额">¥{{ viewForm.paidAmount }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ viewForm.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="statusOptions" :value="viewForm.status"/>
        </el-descriptions-item>
        <el-descriptions-item label="收货人姓名">{{ viewForm.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货人电话">{{ viewForm.receiverPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货人地址" :span="2">{{ viewForm.receiverAddress || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 20px;">
        <h4 style="margin-bottom: 10px;">商品明细</h4>
        <el-table :data="viewForm.details" border>
          <el-table-column label="商品名称" prop="productName" />
          <el-table-column label="单位" prop="unit" width="80" align="center" />
          <el-table-column label="数量" prop="quantity" width="80" align="center" />
          <el-table-column label="单价" width="100" align="center">
            <template #default="scope">
              <span>{{ scope.row.isGift === '1' ? '-' : '¥' + scope.row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100" align="center">
            <template #default="scope">
              <span :style="{ color: scope.row.isGift === '1' ? '#909399' : '#409EFF' }">{{ scope.row.isGift === '1' ? '赠品' : '¥' + scope.row.amount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="是否赠品" width="100" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.isGift === '1'" type="warning">是</el-tag>
              <span v-else>否</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

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
import { listPurchase, getPurchase, delPurchase, addPurchase, updatePurchase } from '@/api/finance/purchase'
import { listSupplier } from '@/api/finance/supplier'
import { listProduct } from '@/api/finance/product'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: "Purchase",
  setup() {
    const dict = useDict('finance_payment_method')
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
      // 进货单表格数据
      purchaseList: [],
      // 供应商选项
      supplierOptions: [],
      // 商品选项
      productOptions: [],
      // 状态选项
      statusOptions: [
        { value: '0', label: '草稿' },
        { value: '1', label: '已确认' },
        { value: '2', label: '已完成' }
      ],
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
        purchaseNo: undefined,
        supplierName: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 查看表单
      viewForm: {},
      // 表单校验
      rules: {
        supplierId: [
          { required: true, message: "供应商不能为空", trigger: "change" }
        ],
        purchaseDate: [
          { required: true, message: "进货日期不能为空", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.getList()
    this.getSupplierList()
    this.getProductList()
  },
  methods: {
    /** 查询进货单列表 */
    getList() {
      this.loading = true
      listPurchase(this.queryParams).then(response => {
        this.purchaseList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 查询供应商列表 */
    getSupplierList() {
      listSupplier({ pageNum: 1, pageSize: 1000, deptId: userStore.currentDeptId }).then(response => {
        this.supplierOptions = response.rows
      })
    },
    /** 查询商品列表 */
    getProductList() {
      listProduct({ pageNum: 1, pageSize: 1000, deptId: userStore.currentDeptId }).then(response => {
        this.productOptions = response.rows
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
        purchaseId: undefined,
        purchaseNo: undefined,
        supplierId: undefined,
        supplierName: undefined,
        purchaseDate: new Date().toISOString().split('T')[0],
        totalAmount: 0,
        paidAmount: 0,
        paymentMethod: undefined,
        totalQuantity: 0,
        status: "0",
        remark: undefined,
        details: []
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
      this.ids = selection.map(item => item.purchaseId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加进货单"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const purchaseId = row.purchaseId || this.ids
      getPurchase(purchaseId).then(response => {
        this.form = response.data
        if (!this.form.details) {
          this.form.details = []
        }
        this.open = true
        this.title = "修改进货单"
      })
    },
    /** 查看详情 */
    handleView(row) {
      getPurchase(row.purchaseId).then(response => {
        this.viewForm = response.data
        if (!this.viewForm.details) {
          this.viewForm.details = []
        }
        this.viewOpen = true
      })
    },
    /** 供应商变化 */
    handleSupplierChange(supplierId) {
      const supplier = this.supplierOptions.find(item => item.supplierId === supplierId)
      if (supplier) {
        this.form.supplierName = supplier.supplierName
      }
    },
    /** 商品变化 */
    handleProductChange(productId, index) {
      const product = this.productOptions.find(item => item.productId === productId)
      if (product) {
        this.form.details[index].productName = product.productName
        this.form.details[index].unit = product.unit
        this.form.details[index].price = product.purchasePrice || 0
        this.calculateAmount(index)
      }
    },
    /** 添加商品明细 */
    handleAddDetail() {
      if (!this.form.details) {
        this.form.details = []
      }
      this.form.details.push({
        detailId: undefined,
        purchaseId: undefined,
        productId: undefined,
        productName: undefined,
        unit: undefined,
        quantity: 1,
        price: 0,
        amount: 0,
        isGift: '0'
      })
    },
    /** 删除商品明细 */
    handleDeleteDetail(index) {
      this.form.details.splice(index, 1)
      this.calculateTotal()
    },
    /** 计算单条金额 */
    calculateAmount(index) {
      const item = this.form.details[index]
      if (item.isGift === '1') {
        item.price = 0
        item.amount = 0
      } else {
        item.amount = (item.price || 0) * (item.quantity || 0)
      }
      this.calculateTotal()
    },
    /** 计算总金额和总数量 */
    calculateTotal() {
      let totalAmount = 0
      let totalQuantity = 0
      if (this.form.details && this.form.details.length > 0) {
        this.form.details.forEach(item => {
          if (item.isGift !== '1') {
            totalAmount += Number(item.amount || 0)
          }
          totalQuantity += Number(item.quantity || 0)
        })
      }
      this.form.totalAmount = parseFloat(totalAmount.toFixed(2))
      this.form.totalQuantity = totalQuantity
    },
    /** 提交按钮 */
    submitForm() {
      // 校验商品明细
      if (!this.form.details || this.form.details.length === 0) {
        this.$modal.msgWarning('请至少添加一个商品')
        return
      }
      for (let i = 0; i < this.form.details.length; i++) {
        const item = this.form.details[i]
        if (!item.productId) {
          this.$modal.msgWarning(`请选择第${i + 1}个商品`)
          return
        }
        if (!item.quantity || item.quantity === 0) {
          this.$modal.msgWarning(`第${i + 1}个商品数量不能为0`)
          return
        }
      }

      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.purchaseId != undefined) {
            updatePurchase(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPurchase(this.form).then(() => {
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
      const purchaseIds = row.purchaseId || this.ids
      ElMessageBox.confirm('是否确认删除进货单编号为"' + purchaseIds + '"的数据项？').then(function() {
        return delPurchase(purchaseIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/purchase/export', {
        ...this.queryParams
      }, `purchase_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style lang="scss" scoped>
.purchase-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
  }
}

.detail-section {
  width: 100%;
}
</style>
