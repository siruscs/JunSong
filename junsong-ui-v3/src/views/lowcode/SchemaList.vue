<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="lc-page-head">
        <div>
          <h3 class="lc-page-title">{{ bizObject?.bizName || '低代码单据' }}</h3>
          <p class="lc-page-desc">业务编码：{{ bizCode }}</p>
        </div>
      </div>

      <el-form v-show="showSearch && queryFields.length" :model="queryParams" :inline="true" label-width="88px">
        <el-form-item v-for="field in queryFields" :key="field.fieldKey" :label="field.fieldLabel">
          <FieldRenderer v-model="queryParams[field.fieldKey]" :field="field" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <RightToolbar v-model:showSearch="showSearch" @query="getList">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        <el-button type="success" :icon="Refresh" @click="getList">刷新</el-button>
      </RightToolbar>

      <el-table v-loading="loading" :data="rows">
        <el-table-column label="单据编号" prop="orderNo" min-width="180" />
        <el-table-column
          v-for="field in listFields"
          :key="field.fieldKey"
          :label="field.fieldLabel"
          min-width="140"
        >
          <template #default="{ row }">
            <FieldRenderer :field="field" :model-value="parseRow(row)[field.fieldKey]" readonly />
          </template>
        </el-table-column>
        <el-table-column label="当前节点" prop="currentTaskName" min-width="140" />
        <el-table-column label="流程状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.workflowStatus).type as any">{{ statusMeta(row.workflowStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="168">
          <template #default="{ row }">{{ formatDateTime(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" :min-width="actionColumnWidth" fixed="right">
          <template #default="{ row }">
            <el-button
              v-for="action in visibleActions(row.workflowStatus)"
              :key="action.actionCode"
              :type="action.buttonStyle"
              link
              @click="handleAction(action, row)"
            >
              {{ action.actionName }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无单据数据" />
        </template>
      </el-table>
    </el-card>

    <SchemaForm
      v-model="formDialog.visible"
      :biz-code="bizCode"
      :record-id="formDialog.recordId"
      @saved="getList"
    />

    <SchemaDetail
      v-model="detailDrawer.visible"
      :biz-code="bizCode"
      :record-id="detailDrawer.recordId"
      @fulfilled="getList"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import FieldRenderer from './fields/FieldRenderer.vue'
import SchemaForm from './SchemaForm.vue'
import SchemaDetail from './SchemaDetail.vue'
import {
  deleteBizInstance,
  getBizObject,
  listBizFields,
  listBizInstances,
  listBizActions,
  submitBizInstance,
  withdrawBizInstance,
  type LcBizField,
  type LcBizInstance,
  type LcBizObject,
} from '@/api/lowcode'
import { isTrue, lcCanEdit, lcCanSubmit, lcCanWithdraw, lcFormatDateTime, lcStatusMeta } from './schema'

const props = defineProps<{ bizCode?: string }>()
const route = useRoute()
const router = useRouter()

const bizCode = computed(
  () =>
    props.bizCode ||
    (route.params.bizCode as string) ||
    (route.meta?.bizCode as string) ||
    (route.path.split('/').filter(Boolean).pop() as string) ||
    '',
)

const loading = ref(false)
const showSearch = ref(true)
const rows = ref<LcBizInstance[]>([])
const bizObject = ref<LcBizObject | null>(null)
const allFields = ref<LcBizField[]>([])
const queryParams = reactive<Record<string, any>>({})

const formDialog = reactive<{ visible: boolean; recordId: number | null }>({ visible: false, recordId: null })
const detailDrawer = reactive<{ visible: boolean; recordId: number | null }>({ visible: false, recordId: null })

// 动作配置
interface BizAction {
  actionCode: string
  actionName: string
  actionType: string
  triggerStatus: string
  apiEndpoint: string
  buttonStyle: string
  buttonIcon: string
  sortOrder: number
}
const bizActions = ref<BizAction[]>([])

function isActionVisible(action: BizAction, status: string | undefined): boolean {
  if (!action.triggerStatus) return true
  if (!status) return false
  return action.triggerStatus.split(',').map(s => s.trim()).includes(status)
}

function visibleActions(status: string | undefined): BizAction[] {
  return bizActions.value
    .filter(a => isActionVisible(a, status))
    .sort((a, b) => a.sortOrder - b.sortOrder)
}

const actionColumnWidth = computed(() => Math.max(320, bizActions.value.length * 80))

function handleAction(action: BizAction, row: LcBizInstance) {
  switch (action.actionCode) {
    case 'VIEW_DETAIL':
      handleView(row)
      break
    case 'EDIT':
      handleEdit(row)
      break
    case 'VIEW_FLOW':
      if (row.processInstanceId) goToWorkflow(row.processInstanceId)
      break
    case 'DELETE':
      handleDelete(row)
      break
    case 'SUBMIT':
      handleSubmit(row)
      break
    case 'WITHDRAW':
      handleWithdraw(row)
      break
    case 'FULFILL':
      handleView(row)
      break
    default:
      ElMessage.info(`动作「${action.actionName}」暂未实现`)
  }
}

const statusMeta = lcStatusMeta
const formatDateTime = lcFormatDateTime
const canEdit = lcCanEdit
const canSubmit = lcCanSubmit
const canWithdraw = lcCanWithdraw

const queryFields = computed(() =>
  allFields.value.filter((f) => isTrue(f.isQuery)).sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0)),
)
const listFields = computed(() =>
  allFields.value.filter((f) => isTrue(f.isList)).sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0)),
)

