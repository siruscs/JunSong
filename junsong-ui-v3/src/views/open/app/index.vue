<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="应用名称" prop="appName">
        <el-input v-model="queryParams.appName" placeholder="请输入应用名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="应用类型" prop="appType">
        <el-select v-model="queryParams.appType" placeholder="应用类型" clearable style="width: 200px">
          <el-option label="Web应用" value="web" />
          <el-option label="移动应用" value="app" />
          <el-option label="小程序" value="miniprogram" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="应用状态" clearable style="width: 200px">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['open:app:add']">新增</el-button>
      <el-button type="success" :icon="Edit" @click="handleUpdate" :disabled="single" v-hasPermi="['open:app:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" @click="handleDelete" :disabled="multiple" v-hasPermi="['open:app:remove']">删除</el-button>
    </RightToolbar>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="appList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="应用ID" align="center" prop="id" width="80" />
      <el-table-column label="应用名称" align="center" prop="appName" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="appType" width="100">
        <template #default="scope">
          <el-tag>{{ typeMap[scope.row.appType] || scope.row.appType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="联系人" align="center" prop="contactName" width="100" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)">{{ statusMap[scope.row.status] || scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" width="280">
        <template #default="scope">
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['open:app:edit']">修改</el-button>
          <el-button link type="success" @click="handleKeys(scope.row)" v-hasPermi="['open:app:query']">API Key</el-button>
          <el-button link type="warning" v-if="scope.row.status === 'PENDING'" @click="handleApprove(scope.row)" v-hasPermi="['open:app:approve']">审批</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['open:app:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="应用名称" prop="appName">
              <el-input v-model="form.appName" placeholder="请输入应用名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应用类型" prop="appType">
              <el-select v-model="form.appType" placeholder="请选择" style="width: 100%">
                <el-option label="Web应用" value="web" />
                <el-option label="移动应用" value="app" />
                <el-option label="小程序" value="miniprogram" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="应用描述" prop="description">
              <el-input v-model="form.description" type="textarea" placeholder="请输入应用描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="官网地址" prop="websiteUrl">
              <el-input v-model="form.websiteUrl" placeholder="https://" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="回调地址" prop="callbackUrl">
              <el-input v-model="form.callbackUrl" placeholder="https://" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系邮箱" prop="contactEmail">
              <el-input v-model="form.contactEmail" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveDialog.visible" title="应用审批" width="500px" append-to-body>
      <div style="margin-bottom: 16px">
        <strong>应用名称：</strong>{{ approveDialog.appName }}
      </div>
      <el-form-item label="审批操作">
        <el-radio-group v-model="approveDialog.action">
          <el-radio value="approve">通过</el-radio>
          <el-radio value="reject">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="驳回原因" v-if="approveDialog.action === 'reject'">
        <el-input v-model="approveDialog.rejectReason" type="textarea" placeholder="请输入驳回原因" />
      </el-form-item>
      <template #footer>
        <el-button type="primary" @click="submitApprove">确 定</el-button>
        <el-button @click="approveDialog.visible = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- API Key 弹窗 -->
    <el-dialog v-model="keysDialog.visible" title="API Key 管理" width="800px" append-to-body>
      <div style="margin-bottom: 12px">
        <strong>应用：</strong>{{ keysDialog.appName }}
      </div>
      <el-table :data="keysDialog.keys" v-loading="keysDialog.loading">
        <el-table-column label="AppKey" prop="appKey" :show-overflow-tooltip="true" />
        <el-table-column label="AppSecret" width="200">
          <template #default="scope">
            <span>{{ scope.row.appSecret }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.keyType === 'production' ? 'success' : 'warning'">
              {{ scope.row.keyType === 'production' ? '生产' : '测试' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="日配额" prop="dailyQuota" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-switch :model-value="scope.row.status === '0'" @change="(val) => handleKeyStatusChange(scope.row, val)" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
      </el-table>
      <el-alert type="info" :closable="false" style="margin-top: 12px">
        测试Key：注册时自动发放，配额100次/天。生产Key：审批通过后发放，配额10000次/天。
      </el-alert>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { useAuth } from '@/composables/useAuth'
import { listApp, getApp, addApp, updateApp, delApp, approveApp, rejectApp, listAppKeys, changeKeyStatus } from '@/api/open/app'

const { hasPermi } = useAuth()

const typeMap: Record<string, string> = { web: 'Web应用', app: '移动应用', miniprogram: '小程序' }
const statusMap: Record<string, string> = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }
function statusTag(status: string) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info'
}

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const appList = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  appName: undefined,
  appType: undefined,
  status: undefined,
})

function getList() {
  loading.value = true
  listApp(queryParams).then(res => {
    appList.value = res.rows
    total.value = res.total
  }).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { (queryParams as any).appName = undefined; (queryParams as any).appType = undefined; (queryParams as any).status = undefined; handleQuery() }
function handleSelectionChange(selection: any[]) { ids.value = selection.map(i => i.id); single.value = selection.length !== 1; multiple.value = !selection.length }

const formDialog = reactive({ visible: false, title: '' })
const formRef = ref()
const form = reactive<any>({ id: undefined, appName: '', appType: 'web', description: '', websiteUrl: '', callbackUrl: '', contactName: '', contactPhone: '', contactEmail: '' })
const rules = {
  appName: [{ required: true, message: '应用名称不能为空', trigger: 'blur' }],
  appType: [{ required: true, message: '应用类型不能为空', trigger: 'change' }],
  contactName: [{ required: true, message: '联系人不能为空', trigger: 'blur' }],
}

function reset() {
  Object.assign(form, { id: undefined, appName: '', appType: 'web', description: '', websiteUrl: '', callbackUrl: '', contactName: '', contactPhone: '', contactEmail: '' })
}

function handleAdd() { reset(); formDialog.visible = true; formDialog.title = '新增应用' }

function handleUpdate(row?: any) {
  reset()
  const id = row?.id || ids.value[0]
  getApp(id).then(res => {
    Object.assign(form, res.data)
    formDialog.visible = true
    formDialog.title = '修改应用'
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    if (form.id) {
      updateApp(form).then(() => { ElMessage.success('修改成功'); formDialog.visible = false; getList() })
    } else {
      addApp(form).then(() => { ElMessage.success('新增成功，已自动发放测试Key'); formDialog.visible = false; getList() })
    }
  })
}

function cancel() { formDialog.visible = false; reset() }

function handleDelete(row?: any) {
  const delIds = row?.id || ids.value
  ElMessageBox.confirm('确认删除选中的应用?', '提示', { type: 'warning' }).then(() => {
    delApp(delIds).then(() => { ElMessage.success('删除成功'); getList() })
  }).catch(() => {})
}

const approveDialog = reactive({ visible: false, appId: 0, appName: '', action: 'approve', rejectReason: '' })

function handleApprove(row: any) {
  approveDialog.appId = row.id
  approveDialog.appName = row.appName
  approveDialog.action = 'approve'
  approveDialog.rejectReason = ''
  approveDialog.visible = true
}

function submitApprove() {
  if (approveDialog.action === 'approve') {
    approveApp(approveDialog.appId).then(() => { ElMessage.success('审批通过，已发放生产Key'); approveDialog.visible = false; getList() })
  } else {
    if (!approveDialog.rejectReason) { ElMessage.warning('请输入驳回原因'); return }
    rejectApp(approveDialog.appId, approveDialog.rejectReason).then(() => { ElMessage.success('已驳回'); approveDialog.visible = false; getList() })
  }
}

const keysDialog = reactive({ visible: false, appName: '', appId: 0, keys: [] as any[], loading: false })

function handleKeys(row: any) {
  keysDialog.appName = row.appName
  keysDialog.appId = row.id
  keysDialog.visible = true
  keysDialog.loading = true
  listAppKeys(row.id).then(res => {
    keysDialog.keys = res.data || []
  }).finally(() => { keysDialog.loading = false })
}

function handleKeyStatusChange(row: any, val: boolean) {
  const newStatus = val ? '0' : '1'
  changeKeyStatus({ id: row.id, status: newStatus }).then(() => {
    row.status = newStatus
    ElMessage.success(val ? '已启用' : '已停用')
  })
}

onMounted(() => { getList() })
</script>
