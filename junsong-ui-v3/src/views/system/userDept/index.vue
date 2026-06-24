<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="用户" prop="userId">
        <el-select v-model="queryParams.userId" placeholder="请选择用户" clearable filterable style="width: 200px">
          <el-option v-for="item in userOptions" :key="item.userId" :label="item.userName" :value="item.userId" />
        </el-select>
      </el-form-item>
      <el-form-item label="部门" prop="deptId">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptTreeOptions"
          :props="defaultProps"
          check-strictly
          node-key="id"
          placeholder="请选择部门"
          clearable
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option label="在职" value="0" />
          <el-option label="离职" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:userDept:add']">新增关联</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="userDeptList">
      <el-table-column label="关联编号" align="center" prop="userDeptId" width="90" />
      <el-table-column label="用户编号" align="center" prop="userId" width="90" />
      <el-table-column label="用户名称" align="center" prop="userName" :show-overflow-tooltip="true" />
      <el-table-column label="部门编号" align="center" prop="deptId" width="90" />
      <el-table-column label="部门名称" align="center" prop="deptName" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
            {{ scope.row.status === '0' ? '在职' : '离职' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="入职日期" align="center" prop="hireDate" width="160" />
      <el-table-column label="离职日期" align="center" prop="leaveDate" width="160" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['system:userDept:edit']">修改</el-button>
          <el-button v-if="scope.row.status === '0'" link type="warning" @click="handleToggleStatus(scope.row)" v-hasPermi="['system:userDept:edit']">离职</el-button>
          <el-button v-else link type="success" @click="handleToggleStatus(scope.row)" v-hasPermi="['system:userDept:edit']">复职</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['system:userDept:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="540px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户" prop="userId">
          <el-select v-model="form.userId" placeholder="请选择用户" filterable style="width: 100%">
            <el-option v-for="item in userOptions" :key="item.userId" :label="item.userName" :value="item.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTreeOptions"
            :props="defaultProps"
            check-strictly
            node-key="id"
            placeholder="请选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">在职</el-radio>
            <el-radio value="1">离职</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="入职日期" prop="hireDate">
          <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择入职日期" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.status === '1'" label="离职日期" prop="leaveDate">
          <el-date-picker v-model="form.leaveDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择离职日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { listUser, deptTreeSelect } from '@/api/system/user'
import {
  listUserDept,
  getUserDept,
  addUserDept,
  updateUserDept,
  delUserDept,
  hireUserToDept,
  leaveUserFromDept,
} from '@/api/system/userDept'

const defaultProps = { children: 'children', label: 'label' }

const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const userDeptList = ref<any[]>([])
const userOptions = ref<any[]>([])
const deptTreeOptions = ref<any[]>([])

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  userId: undefined,
  deptId: undefined,
  status: undefined,
})

const formDialog = reactive({ visible: false, title: '' })
const form = reactive<any>({
  userDeptId: undefined,
  userId: undefined,
  deptId: undefined,
  status: '0',
  hireDate: undefined,
  leaveDate: undefined,
  remark: undefined,
})

const rules = {
  userId: [{ required: true, message: '用户不能为空', trigger: 'change' }],
  deptId: [{ required: true, message: '部门不能为空', trigger: 'change' }],
}

const queryFormRef = ref()
const formRef = ref()

function normalizeListResponse(res: any) {
  const rows = Array.isArray(res?.rows)
    ? res.rows
    : Array.isArray(res?.data)
      ? res.data
      : Array.isArray(res)
        ? res
        : []
  return { rows, total: Number(res?.total ?? rows.length) }
}

function resetForm(target: any, defaults: any) {
  Object.assign(target, defaults)
}

function getList() {
  loading.value = true
  listUserDept(queryParams)
    .then((res: any) => {
      const data = normalizeListResponse(res)
      userDeptList.value = data.rows
      total.value = data.total
    })
    .finally(() => {
      loading.value = false
    })
}

function getUserList() {
  listUser({ pageNum: 1, pageSize: 1000 }).then((res: any) => {
    userOptions.value = res.rows || []
  })
}

function getDeptTree() {
  deptTreeSelect().then((res: any) => {
    deptTreeOptions.value = res.data || res.depts || []
  })
}

function reset() {
  resetForm(form, {
    userDeptId: undefined,
    userId: undefined,
    deptId: undefined,
    status: '0',
    hireDate: undefined,
    leaveDate: undefined,
    remark: undefined,
  })
  nextTick(() => {
    ;(formRef.value as any)?.clearValidate?.()
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  resetForm(queryParams, {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    deptId: undefined,
    status: undefined,
  })
  handleQuery()
}

function handleAdd() {
  reset()
  formDialog.visible = true
  formDialog.title = '添加关联'
}

function handleUpdate(row: any) {
  reset()
  getUserDept(row.userDeptId).then((res: any) => {
    Object.assign(form, res.data || {})
    formDialog.visible = true
    formDialog.title = '修改关联'
  })
}

function submitForm() {
  if (!formRef.value) return
  ;(formRef.value as any).validate((valid: boolean) => {
    if (!valid) return
    const action = form.userDeptId !== undefined ? updateUserDept : addUserDept
    action(form).then(() => {
      ElMessage.success(form.userDeptId !== undefined ? '修改成功' : '新增成功')
      formDialog.visible = false
      getList()
    })
  })
}

function handleToggleStatus(row: any) {
  const isActive = row.status === '0'
  const text = isActive ? '离职' : '复职'
  const action = isActive ? leaveUserFromDept : hireUserToDept
  ElMessageBox.confirm(`确认要对「${row.userName}」执行「${text}」操作吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => action(row.userId, row.deptId))
    .then(() => {
      ElMessage.success(`${text}成功`)
      getList()
    })
    .catch(() => {})
}

function handleDelete(row: any) {
  ElMessageBox.confirm('是否确认删除用户与部门关联编号为"' + row.userDeptId + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => delUserDept(row.userDeptId))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function cancel() {
  formDialog.visible = false
  reset()
}

onMounted(() => {
  getList()
  getUserList()
  getDeptTree()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>
