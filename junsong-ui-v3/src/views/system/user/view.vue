<template>
  <el-drawer v-model="visible" title="用户信息详情" direction="rtl" size="68%" append-to-body>
    <div v-loading="loading" class="drawer-content">
      <h4 class="section-header">基本信息</h4>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名称">{{ info.nickName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="归属部门">{{ info.dept?.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号码">{{ info.phonenumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ info.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登录账号">{{ info.userName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户状态">
          <el-tag :type="info.status === '0' ? 'success' : 'danger'">{{ info.status === '0' ? '正常' : '停用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="岗位">{{ postNames || '无岗位' }}</el-descriptions-item>
        <el-descriptions-item label="用户性别">
          <DictTag :options="dict.type.sys_user_sex" :value="info.sex" />
        </el-descriptions-item>
        <el-descriptions-item label="角色" :span="2">{{ roleNames || '无角色' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="section-header">其他信息</h4>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="创建者">{{ info.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ info.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新者">{{ info.updateBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ info.updateTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录IP">{{ info.loginIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后登录时间">{{ info.loginDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ info.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { getUser } from '@/api/system/user'

const dict = useDict('sys_user_sex')
const visible = ref(false)
const loading = ref(false)
const info = reactive<any>({})
const postOptions = ref<any[]>([])
const roleOptions = ref<any[]>([])

const postNames = computed(() => {
  const ids = info.postIds || []
  return postOptions.value.filter((item) => ids.includes(item.postId)).map((item) => item.postName).join('、')
})

const roleNames = computed(() => {
  const ids = info.roleIds || []
  return roleOptions.value.filter((item) => ids.includes(item.roleId)).map((item) => item.roleName).join('、')
})

function open(userId: number) {
  visible.value = true
  loading.value = true
  getUser(userId)
    .then((res: any) => {
      Object.assign(info, res.data || {})
      info.postIds = res.postIds || []
      info.roleIds = res.roleIds || []
      postOptions.value = res.posts || []
      roleOptions.value = res.roles || []
    })
    .finally(() => {
      loading.value = false
    })
}

defineExpose({ open })
</script>

<style scoped>
.drawer-content {
  padding: 0 20px 20px;
}
.section-header {
  margin: 16px 0 12px;
  color: #303133;
}
</style>
