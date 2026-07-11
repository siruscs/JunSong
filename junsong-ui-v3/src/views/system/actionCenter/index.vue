<template>
  <div class="app-container">
    <RightToolbar v-model:showSearch="showSearch" @query="fetchData">
      <el-button type="warning" :icon="Refresh" @click="fetchData">刷新</el-button>
    </RightToolbar>

    <div v-loading="loading">
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />

      <template v-if="actions !== null">
        <el-row :gutter="12" style="margin-bottom: 16px">
          <el-col :span="6">
            <el-card shadow="never" class="stat-card">
              <div class="stat-value" style="color: #e6a23c">{{ todayPendingCount }}</div>
              <div class="stat-label">今日待做</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never" class="stat-card">
              <div class="stat-value" style="color: #f56c6c">{{ overdueCount }}</div>
              <div class="stat-label">逾期未做</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never" class="stat-card">
              <div class="stat-value" style="color: #409eff">{{ effectPendingCount }}</div>
              <div class="stat-label">效果待复盘</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never" class="stat-card">
              <div class="stat-value" style="color: #f56c6c">{{ touchFailCount24h }}</div>
              <div class="stat-label">近24h触达失败</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header><span>筛选</span></template>
          <el-form :inline="true" :model="filterForm" size="small">
            <el-form-item label="来源">
              <el-select v-model="filterForm.sourceType" clearable placeholder="全部" style="width: 160px">
                <el-option label="财务催收" value="FINANCE_RECEIVABLE" />
                <el-option label="会员增长" value="MEMBER_GROWTH" />
                <el-option label="库存健康" value="STOCK_HEALTH" />
                <el-option label="系统治理" value="SYSTEM_GOVERNANCE" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="filterForm.status" clearable placeholder="全部" style="width: 140px">
                <el-option label="待处理" value="PENDING" />
                <el-option label="处理中" value="IN_PROGRESS" />
                <el-option label="已完成" value="DONE" />
                <el-option label="已忽略" value="IGNORED" />
                <el-option label="效果待复盘" value="EFFECT_PENDING" />
              </el-select>
            </el-form-item>
            <el-form-item label="优先级">
              <el-select v-model="filterForm.priority" clearable placeholder="全部" style="width: 120px">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="filterForm.onlyToday">仅今天</el-checkbox>
              <el-checkbox v-model="filterForm.onlyOverdue">仅逾期</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchData">查询</el-button>
              <el-button @click="resetFilter">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header><span>动作列表</span></template>
          <el-table :data="filteredActions" size="small" border style="width: 100%">
            <el-table-column prop="sourceType" label="来源" width="120">
              <template #default="{ row }">
                <el-tag :type="sourceTagType(row.sourceType)" size="small">{{ sourceLabel(row.sourceType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" show-overflow-tooltip min-width="160" />
            <el-table-column prop="priority" label="优先级" width="80">
              <template #default="{ row }">
                <el-tag :type="priorityTagType(row.priority)" size="small">{{ row.priority }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ownerName" label="负责人" width="90" />
            <el-table-column prop="deptName" label="门店" width="100" show-overflow-tooltip />
            <el-table-column label="最近触达" width="120">
              <template #default="{ row }">
                <span v-if="row.latestTouchStatus" :style="{ color: touchColor(row.latestTouchStatus) }">
                  {{ row.latestTouchStatus }}
                </span>
                <span v-else style="color: #c0c4cc">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="touchCount24h" label="24h触达" width="80" />
            <el-table-column label="入口" width="80">
              <template #default="{ row }">
                <el-button v-if="row.drilldownPath" type="primary" link size="small" @click="goDrilldown(row.drilldownPath)">
                  查看
                </el-button>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="warning"
                  size="small"
                  :disabled="!row.touchable"
                  :loading="touching[row.actionId]"
                  v-hasPermi="['system:action-center:touch']"
                  @click="confirmTouch(row)"
                >
                  触达
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="filteredActions.length === 0" description="暂无动作" :image-size="80" />
        </el-card>
      </template>

      <el-empty v-else-if="!loading && !error" description="暂无动作数据" :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  listActionCenter,
  touchAction,
  type ActionCenterItem,
} from '@/api/system/actionCenter'

const showSearch = ref(true)
const loading = ref(false)
const error = ref('')
const actions = ref<ActionCenterItem[] | null>(null)
const touching = reactive<Record<string, boolean>>({})

const filterForm = reactive({
  sourceType: '',
  status: '',
  priority: '',
  onlyToday: false,
  onlyOverdue: false,
})

const filteredActions = computed(() => {
  if (!actions.value) return []
  let result = actions.value
  if (filterForm.sourceType) result = result.filter((a) => a.sourceType === filterForm.sourceType)
  if (filterForm.status) result = result.filter((a) => a.status === filterForm.status)
  if (filterForm.priority) result = result.filter((a) => a.priority === filterForm.priority)
  if (filterForm.onlyOverdue) result = result.filter((a) => a.status === 'PENDING' && isOverdue(a))
  if (filterForm.onlyToday) result = result.filter((a) => isToday(a.dueDate))
  return result
})

const todayPendingCount = computed(() => {
  if (!actions.value) return 0
  return actions.value.filter((a) => a.status === 'PENDING' && isToday(a.dueDate)).length
})

const overdueCount = computed(() => {
  if (!actions.value) return 0
  return actions.value.filter((a) => a.status === 'PENDING' && isOverdue(a)).length
})

const effectPendingCount = computed(() => {
  if (!actions.value) return 0
  return actions.value.filter((a) => a.status === 'EFFECT_PENDING').length
})

const touchFailCount24h = computed(() => {
  if (!actions.value) return 0
  return actions.value.filter((a) => a.latestTouchStatus === 'FAILED').length
})

function isToday(dateStr?: string): boolean {
  if (!dateStr) return true
  return dateStr === new Date().toISOString().slice(0, 10)
}

function isOverdue(a: ActionCenterItem): boolean {
  if (!a.dueDate) return false
  return a.dueDate < new Date().toISOString().slice(0, 10)
}

function sourceLabel(s: string): string {
  const map: Record<string, string> = {
    FINANCE_RECEIVABLE: '财务催收',
    MEMBER_GROWTH: '会员增长',
    STOCK_HEALTH: '库存健康',
    SYSTEM_GOVERNANCE: '系统治理',
  }
  return map[s] || s
}

function sourceTagType(s: string): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    FINANCE_RECEIVABLE: 'warning',
    MEMBER_GROWTH: 'success',
    STOCK_HEALTH: 'danger',
    SYSTEM_GOVERNANCE: 'info',
  }
  return map[s] || 'info'
}

function priorityTagType(p: string): 'danger' | 'warning' | 'info' {
  if (p === 'HIGH') return 'danger'
  if (p === 'MEDIUM') return 'warning'
  return 'info'
}

function statusTagType(s: string): 'info' | 'warning' | 'success' | 'primary' | 'danger' {
  const map: Record<string, 'info' | 'warning' | 'success' | 'primary' | 'danger'> = {
    PENDING: 'warning',
    IN_PROGRESS: 'primary',
    DONE: 'success',
    IGNORED: 'info',
    EFFECT_PENDING: 'danger',
  }
  return map[s] || 'info'
}

function statusLabel(s: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    IN_PROGRESS: '处理中',
    DONE: '已完成',
    IGNORED: '已忽略',
    EFFECT_PENDING: '效果待复盘',
  }
  return map[s] || s
}

