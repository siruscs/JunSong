<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="岗位编码" prop="postCode">
        <el-input v-model="queryParams.postCode" placeholder="请输入岗位编码" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="岗位名称" prop="postName">
        <el-input v-model="queryParams.postName" placeholder="请输入岗位名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="岗位状态" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:post:add']">新增</el-button>
      <el-button type="success" :icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:post:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:post:remove']">删除</el-button>
      <el-button type="warning" :icon="Download" @click="handleExport" v-hasPermi="['system:post:export']">导出</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="postList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="岗位编号" align="center" key="postId" prop="postId" />
      <el-table-column label="岗位编码" align="center" key="postCode" prop="postCode" :show-overflow-tooltip="true" />
      <el-table-column label="岗位名称" align="center" key="postName" prop="postName" :show-overflow-tooltip="true" />
      <el-table-column label="岗位排序" align="center" key="postSort" prop="postSort" width="80" />
      <el-table-column label="状态" align="center" key="status">
        <template #default="scope">
          <DictTag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['system:post:edit']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['system:post:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="岗位名称" prop="postName">
              <el-input v-model="form.postName" placeholder="请输入岗位名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位编码" prop="postCode">
              <el-input v-model="form.postCode" placeholder="请输入岗位编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="岗位排序" prop="postSort">
              <el-input-number v-model="form.postSort" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Download } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { useDownload } from '@/composables/useDownload'

import { listPost, getPost, addPost, updatePost, delPost } from '@/api/system/post'

const dict = useDict('sys_normal_disable')
const { download } = useDownload()

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const postList = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  postCode: undefined as string | undefined,
  postName: undefined as string | undefined,
  status: undefined as string | undefined,
})

const formDialog = reactive({ visible: false, title: '' })
const form = reactive<any>({
  postId: undefined,
  postCode: '',
  postName: '',
  postSort: 0,
  status: '0',
  remark: '',
})

const rules = {
  postName: [{ required: true, message: '岗位名称不能为空', trigger: 'blur' }],
  postCode: [{ required: true, message: '岗位编码不能为空', trigger: 'blur' }],
  postSort: [{ required: true, message: '岗位排序不能为空', trigger: 'blur' }],
}

const queryFormRef = ref()
const formRef = ref()

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getList() {
  loading.value = true
  listPost(queryParams)
    .then((res: any) => {
      postList.value = res.rows
      total.value = res.total
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
    postCode: undefined,
    postName: undefined,
    status: undefined,
  })
  handleQuery()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.postId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  resetForm(form, {
    postId: undefined,
    postCode: '',
    postName: '',
    postSort: 0,
    status: '0',
    remark: '',
  })
  nextTick(() => {
    if (formRef.value) {
      ;(formRef.value as any).clearValidate?.()
    }
  })
}

function handleAdd() {
  reset()
  formDialog.visible = true
  formDialog.title = '添加岗位'
}

function handleUpdate(row?: any) {
  reset()
  const postId = row?.postId || ids.value[0]
  getPost(postId).then((res: any) => {
    Object.assign(form, res.data)
    formDialog.visible = true
    formDialog.title = '修改岗位'
  })
}

function submitForm() {
  if (!formRef.value) return
  ;(formRef.value as any).validate((valid: boolean) => {
    if (valid) {
      if (form.postId !== undefined) {
        updatePost(form).then(() => {
          ElMessage.success('修改成功')
          formDialog.visible = false
          getList()
        })
      } else {
        addPost(form).then(() => {
          ElMessage.success('新增成功')
          formDialog.visible = false
          getList()
        })
      }
    }
  })
}

function cancel() {
  formDialog.visible = false
  reset()
}

function handleDelete(row?: any) {
  const postIds = row?.postId || ids.value
  const postNames = row?.postName || '选中数据'
  ElMessageBox.confirm('是否确认删除岗位名称为"' + postNames + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delPost(postIds)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleExport() {
  download('/system/post/export', queryParams, `post_${new Date().getTime()}.xlsx`)
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
