<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户地址" prop="ipaddr">
        <el-input v-model="queryParams.ipaddr" placeholder="请输入用户地址" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="登录状态" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_common_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="登录时间">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="danger" :icon="Delete" @click="handleDelete" :disabled="multiple" v-hasPermi="['monitor:logininfor:remove', 'system:logininfor:remove']">删除</el-button>
      <el-button type="warning" :icon="Refresh" @click="handleClean" v-hasPermi="['monitor:logininfor:remove', 'system:logininfor:remove']">清空</el-button>
      <el-button type="primary" :icon="Unlock" @click="handleUnlock" :disabled="single" v-hasPermi="['monitor:logininfor:unlock', 'system:logininfor:unlock']">解锁</el-button>
      <el-button type="warning" :icon="Download" @click="handleExport" v-hasPermi="['monitor:logininfor:export', 'system:logininfor:export']">导出</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="logininforList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="访问编号" align="center" prop="infoId" width="80" />
      <el-table-column label="用户名称" align="center" prop="userName" :show-overflow-tooltip="true" />
      <el-table-column label="登录状态" align="center" width="80">
        <template #default="scope">
          <DictTag :options="dict.type.sys_common_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作信息" align="center" prop="msg" :show-overflow-tooltip="true" />
      <el-table-column label="登录IP" align="center" prop="ipaddr" width="140" />
      <el-table-column label="登录地点" align="center" prop="loginLocation" :show-overflow-tooltip="true" />
      <el-table-column label="浏览器" align="center" prop="browser" :show-overflow-tooltip="true" />
      <el-table-column label="操作系统" align="center" prop="os" :show-overflow-tooltip="true" />
      <el-table-column label="登录时间" align="center" prop="loginTime" width="160" />
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, Unlock, Download } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { useAuth } from '@/composables/useAuth'
import { useDownload } from '@/composables/useDownload'
import { listLogininfor, delLogininfor, cleanLogininfor, unlockUser } from '@/api/system/logininfor'

const { hasPermi } = useAuth()
const { download } = useDownload()
const dict = useDict('sys_common_status')

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const names = ref<string[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const logininforList = ref<any[]>([])
const dateRange = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ipaddr: undefined as string | undefined,
  userName: undefined as string | undefined,
  status: undefined as string | undefined,
  params: {
    beginTime: undefined as string | undefined,
    endTime: undefined as string | undefined,
  },
})

const queryFormRef = ref()

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getList() {
  loading.value = true
  if (dateRange.value && dateRange.value.length > 0) {
    queryParams.params.beginTime = dateRange.value[0]
    queryParams.params.endTime = dateRange.value[1]
  }
  listLogininfor(queryParams)
    .then((res: any) => {
      logininforList.value = res.rows || []
      total.value = res.total || 0
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
    ipaddr: undefined,
    userName: undefined,
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
  ids.value = selection.map((item) => item.infoId)
  names.value = selection.map((item) => item.userName)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleDelete(row?: any) {
  const infoIds = row?.infoId || ids.value
  ElMessageBox.confirm('是否确认删除登录日志编号为"' + infoIds + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delLogininfor(infoIds)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleClean() {
  ElMessageBox.confirm('是否确认清空所有登录日志数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await cleanLogininfor()
      ElMessage.success('清空成功')
      getList()
    })
    .catch(() => {})
}

function handleUnlock() {
  const username = names.value[0]
  ElMessageBox.confirm('是否确认解锁用户"' + username + '"数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => unlockUser(username))
    .then(() => {
      ElMessage.success('用户' + username + '解锁成功')
    })
    .catch(() => {})
}

function handleExport() {
  download('/system/logininfor/export', queryParams, `logininfor_${new Date().getTime()}.xlsx`)
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
