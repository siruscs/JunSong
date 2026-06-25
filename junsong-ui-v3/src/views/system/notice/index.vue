<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="公告标题" prop="noticeTitle">
        <el-input v-model="queryParams.noticeTitle" placeholder="请输入公告标题" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作人员" prop="createBy">
        <el-input v-model="queryParams.createBy" placeholder="请输入发布人" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="公告状态" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_notice_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:notice:add']">新增</el-button>
      <el-button type="success" :icon="Edit" @click="handleUpdate" :disabled="single" v-hasPermi="['system:notice:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" @click="handleDelete" :disabled="multiple" v-hasPermi="['system:notice:remove']">删除</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="公告编号" align="center" prop="noticeId" width="80" />
      <el-table-column label="公告标题" align="center" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-button link type="primary" @click="handleViewData(scope.row)">{{ scope.row.noticeTitle }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="发布人" align="center" prop="createBy" width="120" />
      <el-table-column label="类型" align="center" width="100">
        <template #default="scope">
          <DictTag :options="dict.type.sys_notice_type" :value="scope.row.noticeType" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <DictTag :options="dict.type.sys_notice_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleReadUsers(scope.row)" v-hasPermi="['system:notice:list']">阅读用户</el-button>
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['system:notice:edit']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['system:notice:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="18">
            <el-form-item label="公告标题" prop="noticeTitle">
              <el-input v-model="form.noticeTitle" placeholder="请输入公告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="类型" prop="noticeType">
              <el-radio-group v-model="form.noticeType">
                <el-radio v-for="d in dict.type.sys_notice_type" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="内容" prop="noticeContent">
              <Editor v-model="form.noticeContent" :rows="8" placeholder="请输入公告内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="d in dict.type.sys_notice_status" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
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

    <el-dialog v-model="detailDialog.visible" title="公告详情" width="700px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="公告标题">{{ detail.noticeTitle }}</el-descriptions-item>
        <el-descriptions-item label="公告类型">
          <DictTag :options="dict.type.sys_notice_type" :value="detail.noticeType" />
        </el-descriptions-item>
        <el-descriptions-item label="公告状态">
          <DictTag :options="dict.type.sys_notice_status" :value="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="公告内容">
          <div class="notice-content" v-html="sanitizeHtml(detail.noticeContent)"></div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="detailDialog.visible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <ReadUsersDialog v-model="readUsersDialog.visible" :notice-id="readUsersDialog.noticeId" :notice-title="readUsersDialog.noticeTitle" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import Editor from '@/components/Editor/index.vue'
import ReadUsersDialog from './ReadUsers.vue'
import { useDict } from '@/composables/useDict'
import { useAuth } from '@/composables/useAuth'
import { sanitizeHtml } from '@/utils/xss'
import { listNotice, getNotice, addNotice, updateNotice, delNotice } from '@/api/system/notice'

const { hasPermi } = useAuth()
const dict = useDict('sys_notice_type', 'sys_notice_status')

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const noticeList = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  noticeTitle: undefined as string | undefined,
  createBy: undefined as string | undefined,
  status: undefined as string | undefined,
})

const formDialog = reactive({ visible: false, title: '' })
const detailDialog = reactive({ visible: false })
const readUsersDialog = reactive<any>({ visible: false, noticeId: undefined, noticeTitle: '' })
const detail = reactive<any>({})
const form = reactive<any>({
  noticeId: undefined,
  noticeTitle: '',
  noticeType: '1',
  noticeContent: '',
  status: '0',
})

const rules = {
  noticeTitle: [{ required: true, message: '公告标题不能为空', trigger: 'blur' }],
  noticeType: [{ required: true, message: '公告类型不能为空', trigger: 'blur' }],
  noticeContent: [{ required: true, message: '公告内容不能为空', trigger: 'blur' }],
}

const queryFormRef = ref()
const formRef = ref()

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getList() {
  loading.value = true
  listNotice(queryParams)
    .then((res: any) => {
      noticeList.value = res.rows
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
    noticeTitle: undefined,
    createBy: undefined,
    status: undefined,
  })
  handleQuery()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.noticeId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  resetForm(form, {
    noticeId: undefined,
    noticeTitle: '',
    noticeType: '1',
    noticeContent: '',
    status: '0',
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
  formDialog.title = '添加公告'
}

function handleUpdate(row?: any) {
  reset()
  const noticeId = row?.noticeId || ids.value[0]
  getNotice(noticeId).then((res: any) => {
    Object.assign(form, res.data)
    formDialog.visible = true
    formDialog.title = '修改公告'
  })
}

function handleViewData(row: any) {
  getNotice(row.noticeId).then((res: any) => {
    Object.assign(detail, res.data || {})
    detailDialog.visible = true
  })
}

function handleReadUsers(row: any) {
  readUsersDialog.noticeId = row.noticeId
  readUsersDialog.noticeTitle = row.noticeTitle
  readUsersDialog.visible = true
}

function submitForm() {
  if (!formRef.value) return
  ;(formRef.value as any).validate((valid: boolean) => {
    if (valid) {
      if (form.noticeId !== undefined) {
        updateNotice(form).then(() => {
          ElMessage.success('修改成功')
          formDialog.visible = false
          getList()
        })
      } else {
        addNotice(form).then(() => {
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
  const noticeIds = row?.noticeId || ids.value
  const noticeTitles = row?.noticeTitle || '选中数据'
  ElMessageBox.confirm('是否确认删除公告标题为"' + noticeTitles + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delNotice(noticeIds)
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
.notice-content {
  min-height: 80px;
  word-break: break-word;
}
</style>
