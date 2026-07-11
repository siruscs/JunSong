<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="AppKey" prop="appKey">
        <el-input
          v-model="queryParams.appKey"
          placeholder="请输入 AppKey"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="请求方法" prop="requestMethod">
        <el-select v-model="queryParams.requestMethod" placeholder="全部" clearable style="width: 120px">
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
          <el-option label="PUT" value="PUT" />
          <el-option label="DELETE" value="DELETE" />
        </el-select>
      </el-form-item>
      <el-form-item label="响应状态" prop="responseCode">
        <el-select v-model="queryParams.responseCode" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功 (2xx)" :value="200" />
          <el-option label="失败 (4xx)" :value="400" />
          <el-option label="异常 (5xx)" :value="500" />
        </el-select>
      </el-form-item>
      <el-form-item label="调用状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功" value="success" />
          <el-option label="失败" value="fail" />
        </el-select>
      </el-form-item>
      <el-form-item label="Key类型" prop="keyType">
        <el-select v-model="queryParams.keyType" placeholder="全部" clearable style="width: 120px">
          <el-option label="生产" value="production" />
          <el-option label="测试" value="sandbox" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['open:log:export']">
          导出
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="logList" @sort-change="handleSortChange">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="AppKey" align="center" prop="appKey" width="160" show-overflow-tooltip />
      <el-table-column label="请求方法" align="center" prop="requestMethod" width="90">
        <template #default="{ row }">
          <el-tag :type="methodTagType(row.requestMethod)" size="small">{{ row.requestMethod }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="请求路径" align="left" prop="requestPath" min-width="260" show-overflow-tooltip />
      <el-table-column label="请求IP" align="center" prop="requestIp" width="140" />
      <el-table-column label="请求ID" align="center" prop="requestId" width="160" show-overflow-tooltip />
      <el-table-column label="状态码" align="center" prop="responseCode" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.responseCode)" size="small">{{ row.responseCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="调用状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">{{ row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Key类型" align="center" prop="keyType" width="90">
        <template #default="{ row }">
          <el-tag :type="row.keyType === 'production' ? 'success' : 'warning'" size="small">{{ row.keyType === 'production' ? '生产' : '测试' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="错误码" align="center" prop="errorCode" width="160" show-overflow-tooltip />
      <el-table-column label="耗时" align="center" prop="responseTime" width="90" sortable="custom">
        <template #default="{ row }">{{ row.responseTime }}ms</template>
      </el-table-column>
      <el-table-column label="调用时间" align="center" prop="createTime" width="170" sortable="custom">
        <template #default="{ row }">{{ parseTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listApiLog, exportApiLog } from '@/api/open/apiLog'
import { parseTime } from '@/utils/junsong'
import { saveAs } from 'file-saver'

const loading = ref(true)
const showSearch = ref(true)
const logList = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  appKey: undefined as string | undefined,
  requestMethod: undefined as string | undefined,
  responseCode: undefined as number | undefined,
  status: undefined as string | undefined,
  keyType: undefined as string | undefined,
})

function methodTagType(method: string) {
  const map: Record<string, string> = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

function statusTagType(code: number) {
  if (code >= 200 && code < 300) return 'success'
  if (code >= 400 && code < 500) return 'warning'
  if (code >= 500) return 'danger'
  return 'info'
}

function getList() {
  loading.value = true
  listApiLog(queryParams).then((res: any) => {
    logList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.appKey = undefined
  queryParams.requestMethod = undefined
  queryParams.responseCode = undefined
  queryParams.status = undefined
  queryParams.keyType = undefined
  handleQuery()
}

function handleSortChange({ prop, order }: { prop: string; order: string }) {
  // 排序由后端处理时可在此扩展
  getList()
}

function handleExport() {
  exportApiLog(queryParams).then((data: any) => {
    const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    saveAs(blob, 'API调用日志.xlsx')
  })
}

onMounted(() => {
  getList()
})
</script>