const rowCache = new WeakMap<object, Record<string, any>>()
function parseRow(row: LcBizInstance): Record<string, any> {
  if (!row) return {}
  const cached = rowCache.get(row)
  if (cached) return cached
  let parsed: Record<string, any> = {}
  if (row.formData) {
    try {
      parsed = JSON.parse(row.formData)
    } catch {
      parsed = {}
    }
  }
  rowCache.set(row, parsed)
  return parsed
}

async function loadMeta() {
  const [objRes, fieldRes]: any[] = await Promise.all([
    getBizObject(bizCode.value),
    listBizFields(bizCode.value),
  ])
  bizObject.value = objRes.data || null
  allFields.value = fieldRes.data || fieldRes.rows || []
}

async function loadBizActions() {
  const res: any = await listBizActions(bizCode.value)
  bizActions.value = res.data || res.rows || []
}

async function getList() {
  if (!bizCode.value) return
  loading.value = true
  try {
    const res: any = await listBizInstances(bizCode.value, queryParams)
    rows.value = res.rows || res.data || []
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.keys(queryParams).forEach((key) => delete queryParams[key])
  getList()
}

function handleAdd() {
  formDialog.recordId = null
  formDialog.visible = true
}

function handleEdit(row: LcBizInstance) {
  formDialog.recordId = row.id ?? null
  formDialog.visible = true
}

function handleView(row: LcBizInstance) {
  detailDrawer.recordId = row.id ?? null
  detailDrawer.visible = true
}

async function handleSubmit(row: LcBizInstance) {
  if (!row.id) return
  await ElMessageBox.confirm(`确认提交单据「${row.orderNo || row.id}」进入审批流吗？`, '提交审批确认', { type: 'warning' })
  await submitBizInstance(bizCode.value, row.id)
  ElMessage.success('流程已发起')
  await getList()
}

async function handleWithdraw(row: LcBizInstance) {
  if (!row.id) return
  await ElMessageBox.confirm('撤回后会终止当前流程实例，确认继续吗？', '撤回确认', { type: 'warning' })
  await withdrawBizInstance(bizCode.value, row.id)
  ElMessage.success('单据已撤回')
  await getList()
}

async function handleDelete(row: LcBizInstance) {
  if (!row.id) return
  await ElMessageBox.confirm(`确认删除单据「${row.orderNo || row.id}」吗？`, '删除确认', { type: 'warning' })
  await deleteBizInstance(bizCode.value, row.id)
  ElMessage.success('删除成功')
  await getList()
}

function goToWorkflow(processInstanceId?: string) {
  if (!processInstanceId) return
  router.push({ path: '/workflow/history', query: { processInstanceId } })
}

async function bootstrap() {
  if (!bizCode.value) return
  await loadMeta()
  await loadBizActions()
  await getList()
}

watch(bizCode, () => bootstrap())
onMounted(() => bootstrap())
</script>

<style scoped>
.lc-page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.lc-page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.lc-page-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
