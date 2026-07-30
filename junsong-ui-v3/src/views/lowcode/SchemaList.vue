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
          <el-button :icon="Star" @click="openSaveQueryDialog">保存查询</el-button>
          <el-select
            v-model="selectedQueryName"
            placeholder="常用查询"
            clearable
            style="width: 180px"
            @change="applySavedQuery"
          >
            <el-option
              v-for="q in savedQueries"
              :key="q.name"
              :label="q.name"
              :value="q.name"
            >
              <span style="float: left">{{ q.name }}</span>
              <el-button
                style="float: right; margin-left: 12px"
                link
                type="danger"
                size="small"
                :icon="Delete"
                @click.stop="deleteSavedQuery(q.name)"
              />
            </el-option>
          </el-select>
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
            <span v-if="field.fieldType === 'subform'" class="lc-subform-summary">{{ rowFieldValue(row, field) }}</span>
            <FieldRenderer v-else :field="field" :model-value="rowFieldValue(row, field)" :form-values="parseRow(row)" readonly />
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

      <Pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
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
import { Plus, Refresh, Star, Delete } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
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
import { getRuntimePage } from '@/api/lowcode/admin'
import { listProductSelector } from '@/api/finance/product'
import { useSubmitLock } from '@/composables/useSubmitLock'

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
const queryParams = reactive<Record<string, any>>({ pageNum: 1, pageSize: 10 })
const total = ref(0)

// ===== 常用查询保存 =====
interface SavedQuery {
  name: string
  params: Record<string, any>
  savedAt: number
}
const savedQueries = ref<SavedQuery[]>([])
const selectedQueryName = ref<string>('')

function savedQueriesKey() {
  return bizCode.value + '_savedQueries'
}

function loadSavedQueries() {
  if (!bizCode.value) {
    savedQueries.value = []
    selectedQueryName.value = ''
    return
  }
  try {
    const raw = localStorage.getItem(savedQueriesKey())
    savedQueries.value = raw ? JSON.parse(raw) : []
  } catch {
    savedQueries.value = []
  }
  selectedQueryName.value = ''
}

function persistSavedQueries() {
  localStorage.setItem(savedQueriesKey(), JSON.stringify(savedQueries.value))
}

async function openSaveQueryDialog() {
  const result = await ElMessageBox.prompt('请输入查询名称', '保存常用查询', {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '名称不能为空',
  })
  const name = result.value.trim()
  const snapshot: Record<string, any> = {}
  Object.keys(queryParams).forEach((k) => {
    if (k !== 'pageNum' && k !== 'pageSize' && queryParams[k] !== '' && queryParams[k] != null) {
      snapshot[k] = queryParams[k]
    }
  })
  const item: SavedQuery = { name, params: snapshot, savedAt: Date.now() }
  const idx = savedQueries.value.findIndex((q) => q.name === name)
  if (idx >= 0) {
    savedQueries.value[idx] = item
  } else {
    savedQueries.value.push(item)
  }
  persistSavedQueries()
  selectedQueryName.value = name
  ElMessage.success('查询已保存')
}

function applySavedQuery(name: string) {
  if (!name) return
  const q = savedQueries.value.find((s) => s.name === name)
  if (!q) return
  Object.keys(queryParams).forEach((k) => {
    if (k !== 'pageNum' && k !== 'pageSize') delete queryParams[k]
  })
  Object.assign(queryParams, q.params)
  queryParams.pageNum = 1
  getList()
}

function deleteSavedQuery(name: string) {
  savedQueries.value = savedQueries.value.filter((q) => q.name !== name)
  persistSavedQueries()
  if (selectedQueryName.value === name) selectedQueryName.value = ''
  ElMessage.success('已删除查询')
}

const formDialog = reactive<{ visible: boolean; recordId: number | null }>({ visible: false, recordId: null })
const detailDrawer = reactive<{ visible: boolean; recordId: number | null }>({ visible: false, recordId: null })
const { execute: executeAction } = useSubmitLock()

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
  const configured = bizActions.value
    .filter(a => isActionVisible(a, status))
    .sort((a, b) => a.sortOrder - b.sortOrder)
  const terminalFallback: BizAction[] = []
  const configuredCodes = new Set(configured.map(action => action.actionCode))
  if (!configuredCodes.has('VIEW_DETAIL')) terminalFallback.push({ actionCode: 'VIEW_DETAIL', actionName: '查看', actionType: 'VIEW', triggerStatus: '', apiEndpoint: '', buttonStyle: 'primary', buttonIcon: '', sortOrder: 1 })
  if (!configuredCodes.has('EDIT') && ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(String(status || '').toUpperCase())) terminalFallback.push({ actionCode: 'EDIT', actionName: '修改', actionType: 'EDIT', triggerStatus: '', apiEndpoint: '', buttonStyle: 'primary', buttonIcon: '', sortOrder: 10 })
  if (!configuredCodes.has('SUBMIT') && ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(String(status || '').toUpperCase())) terminalFallback.push({ actionCode: 'SUBMIT', actionName: '重新提交', actionType: 'SUBMIT', triggerStatus: '', apiEndpoint: '', buttonStyle: 'success', buttonIcon: '', sortOrder: 20 })
  if (status !== 'REJECTED') return [...configured, ...terminalFallback].sort((a, b) => a.sortOrder - b.sortOrder)
  const codes = new Set([...configured, ...terminalFallback].map(action => action.actionCode))
  const fallback: BizAction[] = []
  if (!codes.has('EDIT')) fallback.push({ actionCode: 'EDIT', actionName: '修改', actionType: 'EDIT', triggerStatus: 'REJECTED', apiEndpoint: '', buttonStyle: 'primary', buttonIcon: '', sortOrder: 10 })
  if (!codes.has('SUBMIT')) fallback.push({ actionCode: 'SUBMIT', actionName: '重新提交', actionType: 'SUBMIT', triggerStatus: 'REJECTED', apiEndpoint: '', buttonStyle: 'success', buttonIcon: '', sortOrder: 20 })
  return [...configured, ...fallback].sort((a, b) => a.sortOrder - b.sortOrder)
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
const productNameMap = ref<Record<string, string>>({})
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

