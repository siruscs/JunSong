<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="业务编码" prop="bizCode">
        <el-input v-model="queryParams.bizCode" placeholder="请输入业务编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="业务名称" prop="bizName">
        <el-input v-model="queryParams.bizName" placeholder="请输入业务名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="info" :icon="Document" @click="handleTemplateCenter">模板中心</el-button>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新建业务</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="bizList">
      <el-table-column label="业务编码" align="center" prop="bizCode" :show-overflow-tooltip="true" />
      <el-table-column label="业务名称" align="center" prop="bizName" :show-overflow-tooltip="true" />
      <el-table-column label="存储模式" align="center" prop="storageMode" width="100" />
      <el-table-column label="流程Key" align="center" prop="processKey" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">
            {{ scope.row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配置状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.configStatus === 'PUBLISHED' ? 'success' : 'warning'">
            {{ scope.row.configStatus === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="已发布版本" align="center" prop="publishedVersion" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleConfig(scope.row)">配置</el-button>
          <el-button link type="success" @click="handleGenerateMenu(scope.row)">生成菜单</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Document } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import { listBizObject, delBizObject, generateMenu } from '@/api/lowcode/admin'

const router = useRouter()

const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const bizList = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  bizCode: undefined as string | undefined,
  bizName: undefined as string | undefined,
})

const queryFormRef = ref()

function getList() {
  loading.value = true
  listBizObject(queryParams)
    .then((res: any) => {
      bizList.value = res.rows || res.data || []
      total.value = res.total || bizList.value.length
      loading.value = false
    })
    .catch(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 10,
    bizCode: undefined,
    bizName: undefined,
  })
  handleQuery()
}

function handleAdd() {
  router.push('/lowcode/admin-edit')
}

function handleTemplateCenter() {
  router.push('/lowcode/admin-template')
}

function handleConfig(row: any) {
  router.push('/lowcode/admin-edit/' + row.bizCode)
}

function handleGenerateMenu(row: any) {
  ElMessageBox.confirm('确认为「' + row.bizName + '」生成菜单和权限吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info',
  })
    .then(() => {
      generateMenu({ bizCode: row.bizCode, bizName: row.bizName, icon: 'documentation' })
        .then(() => {
          ElMessage.success('菜单生成成功')
        })
        .catch(() => {
          ElMessage.error('菜单生成失败')
        })
    })
    .catch(() => {})
}

function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除业务「' + row.bizName + '」吗？删除后不可恢复。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delBizObject(row.id)
      ElMessage.success('删除成功')
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
