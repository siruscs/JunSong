<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="100px">
      <el-form-item label="门店">
        <el-select v-model="queryParams.deptIds" placeholder="请选择门店" multiple clearable style="width: 300px;">
          <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker v-model="queryParams.startTime" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker v-model="queryParams.endTime" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

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
  </div>
</template>

<script>
import { getDeptTreeSelect } from "@/api/system/dept";
import request from "@/utils/request";

export default {
  name: "MemberReport",
  data() {
    return {
      depts: [],
      queryParams: {
        deptIds: [],
        startTime: null,
        endTime: null
      },
      reportData: {
        totalMemberCount: 0,
        todayNewMemberCount: 0,
        activeMemberCount: 0,
        memberGrowthStats: [],
        memberTypeStats: []
      },
      growthChart: null,
      typeChart: null,
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
      }, 200);
    },
    initGrowthChart() {
      if (!this.$refs.growthChart) return;
      const echarts = require("echarts");
      if (this.growthChart) {
        this.growthChart.dispose();
      }
      this.growthChart = echarts.init(this.$refs.growthChart);
      const data = this.reportData.memberGrowthStats || [];
      this.growthChart.setOption({
        tooltip: { trigger: "axis" },
        xAxis: { type: "category", data: data.map(item => item.dateStr) },
        yAxis: { type: "value" },
        series: [{ type: "line", data: data.map(item => item.count) }]
      });
    },
    initTypeChart() {
      if (!this.$refs.typeChart) return;
      const echarts = require("echarts");
      if (this.typeChart) {
        this.typeChart.dispose();
      }
      this.typeChart = echarts.init(this.$refs.typeChart);
      const data = this.reportData.memberTypeStats || [];
      this.typeChart.setOption({
        tooltip: { trigger: "item" },
        series: [
          {
            type: "pie",
            radius: "50%",
            data: data.map(item => ({ name: item.typeName, value: item.count }))
          }
        ]
      });
    }
  }
};
</script>

<style scoped>
.stat-value {
  text-align: center;
  font-size: 32px;
  font-weight: bold;
  padding: 20px 0;
}
</style>
