<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="委托类型">
        <el-select v-model="queryParams.delegateType" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="all" />
          <el-option label="工作流" value="workflow" />
          <el-option label="系统" value="system" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain :icon="Plus" @click="handleAdd">新增委托</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="delegateList">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="委托人" prop="userId" width="100">
        <template #default="{ row }">
          {{ row.userName || row.userId }}
        </template>
      </el-table-column>
      <el-table-column label="代理人" prop="delegateUserId" width="100">
        <template #default="{ row }">
          {{ row.delegateUserName || row.delegateUserId }}
        </template>
      </el-table-column>
      <el-table-column label="委托类型" prop="delegateType" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.delegateType === 'all'" size="small">全部</el-tag>
          <el-tag v-else-if="row.delegateType === 'workflow'" size="small" type="success">工作流</el-tag>
          <el-tag v-else size="small" type="info">系统</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="指定流程" prop="processKeys" min-width="150" show-overflow-tooltip />
      <el-table-column label="开始时间" prop="startTime" width="160" />
      <el-table-column label="结束时间" prop="endTime" width="160" />
      <el-table-column label="状态" prop="status" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === '0'" size="small" type="success">正常</el-tag>
          <el-tag v-else size="small" type="info">停用</el-tag>
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
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="100px">
        <el-form-item label="代理人" prop="delegateUserId">
          <el-select
            v-model="dialog.form.delegateUserId"
            placeholder="输入用户名搜索"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            style="width: 100%"
          >
            <el-option v-for="u in userOptions" :key="u.userId" :label="u.userName + (u.nickName ? '(' + u.nickName + ')' : '')" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="委托类型" prop="delegateType">
          <el-radio-group v-model="dialog.form.delegateType">
            <el-radio label="all">全部</el-radio>
            <el-radio label="workflow">工作流</el-radio>
            <el-radio label="system">系统</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="dialog.form.delegateType === 'workflow'" label="指定流程">
          <el-input v-model="dialog.form.processKeys" placeholder="多个流程标识用逗号分隔，如：leave-apply,expense-reimburse" />
        </el-form-item>
        <el-form-item label="时间范围" prop="timeRange">
          <el-date-picker
            v-model="dialog.form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dialog.form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dialog.form.remark" type="textarea" :rows="2" />
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
import { listDelegate, addDelegate, updateDelegate, delDelegate, type DelegateItem } from '@/api/system/delegate'
import { listUser } from '@/api/system/user'

const loading = ref(false)
const total = ref(0)
const delegateList = ref<DelegateItem[]>([])
const userOptions = ref<any[]>([])
const userSearchLoading = ref(false)
let userSearchRequestId = 0
const queryRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  delegateType: '',
  status: '',
})

const dialog = reactive({
  visible: false,
  title: '新增委托',
  form: {
    id: undefined as number | undefined,
    delegateUserId: undefined as number | undefined,
    delegateType: 'all',
    processKeys: '',
    timeRange: [] as string[],
    status: '0',
    remark: '',
  },
})

const rules = {
  delegateUserId: [{ required: true, message: '请选择代理人', trigger: 'change' }],
  delegateType: [{ required: true, message: '请选择委托类型', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择时间范围', trigger: 'change' }],
}

async function getList() {
  loading.value = true
  try {
    const res: any = await listDelegate(queryParams)
    delegateList.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function searchUsers(keyword = '') {
  const requestId = ++userSearchRequestId
  userSearchLoading.value = true
  try {
    const res: any = await listUser({ userName: keyword.trim() || undefined, pageNum: 1, pageSize: 20 })
    if (requestId !== userSearchRequestId) return
    const selected = userOptions.value.find((user: any) => user.userId === dialog.form.delegateUserId)
    const rows = res.rows || []
    userOptions.value = selected && !rows.some((user: any) => user.userId === selected.userId) ? [selected, ...rows] : rows
  } finally {
    if (requestId === userSearchRequestId) userSearchLoading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryRef.value?.resetFields()
  queryParams.delegateType = ''
  queryParams.status = ''
  handleQuery()
}

function handleAdd() {
  dialog.title = '新增委托'
  dialog.form = {
    id: undefined,
    delegateUserId: undefined,
    delegateType: 'all',
    processKeys: '',
    timeRange: [],
    status: '0',
    remark: '',
  }
  dialog.visible = true
  searchUsers()
}

function handleEdit(row: DelegateItem) {
  dialog.title = '编辑委托'
  dialog.form = {
    id: row.id,
    delegateUserId: row.delegateUserId,
    delegateType: row.delegateType,
    processKeys: row.processKeys || '',
    timeRange: row.startTime && row.endTime ? [row.startTime, row.endTime] : [],
    status: row.status,
    remark: row.remark || '',
  }
  userOptions.value = [{ userId: row.delegateUserId, userName: row.delegateUserName || String(row.delegateUserId) }]
  dialog.visible = true
  searchUsers()
}

async function submitForm() {
  await formRef.value?.validate()
  const data: Partial<DelegateItem> = {
    delegateUserId: dialog.form.delegateUserId,
    delegateType: dialog.form.delegateType,
    processKeys: dialog.form.processKeys,
    startTime: dialog.form.timeRange[0],
    endTime: dialog.form.timeRange[1],
    status: dialog.form.status,
    remark: dialog.form.remark,
  }
  if (dialog.form.id) {
    data.id = dialog.form.id
    await updateDelegate(data)
    ElMessage.success('修改成功')
  } else {
    await addDelegate(data)
    ElMessage.success('新增成功')
  }
  dialog.visible = false
  getList()
}

async function handleDelete(row: DelegateItem) {
  await ElMessageBox.confirm('确认删除该委托规则吗？', '提示', { type: 'warning' })
  await delDelegate([row.id!])
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => {
  getList()
})
</script>
