<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="表名称" prop="tableName">
        <el-input v-model="queryParams.tableName" placeholder="请输入表名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="表描述" prop="tableComment">
        <el-input v-model="queryParams.tableComment" placeholder="请输入表描述" clearable @keyup.enter="handleQuery" />
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
      <el-button type="primary" plain :icon="Download" @click="handleImport" v-hasPermi="['tool:gen:import']">导入</el-button>
      <el-button type="primary" :disabled="multiple" @click="handleBatchGen" v-hasPermi="['tool:gen:code']">批量生成</el-button>
      <el-button type="danger" :disabled="multiple" @click="handleDelete()" v-hasPermi="['tool:gen:remove']">删除</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" type="index" width="50" align="center" />
      <el-table-column label="表名称" align="left" prop="tableName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="表描述" align="left" prop="tableComment" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="实体" align="left" prop="className" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="模板类型" align="center" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.tplCategory === 'crud'" type="success">单表</el-tag>
          <el-tag v-else-if="row.tplCategory === 'tree'" type="warning">树表</el-tag>
          <el-tag v-else-if="row.tplCategory === 'sub'" type="primary">主子表</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="前端类型" align="center" width="120">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.tplWebType || 'element-plus-typescript' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button link type="primary" @click="handlePreview(row)" v-hasPermi="['tool:gen:preview']">预览</el-button>
          <el-button link type="primary" @click="handleEditTable(row)" v-hasPermi="['tool:gen:edit']">编辑</el-button>
          <el-button link type="primary" @click="handleSynchDb(row)" v-hasPermi="['tool:gen:edit']">同步</el-button>
          <el-dropdown @command="(cmd) => handleCommand(cmd, row)">
            <el-button link type="primary">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="genCode" v-hasPermi="['tool:gen:code']">生成代码</el-dropdown-item>
                <el-dropdown-item command="download" v-hasPermi="['tool:gen:code']">下载代码</el-dropdown-item>
                <el-dropdown-item command="delete" v-hasPermi="['tool:gen:remove']">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <Pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 导入表结构对话框 -->
    <el-dialog v-model="importOpen" title="导入表结构" width="800px" top="5vh" append-to-body>
      <el-form :model="importQuery" ref="importFormRef" :inline="true">
        <el-form-item label="表名称" prop="tableName">
          <el-input v-model="importQuery.tableName" placeholder="请输入表名称" clearable @keyup.enter="getDbList" />
        </el-form-item>
        <el-form-item label="表描述" prop="tableComment">
          <el-input v-model="importQuery.tableComment" placeholder="请输入表描述" clearable @keyup.enter="getDbList" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getDbList">搜索</el-button>
          <el-button @click="resetImportQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-row>
        <el-alert title="选择前端模板类型（影响生成的 Vue 代码版本）" type="info" :closable="false" show-icon style="margin-bottom: 10px;" />
        <el-radio-group v-model="importTplWebType" style="margin-bottom: 10px;">
          <el-radio label="element-plus-typescript">Vue3 + Element Plus + TypeScript（推荐）</el-radio>
          <el-radio label="element-plus">Vue3 + Element Plus</el-radio>
          <el-radio label="element-ui">Vue2 + Element UI</el-radio>
        </el-radio-group>
      </el-row>
      <el-table v-loading="importLoading" :data="dbTableList" @selection-change="handleImportSelectionChange" max-height="400">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="表名称" prop="tableName" :show-overflow-tooltip="true" />
        <el-table-column label="表描述" prop="tableComment" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="更新时间" prop="updateTime" width="170" />
      </el-table>
      <Pagination v-show="importTotal > 0" :total="importTotal" v-model:page="importQuery.pageNum" v-model:limit="importQuery.pageSize" @pagination="getDbList" />
      <template #footer>
        <el-button @click="importOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitImport" :disabled="importSelection.length === 0">导 入</el-button>
      </template>
    </el-dialog>

    <!-- 预览代码对话框 -->
    <el-dialog v-model="preview.visible" title="代码预览" width="80%" top="5vh" append-to-body>
      <el-tabs v-model="preview.activeName">
        <el-tab-pane v-for="(value, key) in preview.data" :key="key" :label="getPreviewName(key)" :name="getPreviewName(key)">
          <el-button style="float: right; margin-bottom: 8px" @click="copyCode(value)">复制</el-button>
          <pre class="code-preview"><code>{{ value }}</code></pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 编辑配置对话框 -->
    <el-dialog v-model="editOpen" :title="editTitle" width="900px" top="3vh" append-to-body destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-tabs v-model="editActiveTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-row>
              <el-col :span="12">
                <el-form-item label="表名称" prop="tableName">
                  <el-input v-model="editForm.tableName" placeholder="请输入表名称" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="表描述" prop="tableComment">
                  <el-input v-model="editForm.tableComment" placeholder="请输入表描述" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="实体类名称" prop="className">
                  <el-input v-model="editForm.className" placeholder="请输入实体类名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="模板类型" prop="tplCategory">
                  <el-select v-model="editForm.tplCategory" placeholder="请选择模板类型" style="width: 100%;">
                    <el-option label="单表（CRUD）" value="crud" />
                    <el-option label="树表（Tree）" value="tree" />
                    <el-option label="主子表（Sub）" value="sub" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="前端类型" prop="tplWebType">
                  <el-select v-model="editForm.tplWebType" placeholder="请选择前端类型" style="width: 100%;">
                    <el-option label="Vue3 + Element Plus + TypeScript" value="element-plus-typescript" />
                    <el-option label="Vue3 + Element Plus" value="element-plus" />
                    <el-option label="Vue2 + Element UI" value="element-ui" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="表单布局" prop="formColNum">
                  <el-select v-model="editForm.formColNum" placeholder="请选择表单布局" style="width: 100%;">
                    <el-option label="单列" :value="1" />
                    <el-option label="双列" :value="2" />
                    <el-option label="三列" :value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生成作者" prop="functionAuthor">
                  <el-input v-model="editForm.functionAuthor" placeholder="请输入作者" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生成功能名" prop="functionName">
                  <el-input v-model="editForm.functionName" placeholder="如：用户管理" />
                </el-form-item>
              </el-col>
            </el-row>
            <!-- 树表配置 -->
            <template v-if="editForm.tplCategory === 'tree'">
              <el-divider content-position="left">树表配置</el-divider>
              <el-row>
                <el-col :span="8">
                  <el-form-item label="树编码字段" prop="treeCode">
                    <el-select v-model="editForm.treeCode" placeholder="请选择" clearable filterable style="width: 100%;">
                      <el-option v-for="col in editForm.columns" :key="col.columnId" :label="col.columnName + '（' + col.javaField + '）'" :value="col.columnName" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="树父编码字段" prop="treeParentCode">
                    <el-select v-model="editForm.treeParentCode" placeholder="请选择" clearable filterable style="width: 100%;">
                      <el-option v-for="col in editForm.columns" :key="col.columnId" :label="col.columnName + '（' + col.javaField + '）'" :value="col.columnName" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="树名称字段" prop="treeName">
                    <el-select v-model="editForm.treeName" placeholder="请选择" clearable filterable style="width: 100%;">
                      <el-option v-for="col in editForm.columns" :key="col.columnId" :label="col.columnName + '（' + col.javaField + '）'" :value="col.columnName" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
            <!-- 主子表配置 -->
            <template v-if="editForm.tplCategory === 'sub'">
              <el-divider content-position="left">主子表配置</el-divider>
              <el-row>
                <el-col :span="12">
                  <el-form-item label="关联子表" prop="subTableName">
                    <el-select v-model="editForm.subTableName" placeholder="请选择关联子表" clearable filterable style="width: 100%;">
                      <el-option v-for="t in subTableOptions" :key="t.tableId" :label="t.tableName + '（' + t.tableComment + '）'" :value="t.tableName" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="子表外键" prop="subTableFkName">
                    <el-input v-model="editForm.subTableFkName" placeholder="如：user_id" />
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
          </el-tab-pane>

          <!-- 字段信息 -->
          <el-tab-pane label="字段信息" name="field">
            <el-alert title="点击行左侧展开按钮可预览字段渲染效果" type="info" :closable="false" show-icon style="margin-bottom: 8px;" />
            <el-table :data="editForm.columns" border size="small" max-height="500" row-key="columnId" :expand-row-keys="expandedFieldKeys">
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="field-preview-panel">
                    <div class="field-preview-label">
                      <el-tag size="small" type="info">{{ row.htmlType }}</el-tag>
                      <span style="margin-left: 8px;">{{ row.columnComment || row.javaField }} — 渲染预览</span>
                      <el-tag v-if="row.isRequired === '1'" size="small" type="danger" style="margin-left: 8px;">必填</el-tag>
                    </div>
                    <div class="field-preview-control">
                      <FieldPreview :html-type="row.htmlType" :dict-type="row.dictType" :label="row.columnComment || row.javaField" />
                    </div>
                    <div class="field-preview-tip">
                      <span v-if="row.htmlType === 'input'">生成代码：el-input 文本输入框</span>
                      <span v-else-if="row.htmlType === 'textarea'">生成代码：el-input type=textarea 多行文本</span>
                      <span v-else-if="row.htmlType === 'select'">生成代码：el-select 下拉框{{ row.dictType ? '（字典：' + row.dictType + '）' : '（需配置字典类型）' }}</span>
                      <span v-else-if="row.htmlType === 'radio'">生成代码：el-radio-group 单选框{{ row.dictType ? '（字典：' + row.dictType + '）' : '（需配置字典类型）' }}</span>
                      <span v-else-if="row.htmlType === 'checkbox'">生成代码：el-checkbox-group 复选框{{ row.dictType ? '（字典：' + row.dictType + '）' : '（需配置字典类型）' }}</span>
                      <span v-else-if="row.htmlType === 'datetime'">生成代码：el-date-picker 日期时间选择器</span>
                      <span v-else-if="row.htmlType === 'imageUpload'">生成代码：ImageUpload 图片上传组件</span>
                      <span v-else-if="row.htmlType === 'upload'">生成代码：FileUpload 文件上传组件</span>
                      <span v-else-if="row.htmlType === 'editor'">生成代码：Editor 富文本编辑器</span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="序号" type="index" width="50" align="center" />
              <el-table-column label="字段列名" prop="columnName" width="140" :show-overflow-tooltip="true" />
              <el-table-column label="字段描述" prop="columnComment" width="120" :show-overflow-tooltip="true">
                <template #default="{ row }">
                  <el-input v-model="row.columnComment" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="物理类型" prop="columnType" width="100" :show-overflow-tooltip="true" />
              <el-table-column label="Java类型" prop="javaType" width="120">
                <template #default="{ row }">
                  <el-select v-model="row.javaType" size="small" style="width: 100%;">
                    <el-option label="String" value="String" />
                    <el-option label="Integer" value="Integer" />
                    <el-option label="Long" value="Long" />
                    <el-option label="Boolean" value="Boolean" />
                    <el-option label="Date" value="Date" />
                    <el-option label="BigDecimal" value="BigDecimal" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="Java属性" prop="javaField" width="120">
                <template #default="{ row }">
                  <el-input v-model="row.javaField" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="插入" width="50" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.isInsert" true-value="1" false-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="编辑" width="50" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.isEdit" true-value="1" false-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="列表" width="50" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.isList" true-value="1" false-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="查询" width="50" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.isQuery" true-value="1" false-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="必填" width="50" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.isRequired" true-value="1" false-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="查询方式" width="100">
                <template #default="{ row }">
                  <el-select v-model="row.queryType" size="small" style="width: 100%;" :disabled="row.isQuery !== '1'">
                    <el-option label="=" value="EQ" />
                    <el-option label="!=" value="NE" />
                    <el-option label=">" value="GT" />
                    <el-option label="<" value="LT" />
                    <el-option label="LIKE" value="LIKE" />
                    <el-option label="BETWEEN" value="BETWEEN" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="显示类型" width="120">
                <template #default="{ row }">
                  <el-select v-model="row.htmlType" size="small" style="width: 100%;">
                    <el-option label="文本框" value="input" />
                    <el-option label="文本域" value="textarea" />
                    <el-option label="下拉框" value="select" />
                    <el-option label="单选框" value="radio" />
                    <el-option label="复选框" value="checkbox" />
                    <el-option label="日期时间" value="datetime" />
                    <el-option label="图片上传" value="imageUpload" />
                    <el-option label="文件上传" value="upload" />
                    <el-option label="富文本" value="editor" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="字典类型" prop="dictType" width="120">
                <template #default="{ row }">
                  <el-input v-model="row.dictType" size="small" placeholder="sys_xxx" />
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 生成信息 -->
          <el-tab-pane label="生成信息" name="gen">
            <el-row>
              <el-col :span="12">
                <el-form-item label="生成包路径" prop="packageName">
                  <el-input v-model="editForm.packageName" placeholder="如：com.junsong.system" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生成模块名" prop="moduleName">
                  <el-input v-model="editForm.moduleName" placeholder="如：system" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生成业务名" prop="businessName">
                  <el-input v-model="editForm.businessName" placeholder="如：user" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="上级菜单" prop="parentMenuId">
                  <el-input-number v-model="editForm.parentMenuId" :min="0" controls-position="right" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生成方式" prop="genType">
                  <el-radio-group v-model="editForm.genType">
                    <el-radio label="0">zip 压缩包下载</el-radio>
                    <el-radio label="1">自定义路径</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12" v-if="editForm.genType === '1'">
                <el-form-item label="自定义路径" prop="genPath">
                  <el-input v-model="editForm.genPath" placeholder="如：/Users/junsong/projects/..." />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="生成详情页" prop="isView">
                  <el-switch v-model="editForm.isView" />
                  <span style="margin-left: 10px; color: #909399; font-size: 12px;">开启后列表将增加"详情"按钮与详情抽屉页</span>
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="editLoading">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, ArrowDown } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import FieldPreview from './FieldPreview.vue'
import {
  listTable,
  getGenTable,
  updateGenTable,
  delTable,
  previewTable,
  listDbTable,
  importTable,
  genCode,
  synchDb,
  downloadCode,
  batchGenCode,
} from '@/api/tool/gen'

