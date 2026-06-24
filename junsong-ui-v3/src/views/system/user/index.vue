<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input v-model="deptName" placeholder="请输入部门名称" clearable style="margin-bottom: 10px" />
        </div>
        <el-scrollbar>
          <el-tree
            ref="deptTreeRef"
            class="tree-border"
            :data="deptOptions"
            :props="defaultProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span>{{ node.label }}</span>
            </template>
          </el-tree>
        </el-scrollbar>
      </el-col>

      <el-col :span="20" :xs="24">
        <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="用户名称" prop="userName">
            <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="手机号码" prop="phonenumber">
            <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 240px">
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
          <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:user:add']">新增</el-button>
          <el-button type="success" :icon="Edit" @click="handleUpdate" :disabled="single" v-hasPermi="['system:user:edit']">修改</el-button>
          <el-button type="danger" :icon="Delete" @click="handleDelete" :disabled="multiple" v-hasPermi="['system:user:remove']">删除</el-button>
          <el-button type="info" :icon="Upload" @click="handleImport" v-hasPermi="['system:user:import']">导入</el-button>
          <el-button type="warning" :icon="Download" @click="handleExport" v-hasPermi="['system:user:export']">导出</el-button>
          <el-button type="info" :icon="Key" @click="handleResetPwd" :disabled="single" v-hasPermi="['system:user:resetPwd']">重置密码</el-button>
          <el-button type="primary" :icon="Switch" @click="handleSwitchDept" v-hasPermi="['system:user:edit']">部门切换</el-button>
        </RightToolbar>

        <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="用户编号" align="center" key="userId" prop="userId" />
          <el-table-column label="用户名称" align="center" key="userName" :show-overflow-tooltip="true">
            <template #default="scope">
              <el-button link type="primary" @click="handleViewData(scope.row)">{{ scope.row.userName }}</el-button>
            </template>
          </el-table-column>
          <el-table-column label="用户昵称" align="center" key="nickName" prop="nickName" :show-overflow-tooltip="true" />
          <el-table-column label="头像" align="center" key="avatar" prop="avatar" width="80">
            <template #default="scope">
              <ImagePreview v-if="scope.row.avatar" :src="scope.row.avatar" :previewSrcList="[scope.row.avatar]" />
              <el-avatar v-else :size="30">{{ scope.row.nickName }}</el-avatar>
            </template>
          </el-table-column>
          <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" :show-overflow-tooltip="true" />
          <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" width="120" />
          <el-table-column label="状态" align="center" key="status">
            <template #default="scope">
              <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
          <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['system:user:edit']">修改</el-button>
              <el-button link type="primary" @click="handleAuthRole(scope.row)" v-hasPermi="['system:user:edit']">分配角色</el-button>
              <el-button link type="primary" @click="handleResetPwd(scope.row)" v-hasPermi="['system:user:resetPwd']">重置密码</el-button>
              <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['system:user:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-col>
    </el-row>

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户部门" prop="deptIds">
              <el-tree-select
                v-model="form.deptIds"
                :data="deptOptions"
                :props="defaultProps"
                check-strictly
                multiple
                collapse-tags
                collapse-tags-tooltip
                node-key="id"
                placeholder="请选择部门"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
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
          <el-col :span="12">
            <el-form-item label="岗位" prop="postIds">
              <el-select v-model="form.postIds" multiple placeholder="请选择岗位" style="width: 100%">
                <el-option v-for="item in postOptions" :key="item.postId" :label="item.postName" :value="item.postId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName" :value="item.roleId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.userId == undefined">
          <el-col :span="12">
            <el-form-item label="用户密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password maxlength="20" />
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

    <el-dialog v-model="resetPwdDialog.visible" :title="resetPwdDialog.title" width="500px" append-to-body>
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="80px">
        <el-form-item label="用户名称">
          <span>{{ form.userName }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitResetPwd">确 定</el-button>
          <el-button @click="resetPwdDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="authRoleDialog.visible" :title="authRoleDialog.title" width="800px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="用户名称">
          <span>{{ form.userName }}</span>
        </el-form-item>
        <el-form-item label="归属角色">
          <el-table v-loading="loading" :data="roleOptions" @selection-change="handleRoleSelectionChange" ref="roleTableRef">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="角色编号" align="center" prop="roleId" />
            <el-table-column label="角色名称" align="center" prop="roleName" />
            <el-table-column label="权限字符" align="center" prop="roleKey" />
            <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAuthRole">确 定</el-button>
          <el-button @click="authRoleDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="switchDeptDialog.visible" :title="switchDeptDialog.title" width="500px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="当前部门">
          <span>{{ switchDeptForm.deptName }}</span>
        </el-form-item>
        <el-form-item label="切换部门" prop="switchDeptId">
          <el-tree-select v-model="switchDeptForm.switchDeptId" :data="deptOptions" :props="defaultProps" check-strictly node-key="id" placeholder="请选择部门" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitSwitchDept">确 定</el-button>
          <el-button @click="switchDeptDialog.visible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <ExcelImportDialog
      ref="importUserRef"
      title="用户导入"
      action="/system/user/importData"
      template-action="/system/user/importTemplate"
      template-file-name="user_template"
      update-support-label="是否更新已经存在的用户数据"
      @success="getList"
    />

    <UserViewDrawer ref="userViewRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox, ElForm } from 'element-plus'
import { Plus, Edit, Delete, Download, Key, Switch, Upload } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import ImagePreview from '@/components/ImagePreview/index.vue'
import ExcelImportDialog from '@/components/ExcelImportDialog/index.vue'
import UserViewDrawer from './view.vue'
import { useDict } from '@/composables/useDict'
import { useAuth } from '@/composables/useAuth'
import { useDownload } from '@/composables/useDownload'
import {
  listUser,
  getUser,
  addUser,
  updateUser,
  delUser,
  resetUserPwd,
  deptTreeSelect,
  changeUserStatus,
  switchDept,
  getUserDeptByUserId,
} from '@/api/system/user'
import { authUser } from '@/api/system/role'
import { listRole } from '@/api/system/role'
import { listPost, postOptionSelect } from '@/api/system/post'

const { hasPermi } = useAuth()
const { download } = useDownload()

const dict = useDict('sys_normal_disable')

const defaultProps = { children: 'children', label: 'label' }

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const userList = ref<any[]>([])
const deptOptions = ref<any[]>([])
const postOptions = ref<any[]>([])
const roleOptions = ref<any[]>([])
const roleIds = ref<any[]>([])
const dateRange = ref<any[]>([])
const deptName = ref('')
const deptTreeRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  deptId: undefined as number | undefined,
  userName: undefined as string | undefined,
  phonenumber: undefined as string | undefined,
  status: undefined as string | undefined,
  params: {
    beginTime: undefined as string | undefined,
    endTime: undefined as string | undefined,
  },
})

