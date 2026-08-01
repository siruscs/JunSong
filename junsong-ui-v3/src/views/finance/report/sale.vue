<template>
  <div class="app-container report-page">
    <div class="report-filter-panel">
      <el-form :model="queryParams" ref="queryForm" label-position="top" class="report-query-form">
        <el-form-item label="门店">
          <el-select v-model="queryParams.deptIds" placeholder="请选择门店" multiple clearable collapse-tags collapse-tags-tooltip class="report-query-control">
            <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
          </el-select>
        </el-form-item>
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
        <div class="metric-label">总销售额</div>
        <div class="metric-value">{{ money(reportData.totalSales) }}</div>
      </div>
      <div class="metric-card success">
        <div class="metric-label">销售笔数</div>
        <div class="metric-value">{{ reportData.totalCount || 0 }}</div>
      </div>
      <div class="metric-card info">
        <div class="metric-label">客单价</div>
        <div class="metric-value">{{ money(reportData.avgPrice) }}</div>
      </div>
    </div>

    <!-- Original Charts -->
    <div class="chart-grid">
      <el-card class="chart-card">
        <template #header><span>门店销售对比</span></template>
        <div ref="deptChart" class="chart-canvas"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>销售排行</span></template>
        <div ref="rankChart" class="chart-canvas"></div>
      </el-card>
      <el-card class="chart-card wide">
        <template #header><span>销售趋势</span></template>
        <div ref="trendChart" class="chart-canvas"></div>
      </el-card>
    </div>

    <!-- Sales Operation Analysis Section -->
    <el-card class="section-card" style="margin-top: 16px;">
      <template #header><span>销售经营分析</span></template>

      <!-- Operation Metrics -->
      <div class="report-metrics">
        <div class="metric-card primary">
          <div class="metric-label">订单数</div>
          <div class="metric-value">{{ opData.orderCount || 0 }}</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">单均金额</div>
          <div class="metric-value">{{ money(opData.avgOrderAmount) }}</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">件均金额</div>
          <div class="metric-value">{{ money(opData.avgItemAmount) }}</div>
        </div>
      </div>
    </el-card>

    <!-- Member vs Non-member & Seckill -->
    <div class="chart-grid" style="margin-top: 16px;">
      <el-card class="chart-card">
        <template #header><span>会员 vs 非会员销售</span></template>
        <div ref="memberPieChart" class="chart-canvas"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>秒杀 vs 常规销售</span></template>
        <div ref="seckillChart" class="chart-canvas"></div>
      </el-card>
    </div>

    <!-- Member Contribution Summary Strip -->
    <el-card class="chart-card" style="margin-top: 16px;">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>会员贡献摘要</span>
          <router-link to="/member/report/member" style="font-size: 13px; font-weight: normal;">查看详情 &rarr;</router-link>
        </div>
      </template>
      <div class="member-summary-row">
        <div class="member-summary-item">
          <span class="member-summary-label">会员销售</span>
          <span class="member-summary-value">{{ money(opData.memberSales) }}</span>
        </div>
        <div class="member-summary-item">
          <span class="member-summary-label">非会员销售</span>
          <span class="member-summary-value">{{ money(opData.nonMemberSales) }}</span>
        </div>
        <div class="member-summary-item">
          <span class="member-summary-label">秒杀销售</span>
          <span class="member-summary-value">{{ money(opData.seckillSales) }}</span>
        </div>
        <div class="member-summary-item">
          <span class="member-summary-label">常规销售</span>
          <span class="member-summary-value">{{ money(opData.normalSales) }}</span>
        </div>
      </div>
    </el-card>

    <!-- Store & Product Rank Tables -->
    <div class="chart-grid" style="margin-top: 16px;">
      <el-card class="chart-card">
        <template #header><span>门店经营排名</span></template>
        <el-table :data="opData.storeRank || []" stripe border style="width: 100%" empty-text="暂无数据" max-height="360">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="deptName" label="门店" min-width="140" />
          <el-table-column label="销售额" min-width="120">
            <template #default="scope">{{ money(scope.row.totalSales) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单数" width="100" />
          <el-table-column label="客单价" width="100">
            <template #default="scope">{{ money(scope.row.avgOrderAmount) }}</template>
          </el-table-column>
          <el-table-column prop="memberSalesRatio" label="会员占比" width="100">
            <template #default="scope">{{ scope.row.memberSalesRatio || 0 }}%</template>
          </el-table-column>
        </el-table>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>商品销售排名</span></template>
        <el-table :data="opData.productRank || []" stripe border style="width: 100%" empty-text="暂无数据" max-height="360">
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column label="销售额" min-width="120">
            <template #default="scope">{{ money(scope.row.totalSales) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="销量" width="100" />
          <el-table-column label="均价" width="100">
            <template #default="scope">{{ money(scope.row.avgPrice) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script>
import request from "@/utils/request";
import { useUserStore } from "@/stores/user";
import { useSettingsStore } from "@/stores/settings";

const userStore = useUserStore();
const settingsStore = useSettingsStore();

