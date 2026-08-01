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
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card>
          <template #header><span>会员总数</span></template>
          <div class="stat-value">{{ reportData.totalMemberCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header><span>新增会员</span></template>
          <div class="stat-value" style="color: #67C23A;">{{ reportData.todayNewMemberCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header><span>活跃会员</span></template>
          <div class="stat-value" style="color: #409EFF;">{{ reportData.activeMemberCount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Original Charts -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span>会员增长趋势</span></template>
          <div ref="growthChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>会员类型分布</span></template>
          <div ref="typeChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Member Contribution Report Section -->
    <el-card class="section-card" style="margin-top: 20px;">
      <template #header><span>会员经营贡献</span></template>

      <!-- Contribution Metrics -->
      <div class="report-metrics">
        <div class="metric-card primary">
          <div class="metric-label">会员销售额</div>
          <div class="metric-value">{{ money(contributionData.memberSales) }}</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">非会员销售额</div>
          <div class="metric-value">{{ money(contributionData.nonMemberSales) }}</div>
        </div>
        <div class="metric-card warning">
          <div class="metric-label">会员销售占比</div>
          <div class="metric-value">{{ contributionData.memberSalesRatio || 0 }}%</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">会员销售笔数</div>
          <div class="metric-value">{{ contributionData.memberSaleCount || 0 }}</div>
        </div>
        <div class="metric-card success">
          <div class="metric-label">会员平均客单价</div>
          <div class="metric-value">{{ money(contributionData.avgMemberSaleAmount) }}</div>
        </div>
      </div>

      <!-- R2: Purchase Rate Metrics -->
      <div class="report-metrics" style="margin-top: 8px;">
        <div class="metric-card primary">
          <div class="metric-label">新会员首购率</div>
          <div class="metric-value">{{ contributionData.newMemberFirstPurchaseRate || 0 }}%</div>
        </div>
        <div class="metric-card success">
          <div class="metric-label">复购会员数</div>
          <div class="metric-value">{{ contributionData.repeatPurchaseCount || 0 }}</div>
        </div>
        <div class="metric-card info">
          <div class="metric-label">复购率</div>
          <div class="metric-value">{{ contributionData.repeatPurchaseRate || 0 }}%</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">秒杀销售额</div>
          <div class="metric-value">{{ money(contributionData.seckillSales) }}</div>
        </div>
        <div class="metric-card warning">
          <div class="metric-label">积分兑换成本</div>
          <div class="metric-value">{{ money(contributionData.pointsRedemptionCost) }}</div>
        </div>
      </div>

      <!-- Data Note -->
      <div v-if="contributionData.dataNote" class="data-note">
        <span class="data-note-label">口径说明：</span>{{ contributionData.dataNote }}
      </div>
    </el-card>

    <!-- Member vs Non-member Trend -->
    <el-card class="section-card" style="margin-top: 16px;">
      <template #header><span>会员 vs 非会员销售趋势</span></template>
      <div ref="salesTrendChart" class="chart-canvas" style="height: 380px;"></div>
    </el-card>

    <!-- Points & Seckill -->
    <div class="chart-grid" style="margin-top: 16px;">
      <el-card class="chart-card">
        <template #header><span>积分兑换成本</span></template>
        <div class="points-display">
          <div class="points-row">
            <span class="points-label">总兑换积分</span>
            <span class="points-value">{{ contributionData.totalPointsRedeemed || 0 }}</span>
          </div>
          <div class="points-row">
            <span class="points-label">兑换成本</span>
            <span class="points-value">{{ money(contributionData.pointsRedeemCost) }}</span>
          </div>
          <div class="points-row">
            <span class="points-label">兑换笔数</span>
            <span class="points-value">{{ contributionData.pointsRedeemCount || 0 }}</span>
          </div>
        </div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>秒杀活动贡献</span></template>
        <el-table :data="contributionData.seckillActivities || []" stripe border style="width: 100%" empty-text="暂无数据" max-height="320">
          <el-table-column prop="activityName" label="活动名称" min-width="140" />
          <el-table-column label="销售额" min-width="100">
            <template #default="scope">{{ money(scope.row.sales) }}</template>
          </el-table-column>
          <el-table-column label="成本" min-width="100">
            <template #default="scope">{{ money(scope.row.cost) }}</template>
          </el-table-column>
          <el-table-column label="利润" min-width="100">
            <template #default="scope">{{ money(scope.row.profit) }}</template>
          </el-table-column>
          <el-table-column prop="participantCount" label="参与人数" width="100" />
        </el-table>
      </el-card>
    </div>
    <!-- Activity ROI Section -->
    <el-card class="section-card" style="margin-top: 20px;">
      <template #header>
        <div class="section-header-row">
          <span>活动 ROI</span>
          <el-button type="primary" link size="small" @click="handleRefreshRoi">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <div v-if="roiLoading" class="roi-loading">加载中...</div>

      <el-table
        v-else
        :data="roiData"
        stripe
        border
        style="width: 100%"
        empty-text="暂无活动数据"
        max-height="420"
      >
        <el-table-column prop="activityName" label="活动名称" min-width="140">
          <template #default="{ row }">
            <router-link
              v-if="row.activityId"
              :to="{ path: '/member/seckill' }"
              class="roi-link"
            >{{ row.activityName }}</router-link>
            <span v-else>{{ row.activityName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '进行中' : '已结束' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="180">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }} ~ {{ formatDate(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="销售额" width="120" align="right">
          <template #default="{ row }">
            <span class="roi-metric">{{ money(row.totalSalesAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单数" width="90" align="center">
          <template #default="{ row }">
            {{ row.totalOrders || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="新会员" width="90" align="center">
          <template #default="{ row }">
            {{ row.newCustomerCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="售罄率" width="100" align="right">
          <template #default="{ row }">
            <span :class="sellThroughClass(row.sellThroughRate)">{{ row.sellThroughRate || 0 }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="折扣成本" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.discountCostStatus === 'AVAILABLE'" class="roi-metric">
              {{ money(row.discountCost) }}
            </span>
            <el-tag v-else type="warning" size="small" effect="plain">不可用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="ROI" width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.roiStatus === 'READY'" :class="roiClass(row.roi)">
              {{ row.roi }}%
            </span>
            <el-tooltip v-else-if="row.roiStatus === 'UNAVAILABLE'" :content="roiUnavailableHint(row)" placement="top" effect="dark">
              <el-tag type="warning" size="small" effect="plain">ROI 暂不可算</el-tag>
            </el-tooltip>
            <el-tag v-else type="info" size="small" effect="plain">暂无数据</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="roi-note">
        <span class="data-note-label">说明：</span>
        折扣成本与 ROI 标记为"不可用"时，表示原价数据不可靠，不做估算。
        售罄率 = (总份额 - 剩余份额) / 总份额。
        <router-link to="/member/seckill-record" class="roi-link">查看秒杀记录明细</router-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getDeptTreeSelect } from "@/api/system/dept";
import request from "@/utils/request";
import { Refresh } from "@element-plus/icons-vue";

export default {
  name: "MemberReport",
  components: {
    Refresh
  },
  data() {
    return {
      depts: [],
      queryParams: {
        deptIds: [],
        startTime: null,
        endTime: null
      },
      roiData: [],
      roiLoading: false,
      reportData: {
        totalMemberCount: 0,
        todayNewMemberCount: 0,
        activeMemberCount: 0,
        memberGrowthStats: [],
        memberTypeStats: []
      },
      contributionData: {
        newMemberCount: 0,
        activeMemberCount: 0,
        repurchaseCount: 0,
        memberSales: 0,
        nonMemberSales: 0,
        memberSalesRatio: 0,
        memberSaleCount: 0,
        avgMemberSaleAmount: 0,
        newMemberFirstPurchaseRate: 0,
        repeatPurchaseCount: 0,
        repeatPurchaseRate: 0,
        pointsRedemptionCost: 0,
        seckillSales: 0,
        seckillCost: 0,
        seckillProfit: 0,
        dataNote: '',
        totalPointsRedeemed: 0,
        pointsRedeemCost: 0,
        pointsRedeemCount: 0,
        salesTrendStats: [],
        seckillActivities: []
      },
      growthChart: null,
      typeChart: null,
      salesTrendChart: null,
      resizeTimer: null
    };
  },
  created() {
    this.getDepts();
  },
  mounted() {
    this.handleQuery();
    window.addEventListener("resize", this.handleResize);
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.handleResize);
    this.growthChart && this.growthChart.dispose();
    this.typeChart && this.typeChart.dispose();
    this.salesTrendChart && this.salesTrendChart.dispose();
  },
  methods: {
    getDepts() {
      getDeptTreeSelect().then(response => {
        this.depts = this.flattenDepts(response.data);
      });
    },
    flattenDepts(depts) {
      let result = [];
      depts.forEach(dept => {
        result.push({ id: dept.id, label: dept.label });
        if (dept.children) {
          result = result.concat(this.flattenDepts(dept.children));
        }
      });
      return result;
    },
    handleQuery() {
      request({
        url: "/member/report/member",
        method: "post",
        data: this.queryParams
      }).then(response => {
        this.reportData = response.data;
        this.$nextTick(() => {
          this.initGrowthChart();
          this.initTypeChart();
        });
      });
      // Fetch contribution data
      request({
        url: "/member/report/contribution",
        method: "post",
        data: this.queryParams
      }).then(response => {
        this.contributionData = response.data || this.contributionData;
        this.$nextTick(() => {
          this.initSalesTrendChart();
        });
      }).catch(() => {
        // keep defaults
      });
      // Fetch activity ROI data
      this.fetchActivityRoi();
    },
    fetchActivityRoi() {
      this.roiLoading = true;
      var deptId = this.queryParams.deptIds && this.queryParams.deptIds.length > 0
        ? this.queryParams.deptIds[0]
        : null;
      request({
        url: "/member/report/activity-roi",
        method: "post",
        params: { deptId: deptId }
      }).then(response => {
        this.roiData = response.data || [];
      }).catch(() => {
        this.roiData = [];
      }).finally(() => {
        this.roiLoading = false;
      });
    },
    handleRefreshRoi() {
      this.fetchActivityRoi();
    },
    formatDate(dateStr) {
      if (!dateStr) return '-';
      if (typeof dateStr === 'string') return dateStr.substring(0, 10);
      return '-';
    },
    sellThroughClass(rate) {
      if (rate >= 80) return 'roi-rate-high';
      if (rate >= 50) return 'roi-rate-mid';
      return 'roi-rate-low';
    },
    roiClass(roi) {
      if (roi > 0) return 'roi-positive';
      if (roi < 0) return 'roi-negative';
      return '';
    },
    roiUnavailableHint(row) {
      if (row && row.suggestion) return row.suggestion;
      switch (row && row.unavailableReason) {
        case 'MISSING_ACTIVITY_COST':
          return '缺少活动成本，暂不能计算 ROI，请先补充活动成本。';
        case 'NO_RELATED_SALES':
          return '暂无关联销售，暂不能计算 ROI。';
        default:
          return 'ROI 暂不可算。';
      }
    },
    resetQuery() {
      this.queryParams = {
        deptIds: [],
        startTime: null,
        endTime: null
      };
      this.handleQuery();
    },
    handleResize() {
      if (this.resizeTimer) {
        clearTimeout(this.resizeTimer);
      }
      this.resizeTimer = setTimeout(() => {
        this.growthChart && this.growthChart.resize();
        this.typeChart && this.typeChart.resize();
        this.salesTrendChart && this.salesTrendChart.resize();
      }, 200);
    },
    getCssVar(name, fallback) {
      const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
      return value || fallback;
    },
    getThemeColor() {
      return this.getCssVar("--theme-primary", "#409EFF");
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
    initGrowthChart() {
      if (!this.$refs.growthChart) return;
      var echarts = require("echarts");
      if (this.growthChart) {
        this.growthChart.dispose();
      }
      this.growthChart = echarts.init(this.$refs.growthChart);
      var data = this.reportData.memberGrowthStats || [];
      this.growthChart.setOption({
        tooltip: { trigger: "axis" },
        xAxis: { type: "category", data: data.map(function(item) { return item.dateStr; }) },
        yAxis: { type: "value" },
        series: [{ type: "line", data: data.map(function(item) { return item.count; }) }]
      });
    },
    initTypeChart() {
      if (!this.$refs.typeChart) return;
      var echarts = require("echarts");
      if (this.typeChart) {
        this.typeChart.dispose();
      }
      this.typeChart = echarts.init(this.$refs.typeChart);
      var data = this.reportData.memberTypeStats || [];
      this.typeChart.setOption(this.getPieOptions(data.map(function(item) { return { name: item.typeName, value: item.count }; })));
    },
    initSalesTrendChart() {
      if (!this.$refs.salesTrendChart) return;
      var echarts = require("echarts");
      if (this.salesTrendChart) this.salesTrendChart.dispose();
      this.salesTrendChart = echarts.init(this.$refs.salesTrendChart);
      var data = this.contributionData.salesTrendStats || [];
      if (!data.length) {
        this.salesTrendChart.setOption({
          graphic: {
            type: "text",
            left: "center",
            top: "middle",
            style: { text: "暂无数据", fill: "#909399", fontSize: 14 }
          }
        });
        return;
      }
      var labels = Array.from(new Set(data.map(function(item) { return item.dateStr; }))).sort();
      var memberMap = {};
      var nonMemberMap = {};
      data.forEach(function(item) {
        if (item.memberSales != null) memberMap[item.dateStr] = item.memberSales;
        if (item.nonMemberSales != null) nonMemberMap[item.dateStr] = item.nonMemberSales;
      });
      var palette = this.getPalette();
      this.salesTrendChart.setOption({
        color: palette,
        tooltip: { trigger: "axis" },
        legend: { top: 0, left: 16, right: 16, type: "scroll", icon: "circle", textStyle: { color: "#606266" } },
        grid: { left: 34, right: 22, top: 70, bottom: 36, containLabel: true },
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
        series: [
          {
            name: "会员销售",
            type: "line",
            smooth: true,
            symbol: "circle",
            symbolSize: 7,
            data: labels.map(function(d) { return Number(memberMap[d] || 0); }),
            lineStyle: { width: 3, color: palette[0] },
            itemStyle: { color: palette[0], borderWidth: 2, borderColor: "#fff" },
            areaStyle: { color: this.getThemeRgba(0.1) }
          },
          {
            name: "非会员销售",
            type: "line",
            smooth: true,
            symbol: "circle",
            symbolSize: 7,
            data: labels.map(function(d) { return Number(nonMemberMap[d] || 0); }),
            lineStyle: { width: 3, color: palette[2] },
            itemStyle: { color: palette[2], borderWidth: 2, borderColor: "#fff" }
          }
        ]
      });
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

.stat-value {
  text-align: center;
  font-size: 32px;
  font-weight: bold;
  padding: 20px 0;
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

@media (max-width: 768px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

.points-display {
  padding: 10px 0;
}

.points-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #edf0f5;
}

.points-row:last-child {
  border-bottom: none;
}

.points-label {
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}

.points-value {
  color: var(--theme-primary-dark);
  font-size: 20px;
  font-weight: 800;
}

.mb20 {
  margin-bottom: 20px;
}

.data-note {
  margin-top: 12px;
  padding: 10px 14px;
  background: #f0f9ff;
  border: 1px solid #d0e8ff;
  border-radius: 6px;
  color: #606266;
  font-size: 12px;
  line-height: 1.6;
}

.data-note-label {
  color: #409EFF;
  font-weight: 700;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.roi-loading {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 14px;
}

.roi-link {
  color: var(--theme-primary, #409EFF);
  text-decoration: none;
  font-weight: 600;
}

.roi-link:hover {
  text-decoration: underline;
}

.roi-metric {
  font-weight: 700;
  color: #303133;
}

.roi-rate-high {
  color: #67C23A;
  font-weight: 700;
}

.roi-rate-mid {
  color: #E6A23C;
  font-weight: 700;
}

.roi-rate-low {
  color: #909399;
  font-weight: 600;
}

.roi-positive {
  color: #67C23A;
  font-weight: 700;
}

.roi-negative {
  color: #F56C6C;
  font-weight: 700;
}

.roi-note {
  margin-top: 12px;
  padding: 10px 14px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  color: #606266;
  font-size: 12px;
  line-height: 1.6;
}
</style>
