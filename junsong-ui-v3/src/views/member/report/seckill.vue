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
          <template #header><span>活动总数</span></template>
          <div class="stat-value">{{ reportData.totalSeckillCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header><span>参与人数</span></template>
          <div class="stat-value" style="color: #67C23A;">{{ reportData.totalParticipantCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header><span>总收入</span></template>
          <div class="stat-value" style="color: #409EFF;">¥{{ reportData.totalRevenue || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span>活动效果统计</span></template>
          <div ref="seckillChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>门店活动对比</span></template>
          <div ref="deptChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getDeptTreeSelect } from "@/api/system/dept";
import request from "@/utils/request";

export default {
  name: "SeckillReport",
  data() {
    return {
      depts: [],
      queryParams: {
        deptIds: [],
        startTime: null,
        endTime: null
      },
      reportData: {
        totalSeckillCount: 0,
        totalParticipantCount: 0,
        totalRevenue: 0,
        seckillStats: [],
        seckillDeptStats: []
      },
      seckillChart: null,
      deptChart: null,
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
    this.seckillChart && this.seckillChart.dispose();
    this.deptChart && this.deptChart.dispose();
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
        url: "/member/report/seckill",
        method: "post",
        data: this.queryParams
      }).then(response => {
        this.reportData = response.data;
        this.$nextTick(() => {
          this.initSeckillChart();
          this.initDeptChart();
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
        this.seckillChart && this.seckillChart.resize();
        this.deptChart && this.deptChart.resize();
      }, 200);
    },
    initSeckillChart() {
      if (!this.$refs.seckillChart) return;
      const echarts = require("echarts");
      if (this.seckillChart) {
        this.seckillChart.dispose();
      }
      this.seckillChart = echarts.init(this.$refs.seckillChart);
      const data = this.reportData.seckillStats || [];
      this.seckillChart.setOption({
        tooltip: { trigger: "axis" },
        xAxis: { type: "category", data: data.map(item => item.activityName) },
        yAxis: { type: "value" },
        series: [{ type: "bar", data: data.map(item => item.revenue) }]
      });
    },
    initDeptChart() {
      if (!this.$refs.deptChart) return;
      const echarts = require("echarts");
      if (this.deptChart) {
        this.deptChart.dispose();
      }
      this.deptChart = echarts.init(this.$refs.deptChart);
      const data = this.reportData.seckillDeptStats || [];
      this.deptChart.setOption({
        tooltip: { trigger: "axis" },
        xAxis: { type: "category", data: data.map(item => item.deptName) },
        yAxis: { type: "value" },
        series: [
          { name: "参与人数", type: "bar", data: data.map(item => item.participantCount) },
          { name: "收入", type: "bar", data: data.map(item => item.revenue) }
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