export default {
  name: "SaleReport",
  data() {
    return {
      depts: [],
      queryParams: {
        deptIds: [],
        startTime: null,
        endTime: null,
        timeType: "day"
      },
      reportData: {
        totalSales: 0,
        totalCount: 0,
        avgPrice: 0,
        deptStats: [],
        rankStats: [],
        trendStats: []
      },
      opData: {
        orderCount: 0,
        avgOrderAmount: 0,
        avgItemAmount: 0,
        memberSales: 0,
        nonMemberSales: 0,
        seckillSales: 0,
        normalSales: 0,
        storeRank: [],
        productRank: []
      },
      deptChart: null,
      rankChart: null,
      trendChart: null,
      memberPieChart: null,
      seckillChart: null,
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
    this.deptChart && this.deptChart.dispose();
    this.rankChart && this.rankChart.dispose();
    this.trendChart && this.trendChart.dispose();
    this.memberPieChart && this.memberPieChart.dispose();
    this.seckillChart && this.seckillChart.dispose();
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
        url: "/finance/report/sale",
        method: "post",
        data: this.queryParams
      }).then(response => {
        this.reportData = response.data;
        this.$nextTick(() => {
          this.initDeptChart();
          this.initRankChart();
          this.initTrendChart();
        });
      });
      // Fetch operation data
      request({
        url: "/finance/report/sale/operation",
        method: "post",
        data: this.queryParams
      }).then(response => {
        this.opData = response.data || this.opData;
        this.$nextTick(() => {
          this.initMemberPieChart();
          this.initSeckillChart();
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
        timeType: "day"
      };
      this.handleQuery();
    },
    handleResize() {
      if (this.resizeTimer) {
        clearTimeout(this.resizeTimer);
      }
      this.resizeTimer = setTimeout(() => {
        this.deptChart && this.deptChart.resize();
        this.rankChart && this.rankChart.resize();
        this.trendChart && this.trendChart.resize();
        this.memberPieChart && this.memberPieChart.resize();
        this.seckillChart && this.seckillChart.resize();
      }, 200);
    },
    reRenderCharts() {
      this.initDeptChart();
      this.initRankChart();
      this.initTrendChart();
      this.initMemberPieChart();
      this.initSeckillChart();
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
    initDeptChart() {
      if (!this.$refs.deptChart) return;
      var echarts = require("echarts");
      if (this.deptChart) this.deptChart.dispose();
      this.deptChart = echarts.init(this.$refs.deptChart);
      var data = this.reportData.deptStats || [];
      this.deptChart.setOption(this.getBarOptions(data.map(function(item) { return item.deptName; }), data.map(function(item) { return item.totalSales; })));
    },
    initRankChart() {
      if (!this.$refs.rankChart) return;
      var echarts = require("echarts");
      if (this.rankChart) this.rankChart.dispose();
      this.rankChart = echarts.init(this.$refs.rankChart);
      var data = (this.reportData.rankStats || []).slice().reverse();
      this.rankChart.setOption(this.getBarOptions(data.map(function(item) { return item.deptName; }), data.map(function(item) { return item.totalSales; }), true));
    },
    initTrendChart() {
      if (!this.$refs.trendChart) return;
      var echarts = require("echarts");
      if (this.trendChart) this.trendChart.dispose();
      this.trendChart = echarts.init(this.$refs.trendChart);
      var data = this.reportData.trendStats || [];
      this.trendChart.setOption(this.getTrendLineOptions(data, "totalSales", "销售"));
    },
    initMemberPieChart() {
      if (!this.$refs.memberPieChart) return;
      var echarts = require("echarts");
      if (this.memberPieChart) this.memberPieChart.dispose();
      this.memberPieChart = echarts.init(this.$refs.memberPieChart);
      var d = this.opData;
      var pieData = [];
      if (d.memberSales || d.nonMemberSales) {
        pieData = [
          { name: "会员销售", value: d.memberSales || 0 },
          { name: "非会员销售", value: d.nonMemberSales || 0 }
        ];
      }
      this.memberPieChart.setOption(this.getPieOptions(pieData));
    },
    initSeckillChart() {
      if (!this.$refs.seckillChart) return;
      var echarts = require("echarts");
      if (this.seckillChart) this.seckillChart.dispose();
      this.seckillChart = echarts.init(this.$refs.seckillChart);
      var d = this.opData;
      var barData = [];
      if (d.seckillSales || d.normalSales) {
        barData = [
          { name: "秒杀销售", value: d.seckillSales || 0 },
          { name: "常规销售", value: d.normalSales || 0 }
        ];
      }
      this.seckillChart.setOption(this.getBarOptions(barData.map(function(i) { return i.name; }), barData.map(function(i) { return i.value; })));
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

.member-summary-row {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 8px 0;
}

.member-summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.member-summary-label {
  color: #909399;
  font-size: 13px;
  font-weight: 600;
}

.member-summary-value {
  color: var(--theme-primary-dark, #303133);
  font-size: 20px;
  font-weight: 800;
}
</style>
