<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="租户名称" prop="tenantName">
        <el-input v-model="queryParams.tenantName" placeholder="请输入租户名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactName">
        <el-input v-model="queryParams.contactName" placeholder="请输入联系人" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="租户状态" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:tenant:add']">新增</el-button>
      <el-button type="success" :icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:tenant:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:tenant:remove']">删除</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="tenantList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="租户ID" align="center" prop="tenantId" width="80" />
      <el-table-column label="租户名称" align="center" prop="tenantName" :show-overflow-tooltip="true" />
      <el-table-column label="联系人" align="center" prop="contactName" width="120" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" width="130" />
      <el-table-column label="账号限制" align="center" prop="accountCount" width="100">
        <template #default="scope">
          {{ scope.row.accountCount === 0 ? '不限' : scope.row.accountCount }}
        </template>
      </el-table-column>
      <el-table-column label="到期时间" align="center" prop="expireTime" width="160">
        <template #default="scope">
          {{ scope.row.expireTime ? scope.row.expireTime : '永久' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
            @change="handleStatusChange(scope.row)" v-hasPermi="['system:tenant:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" :icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:tenant:edit']">修改</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:tenant:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="tenantRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="租户名称" prop="tenantName">
              <el-input v-model="form.tenantName" placeholder="请输入租户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="域名" prop="domain">
              <el-input v-model="form.domain" placeholder="请输入域名（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号限制" prop="accountCount">
              <el-input-number v-model="form.accountCount" :min="0" placeholder="0表示不限" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期时间" prop="expireTime">
              <el-date-picker v-model="form.expireTime" type="datetime" placeholder="选择到期时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col v-if="!form.tenantId" :span="12">
            <el-form-item label="管理员账号" prop="adminUserName">
              <el-input v-model="form.adminUserName" placeholder="请输入管理员账号" />
            </el-form-item>
          </el-col>
          <el-col v-if="!form.tenantId" :span="12">
            <el-form-item label="初始密码" prop="adminPassword">
              <el-input v-model="form.adminPassword" type="password" placeholder="请输入初始密码" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Tenant">
import { getCurrentInstance, reactive, ref, toRefs } from 'vue'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { useDict } from '@/composables/useDict'
import { listTenant, getTenant, addTenant, updateTenant, delTenant, changeTenantStatus } from '@/api/system/tenant'

const { proxy } = getCurrentInstance() as any
const dict = useDict('sys_normal_disable')

const tenantList = ref<any[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {} as any,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    tenantName: undefined,
    contactName: undefined,
    status: undefined
  },
  rules: {
    tenantName: [{ required: true, message: '租户名称不能为空', trigger: 'blur' }],
    adminUserName: [
      {
        validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
          if (!form.value.tenantId && !value) {
            callback(new Error('管理员账号不能为空'))
            return
          }
          callback()
        },
        trigger: 'blur'
      }
    ],
    adminPassword: [
      {
        validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
          if (!form.value.tenantId && !value) {
            callback(new Error('初始密码不能为空'))
            return
          }
          callback()
        },
        trigger: 'blur'
      }
    ]
  } as any
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listTenant(queryParams.value).then((res: any) => {
    tenantList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryFormRef')
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.tenantId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = {
    tenantId: undefined,
    tenantName: undefined,
    contactName: undefined,
    contactPhone: undefined,
    domain: undefined,
    status: '0',
    accountCount: 0,
    expireTime: undefined,
    adminUserName: undefined,
    adminPassword: undefined,
    remark: undefined
  }
  proxy.resetForm('tenantRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加租户'
}

function handleUpdate(row: any) {
  reset()
  const tenantId = row.tenantId
  getTenant(tenantId).then((res: any) => {
    form.value = res.data
    open.value = true
    title.value = '修改租户'
  })
}

function submitForm() {
  proxy.$refs['tenantRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.tenantId) {
        updateTenant(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addTenant(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row?: any) {
  const tenantIds = row?.tenantId ? [row.tenantId] : ids.value
  if (!tenantIds.length) {
    return
  }
  const tenantNames = row?.tenantName || tenantIds.join(',')
  proxy.$modal.confirm('是否确认删除租户"' + tenantNames + '"？').then(() => {
    return Promise.all(tenantIds.map((tenantId) => delTenant(tenantId)))
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleStatusChange(row: any) {
  const text = row.status === '0' ? '启用' : '停用'
  proxy.$modal.confirm('确认要' + text + '"' + row.tenantName + '"吗？').then(() => {
    return changeTenantStatus(row.tenantId, row.status)
  }).then(() => {
    proxy.$modal.msgSuccess(text + '成功')
  }).catch(() => {
    row.status = row.status === '0' ? '1' : '0'
  })
}

function cancel() {
  open.value = false
  reset()
}

getList()
</script>