const loading = ref(false)
const showSearch = ref(true)
const tableList = ref<any[]>([])
const total = ref(0)
const ids = ref<number[]>([])
const tableNames = ref<string[]>([])
const multiple = ref(true)
const queryFormRef = ref()
const dateRange = ref<string[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined as string | undefined,
  tableComment: undefined as string | undefined,
})

/* ==================== 列表 ==================== */
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
  dateRange.value = []
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

/* ==================== 导入表 ==================== */
const importOpen = ref(false)
const importLoading = ref(false)
const dbTableList = ref<any[]>([])
const importTotal = ref(0)
const importSelection = ref<any[]>([])
const importTplWebType = ref('element-plus-typescript')
const importFormRef = ref()
const importQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined as string | undefined,
  tableComment: undefined as string | undefined,
})

function handleImport() {
  importOpen.value = true
  importQuery.pageNum = 1
  importQuery.tableName = undefined
  importQuery.tableComment = undefined
  importSelection.value = []
  getDbList()
}

function getDbList() {
  importLoading.value = true
  listDbTable(importQuery).then((res: any) => {
    dbTableList.value = res.rows || []
    importTotal.value = res.total || 0
  }).finally(() => {
    importLoading.value = false
  })
}

function resetImportQuery() {
  importQuery.tableName = undefined
  importQuery.tableComment = undefined
  importFormRef.value?.resetFields?.()
  getDbList()
}

