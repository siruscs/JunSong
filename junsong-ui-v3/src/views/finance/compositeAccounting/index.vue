<template>
  <div class="app-container">
    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="池名称" prop="poolName">
        <el-input v-model="queryParams.poolName" placeholder="请输入池名称" clearable style="width: 200px;" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="池编号" prop="poolNo">
        <el-input v-model="queryParams.poolNo" placeholder="请输入池编号" clearable style="width: 180px;" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 140px;">
          <el-option label="进行中" value="0" />
          <el-option label="已达回本" value="1" />
          <el-option label="已确认回本" value="2" />
          <el-option label="已关闭" value="3" />
          <el-option label="草稿" value="4" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['finance:compositeAccounting:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['finance:compositeAccounting:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['finance:compositeAccounting:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="poolList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="池名称" align="center" prop="poolName" min-width="160" show-overflow-tooltip />
      <el-table-column label="池编号" align="center" prop="poolNo" width="180" show-overflow-tooltip />
      <el-table-column label="总出资" align="center" prop="totalInvestAmount" width="120">
        <template #default="scope">{{ formatMoney(scope.row.totalInvestAmount) }}</template>
      </el-table-column>
      <el-table-column label="累计回本" align="center" prop="totalReturnAmount" width="120">
        <template #default="scope">{{ formatMoney(scope.row.totalReturnAmount) }}</template>
      </el-table-column>
      <el-table-column label="回本缺口" align="center" prop="breakEvenGap" width="120">
        <template #default="scope">{{ formatMoney(scope.row.breakEvenGap) }}</template>
      </el-table-column>
      <el-table-column label="超额收益" align="center" prop="overReturnAmount" width="120">
        <template #default="scope">{{ formatMoney(scope.row.overReturnAmount) }}</template>
      </el-table-column>
      <el-table-column label="回本进度" align="center" width="160">
        <template #default="scope">
          <el-progress :percentage="calcProgress(scope.row)" :status="calcProgress(scope.row) >= 100 ? 'success' : ''" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleDetail(scope.row)" v-hasPermi="['finance:compositeAccounting:query']">详情</el-button>
          <el-button size="small" type="text" @click="handleUpdate(scope.row)" v-hasPermi="['finance:compositeAccounting:edit']">修改</el-button>
          <el-button size="small" type="text" @click="handleDelete(scope.row)" v-hasPermi="['finance:compositeAccounting:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改 弹窗 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="池名称" prop="poolName">
          <el-input v-model="form.poolName" placeholder="请输入复合核算池名称" />
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailOpen" :title="'复合核算池详情 - ' + (overview.pool ? overview.pool.poolName : '')" size="70%">
      <div v-loading="detailLoading" style="padding: 0 16px 16px;">
        <!-- 基础信息卡片 -->
        <el-card class="mb8" shadow="never" v-if="overview.pool">
          <template #header><span>基础信息</span></template>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="池名称">{{ overview.pool.poolName }}</el-descriptions-item>
            <el-descriptions-item label="池编号">{{ overview.pool.poolNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(overview.pool.status)">{{ statusLabel(overview.pool.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="共享投资人总出资">{{ formatMoney(overview.pool.totalInvestAmount) }}</el-descriptions-item>
            <el-descriptions-item label="累计回本金额">{{ formatMoney(overview.pool.totalReturnAmount) }}</el-descriptions-item>
            <el-descriptions-item label="回本缺口">{{ formatMoney(overview.pool.breakEvenGap) }}</el-descriptions-item>
            <el-descriptions-item label="超额收益">{{ formatMoney(overview.pool.overReturnAmount) }}</el-descriptions-item>
            <el-descriptions-item label="达到回本时间">{{ formatTime(overview.pool.breakEvenTime) }}</el-descriptions-item>
            <el-descriptions-item label="确认回本时间">{{ formatTime(overview.pool.confirmedTime) }}</el-descriptions-item>
            <el-descriptions-item label="回本进度" :span="3">
              <el-progress :percentage="calcProgress(overview.pool)" :status="calcProgress(overview.pool) >= 100 ? 'success' : ''" />
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="3">{{ overview.pool.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 12px; text-align: right;">
            <el-button size="small" @click="handleBindDepts" v-if="overview.pool.status !== '3'" v-hasPermi="['finance:compositeAccounting:edit']">维护店面</el-button>
            <el-button size="small" @click="handleBindInvestors" v-if="overview.pool.status !== '3'" v-hasPermi="['finance:compositeAccounting:edit']">维护投资人</el-button>
            <el-button size="small" @click="handleRecalculate" v-hasPermi="['finance:compositeAccounting:edit']">重新计算</el-button>
            <el-button size="small" type="warning" @click="handleConfirmBreakEven" v-if="overview.pool.status === '1'" v-hasPermi="['finance:compositeAccounting:confirm']">确认回本</el-button>
            <el-button size="small" type="danger" @click="handleClose" v-if="overview.pool.status !== '3'" v-hasPermi="['finance:compositeAccounting:close']">关闭池</el-button>
          </div>
        </el-card>

        <!-- 参与店面 -->
        <el-card class="mb8" shadow="never">
          <template #header><span>参与店面 ({{ (overview.depts || []).length }})</span></template>
          <el-table :data="overview.depts || []" size="small" border>
            <el-table-column label="店面ID" prop="deptId" width="100" align="center" />
            <el-table-column label="店面名称" min-width="160">
              <template #default="scope">{{ resolveDeptName(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="加入时间" align="center" width="170">
              <template #default="scope">{{ formatTime(scope.row.joinTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="90" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '有效' : '停用' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 共享投资人 -->
        <el-card class="mb8" shadow="never">
          <template #header><span>共享投资人 ({{ (overview.investors || []).length }})</span></template>
          <el-table :data="overview.investors || []" size="small" border>
            <el-table-column label="投资人" prop="investorName" min-width="120" />
            <el-table-column label="共享出资款" align="right" width="140">
              <template #default="scope">{{ formatMoney(scope.row.investAmount) }}</template>
            </el-table-column>
            <el-table-column label="出资占比" align="right" width="120">
              <template #default="scope">{{ formatRatio(scope.row.investRatio) }}</template>
            </el-table-column>
            <el-table-column label="已分摊回本" align="right" width="140">
              <template #default="scope">{{ formatMoney(scope.row.returnedAmount) }}</template>
            </el-table-column>
            <el-table-column label="回本进度" align="center" width="160">
              <template #default="scope">
                <el-progress :percentage="investorProgress(scope.row)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 复合核算汇总 -->
        <el-card class="mb8" shadow="never">
          <template #header><span>复合核算汇总</span></template>
          <div class="composite-summary-grid">
            <div class="summary-item">
              <div class="summary-label">汇总后的总费用</div>
              <div class="summary-value">{{ formatMoney((overview.summary || {}).totalVerifiedExpense) }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">总进货</div>
              <div class="summary-value">{{ formatMoney((overview.summary || {}).totalPurchase) }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">总销售</div>
              <div class="summary-value">{{ formatMoney((overview.summary || {}).totalSaleAmount) }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">未核销借支</div>
              <div class="summary-value">{{ formatMoney((overview.summary || {}).totalUnverifiedAdvance) }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">总缴款</div>
              <div class="summary-value">{{ formatMoney((overview.summary || {}).totalSalePayment) }}</div>
            </div>
            <div class="summary-item">
              <div class="summary-label">理论回本差额</div>
              <div class="summary-value">{{ formatMoney((overview.summary || {}).theoreticalBreakEvenGap) }}</div>
            </div>
            <div class="summary-item emphasis">
              <div class="summary-label">回本差额</div>
              <div class="summary-value">{{ formatMoney((overview.summary || {}).breakEvenGap) }}</div>
            </div>
          </div>
        </el-card>

        <!-- 已纳入周期 -->
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>已纳入周期明细 ({{ (overview.periodItems || []).length }})</span>
              <el-button size="small" type="primary" @click="handleInclude" v-if="overview.pool && (overview.pool.status === '1' || overview.pool.status === '2')" v-hasPermi="['finance:compositeAccounting:include']">纳入周期</el-button>
            </div>
          </template>
          <el-table :data="overview.periodItems || []" size="small" border>
            <el-table-column label="周期编号" prop="periodNo" min-width="160" show-overflow-tooltip />
            <el-table-column label="店面" min-width="140" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.deptName || ('店面' + scope.row.deptId) }}</template>
            </el-table-column>
            <el-table-column label="周期开始" align="center" width="170">
              <template #default="scope">{{ formatTime(scope.row.periodStartTime) }}</template>
            </el-table-column>
            <el-table-column label="周期结束" align="center" width="170">
              <template #default="scope">{{ formatTime(scope.row.periodEndTime) }}</template>
            </el-table-column>
            <el-table-column label="周期净利" align="right" width="120">
              <template #default="scope">{{ formatMoney(scope.row.netProfit) }}</template>
            </el-table-column>
            <el-table-column label="店长分润" align="right" width="120">
              <template #default="scope">{{ formatMoney(scope.row.managerProfitAmount) }}</template>
            </el-table-column>
            <el-table-column label="纳入金额" align="right" width="120">
              <template #default="scope">{{ formatMoney(scope.row.investorProfitAmount) }}</template>
            </el-table-column>
            <el-table-column label="纳入方式" prop="includedMode" width="90" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.includedMode === '0' ? 'info' : 'warning'">{{ scope.row.includedMode === '0' ? '自动' : '手动' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="纳入时间" align="center" width="170">
              <template #default="scope">{{ formatTime(scope.row.includedTime) }}</template>
            </el-table-column>
            <el-table-column label="操作人" prop="includedBy" width="110" align="center" />
          </el-table>
        </el-card>
      </div>
    </el-drawer>

    <!-- 维护店面弹窗 -->
    <el-dialog title="维护参与店面" v-model="bindDeptOpen" width="560px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="选择店面">
          <el-select v-model="bindDeptForm.deptIds" multiple filterable placeholder="请选择参与店面(至少2个)" style="width: 100%;">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitBindDepts">确 定</el-button>
          <el-button @click="bindDeptOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 维护投资人弹窗 -->
    <el-dialog title="维护共享投资人" v-model="bindInvestorOpen" width="720px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="投资人">
          <el-table :data="bindInvestorForm.investors" border size="small" style="width: 100%;">
            <el-table-column label="投资人" min-width="140">
              <template #default="scope">
                <el-select v-model="scope.row.investorId" filterable placeholder="请选择" @change="onInvestorChange(scope.row)">
                  <el-option v-for="inv in investorOptions" :key="inv.investorId" :label="inv.investorName" :value="inv.investorId" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="出资款" width="180">
              <template #default="scope">
                <el-input-number v-model="scope.row.investAmount" :min="0" :precision="2" :step="1000" style="width: 100%;" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="scope">
                <el-button size="small" type="text" @click="removeInvestorRow(scope.$index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button size="small" type="primary" plain style="margin-top: 8px;" @click="addInvestorRow">+ 添加投资人</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitBindInvestors">确 定</el-button>
          <el-button @click="bindInvestorOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 纳入周期弹窗 -->
    <el-dialog title="手动纳入周期" v-model="includeOpen" width="900px" append-to-body>
      <el-form :inline="true" label-width="80px">
        <el-form-item label="店面">
          <el-select v-model="includeForm.deptId" filterable placeholder="请选择店面" style="width: 220px;" @change="loadCandidatePeriods">
            <el-option v-for="dept in overview.depts || []" :key="dept.deptId" :label="dept.deptName + ' (' + dept.deptId + ')'" :value="dept.deptId" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="candidatePeriods" border size="small" @selection-change="handleCandidateSelectionChange" max-height="360">
        <el-table-column type="selection" width="50" align="center" :selectable="canSelectCandidate" />
        <el-table-column label="周期编号" prop="periodNo" min-width="160" show-overflow-tooltip />
        <el-table-column label="周期净利" align="right" width="120">
          <template #default="scope">{{ formatMoney(scope.row.netProfit) }}</template>
        </el-table-column>
        <el-table-column label="纳入金额" align="right" width="120">
          <template #default="scope">{{ formatMoney(scope.row.investorProfitAmount) }}</template>
        </el-table-column>
        <el-table-column label="结转时间" align="center" width="170">
          <template #default="scope">{{ scope.row.carryForwardTime }}</template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.includedByOther ? 'danger' : 'success'">{{ scope.row.includedByOther ? '已被纳入' : '可纳入' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 12px; text-align: right;">
        <el-button size="small" @click="handleTrial" :disabled="includeForm.selectedPeriodIds.length === 0">试算</el-button>
        <el-button size="small" type="primary" @click="handleConfirmInclude" :disabled="includeForm.selectedPeriodIds.length === 0" v-hasPermi="['finance:compositeAccounting:include']">确认纳入</el-button>
      </div>
    </el-dialog>

    <!-- 试算结果弹窗 -->
    <el-dialog title="试算结果(不落库)" v-model="trialOpen" width="720px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="本次纳入金额">{{ formatMoney(trialResult.currentIncludeAmount) }}</el-descriptions-item>
        <el-descriptions-item label="累计回本金额">{{ formatMoney(trialResult.totalReturnAmount) }}</el-descriptions-item>
        <el-descriptions-item label="共享投资人总出资">{{ formatMoney(trialResult.totalInvestAmount) }}</el-descriptions-item>
        <el-descriptions-item label="回本缺口">{{ formatMoney(trialResult.breakEvenGap) }}</el-descriptions-item>
        <el-descriptions-item label="超额收益">{{ formatMoney(trialResult.overReturnAmount) }}</el-descriptions-item>
        <el-descriptions-item label="是否达到回本">
          <el-tag :type="trialResult.breakEvenReached ? 'success' : 'info'">{{ trialResult.breakEvenReached ? '已达到' : '未达到' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="回本进度" :span="2">
          <el-progress :percentage="trialProgressPercent" />
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="trialResult.investorAllocations || []" border size="small" style="margin-top: 12px;">
        <el-table-column label="投资人" prop="investorName" min-width="120" />
        <el-table-column label="出资款" align="right" width="130">
          <template #default="scope">{{ formatMoney(scope.row.investAmount) }}</template>
        </el-table-column>
        <el-table-column label="出资占比" align="right" width="110">
          <template #default="scope">{{ formatRatio(scope.row.investRatio) }}</template>
        </el-table-column>
        <el-table-column label="本次分摊" align="right" width="130">
          <template #default="scope">{{ formatMoney(scope.row.currentAllocation) }}</template>
        </el-table-column>
        <el-table-column label="累计已回本" align="right" width="130">
          <template #default="scope">{{ formatMoney(scope.row.totalReturned) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="trialOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCompositeAccounting,
  getCompositeOverview,
  addCompositeAccounting,
  updateCompositeAccounting,
  delCompositeAccounting,
  bindDepts,
  bindInvestors,
  listCandidatePeriods,
  trialIncludePeriods,
  confirmIncludePeriods,
  recalculatePool,
  confirmBreakEven,
  closeCompositePool
} from '@/api/finance/compositeAccounting'
import { listInvestor } from '@/api/finance/investor'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: 'CompositeAccounting',
  data() {
    return {
      loading: false,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      poolList: [],
      deptOptions: [],
      investorOptions: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        poolName: undefined,
        poolNo: undefined,
        status: undefined
      },
      form: {},
      rules: {
        poolName: [{ required: true, message: '池名称不能为空', trigger: 'blur' }]
      },
      // 详情
      detailOpen: false,
      detailLoading: false,
      overview: {},
      // 维护店面
      bindDeptOpen: false,
      bindDeptForm: { poolId: undefined, deptIds: [] },
      // 维护投资人
      bindInvestorOpen: false,
      bindInvestorForm: { poolId: undefined, investors: [] },
      // 纳入周期
      includeOpen: false,
      candidatePeriods: [],
      includeForm: { poolId: undefined, deptId: undefined, selectedPeriodIds: [] },
      // 试算结果
      trialOpen: false,
      trialResult: {}
    }
  },
  computed: {
    trialProgressPercent() {
      if (!this.trialResult || !this.trialResult.totalInvestAmount) return 0
      const p = Number(this.trialResult.breakEvenProgress || 0) * 100
      return Math.min(100, Math.round(p))
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
    resolveDeptName(row) {
      if (row.deptName && !/^\d+$/.test(row.deptName)) {
        return row.deptName
      }
      const dept = this.deptOptions.find(d => d.deptId === row.deptId)
      return dept ? dept.deptName : (row.deptName || row.deptId)
    },
    getList() {
      this.loading = true
      listCompositeAccounting(this.queryParams).then(response => {
        this.poolList = response.rows || []
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
        poolId: undefined,
        poolName: undefined,
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
      this.ids = selection.map(item => item.poolId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增复合核算池'
    },
    handleUpdate(row) {
      this.reset()
      const poolId = row.poolId || this.ids[0]
      getCompositeOverview(poolId).then(response => {
        const pool = response.data && response.data.pool ? response.data.pool : {}
        this.form = {
          poolId: pool.poolId,
          poolName: pool.poolName,
          remark: pool.remark
        }
        this.open = true
        this.title = '修改复合核算池'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const request = this.form.poolId ? updateCompositeAccounting(this.form) : addCompositeAccounting(this.form)
        request.then(() => {
          ElMessage.success(this.form.poolId ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const poolIds = row.poolId || this.ids
      ElMessageBox.confirm('是否确认删除选中的复合核算池？').then(() => delCompositeAccounting(poolIds)).then(() => {
        this.getList()
        ElMessage.success('删除成功')
      }).catch(() => {})
    },
    // ===== 详情 =====
    handleDetail(row) {
      this.detailLoading = true
      this.detailOpen = true
      this.overview = {}
      getCompositeOverview(row.poolId).then(response => {
        this.overview = response.data || {}
        this.detailLoading = false
      }).catch(() => { this.detailLoading = false })
    },
    // ===== 维护店面 =====
    handleBindDepts() {
      this.bindDeptForm.poolId = this.overview.pool.poolId
      this.bindDeptForm.deptIds = (this.overview.depts || []).map(d => d.deptId)
      this.bindDeptOpen = true
    },
    submitBindDepts() {
      if (!this.bindDeptForm.deptIds || this.bindDeptForm.deptIds.length < 2) {
        ElMessage.warning('至少选择 2 个店面')
        return
      }
      bindDepts(this.bindDeptForm.poolId, this.bindDeptForm.deptIds).then(() => {
        ElMessage.success('维护店面成功')
        this.bindDeptOpen = false
        this.refreshOverview()
      })
    },
    // ===== 维护投资人 =====
    handleBindInvestors() {
      this.bindInvestorForm.poolId = this.overview.pool.poolId
      this.bindInvestorForm.investors = (this.overview.investors || []).map(inv => ({
        investorId: inv.investorId,
        investorName: inv.investorName,
        investAmount: Number(inv.investAmount) || 0
      }))
      if (this.bindInvestorForm.investors.length === 0) {
        this.bindInvestorForm.investors.push({ investorId: undefined, investorName: '', investAmount: 0 })
      }
      this.loadInvestorOptions()
      this.bindInvestorOpen = true
    },
    loadInvestorOptions() {
      // 只加载用户权限部门的投资人
      const userDeptIds = userStore.depts.map(d => d.deptId)
      listInvestor({ pageNum: 1, pageSize: 1000 }).then(response => {
        const allInvestors = response.rows || []
        // 按用户权限部门过滤
        this.investorOptions = allInvestors.filter(inv => userDeptIds.includes(inv.deptId))
      })
    },
    onInvestorChange(row) {
      const inv = this.investorOptions.find(i => i.investorId === row.investorId)
      if (inv) {
        row.investorName = inv.investorName
      }
    },
    addInvestorRow() {
      this.bindInvestorForm.investors.push({ investorId: undefined, investorName: '', investAmount: 0 })
    },
    removeInvestorRow(idx) {
      this.bindInvestorForm.investors.splice(idx, 1)
    },
    submitBindInvestors() {
      const list = this.bindInvestorForm.investors.filter(i => i.investorId)
      if (list.length === 0) {
        ElMessage.warning('至少选择 1 个投资人')
        return
      }
      bindInvestors(this.bindInvestorForm.poolId, list).then(() => {
        ElMessage.success('维护投资人成功')
        this.bindInvestorOpen = false
        this.refreshOverview()
      })
    },
    // ===== 纳入周期 =====
    handleInclude() {
      this.includeForm.poolId = this.overview.pool.poolId
      this.includeForm.deptId = undefined
      this.includeForm.selectedPeriodIds = []
      this.candidatePeriods = []
      this.includeOpen = true
    },
    loadCandidatePeriods() {
      if (!this.includeForm.deptId) {
        this.candidatePeriods = []
        return
      }
      listCandidatePeriods(this.includeForm.poolId, this.includeForm.deptId).then(response => {
        this.candidatePeriods = response.data || []
      })
    },
    canSelectCandidate(row) {
      return !row.includedByOther
    },
    handleCandidateSelectionChange(selection) {
      this.includeForm.selectedPeriodIds = selection.map(i => i.periodId)
    },
    handleTrial() {
      trialIncludePeriods(this.includeForm.poolId, this.includeForm.selectedPeriodIds).then(response => {
        this.trialResult = response.data || {}
        this.trialOpen = true
      })
    },
    handleConfirmInclude() {
      ElMessageBox.confirm('是否确认纳入选中的周期？此操作将落库并刷新回本金额。').then(() => {
        return confirmIncludePeriods(this.includeForm.poolId, this.includeForm.selectedPeriodIds)
      }).then(() => {
        ElMessage.success('纳入成功')
        this.includeOpen = false
        this.refreshOverview()
      }).catch(() => {})
    },
    // ===== 其他操作 =====
    handleRecalculate() {
      ElMessageBox.confirm('是否重新计算回本金额？').then(() => {
        return recalculatePool(this.overview.pool.poolId)
      }).then(() => {
        ElMessage.success('重新计算成功')
        this.refreshOverview()
      }).catch(() => {})
    },
    handleConfirmBreakEven() {
      ElMessageBox.confirm('确认整体回本后将锁定回本时点,是否继续？').then(() => {
        return confirmBreakEven(this.overview.pool.poolId)
      }).then(() => {
        ElMessage.success('确认回本成功')
        this.refreshOverview()
      }).catch(() => {})
    },
    handleClose() {
      ElMessageBox.confirm('关闭后复合核算池将不再纳入周期,是否继续？').then(() => {
        return closeCompositePool(this.overview.pool.poolId)
      }).then(() => {
        ElMessage.success('关闭成功')
        this.refreshOverview()
      }).catch(() => {})
    },
    refreshOverview() {
      if (!this.overview.pool) return
      this.detailLoading = true
      getCompositeOverview(this.overview.pool.poolId).then(response => {
        this.overview = response.data || {}
        this.detailLoading = false
      }).catch(() => { this.detailLoading = false })
    },
    // ===== 工具方法 =====
    formatMoney(val) {
      if (val === null || val === undefined || val === '') return '0.00'
      return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    formatRatio(val) {
      if (val === null || val === undefined) return '0%'
      return (Number(val) * 100).toFixed(2) + '%'
    },
    formatTime(val) {
      if (!val) return '-'
      const d = new Date(val)
      if (isNaN(d.getTime())) return val
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    statusLabel(status) {
      const map = { '0': '进行中', '1': '已达回本', '2': '已确认回本', '3': '已关闭', '4': '草稿' }
      return map[status] || status
    },
    statusTagType(status) {
      const map = { '0': 'primary', '1': 'warning', '2': 'success', '3': 'info', '4': 'info' }
      return map[status] || 'info'
    },
    calcProgress(row) {
      if (!row || !row.totalInvestAmount || Number(row.totalInvestAmount) === 0) return 0
      const p = (Number(row.totalReturnAmount) / Number(row.totalInvestAmount)) * 100
      return Math.min(100, Math.round(p))
    },
    investorProgress(row) {
      if (!row || !row.investAmount || Number(row.investAmount) === 0) return 0
      const p = (Number(row.returnedAmount) / Number(row.investAmount)) * 100
      return Math.min(100, Math.round(p))
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.composite-summary-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(128px, 1fr));
  gap: 8px;
}
.summary-item {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #fff;
  min-width: 0;
}
.summary-item.emphasis {
  border-color: #e6a23c;
  background: #fdf6ec;
}
.summary-label {
  color: #606266;
  font-size: 12px;
  line-height: 18px;
}
.summary-value {
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  line-height: 28px;
  word-break: break-word;
}
@media (max-width: 1200px) {
  .composite-summary-grid {
    grid-template-columns: repeat(2, minmax(128px, 1fr));
  }
}
@media (max-width: 640px) {
  .composite-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
