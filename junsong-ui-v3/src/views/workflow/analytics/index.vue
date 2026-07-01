<template>
  <div class="app-container">
    <el-form :inline="true">
      <el-form-item label="流程标识">
        <el-input v-model="processKey" placeholder="如：leave-apply" clearable style="width: 220px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadAll">查询分析</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>流程耗时统计</template>
          <el-empty v-if="!processStats.totalInstances" description="暂无数据，请输入流程标识查询" />
          <el-descriptions v-else :column="2" border>
            <el-descriptions-item label="总实例数">{{ processStats.totalInstances }}</el-descriptions-item>
            <el-descriptions-item label="平均耗时">{{ processStats.avgDurationFormatted }}</el-descriptions-item>
            <el-descriptions-item label="最大耗时">{{ processStats.maxDurationFormatted }}</el-descriptions-item>
            <el-descriptions-item label="最小耗时">{{ processStats.minDurationFormatted }}</el-descriptions-item>
            <el-descriptions-item label="中位数耗时">{{ processStats.medianDurationFormatted }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>人员效率排名（Top 10）</template>
          <el-empty v-if="!userEfficiency.length" description="暂无数据" />
          <el-table v-else :data="userEfficiency.slice(0, 10)" size="small">
            <el-table-column label="排名" width="60">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column label="处理人" prop="assignee" />
            <el-table-column label="处理任务数" prop="totalTasks" width="100" />
            <el-table-column label="平均耗时" prop="avgDurationFormatted" width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>节点平均耗时（柱状图）</template>
          <div ref="nodeDurationChart" style="height: 320px" />
          <el-empty v-if="processKey && !nodeStats.length" description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>节点任务数量（饼图）</template>
          <div ref="nodeTaskCountChart" style="height: 320px" />
          <el-empty v-if="processKey && !nodeStats.length" description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>节点耗时明细（瓶颈识别）</template>
      <el-empty v-if="processKey && !nodeStats.length" description="暂无数据" />
      <el-table v-else :data="nodeStats" size="small">
        <el-table-column label="节点ID" prop="activityId" min-width="140" />
        <el-table-column label="节点名称" prop="activityName" min-width="120" />
        <el-table-column label="任务数" prop="taskCount" width="80" />
        <el-table-column label="平均耗时" prop="avgDurationFormatted" width="120" />
        <el-table-column label="最大耗时" prop="maxDurationFormatted" width="120" />
        <el-table-column label="最小耗时" prop="minDurationFormatted" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getNodeDurationStats, getUserEfficiencyStats, getProcessDurationStats } from '@/api/workflow/analytics'

const processKey = ref('')
const nodeStats = ref<any[]>([])
const userEfficiency = ref<any[]>([])
const processStats = ref<any>({})

const nodeDurationChart = ref<HTMLDivElement>()
const nodeTaskCountChart = ref<HTMLDivElement>()

let chart1: echarts.ECharts | null = null
let chart2: echarts.ECharts | null = null

async function loadAll() {
  if (!processKey.value) {
    const [userRes]: any = await Promise.all([getUserEfficiencyStats()])
    userEfficiency.value = userRes.data || []
    return
  }
  const [nodeRes, userRes, procRes]: any = await Promise.all([
    getNodeDurationStats(processKey.value),
    getUserEfficiencyStats(),
    getProcessDurationStats(processKey.value),
  ])
  nodeStats.value = nodeRes.data || []
  userEfficiency.value = userRes.data || []
  processStats.value = procRes.data || {}

  await nextTick()
  renderCharts()
}

function renderCharts() {
  if (!nodeStats.value.length) return

  // 节点平均耗时柱状图
  if (nodeDurationChart.value) {
    if (chart1) chart1.dispose()
    chart1 = echarts.init(nodeDurationChart.value)
    const avgMinutes = nodeStats.value.map((n: any) => Math.round((n.avgDuration || 0) / 60000))
    chart1.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: nodeStats.value.map((n: any) => n.activityName || n.activityId), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '分钟' },
      series: [{
        data: avgMinutes,
        type: 'bar',
        itemStyle: { color: '#409eff' },
        label: { show: true, position: 'top', formatter: '{c}分' },
      }],
    })
  }

  // 节点任务数量饼图
  if (nodeTaskCountChart.value) {
    if (chart2) chart2.dispose()
    chart2 = echarts.init(nodeTaskCountChart.value)
    chart2.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: nodeStats.value.map((n: any) => ({
          name: n.activityName || n.activityId,
          value: n.taskCount || 0,
        })),
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
      }],
    })
  }
}
</script>
