<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="权限字符" prop="roleKey">
        <el-input v-model="queryParams.roleKey" placeholder="请输入权限字符" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="角色状态" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:role:add']">新增</el-button>
      <el-button type="success" :icon="Edit" @click="handleUpdate" :disabled="single" v-hasPermi="['system:role:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" @click="handleDelete" :disabled="multiple" v-hasPermi="['system:role:remove']">删除</el-button>
      <el-button type="warning" :icon="Download" @click="handleExport" v-hasPermi="['system:role:export']">导出</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="roleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="角色编号" align="center" key="roleId" prop="roleId" />
      <el-table-column label="角色名称" align="center" key="roleName" prop="roleName" :show-overflow-tooltip="true" />
      <el-table-column label="权限字符" align="center" key="roleKey" prop="roleKey" :show-overflow-tooltip="true" />
      <el-table-column label="显示顺序" align="center" key="roleSort" prop="roleSort" width="80" />
      <el-table-column label="状态" align="center" key="status">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['system:role:edit']">修改</el-button>
          <el-button link type="primary" @click="handleAuthUser(scope.row)" v-hasPermi="['system:role:edit']">分配用户</el-button>
          <el-button link type="primary" @click="handleAuthMenu(scope.row)" v-hasPermi="['system:role:edit']">分配菜单权限</el-button>
          <el-button link type="primary" @click="handleAuthDataScope(scope.row)" v-hasPermi="['system:role:edit']">分配数据权限</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['system:role:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="请输入角色名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色标识" prop="roleKey">
              <el-input v-model="form.roleKey" placeholder="请输入角色标识" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="角色排序" prop="roleSort">
              <el-input-number v-model="form.roleSort" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="authUserDialog.visible" :title="authUserDialog.title" width="800px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="角色名称">
          <span>{{ form.roleName }}</span>
        </el-form-item>
      </el-form>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="已分配用户" name="allocated">
          <el-input v-model="userSearch.userName" placeholder="请输入用户名称" clearable style="width: 240px; margin-bottom: 12px">
            <template #append>
              <el-button @click="loadAllocated">搜索</el-button>
            </template>
          </el-input>
          <el-table :data="allocatedUsers" border>
            <el-table-column label="用户编号" align="center" prop="userId" width="80" />
            <el-table-column label="用户名称" align="center" prop="userName" />
            <el-table-column label="用户昵称" align="center" prop="nickName" />
            <el-table-column label="邮箱" align="center" prop="email" />
            <el-table-column label="手机号码" align="center" prop="phonenumber" width="140" />
            <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
            <el-table-column label="操作" align="center" width="100">
              <template #default="scope">
                <el-button link type="danger" @click="handleAuthUserCancel(scope.row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="未分配用户" name="unallocated">
          <el-input v-model="userSearch.userName" placeholder="请输入用户名称" clearable style="width: 240px; margin-bottom: 12px">
            <template #append>
              <el-button @click="loadUnallocated">搜索</el-button>
            </template>
          </el-input>
          <el-table :data="unallocatedUsers" border @selection-change="handleUnallocatedSelectionChange">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="用户编号" align="center" prop="userId" width="80" />
            <el-table-column label="用户名称" align="center" prop="userName" />
            <el-table-column label="用户昵称" align="center" prop="nickName" />
            <el-table-column label="邮箱" align="center" prop="email" />
            <el-table-column label="手机号码" align="center" prop="phonenumber" width="140" />
            <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
          </el-table>
          <div style="margin-top: 12px; text-align: right">
            <el-button type="primary" @click="handleAuthUserSelectAll">批量选择</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="authUserDialog.visible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="authMenuDialog.visible" :title="authMenuDialog.title" width="500px" append-to-body>
      <el-tree
        ref="menuTreeRef"
        :data="menuOptions"
        :props="{ children: 'children', label: 'label' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :check-strictly="!form.menuCheckStrictly"
        @check="handleMenuCheck"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAuthMenu">确 定</el-button>
          <el-button @click="authMenuDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="dataScopeDialog.visible" :title="dataScopeDialog.title" width="600px" append-to-body>
      <el-form ref="dataScopeFormRef" :model="dataScopeForm" label-width="100px">
        <el-form-item label="角色名称">
          <span>{{ dataScopeForm.roleName }}</span>
        </el-form-item>
        <el-form-item label="权限范围">
          <el-radio-group v-model="dataScopeForm.dataScope">
            <el-radio value="1">全部数据权限</el-radio>
            <el-radio value="2">自定数据权限</el-radio>
            <el-radio value="3">本部门数据权限</el-radio>
            <el-radio value="4">本部门及以下数据权限</el-radio>
            <el-radio value="5">仅本人数据权限</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="dataScopeForm.dataScope === '2'" label="数据权限">
          <el-tree
            ref="deptTreeRef"
            :data="deptOptions"
            :props="{ children: 'children', label: 'label' }"
            node-key="id"
            show-checkbox
            default-expand-all
            v-model:checked-keys="checkedDepts"
            @check="handleDeptCheck"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitDataScope">确 定</el-button>
          <el-button @click="dataScopeDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Download } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { useDownload } from '@/composables/useDownload'

import {
  listRole,
  getRole,
  addRole,
  updateRole,
  delRole,
  allocatedUserList,
  unallocatedUserList,
  authUserCancel,
  authUserSelectAll,
  roleTreeselect,
  roleDeptTreeselect,
  dataScope,
  changeRoleStatus,
} from '@/api/system/role'

const dict = useDict('sys_normal_disable')
const { download } = useDownload()

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const roleList = ref<any[]>([])
const dateRange = ref<any[]>([])

const menuOptions = ref<any[]>([])
const checkedMenus = ref<any[]>([])
const menuTreeRef = ref()

const deptOptions = ref<any[]>([])
const checkedDepts = ref<any[]>([])
const deptTreeRef = ref()

const allocatedUsers = ref<any[]>([])
const unallocatedUsers = ref<any[]>([])
const selectedUserIds = ref<number[]>([])
const userSearch = reactive({ userName: '' })
const activeTab = ref('allocated')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: undefined as string | undefined,
  roleKey: undefined as string | undefined,
  status: undefined as string | undefined,
  params: {
    beginTime: undefined as string | undefined,
    endTime: undefined as string | undefined,
  },
})

