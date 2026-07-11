<template>
  <div class="app-container growth-action-page">
    <div class="page-head">
      <div>
        <h2>会员增长动作</h2>
        <p>
          基于现金压力、会员活跃、成长值和复购信号，识别候选会员并生成可执行、可留痕、可复盘的增长动作。
          <el-tag v-if="dashboard.pressureFallbackUsed" type="warning" size="small" style="margin-left: 8px">
            压力等级使用 fallback
          </el-tag>
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新</el-button>
        <el-button
          type="primary"
          :icon="Promotion"
          :loading="generateLoading"
          v-hasPermi="['member:growthAction:generate']"
          @click="openGenerateDialog"
        >
          生成动作
        </el-button>
      </div>
    </div>

    <div class="filter-panel">
      <el-form label-position="right" label-width="80px" class="filter-form" inline>
        <el-form-item label="门店">
          <el-select
            v-model="filter.deptId"
            placeholder="全部门店"
            clearable
            filterable
            class="filter-select"
            @change="loadDashboard"
          >
            <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分层">
          <el-select v-model="filter.segmentType" placeholder="全部分层" clearable class="filter-select" @change="loadDashboard">
            <el-option v-for="item in segmentOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部状态" clearable class="filter-select" @change="loadDashboard">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作类型">
          <el-select v-model="filter.actionType" placeholder="全部类型" clearable class="filter-select" @change="loadDashboard">
            <el-option v-for="item in actionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <el-alert
      v-if="dashboard.pressureLevel === 'HIGH' || dashboard.pressureLevel === 'CRITICAL'"
      :title="`当前门店现金压力等级为 ${pressureLabel(dashboard.pressureLevel)}，优先推荐 PRESSURE_STORE_RECALL 召回动作`"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 12px"
    />

    <div class="kpi-grid">
      <div class="kpi-card">
        <span>待执行动作</span>
        <strong>{{ dashboard.pendingActionCount ?? 0 }}</strong>
        <p>状态为 PENDING / IN_PROGRESS</p>
      </div>
      <div class="kpi-card warning">
        <span>待触达会员</span>
        <strong>{{ dashboard.pendingMemberCount ?? 0 }}</strong>
        <p>尚未执行的动作会员</p>
      </div>
      <div class="kpi-card success">
        <span>已执行会员</span>
        <strong>{{ dashboard.executedMemberCount ?? 0 }}</strong>
        <p>DONE / IGNORED / IN_PROGRESS</p>
      </div>
      <div class="kpi-card success">
        <span>有效会员</span>
        <strong>{{ dashboard.effectiveMemberCount ?? 0 }}</strong>
        <p>7 天内复购/签到/成长值增长</p>
      </div>
      <div class="kpi-card" :class="effectRateClass">
        <span>有效率</span>
        <strong>{{ ratePercent(dashboard.effectRate) }}%</strong>
        <p>有效会员 / 已执行会员</p>
      </div>
    </div>

    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>候选会员</span>
          <div class="section-header-right">
            <el-tag v-if="dashboard.topSegmentType" type="info" size="small">
              优先分层: {{ segmentLabel(dashboard.topSegmentType) }}
            </el-tag>
            <el-tag type="info" size="small">共 {{ candidates.length }} 人</el-tag>
          </div>
        </div>
      </template>
      <el-table :data="candidates" stripe border style="width: 100%" empty-text="暂无候选会员" max-height="420">
        <el-table-column type="index" label="#" width="56" />
        <el-table-column prop="memberName" label="会员" min-width="140" show-overflow-tooltip>
          <template #default="scope">
            <div>{{ scope.row.memberName || '-' }}</div>
            <div class="muted">{{ scope.row.memberNo || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="门店" min-width="120" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="segmentType" label="分层" min-width="150">
          <template #default="scope">
            <el-tag :type="segmentTag(scope.row.segmentType)" size="small">
              {{ segmentLabel(scope.row.segmentType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="growthValue" label="成长值" width="100" />
        <el-table-column prop="cardTypeName" label="等级" min-width="100">
          <template #default="scope">{{ scope.row.cardTypeName || scope.row.cardType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="lastActiveTime" label="最后活跃" min-width="160">
          <template #default="scope">
            <div>{{ formatDateTime(scope.row.lastActiveTime) }}</div>
            <div class="muted" v-if="scope.row.activeDaysAgo != null">{{ scope.row.activeDaysAgo }} 天前</div>
          </template>
        </el-table-column>
        <el-table-column prop="candidateReason" label="入选原因" min-width="220" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.candidateReason || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section-card">
      <template #header><span>最近动作</span></template>
      <el-table :data="recentActions" stripe border style="width: 100%" empty-text="暂无动作记录，点击右上角生成动作" max-height="420">
        <el-table-column type="index" label="#" width="56" />
        <el-table-column prop="actionTitle" label="动作标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="actionType" label="类型" min-width="120">
          <template #default="scope">
            <el-tag size="small">{{ actionTypeLabel(scope.row.actionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="门店" min-width="120" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="pressureLevel" label="压力等级" width="100">
          <template #default="scope">
            <el-tag :type="pressureTag(scope.row.pressureLevel)" size="small">
              {{ pressureLabel(scope.row.pressureLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="candidateCount" label="候选数" width="90" />
        <el-table-column prop="executedCount" label="已执行" width="90" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="scope">
            <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160">
          <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              link
              size="small"
              v-hasPermi="['member:growthAction:execute']"
              @click="openExecuteDialog(scope.row)"
            >
              执行
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>效果复盘</span>
          <div class="effect-controls">
            <el-select
              v-model="filter.actionId"
              placeholder="选择动作查看真实效果"
              clearable
              filterable
              style="width: 280px"
              @change="loadEffect"
            >
              <el-option
                v-for="a in recentActions"
                :key="a.actionId"
                :label="`${a.actionTitle} (${a.actionNo || a.actionId})`"
                :value="a.actionId"
              />
            </el-select>
            <el-button
              v-hasPermi="['member:growthAction:effect']"
              type="primary"
              link
              :loading="effectLoading"
              @click="loadEffect"
            >
              刷新效果
            </el-button>
          </div>
        </div>
      </template>
      <div class="effect-grid">
        <div class="effect-item">
          <span>动作会员总数</span>
          <strong>{{ effectSummary.totalMemberCount ?? 0 }}</strong>
        </div>
        <div class="effect-item success">
          <span>复购人数</span>
          <strong>{{ effectSummary.repurchaseMemberCount ?? 0 }}</strong>
        </div>
        <div class="effect-item success">
          <span>签到人数</span>
          <strong>{{ effectSummary.signInMemberCount ?? 0 }}</strong>
        </div>
        <div class="effect-item success">
          <span>成长值增长人数</span>
          <strong>{{ effectSummary.growthIncreasedMemberCount ?? 0 }}</strong>
        </div>
        <div class="effect-item" :class="effectRateClass">
          <span>有效会员</span>
          <strong>{{ effectSummary.effectiveMemberCount ?? 0 }}</strong>
        </div>
        <div class="effect-item" :class="effectRateClass">
          <span>有效率</span>
          <strong>{{ ratePercent(effectSummary.effectRate) }}%</strong>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="generateDialogVisible" title="生成增长动作" width="560px" :close-on-click-modal="false">
      <el-form ref="generateFormRef" :model="generateForm" label-width="100px" :rules="generateRules">
        <el-form-item label="门店" prop="deptId">
          <el-select v-model="generateForm.deptId" placeholder="选择门店（可空，表示全部授权门店）" clearable filterable class="full-width">
            <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作类型" prop="actionType">
          <el-select v-model="generateForm.actionType" placeholder="留空则按压力/分层自动推导" clearable class="full-width">
            <el-option v-for="item in actionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="候选分层" prop="segmentType">
          <el-select v-model="generateForm.segmentType" placeholder="留空则按压力/分层自动推导" clearable class="full-width">
            <el-option v-for="item in segmentOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="压力等级" prop="pressureLevel">
          <el-select v-model="generateForm.pressureLevel" placeholder="留空则读取最新快照" clearable class="full-width">
            <el-option label="低 LOW" value="LOW" />
            <el-option label="中 MEDIUM" value="MEDIUM" />
            <el-option label="高 HIGH" value="HIGH" />
            <el-option label="严重 CRITICAL" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="候选上限" prop="limit">
          <el-input-number v-model="generateForm.limit" :min="1" :max="50" :step="5" controls-position="right" class="full-width" />
        </el-form-item>
        <el-form-item label="动作原因" prop="actionReason">
          <el-input v-model="generateForm.actionReason" type="textarea" :rows="2" placeholder="说明生成该动作的原因（可选）" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="建议话术" prop="suggestedScript">
          <el-input v-model="generateForm.suggestedScript" type="textarea" :rows="3" placeholder="门店执行人可参考的话术（可选）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="submitGenerate">确认生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="executeDialogVisible" title="执行增长动作" width="560px" :close-on-click-modal="false">
      <el-form ref="executeFormRef" :model="executeForm" label-width="100px" :rules="executeRules">
        <el-form-item label="动作">
          <div class="execute-action-info">
            <strong>{{ executeForm.actionTitle || '-' }}</strong>
            <div class="muted">{{ executeForm.actionNo || '-' }}</div>
          </div>
        </el-form-item>
        <el-form-item label="候选会员">
          <el-select v-model="executeForm.memberId" placeholder="选择要执行的动作会员" filterable :loading="executeMembersLoading" class="full-width" @change="onExecuteMemberChange">
            <el-option
              v-for="m in executeMembers"
              :key="m.memberId"
              :label="`${m.memberName || '-'} (${m.memberNo || '-'}) - ${segmentLabel(m.segmentType)}${m.executeStatus && m.executeStatus !== 'PENDING' ? ' [' + statusLabel(m.executeStatus) + ']' : ''}`"
              :value="m.memberId"
              :disabled="m.executeStatus === 'DONE' || m.executeStatus === 'IGNORED'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="执行状态" prop="executeStatus">
          <el-select v-model="executeForm.executeStatus" placeholder="选择执行状态" class="full-width">
            <el-option label="处理中 IN_PROGRESS" value="IN_PROGRESS" />
            <el-option label="已完成 DONE" value="DONE" />
            <el-option label="已忽略 IGNORED" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行备注" prop="executeNote">
          <el-input
            v-model="executeForm.executeNote"
            type="textarea"
            :rows="3"
            :placeholder="executeNotePlaceholder"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="executeLoading" @click="submitExecute">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Promotion, Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  executeGrowthAction,
  generateGrowthAction,
  getGrowthActionDashboard,
  getGrowthActionEffect,
  listGrowthActionCandidates,
  listGrowthActionMembers,
} from '@/api/member/growthAction'

const userStore = useUserStore()

const loading = ref(false)
const generateLoading = ref(false)
const executeLoading = ref(false)
const effectLoading = ref(false)

const dashboard = ref<Record<string, any>>({})
const candidates = ref<Record<string, any>[]>([])
const recentActions = ref<Record<string, any>[]>([])
const effectSummary = ref<Record<string, any>>({})

const depts = computed(() =>
  (userStore.depts || []).map((dept: any) => ({
    id: dept.deptId,
    label: dept.deptName,
  }))
)

const filter = reactive({
  deptId: undefined as number | undefined,
  segmentType: undefined as string | undefined,
  status: undefined as string | undefined,
  actionType: undefined as string | undefined,
  actionId: undefined as number | undefined,
})

const segmentOptions = [
  { value: 'SLEEPING_HIGH_VALUE', label: '高价值沉睡' },
  { value: 'NEAR_LEVEL_UP', label: '临门升级' },
  { value: 'RECENT_ACTIVE_NO_REPEAT', label: '活跃未复购' },
  { value: 'PRESSURE_STORE_RECALL', label: '压力门店召回' },
]

const statusOptions = [
  { value: 'PENDING', label: '待执行' },
  { value: 'IN_PROGRESS', label: '处理中' },
  { value: 'DONE', label: '已完成' },
  { value: 'IGNORED', label: '已忽略' },
]

const actionTypeOptions = [
  { value: 'RECALL_VISIT', label: '召回到店' },
  { value: 'LEVEL_UP_NUDGE', label: '临门升级提醒' },
  { value: 'SIGN_IN_RECOVER', label: '签到恢复' },
  { value: 'REPEAT_PURCHASE', label: '复购提醒' },
]

function segmentLabel(type: string) {
  return segmentOptions.find((s) => s.value === type)?.label || type || '-'
}

function segmentTag(type: string) {
  if (type === 'PRESSURE_STORE_RECALL') return 'danger'
  if (type === 'SLEEPING_HIGH_VALUE') return 'warning'
  if (type === 'NEAR_LEVEL_UP') return 'success'
  if (type === 'RECENT_ACTIVE_NO_REPEAT') return 'info'
  return ''
}

function statusLabel(status: string) {
  return statusOptions.find((s) => s.value === status)?.label || status || '-'
}

function statusTag(status: string) {
  if (status === 'DONE') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'IGNORED') return 'info'
  if (status === 'PENDING') return 'danger'
  return ''
}

function actionTypeLabel(type: string) {
  return actionTypeOptions.find((a) => a.value === type)?.label || type || '-'
}

function pressureLabel(level: string) {
  const labels: Record<string, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '严重',
  }
  return labels[level || ''] || level || '-'
}

function pressureTag(level: string) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}

function ratePercent(value: any) {
  return Number(value ?? 0).toFixed(2)
}

function formatDateTime(value: any) {
  if (!value) return '-'
  return String(value).substring(0, 19).replace('T', ' ')
}

const effectRateClass = computed(() => {
  const rate = Number(dashboard.value.effectRate ?? 0)
  if (rate >= 30) return 'success'
  if (rate >= 10) return 'warning'
  return 'danger'
})

function loadDashboard() {
  loading.value = true
  return getGrowthActionDashboard({ ...filter })
    .then((res: any) => {
      const data = res.data || {}
      dashboard.value = data
      candidates.value = data.candidates || []
      recentActions.value = data.recentActions || []
      effectSummary.value = data.effectSummary || {}
      // 效果复盘默认选中第一个动作，触发真实 7 天效果回查
      if (!filter.actionId && recentActions.value.length > 0) {
        filter.actionId = recentActions.value[0].actionId
        return loadEffect()
      }
    })
    .catch((e: any) => {
      ElMessage.error(e?.message || '加载增长动作看板失败')
    })
    .finally(() => {
      loading.value = false
    })
}

function loadEffect() {
  effectLoading.value = true
  // 传 actionId 时后端走真实 7 天效果回查（fin_sale_record/mem_member_sign_in/成长值）；
  // 不传 actionId 时回退到全局静态标记位汇总
  const params: Record<string, any> = { ...filter }
  return getGrowthActionEffect(params)
    .then((res: any) => {
      effectSummary.value = res.data || {}
    })
    .catch((e: any) => {
      ElMessage.error(e?.message || '加载效果摘要失败')
    })
    .finally(() => {
      effectLoading.value = false
    })
}

function loadCandidatesOnly() {
  return listGrowthActionCandidates({ ...filter })
    .then((res: any) => {
      candidates.value = res.data || []
    })
    .catch(() => {
      // 静默失败，主流程已在 dashboard 加载
    })
}

// ============ 生成动作弹窗 ============
const generateDialogVisible = ref(false)
const generateFormRef = ref<FormInstance>()
const generateForm = reactive({
  deptId: undefined as number | undefined,
  actionType: undefined as string | undefined,
  segmentType: undefined as string | undefined,
  pressureLevel: undefined as string | undefined,
  limit: 30,
  actionReason: '',
  suggestedScript: '',
})
const generateRules: FormRules = {
  limit: [{ required: true, message: '请输入候选上限', trigger: 'blur' }],
}

function openGenerateDialog() {
  generateForm.deptId = filter.deptId
  generateForm.actionType = filter.actionType
  generateForm.segmentType = filter.segmentType
  generateForm.pressureLevel = dashboard.value.pressureLevel || undefined
  generateForm.limit = 30
  generateForm.actionReason = ''
  generateForm.suggestedScript = ''
  generateDialogVisible.value = true
}

function submitGenerate() {
  generateFormRef.value?.validate((valid) => {
    if (!valid) return
    generateLoading.value = true
    generateGrowthAction({ ...generateForm })
      .then((res: any) => {
        ElMessage.success(res.msg || '动作生成成功')
        generateDialogVisible.value = false
        return Promise.all([loadDashboard(), loadCandidatesOnly()])
      })
      .catch((e: any) => {
        ElMessage.error(e?.message || '动作生成失败')
      })
      .finally(() => {
        generateLoading.value = false
      })
  })
}

// ============ 执行动作弹窗 ============
const executeDialogVisible = ref(false)
const executeFormRef = ref<FormInstance>()
const executeForm = reactive({
  actionId: undefined as number | undefined,
  actionNo: '',
  actionTitle: '',
  memberId: undefined as number | undefined,
  executeStatus: 'IN_PROGRESS',
  executeNote: '',
})
const executeMembers = ref<Record<string, any>[]>([])

const executeRules = computed<FormRules>(() => ({
  memberId: [{ required: true, message: '请选择候选会员', trigger: 'change' }],
  executeStatus: [{ required: true, message: '请选择执行状态', trigger: 'change' }],
  executeNote: [
    {
      validator: (_rule: any, value: string, callback: any) => {
        const status = executeForm.executeStatus
        if ((status === 'DONE' || status === 'IGNORED') && (!value || !value.trim())) {
          callback(new Error('DONE / IGNORED 状态必须填写执行备注'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}))

const executeNotePlaceholder = computed(() => {
  const status = executeForm.executeStatus
  if (status === 'DONE' || status === 'IGNORED') {
    return `${statusLabel(status)} 状态必须填写执行备注，说明处理结果或忽略原因`
  }
  return '记录执行过程或备注（可选）'
})

const executeMembersLoading = ref(false)

function openExecuteDialog(row: any) {
  executeForm.actionId = row.actionId
  executeForm.actionNo = row.actionNo
  executeForm.actionTitle = row.actionTitle
  executeForm.memberId = undefined
  executeForm.executeStatus = 'IN_PROGRESS'
  executeForm.executeNote = ''
  executeMembers.value = []
  executeDialogVisible.value = true

  // 按该 actionId 查询真实会员明细，避免误用 dashboard candidates 导致跨动作提交
  executeMembersLoading.value = true
  listGrowthActionMembers(row.actionId)
    .then((res: any) => {
      const list = res.data || []
      executeMembers.value = list.map((m: any) => ({
        memberId: m.memberId,
        memberName: m.memberName,
        memberNo: m.memberNo,
        segmentType: m.segmentType,
        executeStatus: m.executeStatus,
      }))
      if (executeMembers.value.length === 0) {
        ElMessage.warning('该动作下无候选会员数据')
      }
    })
    .catch(() => {
      ElMessage.error('加载动作会员明细失败')
    })
    .finally(() => {
      executeMembersLoading.value = false
    })
}

function onExecuteMemberChange() {
  executeFormRef.value?.clearValidate('executeNote')
}

function submitExecute() {
  executeFormRef.value?.validate((valid) => {
    if (!valid) return
    executeLoading.value = true
    executeGrowthAction({
      actionId: executeForm.actionId,
      memberId: executeForm.memberId,
      executeStatus: executeForm.executeStatus,
      executeNote: executeForm.executeNote,
    })
      .then((res: any) => {
        ElMessage.success(res.msg || '执行状态已保存')
        executeDialogVisible.value = false
        return Promise.all([loadDashboard(), loadCandidatesOnly(), loadEffect()])
      })
      .catch((e: any) => {
        ElMessage.error(e?.message || '保存执行状态失败')
      })
      .finally(() => {
        executeLoading.value = false
      })
  })
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.growth-action-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.page-head,
.filter-panel,
.section-card {
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fff;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  margin-bottom: 12px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.page-head p {
  margin: 0;
  color: #667085;
  font-size: 13px;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.filter-panel {
  padding: 14px 18px;
  margin-bottom: 12px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 12px;
}

.filter-select {
  min-width: 180px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.kpi-card {
  min-height: 108px;
  padding: 16px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fff;
}

.kpi-card span {
  display: block;
  color: #7a879c;
  font-size: 13px;
  font-weight: 600;
}

.kpi-card strong {
  display: block;
  margin: 10px 0 6px;
  color: #18202f;
  font-size: 26px;
  line-height: 1.1;
}

.kpi-card p {
  margin: 0;
  color: #667085;
  font-size: 12px;
}

.kpi-card.success strong {
  color: #239b63;
}

.kpi-card.warning strong {
  color: #b7791f;
}

.kpi-card.danger strong {
  color: #c24136;
}

.section-card {
  margin-bottom: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.effect-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.effect-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.effect-item {
  padding: 14px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fafbfc;
  text-align: center;
}

.effect-item span {
  display: block;
  color: #7a879c;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.effect-item strong {
  display: block;
  color: #18202f;
  font-size: 22px;
}

.effect-item.success strong {
  color: #239b63;
}

.effect-item.danger strong {
  color: #c24136;
}

.full-width {
  width: 100%;
}

.execute-action-info strong {
  color: #18202f;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .effect-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .kpi-grid,
  .effect-grid {
    grid-template-columns: 1fr;
  }

  .filter-select {
    min-width: 100%;
  }
}
</style>
