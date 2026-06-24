<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="登录地址" prop="ipaddr">
        <el-input v-model="queryParams.ipaddr" placeholder="请输入登录地址" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="danger" :icon="Delete" @click="handleForceLogout" :disabled="multiple" v-hasPermi="['monitor:online:forceLogout']">强退用户</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="onlineList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="会话编号" align="center" prop="tokenId" width="180" />
      <el-table-column label="登录名" align="center" prop="userName" :show-overflow-tooltip="true" />
      <el-table-column label="部门名称" align="center" prop="deptName" :show-overflow-tooltip="true" />
      <el-table-column label="主机" align="center" prop="ipaddr" width="160" />
      <el-table-column label="浏览器" align="center" prop="browser" :show-overflow-tooltip="true" />
      <el-table-column label="操作系统" align="center" prop="os" :show-overflow-tooltip="true" />
      <el-table-column label="登录时间" align="center" prop="loginTime" width="160" />
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleForceLogout(scope.row)" v-hasPermi="['monitor:online:forceLogout']">强制退出</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { useAuth } from '@/composables/useAuth'
import { listOnline, forceLogout } from '@/api/monitor/online'

const { hasPermi } = useAuth()

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<string[]>([])
const multiple = ref(true)
const total = ref(0)
const onlineList = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ipaddr: undefined as string | undefined,
  userName: undefined as string | undefined,
})

const queryFormRef = ref()

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getList() {
  loading.value = true
  listOnline(queryParams)
    .then((res: any) => {
      onlineList.value = res.rows || []
      total.value = res.total || onlineList.value.length
      loading.value = false
    })
    .catch(() => {
      loading.value = false
    })
}

function resetQuery() {
  resetForm(queryParams, {
    pageNum: 1,
    pageSize: 10,
    ipaddr: undefined,
    userName: undefined,
  })
  handleQuery()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.tokenId)
  multiple.value = !selection.length
}

function handleForceLogout(row?: any) {
  const tokenIds = row?.tokenId || ids.value.join(',')
  const userNames = row?.userName || '选中数据'
  ElMessageBox.confirm('是否确认强退用户"' + userNames + '"？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await forceLogout(tokenIds)
      ElMessage.success('操作成功')
      getList()
    })
    .catch(() => {})
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