const formDialog = reactive({ visible: false, title: '' })
const form = reactive<any>({
  userId: undefined,
  deptId: undefined,
  userName: '',
  nickName: '',
  password: '',
  phonenumber: '',
  email: '',
  sex: undefined,
  status: '0',
  remark: '',
  postIds: [] as any[],
  roleIds: [] as any[],
  deptIds: [] as any[],
  dept: { deptName: '' },
})

const defaultForm = { ...form }

const rules = {
  userName: [{ required: true, message: '用户名称不能为空', trigger: 'blur' }],
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '用户密码不能为空', trigger: 'blur' }],
  deptIds: [{ required: true, type: 'array', message: '用户部门不能为空', trigger: 'change' }],
  phonenumber: [
    { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] },
  ],
}

const resetPwdDialog = reactive({ visible: false, title: '' })
const resetPwdForm = reactive<any>({
  userId: undefined,
  newPassword: '',
})
const resetPwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 20, message: '长度在 5 到 20 个字符', trigger: 'blur' },
  ],
}

const authRoleDialog = reactive({ visible: false, title: '' })
const roleTableRef = ref()

const switchDeptDialog = reactive({ visible: false, title: '' })
const switchDeptForm = reactive({
  switchDeptId: undefined as number | undefined,
  deptName: '',
})

const queryFormRef = ref()
const formRef = ref()
const resetPwdFormRef = ref()
const importUserRef = ref()
const userViewRef = ref()

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getDictDefaultValue(options: any[], fallback: any) {
  const defaultOption = options.find((item) => item.raw?.isDefault === 'Y')
  return defaultOption?.value ?? fallback
}

function getList() {
  loading.value = true
  if (dateRange.value && dateRange.value.length > 0) {
    queryParams.params.beginTime = dateRange.value[0]
    queryParams.params.endTime = dateRange.value[1]
  }
  listUser(queryParams)
    .then((res: any) => {
      userList.value = res.rows
      total.value = res.total
      loading.value = false
    })
    .catch(() => {
      loading.value = false
    })
}

function getDeptTree() {
  deptTreeSelect().then((res: any) => {
    deptOptions.value = res.data || res.depts || []
  })
}

function getPostAndRole() {
  postOptionSelect().then((res: any) => {
    postOptions.value = res.data || []
  })
  listRole({ pageNum: 1, pageSize: 100 }).then((res: any) => {
    roleOptions.value = res.rows
  })
}

function filterNode(value: string, data: any) {
  if (!value) return true
  return data.label.indexOf(value) !== -1
}

watch(deptName, (val) => {
  if (deptTreeRef.value) {
    ;(deptTreeRef.value as any).filter(val)
  }
})

