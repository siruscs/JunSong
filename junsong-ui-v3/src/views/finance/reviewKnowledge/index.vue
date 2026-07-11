<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="问题类型" prop="problemType">
        <el-select v-model="queryParams.problemType" placeholder="全部" clearable style="width: 200px">
          <el-option v-for="opt in problemTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="可复用" prop="reusable">
        <el-select v-model="queryParams.reusable" placeholder="全部" clearable style="width: 200px">
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="搜索标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" plain :icon="Plus" @click="handleAdd" v-hasPermi="['finance:reviewKnowledge:add']">新增</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="knowledgeList">
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" min-width="160" />
      <el-table-column label="问题类型" align="center" prop="problemType" width="130">
        <template #default="{ row }">
          <el-tag>{{ problemTypeLabel(row.problemType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="问题摘要" align="center" prop="problemSummary" :show-overflow-tooltip="true" min-width="180" />
      <el-table-column label="采取动作" align="center" prop="actionTaken" :show-overflow-tooltip="true" min-width="180" />
      <el-table-column label="效果摘要" align="center" prop="resultSummary" :show-overflow-tooltip="true" min-width="140">
        <template #default="{ row }">
          <span>{{ row.resultSummary || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="可复用" align="center" width="80">
        <template #default="{ row }">
          <el-tag :type="row.reusable === '1' ? 'success' : 'info'" size="small">
            {{ row.reusable === '1' ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源处理人" align="center" prop="sourceHandlerName" width="100">
        <template #default="{ row }">
          <span>{{ row.sourceHandlerName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="{ row }">
          <span>{{ parseTime(row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)" v-hasPermi="['finance:reviewKnowledge:edit']">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="editDialogVisible" :title="editForm.knowledgeId ? '编辑知识' : '新增知识'" width="600px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="知识标题" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入知识标题" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="问题类型" prop="problemType">
          <el-select v-model="editForm.problemType" placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in problemTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题摘要" prop="problemSummary">
          <el-input v-model="editForm.problemSummary" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="原因分析" prop="rootCause">
          <el-input v-model="editForm.rootCause" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="采取动作" prop="actionTaken">
          <el-input v-model="editForm.actionTaken" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="效果摘要" prop="resultSummary">
          <el-input v-model="editForm.resultSummary" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="可复用" prop="reusable">
          <el-switch v-model="editForm.reusable" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="适用范围" prop="scope">
          <el-radio-group v-model="editForm.scope">
            <el-radio value="GLOBAL">全局知识</el-radio>
            <el-radio value="STORE">指定门店</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="editForm.scope === 'STORE'" label="所属门店" prop="deptId">
          <el-input-number v-model="editForm.deptId" :min="1" placeholder="请输入门店ID" controls-position="right" style="width: 200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">确 定</el-button>
        <el-button @click="editDialogVisible = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/junsong'
import { listReviewKnowledge, addReviewKnowledge, updateReviewKnowledge } from '@/api/finance/reviewKnowledge'
import RightToolbar from '@/components/RightToolbar/index.vue'

const loading = ref(false)
const total = ref(0)
const knowledgeList = ref<any[]>([])
const showSearch = ref(true)

const queryFormRef = ref()
const editFormRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  problemType: undefined as string | undefined,
  reusable: undefined as string | undefined,
  title: undefined as string | undefined,
})

const problemTypeOptions = [
  { label: '销售下滑', value: 'SALES_DROP' },
  { label: '费用异常', value: 'EXPENSE_SPIKE' },
  { label: '利润率偏低', value: 'PROFIT_RATE_DROP' },
  { label: '待核销费用偏高', value: 'PENDING_VERIFY' },
  { label: '分润异常', value: 'PROFIT_SHARE_EXCEPTION' },
  { label: '会员贡献下降', value: 'MEMBER_CONTRIBUTION_DROP' },
]

function problemTypeLabel(type: string) {
  const opt = problemTypeOptions.find(o => o.value === type)
  return opt ? opt.label : type || '-'
}

const editDialogVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  knowledgeId: null as number | null,
  title: '',
  problemType: '',
  problemSummary: '',
  rootCause: '',
  actionTaken: '',
  resultSummary: '',
  reusable: '1',
  scope: 'GLOBAL' as 'GLOBAL' | 'STORE',
  deptId: null as number | null,
})

const editRules = {
  title: [{ required: true, message: '请输入知识标题', trigger: 'blur' }],
  problemType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
  problemSummary: [{ required: true, message: '请输入问题摘要', trigger: 'blur' }],
  actionTaken: [{ required: true, message: '请输入采取动作', trigger: 'blur' }],
}

function getList() {
  loading.value = true
  const params: Record<string, any> = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
  }
  if (queryParams.problemType) params.problemType = queryParams.problemType
  if (queryParams.reusable) params.reusable = queryParams.reusable
  if (queryParams.title) params.title = queryParams.title

  listReviewKnowledge(params)
    .then((res: any) => {
      knowledgeList.value = res.rows || []
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
  queryFormRef.value?.resetFields?.()
  queryParams.problemType = undefined
  queryParams.reusable = undefined
  queryParams.title = undefined
  handleQuery()
}

function handleEdit(row: any) {
  editForm.knowledgeId = row.knowledgeId
  editForm.title = row.title
  editForm.problemType = row.problemType
  editForm.problemSummary = row.problemSummary
  editForm.rootCause = row.rootCause || ''
  editForm.actionTaken = row.actionTaken
  editForm.resultSummary = row.resultSummary || ''
  editForm.reusable = row.reusable || '1'
  editForm.scope = row.deptId ? 'STORE' : 'GLOBAL'
  editForm.deptId = row.deptId || null
  editDialogVisible.value = true
}

function handleAdd() {
  editForm.knowledgeId = null
  editForm.title = ''
  editForm.problemType = ''
  editForm.problemSummary = ''
  editForm.rootCause = ''
  editForm.actionTaken = ''
  editForm.resultSummary = ''
  editForm.reusable = '1'
  editForm.scope = 'GLOBAL'
  editForm.deptId = null
  editDialogVisible.value = true
}

function submitEdit() {
  if (!editFormRef.value) return
  editFormRef.value.validate((valid: boolean) => {
    if (!valid) return
    if (editForm.scope === 'GLOBAL') {
      editForm.deptId = null
    } else if (editForm.scope === 'STORE' && !editForm.deptId) {
      ElMessage.warning('门店知识必须填写所属门店')
      return
    }
    editLoading.value = true
    const promise = editForm.knowledgeId
      ? updateReviewKnowledge({ ...editForm })
      : addReviewKnowledge({ ...editForm })
    promise
      .then(() => {
        ElMessage.success(editForm.knowledgeId ? '知识更新成功' : '知识新增成功')
        editDialogVisible.value = false
        getList()
      })
      .finally(() => {
        editLoading.value = false
      })
  })
}

onMounted(() => {
  getList()
})
</script>
