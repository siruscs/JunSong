<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="模板分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="审批单" value="APPROVAL" />
          <el-option label="采购单" value="PURCHASE" />
          <el-option label="退款单" value="REFUND" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div v-loading="loading" class="template-grid" v-if="templateList.length > 0">
      <el-card v-for="tpl in templateList" :key="tpl.id" class="template-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="template-name">{{ tpl.templateName }}</span>
            <el-tag v-if="tpl.isStarter === '1'" type="warning" size="small">内置</el-tag>
            <el-tag v-else size="small">{{ categoryLabel(tpl.category) }}</el-tag>
          </div>
        </template>
        <div class="template-body">
          <div class="template-thumb">
            <el-icon v-if="!tpl.thumbnail" :size="48"><Document /></el-icon>
            <img v-else :src="tpl.thumbnail" />
          </div>
          <div class="template-info">
            <p class="description">{{ tpl.description || '暂无描述' }}</p>
            <div class="meta">
              <span><el-icon><User /></el-icon> {{ tpl.createBy || 'admin' }}</span>
              <span><el-icon><Star /></el-icon> {{ tpl.usageCount || 0 }} 次使用</span>
            </div>
          </div>
        </div>
        <template #footer>
          <div class="card-footer">
            <el-button type="primary" :icon="DocumentCopy" size="small" @click="handleClone(tpl)">克隆</el-button>
            <el-button :icon="View" size="small" @click="handlePreview(tpl)">预览</el-button>
          </div>
        </template>
      </el-card>
    </div>

    <el-empty v-else-if="!loading" description="暂无可用模板" />

    <!-- 克隆对话框 -->
    <el-dialog v-model="cloneDialogVisible" title="从模板克隆" width="480px" destroy-on-close>
      <el-form ref="cloneFormRef" :model="cloneForm" :rules="cloneRules" label-width="90px">
        <el-form-item label="模板">
          <el-input :model-value="selectedTemplate?.templateName" disabled />
        </el-form-item>
        <el-form-item label="新业务编码" prop="newBizCode">
          <el-input v-model="cloneForm.newBizCode" placeholder="如：my_approval_001" />
        </el-form-item>
        <el-form-item label="新业务名称" prop="newBizName">
          <el-input v-model="cloneForm.newBizName" placeholder="如：我的审批单" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cloneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="cloneLoading" @click="confirmClone">确认克隆</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="模板详情" width="640px" destroy-on-close>
      <el-descriptions :column="2" border v-if="selectedTemplate">
        <el-descriptions-item label="模板编码">{{ selectedTemplate.templateCode }}</el-descriptions-item>
        <el-descriptions-item label="模板名称">{{ selectedTemplate.templateName }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ categoryLabel(selectedTemplate.category) }}</el-descriptions-item>
        <el-descriptions-item label="流程Key">{{ selectedTemplate.processKey || '-' }}</el-descriptions-item>
        <el-descriptions-item label="使用次数">{{ selectedTemplate.usageCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="内置模板">{{ selectedTemplate.isStarter === '1' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ selectedTemplate.description }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ selectedTemplate.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Document, DocumentCopy, View, Star, User } from '@element-plus/icons-vue'
import { listTemplate, cloneTemplate, type LcBizTemplate } from '@/api/lowcode/admin'

const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const templateList = ref<LcBizTemplate[]>([])
const queryParams = reactive({ category: '' as string | undefined })

const cloneDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const cloneLoading = ref(false)
const selectedTemplate = ref<LcBizTemplate | null>(null)

const cloneForm = reactive({ newBizCode: '', newBizName: '' })
const cloneFormRef = ref()
const queryFormRef = ref()

const cloneRules = {
  newBizCode: [{ required: true, message: '请输入新业务编码', trigger: 'blur' }],
  newBizName: [{ required: true, message: '请输入新业务名称', trigger: 'blur' }],
}

function categoryLabel(cat?: string) {
  const map: Record<string, string> = { APPROVAL: '审批单', PURCHASE: '采购单', REFUND: '退款单' }
  return map[cat || ''] || cat || '-'
}

function fetchTemplates() {
  loading.value = true
  listTemplate(queryParams.category || undefined)
    .then((res: any) => {
      templateList.value = res.data || []
      loading.value = false
    })
    .catch(() => { loading.value = false })
}

function handleQuery() { fetchTemplates() }

function resetQuery() {
  queryParams.category = undefined
  fetchTemplates()
}

function handleClone(tpl: LcBizTemplate) {
  selectedTemplate.value = tpl
  cloneForm.newBizCode = ''
  cloneForm.newBizName = tpl.templateName ? '新' + tpl.templateName : ''
  cloneDialogVisible.value = true
}

function confirmClone() {
  cloneFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    cloneLoading.value = true
    cloneTemplate({
      templateId: selectedTemplate.value!.id!,
      newBizCode: cloneForm.newBizCode,
      newBizName: cloneForm.newBizName,
    })
      .then((res: any) => {
        cloneLoading.value = false
        cloneDialogVisible.value = false
        ElMessage.success('克隆成功，业务编码：' + res.data)
        router.push('/lowcode/admin-edit/' + res.data)
      })
      .catch(() => { cloneLoading.value = false })
  })
}

function handlePreview(tpl: LcBizTemplate) {
  selectedTemplate.value = tpl
  previewDialogVisible.value = true
}

onMounted(() => { fetchTemplates() })
</script>

<style scoped>
.app-container { padding: 20px; }

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.template-card { cursor: default; }
.template-card :deep(.el-card__header) { padding: 12px 16px; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.template-name { font-weight: 600; font-size: 15px; }

.template-body { min-height: 100px; }

.template-thumb {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 12px;
  color: #909399;
}

.template-thumb img { width: 100%; height: 100%; object-fit: cover; border-radius: 4px; }

.description {
  font-size: 13px;
  color: #606266;
  margin: 0 0 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 36px;
}

.meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.meta span { display: flex; align-items: center; gap: 4px; }

.card-footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
