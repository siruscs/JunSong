<template>
  <div class="app-container review-task-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">复盘任务管理</h2>
      </div>
      <el-button type="primary" :icon="MagicStick" :loading="generateLoading" @click="handleGenerate">生成复盘任务</el-button>
      <el-button type="warning" :icon="MagicStick" :loading="receivableGenerateLoading" @click="openReceivableGenerateDialog">生成催收任务</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
        <el-form-item label="门店" prop="deptIds">
          <el-select
            v-model="queryParams.deptIds"
            placeholder="请选择门店"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            style="width: 280px"
          >
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围" prop="dateRange">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="全部" name="" />
          <el-tab-pane label="待处理" name="PENDING" />
          <el-tab-pane label="处理中" name="IN_PROGRESS" />
          <el-tab-pane label="已完成" name="DONE" />
          <el-tab-pane label="已忽略" name="IGNORED" />
        </el-tabs>
        <el-switch
          v-model="includeArchived"
          active-text="包含归档"
          inactive-text=""
          style="margin-left: 16px; margin-bottom: 12px;"
          @change="getList"
        />
      </div>

      <el-table v-loading="loading" :data="taskList" stripe border style="width: 100%" empty-text="暂无复盘任务">
        <el-table-column label="门店" align="center" prop="deptName" min-width="120" />
        <el-table-column label="任务类型" align="center" prop="taskType" width="130">
          <template #default="scope">
            <span>{{ taskTypeLabel(scope.row.taskType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="严重级别" align="center" prop="severity" width="100">
          <template #default="scope">
            <el-tag :type="severityTagType(scope.row.severity)" size="small">
              {{ severityLabel(scope.row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="影响金额" align="center" prop="impactAmount" width="120">
          <template #default="scope">
            <span class="money-danger">&yen;{{ formatMoney(scope.row.impactAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="触发原因" align="center" prop="reason" min-width="180" show-overflow-tooltip />
        <el-table-column label="建议动作" align="center" prop="suggestion" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="small">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" align="center" prop="handlerName" width="100" />
        <el-table-column label="更新时间" align="center" prop="updateTime" width="170">
          <template #default="scope">
            <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" fixed="right" width="270">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'PENDING'"
              link
              type="primary"
              size="small"
              @click="handleMarkInProgress(scope.row)"
            >
              处理中
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING' || scope.row.status === 'IN_PROGRESS'"
              link
              type="success"
              size="small"
              @click="handleOpenDoneDialog(scope.row)"
            >
              完成
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING' || scope.row.status === 'IN_PROGRESS'"
              link
              type="info"
              size="small"
              @click="handleOpenIgnoreDialog(scope.row)"
            >
              忽略
            </el-button>
            <el-button
              v-if="scope.row.targetRoute"
              link
              type="warning"
              size="small"
              @click="handleNavigate(scope.row)"
            >
              跳转
            </el-button>
            <el-button
              v-if="scope.row.status === 'DONE'"
              link
              type="danger"
              size="small"
              @click="handleViewEffect(scope.row)"
            >
              效果
            </el-button>
            <el-button
              v-if="scope.row.status === 'DONE'"
              link
              type="success"
              size="small"
              @click="handleOpenKnowledgeDialog(scope.row)"
            >
              沉淀知识
            </el-button>
            <el-button
              v-if="['DONE', 'IGNORED'].includes(scope.row.status)"
              link
              type="warning"
              size="small"
              v-hasPermi="['finance:reviewTask:edit']"
              @click="handleOpenReopenDialog(scope.row)"
            >
              重开
            </el-button>
            <el-button
              link
              type="info"
              size="small"
              @click="openTaskLogs(scope.row)"
            >
              轨迹
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog v-model="doneDialogVisible" title="标记完成" width="500px" append-to-body>
      <el-form ref="doneFormRef" :model="doneForm" :rules="doneRules" label-width="90px">
        <el-form-item label="处理说明" prop="handlerNote">
          <el-input
            v-model="doneForm.handlerNote"
            type="textarea"
            placeholder="请输入处理说明（至少5个字符）"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="doneLoading" @click="submitDone">确 定</el-button>
          <el-button @click="doneDialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="ignoreDialogVisible" title="标记忽略" width="500px" append-to-body>
      <el-form ref="ignoreFormRef" :model="ignoreForm" :rules="ignoreRules" label-width="90px">
        <el-form-item label="忽略原因" prop="ignoreReason">
          <el-input
            v-model="ignoreForm.ignoreReason"
            type="textarea"
            placeholder="请输入忽略原因（至少5个字符）"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="ignoreLoading" @click="submitIgnore">确 定</el-button>
          <el-button @click="ignoreDialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 处理轨迹抽屉 -->
    <el-drawer v-model="logDrawerVisible" title="处理轨迹" size="520px" append-to-body>
      <div v-if="currentTaskForLogs" class="log-task-info">
        <el-tag size="small" :type="severityTagType(currentTaskForLogs.severity)">{{ severityLabel(currentTaskForLogs.severity) }}</el-tag>
        <span class="log-task-type">{{ taskTypeLabel(currentTaskForLogs.taskType) }}</span>
        <span class="log-task-dept">{{ currentTaskForLogs.deptName }}</span>
      </div>
      <el-divider />
      <div v-loading="logLoading">
        <el-timeline v-if="currentTaskLogs.length > 0">
          <el-timeline-item
            v-for="log in currentTaskLogs"
            :key="log.logId"
            :timestamp="parseTime(log.actionTime, '{y}-{m}-{d} {h}:{i}:{s}')"
            placement="top"
          >
            <div class="log-item">
              <el-tag
                size="small"
                :type="log.actionType === 'DONE' ? 'success' : log.actionType === 'IN_PROGRESS' ? '' : log.actionType === 'REOPEN' ? 'warning' : 'info'"
              >
                {{ log.actionType === 'IN_PROGRESS' ? '处理中' : log.actionType === 'DONE' ? '完成' : log.actionType === 'IGNORED' ? '忽略' : log.actionType === 'REOPEN' ? '重开' : log.actionType }}
              </el-tag>
              <span class="log-handler">{{ log.handlerName || '-' }}</span>
            </div>
            <p v-if="log.handlerNote" class="log-note">{{ log.handlerNote }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无处理记录" />
      </div>

      <!-- 推荐经验 -->
      <el-divider content-position="left">推荐经验</el-divider>
      <div v-loading="recommendationLoading">
        <div v-if="recommendedKnowledge.length > 0">
          <div
            v-for="item in recommendedKnowledge"
            :key="item.knowledgeId"
            style="padding: 10px 12px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; margin-bottom: 8px;"
          >
            <div style="font-weight: 600; font-size: 13px; color: #92400e; margin-bottom: 4px;">{{ item.title }}</div>
            <div style="font-size: 12px; color: #78716c; margin-bottom: 4px;">
              <el-tag size="small" type="warning" style="margin-right: 6px;">{{ taskTypeLabel(item.problemType) }}</el-tag>
              {{ item.sourceHandlerName || '-' }}
            </div>
            <div v-if="item.actionTaken" style="font-size: 12px; color: #44403c; line-height: 1.5;">{{ item.actionTaken }}</div>
            <div v-if="item.resultSummary" style="font-size: 12px; color: #166534; margin-top: 4px;">效果: {{ item.resultSummary }}</div>
          </div>
        </div>
        <el-empty v-else-if="!recommendationLoading" description="暂无推荐经验" :image-size="40" />
      </div>
    </el-drawer>

    <!-- 动作效果评估抽屉 -->
    <el-drawer v-model="effectDrawerVisible" title="动作效果评估" size="480px" append-to-body>
      <div v-loading="effectLoading">
        <template v-if="effectData">
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px;">
            <el-radio-group v-model="effectWindowDays" size="small" @change="handleEffectWindowChange">
              <el-radio-button :value="7">7 天</el-radio-button>
              <el-radio-button :value="14">14 天</el-radio-button>
            </el-radio-group>
            <span style="color: #667085; font-size: 13px;">{{ effectData.taskTitle }}</span>
          </div>

          <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 20px;">
            <div
              style="display: flex; flex-direction: column; align-items: center; justify-content: center; width: 80px; height: 80px; border-radius: 10px; color: #fff; font-weight: 700;"
              :style="{ background: effectData.effectLevel === 'GOOD' ? '#10b981' : effectData.effectLevel === 'WATCH' ? '#f59e0b' : '#ef4444' }"
            >
              <div style="font-size: 28px;">{{ effectData.effectScore }}</div>
              <div style="font-size: 11px; opacity: 0.9;">效果分</div>
            </div>
            <el-tag
              :type="effectData.effectLevel === 'GOOD' ? 'success' : effectData.effectLevel === 'WATCH' ? 'warning' : 'danger'"
              size="large"
            >
              {{ effectData.effectLevel === 'GOOD' ? '改善明显' : effectData.effectLevel === 'WATCH' ? '观察中' : '未改善' }}
            </el-tag>
          </div>

          <el-descriptions :column="1" border size="small" style="margin-bottom: 16px;">
            <el-descriptions-item label="销售额">
              {{ formatMoney(effectData.beforeSales) }} → {{ formatMoney(effectData.afterSales) }}
              <span :style="{ color: effectData.salesChangeRate > 0 ? '#10b981' : '#ef4444', marginLeft: '8px' }">
                {{ effectData.salesChangeRate > 0 ? '+' : '' }}{{ effectData.salesChangeRate }}%
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="费用额">
              {{ formatMoney(effectData.beforeExpense) }} → {{ formatMoney(effectData.afterExpense) }}
              <span :style="{ color: effectData.expenseChangeRate < 0 ? '#10b981' : '#ef4444', marginLeft: '8px' }">
                {{ effectData.expenseChangeRate > 0 ? '+' : '' }}{{ effectData.expenseChangeRate }}%
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="经营利润">
              {{ formatMoney(effectData.beforeProfit) }} → {{ formatMoney(effectData.afterProfit) }}
              <span :style="{ color: effectData.profitChangeRate > 0 ? '#10b981' : '#ef4444', marginLeft: '8px' }">
                {{ effectData.profitChangeRate > 0 ? '+' : '' }}{{ effectData.profitChangeRate }}%
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="同类未完成任务">
              {{ effectData.beforeSimilarOpenCount ?? '-' }} → {{ effectData.afterSimilarOpenCount ?? '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="effectData.evidence && effectData.evidence.length > 0" style="margin-top: 12px;">
            <div style="font-weight: 600; margin-bottom: 8px; font-size: 14px;">证据</div>
            <div v-for="(e, i) in effectData.evidence" :key="i" style="padding: 6px 10px; background: #f0fdf4; border-radius: 6px; margin-bottom: 6px; font-size: 13px; color: #166534;">
              {{ e }}
            </div>
          </div>
        </template>
        <el-empty v-else-if="!effectLoading" description="暂无效果数据" />
      </div>
    </el-drawer>

    <el-dialog v-model="reopenDialogVisible" title="重开任务" width="500px" append-to-body>
      <el-form ref="reopenFormRef" :model="reopenForm" :rules="reopenRules" label-width="90px">
        <el-form-item label="重开原因" prop="reason">
          <el-input
            v-model="reopenForm.reason"
            type="textarea"
            placeholder="请输入重开原因（至少3个字符）"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="reopenLoading" @click="submitReopen">确 定</el-button>
          <el-button @click="reopenDialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="knowledgeDialogVisible" title="沉淀知识" width="560px" append-to-body>
      <div v-if="currentKnowledgeTask" class="knowledge-task-info">
        <span class="knowledge-task-title">{{ currentKnowledgeTask.title }}</span>
        <el-tag size="small" :type="severityTagType(currentKnowledgeTask.severity)">{{ severityLabel(currentKnowledgeTask.severity) }}</el-tag>
      </div>
      <el-divider />
      <el-form ref="knowledgeFormRef" :model="knowledgeForm" :rules="knowledgeRules" label-width="90px">
        <el-form-item label="问题类型" prop="problemType">
          <el-select v-model="knowledgeForm.problemType" placeholder="默认使用任务类型" clearable style="width: 100%">
            <el-option v-for="opt in knowledgeProblemTypes" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因分析" prop="rootCause">
          <el-input v-model="knowledgeForm.rootCause" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="可选" />
        </el-form-item>
        <el-form-item label="采取动作" prop="actionTaken">
          <el-input v-model="knowledgeForm.actionTaken" type="textarea" :rows="3" maxlength="500" show-word-limit
            :placeholder="currentKnowledgeTask?.handlerNote || '请输入实际采取的动作'" />
        </el-form-item>
        <el-form-item label="效果摘要" prop="resultSummary">
          <el-input v-model="knowledgeForm.resultSummary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="knowledgeLoading" @click="submitKnowledge">确 定</el-button>
        <el-button @click="knowledgeDialogVisible = false">取 消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="receivableGenerateVisible" title="生成催收任务" width="460px" append-to-body>
      <el-form :model="receivableGenerateForm" label-width="110px">
        <el-form-item label="门店">
          <el-select v-model="receivableGenerateForm.deptId" placeholder="请选择门店" clearable style="width: 100%">
            <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小账龄">
          <el-input-number v-model="receivableGenerateForm.minAgeDays" :min="1" :max="365" controls-position="right" />
        </el-form-item>
        <el-form-item label="最小未缴金额">
          <el-input-number v-model="receivableGenerateForm.minUnpaidAmount" :min="1" :precision="2" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receivableGenerateVisible = false">取 消</el-button>
        <el-button type="primary" :loading="receivableGenerateLoading" @click="submitReceivableGenerate">生 成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Refresh, Search } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/junsong'
import {
  generateReviewTasks,
  generateReceivableCollectionTasks,
  getTaskLogs,
  listReviewTasks,
  markDone,
  markIgnored,
  markInProgress,
  reopenReviewTask,
  getReviewTaskEffect,
} from '@/api/finance/reviewTask'
import { createKnowledgeFromTask, recommendKnowledgeForTask } from '@/api/finance/reviewKnowledge'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const generateLoading = ref(false)
const total = ref(0)
const taskList = ref<any[]>([])
const deptOptions = ref<any[]>([])
const activeTab = ref('')
const dateRange = ref<string[]>([])
const includeArchived = ref(false)
const queryFormRef = ref()
const doneFormRef = ref()
const ignoreFormRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  deptIds: [] as number[],
  status: undefined as string | undefined,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
})

const doneDialogVisible = ref(false)
const doneLoading = ref(false)
const currentDoneTask = ref<any>(null)
const doneForm = reactive({
  handlerNote: '',
})

const ignoreDialogVisible = ref(false)
const ignoreLoading = ref(false)
const currentIgnoreTask = ref<any>(null)
const ignoreForm = reactive({
  ignoreReason: '',
})

const receivableGenerateVisible = ref(false)
const receivableGenerateLoading = ref(false)
const receivableGenerateForm = reactive({
  deptId: undefined as number | undefined,
  minAgeDays: 14,
  minUnpaidAmount: 500,
})

function openReceivableGenerateDialog() {
  receivableGenerateVisible.value = true
  if (!receivableGenerateForm.deptId && queryParams.deptIds && queryParams.deptIds.length === 1) {
    receivableGenerateForm.deptId = queryParams.deptIds[0]
  } else if (!receivableGenerateForm.deptId && userStore.currentDeptId) {
    receivableGenerateForm.deptId = Number(userStore.currentDeptId)
  }
}

async function submitReceivableGenerate() {
  if (!receivableGenerateForm.deptId) {
    ElMessage.warning('请选择门店后再生成催收任务')
    return
  }
  receivableGenerateLoading.value = true
  try {
    const res: any = await generateReceivableCollectionTasks({
      deptId: receivableGenerateForm.deptId,
      minAgeDays: receivableGenerateForm.minAgeDays,
      minUnpaidAmount: receivableGenerateForm.minUnpaidAmount,
    })
    ElMessage.success(res?.msg || '催收复盘任务生成成功')
    receivableGenerateVisible.value = false
    getList()
  } finally {
    receivableGenerateLoading.value = false
  }
}

const reopenDialogVisible = ref(false)
const reopenLoading = ref(false)
const currentReopenTask = ref<any>(null)
const reopenForm = reactive({
  reason: '',
})
const reopenFormRef = ref()

const effectDrawerVisible = ref(false)
const effectLoading = ref(false)
const effectData = ref<any>(null)
const effectWindowDays = ref(7)

const logDrawerVisible = ref(false)
const currentTaskLogs = ref<any[]>([])
const currentTaskForLogs = ref<any>(null)
const logLoading = ref(false)
const recommendedKnowledge = ref<any[]>([])
const recommendationLoading = ref(false)

const taskTypeMap: Record<string, string> = {
  SALES_DROP: '销售下滑',
  EXPENSE_SPIKE: '费用异常',
  PROFIT_RATE_DROP: '利润率偏低',
  PENDING_VERIFY: '待核销费用偏高',
  PROFIT_SHARE_EXCEPTION: '分润异常',
  MEMBER_CONTRIBUTION_DROP: '会员贡献下降',
}

const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待处理', type: 'warning' },
  IN_PROGRESS: { label: '处理中', type: '' },
  DONE: { label: '已完成', type: 'success' },
  IGNORED: { label: '已忽略', type: 'info' },
}

const severityMap: Record<string, { label: string; type: string }> = {
  HIGH: { label: '高', type: 'danger' },
  MEDIUM: { label: '中', type: 'warning' },
  LOW: { label: '低', type: 'info' },
}

const doneRules = {
  handlerNote: [
    { required: true, message: '请输入处理说明', trigger: 'blur' },
    { min: 5, message: '处理说明至少5个字符', trigger: 'blur' },
  ],
}

const ignoreRules = {
  ignoreReason: [
    { required: true, message: '请输入忽略原因', trigger: 'blur' },
    { min: 5, message: '忽略原因至少5个字符', trigger: 'blur' },
  ],
}

const reopenRules = {
  reason: [
    { required: true, message: '请输入重开原因', trigger: 'blur' },
    { min: 3, message: '重开原因至少3个字符', trigger: 'blur' },
  ],
}

function taskTypeLabel(type: string) {
  return taskTypeMap[type] || type || '-'
}

function statusLabel(status: string) {
  return statusMap[status]?.label || status || '-'
}

function statusTagType(status: string) {
  return (statusMap[status]?.type || 'info') as any
}

function severityLabel(severity: string) {
  return severityMap[severity]?.label || severity || '-'
}

function severityTagType(severity: string) {
  return (severityMap[severity]?.type || 'info') as any
}

function formatMoney(value: any) {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed.toFixed(2) : '0.00'
}

function getDeptOptions() {
  deptOptions.value = userStore.depts || []
}

function handleDateRangeChange(val: string[] | null) {
  if (val && val.length === 2) {
    queryParams.startTime = val[0]
    queryParams.endTime = val[1]
  } else {
    queryParams.startTime = undefined
    queryParams.endTime = undefined
  }
}

function getList() {
  loading.value = true
  const params: Record<string, any> = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  if (queryParams.deptIds.length > 0) {
    params.deptIds = queryParams.deptIds.join(',')
  }
  if (queryParams.status) {
    params.status = queryParams.status
  }
  if (queryParams.startTime) {
    params.startTime = queryParams.startTime
  }
  if (queryParams.endTime) {
    params.endTime = queryParams.endTime
  }
  if (includeArchived.value) {
    params.includeArchived = true
  }
  listReviewTasks(params)
    .then((res: any) => {
      taskList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryFormRef.value?.resetFields?.()
  dateRange.value = []
  queryParams.deptIds = []
  queryParams.status = undefined
  queryParams.startTime = undefined
  queryParams.endTime = undefined
  activeTab.value = ''
  handleQuery()
}

function handleTabChange(tab: string) {
  queryParams.status = tab || undefined
  queryParams.pageNum = 1
  getList()
}

function handleMarkInProgress(row: any) {
  ElMessageBox.confirm('是否将该任务标记为处理中？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => markInProgress(row.taskId))
    .then(() => {
      ElMessage.success('已标记为处理中')
      getList()
    })
    .catch(() => {})
}

function handleOpenDoneDialog(row: any) {
  currentDoneTask.value = row
  doneForm.handlerNote = ''
  doneDialogVisible.value = true
}

function submitDone() {
  if (!doneFormRef.value || !currentDoneTask.value) return
  doneFormRef.value.validate((valid: boolean) => {
    if (!valid) return
    doneLoading.value = true
    markDone(currentDoneTask.value.taskId, { handlerNote: doneForm.handlerNote })
      .then(() => {
        ElMessage.success('已标记为完成')
        doneDialogVisible.value = false
        getList()
      })
      .finally(() => {
        doneLoading.value = false
      })
  })
}

function handleOpenIgnoreDialog(row: any) {
  currentIgnoreTask.value = row
  ignoreForm.ignoreReason = ''
  ignoreDialogVisible.value = true
}

function submitIgnore() {
  if (!ignoreFormRef.value || !currentIgnoreTask.value) return
  ignoreFormRef.value.validate((valid: boolean) => {
    if (!valid) return
    ignoreLoading.value = true
    markIgnored(currentIgnoreTask.value.taskId, { ignoreReason: ignoreForm.ignoreReason })
      .then(() => {
        ElMessage.success('已标记为忽略')
        ignoreDialogVisible.value = false
        getList()
      })
      .finally(() => {
        ignoreLoading.value = false
      })
  })
}

function handleNavigate(row: any) {
  if (row.targetRoute) {
    try {
      const query = row.targetParams ? JSON.parse(row.targetParams) : {}
      router.push({ path: row.targetRoute, query })
    } catch {
      router.push({ path: row.targetRoute })
    }
  }
}

function openTaskLogs(task: any) {
  currentTaskForLogs.value = task
  currentTaskLogs.value = []
  recommendedKnowledge.value = []
  logDrawerVisible.value = true
  logLoading.value = true
  getTaskLogs(task.taskId)
    .then((res: any) => {
      currentTaskLogs.value = res.data || []
    })
    .catch(() => {
      currentTaskLogs.value = []
    })
    .finally(() => {
      logLoading.value = false
    })
  // Fetch recommendations in parallel
  recommendationLoading.value = true
  recommendKnowledgeForTask(task.taskId)
    .then((res: any) => {
      recommendedKnowledge.value = res.data || []
    })
    .catch(() => {
      recommendedKnowledge.value = []
    })
    .finally(() => {
      recommendationLoading.value = false
    })
}

function handleGenerate() {
  ElMessageBox.confirm('是否根据当前筛选条件生成复盘任务？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      generateLoading.value = true
      const data: Record<string, any> = {}
      if (queryParams.deptIds.length > 0) {
        data.deptIds = queryParams.deptIds
      }
      if (queryParams.startTime) {
        data.startTime = queryParams.startTime
      }
      if (queryParams.endTime) {
        data.endTime = queryParams.endTime
      }
      return generateReviewTasks(data)
    })
    .then(() => {
      ElMessage.success('复盘任务生成成功')
      getList()
    })
    .catch(() => {})
    .finally(() => {
      generateLoading.value = false
    })
}

const knowledgeDialogVisible = ref(false)
const knowledgeLoading = ref(false)
const currentKnowledgeTask = ref<any>(null)
const knowledgeFormRef = ref()

const knowledgeForm = reactive({
  problemType: '',
  rootCause: '',
  actionTaken: '',
  resultSummary: '',
})

const knowledgeProblemTypes = [
  { label: '销售下滑', value: 'SALES_DROP' },
  { label: '费用异常', value: 'EXPENSE_SPIKE' },
  { label: '利润率偏低', value: 'PROFIT_RATE_DROP' },
  { label: '待核销费用偏高', value: 'PENDING_VERIFY' },
  { label: '分润异常', value: 'PROFIT_SHARE_EXCEPTION' },
  { label: '会员贡献下降', value: 'MEMBER_CONTRIBUTION_DROP' },
]

const knowledgeRules = {
  actionTaken: [
    { required: true, message: '请输入采取动作', trigger: 'blur' },
    { min: 3, message: '采取动作至少3个字符', trigger: 'blur' },
  ],
}

function handleOpenKnowledgeDialog(row: any) {
  currentKnowledgeTask.value = row
  knowledgeForm.problemType = ''
  knowledgeForm.rootCause = ''
  knowledgeForm.actionTaken = ''
  knowledgeForm.resultSummary = ''
  knowledgeDialogVisible.value = true
}

function submitKnowledge() {
  if (!knowledgeFormRef.value || !currentKnowledgeTask.value) return
  knowledgeFormRef.value.validate((valid: boolean) => {
    if (!valid) return
    knowledgeLoading.value = true
    const taskId = currentKnowledgeTask.value.taskId
    const data: Record<string, string> = {}
    if (knowledgeForm.problemType) data.problemType = knowledgeForm.problemType
    if (knowledgeForm.rootCause) data.rootCause = knowledgeForm.rootCause
    if (knowledgeForm.actionTaken) data.actionTaken = knowledgeForm.actionTaken
    if (knowledgeForm.resultSummary) data.resultSummary = knowledgeForm.resultSummary
    createKnowledgeFromTask(taskId, data)
      .then(() => {
        ElMessage.success('知识沉淀成功')
        knowledgeDialogVisible.value = false
      })
      .catch(() => {})
      .finally(() => {
        knowledgeLoading.value = false
      })
  })
}

function handleOpenReopenDialog(row: any) {
  currentReopenTask.value = row
  reopenForm.reason = ''
  reopenDialogVisible.value = true
}

function submitReopen() {
  if (!reopenFormRef.value || !currentReopenTask.value) return
  reopenFormRef.value.validate((valid: boolean) => {
    if (!valid) return
    reopenLoading.value = true
    reopenReviewTask(currentReopenTask.value.taskId, reopenForm.reason)
      .then(() => {
        ElMessage.success('任务已重开')
        reopenDialogVisible.value = false
        getList()
      })
      .finally(() => {
        reopenLoading.value = false
      })
  })
}

function handleViewEffect(row: any) {
  effectData.value = null
  effectWindowDays.value = 7
  effectDrawerVisible.value = true
  effectLoading.value = true
  getReviewTaskEffect(row.taskId, effectWindowDays.value)
    .then((res: any) => {
      effectData.value = res.data || null
    })
    .catch(() => {
      effectData.value = null
    })
    .finally(() => {
      effectLoading.value = false
    })
}

function handleEffectWindowChange(days: number) {
  if (!effectData.value) return
  effectWindowDays.value = days
  effectLoading.value = true
  getReviewTaskEffect(effectData.value.taskId, days)
    .then((res: any) => {
      effectData.value = res.data || null
    })
    .finally(() => {
      effectLoading.value = false
    })
}

onMounted(() => {
  getDeptOptions()
  getList()
})
</script>

<style scoped>
.review-task-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 6px;
  color: #18202f;
  font-size: 22px;
  font-weight: 700;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.filter-card :deep(.el-card__body) {
  padding: 14px 16px 2px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 12px;
}

.table-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.table-card :deep(.el-card__body) {
  padding: 0 16px 16px;
}

.table-card :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.table-card :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.money-danger {
  color: #f56c6c;
  font-weight: 600;
}

.log-task-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.log-task-type {
  font-weight: 600;
  color: #18202f;
}

.log-task-dept {
  color: #667085;
  font-size: 13px;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-handler {
  color: #445065;
  font-size: 13px;
}

.log-note {
  margin: 6px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.knowledge-task-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.knowledge-task-title {
  font-weight: 600;
  color: #18202f;
  font-size: 14px;
}
</style>