function handleImportSelectionChange(selection: any[]) {
  importSelection.value = selection
}

function submitImport() {
  const tables = importSelection.value.map((t) => t.tableName).join(',')
  if (!tables) {
    ElMessage.warning('请选择要导入的表')
    return
  }
  importTable(tables, importTplWebType.value).then(() => {
    ElMessage.success('导入成功')
    importOpen.value = false
    getList()
  })
}

/* ==================== 预览 ==================== */
const preview = reactive({
  visible: false,
  data: {} as Record<string, string>,
  activeName: '',
})

function handlePreview(row: any) {
  previewTable(row.tableId).then((res: any) => {
    preview.data = res.data || {}
    preview.activeName = Object.keys(preview.data).map(getPreviewName)[0] || ''
    preview.visible = true
  })
}

function getPreviewName(key: string) {
  const fileName = key.substring(key.lastIndexOf('/') + 1)
  return fileName.replace('.vm', '')
}

function copyCode(value: string) {
  navigator.clipboard?.writeText(value)
  ElMessage.success('代码已复制')
}

/* ==================== 编辑配置 ==================== */
const editOpen = ref(false)
const editTitle = ref('')
const editLoading = ref(false)
const editActiveTab = ref('basic')
const editFormRef = ref()
const subTableOptions = ref<any[]>([])
const expandedFieldKeys = ref<number[]>([])
const editForm = reactive<any>({
  tableId: undefined,
  tableName: '',
  tableComment: '',
  className: '',
  tplCategory: 'crud',
  tplWebType: 'element-plus-typescript',
  packageName: 'com.junsong',
  moduleName: '',
  businessName: '',
  functionName: '',
  functionAuthor: '',
  formColNum: 2,
  genType: '0',
  genPath: '',
  options: '',
  treeCode: '',
  treeParentCode: '',
  treeName: '',
  parentMenuId: 3,
  isView: false,
  subTableName: '',
  subTableFkName: '',
  columns: [] as any[],
})