function touchColor(s: string): string {
  if (s === 'SUCCESS' || s === 'DRY_RUN') return '#67c23a'
  if (s === 'FAILED') return '#f56c6c'
  if (s.startsWith('SKIPPED')) return '#e6a23c'
  if (s === 'DISABLED') return '#909399'
  return '#606266'
}

function goDrilldown(path: string) {
  if (path) window.location.href = path
}

function resetFilter() {
  filterForm.sourceType = ''
  filterForm.status = ''
  filterForm.priority = ''
  filterForm.onlyToday = false
  filterForm.onlyOverdue = false
  fetchData()
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const res: any = await listActionCenter({
      sourceType: filterForm.sourceType || undefined,
      status: filterForm.status || undefined,
      priority: filterForm.priority || undefined,
      onlyToday: filterForm.onlyToday || undefined,
      onlyOverdue: filterForm.onlyOverdue || undefined,
    })
    actions.value = res.data || []
  } catch (e: any) {
    error.value = e.message || '加载动作列表失败'
    actions.value = []
  } finally {
    loading.value = false
  }
}

function confirmTouch(action: ActionCenterItem) {
  ElMessageBox.confirm(
    `确认触达动作「${action.title}」吗？\n通道：企业微信群机器人\nDEV 环境默认 dry-run，不会真实发送。`,
    '触达确认',
    {
      confirmButtonText: '确认触达',
      cancelButtonText: '取消',
      type: 'warning',
    },
  )
    .then(async () => {
      touching[action.actionId] = true
      try {
        const res: any = await touchAction(action.actionId, {
          channel: 'WEWORK_BOT',
          targetType: 'GROUP',
          targetRef: '',
          message: '',
          force: false,
        })
        const result = res.data
        if (result && result.touchStatus === 'FAILED') {
          ElMessage.error(`触达失败：${result.message || ''}`)
        } else if (result && result.touchStatus === 'SKIPPED_DUPLICATE') {
          ElMessage.warning(`已跳过：30分钟内重复触达`)
        } else if (result && result.touchStatus === 'SKIPPED_RATE_LIMIT') {
          ElMessage.warning(`已跳过：触达频率超限`)
        } else if (result && result.touchStatus === 'DISABLED') {
          ElMessage.info(`触达通道未启用`)
        } else if (result && result.touchStatus === 'DRY_RUN') {
          ElMessage.success(`Dry-run 模拟触达成功（未真实发送）`)
        } else {
          ElMessage.success(`触达完成：${result?.touchStatus || ''}`)
        }
        await fetchData()
      } catch (e: any) {
        ElMessage.error(`触达异常：${e.message || ''}`)
      } finally {
        touching[action.actionId] = false
      }
    })
    .catch(() => {})
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 8px 0;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  line-height: 1.4;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