function handleNodeClick(data: any) {
  queryParams.deptId = data.id
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  queryParams.deptId = undefined
  queryParams.userName = undefined
  queryParams.phonenumber = undefined
  queryParams.status = undefined
  queryParams.params.beginTime = undefined
  queryParams.params.endTime = undefined
  resetForm(queryParams, {
    pageNum: 1,
    pageSize: 10,
    deptId: undefined,
    userName: undefined,
    phonenumber: undefined,
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
  ids.value = selection.map((item) => item.userId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleStatusChange(row: any) {
  const text = row.status === '0' ? '启用' : '停用'
  ElMessageBox.confirm('确认要"' + text + '""' + row.userName + '"用户吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => changeUserStatus(row.userId, row.status))
    .then(() => {
      ElMessage.success(text + '成功')
    })
    .catch(() => {
      row.status = row.status === '0' ? '1' : '0'
    })
}

function handleRoleSelectionChange(selection: any[]) {
  roleIds.value = selection.map((item) => item.roleId)
}

function reset() {
  resetForm(form, {
    userId: undefined,
    deptId: undefined,
    userName: '',
    nickName: '',
    password: '',
    phonenumber: '',
    email: '',
    sex: undefined,
    status: '0',
    remark: '',
    postIds: [],
    roleIds: [],
    deptIds: [],
    dept: { deptName: '' },
  })
  nextTick(() => {
    if (formRef.value) {
      ;(formRef.value as any).clearValidate?.()
    }
  })
}

function handleAdd() {
  reset()
  form.status = getDictDefaultValue(dict.type.sys_normal_disable || [], '0')
  getPostAndRole()
  formDialog.visible = true
  formDialog.title = '添加用户'
}

function handleUpdate(row?: any) {
  reset()
  const userId = row?.userId || ids.value[0]
  getUser(userId).then((res: any) => {
    Object.assign(form, res.data)
    form.postIds = res.data.postIds || []
    form.roleIds = res.data.roleIds || []
    getUserDeptByUserId(userId).then((deptRes: any) => {
      const userDepts = deptRes.data || deptRes.rows || []
      form.deptIds = userDepts.map((dept: any) => dept.deptId)
      form.deptId = form.deptIds[0] || res.data.deptId
    }).catch(() => {
      form.deptIds = res.data.deptId ? [res.data.deptId] : []
      form.deptId = res.data.deptId
    })
    formDialog.visible = true
    formDialog.title = '修改用户'
  })
}

function submitForm() {
  if (!formRef.value) return
  ;(formRef.value as any).validate((valid: boolean) => {
    if (valid) {
      form.deptId = form.deptIds?.[0]
      if (form.userId !== undefined) {
        updateUser(form).then(() => {
          ElMessage.success('修改成功')
          formDialog.visible = false
          getList()
        })
      } else {
        addUser(form).then(() => {
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
  const userIds = row?.userId || ids.value
  const userNames = row?.userName || '选中数据'
  ElMessageBox.confirm('是否确认删除用户名称为"' + userNames + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delUser(userIds)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleExport() {
  download('/system/user/export', queryParams, `user_${new Date().getTime()}.xlsx`)
}

function handleImport() {
  ;(importUserRef.value as any)?.open?.()
}

function handleViewData(row: any) {
  ;(userViewRef.value as any)?.open?.(row.userId)
}

function handleResetPwd(row?: any) {
  if (row?.userId !== undefined) {
    resetPwdForm.userId = row.userId
    form.userName = row.userName
  } else {
    resetPwdForm.userId = ids.value[0]
    const selectUser = userList.value.find((item) => item.userId === resetPwdForm.userId)
    form.userName = selectUser?.userName || ''
  }
  resetPwdForm.newPassword = ''
  resetPwdDialog.visible = true
  resetPwdDialog.title = '重置密码'
}

function submitResetPwd() {
  if (!resetPwdFormRef.value) return
  ;(resetPwdFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      resetUserPwd(resetPwdForm.userId, resetPwdForm.newPassword).then(() => {
        ElMessage.success('修改成功，新密码是：' + resetPwdForm.newPassword)
        resetPwdDialog.visible = false
      })
    }
  })
}

function handleAuthRole(row: any) {
  form.userId = row.userId
  form.userName = row.userName
  form.roleIds = []
  nextTick(() => {
    listRole({ pageNum: 1, pageSize: 100 }).then((res: any) => {
      roleOptions.value = res.rows
      roleIds.value = row.roleIds || []
      if (roleTableRef.value) {
        roleOptions.value.forEach((item) => {
          if (roleIds.value.includes(item.roleId)) {
            ;(roleTableRef.value as any).toggleRowSelection(item, true)
          }
        })
      }
      authRoleDialog.visible = true
      authRoleDialog.title = '分配角色'
    })
  })
}

function submitAuthRole() {
  authUser(form.userId, roleIds.value.join(',')).then(() => {
    ElMessage.success('分配成功')
    authRoleDialog.visible = false
    getList()
  })
}

function handleSwitchDept() {
  switchDeptForm.switchDeptId = undefined
  switchDeptForm.deptName = ''
  if (deptOptions.value && deptOptions.value.length > 0) {
    switchDeptDialog.visible = true
    switchDeptDialog.title = '部门切换'
  } else {
    ElMessage.warning('请先等待部门树加载完成')
  }
}

function submitSwitchDept() {
  if (!switchDeptForm.switchDeptId) {
    ElMessage.warning('请选择要切换的部门')
    return
  }
  switchDept(switchDeptForm.switchDeptId).then(() => {
    ElMessage.success('部门切换成功')
    switchDeptDialog.visible = false
    getList()
  })
}

onMounted(() => {
  getList()
  getDeptTree()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.head-container {
  margin-bottom: 12px;
}
.tree-border {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
}
</style>
