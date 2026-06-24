<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>分配用户 - {{ role.roleName }}</span>
      </template>

      <el-form :model="queryParams" :inline="true" label-width="80px">
        <el-form-item label="用户名称">
          <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="手机号码">
          <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 220px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <RightToolbar v-model:showSearch="showSearch" @query="getList">
        <el-button type="primary" @click="openSelectUser">添加用户</el-button>
        <el-button type="danger" :disabled="!selectedUserIds.length" @click="cancelAll">批量取消授权</el-button>
      </RightToolbar>

      <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="用户名称" prop="userName" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="用户昵称" prop="nickName" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="邮箱" prop="email" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="手机" prop="phonenumber" align="center" width="130" />
        <el-table-column label="创建时间" prop="createTime" align="center" width="180" />
        <el-table-column label="操作" align="center" width="120">
          <template #default="scope">
            <el-button link type="danger" @click="cancelAuth(scope.row)">取消授权</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

      <div class="form-footer">
        <el-button @click="close">返回</el-button>
      </div>
    </el-card>

    <SelectUser v-model="selectUserVisible" :role-id="roleId" @success="getList" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import SelectUser from './selectUser.vue'
import { getRole, allocatedUserList, authUserCancel, authUserCancelAll } from '@/api/system/role'

const route = useRoute()
const router = useRouter()
const roleId = Number(route.params.roleId)
const role = reactive<any>({})
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const userList = ref<any[]>([])
const selectedUserIds = ref<number[]>([])
const selectUserVisible = ref(false)

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  roleId,
  userName: undefined,
  phonenumber: undefined,
})

function getRoleInfo() {
  getRole(roleId).then((res: any) => {
    Object.assign(role, res.data || {})
  })
}

function getList() {
  loading.value = true
  allocatedUserList(queryParams)
    .then((res: any) => {
      userList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.userName = undefined
  queryParams.phonenumber = undefined
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  selectedUserIds.value = selection.map((item) => item.userId)
}

function openSelectUser() {
  selectUserVisible.value = true
}

function cancelAuth(row: any) {
  ElMessageBox.confirm('确认要取消该用户"' + row.userName + '"角色吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => authUserCancel({ userId: row.userId, roleId }))
    .then(() => {
      ElMessage.success('取消成功')
      getList()
    })
    .catch(() => {})
}

function cancelAll() {
  ElMessageBox.confirm('确认要批量取消选中用户的角色吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => authUserCancelAll({ roleId, userIds: selectedUserIds.value.join(',') }))
    .then(() => {
      ElMessage.success('取消成功')
      getList()
    })
    .catch(() => {})
}

function close() {
  router.push('/system/role')
}

onMounted(() => {
  getRoleInfo()
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.form-footer {
  margin-top: 18px;
  text-align: center;
}
</style>
