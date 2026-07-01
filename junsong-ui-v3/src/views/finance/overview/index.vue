<template>
  <div class="app-container report-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">财务管理概览</h2>
      </div>
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

    <!-- Top Metric Cards -->
    <div class="report-metrics">
      <div class="metric-card primary">
        <div class="metric-label">今日销售</div>
        <div class="metric-value">&yen;{{ dashboardData.todaySales || 0 }}</div>
      </div>
      <div class="metric-card primary">
        <div class="metric-label">本月销售</div>
        <div class="metric-value">&yen;{{ dashboardData.monthSales || 0 }}</div>
      </div>
      <div class="metric-card warning">
        <div class="metric-label">今日费用</div>
        <div class="metric-value">&yen;{{ dashboardData.todayExpense || 0 }}</div>
      </div>
      <div class="metric-card warning">
        <div class="metric-label">本月费用</div>
        <div class="metric-value">&yen;{{ dashboardData.monthExpense || 0 }}</div>
      </div>
      <div class="metric-card success">
        <div class="metric-label">毛利润</div>
        <div class="metric-value">&yen;{{ dashboardData.grossProfit || 0 }}</div>
      </div>
      <div class="metric-card success">
        <div class="metric-label">净利润</div>
        <div class="metric-value">&yen;{{ dashboardData.netProfit || 0 }}</div>
      </div>
      <div class="metric-card info">
        <div class="metric-label">利润率</div>
        <div class="metric-value">{{ dashboardData.profitRate || 0 }}%</div>
      </div>
    </div>

    <!-- Cash Health Section -->
    <el-card class="section-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>现金流健康</span>
          <el-tag v-if="cashflowData.cashPressureItems === 0" type="success" size="small">健康</el-tag>
          <el-tag v-else :type="cashflowData.cashPressureItems > 5 ? 'danger' : 'warning'" size="small">{{ cashflowData.cashPressureItems }} 项压力</el-tag>
        </div>
      </template>
      <div class="report-metrics">
        <div class="metric-card" :class="cashflowData.netCashInflow >= 0 ? 'success' : 'danger'">
          <div class="metric-label">净现金流入</div>
          <div class="metric-value" :style="{ color: cashflowData.netCashInflow >= 0 ? '#67C23A' : '#F56C6C' }">&yen;{{ cashflowData.netCashInflow || 0 }}</div>
        </div>
        <div class="metric-card" :class="cashflowData.totalUnverifiedExpense > 5000 ? 'warning' : 'info'">
          <div class="metric-label">未核销费用</div>
          <div class="metric-value">&yen;{{ cashflowData.totalUnverifiedExpense || 0 }}</div>
          <div v-if="cashflowData.totalUnverifiedExpense > 5000" class="metric-hint" style="color:#E6A23C;font-size:12px;margin-top:4px;">金额偏高，建议及时核销</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">借支余额</div>
          <div class="metric-value">&yen;{{ cashflowData.totalAdvanceBalance || 0 }}</div>
        </div>
        <div class="metric-card success">
          <div class="metric-label">已付投资人返款</div>
          <div class="metric-value">&yen;{{ cashflowData.totalPaidInvestorPayment || 0 }}</div>
        </div>
        <div class="metric-card warning">
          <div class="metric-label">未付投资人返款</div>
          <div class="metric-value">&yen;{{ cashflowData.totalUnpaidInvestorPayment || 0 }}</div>
        </div>
        <div class="metric-card" :class="cashflowData.cashPressureItems > 5 ? 'danger' : 'info'">
          <div class="metric-label">现金压力项</div>
          <div class="metric-value">{{ cashflowData.cashPressureItems || 0 }}</div>
        </div>
      </div>
    </el-card>

    <!-- Quick Links -->
    <el-card class="section-card">
      <template #header><span>快捷入口</span></template>
      <div class="quick-links">
        <router-link to="/finance/expense" class="quick-link">费用管理</router-link>
        <router-link to="/finance/costAccounting" class="quick-link">成本核算</router-link>
        <router-link to="/finance/profitShare" class="quick-link">分润结算</router-link>
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
          <template #default="scope">&yen;{{ scope.row.impactAmount || 0 }}</template>
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
            <span v-if="task.impactAmount" class="task-impact">影响: &yen;{{ task.impactAmount }}</span>
          </div>
          <div class="task-footer">
            <span class="task-action">{{ task.suggestedAction }}</span>
            <el-button v-if="task.targetRoute" type="primary" link size="small" @click="goTaskRoute(task)">去处理</el-button>
          </div>
        </div>
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

    <!-- Store Rankings -->
    <div class="chart-grid">
      <el-card class="chart-card">
        <template #header><span>门店销售排行</span></template>
        <el-table :data="dashboardData.salesTopStores || []" stripe border style="width: 100%" empty-text="暂无数据" max-height="320">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="deptName" label="门店" min-width="140" />
          <el-table-column prop="totalSales" label="销售额" min-width="120" />
          <el-table-column prop="orderCount" label="订单数" width="100" />
        </el-table>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>门店利润排行</span></template>
        <el-table :data="dashboardData.profitTopStores || []" stripe border style="width: 100%" empty-text="暂无数据" max-height="320">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="deptName" label="门店" min-width="140" />
          <el-table-column prop="netProfit" label="净利润" min-width="120" />
          <el-table-column prop="profitRate" label="利润率" width="100">
            <template #default="scope">{{ scope.row.profitRate || 0 }}%</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
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
        netCashInflow: 0,
        totalReceivedSalePayment: 0,
        totalVerifiedExpense: 0,
        totalUnverifiedExpense: 0,
        totalAdvanceBalance: 0,
        totalPaidInvestorPayment: 0,
        totalUnpaidInvestorPayment: 0,
        cashPressureItems: 0,
        deptIds: []
      }
    };
  },
  created() {
    this.getDepts();
  },
  mounted() {
    this.loadData();
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
        const [dashRes, alertsRes, tasksRes, cashflowRes] = await Promise.all([
          request({ url: "/finance/dashboard/operation", method: "post", data: this.queryParams }),
          request({ url: "/finance/dashboard/alerts", method: "post", data: this.queryParams }),
          request({ url: "/finance/dashboard/review-tasks", method: "post", data: this.queryParams }),
          request({ url: "/finance/dashboard/cashflow", method: "post", data: this.queryParams })
        ]);
        this.dashboardData = dashRes.data || this.dashboardData;
        this.alerts = alertsRes.data || [];
        this.reviewTasks = tasksRes.data || [];
        this.cashflowData = cashflowRes.data || this.cashflowData;
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
    resetQuery() {
      this.queryParams = {
        deptIds: [],
        timeType: "day"
      };
      this.loadData();
    }
  }
};
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 6px;
  color: #18202f;
  font-size: 22px;
  font-weight: 700;
}

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
