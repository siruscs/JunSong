<template>
  <el-dialog v-model="visible" title="选择用户" width="900px" append-to-body @close="handleClose">
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

    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户名称" prop="userName" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="用户昵称" prop="nickName" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="邮箱" prop="email" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="手机" prop="phonenumber" align="center" width="130" />
      <el-table-column label="创建时间" prop="createTime" align="center" width="180" />
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submit">确 定</el-button>
        <el-button @click="handleClose">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import { unallocatedUserList, authUserSelectAll } from '@/api/system/role'

const props = defineProps<{
  modelValue: boolean
  roleId?: number
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const total = ref(0)
const userList = ref<any[]>([])
const selectedUserIds = ref<number[]>([])
const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  roleId: undefined,
  userName: undefined,
  phonenumber: undefined,
})

watch(
  () => props.modelValue,
  (value) => {
    visible.value = value
    if (value) {
      queryParams.roleId = props.roleId
      getList()
    }
  },
)

function getList() {
  if (!queryParams.roleId) return
  loading.value = true
  unallocatedUserList(queryParams)
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

function submit() {
  if (!selectedUserIds.value.length) {
    ElMessage.warning('请选择要分配的用户')
    return
  }
  authUserSelectAll({ roleId: queryParams.roleId, userIds: selectedUserIds.value.join(',') }).then(() => {
    ElMessage.success('分配成功')
    emit('success')
    handleClose()
  })
}

function handleClose() {
  emit('update:modelValue', false)
}
</script>
