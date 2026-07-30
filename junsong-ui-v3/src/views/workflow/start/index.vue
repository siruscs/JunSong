<template>
  <div class="start-process-container">
    <div class="start-header">
      <span class="start-title">发起新流程</span>
      <el-input
        v-model="keyword"
        placeholder="搜索流程名称"
        clearable
        style="width: 240px"
        :prefix-icon="Search"
      />
    </div>

    <div v-if="activeProcess" class="start-form-page" v-loading="formLoading">
      <div class="start-form-page__header">
        <el-page-header @back="closeStartPage">
          <template #content>{{ startForm.processName }}（{{ startForm.processKey }}）</template>
        </el-page-header>
      </div>
      <el-card class="start-form-page__card" shadow="never">
        <el-form :model="startForm" label-width="120px" class="start-business-form">
          <el-form-item label="业务键">
            <el-input v-model="startForm.businessKey" placeholder="系统可自动生成，也可填写业务单号" />
          </el-form-item>
          <el-divider content-position="left">业务表单</el-divider>
          <template v-if="businessFields.length">
            <el-row :gutter="20">
              <el-col v-for="field in businessFields" :key="field.fieldKey" :span="fieldSpan(field)">
                <el-form-item :label="field.fieldLabel" :required="field.required === '1'">
                  <FieldRenderer v-model="startForm.variables[field.fieldKey]" :field="field" :form-values="startForm.variables" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>
          <el-empty v-else description="该流程尚未绑定低代码业务表单" />
        </el-form>
        <div class="start-form-page__footer">
          <el-button @click="closeStartPage">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitStart">确认发起</el-button>
        </div>
      </el-card>
    </div>

    <div v-else class="category-list" v-loading="loading">
      <template v-if="groupedProcesses.size > 0">
        <el-collapse v-model="activeCategories">
          <el-collapse-item
            v-for="[catKey, processes] in groupedProcesses"
            :key="catKey"
            :name="catKey"
          >
            <template #title>
              <div class="category-header">
                <el-tag :type="getCategoryTagType(catKey)" effect="dark" size="small">
                  {{ getCategoryLabel(catKey) }}
                </el-tag>
                <span class="category-count">{{ processes.length }} 个流程</span>
              </div>
            </template>
            <div class="process-grid">
              <div
                v-for="proc in processes"
                :key="proc.definitionId"
                class="process-card"
                @click="openStartDialog(proc)"
              >
                <div class="process-card-icon">
                  <el-icon><Document /></el-icon>
                </div>
                <div class="process-card-info">
                  <div class="process-card-name">{{ proc.processName }}</div>
                  <div class="process-card-key">{{ proc.processKey }} · v{{ proc.version }}</div>
                </div>
                <el-button type="primary" size="small" :disabled="proc.suspended">
                  {{ proc.suspended ? '已挂起' : '发起' }}
                </el-button>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </template>
      <el-empty v-else-if="!loading" description="暂无可发起的流程" />
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Document } from '@element-plus/icons-vue'
import { listWorkflowDefinitions, type WorkflowDefinitionSummary } from '@/api/workflow/definition'
import { startWorkflowInstance } from '@/api/workflow/instance'
import { saveBizInstance, submitBizInstance } from '@/api/lowcode'
import { getBizConfig, getRuntimePage, listBizObject } from '@/api/lowcode/admin'
import FieldRenderer from '@/views/lowcode/fields/FieldRenderer.vue'
import { parseFieldExt } from '@/views/lowcode/schema'
import { useDict } from '@/composables/useDict'

const router = useRouter()
const dict = useDict('wf_category')

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const allProcesses = ref<WorkflowDefinitionSummary[]>([])
const activeCategories = ref<string[]>([])

const activeProcess = ref<WorkflowDefinitionSummary | null>(null)
const formLoading = ref(false)
const startForm = ref({
  processKey: '',
  bizCode: '',
  processName: '',
  businessKey: '',
  variables: {} as Record<string, any>,
})
const businessFields = ref<any[]>([])

// 按分类分组
const groupedProcesses = computed(() => {
  const filtered = allProcesses.value.filter((p) => {
    if (p.suspended) return false
    if (keyword.value) {
      return p.processName?.toLowerCase().includes(keyword.value.toLowerCase())
    }
    return true
  })

  const groups = new Map<string, WorkflowDefinitionSummary[]>()
  filtered.forEach((p) => {
    const cat = p.category || 'uncategorized'
    if (!groups.has(cat)) {
      groups.set(cat, [])
    }
    groups.get(cat)!.push(p)
  })

  // 排序：按字典顺序
  return new Map([...groups.entries()].sort((a, b) => {
    return getCategorySort(a[0]) - getCategorySort(b[0])
  }))
})

function getCategorySort(catKey: string): number {
  const items = dict.type?.wf_category || []
  const item = items.find((d: any) => d.value === catKey)
  return item ? parseInt(item.raw?.dictSort || '999') : 999
}

function getCategoryLabel(catKey: string): string {
  if (catKey === 'uncategorized') return '未分类'
  const items = dict.type?.wf_category || []
  const item = items.find((d: any) => d.value === catKey)
  return item ? item.label : catKey
}

function getCategoryTagType(catKey: string): any {
  const items = dict.type?.wf_category || []
  const item = items.find((d: any) => d.value === catKey)
  if (item && item.elTagType) {
    return item.elTagType
  }
  return 'info'
}

