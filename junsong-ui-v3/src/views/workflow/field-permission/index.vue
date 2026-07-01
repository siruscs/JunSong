<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="流程标识" prop="processDefinitionKey">
        <el-input v-model="queryParams.processDefinitionKey" placeholder="流程标识" clearable />
      </el-form-item>
      <el-form-item label="节点ID" prop="activityId">
        <el-input v-model="queryParams.activityId" placeholder="节点ID" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain :icon="Plus" @click="handleAdd">新增配置</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="60" />
      <el-table-column label="流程标识" prop="processDefinitionKey" min-width="140" />
      <el-table-column label="节点ID" prop="activityId" min-width="140" />
      <el-table-column label="字段标识" prop="fieldKey" min-width="120" />
      <el-table-column label="字段名称" prop="fieldLabel" min-width="120" />
      <el-table-column label="权限" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.permission === 'hidden'" size="small" type="info">隐藏</el-tag>
          <el-tag v-else-if="row.permission === 'readonly'" size="small">只读</el-tag>
          <el-tag v-else-if="row.permission === 'editable'" size="small" type="success">可编辑</el-tag>
          <el-tag v-else-if="row.permission === 'required'" size="small" type="warning">必填</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="520px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="100px">
        <el-form-item label="流程标识" prop="processDefinitionKey">
          <el-input v-model="dialog.form.processDefinitionKey" placeholder="如：leave-apply" />
        </el-form-item>
        <el-form-item label="节点ID" prop="activityId">
          <el-input v-model="dialog.form.activityId" placeholder="BPMN节点ID" />
        </el-form-item>
        <el-form-item label="字段标识" prop="fieldKey">
          <el-input v-model="dialog.form.fieldKey" placeholder="表单字段key" />
        </el-form-item>
        <el-form-item label="字段名称">
          <el-input v-model="dialog.form.fieldLabel" placeholder="字段显示名称" />
        </el-form-item>
        <el-form-item label="权限" prop="permission">
          <el-select v-model="dialog.form.permission" style="width: 100%">
            <el-option label="隐藏" value="hidden" />
            <el-option label="只读" value="readonly" />
            <el-option label="可编辑" value="editable" />
            <el-option label="必填" value="required" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { listFieldPermission, addFieldPermission, updateFieldPermission, delFieldPermission, type FieldPermissionItem } from '@/api/workflow/fieldPermission'

const loading = ref(false)
const total = ref(0)
const list = ref<FieldPermissionItem[]>([])
const queryRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  processDefinitionKey: '',
  activityId: '',
})

const dialog = reactive({
  visible: false,
  title: '新增字段权限',
  form: {
    id: undefined as number | undefined,
    processDefinitionKey: '',
    activityId: '',
    fieldKey: '',
    fieldLabel: '',
    permission: 'editable',
  },
})

const rules = {
  processDefinitionKey: [{ required: true, message: '请输入流程标识', trigger: 'blur' }],
  activityId: [{ required: true, message: '请输入节点ID', trigger: 'blur' }],
  fieldKey: [{ required: true, message: '请输入字段标识', trigger: 'blur' }],
  permission: [{ required: true, message: '请选择权限', trigger: 'change' }],
}

async function getList() {
  loading.value = true
  try {
    const res: any = await listFieldPermission(queryParams)
    list.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryRef.value?.resetFields()
  queryParams.processDefinitionKey = ''
  queryParams.activityId = ''
  handleQuery()
}

function handleAdd() {
  dialog.title = '新增字段权限'
  dialog.form = {
    id: undefined,
    processDefinitionKey: '',
    activityId: '',
    fieldKey: '',
    fieldLabel: '',
    permission: 'editable',
  }
  dialog.visible = true
}

function handleEdit(row: FieldPermissionItem) {
  dialog.title = '编辑字段权限'
  dialog.form = {
    id: row.id,
    processDefinitionKey: row.processDefinitionKey,
    activityId: row.activityId,
    fieldKey: row.fieldKey,
    fieldLabel: row.fieldLabel || '',
    permission: row.permission,
  }
  dialog.visible = true
}

async function submitForm() {
  await formRef.value?.validate()
  const data: Partial<FieldPermissionItem> = { ...dialog.form }
  if (dialog.form.id) {
    await updateFieldPermission(data)
    ElMessage.success('修改成功')
  } else {
    await addFieldPermission(data)
    ElMessage.success('新增成功')
  }
  dialog.visible = false
  getList()
}

async function handleDelete(row: FieldPermissionItem) {
  await ElMessageBox.confirm('确认删除该字段权限配置吗？', '提示', { type: 'warning' })
  await delFieldPermission(row.id!)
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => {
  getList()
})
</script>
