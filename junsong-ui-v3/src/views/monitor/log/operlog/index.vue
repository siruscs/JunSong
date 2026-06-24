<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="系统模块" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入系统模块" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作人员" prop="operName">
        <el-input v-model="queryParams.operName" placeholder="请输入操作人员" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="businessType">
        <el-select v-model="queryParams.businessType" placeholder="操作类型" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_oper_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="操作状态" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_common_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作时间">
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
      <el-button type="danger" :icon="Delete" @click="handleDelete" :disabled="multiple" v-hasPermi="['monitor:operlog:remove', 'system:operlog:remove']">删除</el-button>
      <el-button type="warning" :icon="Refresh" @click="handleClean" v-hasPermi="['monitor:operlog:remove', 'system:operlog:remove']">清空</el-button>
      <el-button type="warning" :icon="Download" @click="handleExport" v-hasPermi="['monitor:operlog:export', 'system:operlog:export']">导出</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="operlogList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="日志编号" align="center" prop="operId" width="80" />
      <el-table-column label="系统模块" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="操作类型" align="center" width="100">
        <template #default="scope">
          <DictTag :options="dict.type.sys_oper_type" :value="scope.row.businessType" />
        </template>
      </el-table-column>
      <el-table-column label="操作人员" align="center" prop="operName" width="120" />
      <el-table-column label="请求方式" align="center" prop="requestMethod" width="100" />
      <el-table-column label="操作状态" align="center" width="80">
        <template #default="scope">
          <DictTag :options="dict.type.sys_common_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作地址" align="center" prop="operIp" width="140" />
      <el-table-column label="操作地点" align="center" prop="operLocation" :show-overflow-tooltip="true" />
      <el-table-column label="操作时间" align="center" prop="operTime" width="160" />
      <el-table-column label="操作" align="center" width="90">
        <template #default="scope">
          <el-button link type="primary" @click="handleView(scope.row)" v-hasPermi="['monitor:operlog:query', 'system:operlog:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="detailDialog.visible" title="操作日志详情" width="760px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作模块">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="登录信息">{{ detail.operName }} / {{ detail.operIp }} / {{ detail.operLocation }}</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="log-pre">{{ detail.operParam || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回参数" :span="2">
          <pre class="log-pre">{{ detail.jsonResult || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <DictTag :options="dict.type.sys_common_status" :value="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="消耗时间">{{ detail.costTime ? detail.costTime + '毫秒' : '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.status === 1 || detail.status === '1'" label="异常信息" :span="2">
          <pre class="log-pre">{{ detail.errorMsg || '-' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="detailDialog.visible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, Download } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { useAuth } from '@/composables/useAuth'
import { useDownload } from '@/composables/useDownload'
import { listOperlog, delOperlog, cleanOperlog, getOperlog } from '@/api/system/operlog'

const { hasPermi } = useAuth()
const { download } = useDownload()
const dict = useDict('sys_common_status', 'sys_oper_type')

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const operlogList = ref<any[]>([])
const dateRange = ref<any[]>([])
const detailDialog = reactive({ visible: false })
const detail = reactive<any>({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: undefined as string | undefined,
  operName: undefined as string | undefined,
  businessType: undefined as string | undefined,
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
  listOperlog(queryParams)
    .then((res: any) => {
      operlogList.value = res.rows || []
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
    title: undefined,
    operName: undefined,
    businessType: undefined,
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
  ids.value = selection.map((item) => item.operId)
  multiple.value = !selection.length
}

function handleDelete(row?: any) {
  const operIds = row?.operId || ids.value
  ElMessageBox.confirm('是否确认删除操作日志编号为"' + operIds + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delOperlog(operIds)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleClean() {
  ElMessageBox.confirm('是否确认清空所有操作日志数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await cleanOperlog()
      ElMessage.success('清空成功')
      getList()
    })
    .catch(() => {})
}

function handleView(row: any) {
  getOperlog(row.operId).then((res: any) => {
    Object.assign(detail, res.data || {})
    detailDialog.visible = true
  })
}

function handleExport() {
  download('/system/operlog/export', queryParams, `operlog_${new Date().getTime()}.xlsx`)
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.log-pre {
  max-height: 180px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