// 发起页与任务/历史详情共用运行时页面的栅格规则；这里只是不传 readonly。
function fieldSpan(field: any): number {
  const ext = parseFieldExt(field)
  if (ext.span) return Number(ext.span)
  if (['richtext', 'address', 'file', 'image'].includes(field.fieldType) || ext.textarea) return 24
  return 12
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await listWorkflowDefinitions({ latestOnly: true })
    if (res.code === 200) {
      allProcesses.value = res.data || []
      // 默认展开所有分类
      activeCategories.value = [...groupedProcesses.value.keys()]
    }
  } catch (e) {
    console.error('加载流程定义失败', e)
  } finally {
    loading.value = false
  }
}

function openStartDialog(proc: WorkflowDefinitionSummary) {
  activeProcess.value = proc
  startForm.value = {
    processKey: proc.processKey,
    bizCode: '',
    processName: proc.processName || '',
    businessKey: '',
    variables: {},
  }
  businessFields.value = []
  formLoading.value = true
  listBizObject({}).then((res: any) => {
    const objects = (res?.data?.rows || res?.data || res?.rows || []).flat?.() || []
    const object = objects.find((item: any) => item.processKey === proc.processKey && item.configStatus === 'PUBLISHED' && item.delFlag !== '1')
    if (!object?.bizCode) throw new Error(`流程 ${proc.processKey} 未绑定已发布业务对象`)
    startForm.value.bizCode = object.bizCode
    return getBizConfig(object.bizCode)
  }).then(async (res: any) => {
    const config = res?.data || res
    const metadataFields = (config?.fields || [])
      .filter((field: any) => field.stage !== 'FULFILLMENT')
    const fieldMap = new Map<string, any>(metadataFields.map((field: any) => [field.fieldKey, field] as [string, any]))
    // 发起态与流程详情共用低代码 FORM 布局；详情侧仅切换为只读。
    const runtime: any = await getRuntimePage(startForm.value.bizCode, 'FORM').catch(() => null)
    const runtimeFields = runtime?.data?.fields || runtime?.fields
    if (Array.isArray(runtimeFields) && runtimeFields.length) {
      businessFields.value = runtimeFields
        .filter((field: any) => field.visible !== false && field.stage !== 'FULFILLMENT')
        .map((field: any) => ({ ...(fieldMap.get(field.fieldKey) || {}), ...field }))
        .filter((field: any) => field.fieldKey)
    } else {
      businessFields.value = metadataFields.sort((a: any, b: any) => (a.orderNum || 0) - (b.orderNum || 0))
    }
  }).catch(() => {
    businessFields.value = []
  }).finally(() => {
    formLoading.value = false
  })
}

function closeStartPage() {
  activeProcess.value = null
  businessFields.value = []
}

const requiredStartFields = [
  { key: 'processKey', label: '流程标识' },
  { key: 'processName', label: '流程名称' },
]

function precheckStart(): { passed: boolean; missingFields: string[] } {
  const missing: string[] = []
  requiredStartFields.forEach((f) => {
    const value = startForm.value[f.key as 'processKey' | 'processName']
    if (!String(value ?? '').trim()) {
      missing.push(f.label)
    }
  })
  businessFields.value.filter((field: any) => field.required === '1').forEach((field: any) => {
    const value = startForm.value.variables[field.fieldKey]
    if (value === null || value === undefined || value === '') missing.push(field.fieldLabel || field.fieldKey)
  })
  return { passed: missing.length === 0, missingFields: missing }
}

async function submitStart() {
  const { passed, missingFields } = precheckStart()
  if (!passed) {
    await ElMessageBox.alert(
      `以下必填项未填写完整：\n${missingFields.map((f) => `• ${f}`).join('\n')}`,
      '发起预检未通过',
      { type: 'warning', confirmButtonText: '我知道了' },
    )
    return
  }
  submitting.value = true
  try {
    const variables = startForm.value.variables

    let res: any
    // 已登记的低代码流程必须先保存业务单据，再提交 Workflow，
    // 这样驳回后才能回到原表单修改并重新提交。
    if (businessFields.value.length > 0) {
      const saved: any = await saveBizInstance(startForm.value.bizCode, variables, { idempotencyNewKey: true })
      const recordId = Number(saved?.data ?? saved)
      if (!Number.isInteger(recordId) || recordId <= 0) throw new Error('业务单据保存失败：未返回单据编号')
      res = await submitBizInstance(startForm.value.bizCode, recordId, { idempotencyNewKey: true })
    } else {
      res = await startWorkflowInstance({
        processKey: startForm.value.processKey,
        businessKey: startForm.value.businessKey || undefined,
        variables,
        idempotencyNewKey: true,
      })
    }

    if (res.code === 200) {
      ElMessage.success('流程发起成功')
      activeProcess.value = null
      router.push('/workflow/task')
    } else {
      ElMessage.error(res.msg || '发起失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发起失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.start-process-container {
  padding: 16px;
}
.start-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.start-title {
  font-size: 18px;
  font-weight: 600;
}
.category-list {
  min-height: 300px;
}
.category-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.category-count {
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);
}
.process-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
  padding: 8px 0;
}
.process-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light, #ebeef5);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.process-card:hover {
  border-color: var(--el-color-primary, #409eff);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.process-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--el-color-primary-light-9, #ecf5ff);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary, #409eff);
  font-size: 20px;
  flex-shrink: 0;
}
.process-card-info {
  flex: 1;
  min-width: 0;
}
.process-card-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.process-card-key {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  margin-top: 2px;
}
</style>
