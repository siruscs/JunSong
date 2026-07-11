<template>
  <div class="app-container report-page">
    <el-tabs v-model="activeTab" class="report-tabs">
      <el-tab-pane label="单店复盘" name="single">
        <div class="report-filter-panel">
          <el-form :model="queryParams" ref="queryForm" label-position="top" class="report-query-form">
            <el-form-item label="门店">
              <el-select v-model="queryParams.deptId" placeholder="请选择门店" clearable class="report-query-control">
                <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="queryParams.startTime" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" class="report-query-control" />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker v-model="queryParams.endTime" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" class="report-query-control" />
            </el-form-item>
            <el-form-item label="时间粒度">
              <el-select v-model="queryParams.timeType" class="report-query-control">
                <el-option label="按天" value="day" />
                <el-option label="按周" value="week" />
                <el-option label="按月" value="month" />
              </el-select>
            </el-form-item>
            <el-form-item label="操作" class="report-query-actions">
              <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="!hasDept" class="empty-state">
          <el-empty description="请选择门店后查看经营报表" />
        </div>

        <template v-else>
          <div class="report-metrics">
            <div class="metric-card primary"><div class="metric-label">销售额</div><div class="metric-value">&yen;{{ formatNum(reportData.totalSales) }}</div></div>
            <div class="metric-card warning"><div class="metric-label">费用总额</div><div class="metric-value">&yen;{{ formatNum(reportData.totalExpense) }}</div></div>
            <div class="metric-card success"><div class="metric-label">经营利润</div><div class="metric-value">&yen;{{ formatNum(reportData.operatingProfit) }}</div></div>
            <div class="metric-card info"><div class="metric-label">利润率</div><div class="metric-value">{{ formatNum(reportData.operatingProfitRate) }}%</div></div>
            <div class="metric-card danger"><div class="metric-label">未核销费用</div><div class="metric-value">&yen;{{ formatNum(reportData.unverifiedExpense) }}</div></div>
            <div class="metric-card danger"><div class="metric-label">未核销借支</div><div class="metric-value">&yen;{{ formatNum(reportData.unverifiedAdvance) }}</div></div>
          </div>
          <div v-if="reportData.salesChangeRate != null" class="comparison-row">
            <div class="comparison-card"><div class="comparison-label">销售额较上期</div><div class="comparison-value" :class="changeClass(reportData.salesChangeRate, 'sales')">{{ reportData.salesChangeRate > 0 ? '+' : '' }}{{ formatNum(reportData.salesChangeRate) }}%</div></div>
            <div class="comparison-card"><div class="comparison-label">费用较上期</div><div class="comparison-value" :class="changeClass(reportData.expenseChangeRate, 'expense')">{{ reportData.expenseChangeRate > 0 ? '+' : '' }}{{ formatNum(reportData.expenseChangeRate) }}%</div></div>
            <div class="comparison-card"><div class="comparison-label">经营利润较上期</div><div class="comparison-value" :class="changeClass(reportData.profitChangeRate, 'profit')">{{ reportData.profitChangeRate > 0 ? '+' : '' }}{{ formatNum(reportData.profitChangeRate) }}%</div></div>
          </div>
          <div v-if="reportData.suggestions && reportData.suggestions.length > 0" class="suggestions-panel">
            <div class="suggestions-title">经营建议</div>
            <ul class="suggestions-list"><li v-for="(s, idx) in reportData.suggestions" :key="idx">{{ s }}</li></ul>
          </div>
          <div v-if="reportData.alerts && reportData.alerts.length > 0" class="alert-panel">
            <el-alert v-for="(alert, idx) in reportData.alerts" :key="idx" :title="alert" type="warning" :closable="false" show-icon class="alert-item" />
          </div>
          <div v-else class="alert-panel alert-empty"><span>暂无经营提醒</span></div>
          <div class="chart-grid">
            <el-card class="chart-card wide"><template #header><span>经营趋势</span></template><div ref="trendChart" class="chart-canvas"></div></el-card>
            <el-card class="chart-card"><template #header><span>费用分类</span></template><div ref="categoryChart" class="chart-canvas"></div></el-card>
            <el-card class="chart-card"><template #header><span>未核销清单</span></template>
              <div class="pending-list-wrap">
                <el-table :data="reportData.pendingItems || []" size="small" max-height="340" empty-text="暂无未核销项" stripe>
                  <el-table-column prop="itemType" label="类型" width="80"><template #default="{ row }"><el-tag :type="row.itemType === 'EXPENSE' ? 'warning' : 'info'" size="small">{{ row.itemType === 'EXPENSE' ? '费用' : '借支' }}</el-tag></template></el-table-column>
                  <el-table-column prop="itemNo" label="单据编号" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="amount" label="金额" width="120" align="right"><template #default="{ row }">&yen;{{ formatNum(row.amount) }}</template></el-table-column>
                  <el-table-column prop="occurTime" label="日期" width="110"><template #default="{ row }">{{ formatDate(row.occurTime) }}</template></el-table-column>
                </el-table>
              </div>
            </el-card>
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane label="授权多店复盘" name="multi">
        <div class="report-filter-panel">
          <el-form :model="multiQuery" label-position="top" class="report-query-form multi-query">
            <el-form-item label="门店">
              <el-select v-model="multiQuery.deptIds" multiple collapse-tags collapse-tags-tooltip placeholder="全部授权门店" class="report-query-control">
                <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="multiQuery.startTime" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" class="report-query-control" />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker v-model="multiQuery.endTime" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" class="report-query-control" />
            </el-form-item>
            <el-form-item label="时间粒度">
              <el-select v-model="multiQuery.timeType" class="report-query-control">
                <el-option label="按天" value="day" /><el-option label="按周" value="week" /><el-option label="按月" value="month" />
              </el-select>
            </el-form-item>
            <el-form-item label="操作" class="report-query-actions">
              <el-button type="primary" icon="Search" @click="handleMultiQuery">查询</el-button>
              <el-button icon="Refresh" @click="resetMultiQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 多店汇总卡片 -->
        <div v-if="portfolioData.storeCount > 0" class="report-metrics">
          <div class="metric-card primary"><div class="metric-label">授权门店数</div><div class="metric-value">{{ portfolioData.storeCount }}</div></div>
          <div class="metric-card primary"><div class="metric-label">总销售额</div><div class="metric-value">&yen;{{ formatNum(portfolioData.totalSales) }}</div></div>
          <div class="metric-card warning"><div class="metric-label">总费用</div><div class="metric-value">&yen;{{ formatNum(portfolioData.totalExpense) }}</div></div>
          <div class="metric-card success"><div class="metric-label">经营利润</div><div class="metric-value">&yen;{{ formatNum(portfolioData.operatingProfit) }}</div></div>
          <div class="metric-card danger"><div class="metric-label">风险门店</div><div class="metric-value">{{ riskStoreCount }}</div></div>
        </div>

        <!-- 经营建议 -->
        <div v-if="portfolioData.suggestions && portfolioData.suggestions.length > 0" class="suggestions-panel">
          <div class="suggestions-title">复盘建议</div>
          <ul class="suggestions-list"><li v-for="(s, idx) in portfolioData.suggestions" :key="idx">{{ s }}</li></ul>
        </div>

        <!-- R11: 健康分分布 -->
        <div v-if="portfolioData.storeCount > 0" class="health-distribution">
          <span class="health-dist-item good">良好 {{ healthDistCount.GOOD || 0 }}</span>
          <span class="health-dist-item watch">关注 {{ healthDistCount.WATCH || 0 }}</span>
          <span class="health-dist-item risk">风险 {{ healthDistCount.RISK || 0 }}</span>
          <el-button type="primary" size="small" :loading="generateTaskLoading" @click="handleGenerateHealthTasks" style="margin-left:auto">生成复盘任务</el-button>
        </div>

        <!-- 门店健康矩阵 -->
        <el-card v-if="portfolioData.stores && portfolioData.stores.length > 0" class="chart-card" style="margin-bottom:16px">
          <template #header><span>门店健康矩阵（点击门店行可切换到单店复盘）</span></template>
          <div style="overflow-x:auto">
            <el-table :data="portfolioData.stores" size="small" stripe
                      :row-class-name="storeRowClassName"
                      @row-click="handleStoreRowClick">
              <el-table-column prop="deptName" label="门店" min-width="120" />
              <el-table-column label="健康分" width="90" align="center">
                <template #default="{ row }">
                  <span :class="'score-' + (row.healthLevel || '').toLowerCase()">{{ row.healthScore }}</span>
                </template>
              </el-table-column>
              <el-table-column label="等级" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="healthTagType(row.healthLevel)" size="small">{{ row.healthLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalSales" label="销售额" width="120" align="right"><template #default="{ row }">&yen;{{ formatNum(row.totalSales) }}</template></el-table-column>
              <el-table-column prop="operatingProfitRate" label="利润率" width="90" align="right"><template #default="{ row }">{{ formatNum(row.operatingProfitRate) }}%</template></el-table-column>
              <el-table-column prop="unverifiedAmount" label="未核销" width="120" align="right"><template #default="{ row }">&yen;{{ formatNum(row.unverifiedAmount) }}</template></el-table-column>
              <el-table-column label="扣分原因" min-width="180">
                <template #default="{ row }">
                  <template v-if="row.healthFactors && row.healthFactors.length">
                    <el-tag v-for="f in row.healthFactors.slice(0, 2)" :key="f.factorCode" :type="severityTagType(f.severity)" size="small" style="margin-right:4px">{{ f.factorName }}</el-tag>
                    <el-button v-if="row.healthFactors.length > 2" link size="small" @click.stop="showFactorDrawer(row)">+{{ row.healthFactors.length - 2 }}</el-button>
                  </template>
                  <span v-else class="text-muted">无</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.healthFactors && row.healthFactors.length" link type="primary" size="small" @click.stop="showFactorDrawer(row)">明细</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>

        <!-- R11: 扣分因子明细抽屉 -->
        <el-drawer v-model="factorDrawerVisible" title="健康分扣分明细" size="480px">
          <div v-if="factorDrawerRow" class="factor-drawer-content">
            <div class="factor-drawer-header">
              <strong>{{ factorDrawerRow.deptName }}</strong>
              <span :class="'score-' + (factorDrawerRow.healthLevel || '').toLowerCase()">
                健康分 {{ factorDrawerRow.healthScore }}（{{ factorDrawerRow.healthLevel }}）
              </span>
            </div>
            <p v-if="factorDrawerRow.healthSummary" class="factor-summary">{{ factorDrawerRow.healthSummary }}</p>
            <div v-for="f in factorDrawerRow.healthFactors" :key="f.factorCode" class="factor-item">
              <div class="factor-item-head">
                <el-tag :type="severityTagType(f.severity)" size="small">{{ f.severity }}</el-tag>
                <span class="factor-name">{{ f.factorName }}</span>
                <span class="factor-deduct">-{{ f.deductedScore }}分</span>
              </div>
              <p class="factor-reason">{{ f.reason }}</p>
              <p class="factor-suggestion">建议：{{ f.suggestion }}</p>
            </div>
          </div>
        </el-drawer>

        <!-- 复盘任务清单 -->
        <el-card v-if="portfolioData.reviewTasks && portfolioData.reviewTasks.length > 0" class="chart-card">
          <template #header><span>复盘任务清单</span></template>
          <div style="overflow-x:auto">
            <el-table :data="portfolioData.reviewTasks" size="small" stripe>
              <el-table-column label="优先级" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="severityTagType(row.severity)" size="small">{{ row.severity }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="任务" min-width="180" />
              <el-table-column prop="deptName" label="门店" width="120" />
              <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
              <el-table-column prop="amount" label="相关金额" width="120" align="right"><template #default="{ row }">&yen;{{ formatNum(row.amount) }}</template></el-table-column>
            </el-table>
          </div>
        </el-card>

        <el-empty v-if="!portfolioData.storeCount && multiQueried" description="暂无授权门店数据" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getStoreOperationSummary, getAuthorizedStorePortfolio, generateStoreHealthReviewTasks } from "@/api/finance/storeReport";
import { useUserStore } from "@/stores/user";
import { useSettingsStore } from "@/stores/settings";

const userStore = useUserStore();
const settingsStore = useSettingsStore();

export default {
  name: "StoreReport",
  data() {
    return {
      activeTab: "single",
      depts: [],
      hasDept: false,
      queryParams: { deptId: null, startTime: null, endTime: null, timeType: "day" },
      reportData: this.getEmptyData(),
      trendChart: null, categoryChart: null, resizeTimer: null, themeUnsubscribe: null,
      // 多店
      multiQuery: { deptIds: [], startTime: null, endTime: null, timeType: "day" },
      portfolioData: { storeCount: 0, totalSales: 0, totalExpense: 0, operatingProfit: 0, operatingProfitRate: 0, stores: [], reviewTasks: [], suggestions: [] },
      multiQueried: false,
      // R11: drawer and task generation
      factorDrawerVisible: false,
      factorDrawerRow: null,
      generateTaskLoading: false
    };
  },
  computed: {
    riskStoreCount() {
      return (this.portfolioData.stores || []).filter(s => s.healthLevel === "RISK").length;
    },
    healthDistCount() {
      const counts = { GOOD: 0, WATCH: 0, RISK: 0 };
      (this.portfolioData.stores || []).forEach(s => { if (counts[s.healthLevel] !== undefined) counts[s.healthLevel]++; });
      return counts;
    }
  },
  created() {
    this.getDepts();
    if (userStore.currentDeptId) {
      this.queryParams.deptId = userStore.currentDeptId;
      this.hasDept = true;
    }
    this.multiQuery.deptIds = this.depts.map(d => d.id);
  },
  mounted() {
    if (this.hasDept) { this.handleQuery(); }
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
    this.categoryChart && this.categoryChart.dispose();
  },
  methods: {
    getDepts() {
      this.depts = (userStore.depts || []).map(dept => ({ id: dept.deptId, label: dept.deptName }));
    },
    handleQuery() {
      if (!this.queryParams.deptId) { this.hasDept = false; this.reportData = this.getEmptyData(); return; }
      this.hasDept = true;
      getStoreOperationSummary(this.queryParams).then(response => {
        this.reportData = response.data || this.getEmptyData();
        this.$nextTick(() => { this.initTrendChart(); this.initCategoryChart(); });
      });
    },
    resetQuery() {
      this.queryParams = { deptId: userStore.currentDeptId || null, startTime: null, endTime: null, timeType: "day" };
      this.hasDept = !!this.queryParams.deptId;
      if (this.hasDept) { this.handleQuery(); } else { this.reportData = this.getEmptyData(); }
    },
    handleMultiQuery() {
      this.multiQueried = true;
      getAuthorizedStorePortfolio(this.multiQuery).then(response => {
        this.portfolioData = response.data || { storeCount: 0, totalSales: 0, totalExpense: 0, operatingProfit: 0, operatingProfitRate: 0, stores: [], reviewTasks: [], suggestions: [] };
      });
    },
    resetMultiQuery() {
      this.multiQuery = { deptIds: this.depts.map(d => d.id), startTime: null, endTime: null, timeType: "day" };
      this.portfolioData = { storeCount: 0, totalSales: 0, totalExpense: 0, operatingProfit: 0, operatingProfitRate: 0, stores: [], reviewTasks: [], suggestions: [] };
      this.multiQueried = false;
    },
    healthTagType(level) {
      if (level === "GOOD") return "success";
      if (level === "WATCH") return "warning";
      if (level === "RISK") return "danger";
      return "info";
    },
    severityTagType(sev) {
      if (sev === "HIGH") return "danger";
      if (sev === "MEDIUM") return "warning";
      return "info";
    },
    storeRowClassName({ row }) {
      if (row.highRiskCount > 0) return "high-risk-row";
      if (row.healthLevel === "RISK") return "risk-row";
      return "";
    },
    handleStoreRowClick(row) {
      if (!row.deptId) return;
      this.queryParams.deptId = row.deptId;
      this.hasDept = true;
      this.activeTab = "single";
      this.handleQuery();
    },
    showFactorDrawer(row) {
      this.factorDrawerRow = row;
      this.factorDrawerVisible = true;
    },
    handleGenerateHealthTasks() {
      this.generateTaskLoading = true;
      generateStoreHealthReviewTasks({
        deptIds: this.multiQuery.deptIds,
        startTime: this.multiQuery.startTime,
        endTime: this.multiQuery.endTime
      }).then(res => {
        const d = res.data || {};
        this.$message.success(`生成复盘任务 ${d.insertedCount || 0} 条，跳过 ${d.skippedCount || 0} 条（已存在）`);
      }).catch(() => {
        this.$message.error("生成复盘任务失败");
      }).finally(() => {
        this.generateTaskLoading = false;
      });
    },
    getEmptyData() {
      return { totalSales: 0, totalExpense: 0, operatingProfit: 0, operatingProfitRate: 0, unverifiedExpense: 0, unverifiedAdvance: 0, saleCount: 0, saleQuantity: 0, avgOrderAmount: 0, previousTotalSales: 0, previousTotalExpense: 0, previousOperatingProfit: 0, salesChangeRate: null, expenseChangeRate: null, profitChangeRate: null, suggestions: [], alerts: [], trendRows: [], expenseCategories: [], pendingItems: [] };
    },
    handleResize() { if (this.resizeTimer) clearTimeout(this.resizeTimer); this.resizeTimer = setTimeout(() => { this.trendChart && this.trendChart.resize(); this.categoryChart && this.categoryChart.resize(); }, 200); },
    reRenderCharts() { this.initTrendChart(); this.initCategoryChart(); },
    formatNum(val) { if (val == null) return "0"; return Number(val).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }); },
    changeClass(val, type) { if (val == null || val === 0) return "change-neutral"; if (type === "expense") { return val > 0 ? "change-danger" : "change-good"; } return val > 0 ? "change-good" : "change-danger"; },
    formatDate(val) { if (!val) return ""; const d = new Date(val); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`; },
    getCssVar(name, fallback) { const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim(); return v || fallback; },
    getThemeColor() { return this.getCssVar("--theme-primary", "#409EFF"); },
    getThemeRgb() { return this.getCssVar("--theme-primary-rgb", "64, 158, 255"); },
    getThemeRgba(a) { return `rgba(${this.getThemeRgb()}, ${a})`; },
    getPalette() { return [this.getThemeColor(), this.getThemeRgba(0.72), "#67C23A", "#E6A23C", "#909399", "#F56C6C"]; },
    getEmptyGraphic(data) { return data && data.length ? null : { type: "text", left: "center", top: "middle", style: { text: "暂无数据", fill: "#909399", fontSize: 14 } }; },
    getAxisBase() { return { axisLine: { lineStyle: { color: "#dcdfe6" } }, axisTick: { show: false }, axisLabel: { color: "#606266" }, splitLine: { lineStyle: { color: "#edf0f5", type: "dashed" } } }; },
    initTrendChart() {
      if (!this.$refs.trendChart) return;
      const echarts = require("echarts");
      if (this.trendChart) this.trendChart.dispose();
      this.trendChart = echarts.init(this.$refs.trendChart);
      const rows = this.reportData.trendRows || [];
      const labels = rows.map(r => r.dateStr);
      this.trendChart.setOption({
        color: this.getPalette(), tooltip: { trigger: "axis" },
        legend: { top: 0, left: 16, right: 16, type: "scroll", icon: "circle", textStyle: { color: "#606266" } },
        grid: { left: 34, right: 22, top: 50, bottom: 36, containLabel: true },
        graphic: this.getEmptyGraphic(rows),
        xAxis: { ...this.getAxisBase(), type: "category", boundaryGap: false, data: labels },
        yAxis: { ...this.getAxisBase(), type: "value" },
        series: [
          { name: "销售额", type: "line", smooth: true, symbol: "circle", symbolSize: 7, data: rows.map(r => Number(r.salesAmount || 0)), lineStyle: { width: 3 }, itemStyle: { borderWidth: 2, borderColor: "#fff" } },
          { name: "费用", type: "line", smooth: true, symbol: "circle", symbolSize: 7, data: rows.map(r => Number(r.expenseAmount || 0)), lineStyle: { width: 3 }, itemStyle: { borderWidth: 2, borderColor: "#fff" } },
          { name: "经营利润", type: "line", smooth: true, symbol: "diamond", symbolSize: 7, data: rows.map(r => Number(r.operatingProfit || 0)), lineStyle: { width: 3, type: "dashed" }, itemStyle: { borderWidth: 2, borderColor: "#fff" } }
        ]
      });
    },
    initCategoryChart() {
      if (!this.$refs.categoryChart) return;
      const echarts = require("echarts");
      if (this.categoryChart) this.categoryChart.dispose();
      this.categoryChart = echarts.init(this.$refs.categoryChart);
      const cats = this.reportData.expenseCategories || [];
      this.categoryChart.setOption({
        color: this.getPalette(), tooltip: { trigger: "item", formatter: "{b}: ¥{c} ({d}%)" },
        legend: { bottom: 0, left: "center", icon: "circle", textStyle: { color: "#606266" } },
        graphic: this.getEmptyGraphic(cats),
        series: [{ type: "pie", radius: ["46%", "68%"], center: ["50%", "44%"], avoidLabelOverlap: true, label: { color: "#606266", formatter: "{b}\n{d}%" }, labelLine: { smooth: true, lineStyle: { color: "#c0c4cc" } }, itemStyle: { borderColor: "#fff", borderWidth: 2 }, data: cats.map(c => ({ name: c.categoryName, value: c.amount })) }]
      });
    }
  }
};
</script>

<style scoped>
.report-page { background: #f5f7fb; min-height: calc(100vh - 84px); }
.report-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
.report-tabs :deep(.el-tabs__content) { overflow: visible; }
.report-filter-panel { padding: 14px 16px; margin-bottom: 16px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; }
.report-query-form { display: grid; grid-template-columns: minmax(200px, 1.2fr) minmax(180px, 1fr) minmax(180px, 1fr) minmax(120px, 0.6fr) auto; gap: 14px 18px; align-items: end; }
.multi-query { grid-template-columns: minmax(260px, 1.5fr) minmax(180px, 1fr) minmax(180px, 1fr) minmax(120px, 0.6fr) auto; }
.report-query-form :deep(.el-form-item) { margin-bottom: 0; }
.report-query-form :deep(.el-form-item__label) { justify-content: flex-start; margin-bottom: 8px; color: #606266; font-weight: 600; line-height: 1.2; }
.report-query-control { width: 100%; }
.report-query-actions :deep(.el-form-item__content) { display: flex; flex-wrap: nowrap; gap: 10px; }
@media (max-width: 1100px) { .report-query-form { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 768px) { .report-query-form { grid-template-columns: 1fr; } }
.empty-state { display: flex; justify-content: center; align-items: center; min-height: 400px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; }
.report-metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 16px; }
.metric-card { position: relative; min-height: 88px; padding: 14px 16px; overflow: hidden; background: #fff; border: 1px solid rgba(var(--theme-primary-rgb), 0.14); border-radius: 8px; box-shadow: 0 8px 22px rgba(var(--theme-primary-rgb), 0.06); }
.metric-card::before { content: ""; position: absolute; left: 0; top: 14px; bottom: 14px; width: 4px; border-radius: 0 8px 8px 0; background: var(--theme-primary); }
.metric-card.warning::before { background: #E6A23C; } .metric-card.success::before { background: #67C23A; } .metric-card.info::before { background: #909399; } .metric-card.danger::before { background: #F56C6C; }
.metric-card::after { content: ""; position: absolute; right: -34px; top: -48px; width: 116px; height: 116px; border-radius: 999px; background: rgba(var(--theme-primary-rgb), 0.08); }
.metric-label { position: relative; z-index: 1; color: #606266; font-size: 13px; font-weight: 600; line-height: 1.3; }
.metric-value { position: relative; z-index: 1; margin-top: 10px; color: var(--theme-primary-dark); font-size: 22px; font-weight: 800; line-height: 1.2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.alert-panel { margin-bottom: 16px; } .alert-item { margin-bottom: 8px; }
.alert-empty { padding: 12px 16px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; color: #909399; font-size: 14px; text-align: center; }
.comparison-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 16px; }
.comparison-card { padding: 12px 16px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; text-align: center; }
.comparison-label { color: #909399; font-size: 13px; margin-bottom: 6px; }
.comparison-value { font-size: 18px; font-weight: 700; }
.change-good { color: #67C23A; } .change-danger { color: #F56C6C; } .change-neutral { color: #909399; }
.suggestions-panel { padding: 14px 16px; margin-bottom: 16px; background: #fff; border: 1px solid rgba(var(--theme-primary-rgb), 0.2); border-radius: 8px; }
.suggestions-title { color: #303133; font-weight: 700; font-size: 14px; margin-bottom: 8px; }
.suggestions-list { margin: 0; padding-left: 20px; } .suggestions-list li { color: #606266; font-size: 13px; line-height: 1.8; }
.chart-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.chart-card { border-radius: 8px; border: 1px solid #ebeef5; box-shadow: 0 8px 24px rgba(24, 39, 75, 0.04); }
.chart-card.wide { grid-column: 1 / -1; }
.chart-card :deep(.el-card__header) { padding: 14px 18px; border-bottom: 1px solid #edf0f5; color: #303133; font-weight: 700; }
.chart-card :deep(.el-card__body) { padding: 14px 16px 18px; }
.chart-canvas { width: 100%; height: 360px; } .chart-card.wide .chart-canvas { height: 380px; }
.pending-list-wrap { min-height: 200px; }
/* R8-C: 多门店对比行样式 */
:deep(.high-risk-row) { background: rgba(245, 108, 108, 0.08) !important; }
:deep(.high-risk-row:hover) { background: rgba(245, 108, 108, 0.14) !important; }
:deep(.risk-row) { background: rgba(230, 162, 60, 0.06) !important; }
.negative-cashflow { color: #F56C6C; font-weight: 700; }
.risk-text { color: #E6A23C; font-weight: 600; }
.action-urgent { color: #F56C6C; font-weight: 600; }
.text-muted { color: #C0C4CC; }
/* R11: 健康分分布 */
.health-distribution { display: flex; align-items: center; gap: 16px; padding: 10px 16px; margin-bottom: 12px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; }
.health-dist-item { font-weight: 700; font-size: 14px; }
.health-dist-item.good { color: #67C23A; } .health-dist-item.watch { color: #E6A23C; } .health-dist-item.risk { color: #F56C6C; }
.score-good { color: #67C23A; font-weight: 700; } .score-watch { color: #E6A23C; font-weight: 700; } .score-risk { color: #F56C6C; font-weight: 700; }
/* R11: 因子抽屉 */
.factor-drawer-content { padding: 0 4px; }
.factor-drawer-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.factor-summary { color: #606266; font-size: 13px; margin-bottom: 16px; padding: 8px 12px; background: #f5f7fa; border-radius: 6px; }
.factor-item { padding: 12px; margin-bottom: 8px; border: 1px solid #ebeef5; border-radius: 6px; }
.factor-item-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.factor-name { font-weight: 600; color: #303133; }
.factor-deduct { margin-left: auto; color: #F56C6C; font-weight: 700; font-size: 13px; }
.factor-reason { color: #606266; font-size: 13px; margin: 4px 0; }
.factor-suggestion { color: #909399; font-size: 12px; margin: 0; }
@media (max-width: 768px) { .chart-grid { grid-template-columns: 1fr; } .chart-card.wide { grid-column: auto; } .chart-canvas, .chart-card.wide .chart-canvas { height: 320px; } }
</style>