const editRules = {
  tableComment: [{ required: true, message: '请输入表描述', trigger: 'blur' }],
  className: [{ required: true, message: '请输入实体类名称', trigger: 'blur' }],
  tplCategory: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  tplWebType: [{ required: true, message: '请选择前端类型', trigger: 'change' }],
  packageName: [{ required: true, message: '请输入生成包路径', trigger: 'blur' }],
  moduleName: [{ required: true, message: '请输入生成模块名', trigger: 'blur' }],
  businessName: [{ required: true, message: '请输入生成业务名', trigger: 'blur' }],
  functionName: [{ required: true, message: '请输入生成功能名', trigger: 'blur' }],
  functionAuthor: [{ required: true, message: '请输入作者', trigger: 'blur' }],
}

function handleEditTable(row: any) {
  getGenTable(row.tableId).then((res: any) => {
    const data = res.data || {}
    const info = data.info || {}
    subTableOptions.value = data.tables || []
    Object.keys(editForm).forEach((key) => {
      if (info[key] !== undefined) {
        editForm[key] = info[key]
      }
    })
    editForm.columns = data.rows || []
    editActiveTab.value = 'basic'
    expandedFieldKeys.value = []
    editTitle.value = '编辑「' + info.tableName + '」配置'
    editOpen.value = true
  })
}

