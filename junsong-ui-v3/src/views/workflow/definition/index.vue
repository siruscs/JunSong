<template>
  <div class="workflow-definition-page app-container">
    <section class="workflow-overview">
      <el-card
        v-for="card in overviewCards"
        :key="card.key"
        class="overview-card"
        shadow="hover"
      >
        <div class="overview-card__label">{{ card.label }}</div>
        <div class="overview-card__value">{{ card.value }}</div>
        <div class="overview-card__hint">{{ card.hint }}</div>
      </el-card>
    </section>

    <el-card class="workflow-table-card" shadow="never">
      <el-form
        ref="queryFormRef"
        :model="queryParams"
        :inline="true"
        label-width="88px"
        v-show="showSearch"
      >
        <el-form-item label="流程标识" prop="key">
          <el-input
            v-model="queryParams.key"
            placeholder="请输入流程标识"
            clearable
            @keyup.enter="getList"
          />
        </el-form-item>
        <el-form-item label="关键字" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            placeholder="流程名称 / 关键字"
            clearable
            @keyup.enter="getList"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.category" placeholder="请选择分类" clearable style="width: 160px">
            <el-option v-for="c in categoryOptions" :key="c.dictValue" :label="c.dictLabel" :value="c.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button link type="primary" @click="categoryManageDialog.visible = true">
            <el-icon><Setting /></el-icon>
            管理分类
          </el-button>
        </el-form-item>
        <el-form-item label="最新版本">
          <el-switch v-model="queryParams.latestOnly" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <RightToolbar v-model:showSearch="showSearch" @query="getList">
        <el-button
          type="primary"
          :icon="Plus"
          @click="handleCreate"
          v-hasPermi="['workflow:definition:add']"
        >
          新建流程
        </el-button>
        <el-button
          type="success"
          :icon="Refresh"
          @click="getList"
          v-hasPermi="['workflow:definition:list']"
        >
          刷新
        </el-button>
      </RightToolbar>

      <el-table v-loading="loading" :data="definitionList">
        <el-table-column label="流程名称" min-width="220">
          <template #default="{ row }">
            <div class="definition-title">
              <el-button link type="primary" @click="handleDetail(row)">
                {{ row.processName || row.processKey }}
              </el-button>
              <div class="definition-title__sub">{{ row.definitionId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="流程标识" prop="processKey" min-width="160" />
        <el-table-column label="分类" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.category" size="small" :type="getCategoryTagType(row.category)">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.suspended ? 'danger' : 'success'">
              {{ row.suspended ? '已挂起' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.deploymentTime) }}
          </template>
        </el-table-column>
        <el-table-column label="资源文件" prop="resourceName" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" min-width="420" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button
              link
              type="primary"
              @click="handleEdit(row)"
              v-hasPermi="['workflow:definition:edit']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="primary"
              @click="handleValidate(row)"
              v-hasPermi="['workflow:definition:edit']"
            >
              校验
            </el-button>
            <el-button
              link
              type="success"
              @click="handlePublish(row)"
              v-hasPermi="['workflow:definition:deploy']"
            >
              发布
            </el-button>
            <el-button
              v-if="!row.suspended"
              link
              type="success"
              @click="handleStart(row)"
              v-hasPermi="['workflow:instance:add', 'workflow:instance:list']"
            >
              发起
            </el-button>
            <el-button
              v-if="row.suspended"
              link
              type="warning"
              @click="handleActivate(row)"
              v-hasPermi="['workflow:definition:edit']"
            >
              激活
            </el-button>
            <el-button
              v-else
              link
              type="warning"
              @click="handleSuspend(row)"
              v-hasPermi="['workflow:definition:edit']"
            >
              挂起
            </el-button>
            <el-button link type="info" @click="handleDownloadXml(row)">XML</el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row)"
              v-hasPermi="['workflow:definition:remove']"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailDialog.visible" title="流程定义详情" width="720px">
      <el-descriptions v-if="detailDialog.data" :column="2" border>
        <el-descriptions-item label="流程名称">
          {{ detailDialog.data.processName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="流程标识">
          {{ detailDialog.data.processKey }}
        </el-descriptions-item>
        <el-descriptions-item label="分类">
          <el-tag v-if="detailDialog.data.category" size="small" :type="getCategoryTagType(detailDialog.data.category)">
            {{ getCategoryLabel(detailDialog.data.category) }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="定义 ID">
          {{ detailDialog.data.definitionId }}
        </el-descriptions-item>
        <el-descriptions-item label="部署 ID">
          {{ detailDialog.data.deploymentId }}
        </el-descriptions-item>
        <el-descriptions-item label="版本">
          v{{ detailDialog.data.version }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detailDialog.data.suspended ? '已挂起' : '运行中' }}
        </el-descriptions-item>
        <el-descriptions-item label="资源文件" :span="2">
          {{ detailDialog.data.resourceName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="发布时间" :span="2">
          {{ formatDateTime(detailDialog.data.deploymentTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">最近一次校验</el-divider>
      <template v-if="lastValidationResult">
        <el-alert
          :title="lastValidationResult.valid ? '校验通过' : '校验未通过'"
          :type="lastValidationResult.valid ? 'success' : 'warning'"
          :closable="false"
          show-icon
        />
        <ul class="validation-list">
          <li v-for="(message, index) in lastValidationResult.validationMessages" :key="index">
            {{ message }}
          </li>
        </ul>
      </template>
      <el-empty v-else description="当前会话暂无校验记录" />
    </el-dialog>

    <el-dialog v-model="publishDialog.visible" title="发布流程" width="520px">
      <el-form label-width="88px">
        <el-form-item label="流程名称">
          <span>{{ publishDialog.row?.processName || publishDialog.row?.processKey || '-' }}</span>
        </el-form-item>
        <el-form-item label="流程标识">
          <span>{{ publishDialog.row?.processKey || '-' }}</span>
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="publishDialog.category"
            placeholder="选择或输入新分类"
            clearable
            filterable
            allow-create
            default-first-option
            style="width: 100%"
          >
            <el-option
              v-for="c in categoryOptions"
              :key="c.dictValue"
              :label="c.dictLabel"
              :value="c.dictValue"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmPublish">确认发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="startDialog.visible" title="发起流程" width="620px">
      <el-form
        ref="startFormRef"
        :model="startDialog.form"
        :rules="startRules"
        label-width="88px"
      >
        <el-form-item label="流程名称">
          <div>{{ startDialog.row?.processName || startDialog.row?.processKey || '-' }}</div>
        </el-form-item>
        <el-form-item label="流程标识">
          <div>{{ startDialog.row?.processKey || '-' }}</div>
        </el-form-item>
        <el-form-item label="分类">
          <el-tag v-if="startDialog.row?.category" size="small" :type="getCategoryTagType(startDialog.row.category)">
            {{ getCategoryLabel(startDialog.row.category) }}
          </el-tag>
          <span v-else>-</span>
        </el-form-item>
        <el-form-item label="业务键" prop="businessKey">
          <el-input
            v-model="startDialog.form.businessKey"
            placeholder="可选，建议填写业务单号或业务主键"
            clearable
          />
        </el-form-item>
        <el-form-item label="流程变量" prop="variablesText">
          <el-input
            v-model="startDialog.form.variablesText"
            type="textarea"
            :rows="6"
            placeholder='可选，传入 JSON 对象，例如：{"days": 3, "reason": "门店请假"}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="startDialog.submitting" @click="submitStart">
          确认发起
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryManageDialog.visible" title="流程分类管理" width="600px" @open="resetCategoryForm">
      <el-table :data="categoryOptions" size="small" border>
        <el-table-column label="排序" prop="dictSort" width="70" align="center" />
        <el-table-column label="分类名称" prop="dictLabel" min-width="120" />
        <el-table-column label="编码" prop="dictValue" min-width="100" />
        <el-table-column label="样式" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.listClass || 'info'">{{ row.dictLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEditCategory(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">{{ categoryManageDialog.isEdit ? '编辑分类' : '新增分类' }}</el-divider>
      <el-form :model="categoryManageDialog.form" inline>
        <el-form-item label="名称" required>
          <el-input v-model="categoryManageDialog.form.dictLabel" placeholder="如：财务流程" clearable />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="categoryManageDialog.form.dictValue" placeholder="如：finance" clearable />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryManageDialog.form.dictSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveCategory">{{ categoryManageDialog.isEdit ? '更新' : '添加' }}</el-button>
          <el-button v-if="categoryManageDialog.isEdit" @click="resetCategoryForm">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Setting } from '@element-plus/icons-vue'
import { parseTime, resetForm as resetFormUtil } from '@/utils/junsong'
import {
  activateWorkflowDefinition,
  deleteWorkflowDeployment,
  deployWorkflowDefinition,
  getWorkflowDefinitionDetail,
  getWorkflowDefinitionXml,
  listWorkflowDefinitions,
  suspendWorkflowDefinition,
  type WorkflowDefinitionDeployResult,
  type WorkflowDefinitionSummary,
  type WorkflowDefinitionValidationResult,
  validateWorkflowDefinition,
} from '@/api/workflow/definition'
import { getDictDataByType, addData, updateData, delData } from '@/api/system/dict/data'
import { startWorkflowInstance } from '@/api/workflow/instance'
import { parseWorkflowVariablesText } from '../shared/runtime'

interface DetailDialogState {
  visible: boolean
  data: WorkflowDefinitionSummary | null
}

interface StartDialogState {
  visible: boolean
  submitting: boolean
  row: WorkflowDefinitionSummary | null
  form: {
    businessKey: string
    variablesText: string
  }
}

const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const queryFormRef = ref()
const startFormRef = ref()
const definitionList = ref<WorkflowDefinitionSummary[]>([])
const lastValidationResult = ref<WorkflowDefinitionValidationResult | null>(null)
const lastDeployResult = ref<WorkflowDefinitionDeployResult | null>(null)
const detailDialog = reactive<DetailDialogState>({
  visible: false,
  data: null,
})
const startDialog = reactive<StartDialogState>({
  visible: false,
  submitting: false,
  row: null,
  form: {
    businessKey: '',
    variablesText: '',
  },
})
const publishDialog = reactive({
  visible: false,
  row: null as WorkflowDefinitionSummary | null,
  xmlDetail: null as { xml: string; processKey: string; processName: string } | null,
  category: '',
})

interface DictItem {
  dictCode: number
  dictLabel: string
  dictValue: string
  dictSort: number
  listClass?: string
}

const categoryOptions = ref<DictItem[]>([])

const queryParams = reactive({
  key: '',
  keyword: '',
  category: '',
  latestOnly: true,
})
const startRules = {
  variablesText: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        try {
          parseWorkflowVariablesText(value)
          callback()
        } catch (error) {
          callback(error instanceof Error ? error : new Error('流程变量 JSON 格式不正确'))
        }
      },
      trigger: 'blur',
    },
  ],
}

const overview = computed(() => {
  const items = definitionList.value
  const now = Date.now()
  const recentWindow = 1000 * 60 * 60 * 24 * 7
  return {
    total: items.length,
    active: items.filter((item) => !item.suspended).length,
    suspended: items.filter((item) => item.suspended).length,
    recent: items.filter((item) => {
      if (!item.deploymentTime) return false
      return now - new Date(item.deploymentTime).getTime() <= recentWindow
    }).length,
  }
})

const overviewCards = computed(() => [
  { key: 'total', label: '定义总数', value: overview.value.total, hint: '当前检索范围内的流程定义数量' },
  { key: 'active', label: '运行中', value: overview.value.active, hint: '可直接发起或继续运行的定义' },
  { key: 'suspended', label: '已挂起', value: overview.value.suspended, hint: '已暂停，不再继续处理的定义' },
  {
    key: 'recent',
    label: '7日内发布',
    value: overview.value.recent,
    hint: lastDeployResult.value
      ? `最近发布：${lastDeployResult.value.processName || lastDeployResult.value.processKey} v${lastDeployResult.value.version}`
      : '帮助快速感知近期变更节奏',
  },
])

function formatDateTime(value: string | null) {
  return value ? parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || value : '-'
}

async function getList() {
  loading.value = true
  try {
    const res = await listWorkflowDefinitions(queryParams)
    definitionList.value = Array.isArray(res.data) ? res.data : []
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.key = ''
  queryParams.keyword = ''
  queryParams.category = ''
  queryParams.latestOnly = true
  resetFormUtil(queryFormRef.value)
  getList()
}

function handleCreate() {
  router.push('/workflow/designer/new')
}

function handleEdit(row: WorkflowDefinitionSummary) {
  router.push(`/workflow/designer/edit/${row.definitionId}`)
}

function resetStartForm() {
  startDialog.form.businessKey = ''
  startDialog.form.variablesText = ''
  startDialog.submitting = false
}

function handleStart(row: WorkflowDefinitionSummary) {
  startDialog.row = row
  startDialog.visible = true
  resetStartForm()
}

async function handleDetail(row: WorkflowDefinitionSummary) {
  const res = await getWorkflowDefinitionDetail(row.definitionId)
  detailDialog.data = res.data
  detailDialog.visible = true
}

async function loadDefinitionXml(row: WorkflowDefinitionSummary) {
  const res = await getWorkflowDefinitionXml(row.definitionId)
  return res.data
}

async function handleValidate(row: WorkflowDefinitionSummary) {
  const xmlDetail = await loadDefinitionXml(row)
  const res = await validateWorkflowDefinition({
    xml: xmlDetail.xml,
    processKey: xmlDetail.processKey,
    processName: xmlDetail.processName,
  })
  lastValidationResult.value = res.data
  detailDialog.data = row
  detailDialog.visible = true
  ElMessage.success(res.data.valid ? '流程校验通过' : '流程校验完成，请查看提示')
}

function openPublishDialog(row: WorkflowDefinitionSummary) {
  publishDialog.row = row
  publishDialog.category = row.category || ''
  publishDialog.visible = true
}

async function confirmPublish() {
  if (!publishDialog.xmlDetail || !publishDialog.row) return
  const res = await deployWorkflowDefinition({
    xml: publishDialog.xmlDetail.xml,
    processKey: publishDialog.xmlDetail.processKey,
    processName: publishDialog.xmlDetail.processName,
    deploymentName: publishDialog.xmlDetail.processName || publishDialog.xmlDetail.processKey,
    category: publishDialog.category || undefined,
  })
  lastDeployResult.value = res.data
  publishDialog.visible = false
  ElMessage.success(`发布成功，当前版本 v${res.data.version}`)
  await getList()
}

async function handlePublish(row: WorkflowDefinitionSummary) {
  const xmlDetail = await loadDefinitionXml(row)
  const validation = await validateWorkflowDefinition({
    xml: xmlDetail.xml,
    processKey: xmlDetail.processKey,
    processName: xmlDetail.processName,
  })
  lastValidationResult.value = validation.data
  if (!validation.data.valid) {
    detailDialog.data = row
    detailDialog.visible = true
    ElMessage.warning('校验未通过，已阻止发布')
    return
  }
  publishDialog.xmlDetail = {
    xml: xmlDetail.xml,
    processKey: xmlDetail.processKey,
    processName: xmlDetail.processName,
  }
  openPublishDialog(row)
}

async function handleSuspend(row: WorkflowDefinitionSummary) {
  await ElMessageBox.confirm(`确认挂起流程"${row.processName || row.processKey}"吗？`, '提示', {
    type: 'warning',
  })
  await suspendWorkflowDefinition(row.definitionId)
  ElMessage.success('流程已挂起')
  await getList()
}

async function handleActivate(row: WorkflowDefinitionSummary) {
  await ElMessageBox.confirm(`确认激活流程"${row.processName || row.processKey}"吗？`, '提示', {
    type: 'warning',
  })
  await activateWorkflowDefinition(row.definitionId)
  ElMessage.success('流程已激活')
  await getList()
}

async function handleDelete(row: WorkflowDefinitionSummary) {
  await ElMessageBox.confirm(
    `确认删除部署"${row.processName || row.processKey}"吗？删除后对应流程定义将不可恢复。`,
    '删除确认',
    { type: 'warning' },
  )
  await deleteWorkflowDeployment(row.deploymentId, true)
  ElMessage.success('流程部署已删除')
  await getList()
}

async function handleDownloadXml(row: WorkflowDefinitionSummary) {
  const xmlDetail = await loadDefinitionXml(row)
  const blob = new Blob([xmlDetail.xml], { type: 'application/xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${xmlDetail.processKey || row.processKey}.bpmn20.xml`
  link.click()
  URL.revokeObjectURL(url)
}

async function submitStart() {
  if (!startDialog.row || !startFormRef.value) return
  await startFormRef.value.validate()
  startDialog.submitting = true
  try {
    const variables = parseWorkflowVariablesText(startDialog.form.variablesText)
    await startWorkflowInstance({
      processKey: startDialog.row.processKey,
      businessKey: startDialog.form.businessKey.trim() || undefined,
      variables,
    })
    startDialog.visible = false
    ElMessage.success('流程已发起')
    router.push({
      path: '/workflow/instance',
      query: {
        processKey: startDialog.row.processKey,
        ...(startDialog.form.businessKey.trim() ? { businessKey: startDialog.form.businessKey.trim() } : {}),
      },
    })
  } finally {
    startDialog.submitting = false
  }
}

async function loadCategories() {
  try {
    const res = await getDictDataByType('wf_category')
    categoryOptions.value = (res.data || []).sort((a: DictItem, b: DictItem) => a.dictSort - b.dictSort)
  } catch (e) {
    categoryOptions.value = []
  }
}

function getCategoryLabel(value: string) {
  const item = categoryOptions.value.find(c => c.dictValue === value)
  return item?.dictLabel || value
}

function getCategoryTagType(value: string) {
  const item = categoryOptions.value.find(c => c.dictValue === value)
  return item?.listClass || 'info'
}

const categoryManageDialog = reactive({
  visible: false,
  loading: false,
  form: { dictLabel: '', dictValue: '', dictSort: 0 } as Partial<DictItem>,
  isEdit: false,
})

function resetCategoryForm() {
  categoryManageDialog.form = { dictLabel: '', dictValue: '', dictSort: categoryOptions.value.length + 1 }
  categoryManageDialog.isEdit = false
}

function handleEditCategory(row: DictItem) {
  categoryManageDialog.form = { ...row }
  categoryManageDialog.isEdit = true
}

async function handleSaveCategory() {
  if (!categoryManageDialog.form.dictLabel || !categoryManageDialog.form.dictValue) {
    ElMessage.warning('请填写分类名称和编码')
    return
  }
  const payload = {
    dictType: 'wf_category',
    dictLabel: categoryManageDialog.form.dictLabel,
    dictValue: categoryManageDialog.form.dictValue,
    dictSort: categoryManageDialog.form.dictSort || 0,
    status: '0',
    isDefault: 'N',
  }
  if (categoryManageDialog.isEdit && categoryManageDialog.form.dictCode) {
    await updateData({ ...payload, dictCode: categoryManageDialog.form.dictCode })
    ElMessage.success('分类已更新')
  } else {
    await addData(payload)
    ElMessage.success('分类已添加')
  }
  resetCategoryForm()
  await loadCategories()
}

async function handleDeleteCategory(row: DictItem) {
  await ElMessageBox.confirm(`确认删除分类"${row.dictLabel}"吗？`, '提示', { type: 'warning' })
  await delData(row.dictCode)
  ElMessage.success('分类已删除')
  await loadCategories()
}

onMounted(() => {
  loadCategories()
  getList()
})
</script>

<style scoped lang="scss">
.workflow-definition-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workflow-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  border: 1px solid var(--el-border-color-lighter);
}

.overview-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.overview-card__value {
  margin-top: 12px;
  font-size: 32px;
  line-height: 1;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.overview-card__hint {
  margin-top: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.workflow-table-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.definition-title {
  display: flex;
  flex-direction: column;
}

.definition-title__sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.validation-list {
  margin: 12px 0 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
}

@media (max-width: 1200px) {
  .workflow-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .workflow-overview {
    grid-template-columns: 1fr;
  }
}
</style>
