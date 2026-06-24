<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="表名称" prop="tableName">
        <el-input v-model="queryParams.tableName" placeholder="请输入表名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="表描述" prop="tableComment">
        <el-input v-model="queryParams.tableComment" placeholder="请输入表描述" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :disabled="multiple" @click="handleGenTable()">生成</el-button>
      <el-button type="danger" :disabled="multiple" @click="handleDelete()">删除</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="表名称" prop="tableName" min-width="140" />
      <el-table-column label="表描述" prop="tableComment" min-width="140" />
      <el-table-column label="实体" prop="className" min-width="140" />
      <el-table-column label="创建时间" prop="createTime" width="170" />
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" align="center" width="260">
        <template #default="{ row }">
          <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
          <el-button link type="primary" @click="handleSynchDb(row)">同步</el-button>
          <el-button link type="primary" @click="handleGenTable(row)">生成代码</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="preview.visible" title="代码预览" width="80%" top="5vh" append-to-body>
      <el-tabs v-model="preview.activeName">
        <el-tab-pane v-for="(value, key) in preview.data" :key="key" :label="getPreviewName(key)" :name="getPreviewName(key)">
          <el-button style="float: right; margin-bottom: 8px" @click="copyCode(value)">复制</el-button>
          <pre class="code-preview"><code>{{ value }}</code></pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { delTable, genCode, listTable, previewTable, synchDb } from '@/api/tool/gen'

const loading = ref(false)
const showSearch = ref(true)
const tableList = ref<any[]>([])
const total = ref(0)
const ids = ref<number[]>([])
const tableNames = ref<string[]>([])
const multiple = ref(true)
const queryFormRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined as string | undefined,
  tableComment: undefined as string | undefined,
})

const preview = reactive({
  visible: false,
  data: {} as Record<string, string>,
  activeName: '',
})

function getList() {
  loading.value = true
  listTable(queryParams).then((res: any) => {
    tableList.value = res.rows || []
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
  queryParams.tableName = undefined
  queryParams.tableComment = undefined
  queryFormRef.value?.resetFields?.()
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.tableId)
  tableNames.value = selection.map((item) => item.tableName)
  multiple.value = selection.length === 0
}

function handlePreview(row: any) {
  previewTable(row.tableId).then((res: any) => {
    preview.data = res.data || {}
    preview.activeName = Object.keys(preview.data).map(getPreviewName)[0] || ''
    preview.visible = true
  })
}

function handleSynchDb(row: any) {
  synchDb(row.tableName).then(() => {
    ElMessage.success('同步成功')
    getList()
  })
}

function handleGenTable(row?: any) {
  const names = row?.tableName ? [row.tableName] : tableNames.value
  if (!names.length) {
    ElMessage.warning('请选择要生成的表')
    return
  }
  Promise.all(names.map((name) => genCode(name))).then(() => {
    ElMessage.success('生成成功')
  })
}

function handleDelete(row?: any) {
  const tableIds = row?.tableId || ids.value.join(',')
  if (!tableIds) {
    ElMessage.warning('请选择要删除的表')
    return
  }
  ElMessageBox.confirm('是否确认删除选中的代码生成配置？', '提示', { type: 'warning' })
    .then(() => delTable(tableIds))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function getPreviewName(key: string) {
  const fileName = key.substring(key.lastIndexOf('/') + 1)
  return fileName.replace('.vm', '')
}

function copyCode(value: string) {
  navigator.clipboard?.writeText(value)
  ElMessage.success('代码已复制')
}

onMounted(getList)
</script>

<style scoped>
.code-preview {
  max-height: 560px;
  overflow: auto;
  padding: 12px;
  border-radius: 6px;
  background: #f5f7fa;
}
</style>
