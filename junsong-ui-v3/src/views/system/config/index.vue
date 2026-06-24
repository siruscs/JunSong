<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="参数名称" prop="configName">
        <el-input v-model="queryParams.configName" placeholder="请输入参数名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="参数键名" prop="configKey">
        <el-input v-model="queryParams.configKey" placeholder="请输入参数键名" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="参数键值" prop="configValue">
        <el-input v-model="queryParams.configValue" placeholder="请输入参数键值" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="参数状态" clearable style="width: 240px">
          <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
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
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:config:add']">新增</el-button>
      <el-button type="success" :icon="Edit" @click="handleUpdate" :disabled="single" v-hasPermi="['system:config:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" @click="handleDelete" :disabled="multiple" v-hasPermi="['system:config:remove']">删除</el-button>
      <el-button type="warning" :icon="Download" @click="handleExport" v-hasPermi="['system:config:export']">导出</el-button>
      <el-button type="warning" :icon="Refresh" @click="handleRefreshCache" v-hasPermi="['system:config:remove']">刷新缓存</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="configList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="参数编号" align="center" prop="configId" width="80" />
      <el-table-column label="参数名称" align="center" prop="configName" :show-overflow-tooltip="true" />
      <el-table-column label="参数键名" align="center" prop="configKey" :show-overflow-tooltip="true" />
      <el-table-column label="参数键值" align="center" prop="configValue" :show-overflow-tooltip="true" />
      <el-table-column label="系统内置" align="center" width="100">
        <template #default="scope">
          <DictTag :options="dict.type.sys_yes_no" :value="scope.row.configType" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <DictTag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['system:config:edit']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['system:config:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="参数名称" prop="configName">
              <el-input v-model="form.configName" placeholder="请输入参数名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参数键名" prop="configKey">
              <el-input v-model="form.configKey" placeholder="请输入参数键名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="参数键值" prop="configValue">
              <el-input v-model="form.configValue" type="textarea" :rows="3" placeholder="请输入参数键值" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="系统内置">
              <el-radio-group v-model="form.configType">
                <el-radio v-for="d in dict.type.sys_yes_no" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
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
import { Plus, Edit, Delete, Download, Refresh } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { useAuth } from '@/composables/useAuth'
import { useDownload } from '@/composables/useDownload'
import { listConfig, getConfig, addConfig, updateConfig, delConfig, refreshConfigCache } from '@/api/system/config'

const { hasPermi } = useAuth()
const { download } = useDownload()
const dict = useDict('sys_normal_disable', 'sys_yes_no')

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const configList = ref<any[]>([])
const dateRange = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  configName: undefined as string | undefined,
  configKey: undefined as string | undefined,
  configValue: undefined as string | undefined,
  status: undefined as string | undefined,
  params: {
    beginTime: undefined as string | undefined,
    endTime: undefined as string | undefined,
  },
})

const formDialog = reactive({ visible: false, title: '' })
const form = reactive<any>({
  configId: undefined,
  configName: '',
  configKey: '',
  configValue: '',
  configType: 'N',
  status: '0',
  remark: '',
})

const rules = {
  configName: [{ required: true, message: '参数名称不能为空', trigger: 'blur' }],
  configKey: [{ required: true, message: '参数键名不能为空', trigger: 'blur' }],
  configValue: [{ required: true, message: '参数键值不能为空', trigger: 'blur' }],
}

const queryFormRef = ref()
const formRef = ref()

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getList() {
  loading.value = true
  if (dateRange.value && dateRange.value.length > 0) {
    queryParams.params.beginTime = dateRange.value[0]
    queryParams.params.endTime = dateRange.value[1]
  }
  listConfig(queryParams)
    .then((res: any) => {
      configList.value = res.rows
      total.value = res.total
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
    configName: undefined,
    configKey: undefined,
    configValue: undefined,
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
  ids.value = selection.map((item) => item.configId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  resetForm(form, {
    configId: undefined,
    configName: '',
    configKey: '',
    configValue: '',
    configType: 'N',
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
  formDialog.title = '添加参数'
}

function handleUpdate(row?: any) {
  reset()
  const configId = row?.configId || ids.value[0]
  getConfig(configId).then((res: any) => {
    Object.assign(form, res.data)
    formDialog.visible = true
    formDialog.title = '修改参数'
  })
}

function submitForm() {
  if (!formRef.value) return
  ;(formRef.value as any).validate((valid: boolean) => {
    if (valid) {
      if (form.configId !== undefined) {
        updateConfig(form).then(() => {
          ElMessage.success('修改成功')
          formDialog.visible = false
          getList()
        })
      } else {
        addConfig(form).then(() => {
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
  const configIds = row?.configId || ids.value
  const configNames = row?.configName || '选中数据'
  ElMessageBox.confirm('是否确认删除参数名称为"' + configNames + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delConfig(configIds)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleRefreshCache() {
  ElMessageBox.confirm('是否确认刷新缓存数据？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await refreshConfigCache()
      ElMessage.success('刷新成功')
    })
    .catch(() => {})
}

function handleExport() {
  download('/system/config/export', queryParams, `config_${new Date().getTime()}.xlsx`)
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
