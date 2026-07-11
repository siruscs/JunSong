<template>
  <div class="app-container">
    <RightToolbar v-model:showSearch="showSearch" @query="fetchDashboard">
      <el-button type="warning" :icon="Refresh" @click="fetchDashboard">刷新</el-button>
    </RightToolbar>

    <div v-loading="loading">
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />

      <template v-if="dashboard">
        <el-row :gutter="16" style="margin-bottom: 20px">
          <el-col :span="6">
            <el-card shadow="never">
              <template #header><span>整体状态</span></template>
              <el-tag :type="statusType" size="large">{{ statusLabel }}</el-tag>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never">
              <template #header><span>问题总数</span></template>
              <span class="metric-number">{{ dashboard.totalIssueCount }}</span>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="never">
              <template #header><span style="color: #f56c6c">HIGH</span></template>
              <span class="metric-number" style="color: #f56c6c">{{ dashboard.highIssueCount }}</span>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="never">
              <template #header><span style="color: #e6a23c">MEDIUM</span></template>
              <span class="metric-number" style="color: #e6a23c">{{ dashboard.mediumIssueCount }}</span>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="never">
              <template #header><span style="color: #909399">LOW</span></template>
              <span class="metric-number" style="color: #909399">{{ dashboard.lowIssueCount }}</span>
            </el-card>
          </el-col>
        </el-row>

        <el-alert
          v-if="dashboard.dbErrorCount > 0"
          type="error"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        >
          <template #title>
            有 {{ dashboard.dbErrorCount }} 项数据质量查询失败，结果可能不完整
          </template>
          <template #default>
            <ul style="margin: 4px 0 0 16px; padding: 0">
              <li v-for="(err, idx) in dashboard.dbErrors" :key="idx">{{ err }}</li>
            </ul>
          </template>
        </el-alert>

        <el-empty v-if="dashboard.issues.length === 0 && dashboard.dbErrorCount === 0" description="暂无数据质量问题" />
        <el-empty v-else-if="dashboard.issues.length === 0 && dashboard.dbErrorCount > 0" description="无法检测数据质量（查询异常）" />

        <el-table v-if="dashboard.issues.length > 0" :data="dashboard.issues" stripe border>
          <el-table-column label="问题类型" prop="issueType" min-width="200" />
          <el-table-column label="模块" prop="module" width="100">
            <template #default="{ row }">
              <el-tag>{{ row.module }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="严重级别" prop="severity" width="100">
            <template #default="{ row }">
              <el-tag :type="severityTagType(row.severity)">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="问题数量" prop="issueCount" width="100" align="center" />
          <el-table-column label="来源表" prop="sourceTables" min-width="180" show-overflow-tooltip />
          <el-table-column label="原因" prop="reason" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.drilldownPath"
                type="primary"
                link
                @click="goToDrilldown(row.drilldownPath)"
              >查看</el-button>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getDataQualityDashboard, type DataQualityDashboard } from '@/api/system/dataQuality'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const showSearch = ref(true)
const dashboard = ref<DataQualityDashboard | null>(null)

const statusType = computed(() => {
  if (!dashboard.value) return 'info'
  const s = dashboard.value.status
  if (s === 'HEALTHY') return 'success'
  if (s === 'WARN') return 'warning'
  if (s === 'ERROR') return 'danger'
  return 'danger'
})

const statusLabel = computed(() => {
  if (!dashboard.value) return '—'
  const s = dashboard.value.status
  if (s === 'HEALTHY') return '健康'
  if (s === 'WARN') return '警告'
  if (s === 'ERROR') return '查询异常'
  return '阻断'
})

function severityTagType(severity: string) {
  if (severity === 'HIGH') return 'danger'
  if (severity === 'MEDIUM') return 'warning'
  return 'info'
}

function goToDrilldown(path: string) {
  if (path) router.push(path)
}

async function fetchDashboard() {
  loading.value = true
  error.value = ''
  try {
    const res: any = await getDataQualityDashboard()
    dashboard.value = res.data
  } catch (e: any) {
    error.value = e?.message || '加载数据质量看板失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})
</script>

<style scoped>
.metric-number {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.4;
}
.text-muted {
  color: #c0c4cc;
}
</style>
