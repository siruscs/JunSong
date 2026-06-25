<template>
  <div class="app-container">
    <div class="edit-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="header-title">{{ isEdit ? '编辑业务配置' : '新建业务配置' }}</span>
        </template>
      </el-page-header>
      <div class="header-actions">
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        <el-button type="success" :loading="generating" @click="handleSaveAndGenerateMenu">保存并生成菜单</el-button>
        <el-button type="warning" :loading="publishing" @click="handlePublish" v-if="isEdit">发布版本</el-button>
        <el-button type="info" @click="loadHistory" v-if="isEdit">版本历史</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="edit-tabs">
      <!-- ===== 基本信息 Tab ===== -->
      <el-tab-pane label="基本信息" name="basic">
        <el-form ref="basicFormRef" :model="config.bizObject" :rules="basicRules" label-width="120px" style="max-width: 700px">
          <el-form-item label="业务编码" prop="bizCode">
            <el-input v-model="config.bizObject.bizCode" placeholder="如 leave_apply" :disabled="isEdit" />
          </el-form-item>
          <el-form-item label="业务名称" prop="bizName">
            <el-input v-model="config.bizObject.bizName" placeholder="如 演示申请单" />
          </el-form-item>
          <el-form-item label="存储模式" prop="storageMode">
            <el-select v-model="config.bizObject.storageMode" placeholder="请选择" style="width: 100%">
              <el-option label="通用表单(GENERIC)" value="GENERIC" />
              <el-option label="原生表(NATIVE)" value="NATIVE" />
            </el-select>
          </el-form-item>
          <el-form-item label="流程Key" prop="processKey">
            <el-input v-model="config.bizObject.processKey" placeholder="如 leave-apply" />
          </el-form-item>
          <el-form-item label="单据前缀" prop="orderNoPrefix">
            <el-input v-model="config.bizObject.orderNoPrefix" placeholder="如 DA" />
          </el-form-item>
          <el-form-item label="启用工作流" prop="workflowEnabled">
            <el-switch v-model="config.bizObject.workflowEnabled" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item label="启用履约" prop="fulfillmentEnabled">
            <el-switch v-model="config.bizObject.fulfillmentEnabled" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="config.bizObject.status">
              <el-radio value="0">正常</el-radio>
              <el-radio value="1">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- ===== 字段 Tab ===== -->
      <el-tab-pane :label="'字段（' + (config.fields?.length || 0) + '）'" name="fields">
        <div class="tab-toolbar">
          <el-button type="primary" :icon="Plus" @click="addField">新增字段</el-button>
          <el-button type="success" :disabled="!selectedFields.length" @click="batchSetRequired">批量必填</el-button>
          <el-button type="success" :disabled="!selectedFields.length" @click="batchSetList">批量列表</el-button>
          <el-button type="success" :disabled="!selectedFields.length" @click="batchSetQuery">批量查询</el-button>
          <el-button type="danger" :disabled="!selectedFields.length" @click="batchDeleteFields">批量删除</el-button>
        </div>
        <el-table ref="fieldTableRef" :data="config.fields" border size="small" row-key="fieldKey" @selection-change="handleFieldSelectionChange">
          <el-table-column type="selection" width="40" align="center" />
          <el-table-column width="40" align="center">
            <template #default>
              <el-icon class="drag-handle" style="cursor: move; color: #909399"><Sort /></el-icon>
            </template>
          </el-table-column>
          <el-table-column label="字段Key" prop="fieldKey" width="140">
            <template #default="{ row }">
              <el-input v-model="row.fieldKey" size="small" placeholder="如 applicant" />
            </template>
          </el-table-column>
          <el-table-column label="字段名称" prop="fieldLabel" width="140">
            <template #default="{ row }">
              <el-input v-model="row.fieldLabel" size="small" placeholder="如 申请人" />
            </template>
          </el-table-column>
          <el-table-column label="字段类型" prop="fieldType" width="140">
            <template #default="{ row }">
              <el-select v-model="row.fieldType" size="small" placeholder="选择类型">
                <el-option v-for="t in fieldTypes" :key="t" :label="t" :value="t" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必填" prop="required" width="70" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.required" true-value="1" false-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="查询" prop="isQuery" width="60" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.isQuery" true-value="1" false-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="列表" prop="isList" width="60" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.isList" true-value="1" false-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="详情" prop="isDetail" width="60" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.isDetail" true-value="1" false-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="流程变量" prop="isProcessVar" width="90" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.isProcessVar" true-value="1" false-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="字典类型" prop="dictType" width="120">
            <template #default="{ row }">
              <el-input v-model="row.dictType" size="small" placeholder="如 sys_user_sex" />
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="orderNum" width="80" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.orderNum" :min="0" size="small" controls-position="right" style="width: 70px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row, $index }">
              <el-button link type="primary" @click="copyField(row)">复制</el-button>
              <el-button link type="danger" @click="removeField($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ===== 处理人 Tab ===== -->
      <el-tab-pane :label="'处理人（' + (config.nodeAssignees?.length || 0) + '）'" name="assignees">
        <div class="tab-toolbar">
          <el-button type="primary" :icon="Plus" @click="addAssignee">新增处理人</el-button>
        </div>
        <el-table :data="config.nodeAssignees" border size="small">
          <el-table-column label="节点Key" prop="taskKey" width="160">
            <template #default="{ row }">
              <el-input v-model="row.taskKey" size="small" placeholder="如 approve" />
            </template>
          </el-table-column>
          <el-table-column label="处理人类型" prop="assigneeType" width="180">
            <template #default="{ row }">
              <el-select v-model="row.assigneeType" size="small" placeholder="选择类型">
                <el-option label="固定用户(FIXED_USER)" value="FIXED_USER" />
                <el-option label="固定角色(FIXED_ROLE)" value="FIXED_ROLE" />
                <el-option label="部门负责人(DEPT_LEADER)" value="DEPT_LEADER" />
                <el-option label="发起人负责人(INITIATOR_LEADER)" value="INITIATOR_LEADER" />
                <el-option label="发起人(INITIATOR)" value="INITIATOR" />
                <el-option label="表单用户字段(FORM_FIELD_USER)" value="FORM_FIELD_USER" />
                <el-option label="表单部门负责人(FORM_FIELD_DEPT_LEADER)" value="FORM_FIELD_DEPT_LEADER" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="处理人值" prop="assigneeValue" min-width="200">
            <template #default="{ row }">
              <el-input v-model="row.assigneeValue" size="small" placeholder="用户ID/角色Key/字段Key" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeAssignee($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ===== 分支规则 Tab ===== -->
      <el-tab-pane :label="'分支规则（' + (config.branchRules?.length || 0) + '）'" name="branches">
        <div class="tab-toolbar">
          <el-button type="primary" :icon="Plus" @click="addBranch">新增分支规则</el-button>
        </div>
        <el-table :data="config.branchRules" border size="small">
          <el-table-column label="源节点" prop="sourceKey" width="140">
            <template #default="{ row }">
              <el-input v-model="row.sourceKey" size="small" placeholder="如 submit" />
            </template>
          </el-table-column>
          <el-table-column label="字段Key" prop="fieldKey" width="140">
            <template #default="{ row }">
              <el-input v-model="row.fieldKey" size="small" placeholder="如 amount" />
            </template>
          </el-table-column>
          <el-table-column label="操作符" prop="operator" width="100">
            <template #default="{ row }">
              <el-select v-model="row.operator" size="small" placeholder="选择">
                <el-option label=">=" value=">=" />
                <el-option label=">" value=">" />
                <el-option label="=" value="=" />
                <el-option label="<" value="<" />
                <el-option label="<=" value="<=" />
                <el-option label="in" value="in" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="比较值" prop="compareValue" width="140">
            <template #default="{ row }">
              <el-input v-model="row.compareValue" size="small" placeholder="如 1000" />
            </template>
          </el-table-column>
          <el-table-column label="目标变量" prop="targetVar" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.targetVar" size="small" placeholder="如 flow_goto_large" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeBranch($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ===== 动作 Tab ===== -->
      <el-tab-pane :label="'动作（' + (config.actions?.length || 0) + '）'" name="actions">
        <div class="tab-toolbar">
          <el-button type="primary" :icon="Plus" @click="addAction">新增动作</el-button>
        </div>
        <el-table :data="config.actions" border size="small">
          <el-table-column label="动作编码" prop="actionCode" width="140">
            <template #default="{ row }">
              <el-input v-model="row.actionCode" size="small" placeholder="如 SUBMIT" />
            </template>
          </el-table-column>
          <el-table-column label="动作名称" prop="actionName" width="120">
            <template #default="{ row }">
              <el-input v-model="row.actionName" size="small" placeholder="如 提交审批" />
            </template>
          </el-table-column>
          <el-table-column label="类型" prop="actionType" width="120">
            <template #default="{ row }">
              <el-select v-model="row.actionType" size="small">
                <el-option label="内置(BUILTIN)" value="BUILTIN" />
                <el-option label="自定义(API)" value="API" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="触发状态" prop="triggerStatus" min-width="200">
            <template #default="{ row }">
              <el-input v-model="row.triggerStatus" size="small" placeholder="如 DRAFT,REJECTED" />
            </template>
          </el-table-column>
          <el-table-column label="按钮样式" prop="buttonStyle" width="120">
            <template #default="{ row }">
              <el-select v-model="row.buttonStyle" size="small">
                <el-option label="primary" value="primary" />
                <el-option label="success" value="success" />
                <el-option label="warning" value="warning" />
                <el-option label="danger" value="danger" />
                <el-option label="info" value="info" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="API端点" prop="apiEndpoint" min-width="200">
            <template #default="{ row }">
              <el-input v-model="row.apiEndpoint" size="small" placeholder="/lowcode/biz/{bizCode}/{id}/submit" />
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="sortOrder" width="80" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" :min="0" size="small" controls-position="right" style="width: 70px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeAction($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ===== 后置动作 Tab ===== -->
      <el-tab-pane :label="'后置动作（' + (config.postActions?.length || 0) + '）'" name="postActions">
        <div class="tab-toolbar">
          <el-button type="primary" :icon="Plus" @click="addPostAction">新增后置动作</el-button>
        </div>
        <el-table :data="config.postActions" border size="small">
          <el-table-column label="触发事件" prop="triggerEvent" width="180">
            <template #default="{ row }">
              <el-select v-model="row.triggerEvent" size="small">
                <el-option label="提交后(AFTER_SUBMIT)" value="AFTER_SUBMIT" />
                <el-option label="审批后(AFTER_APPROVE)" value="AFTER_APPROVE" />
                <el-option label="驳回后(AFTER_REJECT)" value="AFTER_REJECT" />
                <el-option label="撤回后(AFTER_WITHDRAW)" value="AFTER_WITHDRAW" />
                <el-option label="履约后(AFTER_FULFILL)" value="AFTER_FULFILL" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="动作类型" prop="actionType" width="180">
            <template #default="{ row }">
              <el-select v-model="row.actionType" size="small">
                <el-option label="更新状态(UPDATE_STATUS)" value="UPDATE_STATUS" />
                <el-option label="更新字段(UPDATE_FIELD)" value="UPDATE_FIELD" />
                <el-option label="发消息(SEND_MESSAGE)" value="SEND_MESSAGE" />
                <el-option label="调服务(CALL_SERVICE)" value="CALL_SERVICE" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="目标字段" prop="targetField" width="120">
            <template #default="{ row }">
              <el-input v-model="row.targetField" size="small" placeholder="UPDATE_FIELD用" />
            </template>
          </el-table-column>
          <el-table-column label="目标值" prop="targetValue" width="140">
            <template #default="{ row }">
              <el-input v-model="row.targetValue" size="small" placeholder="如 APPROVED" />
            </template>
          </el-table-column>
          <el-table-column label="条件表达式" prop="conditionExpr" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.conditionExpr" size="small" placeholder="如 processEnded==true" />
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="sortOrder" width="80" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" :min="0" size="small" controls-position="right" style="width: 70px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removePostAction($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 版本历史弹窗 -->
    <el-dialog v-model="showHistory" title="版本历史" width="700px">
      <el-table :data="historyList" border>
        <el-table-column label="版本号" prop="versionNo" width="80" align="center" />
        <el-table-column label="发布说明" prop="publishRemark" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column label="发布人" prop="createBy" width="120" />
        <el-table-column label="发布时间" prop="createTime" width="180" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="warning" @click="handleRollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Sort } from '@element-plus/icons-vue'
import Sortable from 'sortablejs'
import {
  getBizConfig, saveBizConfig, generateMenu,
  publishConfig, listConfigHistory, rollbackConfig,
  type LcBizConfigSnapshot,
} from '@/api/lowcode/admin'

const route = useRoute()
const router = useRouter()

const bizCode = computed(() => route.params.bizCode as string | undefined)
const isEdit = computed(() => !!bizCode.value)

const activeTab = ref('basic')
const loading = ref(false)
const saving = ref(false)
const generating = ref(false)
const publishing = ref(false)
const showHistory = ref(false)
const historyList = ref<LcBizConfigSnapshot[]>([])

const config = reactive<any>({
  bizObject: {
    bizCode: '',
    bizName: '',
    storageMode: 'GENERIC',
    processKey: '',
    orderNoPrefix: '',
    workflowEnabled: '1',
    fulfillmentEnabled: '0',
    status: '0',
  },
  fields: [],
  pageSchemas: [],
  nodeAssignees: [],
  branchRules: [],
  actions: [],
  postActions: [],
})

const basicFormRef = ref()
const fieldTableRef = ref<any>(null)
const selectedFields = ref<any[]>([])
let fieldSortable: Sortable | null = null

const basicRules = {
  bizCode: [{ required: true, message: '业务编码不能为空', trigger: 'blur' }],
  bizName: [{ required: true, message: '业务名称不能为空', trigger: 'blur' }],
  storageMode: [{ required: true, message: '请选择存储模式', trigger: 'change' }],
}

// schema.ts 支持的字段类型
const fieldTypes = [
  'string', 'textarea', 'number', 'decimal', 'percent', 'boolean',
  'date', 'datetime', 'date-range', 'time', 'time-range',
  'dict', 'select', 'multi-select', 'sys-ref',
  'computed', 'region', 'address', 'geo', 'file', 'image', 'richtext',
]

function loadConfig() {
  if (!bizCode.value) return
  loading.value = true
  getBizConfig(bizCode.value)
    .then((res: any) => {
      const data = res.data || res
      if (data) {
        Object.assign(config.bizObject, data.bizObject || {})
        config.fields = data.fields || []
        config.pageSchemas = data.pageSchemas || []
        config.nodeAssignees = data.nodeAssignees || []
        config.branchRules = data.branchRules || []
        config.actions = data.actions || []
        config.postActions = data.postActions || []
      }
      loading.value = false
      if (activeTab.value === 'fields') {
        nextTick(() => initFieldSortable())
      }
    })
    .catch(() => {
      loading.value = false
    })
}

function addField() {
  config.fields.push({
    fieldKey: '',
    fieldLabel: '',
    fieldType: 'string',
    required: '0',
    isQuery: '0',
    isList: '0',
    isDetail: '1',
    isProcessVar: '0',
    dictType: '',
    orderNum: config.fields.length + 1,
  })
  if (activeTab.value === 'fields') {
    nextTick(() => initFieldSortable())
  }
}

function removeField(index: number) {
  config.fields.splice(index, 1)
}

function handleFieldSelectionChange(val: any[]) {
  selectedFields.value = val
}

function initFieldSortable() {
  if (fieldSortable) {
    fieldSortable.destroy()
    fieldSortable = null
  }
  const el = fieldTableRef.value?.$el.querySelector('.el-table__body-wrapper tbody')
  if (!el) return
  fieldSortable = Sortable.create(el, {
    handle: '.drag-handle',
    animation: 150,
    onEnd: (evt) => {
      const { oldIndex, newIndex } = evt
      if (oldIndex === newIndex || oldIndex == null || newIndex == null) return
      const moved = config.fields.splice(oldIndex, 1)[0]
      config.fields.splice(newIndex, 0, moved)
      config.fields.forEach((f: any, i: number) => { f.orderNum = i + 1 })
      fieldSortable?.sort(config.fields.map((_: any, i: number) => i.toString()), true)
      ElMessage.success('字段排序已更新')
    },
  })
}

function copyField(row: any) {
  const copy = { ...row, fieldKey: row.fieldKey + '_copy', orderNum: config.fields.length + 1 }
  config.fields.push(copy)
  ElMessage.success('已复制字段')
  if (activeTab.value === 'fields') {
    nextTick(() => initFieldSortable())
  }
}

function batchSetRequired() {
  selectedFields.value.forEach((row: any) => { row.required = '1' })
  ElMessage.success('已批量设为必填')
}

function batchSetList() {
  selectedFields.value.forEach((row: any) => { row.isList = '1' })
  ElMessage.success('已批量设为列表显示')
}

function batchSetQuery() {
  selectedFields.value.forEach((row: any) => { row.isQuery = '1' })
  ElMessage.success('已批量设为查询条件')
}

function batchDeleteFields() {
  ElMessageBox.confirm('确认删除选中的 ' + selectedFields.value.length + ' 个字段吗？', '批量删除', { type: 'warning' })
    .then(() => {
      const keys = new Set(selectedFields.value.map((r: any) => r.fieldKey))
      config.fields = config.fields.filter((f: any) => !keys.has(f.fieldKey))
      selectedFields.value = []
      ElMessage.success('批量删除成功')
      if (activeTab.value === 'fields') {
        nextTick(() => initFieldSortable())
      }
    })
    .catch(() => {})
}

function handleSave() {
  if (!basicFormRef.value) return
  basicFormRef.value.validate((valid: boolean) => {
    if (!valid) {
      activeTab.value = 'basic'
      return
    }
    saving.value = true
    saveBizConfig(config)
      .then(() => {
        ElMessage.success('保存成功')
        if (bizCode.value) {
          loadConfig()
        }
        saving.value = false
      })
      .catch(() => {
        saving.value = false
      })
  })
}

function addAssignee() {
  config.nodeAssignees.push({
    taskKey: '',
    assigneeType: 'FIXED_USER',
    assigneeValue: '',
  })
}

function removeAssignee(index: number) {
  config.nodeAssignees.splice(index, 1)
}

function addBranch() {
  config.branchRules.push({
    sourceKey: '',
    fieldKey: '',
    operator: '>=',
    compareValue: '',
    targetVar: '',
  })
}

function removeBranch(index: number) {
  config.branchRules.splice(index, 1)
}

function addAction() {
  config.actions.push({
    actionCode: '',
    actionName: '',
    actionType: 'BUILTIN',
    triggerStatus: 'DRAFT',
    apiEndpoint: '',
    buttonStyle: 'primary',
    buttonIcon: '',
    sortOrder: config.actions.length + 1,
  })
}

function removeAction(index: number) {
  config.actions.splice(index, 1)
}

function addPostAction() {
  config.postActions.push({
    triggerEvent: 'AFTER_APPROVE',
    actionType: 'UPDATE_STATUS',
    targetField: '',
    targetValue: '',
    conditionExpr: '',
    callbackUrl: '',
    sortOrder: config.postActions.length + 1,
  })
}

function removePostAction(index: number) {
  config.postActions.splice(index, 1)
}

function handleSaveAndGenerateMenu() {
  if (!basicFormRef.value) return
  basicFormRef.value.validate((valid: boolean) => {
    if (!valid) {
      activeTab.value = 'basic'
      return
    }
    generating.value = true
    saveBizConfig(config)
      .then(() => {
        return generateMenu({
          bizCode: config.bizObject.bizCode,
          bizName: config.bizObject.bizName,
          icon: 'documentation',
        })
      })
      .then(() => {
        ElMessage.success('菜单已生成，请重新登录或刷新菜单查看')
        if (bizCode.value) {
          loadConfig()
        }
        generating.value = false
      })
      .catch(() => {
        ElMessage.error('生成菜单失败')
        generating.value = false
      })
  })
}

async function handlePublish() {
  if (!bizCode.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入发布说明（可选）', '发布版本', {
      confirmButtonText: '发布',
      cancelButtonText: '取消',
      inputPlaceholder: '如：新增审批人字段',
    })
    publishing.value = true
    await publishConfig(bizCode.value, value || undefined)
    ElMessage.success('发布成功')
    await loadConfig()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('发布失败')
    }
  } finally {
    publishing.value = false
  }
}

async function loadHistory() {
  if (!bizCode.value) return
  try {
    const res: any = await listConfigHistory(bizCode.value)
    historyList.value = res.data || []
    showHistory.value = true
  } catch {
    ElMessage.error('加载版本历史失败')
  }
}

async function handleRollback(row: LcBizConfigSnapshot) {
  if (!bizCode.value) return
  try {
    await ElMessageBox.confirm(
      '确认回滚到版本 ' + row.versionNo + ' 吗？当前草稿将被覆盖，需重新发布以生效。',
      '回滚确认',
      { type: 'warning' },
    )
    await rollbackConfig(bizCode.value, row.versionNo)
    ElMessage.success('回滚成功，请重新发布以生效')
    await loadConfig()
    showHistory.value = false
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('回滚失败')
    }
  }
}

function goBack() {
  router.push('/lowcode/admin')
}

watch(() => activeTab.value, (val) => {
  if (val === 'fields') {
    nextTick(() => initFieldSortable())
  }
})

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.edit-tabs {
  margin-top: 8px;
}
.tab-toolbar {
  margin-bottom: 12px;
}
.drag-handle {
  font-size: 16px;
}
.drag-handle:hover {
  color: #409eff !important;
}
</style>
