<template>
  <div class="app-container report-page">
    <div class="page-head">
      <h2 class="page-title">财务管理概览</h2>
      <el-button type="primary" icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <el-result v-if="permissionDenied" icon="warning" title="暂无权限" sub-title="暂无权限查看该概览数据，请联系管理员开通相应权限。">
      <template #extra>
        <el-button type="primary" @click="loadData">重试</el-button>
      </template>
    </el-result>

    <template v-else>
    <el-alert v-if="loadError" :title="loadError" type="error" show-icon closable style="margin-bottom: 14px" @close="loadError = ''" />

    <div class="report-filter-panel">
      <el-form :model="queryParams" ref="queryForm" label-position="top" class="report-query-form">
        <el-form-item label="门店">
          <el-select v-model="queryParams.deptIds" placeholder="请选择门店" multiple clearable collapse-tags collapse-tags-tooltip class="report-query-control">
            <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间粒度">
          <el-select v-model="queryParams.timeType" placeholder="请选择" class="report-query-control">
            <el-option label="日" value="day" />
            <el-option label="周" value="week" />
            <el-option label="月" value="month" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作" class="report-query-actions">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="dashboard-intro">
      <div>
        <span class="eyebrow">经营驾驶舱 / {{ queryParams.timeType === 'day' ? '今日' : queryParams.timeType === 'week' ? '本周' : '本月' }}</span>
        <h3>经营结论</h3>
        <p>先看结果，再处理影响经营的事项。</p>
      </div>
      <div class="data-stamp">数据随查询条件更新</div>
    </div>

    <!-- Top Metric Cards -->
    <div class="report-metrics">
      <div class="metric-card primary metric-card-hero">
        <div class="metric-label">本期销售额</div>
        <div class="metric-value">{{ money(dashboardData.monthSales || dashboardData.todaySales) }}</div>
      </div>
      <div class="metric-card success metric-card-hero">
        <div class="metric-label">毛利润</div>
        <div class="metric-value">{{ money(dashboardData.grossProfit) }}</div>
      </div>
      <div class="metric-card success metric-card-hero">
        <div class="metric-label">毛利率</div>
        <div class="metric-value">{{ dashboardData.profitRate || 0 }}%</div>
      </div>
      <div class="metric-card info metric-card-hero">
        <div class="metric-label">现金净流入</div>
        <div class="metric-value">{{ money(cashflowData.netCashflowAmount) }}</div>
      </div>
      <div class="metric-card warning metric-card-hero">
        <div class="metric-label">期末应收余额</div>
        <div class="metric-value">{{ money(dashboardData.endingReceivableAmount) }}</div>
      </div>
    </div>

    <el-card class="section-card action-board">
      <template #header><span>风险待办</span><el-tag size="small" :type="pendingCount > 0 ? 'warning' : 'success'">{{ pendingCount > 0 ? `${pendingCount} 项待处理` : '当前健康' }}</el-tag></template>
      <div class="action-grid">
        <button class="action-item" type="button" @click="$router.push('/finance/expense')"><span class="action-icon warning">费</span><span><strong>{{ dashboardData.unverifiedExpenseCount || 0 }}</strong><small>费用待核销</small></span><span class="action-arrow">→</span></button>
        <button class="action-item" type="button" @click="$router.push('/finance/advance')"><span class="action-icon info">借</span><span><strong>{{ dashboardData.unverifiedAdvanceCount || 0 }}</strong><small>借支待核销</small></span><span class="action-arrow">→</span></button>
        <button class="action-item" type="button" @click="$router.push('/finance/report/sale')"><span class="action-icon danger">收</span><span><strong>{{ dashboardData.overdueReceivableCount || 0 }}</strong><small>逾期应收</small></span><span class="action-arrow">→</span></button>
        <button class="action-item" type="button" @click="$router.push('/finance/report/stock')"><span class="action-icon danger">库</span><span><strong>{{ dashboardData.stockAnomalyCount || 0 }}</strong><small>库存异常</small></span><span class="action-arrow">→</span></button>
        <button class="action-item" type="button" @click="$router.push('/finance/profitShare')"><span class="action-icon primary">润</span><span><strong>{{ dashboardData.unsettledProfitShareCount || 0 }}</strong><small>待结转分润</small></span><span class="action-arrow">→</span></button>
        <button class="action-item" type="button" @click="$router.push('/finance/accountingPeriod')"><span class="action-icon info">期</span><span><strong>{{ dashboardData.currentPeriodStatus || '正常' }}</strong><small>核算周期状态</small></span><span class="action-arrow">→</span></button>
      </div>
    </el-card>

    <!-- 现金流速览 Section (R7-D) -->
    <el-card class="section-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>现金流速览</span>
          <el-tag :type="cashflowData.netCashflowAmount >= 0 ? 'success' : 'danger'" size="small">
            {{ cashflowData.netCashflowAmount >= 0 ? '正流入' : '净流出' }}
          </el-tag>
        </div>
      </template>
      <div class="report-metrics">
        <div class="metric-card success">
          <div class="metric-label">现金流入</div>
          <div class="metric-value" style="color:#67C23A">{{ money(cashflowData.cashInAmount) }}</div>
        </div>
        <div class="metric-card warning">
          <div class="metric-label">现金流出</div>
          <div class="metric-value" style="color:#E6A23C">{{ money(cashflowData.cashOutAmount) }}</div>
        </div>
        <div class="metric-card" :class="cashflowData.netCashflowAmount >= 0 ? 'success' : 'danger'">
          <div class="metric-label">净现金流</div>
          <div class="metric-value" :style="{ color: cashflowData.netCashflowAmount >= 0 ? '#67C23A' : '#F56C6C' }">{{ money(cashflowData.netCashflowAmount) }}</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">未核销费用</div>
          <div class="metric-value">{{ money(cashflowData.pendingExpenseAmount) }}</div>
          <div class="metric-hint" style="color:#909399;font-size:12px;margin-top:4px;">{{ cashflowData.pendingExpenseCount || 0 }} 笔待核销</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">未核销借支</div>
          <div class="metric-value">{{ money(cashflowData.pendingAdvanceAmount) }}</div>
          <div class="metric-hint" style="color:#909399;font-size:12px;margin-top:4px;">{{ cashflowData.pendingAdvanceCount || 0 }} 笔待核销</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">待分润</div>
          <div class="metric-value">{{ money(cashflowData.pendingProfitShareAmount) }}</div>
          <div class="metric-hint" style="color:#909399;font-size:12px;margin-top:4px;">{{ cashflowData.pendingProfitShareCount || 0 }} 笔待分润</div>
        </div>
      </div>
      <!-- 待结算明细列表 -->
      <div v-if="cashflowData.pendingItems && cashflowData.pendingItems.length > 0" style="margin-top:14px;">
        <div style="font-weight:600;color:#606266;margin-bottom:8px;font-size:13px;">待结算明细</div>
        <el-table :data="cashflowData.pendingItems" stripe border style="width:100%" empty-text="暂无待结算" max-height="280">
          <el-table-column prop="type" label="类型" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.type === 'EXPENSE' ? 'warning' : scope.row.type === 'ADVANCE' ? 'info' : 'success'" size="small">
                {{ scope.row.type === 'EXPENSE' ? '待核销费用' : scope.row.type === 'ADVANCE' ? '待核销借支' : '待分润' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deptName" label="门店" min-width="120" />
          <el-table-column prop="amount" label="金额" min-width="100">
            <template #default="scope">{{ money(scope.row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="createDate" label="创建时间" min-width="160" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="goPendingRoute(scope.row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- R14-A: 应收速览 Section -->
    <el-card class="section-card receivable-section">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>应收速览</span>
          <el-tag :type="Number(dashboardData.overdueReceivableCount || 0) > 0 ? 'warning' : 'success'" size="small">
            {{ Number(dashboardData.overdueReceivableCount || 0) > 0 ? '需要跟进' : '暂无逾期' }}
          </el-tag>
        </div>
      </template>
      <div class="report-metrics">
        <div class="metric-card success">
          <div class="metric-label">本期实收</div>
          <div class="metric-value">{{ money(dashboardData.currentPeriodPaymentAmount) }}</div>
        </div>
        <div class="metric-card success">
          <div class="metric-label">历史欠款回收</div>
          <div class="metric-value">{{ money(dashboardData.historicalReceivableCollectedAmount) }}</div>
        </div>
        <div class="metric-card warning">
          <div class="metric-label">本期新增应收</div>
          <div class="metric-value">{{ money(dashboardData.currentPeriodNewReceivableAmount) }}</div>
        </div>
        <div class="metric-card danger">
          <div class="metric-label">期末应收余额</div>
          <div class="metric-value">{{ money(dashboardData.endingReceivableAmount) }}</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">逾期应收</div>
          <div class="metric-value">{{ dashboardData.overdueReceivableCount || 0 }} 笔</div>
        </div>
      </div>
    </el-card>

    <!-- R16: 现金流预测 Section -->
    <el-card class="section-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>现金流预测</span>
          <el-button type="primary" link size="small" @click="$router.push('/finance/cashflowForecast')">查看预测</el-button>
        </div>
      </template>
      <div class="report-metrics">
        <div class="metric-card primary">
          <div class="metric-label">未来7天预计回款</div>
          <div class="metric-value">{{ money(cashflowForecastData.forecast7dAmount) }}</div>
        </div>
        <div class="metric-card" :class="pressureMetricClass(cashflowForecastData.pressureLevel)">
          <div class="metric-label">现金压力指数</div>
          <div class="metric-value">{{ cashflowForecastData.pressureScore || 0 }}分 ({{ pressureLevelLabel(cashflowForecastData.pressureLevel) }})</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">本周逾期承诺</div>
          <div class="metric-value">{{ money(cashflowForecastData.weeklyOverduePromiseAmount) }}</div>
        </div>
      </div>
    </el-card>

    <!-- Alert Center -->
    <el-card class="section-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>经营预警中心</span>
          <el-tag v-if="alerts.length === 0" type="success" size="small">经营健康</el-tag>
          <el-tag v-else :type="alerts.some(a => a.alertLevel === 'HIGH') ? 'danger' : 'warning'" size="small">{{ alerts.length }} 条预警</el-tag>
        </div>
      </template>
      <div v-if="alerts.length === 0" class="healthy-state">
        <el-icon style="font-size:32px;color:#67C23A;"><CircleCheck /></el-icon>
        <p>当前经营状况健康，暂无预警</p>
      </div>
      <el-table v-else :data="alerts" stripe border style="width: 100%" empty-text="暂无预警">
        <el-table-column prop="alertLevel" label="级别" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.alertLevel === 'HIGH' ? 'danger' : scope.row.alertLevel === 'MEDIUM' ? 'warning' : 'info'" size="small">
              {{ scope.row.alertLevel === 'HIGH' ? '严重' : scope.row.alertLevel === 'MEDIUM' ? '警告' : '提示' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="预警" min-width="140" />
        <el-table-column prop="reason" label="原因" min-width="240" />
        <el-table-column prop="impactAmount" label="影响金额" width="120">
          <template #default="scope">{{ money(scope.row.impactAmount) }}</template>
        </el-table-column>
        <el-table-column prop="suggestedAction" label="建议操作" min-width="180" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.targetRoute" type="primary" link size="small" @click="goAlertRoute(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Review Tasks -->
    <el-card class="section-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>今日复盘任务</span>
          <el-tag v-if="reviewTasks.length === 0 || (reviewTasks.length === 1 && reviewTasks[0].taskId === 'HEALTHY')" type="success" size="small">全部正常</el-tag>
          <el-tag v-else :type="reviewTasks.some(t => t.priority === 'HIGH') ? 'danger' : 'warning'" size="small">{{ reviewTasks.filter(t => t.taskId !== 'HEALTHY').length }} 项待处理</el-tag>
        </div>
      </template>
      <div v-if="reviewTasks.length === 0" class="healthy-state">
        <el-icon style="font-size:32px;color:#67C23A;"><CircleCheck /></el-icon>
        <p>今日无复盘任务</p>
      </div>
      <div v-else-if="reviewTasks.length === 1 && reviewTasks[0].taskId === 'HEALTHY'" class="healthy-state">
        <el-icon style="font-size:32px;color:#67C23A;"><CircleCheck /></el-icon>
        <p>{{ reviewTasks[0].reason }}</p>
      </div>
      <div v-else class="task-list">
        <div v-for="task in reviewTasks.filter(t => t.taskId !== 'HEALTHY')" :key="task.taskId" class="task-item" :class="'task-' + (task.priority || 'LOW').toLowerCase()">
          <div class="task-header">
            <el-tag :type="task.priority === 'HIGH' ? 'danger' : task.priority === 'MEDIUM' ? 'warning' : 'info'" size="small" class="task-priority">
              {{ task.priority === 'HIGH' ? '紧急' : task.priority === 'MEDIUM' ? '重要' : '一般' }}
            </el-tag>
            <span class="task-title">{{ task.taskTitle }}</span>
          </div>
          <div class="task-body">
            <span class="task-reason">{{ task.reason }}</span>
            <span v-if="task.impactAmount" class="task-impact">影响: {{ money(task.impactAmount) }}</span>
          </div>
          <div class="task-footer">
            <span class="task-action">{{ task.suggestedAction }}</span>
            <el-button v-if="task.targetRoute" type="primary" link size="small" @click="goTaskRoute(task)">去处理</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- R12-G: 动作成效面板 -->
    <el-card class="section-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>动作成效</span>
          <el-tag v-if="effectSummary.evaluatedTaskCount > 0"
                  :type="effectSummary.averageEffectScore >= 60 ? 'success' : effectSummary.averageEffectScore >= 40 ? 'warning' : 'danger'"
                  size="small">
            均分 {{ effectSummary.averageEffectScore }}
          </el-tag>
          <el-tag v-else type="info" size="small">暂无评估</el-tag>
        </div>
      </template>
      <div v-if="effectSummary.evaluatedTaskCount > 0">
        <div class="report-metrics">
          <div class="metric-card primary">
            <div class="metric-label">已评估动作</div>
            <div class="metric-value">{{ effectSummary.evaluatedTaskCount }}</div>
          </div>
          <div class="metric-card" style="border-left-color:#67C23A;">
            <div class="metric-label">改善明显</div>
            <div class="metric-value" style="color:#67C23A;">{{ effectSummary.goodEffectCount }}</div>
          </div>
          <div class="metric-card" style="border-left-color:#E6A23C;">
            <div class="metric-label">观察中</div>
            <div class="metric-value" style="color:#E6A23C;">{{ effectSummary.watchEffectCount }}</div>
          </div>
          <div class="metric-card" style="border-left-color:#F56C6C;">
            <div class="metric-label">未改善</div>
            <div class="metric-value" style="color:#F56C6C;">{{ effectSummary.noImprovementCount }}</div>
          </div>
        </div>
        <div v-if="effectSummary.reopenCandidates && effectSummary.reopenCandidates.length > 0" style="margin-top:14px;">
          <div style="font-weight:600;color:#606266;margin-bottom:8px;font-size:13px;">待重开任务</div>
          <div v-for="rc in effectSummary.reopenCandidates" :key="rc.taskId" class="task-item" style="padding:8px 12px;border:1px solid #ebeef5;border-radius:6px;margin-bottom:6px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <div>
                <el-tag size="small" effect="plain">{{ rc.taskType }}</el-tag>
                <span style="margin-left:6px;font-weight:600;">{{ rc.title }}</span>
              </div>
              <span style="color:#909399;font-size:12px;">{{ rc.deptName }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="healthy-state">
        <p style="color:#909399;">暂无已评估的动作成效数据</p>
      </div>
    </el-card>

    <!-- Pending Items -->
    <el-card class="section-card">
      <template #header><span>待办事项</span></template>
      <div class="report-metrics">
        <div class="metric-card info">
          <div class="metric-label">未核销费用</div>
          <div class="metric-value">{{ dashboardData.unverifiedExpenseCount || 0 }}</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">未核销借支</div>
          <div class="metric-value">{{ dashboardData.unverifiedAdvanceCount || 0 }}</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">未结转分润</div>
          <div class="metric-value">{{ dashboardData.unsettledProfitShareCount || 0 }}</div>
        </div>
        <div class="metric-card primary">
          <div class="metric-label">当前期间状态</div>
          <div class="metric-value" style="font-size: 16px;">{{ dashboardData.currentPeriodStatus || '暂无数据' }}</div>
        </div>
      </div>
    </el-card>

    <!-- Daily Settlement & Period Lock -->
    <el-card class="section-card">
      <template #header><span>日结闭环与期间锁账</span></template>
      <div class="report-metrics">
        <div class="metric-card success">
          <div class="metric-label">日结闭环状态</div>
          <div class="metric-value" style="font-size: 16px;">{{ dashboardData.dailySettlementStatus || '正常' }}</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">期间锁账状态</div>
          <div class="metric-value" style="font-size: 16px;">{{ dashboardData.currentPeriodStatus || '未锁定' }}</div>
        </div>
      </div>
    </el-card>

    <div class="section-kicker">趋势与排行</div>
    <!-- Store Rankings -->
    <div class="chart-grid">
      <el-card class="chart-card">
        <template #header><span>门店销售排行</span></template>
        <el-table :data="dashboardData.salesTopStores || []" stripe border style="width: 100%" empty-text="暂无数据" max-height="320">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="deptName" label="门店" min-width="140" />
          <el-table-column label="销售额" min-width="120">
            <template #default="scope">{{ money(scope.row.totalSales) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单数" width="100" />
        </el-table>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>门店利润排行</span></template>
        <el-table :data="dashboardData.profitTopStores || []" stripe border style="width: 100%" empty-text="暂无数据" max-height="320">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="deptName" label="门店" min-width="140" />
          <el-table-column label="净利润" min-width="120">
            <template #default="scope">{{ money(scope.row.netProfit) }}</template>
          </el-table-column>
          <el-table-column prop="profitRate" label="利润率" width="100">
            <template #default="scope">{{ scope.row.profitRate || 0 }}%</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- Report Workbench -->
    <el-card class="section-card report-workbench">
      <template #header><div class="section-heading"><span>报表工作台</span><small>从经营结论进入专题分析</small></div></template>
      <div class="report-links">
        <router-link to="/finance/report/sale" class="report-link"><strong>销售经营分析</strong><span>销售额、订单、会员与门店排行</span><em>→</em></router-link>
        <router-link to="/finance/report/profit" class="report-link"><strong>利润分析</strong><span>毛利、净利与利润钻取</span><em>→</em></router-link>
        <router-link to="/finance/report/expense" class="report-link"><strong>费用异常</strong><span>费用趋势与异常明细</span><em>→</em></router-link>
        <router-link to="/finance/report/profitShare" class="report-link"><strong>分润结算</strong><span>分润结转与结算看板</span><em>→</em></router-link>
        <router-link to="/finance/report/stock" class="report-link"><strong>库存价值与对账</strong><span>库存金额、流水与异常对账</span><em>→</em></router-link>
        <router-link to="/finance/report/store" class="report-link"><strong>门店经营分析</strong><span>授权门店健康与趋势</span><em>→</em></router-link>
      </div>
    </el-card>
    </template>
  </div>
</template>

<script>
import request from "@/utils/request";
import { useUserStore } from "@/stores/user";
import { CircleCheck } from "@element-plus/icons-vue";

const userStore = useUserStore();

export default {
  name: "FinanceOverview",
  components: { CircleCheck },
  data() {
    return {
      loading: false,
      loadError: '',
      permissionDenied: false,
      depts: [],
      alerts: [],
      reviewTasks: [],
      queryParams: {
        deptIds: [],
        timeType: "day"
      },
      dashboardData: {
        todaySales: 0,
        monthSales: 0,
        todayExpense: 0,
        monthExpense: 0,
        grossProfit: 0,
        netProfit: 0,
        profitRate: 0,
        warnings: [],
        unverifiedExpenseCount: 0,
        unverifiedAdvanceCount: 0,
        unsettledProfitShareCount: 0,
        currentPeriodStatus: "",
        dailySettlementStatus: "",
        salesTopStores: [],
        profitTopStores: []
      },
      cashflowData: {
        cashInAmount: 0,
        cashOutAmount: 0,
        netCashflowAmount: 0,
        pendingExpenseAmount: 0,
        pendingAdvanceAmount: 0,
        pendingProfitShareAmount: 0,
        pendingExpenseCount: 0,
        pendingAdvanceCount: 0,
        pendingProfitShareCount: 0,
        trendRows: [],
        pendingItems: []
      },
      effectSummary: {
        evaluatedTaskCount: 0,
        goodEffectCount: 0,
        watchEffectCount: 0,
        noImprovementCount: 0,
        averageEffectScore: 0,
        reopenCandidates: []
      },
      cashflowForecastData: {
        forecast7dAmount: 0,
        pressureScore: 0,
        pressureLevel: 'LOW',
        weeklyOverduePromiseAmount: 0
      }
    };
  },
  created() {
    this.getDepts();
  },
  mounted() {
    this.loadData();
  },
  computed: {
    pendingCount() {
      return Number(this.dashboardData.unverifiedExpenseCount || 0)
        + Number(this.dashboardData.unverifiedAdvanceCount || 0)
        + Number(this.dashboardData.overdueReceivableCount || 0)
        + Number(this.dashboardData.stockAnomalyCount || 0)
        + Number(this.dashboardData.unsettledProfitShareCount || 0);
    }
  },
  methods: {
    getDepts() {
      this.depts = (userStore.depts || []).map(dept => ({
        id: dept.deptId,
        label: dept.deptName
      }));
    },
    async loadData() {
      this.loading = true;
      this.loadError = '';
      this.permissionDenied = false;
      try {
        const [dashRes, alertsRes, tasksRes, cashflowRes, effectRes, forecastRes] = await Promise.all([
          request({ url: "/finance/dashboard/operation", method: "post", data: this.queryParams }),
          request({ url: "/finance/dashboard/alerts", method: "post", data: this.queryParams }),
          request({ url: "/finance/dashboard/review-tasks", method: "post", data: this.queryParams }),
          request({ url: "/finance/cashflow/dashboard", method: "post", data: this.queryParams }),
          request({ url: "/finance/review-task/effect-summary", method: "get", params: { windowDays: 7 } }),
          request({ url: "/finance/cashflow-forecast/dashboard", method: "post", data: this.queryParams })
        ]);
        this.dashboardData = dashRes.data || this.dashboardData;
        this.alerts = alertsRes.data || [];
        this.reviewTasks = tasksRes.data || [];
        this.cashflowData = cashflowRes.data || this.cashflowData;
        this.effectSummary = effectRes.data || this.effectSummary;
        this.cashflowForecastData = this.mapCashflowForecast(forecastRes.data);
      } catch (e) {
        if (e?.response?.status === 403 || e?.message?.includes('403')) {
          this.permissionDenied = true;
        } else {
          this.loadError = e?.message || '加载概览数据失败，请稍后重试';
        }
      } finally {
        this.loading = false;
      }
    },
    handleQuery() {
      this.loadData();
    },
    goAlertRoute(alert) {
      if (alert.targetRoute) {
        this.$router.push({ path: alert.targetRoute, query: alert.targetParams ? JSON.parse(alert.targetParams) : {} });
      }
    },
    goTaskRoute(task) {
      if (task.targetRoute) {
        this.$router.push({ path: task.targetRoute, query: task.targetParams ? JSON.parse(task.targetParams) : {} });
      }
    },
    goPendingRoute(item) {
      const routeMap = {
        EXPENSE: '/finance/expense',
        ADVANCE: '/finance/advance',
        PROFIT_SHARE: '/finance/profitShare'
      };
      const path = routeMap[item.type];
      if (path) {
        this.$router.push({ path, query: { id: item.bizId } });
      }
    },
    resetQuery() {
      this.queryParams = {
        deptIds: [],
        timeType: "day"
      };
      this.loadData();
    },
    mapCashflowForecast(data) {
      if (!data) return this.cashflowForecastData;
      const windows = data.windows || [];
      const window7d = windows.find((w) => w.windowDays === 7) || {};
      const pressure = data.pressure || {};
      const rhythm = data.weeklyRhythm || {};
      return {
        forecast7dAmount: window7d.forecastReceivableAmount || 0,
        pressureScore: pressure.pressureScore || 0,
        pressureLevel: pressure.pressureLevel || 'LOW',
        weeklyOverduePromiseAmount: rhythm.weeklyOverduePromiseAmount || 0
      };
    },
    pressureLevelLabel(level) {
      const labels = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' };
      return labels[level] || '低';
    },
    pressureMetricClass(level) {
      if (level === 'CRITICAL' || level === 'HIGH') return 'danger';
      if (level === 'MEDIUM') return 'warning';
      return 'success';
    }
  }
};
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 20px 2px;
  margin-bottom: 0;
}

.page-title {
  margin: 0;
  color: #18202f;
  font-size: 20px;
  font-weight: 700;
}

.dashboard-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 20px 14px;
}

.eyebrow, .section-kicker {
  color: #7a8497;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.dashboard-intro h3 {
  margin: 5px 0 2px;
  color: #18202f;
  font-size: 20px;
}

.dashboard-intro p, .data-stamp {
  margin: 0;
  color: #8a94a6;
  font-size: 12px;
}

.data-stamp { padding-bottom: 3px; }

.metric-card-hero { min-height: 104px; }
.metric-card-hero .metric-value { font-variant-numeric: tabular-nums; }

.action-board :deep(.el-card__header), .section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 11px 12px;
  text-align: left;
  cursor: pointer;
  background: #fbfcfe;
  border: 1px solid #e8edf4;
  border-radius: 7px;
  transition: border-color .2s, transform .2s, box-shadow .2s;
}

.action-item:hover, .action-item:focus-visible {
  border-color: #9eb9da;
  box-shadow: 0 4px 12px rgba(31, 78, 121, .08);
  outline: none;
  transform: translateY(-1px);
}

.action-icon {
  display: grid;
  flex: 0 0 28px;
  place-items: center;
  width: 28px;
  height: 28px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border-radius: 6px;
}
.action-icon.primary { background: #3b74b9; }
.action-icon.warning { background: #c9923e; }
.action-icon.danger { background: #c75a5a; }
.action-icon.info { background: #718198; }
.action-item strong, .action-item small { display: block; }
.action-item strong { color: #202a3a; font-size: 15px; font-variant-numeric: tabular-nums; }
.action-item small { margin-top: 2px; color: #7d8798; font-size: 12px; }
.action-arrow { margin-left: auto; color: #9aa6b7; }
.section-kicker { margin: 24px 0 10px; }
.section-heading small { color: #8a94a6; font-size: 12px; font-weight: 400; }

.report-links {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.report-link {
  position: relative;
  display: flex;
  min-height: 82px;
  flex-direction: column;
  justify-content: center;
  padding: 14px 38px 14px 15px;
  color: inherit;
  text-decoration: none;
  background: #f8fafc;
  border: 1px solid #e6ebf2;
  border-radius: 7px;
  transition: border-color .2s, background .2s, transform .2s;
}

.report-link:hover, .report-link:focus-visible {
  background: #f2f7fd;
  border-color: #9eb9da;
  outline: none;
  transform: translateY(-1px);
}
.report-link strong { color: #263449; font-size: 14px; }
.report-link span { margin-top: 5px; color: #8590a1; font-size: 12px; line-height: 1.4; }
.report-link em { position: absolute; right: 15px; top: 50%; color: #7090b5; font-size: 18px; font-style: normal; transform: translateY(-50%); }

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.quick-link {
  display: inline-block;
  padding: 8px 20px;
  color: var(--theme-primary, #409EFF);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  background: rgba(var(--theme-primary-rgb, 64, 158, 255), 0.08);
  border-radius: 6px;
  transition: background 0.2s;
}

.quick-link:hover {
  background: rgba(var(--theme-primary-rgb, 64, 158, 255), 0.16);
}

.report-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.report-filter-panel {
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.report-query-form {
  display: grid;
  grid-template-columns: minmax(260px, 1.4fr) minmax(180px, 0.8fr) auto;
  gap: 14px 18px;
  align-items: end;
}

.report-query-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.report-query-form :deep(.el-form-item__label) {
  justify-content: flex-start;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 600;
  line-height: 1.2;
}

.report-query-control {
  width: 100%;
}

.report-query-actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
}

@media (max-width: 768px) {
  .report-query-form {
    grid-template-columns: 1fr;
  }
}

.report-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.metric-card {
  position: relative;
  min-height: 88px;
  padding: 14px 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(var(--theme-primary-rgb), 0.14);
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(var(--theme-primary-rgb), 0.06);
}

.metric-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 4px;
  border-radius: 0 8px 8px 0;
  background: var(--theme-primary);
}

.metric-card.warning::before {
  background: #E6A23C;
}

.metric-card.danger::before {
  background: #F56C6C;
}

.metric-card.success::before {
  background: #67C23A;
}

.metric-card.info::before {
  background: #909399;
}

.metric-card::after {
  content: "";
  position: absolute;
  right: -34px;
  top: -48px;
  width: 116px;
  height: 116px;
  border-radius: 999px;
  background: rgba(var(--theme-primary-rgb), 0.08);
}

.metric-label {
  position: relative;
  z-index: 1;
  color: #606266;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
}

.metric-value {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  color: var(--theme-primary-dark);
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.section-card {
  margin-bottom: 16px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-shadow: 0 8px 24px rgba(24, 39, 75, 0.04);
}

.section-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid #edf0f5;
  color: #303133;
  font-weight: 700;
}

.section-card :deep(.el-card__body) {
  padding: 14px 16px 18px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-shadow: 0 8px 24px rgba(24, 39, 75, 0.04);
}

.chart-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid #edf0f5;
  color: #303133;
  font-weight: 700;
}

.chart-card :deep(.el-card__body) {
  padding: 14px 16px 18px;
}

@media (max-width: 768px) {
  .dashboard-intro { align-items: flex-start; flex-direction: column; }
  .action-grid, .report-links { grid-template-columns: 1fr; }
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

.healthy-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: #909399;
  font-size: 14px;
}

.healthy-state p {
  margin: 12px 0 0;
  color: #606266;
  font-weight: 600;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-item {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-left: 4px solid #909399;
  border-radius: 6px;
  background: #fafbfc;
  transition: box-shadow 0.2s;
}

.task-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.task-item.task-high {
  border-left-color: #F56C6C;
  background: #fef0f0;
}

.task-item.task-medium {
  border-left-color: #E6A23C;
  background: #fdf6ec;
}

.task-item.task-low {
  border-left-color: #909399;
}

.task-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.task-title {
  font-weight: 700;
  font-size: 14px;
  color: #303133;
}

.task-body {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 6px;
}

.task-reason {
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.task-impact {
  color: #F56C6C;
  font-size: 13px;
  font-weight: 600;
}

.task-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-action {
  color: #909399;
  font-size: 12px;
}
</style>
