<template>
  <el-dialog v-model="visible" :title="`「${noticeTitle}」已读用户`" width="760px" top="6vh" append-to-body @close="handleClose">
    <el-form :model="queryParams" :inline="true" label-width="80px">
      <el-form-item label="用户名称">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert :closable="false" type="info" show-icon style="margin-bottom: 12px">
      <template #title>
        共 <strong>{{ total }}</strong> 人已读
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="userList">
      <el-table-column label="用户名称" prop="userName" align="center" />
      <el-table-column label="用户昵称" prop="nickName" align="center" />
      <el-table-column label="部门" prop="deptName" align="center" />
      <el-table-column label="阅读时间" prop="readTime" align="center" width="180" />
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import Pagination from '@/components/Pagination/index.vue'
import { listNoticeReadUsers } from '@/api/system/notice'

const props = defineProps<{
  modelValue: boolean
  noticeId?: number
  noticeTitle?: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const visible = ref(false)
const loading = ref(false)
const total = ref(0)
const userList = ref<any[]>([])
const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  noticeId: undefined,
  userName: undefined,
})

watch(
  () => props.modelValue,
  (value) => {
    visible.value = value
    if (value) {
      queryParams.noticeId = props.noticeId
      getList()
    }
  },
)

function getList() {
  if (!queryParams.noticeId) return
  loading.value = true
  listNoticeReadUsers(queryParams)
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
  handleQuery()
}

function handleClose() {
  emit('update:modelValue', false)
}
</script>