function submitEdit() {
  editFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    // 树表校验
    if (editForm.tplCategory === 'tree') {
      if (!editForm.treeCode || !editForm.treeParentCode || !editForm.treeName) {
        ElMessage.error('树表必须配置树编码、树父编码、树名称字段')
        editActiveTab.value = 'basic'
        return
      }
    }
    // 主子表校验
    if (editForm.tplCategory === 'sub') {
      if (!editForm.subTableName || !editForm.subTableFkName) {
        ElMessage.error('主子表必须配置关联子表和子表外键')
        editActiveTab.value = 'basic'
        return
      }
    }
    editLoading.value = true
    const payload = { ...editForm }
    // 构建 options JSON
    payload.options = JSON.stringify({
      treeCode: editForm.treeCode,
      treeParentCode: editForm.treeParentCode,
      treeName: editForm.treeName,
      parentMenuId: editForm.parentMenuId,
      parentMenuName: '',
      genView: editForm.isView,
    })
    updateGenTable(payload).then(() => {
      ElMessage.success('保存成功')
      editOpen.value = false
      getList()
    }).finally(() => {
      editLoading.value = false
    })
  })
}

/* ==================== 同步 / 生成 / 下载 / 删除 ==================== */
function handleSynchDb(row: any) {
  ElMessageBox.confirm('确认要强制同步「' + row.tableName + '」的表结构吗？', '提示', { type: 'warning' })
    .then(() => synchDb(row.tableName))
    .then(() => {
      ElMessage.success('同步成功')
      getList()
    })
    .catch(() => {})
}

function handleCommand(cmd: string, row: any) {
  if (cmd === 'genCode') handleGenCode(row)
  else if (cmd === 'download') handleDownload(row)
  else if (cmd === 'delete') handleDelete(row)
}

function handleGenCode(row: any) {
  genCode(row.tableName).then(() => {
    ElMessage.success('代码已生成到自定义路径')
  })
}

function handleDownload(row: any) {
  ElMessage.info('正在打包下载，请稍候...')
  downloadCode(row.tableName).then((res: any) => {
    const blob = new Blob([res], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.tableName + '.zip'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  })
}

function handleBatchGen() {
  const names = tableNames.value
  if (!names.length) {
    ElMessage.warning('请选择要生成的表')
    return
  }
  ElMessage.info('正在打包下载，请稍候...')
  batchGenCode(names.join(',')).then((res: any) => {
    const blob = new Blob([res], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'junsong.zip'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
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
.field-preview-panel {
  padding: 12px 20px;
  background: #f9fafc;
  border-radius: 6px;
}
.field-preview-label {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 600;
  color: #303133;
}
.field-preview-control {
  max-width: 400px;
  margin-bottom: 10px;
}
.field-preview-tip {
  font-size: 12px;
  color: #909399;
}
</style>
