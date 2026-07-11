<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="日期范围" prop="dateRange">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="warning" plain :icon="Refresh" @click="getList">刷新</el-button>
    </RightToolbar>

    <el-alert v-if="loadError" :title="loadError" type="error" show-icon closable style="margin-bottom: 14px" @close="loadError = ''" />

    <div v-loading="loading" class="dashboard-body">
      <div class="score-card" :class="scoreClass">
        <div class="score-label">复盘质量分</div>
        <div class="score-value">{{ Number(dashboard.qualityScore || 0).toFixed(0) }}</div>
        <div class="score-hint">满分 100</div>
      </div>

      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-label">复盘任务总数</div>
          <div class="metric-value">{{ dashboard.totalTaskCount || 0 }}</div>
        </div>
        <div class="metric-card success">
          <div class="metric-label">已完成</div>
          <div class="metric-value">{{ dashboard.doneTaskCount || 0 }}</div>
        </div>
        <div class="metric-card danger">
          <div class="metric-label">逾期未关闭</div>
          <div class="metric-value">{{ dashboard.overdueTaskCount || 0 }}</div>
        </div>
        <div class="metric-card warning">
          <div class="metric-label">逾期比例</div>
          <div class="metric-value">{{ Number(dashboard.overdueRatio || 0).toFixed(1) }}%</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">平均首次响应(h)</div>
          <div class="metric-value">{{ Number(dashboard.avgFirstResponseHours || 0).toFixed(1) }}</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">平均关闭时长(h)</div>
          <div class="metric-value">{{ Number(dashboard.avgCloseHours || 0).toFixed(1) }}</div>
        </div>
        <div class="metric-card warning">
          <div class="metric-label">未填备注已完成</div>
          <div class="metric-value">{{ dashboard.noNoteDoneCount || 0 }}</div>
        </div>
      </div>

      <div class="suggestions-panel">
        <h3 class="section-title">改进建议</h3>
        <div v-if="dashboard.suggestions && dashboard.suggestions.length > 0" class="suggestion-list">
          <div v-for="(s, i) in dashboard.suggestions" :key="i" class="suggestion-item">
            <el-icon class="suggestion-icon"><WarningFilled /></el-icon>
            <span>{{ s }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无改进建议，复盘质量良好" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh, WarningFilled } from '@element-plus/icons-vue'
import { getReviewQualityDashboard } from '@/api/finance/reviewQuality'
import RightToolbar from '@/components/RightToolbar/index.vue'

const loading = ref(false)
const loadError = ref('')
const showSearch = ref(true)
const queryFormRef = ref()
const dateRange = ref<string[]>([])

const queryParams = reactive({})

const dashboard = reactive({
  qualityScore: 0,
  totalTaskCount: 0,
  doneTaskCount: 0,
  overdueTaskCount: 0,
  overdueRatio: 0,
  avgFirstResponseHours: 0,
  avgCloseHours: 0,
  noNoteDoneCount: 0,
  suggestions: [] as string[],
})

const scoreClass = computed(() => {
  const s = Number(dashboard.qualityScore || 0)
  if (s >= 80) return 'score-good'
  if (s >= 60) return 'score-watch'
  return 'score-risk'
})

async function getList() {
  loading.value = true
  loadError.value = ''
  try {
    const data: any = {
      startDate: dateRange.value && dateRange.value[0] ? dateRange.value[0] : null,
      endDate: dateRange.value && dateRange.value[1] ? dateRange.value[1] : null,
    }
    const res: any = await getReviewQualityDashboard(data)
    const d = res.data || {}
    Object.assign(dashboard, {
      qualityScore: Number(d.qualityScore || 0),
      totalTaskCount: Number(d.totalTaskCount || 0),
      doneTaskCount: Number(d.doneTaskCount || 0),
      overdueTaskCount: Number(d.overdueTaskCount || 0),
      overdueRatio: Number(d.overdueRatio || 0),
      avgFirstResponseHours: Number(d.avgFirstResponseHours || 0),
      avgCloseHours: Number(d.avgCloseHours || 0),
      noNoteDoneCount: Number(d.noNoteDoneCount || 0),
      suggestions: Array.isArray(d.suggestions) ? d.suggestions : [],
    })
  } catch (e: any) {
    loadError.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  getList()
}

function resetQuery() {
  dateRange.value = []
  getList()
}

onMounted(getList)
</script>

<style scoped>
.dashboard-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.score-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  border-radius: 10px;
  color: #fff;
}
.score-card.score-good {
  background: linear-gradient(135deg, #0ea573, #10b981);
}
.score-card.score-watch {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}
.score-card.score-risk {
  background: linear-gradient(135deg, #d4456a, #ef4444);
}
.score-label {
  font-size: 14px;
  opacity: 0.9;
}
.score-value {
  font-size: 48px;
  font-weight: 700;
  line-height: 1.2;
}
.score-hint {
  font-size: 12px;
  opacity: 0.8;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
}
.metric-card {
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.metric-card.success {
  background: rgba(14, 165, 115, 0.06);
  border-color: rgba(14, 165, 115, 0.2);
}
.metric-card.danger {
  background: rgba(212, 69, 106, 0.06);
  border-color: rgba(212, 69, 106, 0.2);
}
.metric-card.warning {
  background: rgba(217, 119, 6, 0.06);
  border-color: rgba(217, 119, 6, 0.2);
}
.metric-label {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}
.metric-value {
  color: #1e293b;
  font-size: 22px;
  font-weight: 700;
}

.suggestions-panel {
  padding: 18px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}
.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
  padding: 8px 12px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}
.suggestion-icon {
  color: #d97706;
  flex-shrink: 0;
  margin-top: 2px;
}
</style>
