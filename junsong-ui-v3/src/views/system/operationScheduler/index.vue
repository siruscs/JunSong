<template>
  <div class="app-container">
    <RightToolbar v-model:showSearch="showSearch" @query="fetchDashboard">
      <el-button type="warning" :icon="Refresh" @click="fetchDashboard">刷新</el-button>
    </RightToolbar>

    <div v-loading="loading">
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />

      <el-alert
        v-if="dashboard && dashboard.failureCount24h > 0"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      >
        <template #title>
          最近 24 小时有 {{ dashboard.failureCount24h }} 个调度任务失败/部分失败，请关注
        </template>
      </el-alert>

      <template v-if="dashboard">
        <el-row :gutter="16" style="margin-bottom: 20px">
          <el-col v-for="job in jobDefinitions" :key="job.code" :span="6">
            <el-card shadow="never" class="job-card">
              <template #header>
                <div class="job-header">
                  <span>{{ job.name }}</span>
                  <el-tag v-if="getJobLog(job.code)" :type="statusTagType(getJobLog(job.code)!.status)" size="small">
                    {{ getJobLog(job.code)!.status }}
                  </el-tag>
                  <el-tag v-else type="info" size="small">未执行</el-tag>
                </div>
              </template>
              <div v-if="getJobLog(job.code)" class="job-detail">
                <div class="detail-row">
                  <span class="detail-label">触发方式：</span>
                  <span>{{ getJobLog(job.code)!.triggerType }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">开始时间：</span>
                  <span>{{ getJobLog(job.code)!.startedAt || '-' }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">耗时：</span>
                  <span>{{ getJobLog(job.code)!.durationMs != null ? getJobLog(job.code)!.durationMs + ' ms' : '-' }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">影响行数：</span>
                  <span>{{ getJobLog(job.code)!.affectedRows }}</span>
                </div>
                <div v-if="getJobLog(job.code)!.resultSummary" class="detail-row detail-summary">
                  {{ getJobLog(job.code)!.resultSummary }}
                </div>
                <div v-if="getJobLog(job.code)!.errorMessage" class="detail-row detail-error">
                  {{ getJobLog(job.code)!.errorMessage }}
                </div>
              </div>
              <div v-else class="job-empty">暂无执行记录</div>
              <div class="job-action">
                <el-button
                  type="primary"
                  size="small"
                  :loading="triggering[job.code]"
                  :disabled="triggering[job.code]"
                  v-hasPermi="['system:operation-scheduler:trigger']"
                  @click="confirmTrigger(job)"
                >
                  手动触发
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never">
          <template #header><span>最近失败列表</span></template>
          <el-table v-if="dashboard.recentFailures && dashboard.recentFailures.length > 0" :data="dashboard.recentFailures" size="small" border>
            <el-table-column prop="jobCode" label="任务编码" width="240" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startedAt" label="开始时间" width="180" />
            <el-table-column prop="resultSummary" label="结果摘要" show-overflow-tooltip />
            <el-table-column prop="errorMessage" label="错误信息" show-overflow-tooltip />
          </el-table>
          <el-empty v-else description="最近 24 小时无失败记录" :image-size="80" />
        </el-card>
      </template>

      <el-empty v-else-if="!loading && !error" description="暂无调度数据" :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getOperationSchedulerDashboard,
  triggerOperationScheduler,
  type OperationScheduleDashboard,
  type OperationScheduleLog,
} from '@/api/system/operationScheduler'

const showSearch = ref(true)
const loading = ref(false)
const error = ref('')
const dashboard = ref<OperationScheduleDashboard | null>(null)
const triggering = reactive<Record<string, boolean>>({})

const jobDefinitions = [
  { code: 'R21_CASHFLOW_FORECAST_SNAPSHOT', name: '现金流预测快照' },
  { code: 'R21_MEMBER_GROWTH_EFFECT_BACKFILL', name: '会员增长效果回填' },
  { code: 'R21_STOCK_DAILY_SNAPSHOT', name: '库存每日快照' },
  { code: 'R21_OPERATION_MEMO_DRAFT', name: '经营纪要草稿' },
]

function getJobLog(jobCode: string): OperationScheduleLog | undefined {
  return dashboard.value?.recentLogs?.find((log) => log.jobCode === jobCode)
}

function statusTagType(status: string): 'success' | 'danger' | 'warning' | 'info' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'PARTIAL':
      return 'warning'
    case 'SKIPPED':
      return 'info'
    default:
      return 'info'
  }
}

async function fetchDashboard() {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = (await getOperationSchedulerDashboard() as any).data
  } catch (e: any) {
    error.value = e.message || '加载调度看板失败'
  } finally {
    loading.value = false
  }
}

function confirmTrigger(job: { code: string; name: string }) {
  ElMessageBox.confirm(`确认手动触发任务「${job.name}」吗？`, '二次确认', {
    confirmButtonText: '确认触发',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      triggering[job.code] = true
      try {
        const res: any = await triggerOperationScheduler(job.code)
        const result = res.data
        if (result.status === 'FAILED') {
          ElMessage.error(`${job.name} 触发失败：${result.errorMessage || ''}`)
        } else if (result.status === 'PARTIAL') {
          ElMessage.warning(`${job.name} 部分成功：${result.resultSummary || ''}`)
        } else {
          ElMessage.success(`${job.name} 触发完成：${result.status}`)
        }
        await fetchDashboard()
      } catch (e: any) {
        ElMessage.error(`${job.name} 触发异常：${e.message || ''}`)
      } finally {
        triggering[job.code] = false
      }
    })
    .catch(() => {})
}

onMounted(() => {
  fetchDashboard()
})
</script>

<style scoped>
.job-card {
  margin-bottom: 16px;
}
.job-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.job-detail {
  font-size: 13px;
  line-height: 1.8;
}
.detail-row {
  color: #606266;
}
.detail-label {
  color: #909399;
}
.detail-summary {
  color: #409eff;
  margin-top: 4px;
}
.detail-error {
  color: #f56c6c;
  margin-top: 4px;
  word-break: break-all;
}
.job-empty {
  color: #c0c4cc;
  font-size: 13px;
  padding: 8px 0;
}
.job-action {
  margin-top: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
</style>
