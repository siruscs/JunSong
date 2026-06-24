<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="任务名称" prop="jobName">
        <el-input v-model="queryParams.jobName" placeholder="请输入任务名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务组名" prop="jobGroup">
        <el-select v-model="queryParams.jobGroup" placeholder="任务组名" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_job_group" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="任务状态" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['monitor:job:add']">新增</el-button>
      <el-button type="success" :icon="Edit" @click="handleUpdate" :disabled="single" v-hasPermi="['monitor:job:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" @click="handleDelete" :disabled="multiple" v-hasPermi="['monitor:job:remove']">删除</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="jobList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务编号" align="center" prop="jobId" width="80" />
      <el-table-column label="任务名称" align="center" prop="jobName" :show-overflow-tooltip="true" />
      <el-table-column label="任务组名" align="center" prop="jobGroup" width="120">
        <template #default="scope">
          <DictTag :options="dict.type.sys_job_group" :value="scope.row.jobGroup" />
        </template>
      </el-table-column>
      <el-table-column label="调用目标字符串" align="center" prop="invokeTarget" :show-overflow-tooltip="true" />
      <el-table-column label="cron执行表达式" align="center" prop="cronExpression" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="'0'"
            :inactive-value="'1'"
            :loading="scope.row.isLoading || false"
            @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['monitor:job:edit']">修改</el-button>
          <el-button link type="primary" @click="handleDelete(scope.row)" v-hasPermi="['monitor:job:remove']">删除</el-button>
          <el-button link type="primary" @click="handleRun(scope.row)" v-hasPermi="['monitor:job:edit']">执行一次</el-button>
          <el-button link type="primary" @click="handleJobLog(scope.row)">任务日志</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="任务名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务分组" prop="jobGroup">
              <el-select v-model="form.jobGroup" placeholder="请选择任务分组" style="width: 100%">
                <el-option v-for="d in dict.type.sys_job_group" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="调用方法" prop="invokeTarget">
              <el-input v-model="form.invokeTarget" placeholder="如：ryTask.ryParams('ry')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="任务表达式" prop="cronExpression">
              <el-input v-model="form.cronExpression" placeholder="如：0/10 * * * * ?" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="执行策略" prop="misfirePolicy">
              <el-radio-group v-model="form.misfirePolicy">
                <el-radio value="1">立即执行</el-radio>
                <el-radio value="2">执行一次</el-radio>
                <el-radio value="3">放弃执行</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否并发" prop="concurrent">
              <el-radio-group v-model="form.concurrent">
                <el-radio value="0">允许</el-radio>
                <el-radio value="1">禁止</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注信息">
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

    <el-dialog v-model="jobLogDialog.visible" title="执行日志" width="900px" append-to-body>
      <el-table :data="jobLogList" border>
        <el-table-column label="日志编号" align="center" prop="jobLogId" width="100" />
        <el-table-column label="任务名称" align="center" prop="jobName" />
        <el-table-column label="任务组名" align="center" prop="jobGroup" />
        <el-table-column label="执行目标" align="center" prop="invokeTarget" :show-overflow-tooltip="true" />
        <el-table-column label="执行时间" align="center" prop="createTime" width="160" />
        <el-table-column label="执行状态" align="center" width="80">
          <template #default="scope">
            <DictTag :options="dict.type.sys_common_status" :value="scope.row.status" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="jobLogDialog.visible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { useAuth } from '@/composables/useAuth'
import { listJob, getJob, addJob, updateJob, delJob, runJob, changeJobStatus, listJobLog } from '@/api/monitor/job'

const { hasPermi } = useAuth()
const dict = useDict('sys_normal_disable', 'sys_job_group', 'sys_common_status')

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const jobList = ref<any[]>([])
const jobLogList = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  jobName: undefined as string | undefined,
  jobGroup: undefined as string | undefined,
  status: undefined as string | undefined,
})

const formDialog = reactive({ visible: false, title: '' })
const jobLogDialog = reactive({ visible: false })
const form = reactive<any>({
  jobId: undefined,
  jobName: '',
  jobGroup: 'DEFAULT',
  invokeTarget: '',
  cronExpression: '',
  misfirePolicy: '3',
  concurrent: '1',
  status: '0',
  remark: '',
})

const rules = {
  jobName: [{ required: true, message: '任务名称不能为空', trigger: 'blur' }],
  jobGroup: [{ required: true, message: '任务分组不能为空', trigger: 'blur' }],
  invokeTarget: [{ required: true, message: '调用目标字符串不能为空', trigger: 'blur' }],
  cronExpression: [{ required: true, message: 'cron执行表达式不能为空', trigger: 'blur' }],
}

const queryFormRef = ref()
const formRef = ref()

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getList() {
  loading.value = true
  listJob(queryParams)
    .then((res: any) => {
      jobList.value = res.rows
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
    jobName: undefined,
    jobGroup: undefined,
    status: undefined,
  })
  handleQuery()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.jobId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  resetForm(form, {
    jobId: undefined,
    jobName: '',
    jobGroup: 'DEFAULT',
    invokeTarget: '',
    cronExpression: '',
    misfirePolicy: '3',
    concurrent: '1',
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
  formDialog.title = '添加任务'
}

function handleUpdate(row?: any) {
  reset()
  const jobId = row?.jobId || ids.value[0]
  getJob(jobId).then((res: any) => {
    Object.assign(form, res.data)
    formDialog.visible = true
    formDialog.title = '修改任务'
  })
}

function submitForm() {
  if (!formRef.value) return
  ;(formRef.value as any).validate((valid: boolean) => {
    if (valid) {
      if (form.jobId !== undefined) {
        updateJob(form).then(() => {
          ElMessage.success('修改成功')
          formDialog.visible = false
          getList()
        })
      } else {
        addJob(form).then(() => {
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
  const jobIds = row?.jobId || ids.value
  const jobNames = row?.jobName || '选中数据'
  ElMessageBox.confirm('是否确认删除任务名称为"' + jobNames + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delJob(jobIds)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleStatusChange(row: any) {
  row.isLoading = true
  changeJobStatus(row.jobId, row.status)
    .then(() => {
      ElMessage.success('状态修改成功')
      row.isLoading = false
    })
    .catch(() => {
      row.isLoading = false
      row.status = row.status === '0' ? '1' : '0'
    })
}

function handleRun(row: any) {
  ElMessageBox.confirm('确认立即执行"' + row.jobName + '"任务？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await runJob(row.jobId, row.jobGroup)
      ElMessage.success('执行成功')
    })
    .catch(() => {})
}

function handleJobLog(row: any) {
  listJobLog({ jobId: row.jobId, pageNum: 1, pageSize: 10 }).then((res: any) => {
    jobLogList.value = res.rows || []
    jobLogDialog.visible = true
  })
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
