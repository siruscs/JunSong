<template>
  <div class="app-container accounting-period-page">
    <el-card class="period-panel" shadow="never">
      <div class="period-header">
        <div>
          <div class="panel-title">店面核算状态</div>
          <div class="panel-subtitle">按当前核算周期汇总实际缴款、已核销费用、进货款和未核销借支；销售额不直接计入收入</div>
        </div>
        <div class="period-actions">
          <el-button type="warning" size="small" @click="handleTrialBreakEven" v-hasPermi="['finance:accountingPeriod:query']">试算回本</el-button>
          <el-button type="success" size="small" v-if="currentPeriod && currentPeriod.status === '0'" @click="handleCarryForward" v-hasPermi="['finance:accountingPeriod:carryForward']">结转</el-button>
          <el-button type="danger" size="small" v-if="canRollback" @click="handleRollbackCarryForward" v-hasPermi="['finance:accountingPeriod:rollback']">结转回退</el-button>
          <el-button size="small" @click="handleInitCurrent" v-hasPermi="['finance:accountingPeriod:init']">初始化周期</el-button>
        </div>
      </div>

      <div v-if="currentPeriod" class="period-summary">
        <div class="status-box" :class="'status-' + currentPeriod.status">
          <div class="status-label">当前状态</div>
          <div class="status-value">{{ getStatusText(currentPeriod.status) }}</div>
          <div class="status-desc">
            <span v-if="currentPeriod.status === '2'">结转时间：{{ parseTime(currentPeriod.carryForwardTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <span v-else>距回本还差：¥{{ formatMoney(breakEvenGap) }}</span>
          </div>
        </div>
        <div class="metric-grid">
          <div class="metric-card">
            <div class="metric-label">实际缴款收入</div>
            <div class="metric-value income">¥{{ formatMoney(currentPeriod.totalSalePayment) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">已核销费用</div>
            <div class="metric-value cost">¥{{ formatMoney(currentPeriod.totalVerifiedExpense) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">进货款</div>
            <div class="metric-value cost">¥{{ formatMoney(currentPeriod.totalPurchase) }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">借支未核销</div>
            <div class="metric-value cost">¥{{ formatMoney(currentPeriod.totalUnverifiedAdvance) }}</div>
          </div>
          <div class="metric-card emphasis">
            <div class="metric-label">净利</div>
            <div class="metric-value" :class="Number(currentPeriod.netProfit || 0) >= 0 ? 'income' : 'cost'">¥{{ formatMoney(currentPeriod.netProfit) }}</div>
          </div>
        </div>
      </div>

      <el-empty v-else description="请选择店面并初始化当前核算周期" />
    </el-card>

    <!-- 试算回本结果弹窗 -->
    <el-dialog title="试算回本结果" v-model="trialOpen" width="600px" append-to-body>
      <div v-if="trialResult" class="trial-result">
        <el-result :icon="trialResult.isBreakEven ? 'success' : 'warning'"
          :title="trialResult.isBreakEven ? '已达到回本条件' : '暂未达到回本条件'">
          <template #sub-title>
            <div class="trial-detail">
              <div class="trial-row">
                <span class="trial-label">实际缴款收入</span>
                <span class="trial-value income">¥{{ formatMoney(trialResult.totalSalePayment) }}</span>
              </div>
              <div class="trial-row">
                <span class="trial-label">周期成本（已核销费用+进货+未核销借支）</span>
                <span class="trial-value cost">¥{{ formatMoney(trialResult.costTotal) }}</span>
              </div>
              <div class="trial-row" v-if="!trialResult.isBreakEven">
                <span class="trial-label">距回本还差</span>
                <span class="trial-value cost">¥{{ formatMoney(trialResult.gap) }}</span>
              </div>
              <div class="trial-row">
                <span class="trial-label">净利润</span>
                <span class="trial-value" :class="Number(trialResult.netProfit) >= 0 ? 'income' : 'cost'">¥{{ formatMoney(trialResult.netProfit) }}</span>
              </div>
            </div>
          </template>
          <template #extra>
            <el-button type="primary" @click="trialOpen = false">确 定</el-button>
          </template>
        </el-result>
      </div>
    </el-dialog>

    <div class="period-list-panel">
      <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px" class="query-form">
        <el-form-item label="机构" prop="deptIds">
          <el-select v-model="queryParams.deptIds" placeholder="选择机构" filterable clearable multiple collapse-tags collapse-tags-tooltip style="width: 260px;">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="选择状态" clearable style="width: 160px;">
            <el-option label="进行中" value="0" />
            <el-option label="已结转" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期编号" prop="periodNo">
          <el-input v-model="queryParams.periodNo" placeholder="请输入周期编号" clearable style="width: 180px;" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="query-actions">
          <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
          <el-button size="small" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="table-toolbar">
        <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="periodList" class="period-table">
      <el-table-column label="周期编号" align="center" prop="periodNo" min-width="160" />
      <el-table-column label="机构" align="center" prop="deptId" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ getDeptName(scope.row.deptId) }}</template>
      </el-table-column>
      <el-table-column label="周期开始" align="center" prop="startTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="总费用" align="center" prop="totalVerifiedExpense" width="110">
        <template #default="scope">¥{{ formatMoney(scope.row.totalVerifiedExpense) }}</template>
      </el-table-column>
      <el-table-column label="总进货" align="center" prop="totalPurchase" width="110">
        <template #default="scope">¥{{ formatMoney(scope.row.totalPurchase) }}</template>
      </el-table-column>
      <el-table-column label="总销售" align="center" prop="totalSaleAmount" width="110">
        <template #default="scope">¥{{ formatMoney(scope.row.totalSaleAmount) }}</template>
      </el-table-column>
      <el-table-column label="总缴款" align="center" prop="totalSalePayment" width="110">
        <template #default="scope">¥{{ formatMoney(scope.row.totalSalePayment) }}</template>
      </el-table-column>
      <el-table-column label="利润" align="center" prop="netProfit" width="110">
        <template #default="scope">
          <span :class="Number(scope.row.netProfit || 0) >= 0 ? 'income-text' : 'cost-text'">¥{{ formatMoney(scope.row.netProfit) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结转时间" align="center" prop="carryForwardTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.carryForwardTime, '{y}-{m}-{d} {h}:{i}:{s}') || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right" width="440">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleDetail(scope.row)">明细</el-button>
          <el-button size="small" type="text" @click="handleExportDetail(scope.row)">导出</el-button>
          <el-button size="small" type="text" @click="handleCheckBeforeLock(scope.row)" v-hasPermi="['finance:accountingPeriod:checkBeforeLock']">锁账检查</el-button>
          <el-button size="small" type="text" style="color:#e6a23c" @click="handleOpsAdjustStartTime(scope.row)" v-hasPermi="['finance:accountingPeriod:opsAdjustStartTime']">调整起始时间</el-button>
        </template>
      </el-table-column>
      </el-table>

      <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </div>

    <el-dialog :title="detailTitle" v-model="detailOpen" width="1100px" append-to-body class="period-detail-dialog">
      <el-tabs v-model="detailActiveTab">
        <el-tab-pane label="费用" name="expense">
          <el-table v-loading="detailLoading" :data="detail.expenses" height="360">
            <el-table-column label="费用单号" prop="expenseNo" min-width="150" />
            <el-table-column label="费用日期" prop="expenseDate" width="120">
              <template #default="scope">{{ parseTime(scope.row.expenseDate, '{y}-{m}-{d}') }}</template>
            </el-table-column>
            <el-table-column label="类别" prop="expenseType" width="100" />
            <el-table-column label="内容" prop="expenseContent" min-width="180" show-overflow-tooltip />
            <el-table-column label="金额" prop="expenseAmount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.expenseAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="90">
              <template #default="scope">{{ scope.row.status === '1' ? '已核销' : '未核销' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="借支" name="advance">
          <el-table v-loading="detailLoading" :data="detail.advances" height="360">
            <el-table-column label="借支单号" prop="advanceNo" min-width="150" />
            <el-table-column label="借支日期" prop="advanceDate" width="120">
              <template #default="scope">{{ parseTime(scope.row.advanceDate, '{y}-{m}-{d}') }}</template>
            </el-table-column>
            <el-table-column label="借款人" prop="borrower" width="120" />
            <el-table-column label="用途" prop="purpose" min-width="180" show-overflow-tooltip />
            <el-table-column label="金额" prop="advanceAmount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.advanceAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="90">
              <template #default="scope">{{ scope.row.status === '1' ? '已核销' : '未核销' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="进货" name="purchase">
          <el-table v-loading="detailLoading" :data="purchaseDetailRows" height="360" border :span-method="purchaseDetailSpan">
            <el-table-column label="进货单号" prop="purchaseNo" min-width="150" />
            <el-table-column label="供应商" prop="supplierName" min-width="150" show-overflow-tooltip />
            <el-table-column label="进货日期" prop="purchaseDate" width="120">
              <template #default="scope">{{ parseTime(scope.row.purchaseDate, '{y}-{m}-{d}') }}</template>
            </el-table-column>
            <el-table-column label="总金额" prop="totalAmount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="100">
              <template #default="scope">{{ getPurchaseStatusText(scope.row.status) }}</template>
            </el-table-column>
            <el-table-column label="商品名称" prop="productName" min-width="140" show-overflow-tooltip />
            <el-table-column label="数量" prop="normalQuantity" width="90" align="right" />
            <el-table-column label="赠数" prop="giftQuantity" width="90" align="right" />
            <el-table-column label="单价" prop="price" width="100" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.price) }}</template>
            </el-table-column>
            <el-table-column label="金额" prop="amount" width="110" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="投资来源" name="invest">
          <el-table v-loading="detailLoading" :data="detail.investRecords" height="360">
            <el-table-column label="投资人" prop="investorName" min-width="140" />
            <el-table-column label="投资时间" prop="investTime" width="170">
              <template #default="scope">{{ parseTime(scope.row.investTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
            </el-table-column>
            <el-table-column label="投资金额" prop="investAmount" width="130" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.investAmount) }}</template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="销售缴款" name="sale">
          <div class="detail-hint">缴款按实际发生计入当前核算周期；历史销售欠款不会回写原销售周期。</div>
          <el-table v-loading="detailLoading" :data="salePaymentRows" height="360" border :span-method="salePaymentSpan">
            <el-table-column label="商品名称" prop="productName" min-width="140" show-overflow-tooltip />
            <el-table-column label="销售日期" prop="saleDate" width="120" align="center">
              <template #default="scope">{{ parseTime(scope.row.saleDate, '{y}-{m}-{d}') }}</template>
            </el-table-column>
            <el-table-column label="销售金额" prop="saleAmount" width="110" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.saleAmount) }}</template>
            </el-table-column>
            <el-table-column label="已缴款" prop="paidAmount" width="110" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.paidAmount) }}</template>
            </el-table-column>
            <el-table-column label="未缴款" prop="unpaidAmount" width="110" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.unpaidAmount) }}</template>
            </el-table-column>
            <el-table-column label="缴款时间" prop="paymentDate" width="170">
              <template #default="scope">{{ parseTime(scope.row.paymentDate, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
            </el-table-column>
            <el-table-column label="缴款归属" width="120" align="center">
              <template #default="scope">
                <el-tag v-if="scope.row.isHistoricalPayment" type="warning" size="small">历史欠款回收</el-tag>
                <el-tag v-else type="success" size="small">本周期销售</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="缴款金额" prop="paymentAmount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.paymentAmount) }}</template>
            </el-table-column>
            <el-table-column label="付款方式" prop="paymentMethod" min-width="110" align="center">
              <template #default="scope">
                <dict-tag :options="dict.type.finance_payment_method" :value="scope.row.paymentMethod" />
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="分润结果" name="share">
          <el-table v-loading="detailLoading" :data="detail.profitShares" height="180">
            <el-table-column label="分润单号" prop="shareNo" min-width="150" />
            <el-table-column label="分润时间" prop="shareTime" width="170">
              <template #default="scope">{{ parseTime(scope.row.shareTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
            </el-table-column>
            <el-table-column label="净利" prop="netProfit" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.netProfit) }}</template>
            </el-table-column>
            <el-table-column label="店长分润" prop="managerProfitAmount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.managerProfitAmount) }}</template>
            </el-table-column>
            <el-table-column label="投资人分润" prop="investorProfitAmount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.investorProfitAmount) }}</template>
            </el-table-column>
            <el-table-column label="店长比例" prop="managerProfitRate" width="100" align="right">
              <template #default="scope">{{ formatRate(scope.row.managerProfitRate) }}</template>
            </el-table-column>
          </el-table>
          <el-table v-loading="detailLoading" :data="profitShareDetails" height="180" style="margin-top: 12px;">
            <el-table-column label="接收方" prop="receiverName" min-width="140" />
            <el-table-column label="类型" prop="receiverType" width="100">
              <template #default="scope">{{ scope.row.receiverType === '1' ? '店长' : '投资人' }}</template>
            </el-table-column>
            <el-table-column label="投资金额" prop="investAmount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.investAmount) }}</template>
            </el-table-column>
            <el-table-column label="投资占比" prop="investRatio" width="100" align="right">
              <template #default="scope">{{ formatRate(scope.row.investRatio) }}</template>
            </el-table-column>
            <el-table-column label="分润金额" prop="shareAmount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.shareAmount) }}</template>
            </el-table-column>
            <el-table-column label="返款ID" prop="paymentId" width="100" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="分润返款" name="payment">
          <el-table v-loading="detailLoading" :data="detail.investorPayments" height="360">
            <el-table-column label="返款单号" prop="paymentNo" min-width="150" />
            <el-table-column label="投资人" prop="investorName" min-width="120" />
            <el-table-column label="返款日期" prop="paymentDate" width="160">
              <template #default="scope">{{ parseTime(scope.row.paymentDate, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
            </el-table-column>
            <el-table-column label="金额" prop="amount" width="120" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.amount) }}</template>
            </el-table-column>
            <el-table-column label="来源" prop="sourceType" width="110">
              <template #default="scope">{{ scope.row.sourceType === '1' ? '结转自动' : '手工' }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="paymentStatus" width="100">
              <template #default="scope">{{ scope.row.paymentStatus === '1' ? '已返款' : '待返款' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
      </template>
    </el-dialog>

    <!-- 锁账前检查弹窗 -->
    <el-dialog title="锁账前检查" v-model="checkDialogOpen" width="600px" append-to-body>
      <div v-loading="checkLoading">
        <div v-if="checkResult">
          <div v-if="checkResult.canLock && !checkResult.hasWarning" class="check-summary check-summary-success">
            <el-icon class="check-summary-icon"><SuccessFilled /></el-icon>
            <span>所有检查项均通过，可以安全锁账。</span>
          </div>
          <div v-else-if="checkResult.canLock && checkResult.hasWarning" class="check-summary check-summary-warning">
            <el-icon class="check-summary-icon"><WarningFilled /></el-icon>
            <span>存在警告项，可以强制结转但建议先处理。</span>
          </div>
          <div v-else class="check-summary check-summary-block">
            <el-icon class="check-summary-icon"><CircleCloseFilled /></el-icon>
            <span>存在阻塞项，必须先处理后才能锁账。</span>
          </div>

          <div class="check-items">
            <div v-for="item in checkResult.items" :key="item.checkType"
                 class="check-item"
                 :class="{ 'check-item-block': item.level === 'BLOCK' && item.count > 0, 'check-item-warning': item.level === 'WARNING' && item.count > 0, 'check-item-info': item.level === 'INFO' && item.count > 0, 'check-item-clean': item.count === 0 }">
              <div class="check-item-header">
                <el-tag :type="getCheckLevelTagType(item)" size="small">{{ item.level }}</el-tag>
                <span class="check-item-title">{{ item.title }}</span>
                <span class="check-item-count">{{ item.count }}笔</span>
              </div>
              <div class="check-item-desc">{{ item.description }}</div>
            </div>
          </div>

          <div v-if="checkResult.canLock && checkResult.hasWarning" class="force-carry-forward-section">
            <el-divider>强制结转</el-divider>
            <el-input v-model="forceCarryForwardReason" type="textarea" :rows="3" placeholder="请填写强制结转原因（必填）" />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="checkDialogOpen = false">关 闭</el-button>
          <el-button v-if="checkResult && checkResult.canLock && checkResult.hasWarning"
                     type="warning"
                     :disabled="!forceCarryForwardReason || forceCarryForwardReason.trim().length === 0"
                     @click="handleForceCarryForward"
                     v-hasPermi="['finance:accountingPeriod:carryForward']">强制结转</el-button>
          <el-button v-if="checkResult && checkResult.canLock && !checkResult.hasWarning"
                     type="success"
                     @click="handleProceedCarryForward"
                     v-hasPermi="['finance:accountingPeriod:carryForward']">确认结转</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 运维调整起始时间弹窗 -->
    <el-dialog title="运维调整周期起始/结束时间" v-model="opsAdjustOpen" width="560px" append-to-body>
      <el-form ref="opsAdjustFormRef" :model="opsAdjustForm" :rules="opsAdjustRules" label-width="120px">
        <el-form-item label="周期编号">
          <span>{{ opsAdjustRow?.periodNo || '-' }}</span>
        </el-form-item>
        <el-form-item label="机构">
          <span>{{ getDeptName(opsAdjustRow?.deptId) }}</span>
        </el-form-item>
        <el-form-item label="原起始时间">
          <span>{{ parseTime(opsAdjustRow?.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </el-form-item>
        <el-form-item label="原结束时间">
          <span>{{ parseTime(opsAdjustRow?.endTime, '{y}-{m}-{d} {h}:{i}:{s}') || '（进行中，暂无结束时间）' }}</span>
        </el-form-item>
        <el-form-item label="新起始时间" prop="startTime">
          <el-date-picker v-model="opsAdjustForm.startTime" type="datetime" placeholder="选择新起始时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="新结束时间">
          <el-date-picker v-model="opsAdjustForm.endTime" type="datetime" placeholder="留空表示不修改结束时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input v-model="opsAdjustForm.reason" type="textarea" :rows="3" placeholder="请输入调整原因（不少于5个字）" maxlength="200" show-word-limit />
        </el-form-item>
        <el-alert type="warning" :closable="false" show-icon>
          <div style="line-height: 1.6">
            该操作仅调整周期起始/结束时间，不重新核算周期金额，不重新计算分润。<br/>
            如该周期已存在分润记录，分润时间将同步为周期结束时间。
          </div>
        </el-alert>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="opsAdjustOpen = false">取 消</el-button>
          <el-button type="primary" :loading="opsAdjustSubmitting" @click="submitOpsAdjustStartTime">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAccountingPeriod, getCurrentAccountingPeriod, initCurrentAccountingPeriod, trialBreakEven, carryForward, rollbackCarryForward, getAccountingPeriodDetail, checkBeforeLock, opsAdjustAccountingPeriodStartTime } from '@/api/finance/accountingPeriod'
import { useUserStore } from '@/stores/user'
import { useDict } from '@/composables/useDict'
import { saveAs } from 'file-saver'
import ExcelJS from 'exceljs'

const userStore = useUserStore()

export default {
  name: 'AccountingPeriod',
  setup() {
    const dict = useDict('finance_payment_method')
    return { dict }
  },
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      periodList: [],
      currentPeriod: null,
      trialOpen: false,
      trialResult: null,
      detailOpen: false,
      detailLoading: false,
      detailTitle: '周期明细',
      detailActiveTab: 'expense',
      detail: {
        expenses: [],
        advances: [],
        purchases: [],
        investRecords: [],
        sales: [],
        salePayments: [],
        profitShares: [],
        investorPayments: []
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deptIds: userStore.currentDeptId ? [userStore.currentDeptId] : [],
        status: undefined,
        periodNo: undefined
      },
      // 锁账检查弹窗
      checkDialogOpen: false,
      checkLoading: false,
      checkResult: null,
      checkDeptId: null,
      forceCarryForwardReason: '',
      // 运维调整起始时间弹窗
      opsAdjustOpen: false,
      opsAdjustSubmitting: false,
      opsAdjustRow: null,
      opsAdjustForm: {
        startTime: '',
        endTime: '',
        reason: ''
      },
      opsAdjustRules: {
        startTime: [{ required: true, message: '新起始时间不能为空', trigger: 'change' }],
        reason: [
          { required: true, message: '调整原因不能为空', trigger: 'blur' },
          { min: 5, message: '调整原因不少于5个字', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    deptOptions() {
      return userStore.depts || []
    },
    breakEvenGap() {
      if (!this.currentPeriod) {
        return 0
      }
      const costTotal = this.getCostTotal(this.currentPeriod)
      const salePayment = Number(this.currentPeriod.totalSalePayment || 0)
      return Math.max(costTotal - salePayment, 0)
    },
    canRollback() {
      // 当有已结转的周期且当前有进行中的周期时，可以回退
      if (!this.currentPeriod || this.currentPeriod.status !== '0') {
        return false
      }
      // 检查是否有已结转的周期
      return this.periodList.some(p => p.status === '2')
    },
    profitShareDetails() {
      return (this.detail.profitShares || []).reduce((list, share) => {
        return list.concat(share.details || [])
      }, [])
    },
    salePaymentRows() {
      const saleMap = {}
      ;(this.detail.sales || []).forEach(sale => {
        saleMap[sale.saleId] = sale
      })
      const rows = (this.detail.salePayments || []).map(p => {
        const sale = saleMap[p.saleId] || {}
        return {
          ...p,
          saleNo: sale.saleNo || '',
          productName: sale.productName || '-',
          saleDate: sale.saleDate,
          saleAmount: sale.saleAmount,
          paidAmount: sale.paidAmount,
          unpaidAmount: Number(sale.saleAmount || 0) - Number(sale.paidAmount || 0),
          salePeriodId: sale.periodId,
          paymentPeriodId: p.periodId,
          isHistoricalPayment: sale.periodId != null && p.periodId != null && String(sale.periodId) !== String(p.periodId)
        }
      })
      rows.sort((a, b) => String(a.saleNo).localeCompare(String(b.saleNo)))
      const groupSize = {}
      rows.forEach(r => {
        groupSize[r.saleNo] = (groupSize[r.saleNo] || 0) + 1
      })
      let prev = null
      rows.forEach(r => {
        if (r.saleNo !== prev) {
          r._rowspan = groupSize[r.saleNo]
          prev = r.saleNo
        } else {
          r._rowspan = 0
        }
      })
      return rows
    },
    purchaseDetailRows() {
      const rows = []
      ;(this.detail.purchases || []).forEach(purchase => {
        const details = purchase.details && purchase.details.length ? purchase.details : [{}]
        details.forEach((item, index) => {
          const gift = String(item.isGift) === '1'
          rows.push({
            ...purchase,
            ...item,
            productName: item.productName || '-',
            normalQuantity: gift ? 0 : (item.quantity || 0),
            giftQuantity: gift ? (item.quantity || 0) : 0,
            _purchaseRowspan: index === 0 ? details.length : 0
          })
        })
      })
      return rows
    }
  },
  created() {
    this.getList()
    this.handleDeptChange(userStore.currentDeptId)
  },
  methods: {
    getList() {
      this.loading = true
      const deptIds = this.queryParams.deptIds && this.queryParams.deptIds.length > 0
        ? this.queryParams.deptIds
        : (userStore.currentDeptId ? [userStore.currentDeptId] : [])

      if (deptIds.length === 0) {
        this.periodList = []
        this.total = 0
        this.loading = false
        return
      }

      // 为每个 deptId 并行调用 API 并合并结果
      const promises = deptIds.map(deptId =>
        listAccountingPeriod({ ...this.queryParams, deptId, deptIds: undefined })
      )
      Promise.all(promises).then(results => {
        let allRows = []
        let totalCount = 0
        results.forEach(res => {
          allRows = allRows.concat(res.rows || [])
          totalCount += res.total || 0
        })
        this.periodList = allRows
        this.total = totalCount
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleDeptChange(deptId) {
      this.currentPeriod = null
      if (!deptId) {
        return
      }
      getCurrentAccountingPeriod(deptId).then(response => {
        this.currentPeriod = response.data
      })
    },
    handleInitCurrent() {
      initCurrentAccountingPeriod(userStore.currentDeptId).then(response => {
        this.currentPeriod = response.data
        this.queryParams.deptIds = userStore.currentDeptId ? [userStore.currentDeptId] : []
        this.getList()
        ElMessage.success('当前核算周期已初始化')
      })
    },
    handleTrialBreakEven() {
      trialBreakEven(userStore.currentDeptId).then(response => {
        const period = response.data
        const costTotal = Number(period.totalVerifiedExpense || 0) + Number(period.totalPurchase || 0) + Number(period.totalUnverifiedAdvance || 0)
        const salePayment = Number(period.totalSalePayment || 0)
        const isBreakEven = salePayment >= costTotal
        this.trialResult = {
          isBreakEven: isBreakEven,
          totalSalePayment: period.totalSalePayment,
          costTotal: costTotal,
          gap: Math.max(costTotal - salePayment, 0),
          netProfit: period.netProfit
        }
        this.trialOpen = true
        // 刷新当前周期数据
        this.currentPeriod = period
      })
    },
    handleCarryForward() {
      ElMessageBox.confirm('确认对当前核算周期进行结转？结转后将自动执行分润计算并创建新的核算周期。').then(() => {
        return carryForward(userStore.currentDeptId)
      }).then(response => {
        this.currentPeriod = response.data
        this.getList()
        ElMessage.success('结转完成，新核算周期已创建')
      }).catch(() => {})
    },
    handleRollbackCarryForward() {
      ElMessageBox.prompt('请输入结转回退原因（必填，将记录审计日志）', '结转回退确认', {
        confirmButtonText: '确认回退',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入回退原因',
        inputValidator: (value) => {
          if (!value || !value.trim()) return '回退原因不能为空'
          return true
        }
      }).then(({ value }) => {
        return rollbackCarryForward(userStore.currentDeptId, value.trim())
      }).then(response => {
        this.currentPeriod = response.data
        this.getList()
        ElMessage.success('结转已回退')
      }).catch(() => {})
    },
    handleDetail(row) {
      this.detailOpen = true
      this.detailLoading = true
      this.detailActiveTab = 'expense'
      this.detailTitle = `周期明细：${row.periodNo || row.periodId}`
      getAccountingPeriodDetail(row.periodId).then(response => {
        const data = response.data || {}
        this.detail.expenses = data.expenses || []
        this.detail.advances = data.advances || []
        this.detail.purchases = data.purchases || []
        this.detail.investRecords = data.investRecords || []
        this.detail.sales = data.sales || []
        this.detail.salePayments = data.salePayments || []
        this.detail.profitShares = data.profitShares || []
        this.detail.investorPayments = data.investorPayments || []
      }).finally(() => {
        this.detailLoading = false
      })
    },
    handleExportDetail(row) {
      const loading = ElMessage({ message: '正在生成明细导出...', type: 'info', duration: 0 })
      getAccountingPeriodDetail(row.periodId).then(async response => {
        const data = response.data || {}
        const buffer = await this.buildDetailExportXlsx(row, data)
        const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        saveAs(blob, `周期明细_${row.periodNo || row.periodId}_${new Date().getTime()}.xlsx`)
        ElMessage.success('导出成功')
      }).catch(() => {
        ElMessage.error('导出失败')
      }).finally(() => {
        loading.close()
      })
    },
    async buildDetailExportXlsx(row, data) {
      const money = v => Number(v || 0)
      const dateTime = v => this.parseTime(v, '{y}-{m}-{d} {h}:{i}:{s}') || ''
      const date = v => this.parseTime(v, '{y}-{m}-{d}') || ''
      const methodLabel = v => {
        const opt = (this.dict.type.finance_payment_method || []).find(d => d.value === String(v))
        return opt ? opt.label : (v || '')
      }

      const TOTAL_COLS = 10
      const BORDER = { top: { style: 'thin', color: { argb: 'FFDCDFE6' } }, left: { style: 'thin', color: { argb: 'FFDCDFE6' } }, bottom: { style: 'thin', color: { argb: 'FFDCDFE6' } }, right: { style: 'thin', color: { argb: 'FFDCDFE6' } } }
      const HEADER_FILL = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF5F7FA' } }
      const TITLE_FILL = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFECF5FF' } }

      const wb = new ExcelJS.Workbook()
      const ws = wb.addWorksheet('周期明细')
      ws.columns = [{ width: 20 }, { width: 16 }, { width: 14 }, { width: 20 }, { width: 16 }, { width: 20 }, { width: 12 }, { width: 12 }, { width: 12 }, { width: 16 }]

      const applyBorder = (r) => { for (let c = 1; c <= TOTAL_COLS; c++) { r.getCell(c).border = BORDER } }

      const addBigTitle = (text) => {
        const r = ws.addRow([text])
        ws.mergeCells(r.number, 1, r.number, TOTAL_COLS)
        const cell = r.getCell(1)
        cell.font = { bold: true, size: 15, color: { argb: 'FF303133' } }
        cell.alignment = { vertical: 'middle', horizontal: 'left' }
        r.height = 24
      }
      const addSectionTitle = (text) => {
        const r = ws.addRow([text])
        ws.mergeCells(r.number, 1, r.number, TOTAL_COLS)
        const cell = r.getCell(1)
        cell.font = { bold: true, size: 12, color: { argb: 'FF303133' } }
        cell.fill = TITLE_FILL
        cell.alignment = { vertical: 'middle', horizontal: 'left' }
        applyBorder(r)
        r.height = 20
      }
      const addHeaderRow = (headers) => {
        const r = ws.addRow(headers)
        headers.forEach((h, i) => {
          const cell = r.getCell(i + 1)
          cell.font = { bold: true, color: { argb: 'FF303133' } }
          cell.fill = HEADER_FILL
          cell.alignment = { vertical: 'middle', horizontal: 'center' }
          cell.border = BORDER
        })
        r.height = 18
      }
      const addDataRow = (values, aligns) => {
        const r = ws.addRow(values)
        values.forEach((v, i) => {
          const cell = r.getCell(i + 1)
          cell.border = BORDER
          cell.alignment = { vertical: 'middle', horizontal: (aligns && aligns[i]) || 'left', wrapText: true }
        })
        return r
      }
      const addEmptyRow = (span) => {
        const r = ws.addRow(['暂无数据'])
        ws.mergeCells(r.number, 1, r.number, span)
        r.getCell(1).alignment = { horizontal: 'center' }
        applyBorder(r)
      }
      const spacer = () => ws.addRow([])

      addBigTitle(`周期明细：${row.periodNo || row.periodId}（${this.getDeptName(row.deptId)}）`)
      spacer()

      addSectionTitle('汇总')
      addHeaderRow(['周期', '总费用', '总进货', '总销售', '总缴款', '利润', '结转时间'])
      const sumRow = addDataRow([
        row.periodNo || row.periodId,
        money(row.totalVerifiedExpense),
        money(row.totalPurchase),
        money(row.totalSaleAmount),
        money(row.totalSalePayment),
        money(row.netProfit),
        this.parseTime(row.carryForwardTime, '{y}-{m}-{d} {h}:{i}:{s}') || '-'
      ], ['center', 'right', 'right', 'right', 'right', 'right', 'center'])
      ;[2, 3, 4, 5, 6].forEach(c => { sumRow.getCell(c).numFmt = '¥#,##0.00' })
      spacer()

      const addSection = (title, headers, rows, buildRow, aligns) => {
        addSectionTitle(title)
        addHeaderRow(headers)
        if (!rows || rows.length === 0) {
          addEmptyRow(headers.length)
        } else {
          rows.forEach(item => {
            const { values, moneyCols } = buildRow(item)
            const r = addDataRow(values, aligns)
            ;(moneyCols || []).forEach(c => { r.getCell(c).numFmt = '¥#,##0.00' })
          })
        }
        spacer()
      }

      addSection('费用', ['费用单号', '费用日期', '类别', '内容', '金额', '状态'],
        data.expenses || [],
        r => ({ values: [r.expenseNo, date(r.expenseDate), r.expenseType, r.expenseContent, money(r.expenseAmount), r.status === '1' ? '已核销' : '未核销'], moneyCols: [5] }),
        ['left', 'center', 'center', 'left', 'right', 'center'])

      addSection('借支', ['借支单号', '借支日期', '借款人', '用途', '金额', '状态'],
        data.advances || [],
        r => ({ values: [r.advanceNo, date(r.advanceDate), r.borrower, r.purpose, money(r.advanceAmount), r.status === '1' ? '已核销' : '未核销'], moneyCols: [5] }),
        ['left', 'center', 'left', 'left', 'right', 'center'])

      const purchaseRows = []
      ;(data.purchases || []).forEach(p => {
        const details = p.details && p.details.length ? p.details : [{}]
        details.forEach(d => {
          const gift = String(d.isGift) === '1'
          purchaseRows.push({ p, d, normalQuantity: gift ? 0 : (d.quantity || 0), giftQuantity: gift ? (d.quantity || 0) : 0 })
        })
      })
      addSection('进货', ['进货单号', '供应商', '进货日期', '总金额', '状态', '商品名称', '数量', '赠数', '单价', '金额'],
        purchaseRows,
        r => ({ values: [r.p.purchaseNo, r.p.supplierName, date(r.p.purchaseDate), money(r.p.totalAmount), this.getPurchaseStatusText(r.p.status), r.d.productName || '-', r.normalQuantity, r.giftQuantity, money(r.d.price), money(r.d.amount)], moneyCols: [4, 9, 10] }),
        ['left', 'left', 'center', 'right', 'center', 'left', 'right', 'right', 'right', 'right'])

      addSection('投资来源', ['投资人', '投资时间', '投资金额', '备注'],
        data.investRecords || [],
        r => ({ values: [r.investorName, dateTime(r.investTime), money(r.investAmount), r.remark], moneyCols: [3] }),
        ['left', 'center', 'right', 'left'])

      // 销售缴款（按销售单号合并前5列）
      addSectionTitle('销售缴款')
      const saleHeaders = ['商品名称', '销售日期', '销售金额', '已缴款', '未缴款', '缴款时间', '缴款金额', '付款方式', '备注']
      addHeaderRow(saleHeaders)
      const saleMap = {}
      ;(data.sales || []).forEach(s => { saleMap[s.saleId] = s })
      const payRows = (data.salePayments || []).map(p => {
        const s = saleMap[p.saleId] || {}
        return { ...p, saleNo: s.saleNo || '', productName: s.productName || '-', saleDate: s.saleDate, saleAmount: s.saleAmount, paidAmount: s.paidAmount, unpaidAmount: Number(s.saleAmount || 0) - Number(s.paidAmount || 0) }
      })
      payRows.sort((a, b) => String(a.saleNo).localeCompare(String(b.saleNo)))
      if (payRows.length === 0) {
        addEmptyRow(saleHeaders.length)
      } else {
        const groupSize = {}
        payRows.forEach(r => { groupSize[r.saleNo] = (groupSize[r.saleNo] || 0) + 1 })
        let idx = 0
        let prevNo = null
        let groupStartRow = 0
        payRows.forEach(r => {
          const rowValues = [
            r.productName, date(r.saleDate), money(r.saleAmount), money(r.paidAmount), money(r.unpaidAmount),
            dateTime(r.paymentDate), money(r.paymentAmount), methodLabel(r.paymentMethod), r.remark
          ]
          const excelRow = addDataRow(rowValues, ['left', 'center', 'right', 'right', 'right', 'center', 'right', 'center', 'left'])
          ;[3, 4, 5, 7].forEach(c => { excelRow.getCell(c).numFmt = '¥#,##0.00' })
          if (r.saleNo !== prevNo) {
            if (prevNo !== null && idx - groupStartRow > 0) {
              for (let c = 1; c <= 5; c++) { ws.mergeCells(groupStartRow, c, idx, c) }
            }
            groupStartRow = excelRow.number
            prevNo = r.saleNo
          }
          idx = excelRow.number
        })
        if (prevNo !== null && idx - groupStartRow > 0) {
          for (let c = 1; c <= 5; c++) { ws.mergeCells(groupStartRow, c, idx, c) }
        }
      }
      spacer()

      addSection('分润结果', ['分润单号', '分润时间', '净利', '店长分润', '投资人分润', '店长比例'],
        data.profitShares || [],
        r => ({ values: [r.shareNo, dateTime(r.shareTime), money(r.netProfit), money(r.managerProfitAmount), money(r.investorProfitAmount), this.formatRate(r.managerProfitRate)], moneyCols: [3, 4, 5] }),
        ['left', 'center', 'right', 'right', 'right', 'right'])

      addSection('分润返款', ['返款单号', '投资人', '返款日期', '金额', '来源', '状态'],
        data.investorPayments || [],
        r => ({ values: [r.paymentNo, r.investorName, dateTime(r.paymentDate), money(r.amount), r.sourceType === '1' ? '结转自动' : '手工', r.paymentStatus === '1' ? '已返款' : '待返款'], moneyCols: [4] }),
        ['left', 'left', 'center', 'right', 'center', 'center'])

      return await wb.xlsx.writeBuffer()
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    getStatusText(status) {
      const statusMap = {
        '0': '进行中',
        '2': '已结转'
      }
      return statusMap[status] || status || '-'
    },
    getStatusType(status) {
      const typeMap = {
        '0': 'warning',
        '2': 'info'
      }
      return typeMap[status] || 'info'
    },
    getPurchaseStatusText(status) {
      const statusMap = {
        '0': '草稿',
        '1': '已确认',
        '2': '已完成'
      }
      return statusMap[status] || status || '-'
    },
    formatRate(value) {
      return (Number(value || 0) * 100).toFixed(2) + '%'
    },
    getCostTotal(row) {
      return Number(row.totalVerifiedExpense || 0) + Number(row.totalPurchase || 0) + Number(row.totalUnverifiedAdvance || 0)
    },
    getDeptName(deptId) {
      const dept = this.deptOptions.find(item => item.deptId === deptId)
      return dept ? dept.deptName : deptId || '-'
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    salePaymentSpan({ row, columnIndex }) {
      if (columnIndex >= 0 && columnIndex <= 4) {
        return { rowspan: row._rowspan, colspan: row._rowspan > 0 ? 1 : 0 }
      }
    },
    purchaseDetailSpan({ row, columnIndex }) {
      if (columnIndex >= 0 && columnIndex <= 4) {
        return { rowspan: row._purchaseRowspan, colspan: row._purchaseRowspan > 0 ? 1 : 0 }
      }
    },
    handleCheckBeforeLock(row) {
      this.checkDeptId = row.deptId
      this.checkResult = null
      this.forceCarryForwardReason = ''
      this.checkDialogOpen = true
      this.checkLoading = true
      checkBeforeLock(row.deptId).then(response => {
        this.checkResult = response.data
      }).catch(() => {
        ElMessage.error('锁账检查请求失败')
      }).finally(() => {
        this.checkLoading = false
      })
    },
    getCheckLevelTagType(item) {
      if (item.count === 0) return 'success'
      if (item.level === 'BLOCK') return 'danger'
      if (item.level === 'WARNING') return 'warning'
      return 'info'
    },
    handleForceCarryForward() {
      if (!this.forceCarryForwardReason || this.forceCarryForwardReason.trim().length === 0) {
        ElMessage.warning('请填写强制结转原因')
        return
      }
      ElMessageBox.confirm('确认强制结转？当前存在未处理的警告项。').then(() => {
        return carryForward(this.checkDeptId || userStore.currentDeptId)
      }).then(response => {
        this.currentPeriod = response.data
        this.getList()
        this.checkDialogOpen = false
        ElMessage.success('强制结转完成')
      }).catch(() => {})
    },
    handleProceedCarryForward() {
      ElMessageBox.confirm('确认对当前核算周期进行结转？').then(() => {
        return carryForward(this.checkDeptId || userStore.currentDeptId)
      }).then(response => {
        this.currentPeriod = response.data
        this.getList()
        this.checkDialogOpen = false
        ElMessage.success('结转完成')
      }).catch(() => {})
    },
    handleOpsAdjustStartTime(row) {
      this.opsAdjustRow = row
      this.opsAdjustForm = { startTime: '', endTime: '', reason: '' }
      this.opsAdjustOpen = true
    },
    submitOpsAdjustStartTime() {
      this.$refs.opsAdjustFormRef.validate(valid => {
        if (!valid) return
        this.opsAdjustSubmitting = true
        opsAdjustAccountingPeriodStartTime(this.opsAdjustRow.periodId, {
          startTime: this.opsAdjustForm.startTime,
          endTime: this.opsAdjustForm.endTime || null,
          reason: this.opsAdjustForm.reason
        }).then(() => {
          this.opsAdjustOpen = false
          this.getList()
          if (this.detailOpen && this.detail.periodId === this.opsAdjustRow.periodId) {
            this.handleDetail(this.opsAdjustRow)
          }
          ElMessage.success('起始时间已调整')
        }).catch(() => {
          // 后端错误信息由 request 拦截器展示，弹窗保留
        }).finally(() => {
          this.opsAdjustSubmitting = false
        })
      })
    }
  }
}
</script>

<style scoped>
.accounting-period-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.period-panel {
  border: 1px solid #e5e9f2;
  margin-bottom: 18px;
}

.period-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-title {
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 700;
  line-height: 26px;
}

.panel-subtitle {
  color: #7a869a;
  font-size: 13px;
  line-height: 22px;
  margin-top: 4px;
}

.period-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dept-select {
  width: 220px;
}

.period-summary {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  margin-top: 18px;
}

.status-box {
  border-radius: 8px;
  padding: 20px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 120px;
}

.status-0 {
  background: linear-gradient(135deg, #e6a23c 0%, #c8792a 100%);
}

.status-2 {
  background: linear-gradient(135deg, #606266 0%, #303133 100%);
}

.status-label {
  opacity: 0.85;
  font-size: 12px;
}

.status-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 36px;
}

.status-desc {
  font-size: 12px;
  line-height: 20px;
  opacity: 0.9;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.metric-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 120px;
}

.metric-card.emphasis {
  border-color: #a3d8ff;
  background: #f0f9ff;
}

.metric-label {
  color: #7a869a;
  font-size: 12px;
  line-height: 18px;
}

.metric-value {
  color: #303133;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.income,
.income-text {
  color: #2fb344;
}

.cost,
.cost-text {
  color: #f56c6c;
}

.period-list-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-top: 18px;
}

.query-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 12px 16px;
  padding-bottom: 14px;
  margin-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.query-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.query-actions {
  margin-left: auto;
}

.table-toolbar {
  min-height: 32px;
  margin-bottom: 12px;
}

.period-table {
  width: 100%;
}

.trial-result {
  padding: 10px 0;
}

.trial-detail {
  text-align: left;
  display: inline-block;
  margin-top: 8px;
}

.trial-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
  min-width: 400px;
}

.trial-label {
  color: #606266;
  font-size: 14px;
}

.trial-value {
  font-size: 16px;
  font-weight: 700;
}

@media (max-width: 1200px) {
  .period-summary {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

.check-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 16px;
}

.check-summary-success {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.check-summary-warning {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #faecd8;
}

.check-summary-block {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
}

.detail-hint {
  margin: 0 0 8px;
  padding: 8px 12px;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  background: #f4f9ff;
  color: #409eff;
  font-size: 13px;
}

.check-summary-icon {
  font-size: 20px;
}

.check-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.check-item {
  border-radius: 6px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
}

.check-item-block {
  background: #fef0f0;
  border-color: #fbc4c4;
}

.check-item-warning {
  background: #fdf6ec;
  border-color: #f5dab1;
}

.check-item-info {
  background: #f4f4f5;
  border-color: #d3d4d6;
}

.check-item-clean {
  background: #f0f9eb;
  border-color: #e1f3d8;
}

.check-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.check-item-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.check-item-count {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

.check-item-desc {
  font-size: 13px;
  color: #606266;
  line-height: 20px;
}

.force-carry-forward-section {
  margin-top: 12px;
}
</style>
