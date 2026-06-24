<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="24" :xs="24">
        <div class="dict-type-panel">
          <el-form :model="typeQueryParams" ref="typeQueryFormRef" :inline="true" v-show="typeShowSearch" label-width="80px">
            <el-form-item label="字典名称" prop="dictName">
              <el-input v-model="typeQueryParams.dictName" placeholder="请输入字典名称" clearable @keyup.enter="getTypeList" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleTypeQuery">搜索</el-button>
              <el-button @click="resetTypeQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <RightToolbar v-model:showSearch="typeShowSearch" @query="getTypeList">
            <el-button type="primary" :icon="Plus" @click="handleTypeAdd" v-hasPermi="['system:dict:add']">新增</el-button>
            <el-button type="success" :icon="Edit" @click="handleTypeUpdate" :disabled="singleType" v-hasPermi="['system:dict:edit']">修改</el-button>
            <el-button type="danger" :icon="Delete" @click="handleTypeDelete" :disabled="multipleType" v-hasPermi="['system:dict:remove']">删除</el-button>
            <el-button type="warning" :icon="Download" @click="handleTypeExport" v-hasPermi="['system:dict:export']">导出</el-button>
            <el-button type="warning" :icon="Refresh" @click="handleRefreshCache" v-hasPermi="['system:dict:remove']">刷新缓存</el-button>
          </RightToolbar>

          <el-table v-loading="typeLoading" :data="typeList" @selection-change="handleTypeSelectionChange" @row-click="handleTypeRowClick" highlight-current-row>
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="字典编号" align="center" prop="dictId" width="80" />
            <el-table-column label="字典名称" align="center" prop="dictName" :show-overflow-tooltip="true" />
            <el-table-column label="字典类型" align="center" prop="dictType" :show-overflow-tooltip="true" />
            <el-table-column label="状态" align="center" width="80">
              <template #default="scope">
                <DictTag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
            <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-button link type="primary" @click.stop="handleTypeUpdate(scope.row)" v-hasPermi="['system:dict:edit']">修改</el-button>
                <el-button link type="primary" @click.stop="handleDataSelect(scope.row)">字典数据</el-button>
                <el-button link type="danger" @click.stop="handleTypeDelete(scope.row)" v-hasPermi="['system:dict:remove']">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <Pagination v-show="typeTotal > 0" :total="typeTotal" v-model:page="typeQueryParams.pageNum" v-model:limit="typeQueryParams.pageSize" @pagination="getTypeList" />
        </div>
      </el-col>

      <el-dialog v-model="dataPanelDialog.visible" :title="dataPanelDialog.title" width="1100px" append-to-body>
        <div class="dict-data-panel">
          <el-form :model="dataQueryParams" ref="dataQueryFormRef" :inline="true" v-show="dataShowSearch" label-width="80px">
            <el-form-item label="数据标签" prop="dictLabel">
              <el-input v-model="dataQueryParams.dictLabel" placeholder="请输入数据标签" clearable @keyup.enter="getDataList" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="dataQueryParams.status" placeholder="数据状态" clearable style="width: 160px">
                <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleDataQuery">搜索</el-button>
              <el-button @click="resetDataQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <RightToolbar v-model:showSearch="dataShowSearch" @query="getDataList">
            <el-button type="primary" :icon="Plus" :disabled="!currentType" @click="handleDataAdd" v-hasPermi="['system:dict:add']">新增</el-button>
            <el-button type="success" :icon="Edit" :disabled="singleData" @click="handleDataUpdate" v-hasPermi="['system:dict:edit']">修改</el-button>
            <el-button type="danger" :icon="Delete" :disabled="multipleData" @click="handleDataDelete" v-hasPermi="['system:dict:remove']">删除</el-button>
          </RightToolbar>

          <el-table v-loading="dataLoading" :data="dataList" @selection-change="handleDataSelectionChange">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="数据编号" align="center" prop="dictCode" width="80" />
            <el-table-column label="数据标签" align="center" prop="dictLabel" :show-overflow-tooltip="true" />
            <el-table-column label="数据键值" align="center" prop="dictValue" :show-overflow-tooltip="true" />
            <el-table-column label="回显样式" align="center" prop="listClass" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.listClass" :type="scope.row.listClass === 'primary' ? '' : scope.row.listClass">{{ scope.row.listClass }}</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="表格字典样式" align="center" prop="cssClass" width="120" />
            <el-table-column label="默认值" prop="isDefault" align="center" width="90">
              <template #default="scope">
                <el-tag :type="scope.row.isDefault === 'Y' ? 'success' : 'info'">{{ scope.row.isDefault === 'Y' ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" align="center" width="80">
              <template #default="scope">
                <DictTag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
            <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
            <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-button link type="primary" @click="handleDataUpdate(scope.row)" v-hasPermi="['system:dict:edit']">修改</el-button>
                <el-button link type="danger" @click="handleDataDelete(scope.row)" v-hasPermi="['system:dict:remove']">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <Pagination v-show="dataTotal > 0" :total="dataTotal" v-model:page="dataQueryParams.pageNum" v-model:limit="dataQueryParams.pageSize" @pagination="getDataList" />
        </div>
      </el-dialog>
    </el-row>

    <el-dialog v-model="typeDialog.visible" :title="typeDialog.title" width="600px" append-to-body>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="字典名称" prop="dictName">
              <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典类型" prop="dictType">
              <el-input v-model="typeForm.dictType" placeholder="请输入字典类型" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="typeForm.status">
                <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="typeForm.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitTypeForm">确 定</el-button>
          <el-button @click="cancelType">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="dataDialog.visible" :title="dataDialog.title" width="600px" append-to-body>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="数据标签" prop="dictLabel">
              <el-input v-model="dataForm.dictLabel" placeholder="请输入数据标签" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据键值" prop="dictValue">
              <el-input v-model="dataForm.dictValue" placeholder="请输入数据键值" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="dictSort">
              <el-input-number v-model="dataForm.dictSort" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="dataForm.status">
                <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="样式类型" prop="listClass">
              <el-select v-model="dataForm.listClass" placeholder="请选择样式类型" style="width: 100%">
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
            <el-form-item label="CSS Class" prop="cssClass">
              <el-input v-model="dataForm.cssClass" placeholder="请输入CSS Class" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否默认">
              <el-radio-group v-model="dataForm.isDefault">
                <el-radio value="Y">是</el-radio>
                <el-radio value="N">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="dataForm.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitDataForm">确 定</el-button>
          <el-button @click="cancelData">取 消</el-button>
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
import { useDictStore } from '@/stores/dict'
import { listType, getType, addType, updateType, delType, refreshCache } from '@/api/system/dict/type'
import { listData, getData, addData, updateData, delData } from '@/api/system/dict/data'

const { hasPermi } = useAuth()
const { download } = useDownload()
const dict = useDict('sys_normal_disable')
const dictStore = useDictStore()

const typeLoading = ref(true)
const typeShowSearch = ref(true)
const typeIds = ref<number[]>([])
const singleType = ref(true)
const multipleType = ref(true)
const typeTotal = ref(0)
const typeList = ref<any[]>([])
const currentType = ref<any>(null)

const typeQueryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  dictName: undefined as string | undefined,
  dictType: undefined as string | undefined,
  status: undefined as string | undefined,
})

