<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="销售记录" name="sale">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="销售单号" prop="saleNo">
        <el-input v-model="queryParams.saleNo" placeholder="请输入销售单号" clearable style="width: 180px;" />
      </el-form-item>
      <el-form-item label="商品名称" prop="productName">
        <el-input v-model="queryParams.productName" placeholder="请输入商品名称" clearable style="width: 180px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['finance:sale:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['finance:sale:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="saleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="销售单号" align="center" prop="saleNo" width="180" />
      <el-table-column label="商品名称" align="center" prop="productName" />
      <el-table-column label="销售日期" align="center" prop="saleDate" width="120" />
      <el-table-column label="销售金额" align="center" prop="saleAmount">
        <template #default="scope">
          <span style="color: #F56C6C; font-weight: bold;">{{ money(scope.row.saleAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已缴金额" align="center" prop="paidAmount">
        <template #default="scope">
          <span style="color: #67C23A; font-weight: bold;">{{ money(scope.row.paidAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="欠缴金额" align="center">
        <template #default="scope">
          <span style="color: #E6A23C; font-weight: bold;">¥{{ ((scope.row.saleAmount || 0) - (scope.row.paidAmount || 0)).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="销售数量" align="center" prop="saleQuantity" />
      <el-table-column label="赠品数量" align="center" prop="giftQuantity" />
      <el-table-column label="加赠单价" align="center">
        <template #default="scope">{{ giftUnitPrice(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleView(scope.row)">查看</el-button>
          <el-button v-if="!isCarried(scope.row)" size="small" type="text" @click="handleUpdate(scope.row)" v-hasPermi="['finance:sale:edit']">修改</el-button>
          <el-button v-if="needsPaymentAdjustment(scope.row)" size="small" type="text" @click="handlePayment(scope.row)" v-hasPermi="['finance:sale:edit']">缴款</el-button>
          <el-button v-if="!isCarried(scope.row)" size="small" type="text" @click="handleDelete(scope.row)" v-hasPermi="['finance:sale:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <el-tab-pane label="历史欠款" name="receivable">
        <el-form :model="receivableQuery" ref="receivableForm" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="销售单号" prop="saleNo">
            <el-input v-model="receivableQuery.saleNo" placeholder="请输入销售单号" clearable style="width: 180px;" />
          </el-form-item>
          <el-form-item label="商品名称" prop="productName">
            <el-input v-model="receivableQuery.productName" placeholder="请输入商品名称" clearable style="width: 180px;" />
          </el-form-item>
          <el-form-item label="缴款状态" prop="status">
            <el-select v-model="receivableQuery.status" placeholder="全部" clearable style="width: 140px;">
              <el-option label="待缴款" value="0" />
              <el-option label="部分缴款" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="handleReceivableQuery">搜索</el-button>
            <el-button size="small" @click="resetReceivableQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px;"
          title="历史欠款仅显示未缴清销售单，按账龄降序排列。对历史销售单缴款只新增缴款记录，销售业务仍归属原销售周期；本次缴款计入当前进行中核算周期。" />

        <el-table v-loading="receivableLoading" :data="receivableList" border>
          <el-table-column label="销售单号" align="center" prop="saleNo" width="170" />
          <el-table-column label="商品名称" align="center" prop="productName" show-overflow-tooltip />
          <el-table-column label="会员" align="center" prop="memberName" show-overflow-tooltip width="120" />
          <el-table-column label="销售周期" align="center" prop="periodNo" width="140" show-overflow-tooltip />
          <el-table-column label="销售日期" align="center" prop="saleDate" width="110">
            <template #default="scope">{{ parseTime(scope.row.saleDate, '{y}-{m}-{d}') }}</template>
          </el-table-column>
          <el-table-column label="账龄(天)" align="center" width="90">
            <template #default="scope">
              <span :style="{ color: getAgeColor(scope.row) }">{{ computeAgeDays(scope.row.saleDate) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="销售金额" align="right" width="110">
            <template #default="scope"><span style="color: #F56C6C; font-weight: bold;">{{ money(scope.row.saleAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="已缴金额" align="right" width="110">
            <template #default="scope"><span style="color: #67C23A; font-weight: bold;">{{ money(scope.row.paidAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="剩余应收" align="right" width="110">
            <template #default="scope"><span style="color: #E6A23C; font-weight: bold;">¥{{ ((scope.row.saleAmount || 0) - (scope.row.paidAmount || 0)).toFixed(2) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="90">
            <template #default="scope"><el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="最近缴款时间" align="center" width="160">
            <template #default="scope">{{ scope.row.latestPaymentDate ? parseTime(scope.row.latestPaymentDate, '{y}-{m}-{d} {h}:{i}:{s}') : '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="140" fixed="right">
            <template #default="scope">
              <el-button size="small" type="text" @click="handleView(scope.row)">查看</el-button>
              <el-button size="small" type="text" @click="handlePayment(scope.row)" v-hasPermi="['finance:sale:edit']">缴款</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="receivableTotal>0" :total="receivableTotal" v-model:page="receivableQuery.pageNum" v-model:limit="receivableQuery.pageSize" @pagination="getReceivableList" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商品" prop="productId">
          <el-select v-model="form.productId" placeholder="请选择商品" style="width: 100%;" @change="handleProductChange">
            <el-option v-for="item in productOptions" :key="item.productId" :label="item.productName" :value="item.productId" />
          </el-select>
        </el-form-item>
        <el-form-item label="销售日期" prop="saleDate">
          <el-date-picker v-model="form.saleDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="销售金额" prop="saleAmount">
          <el-input-number v-model="form.saleAmount" :precision="2" :step="0.1" style="width: 100%;" @change="calculateUnitPrice" />
        </el-form-item>
        <el-form-item label="销售数量" prop="saleQuantity">
          <el-input-number v-model="form.saleQuantity" :min="1" :step="1" style="width: 100%;" @change="calculateUnitPrice" />
        </el-form-item>
        <el-form-item label="赠品数量" prop="giftQuantity">
          <el-input-number v-model="form.giftQuantity" :min="0" :step="1" style="width: 100%;" />
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

    <el-dialog title="缴款" v-model="paymentOpen" width="600px" append-to-body>
      <el-form ref="paymentForm" :model="paymentForm" :rules="paymentRules" label-width="100px">
        <el-form-item label="销售单号">
          <el-input v-model="paymentForm.saleNo" disabled />
        </el-form-item>
        <el-form-item label="销售周期" v-if="paymentForm.periodNo">
          <el-input v-model="paymentForm.periodNo" disabled />
        </el-form-item>
        <el-form-item label="销售金额">
          <el-input :model-value="money(paymentForm.saleAmount)" disabled />
        </el-form-item>
        <el-form-item label="累计已缴">
          <el-input :model-value="money(paymentForm.paidAmount)" disabled />
        </el-form-item>
        <el-form-item label="剩余应收">
          <el-input :model-value="'¥' + ((paymentForm.saleAmount || 0) - (paymentForm.paidAmount || 0)).toFixed(2)" disabled />
        </el-form-item>
        <el-form-item label="缴款日期" prop="paymentDate">
          <el-date-picker v-model="paymentForm.paymentDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="缴款金额" prop="paymentAmount">
          <el-input-number v-model="paymentForm.paymentAmount" :precision="2" :step="0.1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="付款方式" prop="paymentMethod">
          <el-select v-model="paymentForm.paymentMethod" placeholder="请选择付款方式" style="width: 100%;">
            <el-option v-for="dict in dict.type.finance_payment_method" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="paymentForm.remark" type="textarea" placeholder="请输入备注" :rows="3" />
        </el-form-item>
        <el-alert v-if="!paymentForm.isEdit" type="warning" :closable="false" show-icon
          title="本次缴款计入当前进行中核算周期，不影响销售单原周期归属。" />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitPaymentForm">确 定</el-button>
        <el-button @click="paymentOpen = false">取 消</el-button>
      </div>
      </template>
    </el-dialog>

    <el-dialog title="详情" v-model="viewOpen" width="800px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="销售单号">{{ viewForm.saleNo }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ viewForm.productName }}</el-descriptions-item>
        <el-descriptions-item label="销售日期">{{ parseTime(viewForm.saleDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="销售金额">
          <span style="color: #F56C6C; font-weight: bold;">{{ money(viewForm.saleAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="已缴金额">
          <span style="color: #67C23A; font-weight: bold;">{{ money(viewForm.paidAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="待缴金额">
          <span style="color: #E6A23C; font-weight: bold;">{{ money(viewForm.saleAmount - viewForm.paidAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="销售数量">{{ viewForm.saleQuantity }}</el-descriptions-item>
        <el-descriptions-item label="赠品数量">{{ viewForm.giftQuantity }}</el-descriptions-item>
        <el-descriptions-item label="加赠单价">{{ giftUnitPrice(viewForm) }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ viewForm.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="单价">{{ money(viewForm.unitPrice) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(viewForm.status)">{{ getStatusText(viewForm.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          <span style="display: block; min-width: 0; white-space: pre-wrap; overflow-wrap: anywhere; word-break: break-word;">{{ viewForm.remark || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="viewForm.payments && viewForm.payments.length > 0" style="margin-top: 20px;">
        <h4 style="margin-bottom: 10px;">缴款记录</h4>
        <el-table :data="viewForm.payments" border>
          <el-table-column label="缴款单号" prop="paymentNo" width="180" />
          <el-table-column label="缴款时间" prop="createTime" width="160" align="center">
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="缴款金额" width="120" align="center">
            <template #default="scope">
              <span style="color: #67C23A; font-weight: bold;">{{ money(scope.row.paymentAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="付款方式" prop="paymentMethod" align="center">
            <template #default="scope">
              <dict-tag :options="dict.type.finance_payment_method" :value="scope.row.paymentMethod" />
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" />
          <el-table-column label="操作" width="140" align="center">
            <template #default="scope">
              <el-button size="small" type="text" @click="handleEditPayment(scope.row)" v-hasPermi="['finance:sale:payment']">修改</el-button>
              <el-button size="small" type="text" @click="handleDeletePayment(scope.row)" v-hasPermi="['finance:sale:payment']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

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
import { listSale, listReceivable, getSale, delSale, addSale, updateSale, addPayment, updatePayment, delPayment } from '@/api/finance/sale'
import { listProduct, listProductSelector } from '@/api/finance/product'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: "Sale",
  setup() {
    const dict = useDict('finance_payment_method')
    return { dict, userStore }
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      saleList: [],
      productOptions: [],
      activeTab: "sale",
      receivableLoading: false,
      receivableList: [],
      receivableTotal: 0,
      receivableQuery: {
        pageNum: 1,
        pageSize: 10,
        saleNo: undefined,
        productName: undefined,
        status: undefined
      },
      title: "",
      open: false,
      paymentOpen: false,
      viewOpen: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        saleNo: undefined,
        productName: undefined
      },
      form: {},
      paymentForm: {},
      viewForm: {},
      rules: {
        productId: [{ required: true, message: "商品不能为空", trigger: "change" }],
        saleDate: [{ required: true, message: "销售日期不能为空", trigger: "change" }],
        saleAmount: [{ required: true, message: "销售金额不能为空", trigger: "blur" }],
        saleQuantity: [{ required: true, message: "销售数量不能为空", trigger: "blur" }]
      },
      paymentRules: {
        paymentAmount: [{ required: true, message: "缴款金额不能为空", trigger: "blur" }],
        paymentMethod: [{ required: true, message: "付款方式不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
    this.getProductList()
  },
  methods: {
    getList() {
      this.loading = true
      listSale({ ...this.queryParams, deptId: userStore.currentDeptId }).then(response => {
        this.saleList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getReceivableList() {
      this.receivableLoading = true
      listReceivable({ ...this.receivableQuery, deptId: userStore.currentDeptId }).then(response => {
        this.receivableList = response.rows
        this.receivableTotal = response.total
        this.receivableLoading = false
      }).catch(() => {
        this.receivableLoading = false
      })
    },
    handleTabChange(name) {
      if (name === 'receivable') {
        this.getReceivableList()
      } else {
        this.getList()
      }
    },
    handleReceivableQuery() {
      this.receivableQuery.pageNum = 1
      this.getReceivableList()
    },
    resetReceivableQuery() {
      this.resetForm("receivableForm")
      this.receivableQuery.status = undefined
      this.handleReceivableQuery()
    },
    computeAgeDays(saleDate) {
      if (!saleDate) return 0
      const diff = Date.now() - new Date(saleDate).getTime()
      return Math.floor(diff / (1000 * 60 * 60 * 24))
    },
    getAgeColor(row) {
      const days = this.computeAgeDays(row.saleDate)
      if (days > 60) return '#F56C6C'
      if (days > 30) return '#E6A23C'
      return '#67C23A'
    },
    isCarried(row) {
      return row && row.periodStatus === '2'
    },
    needsPaymentAdjustment(row) {
      if (!row) return false
      return Math.round((Number(row.saleAmount || 0) - Number(row.paidAmount || 0)) * 100) !== 0
    },
    getProductList() {
      listProductSelector().then(response => {
        this.productOptions = response.data || []
      })
    },
    getStatusText(status) {
      const statusMap = { '0': '待缴款', '1': '部分缴款', '2': '已缴清' }
      return statusMap[status] || status
    },
    getStatusType(status) {
      const typeMap = { '0': 'danger', '1': 'warning', '2': 'success' }
      return typeMap[status] || 'info'
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        saleId: undefined,
        saleNo: undefined,
        productId: undefined,
        productName: undefined,
        saleDate: new Date().toISOString().split('T')[0],
        saleAmount: 0,
        saleQuantity: 1,
        giftQuantity: 0,
        totalQuantity: 1,
        unitPrice: 0,
        paidAmount: 0,
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
      this.ids = selection.map(item => item.saleId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加销售记录"
    },
    handleUpdate(row) {
      this.reset()
      const saleId = row.saleId || this.ids
      getSale(saleId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改销售记录"
      })
    },
    handleView(row) {
      getSale(row.saleId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    formatToday() {
      const d = new Date()
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },
    handlePayment(row) {
      this.paymentForm = {
        saleId: row.saleId,
        saleNo: row.saleNo,
        periodNo: row.periodNo,
        saleAmount: row.saleAmount,
        paidAmount: row.paidAmount,
        paymentDate: this.formatToday(),
        paymentAmount: row.saleAmount - row.paidAmount,
        paymentMethod: undefined,
        remark: undefined
      }
      this.paymentOpen = true
    },
    handleProductChange(productId) {
      const product = this.productOptions.find(item => item.productId === productId)
      if (product) {
        this.form.productName = product.productName
      }
    },
    calculateUnitPrice() {
      this.form.totalQuantity = (this.form.saleQuantity || 0) + (this.form.giftQuantity || 0)
      if (this.form.saleQuantity && this.form.saleQuantity > 0 && this.form.saleAmount) {
        this.form.unitPrice = (this.form.saleAmount / this.form.saleQuantity).toFixed(2)
      } else {
        this.form.unitPrice = 0
      }
    },
    giftUnitPrice(row) {
        const amount = Number(row?.saleAmount || 0)
        const totalQuantity = Number(row?.saleQuantity || 0) + Number(row?.giftQuantity || 0)
        // 退货单 saleQuantity 为负，totalQuantity 可能为负；只要非零就应计算单价
        return totalQuantity !== 0 ? `¥${(amount / totalQuantity).toFixed(2)}` : '-'
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.saleId != undefined) {
            updateSale(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            this.form.deptId = userStore.currentDeptId
            addSale(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    submitPaymentForm() {
      this.$refs["paymentForm"].validate(valid => {
        if (valid) {
          const data = {
            paymentAmount: this.paymentForm.paymentAmount,
            paymentMethod: this.paymentForm.paymentMethod,
            remark: this.paymentForm.remark,
            paymentDate: this.paymentForm.paymentDate
          }
          if (this.paymentForm.isEdit) {
            updatePayment(this.paymentForm.paymentId, data).then(() => {
              ElMessage.success("修改成功")
              this.paymentOpen = false
              this.handleView({ saleId: this.viewForm.saleId })
              if (this.activeTab === 'receivable') {
                this.getReceivableList()
              } else {
                this.getList()
              }
            })
          } else {
            addPayment(this.paymentForm.saleId, data).then(() => {
              ElMessage.success("缴款成功")
              this.paymentOpen = false
              if (this.activeTab === 'receivable') {
                this.getReceivableList()
              } else {
                this.getList()
              }
            })
          }
        }
      })
    },
    handleDelete(row) {
      const saleIds = row.saleId || this.ids
      ElMessageBox.confirm('是否确认删除销售记录编号为"' + saleIds + '"的数据项？').then(() => {
        return delSale(saleIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleEditPayment(row) {
      this.paymentForm = {
        paymentId: row.paymentId,
        saleId: this.viewForm.saleId,
        saleNo: this.viewForm.saleNo,
        saleAmount: this.viewForm.saleAmount,
        paidAmount: this.viewForm.paidAmount,
        paymentDate: row.paymentDate ? String(row.paymentDate).substring(0, 10) : this.formatToday(),
        paymentAmount: row.paymentAmount,
        paymentMethod: row.paymentMethod,
        remark: row.remark,
        isEdit: true
      }
      this.paymentOpen = true
    },
    handleDeletePayment(row) {
      ElMessageBox.confirm('是否确认删除该缴款记录？').then(() => {
        return delPayment(row.paymentId)
      }).then(() => {
        ElMessage.success("删除成功")
        this.handleView({ saleId: this.viewForm.saleId })
      }).catch(() => {})
    }
  }
}
</script>
