<template>
  <div class="app-container report-page">
    <div class="report-filter-panel">
      <el-form :model="queryParams" ref="queryForm" label-position="top" class="report-query-form">
        <el-form-item label="门店">
          <el-select v-model="queryParams.deptIds" placeholder="请选择门店" multiple clearable collapse-tags collapse-tags-tooltip class="report-query-control">
            <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
          </el-select>
        </el-form-item>
        <AccountingPeriodFilter v-model="queryParams.periodId" />
        <el-form-item label="开始日期">
          <el-date-picker v-model="queryParams.startTime" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" class="report-query-control" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="queryParams.endTime" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" class="report-query-control" />
        </el-form-item>
        <el-form-item label="操作" class="report-query-actions">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Original Metrics -->
    <div class="report-metrics">
      <div class="metric-card primary">
        <div class="metric-label">总分润</div>
        <div class="metric-value">{{ money(reportData.totalProfitShare) }}</div>
      </div>
      <div class="metric-card success">
        <div class="metric-label">店长分润</div>
        <div class="metric-value">{{ money(reportData.totalManagerProfit) }}</div>
      </div>
      <div class="metric-card info">
        <div class="metric-label">投资人分润</div>
        <div class="metric-value">{{ money(reportData.totalInvestorProfit) }}</div>
      </div>
    </div>

    <!-- Original Charts -->
    <div class="chart-grid">
      <el-card class="chart-card wide">
        <template #header><span>分润趋势</span></template>
        <div ref="trendChart" class="chart-canvas"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>店长分润统计</span></template>
        <div ref="managerChart" class="chart-canvas"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>投资人分润统计</span></template>
        <div ref="investorChart" class="chart-canvas"></div>
      </el-card>
    </div>

    <!-- Settlement Dashboard Section -->
    <el-card class="section-card" style="margin-top: 16px;">
      <template #header><span>结转看板</span></template>

      <!-- Settlement Metrics -->
      <div class="report-metrics">
        <div class="metric-card primary">
          <div class="metric-label">应付金额</div>
          <div class="metric-value">{{ money(settlementData.payableAmount) }}</div>
        </div>
        <div class="metric-card success">
          <div class="metric-label">已付金额</div>
          <div class="metric-value">{{ money(settlementData.paidAmount) }}</div>
        </div>
        <div class="metric-card warning">
          <div class="metric-label">待付金额</div>
          <div class="metric-value">{{ money(settlementData.pendingAmount) }}</div>
        </div>
      </div>
    </el-card>

    <!-- Manager vs Investor Pie & Dept Settlement Table -->
    <div class="chart-grid" style="margin-top: 16px;">
      <el-card class="chart-card">
        <template #header><span>店长 vs 投资人分润占比</span></template>
        <div ref="sharePieChart" class="chart-canvas"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>门店结转明细</span></template>
        <el-table :data="settlementData.deptSettlements || []" stripe border style="width: 100%" empty-text="暂无数据" max-height="360">
          <el-table-column prop="deptName" label="门店" min-width="120" />
          <el-table-column label="净利润" min-width="110">
            <template #default="scope">{{ money(scope.row.netProfit) }}</template>
          </el-table-column>
          <el-table-column label="店长分润" min-width="110">
            <template #default="scope">{{ money(scope.row.managerShare) }}</template>
          </el-table-column>
          <el-table-column label="投资人分润" min-width="110">
            <template #default="scope">{{ money(scope.row.investorShare) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'settled' ? 'success' : scope.row.status === 'pending' ? 'warning' : 'info'" size="small">
                {{ scope.row.status === 'settled' ? '已结转' : scope.row.status === 'pending' ? '待结转' : (scope.row.status || '未知') }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- Exception List -->
    <el-card class="section-card" style="margin-top: 16px;" v-if="settlementData.exceptions && settlementData.exceptions.length">
      <template #header><span>异常清单</span></template>
      <el-table :data="settlementData.exceptions || []" stripe border style="width: 100%" empty-text="暂无异常">
        <el-table-column prop="deptName" label="门店" min-width="120" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="message" label="说明" min-width="200" />
        <el-table-column label="涉及金额" width="120">
          <template #default="scope">{{ money(scope.row.amount) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from "@/utils/request";
import AccountingPeriodFilter from "@/components/finance/AccountingPeriodFilter.vue";
import { useUserStore } from "@/stores/user";
import { useSettingsStore } from "@/stores/settings";

const userStore = useUserStore();
const settingsStore = useSettingsStore();

export default {
  name: "ProfitShareReport",
  components: { AccountingPeriodFilter },
  data() {
    return {
      depts: [],
      queryParams: {
        deptIds: [],
        startTime: null,
        endTime: null,
        periodId: null,
        timeType: "day"
      },
      reportData: {
        totalProfitShare: 0,
        totalManagerProfit: 0,
        totalInvestorProfit: 0,
        managerStats: [],
        investorStats: [],
        trendStats: []
      },
      settlementData: {
        payableAmount: 0,
        paidAmount: 0,
        pendingAmount: 0,
        deptSettlements: [],
        exceptions: []
      },
      trendChart: null,
      managerChart: null,
      investorChart: null,
      sharePieChart: null,
      resizeTimer: null,
      themeUnsubscribe: null
    };
  },
  created() {
    this.getDepts();
  },
  mounted() {
    this.handleQuery();
    window.addEventListener("resize", this.handleResize);
    this.themeUnsubscribe = settingsStore.$subscribe((mutation, state) => {
      void state.themePreset;
      this.$nextTick(() => this.reRenderCharts());
    });
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.handleResize);
    if (this.themeUnsubscribe) { this.themeUnsubscribe(); this.themeUnsubscribe = null; }
    this.trendChart && this.trendChart.dispose();
    this.managerChart && this.managerChart.dispose();
    this.investorChart && this.investorChart.dispose();
    this.sharePieChart && this.sharePieChart.dispose();
  },
  methods: {
    getDepts() {
      this.depts = (userStore.depts || []).map(dept => ({
        id: dept.deptId,
        label: dept.deptName
      }));
    },
    handleQuery() {
      request({
        url: "/finance/report/profitShare",
        method: "post",
        data: this.queryParams
      }).then(response => {
        this.reportData = response.data;
        this.$nextTick(() => {
          this.initTrendChart();
          this.initManagerChart();
          this.initInvestorChart();
        });
      });
      // Fetch settlement data
      request({
        url: "/finance/report/profitShare/settlement",
        method: "post",
        data: this.queryParams
      }).then(response => {
        this.settlementData = response.data || this.settlementData;
        this.$nextTick(() => {
          this.initSharePieChart();
        });
      }).catch(() => {
        // keep defaults
      });
    },
    resetQuery() {
      this.queryParams = {
        deptIds: [],
        startTime: null,
        endTime: null,
        periodId: null,
        timeType: "day"
      };
      this.handleQuery();
    },
    handleResize() {
      if (this.resizeTimer) {
        clearTimeout(this.resizeTimer);
      }
      this.resizeTimer = setTimeout(() => {
        this.trendChart && this.trendChart.resize();
        this.managerChart && this.managerChart.resize();
        this.investorChart && this.investorChart.resize();
        this.sharePieChart && this.sharePieChart.resize();
      }, 200);
    },
    reRenderCharts() {
      this.initTrendChart();
      this.initManagerChart();
      this.initInvestorChart();
      this.initSharePieChart();
    },
    getCssVar(name, fallback) {
      const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
      return value || fallback;
    },
    getThemeColor() {
      return this.getCssVar("--theme-primary", "#409EFF");
    },
    getThemeDarkColor() {
      return this.getCssVar("--theme-primary-dark", this.getThemeColor());
    },
    getThemeRgb() {
      return this.getCssVar("--theme-primary-rgb", "64, 158, 255");
    },
    getThemeRgba(alpha) {
      return `rgba(${this.getThemeRgb()}, ${alpha})`;
    },
    getPalette() {
      return [this.getThemeColor(), this.getThemeRgba(0.72), "#67C23A", "#E6A23C", "#909399", "#F56C6C"];
    },
    getEmptyGraphic(data) {
      return data && data.length ? null : {
        type: "text",
        left: "center",
        top: "middle",
        style: {
          text: "暂无数据",
          fill: "#909399",
          fontSize: 14
        }
      };
    },
    getAxisBase() {
      return {
        axisLine: { lineStyle: { color: "#dcdfe6" } },
        axisTick: { show: false },
        axisLabel: { color: "#606266" },
        splitLine: { lineStyle: { color: "#edf0f5", type: "dashed" } }
      };
    },
    getBarOptions(labels, values, horizontal) {
      var h = horizontal || false;
      var data = values || [];
      var categoryAxis = Object.assign({}, this.getAxisBase(), {
        type: "category",
        data: labels,
        axisLabel: { color: "#606266", interval: 0, rotate: h ? 0 : 20 }
      });
      var valueAxis = Object.assign({}, this.getAxisBase(), {
        type: "value",
        splitLine: { lineStyle: { color: "#edf0f5", type: "dashed" } }
      });
      return {
        color: this.getPalette(),
        tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
        grid: { left: 34, right: 22, top: 28, bottom: h ? 24 : 58, containLabel: true },
        graphic: this.getEmptyGraphic(data),
        xAxis: h ? valueAxis : categoryAxis,
        yAxis: h ? categoryAxis : valueAxis,
        series: [{
          type: "bar",
          barMaxWidth: 34,
          data: data,
          itemStyle: { color: this.getThemeColor(), borderRadius: h ? [0, 6, 6, 0] : [6, 6, 0, 0] }
        }]
      };
    },
    getTrendLineOptions(data, valueKey, labelSuffix) {
      var rows = data || [];
      var labels = Array.from(new Set(rows.map(function(item) { return item.dateStr; }))).sort();
      var deptMap = new Map();
      rows.forEach(function(item) {
        var deptKey = item.deptId || "unknown";
        if (!deptMap.has(deptKey)) {
          deptMap.set(deptKey, { deptName: item.deptName || "未知门店", items: new Map() });
        }
        deptMap.get(deptKey).items.set(item.dateStr, item);
      });
      var palette = this.getPalette();
      var self = this;
      var series = Array.from(deptMap.values()).map(function(dept, index) {
        var color = palette[index % palette.length];
        return {
          name: dept.deptName + "-" + labelSuffix,
          type: "line",
          smooth: true,
          symbol: "circle",
          symbolSize: 7,
          data: labels.map(function(date) { return Number((dept.items.get(date) || {})[valueKey] || 0); }),
          lineStyle: { width: 3, color: color },
          itemStyle: { color: color, borderWidth: 2, borderColor: "#fff" },
          areaStyle: { color: index === 0 ? self.getThemeRgba(0.1) : undefined }
        };
      });
      return {
        color: palette,
        tooltip: { trigger: "axis" },
        legend: { top: 0, left: 16, right: 16, type: "scroll", icon: "circle", textStyle: { color: "#606266" } },
        grid: { left: 34, right: 22, top: 70, bottom: 36, containLabel: true },
        graphic: this.getEmptyGraphic(rows),
        xAxis: Object.assign({}, this.getAxisBase(), {
          type: "category",
          boundaryGap: false,
          data: labels,
          axisLabel: { color: "#606266" }
        }),
        yAxis: Object.assign({}, this.getAxisBase(), {
          type: "value",
          splitLine: { lineStyle: { color: "#edf0f5", type: "dashed" } }
        }),
        series: series
      };
    },
    getPieOptions(data) {
      return {
        color: this.getPalette(),
        tooltip: { trigger: "item" },
        legend: { bottom: 0, left: "center", icon: "circle", textStyle: { color: "#606266" } },
        graphic: this.getEmptyGraphic(data),
        series: [{
          type: "pie",
          radius: ["46%", "68%"],
          center: ["50%", "44%"],
          avoidLabelOverlap: true,
          label: { color: "#606266", formatter: "{b}\n{d}%" },
          labelLine: { smooth: true, lineStyle: { color: "#c0c4cc" } },
          itemStyle: { borderColor: "#fff", borderWidth: 2 },
          data: data
        }]
      };
    },
    initTrendChart() {
      if (!this.$refs.trendChart) return;
      var echarts = require("echarts");
      if (this.trendChart) this.trendChart.dispose();
      this.trendChart = echarts.init(this.$refs.trendChart);
      this.trendChart.setOption(this.getTrendLineOptions(this.reportData.trendStats || [], "totalAmount", "分润"));
    },
    initManagerChart() {
      if (!this.$refs.managerChart) return;
      var echarts = require("echarts");
      if (this.managerChart) this.managerChart.dispose();
      this.managerChart = echarts.init(this.$refs.managerChart);
      var data = this.reportData.managerStats || [];
      this.managerChart.setOption(this.getBarOptions(data.map(function(item) { return item.deptName || item.dateStr; }), data.map(function(item) { return item.amount; })));
    },
    initInvestorChart() {
      if (!this.$refs.investorChart) return;
      var echarts = require("echarts");
      if (this.investorChart) this.investorChart.dispose();
      this.investorChart = echarts.init(this.$refs.investorChart);
      var data = this.reportData.investorStats || [];
      this.investorChart.setOption(this.getBarOptions(data.map(function(item) { return item.deptName || item.dateStr; }), data.map(function(item) { return item.amount; })));
    },
    initSharePieChart() {
      if (!this.$refs.sharePieChart) return;
      var echarts = require("echarts");
      if (this.sharePieChart) this.sharePieChart.dispose();
      this.sharePieChart = echarts.init(this.$refs.sharePieChart);
      var d = this.settlementData;
      var pieData = [];
      if (d.paidAmount || d.pendingAmount) {
        pieData = [
          { name: "店长分润", value: Number(d.paidAmount || 0) },
          { name: "投资人分润", value: Number(d.pendingAmount || 0) }
        ];
      }
      this.sharePieChart.setOption(this.getPieOptions(pieData));
    }
  }
};
</script>

<style scoped>
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
  grid-template-columns: minmax(260px, 1.4fr) minmax(220px, 1fr) minmax(220px, 1fr) auto;
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

@media (max-width: 1100px) {
  .report-query-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .report-query-form {
    grid-template-columns: 1fr;
  }
}

.report-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
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

.metric-card.success::before {
  background: #67C23A;
}

.metric-card.warning::before {
  background: #E6A23C;
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

.chart-card.wide {
  grid-column: 1 / -1;
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

.chart-canvas {
  width: 100%;
  height: 360px;
}

.chart-card.wide .chart-canvas {
  height: 380px;
}

@media (max-width: 768px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.wide {
    grid-column: auto;
  }

  .chart-canvas,
  .chart-card.wide .chart-canvas {
    height: 320px;
  }
}
</style>
