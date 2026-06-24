<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>分配角色</span>
      </template>
      <el-form label-width="90px">
        <el-form-item label="用户名称">
          <el-input v-model="user.userName" disabled />
        </el-form-item>
        <el-form-item label="用户昵称">
          <el-input v-model="user.nickName" disabled />
        </el-form-item>
      </el-form>

      <el-table ref="roleTableRef" v-loading="loading" :data="roles" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="角色编号" prop="roleId" align="center" width="100" />
        <el-table-column label="角色名称" prop="roleName" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="权限字符" prop="roleKey" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" prop="createTime" align="center" width="180" />
      </el-table>

      <div class="form-footer">
        <el-button type="primary" @click="submitForm">提交</el-button>
        <el-button @click="close">返回</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAuthRole, updateAuthRole } from '@/api/system/user'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const roleTableRef = ref()
const roles = ref<any[]>([])
const roleIds = ref<number[]>([])
const user = reactive<any>({})

function getInfo() {
  loading.value = true
  const userId = Number(route.params.userId)
  getAuthRole(userId)
    .then((res: any) => {
      Object.assign(user, res.user || {})
      roles.value = res.roles || []
      nextTick(() => {
        roles.value.forEach((role) => {
          if (role.flag) {
            ;(roleTableRef.value as any)?.toggleRowSelection(role, true)
          }
        })
      })
    })
    .finally(() => {
      loading.value = false
    })
}

function handleSelectionChange(selection: any[]) {
  roleIds.value = selection.map((item) => item.roleId)
}

function submitForm() {
  updateAuthRole({ userId: user.userId, roleIds: roleIds.value.join(',') }).then(() => {
    ElMessage.success('授权成功')
    close()
  })
}

function close() {
  router.push('/system/user')
}

onMounted(getInfo)
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
