<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="字典名称">
        <el-input v-model="typeInfo.dictName" disabled style="width: 220px" />
      </el-form-item>
      <el-form-item label="字典类型">
        <el-input v-model="queryParams.dictType" disabled style="width: 220px" />
      </el-form-item>
      <el-form-item label="数据标签">
        <el-input v-model="queryParams.dictLabel" placeholder="请输入数据标签" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="数据状态" clearable style="width: 160px">
          <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:dict:add']">新增</el-button>
      <el-button type="success" :icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:dict:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:dict:remove']">删除</el-button>
      <el-button type="warning" :icon="Download" @click="handleExport" v-hasPermi="['system:dict:export']">导出</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="字典编码" prop="dictCode" align="center" width="100" />
      <el-table-column label="数据标签" prop="dictLabel" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="数据键值" prop="dictValue" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="样式属性" prop="cssClass" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="回显样式" prop="listClass" align="center" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.listClass" :type="scope.row.listClass === 'primary' ? '' : scope.row.listClass">{{ scope.row.listClass }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="默认值" prop="isDefault" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.isDefault === 'Y' ? 'success' : 'info'">{{ scope.row.isDefault === 'Y' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="dictSort" align="center" width="80" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <DictTag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" prop="createTime" align="center" width="160" />
      <el-table-column label="操作" align="center" width="150">
        <template #default="scope">
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['system:dict:edit']">修改</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['system:dict:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="formDialog.visible" :title="formDialog.title" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="数据标签" prop="dictLabel">
              <el-input v-model="form.dictLabel" placeholder="请输入数据标签" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据键值" prop="dictValue">
              <el-input v-model="form.dictValue" placeholder="请输入数据键值" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="dictSort">
              <el-input-number v-model="form.dictSort" :min="0" :max="999" controls-position="right" style="width: 100%" />
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
          <el-col :span="12">
            <el-form-item label="样式类型">
              <el-select v-model="form.listClass" placeholder="请选择样式类型" style="width: 100%">
                <el-option label="默认" value="default" />
                <el-option label="主要" value="primary" />
                <el-option label="成功" value="success" />
                <el-option label="信息" value="info" />
                <el-option label="警告" value="warning" />
                <el-option label="危险" value="danger" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="CSS Class">
              <el-input v-model="form.cssClass" placeholder="请输入CSS Class" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否默认">
              <el-radio-group v-model="form.isDefault">
                <el-radio value="Y">是</el-radio>
                <el-radio value="N">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
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
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Download } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import { useDict } from '@/composables/useDict'
import { useDownload } from '@/composables/useDownload'
import { useDictStore } from '@/stores/dict'
import { getType } from '@/api/system/dict/type'
import { listData, getData, addData, updateData, delData } from '@/api/system/dict/data'

const route = useRoute()
const dict = useDict('sys_normal_disable')
const { download } = useDownload()
const dictStore = useDictStore()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const dataList = ref<any[]>([])
const typeInfo = reactive<any>({})

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  dictType: '',
  dictLabel: undefined,
  status: undefined,
})

const formDialog = reactive({ visible: false, title: '' })
const form = reactive<any>({
  dictCode: undefined,
  dictSort: 0,
  dictLabel: '',
  dictValue: '',
  dictType: '',
  cssClass: '',
  listClass: 'default',
  isDefault: 'N',
  status: '0',
  remark: '',
})

const rules = {
  dictLabel: [{ required: true, message: '数据标签不能为空', trigger: 'blur' }],
  dictValue: [{ required: true, message: '数据键值不能为空', trigger: 'blur' }],
  dictSort: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
}

const formRef = ref()

function resetForm(target: any, defaults: any) {
  Object.assign(target, defaults)
}

function getTypeInfo() {
  const dictId = Number(route.params.dictId)
  getType(dictId).then((res: any) => {
    Object.assign(typeInfo, res.data || {})
    queryParams.dictType = typeInfo.dictType
    getList()
  })
}

function getList() {
  loading.value = true
  listData(queryParams)
    .then((res: any) => {
      dataList.value = res.rows || []
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
  queryParams.dictLabel = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.dictCode)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  resetForm(form, {
    dictCode: undefined,
    dictSort: 0,
    dictLabel: '',
    dictValue: '',
    dictType: queryParams.dictType,
    cssClass: '',
    listClass: 'default',
    isDefault: 'N',
    status: '0',
    remark: '',
  })
  nextTick(() => {
    ;(formRef.value as any)?.clearValidate?.()
  })
}

function handleAdd() {
  reset()
  formDialog.visible = true
  formDialog.title = '添加字典数据'
}

function handleUpdate(row?: any) {
  reset()
  const dictCode = row?.dictCode || ids.value[0]
  getData(dictCode).then((res: any) => {
    Object.assign(form, res.data || {})
    formDialog.visible = true
    formDialog.title = '修改字典数据'
  })
}

function submitForm() {
  if (!formRef.value) return
  ;(formRef.value as any).validate((valid: boolean) => {
    if (!valid) return
    const action = form.dictCode !== undefined ? updateData : addData
    action(form).then(() => {
      dictStore.removeDict(form.dictType)
      ElMessage.success(form.dictCode !== undefined ? '修改成功' : '新增成功')
      formDialog.visible = false
      getList()
    })
  })
}

function handleDelete(row?: any) {
  const dictCodes = row?.dictCode || ids.value
  ElMessageBox.confirm('是否确认删除字典编码为"' + dictCodes + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => delData(dictCodes as any))
    .then(() => {
      dictStore.removeDict(queryParams.dictType)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleExport() {
  download('/system/dict/data/export', queryParams, `dict_data_${new Date().getTime()}.xlsx`)
}

function cancel() {
  formDialog.visible = false
  reset()
}

onMounted(getTypeInfo)
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>
