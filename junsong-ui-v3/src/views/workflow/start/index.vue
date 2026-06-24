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

    <div class="category-list" v-loading="loading">
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

    <!-- 发起流程对话框 -->
    <el-dialog v-model="startDialogVisible" title="发起流程" width="500px">
      <el-form :model="startForm" label-width="100px">
        <el-form-item label="流程名称">
          <el-input :model-value="startForm.processName" disabled />
        </el-form-item>
        <el-form-item label="流程标识">
          <el-input :model-value="startForm.processKey" disabled />
        </el-form-item>
        <el-form-item label="业务键">
          <el-input v-model="startForm.businessKey" placeholder="可选，业务标识" />
        </el-form-item>
        <el-form-item label="流程变量">
          <el-input
            v-model="startForm.variablesJson"
            type="textarea"
            :rows="4"
            placeholder='可选，JSON格式，如 {"reason":"请假事由"}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStart">确认发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Document } from '@element-plus/icons-vue'
import { listWorkflowDefinitions, type WorkflowDefinitionSummary } from '@/api/workflow/definition'
import { startWorkflowInstance } from '@/api/workflow/instance'
import { useDict } from '@/composables/useDict'

const router = useRouter()
const dict = useDict('wf_category')

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const allProcesses = ref<WorkflowDefinitionSummary[]>([])
const activeCategories = ref<string[]>([])

const startDialogVisible = ref(false)
const startForm = ref({
  processKey: '',
  processName: '',
  businessKey: '',
  variablesJson: '',
})

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
  const items = dict.wf_category || []
  const item = items.find((d: any) => d.value === catKey)
  return item ? parseInt(item.sort || '999') : 999
}

function getCategoryLabel(catKey: string): string {
  if (catKey === 'uncategorized') return '未分类'
  const items = dict.wf_category || []
  const item = items.find((d: any) => d.value === catKey)
  return item ? item.label : catKey
}

function getCategoryTagType(catKey: string): any {
  const items = dict.wf_category || []
  const item = items.find((d: any) => d.value === catKey)
  if (item && item.listClass) {
    return item.listClass
  }
  return 'info'
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
  startForm.value = {
    processKey: proc.processKey,
    processName: proc.processName || '',
    businessKey: '',
    variablesJson: '',
  }
  startDialogVisible.value = true
}

async function submitStart() {
  submitting.value = true
  try {
    let variables: Record<string, any> | undefined
    if (startForm.value.variablesJson.trim()) {
      try {
        variables = JSON.parse(startForm.value.variablesJson)
      } catch {
        ElMessage.error('流程变量JSON格式错误')
        submitting.value = false
        return
      }
    }

    const res: any = await startWorkflowInstance({
      processKey: startForm.value.processKey,
      businessKey: startForm.value.businessKey || undefined,
      variables,
    })

    if (res.code === 200) {
      ElMessage.success('流程发起成功')
      startDialogVisible.value = false
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
