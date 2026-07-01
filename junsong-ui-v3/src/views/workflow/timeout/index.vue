<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="流程标识" prop="processDefinitionKey">
        <el-input v-model="queryParams.processDefinitionKey" placeholder="流程标识" clearable />
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
      <el-table-column label="节点名称" prop="activityName" min-width="120" />
      <el-table-column label="超时时间(分钟)" prop="timeoutMinutes" width="120" />
      <el-table-column label="升级类型" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.escalationType === 'urge'" size="small">催办</el-tag>
          <el-tag v-else-if="row.escalationType === 'transfer'" size="small" type="warning">转上级</el-tag>
          <el-tag v-else-if="row.escalationType === 'auto_approve'" size="small" type="success">自动通过</el-tag>
          <el-tag v-else-if="row.escalationType === 'auto_reject'" size="small" type="danger">自动驳回</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="升级目标" prop="escalationTarget" min-width="120" show-overflow-tooltip />
      <el-table-column label="工作日计算" width="100">
        <template #default="{ row }">
          {{ row.isWorkday === '1' ? '是' : '否' }}
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

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="560px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="120px">
        <el-form-item label="流程标识" prop="processDefinitionKey">
          <el-input v-model="dialog.form.processDefinitionKey" placeholder="如：leave-apply" />
        </el-form-item>
        <el-form-item label="节点ID" prop="activityId">
          <el-input v-model="dialog.form.activityId" placeholder="BPMN节点ID" />
        </el-form-item>
        <el-form-item label="节点名称">
          <el-input v-model="dialog.form.activityName" placeholder="节点显示名称" />
        </el-form-item>
        <el-form-item label="超时时间(分钟)" prop="timeoutMinutes">
          <el-input-number v-model="dialog.form.timeoutMinutes" :min="1" :max="99999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="升级类型" prop="escalationType">
          <el-select v-model="dialog.form.escalationType" style="width: 100%">
            <el-option label="催办" value="urge" />
            <el-option label="转上级" value="transfer" />
            <el-option label="自动通过" value="auto_approve" />
            <el-option label="自动驳回" value="auto_reject" />
          </el-select>
        </el-form-item>
        <el-form-item label="升级目标" v-if="dialog.form.escalationType === 'transfer'">
          <el-input v-model="dialog.form.escalationTarget" placeholder="转办目标用户名" />
        </el-form-item>
        <el-form-item label="按工作日计算">
          <el-radio-group v-model="dialog.form.isWorkday">
            <el-radio label="0">否</el-radio>
            <el-radio label="1">是</el-radio>
          </el-radio-group>
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
import { listTimeout, addTimeout, updateTimeout, delTimeout, type TimeoutItem } from '@/api/workflow/timeout'

const loading = ref(false)
const total = ref(0)
const list = ref<TimeoutItem[]>([])
const queryRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  processDefinitionKey: '',
})

const dialog = reactive({
  visible: false,
  title: '新增超时配置',
  form: {
    id: undefined as number | undefined,
    processDefinitionKey: '',
    activityId: '',
    activityName: '',
    timeoutMinutes: 60,
    escalationType: 'urge',
    escalationTarget: '',
    isWorkday: '0',
  },
})

const rules = {
  processDefinitionKey: [{ required: true, message: '请输入流程标识', trigger: 'blur' }],
  activityId: [{ required: true, message: '请输入节点ID', trigger: 'blur' }],
  timeoutMinutes: [{ required: true, message: '请输入超时时间', trigger: 'blur' }],
  escalationType: [{ required: true, message: '请选择升级类型', trigger: 'change' }],
}

async function getList() {
  loading.value = true
  try {
    const res: any = await listTimeout(queryParams)
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
  handleQuery()
}

function handleAdd() {
  dialog.title = '新增超时配置'
  dialog.form = {
    id: undefined,
    processDefinitionKey: '',
    activityId: '',
    activityName: '',
    timeoutMinutes: 60,
    escalationType: 'urge',
    escalationTarget: '',
    isWorkday: '0',
  }
  dialog.visible = true
}

function handleEdit(row: TimeoutItem) {
  dialog.title = '编辑超时配置'
  dialog.form = {
    id: row.id,
    processDefinitionKey: row.processDefinitionKey,
    activityId: row.activityId,
    activityName: row.activityName || '',
    timeoutMinutes: row.timeoutMinutes,
    escalationType: row.escalationType,
    escalationTarget: row.escalationTarget || '',
    isWorkday: row.isWorkday || '0',
  }
  dialog.visible = true
}

async function submitForm() {
  await formRef.value?.validate()
  const data: Partial<TimeoutItem> = { ...dialog.form }
  if (dialog.form.id) {
    await updateTimeout(data)
    ElMessage.success('修改成功')
  } else {
    await addTimeout(data)
    ElMessage.success('新增成功')
  }
  dialog.visible = false
  getList()
}

async function handleDelete(row: TimeoutItem) {
  await ElMessageBox.confirm('确认删除该超时配置吗？', '提示', { type: 'warning' })
  await delTimeout(row.id!)
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => {
  getList()
})
</script>