const typeDialog = reactive({ visible: false, title: '' })
const typeForm = reactive<any>({
  dictId: undefined,
  dictName: '',
  dictType: '',
  status: '0',
  remark: '',
})

const typeRules = {
  dictName: [{ required: true, message: '字典名称不能为空', trigger: 'blur' }],
  dictType: [{ required: true, message: '字典类型不能为空', trigger: 'blur' }],
}

const typeQueryFormRef = ref()
const typeFormRef = ref()

const dataLoading = ref(true)
const dataShowSearch = ref(true)
const dataIds = ref<number[]>([])
const singleData = ref(true)
const multipleData = ref(true)
const dataTotal = ref(0)
const dataList = ref<any[]>([])
const dataPanelDialog = reactive({ visible: false, title: '字典数据' })

const dataQueryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  dictType: '' as string,
  dictLabel: undefined as string | undefined,
  status: undefined as string | undefined,
})

const dataDialog = reactive({ visible: false, title: '' })
const dataForm = reactive<any>({
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

const dataRules = {
  dictLabel: [{ required: true, message: '数据标签不能为空', trigger: 'blur' }],
  dictValue: [{ required: true, message: '数据键值不能为空', trigger: 'blur' }],
  dictSort: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
}

const dataQueryFormRef = ref()
const dataFormRef = ref()

function normalizeListResponse(res: any) {
  const rows = Array.isArray(res?.rows)
    ? res.rows
    : Array.isArray(res?.data)
      ? res.data
      : []
  const total = Number(res?.total ?? rows.length)

  return { rows, total }
}

function resetForm(targetForm: any, defaultData: any) {
  Object.assign(targetForm, defaultData)
}

function getTypeList() {
  typeLoading.value = true
  listType(typeQueryParams)
    .then((res: any) => {
      const { rows, total } = normalizeListResponse(res)
      typeList.value = rows
      typeTotal.value = total
      typeLoading.value = false
    })
    .catch(() => {
      typeLoading.value = false
    })
}

function handleTypeQuery() {
  typeQueryParams.pageNum = 1
  getTypeList()
}

function resetTypeQuery() {
  resetForm(typeQueryParams, {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined,
  })
  handleTypeQuery()
}

function handleTypeSelectionChange(selection: any[]) {
  typeIds.value = selection.map((item) => item.dictId)
  singleType.value = selection.length !== 1
  multipleType.value = !selection.length
}

function handleTypeRowClick(row: any) {
  currentType.value = row
  dataQueryParams.dictType = row.dictType
  dataQueryParams.pageNum = 1
  dataPanelDialog.title = `字典数据 - ${row.dictName || row.dictType}`
  dataPanelDialog.visible = true
  getDataList()
}

function handleDataSelect(row: any) {
  currentType.value = row
  dataQueryParams.dictType = row.dictType
  dataQueryParams.pageNum = 1
  dataPanelDialog.title = `字典数据 - ${row.dictName || row.dictType}`
  dataPanelDialog.visible = true
  getDataList()
}

function resetType() {
  resetForm(typeForm, {
    dictId: undefined,
    dictName: '',
    dictType: '',
    status: '0',
    remark: '',
  })
  nextTick(() => {
    if (typeFormRef.value) {
      ;(typeFormRef.value as any).clearValidate?.()
    }
  })
}

function handleTypeAdd() {
  resetType()
  typeDialog.visible = true
  typeDialog.title = '添加字典类型'
}

function handleTypeUpdate(row?: any) {
  resetType()
  const dictId = row?.dictId || typeIds.value[0]
  getType(dictId).then((res: any) => {
    Object.assign(typeForm, res.data)
    typeDialog.visible = true
    typeDialog.title = '修改字典类型'
  })
}

function submitTypeForm() {
  if (!typeFormRef.value) return
  ;(typeFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      if (typeForm.dictId !== undefined) {
        updateType(typeForm).then(() => {
          ElMessage.success('修改成功')
          typeDialog.visible = false
          getTypeList()
        })
      } else {
        addType(typeForm).then(() => {
          ElMessage.success('新增成功')
          typeDialog.visible = false
          getTypeList()
        })
      }
    }
  })
}

