<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="会员编号" prop="memberNo">
        <el-input
          v-model="queryParams.memberNo"
          placeholder="请输入会员编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="兑换日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['member:pointsExchange:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['member:pointsExchange:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['member:pointsExchange:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:pointsExchange:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="exchangeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="会员编号" align="center" prop="memberNo" width="120" />
      <el-table-column label="会员姓名" align="center" prop="memberName" width="100" />
      <el-table-column label="兑换物品" align="center" prop="goodsName" width="180" />
      <el-table-column label="兑换数量" align="center" prop="quantity" width="100" />
      <el-table-column label="扣减积分" align="center" prop="actualPoints" width="120">
        <template #default="scope">
          <span style="color: #F56C6C;">-{{ scope.row.actualPoints || scope.row.pointsDeducted }}积分</span>
        </template>
      </el-table-column>
      <el-table-column label="付款方式" align="center" prop="paymentMethod" width="120">
        <template #default="scope">
          <dict-tag :options="paymentMethodDictOptions" :value="scope.row.paymentMethod" />
        </template>
      </el-table-column>
      <el-table-column label="补差价金额" align="center" prop="extraAmount" width="120">
        <template #default="scope">
          <span v-if="scope.row.extraAmount > 0" style="color: #E6A23C;">¥{{ scope.row.extraAmount }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="兑换日期" align="center" prop="exchangeDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.exchangeDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button
            size="small"
            link type="primary"
            icon="View"
            @click="handleView(scope.row)"
          >查看</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['member:pointsExchange:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:pointsExchange:remove']"
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

    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会员编号" prop="memberCode">
              <el-input v-model="form.memberCode" placeholder="请输入会员编号" @blur="handleMemberSearch" @keyup.enter="handleMemberSearch">
                <template #append><el-button icon="Search" @click="handleMemberSearch"></el-button></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会员姓名">
              <el-input v-model="form.memberName" placeholder="自动获取" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="兑换物品" prop="goodsId">
              <el-select v-model="form.goodsId" placeholder="请选择兑换物品" style="width: 100%;" @change="handleGoodsChange">
                <el-option
                  v-for="item in goodsOptions"
                  :key="item.goodsId"
                  :label="item.goodsName + ' (' + item.pointsPrice + '积分)'"
                  :value="item.goodsId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="兑换数量" prop="exchangeNum">
              <el-input-number v-model="form.exchangeNum" :min="1" style="width: 100%;" @change="calculatePoints" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物品积分价格">
              <el-input v-model="form.goodsPointsPrice" disabled>
                <template #prepend>单价</template>
                <template #append>积分</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="扣减积分">
              <el-input v-model="form.actualDeductPoints" disabled>
                <template #prepend>实际扣减</template>
                <template #append>积分</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="兑换日期" prop="exchangeDate">
              <el-date-picker
                v-model="form.exchangeDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式" prop="diffPaymentMethod">
              <el-select v-model="form.diffPaymentMethod" placeholder="请选择" style="width: 100%;">
                <el-option v-for="dict in dict.type.finance_payment_method" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="补差价金额" prop="diffAmount">
              <el-input-number v-model="form.diffAmount" :precision="2" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会员积分余额">
              <!-- 醒目显示会员当前总积分（纯数字） -->
              <div class="balance-highlight" :class="{ 'balance-warning': form.deductPoints > form.memberPoints, 'balance-ok': form.deductPoints <= form.memberPoints && form.memberPoints > 0 }">
                <span class="balance-value">{{ form.memberPoints || 0 }}</span>
              </div>
              <div v-if="form.deductPoints > form.memberPoints" style="margin-top: 6px;">
                <el-alert :title="'积分不足！需 ' + form.deductPoints + ' 积分，实际扣减 ' + form.actualDeductPoints + ' 积分，缺少 ' + (form.deductPoints - form.memberPoints) + ' 积分，请选择补差价方式'" type="error" :closable="false" show-icon />
              </div>
              <div v-else-if="form.deductPoints > 0 && form.memberPoints >= form.deductPoints" style="margin-top: 6px; font-size: 12px; color: #67C23A;">
                兑换后剩余：{{ form.memberPoints - form.deductPoints }} 积分
              </div>
            </el-form-item>
          </el-col>
        </el-row>
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

    <el-dialog title="积分兑换详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="会员编号">{{ viewForm.memberNo }}</el-descriptions-item>
        <el-descriptions-item label="会员姓名">{{ viewForm.memberName }}</el-descriptions-item>
        <el-descriptions-item label="兑换物品" :span="2">{{ viewForm.goodsName }}</el-descriptions-item>
        <el-descriptions-item label="兑换数量">{{ viewForm.quantity }}</el-descriptions-item>
        <el-descriptions-item label="物品积分价格">
          <span style="color: #409EFF;">{{ viewForm.pointsPrice }}积分</span>
        </el-descriptions-item>
        <el-descriptions-item label="扣减积分">
          <span style="color: #F56C6C;">-{{ viewForm.actualPoints || viewForm.pointsDeducted }}积分</span>
        </el-descriptions-item>
        <el-descriptions-item label="兑换日期">{{ parseTime(viewForm.exchangeDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ getPaymentMethodName(viewForm.paymentMethod) }}</el-descriptions-item>
        <el-descriptions-item label="补差价金额">
          <span v-if="viewForm.extraAmount > 0" style="color: #E6A23C;">¥{{ viewForm.extraAmount }}</span>
          <span v-else>-</span>
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
import { ElMessage, ElMessageBox } from "element-plus"
import { parseTime } from "@/utils/junsong"
import { useDownload } from "@/composables/useDownload"
import { useDict } from "@/composables/useDict"
import { listPointsExchange, getPointsExchange, delPointsExchange, addPointsExchange, updatePointsExchange, receiveExchange, listPointsGoods } from "@/api/member/pointsExchange"
import { getMemberByNo } from "@/api/member/member"
import { listPointsRecord } from "@/api/member/pointsRecord"
const { download } = useDownload()
const dict = useDict('finance_payment_method')

export default {
  name: "PointsExchange",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      exchangeList: [],
      goodsOptions: [],
      title: "",
      open: false,
      viewOpen: false,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        memberNo: undefined,
        beginExchangeDate: undefined,
        endExchangeDate: undefined
      },
      form: {},
      viewForm: {},
      rules: {
        memberCode: [
          { required: true, message: "会员编号不能为空", trigger: "blur" }
        ],
        goodsId: [
          { required: true, message: "兑换物品不能为空", trigger: "change" }
        ],
        exchangeNum: [
          { required: true, message: "兑换数量不能为空", trigger: "blur" }
        ],
        exchangeDate: [
          { required: true, message: "兑换日期不能为空", trigger: "change" }
        ]
      },
      dict,
      legacyPaymentMethodOptions: {
        '0': '无差价',
        '1': '现金',
        '2': '微信支付',
        '3': '支付宝',
        '4': '银行卡'
      }
    }
  },
  computed: {
    paymentMethodDictOptions() {
      const options = [...(this.dict.type.finance_payment_method || [])]
      const existingLabels = new Set(options.map((item) => item.label))
      Object.entries(this.legacyPaymentMethodOptions).forEach(([value, label]) => {
        if (!options.some((item) => String(item.value) === String(value)) && existingLabels.has(label)) {
          options.push({ label, value })
        }
      })
      return options
    }
  },
  created() {
    this.getList()
    this.getGoodsOptions()
  },
  methods: {
    parseTime,
    resetForm(formName) {
      this.$refs[formName]?.resetFields?.()
    },
    download,
    getList() {
      this.loading = true
      const params = {
        ...this.queryParams
      }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginExchangeDate = this.dateRange[0]
        params.endExchangeDate = this.dateRange[1]
      }
      listPointsExchange(params).then(response => {
        this.exchangeList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getGoodsOptions() {
      listPointsGoods({ pageNum: 1, pageSize: 1000, status: '0' }).then(response => {
        this.goodsOptions = response.rows
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        exchangeId: undefined,
        memberId: undefined,
        memberCode: undefined,
        memberName: undefined,
        memberPoints: 0,
        goodsId: undefined,
        goodsName: undefined,
        goodsPointsPrice: 0,
        exchangeNum: 1,
        deductPoints: 0,
        actualDeductPoints: 0,
        exchangeDate: undefined,
        diffPaymentMethod: '0',
        diffAmount: undefined,
        receiveStatus: '0',
        remark: undefined
      }
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.exchangeId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加积分兑换"
    },
    handleView(row) {
      getPointsExchange(row.exchangeId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    handleUpdate(row) {
      this.reset()
      const exchangeId = row.exchangeId || this.ids
      getPointsExchange(exchangeId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改积分兑换"
      })
    },
    handleGoodsChange(goodsId) {
      const goods = this.goodsOptions.find(item => item.goodsId === goodsId)
      if (goods) {
        this.form.goodsName = goods.goodsName
        this.form.goodsPointsPrice = goods.pointsPrice
        this.calculatePoints()
      }
    },
    handleMemberSearch() {
      if (this.form.memberCode) {
        getMemberByNo(this.form.memberCode).then(response => {
          if (response.data) {
            this.form.memberId = response.data.memberId
            this.form.memberName = response.data.memberName
            // 从积分记录表获取该会员最新余额（而非会员表的静态积分值）
            this.fetchMemberBalance(response.data.memberId)
          } else {
            ElMessage.warning("未找到该会员编号（仅限当前机构会员）")
            this.form.memberId = undefined
            this.form.memberName = ''
            this.form.memberPoints = 0
          }
        }).catch(() => {
          ElMessage.error("未找到该会员编号（仅限当前机构会员）")
          this.form.memberId = undefined
          this.form.memberName = ''
          this.form.memberPoints = 0
        })
      }
    },
    /** 获取会员当前总积分（从积分记录表取最新一条的balance） */
    fetchMemberBalance(memberId) {
      listPointsRecord({ memberId: memberId, pageNum: 1, pageSize: 1 }).then(res => {
        if (res.rows && res.rows.length > 0) {
          this.form.memberPoints = res.rows[0].balance || 0
        } else {
          this.form.memberPoints = 0
        }
        this.calculatePoints()
      }).catch(() => {
        this.form.memberPoints = 0
        this.calculatePoints()
      })
    },
    calculatePoints() {
      if (this.form.goodsPointsPrice && this.form.exchangeNum) {
        this.form.deductPoints = this.form.goodsPointsPrice * this.form.exchangeNum
        const memberPoints = this.form.memberPoints || 0
        this.form.actualDeductPoints = Math.min(this.form.deductPoints, memberPoints)
      }
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 超分校验：扣减积分不能超过会员总积分
          const deductPoints = this.form.deductPoints || 0
          const memberPoints = this.form.memberPoints || 0
          if (deductPoints > memberPoints) {
            const shortage = deductPoints - memberPoints
            if (!this.form.diffPaymentMethod || this.form.diffPaymentMethod === '0') {
              ElMessage.warning(`扣减积分(${deductPoints})超过会员总积分(${memberPoints})，缺少${shortage}积分，请选择付款方式`)
              return
            }
            // 补差金额必须大于0
            if (!this.form.diffAmount || this.form.diffAmount <= 0) {
              ElMessage.warning(`扣减积分超过总积分，缺少${shortage}积分，请填写补差价金额`)
              return
            }
          }
          // 映射前端字段名到后端实体字段名
          const postData = {
            exchangeId: this.form.exchangeId,
            memberId: this.form.memberId,
            memberNo: this.form.memberCode,
            memberName: this.form.memberName,
            goodsId: this.form.goodsId,
            goodsName: this.form.goodsName,
            exchangeDate: this.form.exchangeDate,
            quantity: this.form.exchangeNum,
            pointsDeducted: this.form.deductPoints,
            paymentMethod: this.form.diffPaymentMethod,
            extraAmount: this.form.diffAmount,
            status: this.form.receiveStatus || '0',
            remark: this.form.remark
          }
          if (this.form.exchangeId != undefined) {
            updatePointsExchange(postData).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPointsExchange(postData).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleReceive(row) {
      ElMessageBox.confirm('是否确认领取兑换记录编号为"' + row.exchangeId + '"的兑换？').then(() => {
        return receiveExchange(row.exchangeId)
      }).then(() => {
        this.getList()
        ElMessage.success("领取成功")
      }).catch(() => {})
    },
    handleDelete(row) {
      const exchangeIds = row.exchangeId || this.ids
      ElMessageBox.confirm('是否确认删除兑换记录编号为"' + exchangeIds + '"的数据项？').then(function() {
        return delPointsExchange(exchangeIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('member/pointsExchange/export', {
        ...this.queryParams
      }, `pointsExchange_${new Date().getTime()}.xlsx`)
    },
    getPaymentMethodName(method) {
      const hit = this.paymentMethodDictOptions.find((item) => String(item.value) === String(method))
      return hit?.label || method || '-'
    }
  }
}
</script>

<style scoped>
/* ========== 会员积分余额醒目显示（纯数字） ========== */
.balance-highlight {
  text-align: center;
  padding: 20px 12px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}
.balance-highlight.balance-ok {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  box-shadow: 0 4px 15px rgba(56, 239, 125, 0.35);
}
.balance-highlight.balance-warning {
  background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%);
  box-shadow: 0 4px 15px rgba(235, 51, 73, 0.4);
  animation: balancePulse 1.5s infinite;
}
@keyframes balancePulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.03); }
  100% { transform: scale(1); }
}
.balance-value {
  font-size: 42px;
  font-weight: 800;
  color: #fff;
  line-height: 1.2;
  text-shadow: 0 2px 8px rgba(0,0,0,0.25);
}
</style>