function rowFieldValue(row: LcBizInstance, field: LcBizField) {
  const parsed = parseRow(row)
  if (field.fieldKey === 'take_no') return parsed.take_no || row.orderNo
  if (field.fieldType === 'subform') {
    const rows = Array.isArray(parsed[field.fieldKey]) ? parsed[field.fieldKey] : []
    return rows.map((item: any, index: number) => {
      const product = item.productName || item.product_name || productNameMap.value[String(item.product_id)] || item.product_id || ('第' + (index + 1) + '行')
      const quantity = item.actual_quantity ?? item.quantity ?? '-'
      return product + ' × ' + quantity
    }).join('；') || '-'
  }
  return parsed[field.fieldKey]
}

async function loadMeta() {
  // 普通 Flowable 流程也可能复用列表路由；只有确认存在低代码对象后才请求运行时 Schema，
  // 避免把 task 等流程标识误报为“未发布页面 Schema”。
  const objRes: any = await getBizObject(bizCode.value)
  bizObject.value = objRes.data || null
  if (!bizObject.value) {
    allFields.value = []
    return
  }
  const [fieldRes, runtimeRes]: any[] = await Promise.all([
    listBizFields(bizCode.value),
    getRuntimePage(bizCode.value, 'LIST').catch(() => null),
  ])
  allFields.value = fieldRes.data || fieldRes.rows || []
  const runtimeFields = runtimeRes?.data?.fields || runtimeRes?.fields
  if (Array.isArray(runtimeFields) && runtimeFields.length) {
    const fieldMap = new Map(allFields.value.map((field) => [field.fieldKey, field]))
    allFields.value = runtimeFields
      .filter((runtimeField: any) => runtimeField.visible !== false)
      .map((runtimeField: any) => fieldMap.get(runtimeField.fieldKey))
      .filter(Boolean) as LcBizField[]
  }
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
    total.value = res.total || 0
    if (bizCode.value === 'stocktake') {
      try {
        const products: any = await listProductSelector()
        const options = products.data || products.rows || []
        productNameMap.value = Object.fromEntries(options.map((item: any) => [String(item.productId ?? item.id), item.productName ?? item.name]))
      } catch { productNameMap.value = {} }
    }
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.keys(queryParams).forEach((key) => {
    if (key !== 'pageNum' && key !== 'pageSize') delete queryParams[key]
  })
  queryParams.pageNum = 1
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
  await executeAction(`${bizCode.value}:submit:${row.id}`, async () => {
    await ElMessageBox.confirm(`确认提交单据「${row.orderNo || row.id}」进入审批流吗？`, '提交审批确认', { type: 'warning' })
    await submitBizInstance(bizCode.value, row.id!)
    ElMessage.success('流程已发起')
    await getList()
  })
}

async function handleWithdraw(row: LcBizInstance) {
  if (!row.id) return
  await executeAction(`${bizCode.value}:withdraw:${row.id}`, async () => {
    await ElMessageBox.confirm('撤回后会终止当前流程实例，确认继续吗？', '撤回确认', { type: 'warning' })
    await withdrawBizInstance(bizCode.value, row.id!)
    ElMessage.success('单据已撤回')
    await getList()
  })
}

async function handleDelete(row: LcBizInstance) {
  if (!row.id) return
  await executeAction(`${bizCode.value}:delete:${row.id}`, async () => {
    await ElMessageBox.confirm(`确认删除单据「${row.orderNo || row.id}」吗？`, '删除确认', { type: 'warning' })
    await deleteBizInstance(bizCode.value, row.id!)
    ElMessage.success('删除成功')
    await getList()
  })
}

function goToWorkflow(processInstanceId?: string) {
  if (!processInstanceId) return
  router.push({ path: '/workflow/history', query: { processInstanceId } })
}

async function bootstrap() {
  if (!bizCode.value) return
  loadSavedQueries()
  await loadMeta()
  await loadBizActions()
  await getList()
  const orderNo = typeof route.query.orderNo === 'string' ? route.query.orderNo : ''
  if (orderNo) {
    const row = rows.value.find((item) => item.orderNo === orderNo || String(item.orderNo || '').trim() === orderNo.trim())
    if (row) {
      if (route.query.readonly === '1') handleView(row)
      else if (canEdit(row.workflowStatus)) handleEdit(row)
      else handleView(row)
    } else {
      ElMessage.warning('未找到对应业务单据，请刷新后重试')
    }
  }
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