function cancelType() {
  typeDialog.visible = false
  resetType()
}

function handleTypeDelete(row?: any) {
  const dictIds = row?.dictId || typeIds.value
  const dictNames = row?.dictName || '选中数据'
  ElMessageBox.confirm('是否确认删除字典类型为"' + dictNames + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delType(dictIds)
      ElMessage.success('删除成功')
      getTypeList()
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
      await refreshCache()
      dictStore.cleanDict()
      ElMessage.success('刷新成功')
    })
    .catch(() => {})
}

function handleTypeExport() {
  download('/system/dict/type/export', typeQueryParams, `dict_${new Date().getTime()}.xlsx`)
}

function getDataList() {
  if (!dataQueryParams.dictType) {
    dataList.value = []
    dataTotal.value = 0
    dataLoading.value = false
    return
  }
  dataLoading.value = true
  listData(dataQueryParams)
    .then((res: any) => {
      const { rows, total } = normalizeListResponse(res)
      dataList.value = rows
      dataTotal.value = total
      dataLoading.value = false
    })
    .catch(() => {
      dataLoading.value = false
    })
}

function handleDataQuery() {
  dataQueryParams.pageNum = 1
  getDataList()
}

function resetDataQuery() {
  resetForm(dataQueryParams, {
    pageNum: 1,
    pageSize: 10,
    dictType: currentType.value?.dictType || '',
    dictLabel: undefined,
    status: undefined,
  })
  handleDataQuery()
}

function handleDataSelectionChange(selection: any[]) {
  dataIds.value = selection.map((item) => item.dictCode)
  singleData.value = selection.length !== 1
  multipleData.value = !selection.length
}

function resetData() {
  resetForm(dataForm, {
    dictCode: undefined,
    dictSort: 0,
    dictLabel: '',
    dictValue: '',
    dictType: currentType.value?.dictType || '',
    cssClass: '',
    listClass: 'default',
    isDefault: 'N',
    status: '0',
    remark: '',
  })
  nextTick(() => {
    if (dataFormRef.value) {
      ;(dataFormRef.value as any).clearValidate?.()
    }
  })
}

function handleDataAdd() {
  resetData()
  dataDialog.visible = true
  dataDialog.title = '添加字典数据'
}

function handleDataUpdate(row?: any) {
  resetData()
  const dictCode = row?.dictCode || dataIds.value[0]
  getData(dictCode).then((res: any) => {
    Object.assign(dataForm, res.data)
    dataDialog.visible = true
    dataDialog.title = '修改字典数据'
  })
}

function submitDataForm() {
  if (!dataFormRef.value) return
  ;(dataFormRef.value as any).validate((valid: boolean) => {
    if (valid) {
      if (dataForm.dictCode !== undefined) {
        updateData(dataForm).then(() => {
          dictStore.removeDict(dataForm.dictType)
          ElMessage.success('修改成功')
          dataDialog.visible = false
          getDataList()
        })
      } else {
        addData(dataForm).then(() => {
          dictStore.removeDict(dataForm.dictType)
          ElMessage.success('新增成功')
          dataDialog.visible = false
          getDataList()
        })
      }
    }
  })
}

function cancelData() {
  dataDialog.visible = false
  resetData()
}

function handleDataDelete(row?: any) {
  const dictCodes = row?.dictCode || dataIds.value
  const dictLabels = row?.dictLabel || '选中数据'
  ElMessageBox.confirm('是否确认删除字典数据为"' + dictLabels + '"的数据项？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await delData(dictCodes)
      dictStore.removeDict(dataQueryParams.dictType)
      ElMessage.success('删除成功')
      getDataList()
    })
    .catch(() => {})
}

onMounted(() => {
  getTypeList()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.dict-type-panel,
.dict-data-panel {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}
</style>