const formDialog = reactive({ visible: false, title: '' })
const form = reactive<any>({
  roleId: undefined,
  roleName: '',
  roleKey: '',
  roleSort: 0,
  status: '0',
  menuCheckStrictly: true,
  remark: '',
})

const rules = {
  roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
  roleKey: [{ required: true, message: '权限字符不能为空', trigger: 'blur' }],
  roleSort: [{ required: true, message: '角色排序不能为空', trigger: 'blur' }],
}

const authUserDialog = reactive({ visible: false, title: '' })
const authMenuDialog = reactive({ visible: false, title: '' })
const dataScopeDialog = reactive({ visible: false, title: '' })
const dataScopeForm = reactive<any>({
  roleId: undefined,
  roleName: '',
  dataScope: '1',
})

const queryFormRef = ref()
const formRef = ref()
const dataScopeFormRef = ref()

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getList() {
  loading.value = true
  if (dateRange.value && dateRange.value.length > 0) {
    queryParams.params.beginTime = dateRange.value[0]
    queryParams.params.endTime = dateRange.value[1]
  }
  listRole(queryParams)
    .then((res: any) => {
      roleList.value = res.rows
      total.value = res.total
      loading.value = false
    })
    .catch(() => {
      loading.value = false
    })
}

function resetQuery() {
  dateRange.value = []
  resetForm(queryParams, {
    pageNum: 1,
    pageSize: 10,
    roleName: undefined,
    roleKey: undefined,
    status: undefined,
    params: { beginTime: undefined, endTime: undefined },
  })
  handleQuery()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.roleId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  resetForm(form, {
    roleId: undefined,
    roleName: '',
    roleKey: '',
    roleSort: 0,
    status: '0',
    remark: '',
  })
  nextTick(() => {
    if (formRef.value) {
      ;(formRef.value as any).clearValidate?.()
    }
  })
}

function handleAdd() {
  reset()
  formDialog.visible = true
  formDialog.title = '添加角色'
}

function handleUpdate(row?: any) {
  reset()
  const roleId = row?.roleId || ids.value[0]
  getRole(roleId).then((res: any) => {
    Object.assign(form, res.data)
    formDialog.visible = true
    formDialog.title = '修改角色'
  })
}

function submitForm() {
  if (!formRef.value) return
  ;(formRef.value as any).validate((valid: boolean) => {
    if (valid) {
      if (form.roleId !== undefined) {
        updateRole(form).then(() => {
          ElMessage.success('修改成功')
          formDialog.visible = false
          getList()
        })
      } else {
        addRole(form).then(() => {
          ElMessage.success('新增成功')
          formDialog.visible = false
          getList()
        })
      }
    }
  })
}

function cancel() {
  formDialog.visible = false
  reset()
}

function handleDelete(row?: any) {
  const roleIds = row?.roleId || ids.value
  const roleNames = row?.roleName || '选中数据'
  ElMessageBox.confirm('是否确认删除角色名称为"' + roleNames + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delRole(roleIds)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleStatusChange(row: any) {
  const text = row.status === '0' ? '启用' : '停用'
  ElMessageBox.confirm('确认要"' + text + '""' + row.roleName + '"角色吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => changeRoleStatus(row.roleId, row.status))
    .then(() => {
      ElMessage.success(text + '成功')
    })
    .catch(() => {
      row.status = row.status === '0' ? '1' : '0'
    })
}

function handleExport() {
  download('/system/role/export', queryParams, `role_${new Date().getTime()}.xlsx`)
}

function handleAuthUser(row: any) {
  form.roleId = row.roleId
  form.roleName = row.roleName
  activeTab.value = 'allocated'
  userSearch.userName = ''
  loadAllocated()
  authUserDialog.visible = true
  authUserDialog.title = '分配用户'
}

function loadAllocated() {
  allocatedUserList({ roleId: form.roleId, userName: userSearch.userName, pageNum: 1, pageSize: 100 }).then((res: any) => {
    allocatedUsers.value = res.rows
  })
}

function loadUnallocated() {
  unallocatedUserList({ roleId: form.roleId, userName: userSearch.userName, pageNum: 1, pageSize: 100 }).then((res: any) => {
    unallocatedUsers.value = res.rows
  })
}

function handleAuthUserCancel(row: any) {
  ElMessageBox.confirm('确认要取消该用户"' + row.userName + '"角色吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await authUserCancel({ userId: row.userId, roleId: form.roleId })
      ElMessage.success('取消成功')
      loadAllocated()
    })
    .catch(() => {})
}

function handleAuthUserSelectAll() {
  const userIds = selectedUserIds.value.length > 0 ? selectedUserIds.value : unallocatedUsers.value.map((u) => u.userId)
  if (userIds.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }
  ElMessageBox.confirm('确认要将选中的用户分配到该角色吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await authUserSelectAll({ userIds: userIds.join(','), roleId: form.roleId })
      ElMessage.success('分配成功')
      loadAllocated()
    })
    .catch(() => {})
}

function handleUnallocatedSelectionChange(selection: any[]) {
  selectedUserIds.value = selection.map((item) => item.userId)
}

function handleAuthMenu(row: any) {
  Object.assign(form, row)
  roleTreeselect(row.roleId).then((res: any) => {
    menuOptions.value = res.menus
    checkedMenus.value = res.checkedKeys || []
    authMenuDialog.visible = true
    authMenuDialog.title = '分配菜单权限'
    nextTick(() => {
      ;(menuTreeRef.value as any)?.setCheckedKeys?.(checkedMenus.value)
    })
  })
}

function handleMenuCheck(_: any, checkedInfo: any) {
  checkedMenus.value = checkedInfo.checkedKeys || []
}

function submitAuthMenu() {
  const tree = menuTreeRef.value as any
  const menuIds = [
    ...(tree?.getCheckedKeys?.() || checkedMenus.value),
    ...(tree?.getHalfCheckedKeys?.() || []),
  ]
  updateRole({ ...form, menuIds }).then(() => {
    ElMessage.success('分配成功')
    authMenuDialog.visible = false
    getList()
  })
}

function handleAuthDataScope(row: any) {
  dataScopeForm.roleId = row.roleId
  dataScopeForm.roleName = row.roleName
  dataScopeForm.dataScope = row.dataScope || '1'
  roleDeptTreeselect(row.roleId).then((res: any) => {
    deptOptions.value = res.depts || res.data || []
    checkedDepts.value = res.checkedKeys || []
    dataScopeDialog.visible = true
    dataScopeDialog.title = '分配数据权限'
    nextTick(() => {
      ;(deptTreeRef.value as any)?.setCheckedKeys?.(checkedDepts.value)
    })
  })
}

function handleDeptCheck(_: any, checkedInfo: any) {
  checkedDepts.value = checkedInfo.checkedKeys || []
}

function submitDataScope() {
  const tree = deptTreeRef.value as any
  const payload: any = {
    roleId: dataScopeForm.roleId,
    dataScope: dataScopeForm.dataScope,
    deptIds: dataScopeForm.dataScope === '2' ? (tree?.getCheckedKeys?.() || checkedDepts.value) : [],
  }
  dataScope(payload).then(() => {
    ElMessage.success('分配成功')
    dataScopeDialog.visible = false
    getList()
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>
