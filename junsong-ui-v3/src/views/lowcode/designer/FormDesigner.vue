<template>
  <div class="form-designer">
    <!-- 左侧控件面板 -->
    <aside class="designer-sidebar designer-sidebar--left">
      <el-card shadow="never" class="designer-card">
        <template #header>
          <span>控件库</span>
        </template>
        <div class="control-list">
          <div
            v-for="ctrl in controlList"
            :key="ctrl.type"
            class="control-item"
            draggable="true"
            @dragstart="handleControlDragStart(ctrl)"
            @dragend="handleControlDragEnd"
          >
            <el-icon :size="16"><component :is="ctrl.icon" /></el-icon>
            <span>{{ ctrl.label }}</span>
          </div>
        </div>
      </el-card>
    </aside>

    <!-- 中间画布 -->
    <main
      class="designer-canvas"
      @dragover.prevent
      @drop="handleCanvasDrop"
    >
      <el-empty v-if="!fields.length" description="从左侧拖拽控件到此处" />
      <el-row v-else ref="canvasListRef" :gutter="12" class="canvas-content">
        <el-col
          v-for="(field, index) in fields"
          :key="field.__id"
          :span="fieldSpan(field)"
          class="canvas-col"
        >
          <div
            class="canvas-field"
            :class="{ active: selectedIndex === index }"
            @click="selectField(index)"
          >
            <div class="field-header">
              <el-checkbox
                class="field-select-checkbox"
                :model-value="isFieldSelected(field)"
                @update:model-value="(val) => toggleFieldSelection(field, val as boolean)"
                @click.stop
              />
              <div class="field-drag-handle" title="拖动排序">
                <el-icon><Rank /></el-icon>
              </div>
              <div class="field-index">{{ index + 1 }}</div>
              <div class="field-label-text">
                <span class="field-name">{{ field.fieldLabel || '未命名字段' }}</span>
                <el-tag size="small" type="info" class="field-type-tag">{{ typeLabel(field.fieldType) }}</el-tag>
                <el-tag v-if="field.required === '1'" size="small" type="danger">必填</el-tag>
              </div>
              <div class="field-actions">
                <el-button link type="danger" size="small" @click.stop="removeField(index)" title="删除">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="field-preview">
              <el-divider v-if="field.fieldType === 'divider'" content-position="left">
                {{ field.fieldLabel || '分隔线' }}
              </el-divider>
              <h3 v-else-if="field.fieldType === 'title'" class="lc-title-field">
                {{ field.fieldLabel || '标题' }}
              </h3>
              <el-form-item v-else :label="field.fieldLabel || '未命名'" :required="field.required === '1'">
                <FieldRenderer
                  :field="field"
                  :model-value="previewValue(field)"
                />
              </el-form-item>
            </div>
          </div>
        </el-col>
      </el-row>
    </main>

    <!-- 右侧属性面板 -->
    <aside class="designer-sidebar designer-sidebar--right">
      <el-card shadow="never" class="designer-card">
        <template #header>
          <span>属性配置</span>
        </template>
        <el-empty v-if="selectedIndex === -1" description="请选择一个字段" :image-size="60" />
        <el-form v-else :model="selectedField" label-width="80px" size="small">
          <template v-if="isLayoutField(selectedField)">
            <el-form-item label="标题文字">
              <el-input v-model="selectedField.fieldLabel" placeholder="如：基本信息" />
            </el-form-item>
          </template>
          <template v-else>
          <el-form-item label="字段名称">
            <el-input v-model="selectedField.fieldLabel" placeholder="如：申请人" />
          </el-form-item>
          <el-form-item label="字段Key">
            <el-input v-model="selectedField.fieldKey" placeholder="如：applicant" />
          </el-form-item>
          <el-form-item label="字段类型">
            <el-select v-model="selectedField.fieldType" style="width: 100%" @change="onTypeChange">
              <el-option v-for="ctrl in controlList" :key="ctrl.type" :label="ctrl.label" :value="ctrl.type" />
            </el-select>
          </el-form-item>
          <el-form-item label="必填">
            <el-switch v-model="selectedField.required" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item label="列表展示">
            <el-switch v-model="selectedField.isList" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item label="查询条件">
            <el-switch v-model="selectedField.isQuery" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item label="栅格宽度">
            <el-slider v-model="selectedFieldSpan" :min="4" :max="24" :step="4" show-stops />
          </el-form-item>
          <el-form-item v-if="selectedField.fieldType === 'dict'" label="字典类型">
            <el-input v-model="selectedField.dictType" placeholder="如：sys_user_sex" />
          </el-form-item>
          <el-form-item v-if="selectedField.fieldType === 'address'" label="显示经纬度">
            <el-switch v-model="selectedShowLngLat" />
          </el-form-item>
          <!-- 子表单：子字段配置 -->
          <el-form-item v-if="selectedField.fieldType === 'subform'" label="子字段">
            <div class="subfield-config">
              <div class="subfield-list">
                <div v-for="(sf, idx) in selectedSubFields" :key="idx" class="subfield-item">
                  <el-input v-model="sf.fieldLabel" size="small" placeholder="字段名" class="subfield-input-label" />
                  <el-select v-model="sf.fieldType" size="small" class="subfield-input-type">
                    <el-option v-for="ctrl in subFieldTypeOptions" :key="ctrl.type" :label="ctrl.label" :value="ctrl.type" />
                  </el-select>
                  <el-input v-model="sf.fieldKey" size="small" placeholder="Key" class="subfield-input-key" />
                  <el-button :icon="Delete" size="small" text type="danger" @click="removeSubField(idx)" />
                </div>
              </div>
              <el-button :icon="Plus" size="small" plain @click="addSubField" class="subfield-add-btn">添加子字段</el-button>
            </div>
          </el-form-item>
          <el-form-item label="默认值">
            <el-input v-model="selectedField.defaultValue" placeholder="如：默认值" />
          </el-form-item>
          <el-form-item label="排序号">
            <el-input-number v-model="selectedField.orderNum" :min="0" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="duplicateField">复制字段</el-button>
          </el-form-item>
          </template>
        </el-form>
      </el-card>
    </aside>

    <!-- 批量操作工具栏 -->
    <transition name="el-fade-in">
      <div v-if="selectedFieldIds.length" class="batch-toolbar">
        <div class="batch-toolbar-left">
          <el-checkbox
            :model-value="isAllSelected"
            :indeterminate="isIndeterminate"
            @update:model-value="(val) => toggleSelectAll(val as boolean)"
          >
            全选
          </el-checkbox>
          <span class="batch-count">已选 {{ selectedFields.length }} 个字段</span>
        </div>
        <div class="batch-toolbar-right">
          <el-button size="small" type="primary" @click="batchSetRequired('1')">批量必填</el-button>
          <el-button size="small" @click="batchSetRequired('0')">取消必填</el-button>
          <el-button size="small" type="success" @click="batchSetIsList('1')">列表展示</el-button>
          <el-button size="small" @click="batchSetIsList('0')">取消列表</el-button>
          <el-select
            v-model="batchSpanValue"
            size="small"
            placeholder="栅格宽度"
            style="width: 130px"
            @change="onBatchSpanChange"
          >
            <el-option :value="12" label="栅格 12" />
            <el-option :value="24" label="栅格 24" />
          </el-select>
          <el-button size="small" text type="info" @click="selectedFieldIds = []">取消选择</el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Delete, Document, Calendar, Switch, Files, Picture, MapLocation, Grid, Money,
  Clock, Location, User, Rank, Plus,
} from '@element-plus/icons-vue'
import Sortable from 'sortablejs'
import FieldRenderer from '../fields/FieldRenderer.vue'

export interface DesignerField {
  __id: string
  fieldKey: string
  fieldLabel: string
  fieldType: string
  required: string
  isList: string
  isQuery: string
  isDetail: string
  isProcessVar: string
  orderNum: number
  dictType?: string
  defaultValue?: string
  fieldExt?: string
  bizCode: string
}

const props = defineProps<{
  modelValue: DesignerField[]
}>()

const emit = defineEmits<{ (e: 'update:modelValue', val: DesignerField[]): void }>()

const fields = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const selectedIndex = ref(-1)
const canvasListRef = ref<any>(null)
let sortableInstance: Sortable | null = null

const draggedControl = ref<any>(null)

const selectedField = computed(() => {
  if (selectedIndex.value === -1) return null
  return fields.value[selectedIndex.value]
})

// ===== 批量选择与批量编辑 =====
const selectedFieldIds = ref<string[]>([])

function isFieldSelected(field: DesignerField): boolean {
  return selectedFieldIds.value.includes(field.__id)
}

function toggleFieldSelection(field: DesignerField, checked: boolean) {
  if (checked) {
    if (!selectedFieldIds.value.includes(field.__id)) {
      selectedFieldIds.value = [...selectedFieldIds.value, field.__id]
    }
  } else {
    selectedFieldIds.value = selectedFieldIds.value.filter((id) => id !== field.__id)
  }
}

const selectedFields = computed(() => {
  const idSet = new Set(selectedFieldIds.value)
  return fields.value.filter((f) => idSet.has(f.__id))
})

const isAllSelected = computed(
  () => fields.value.length > 0 && selectedFields.value.length === fields.value.length,
)

const isIndeterminate = computed(
  () => selectedFieldIds.value.length > 0 && selectedFields.value.length < fields.value.length,
)

function toggleSelectAll(checked: boolean) {
  selectedFieldIds.value = checked ? fields.value.map((f) => f.__id) : []
}

function batchSetRequired(val: string) {
  if (!selectedFields.value.length) return
  selectedFields.value.forEach((f) => {
    f.required = val
  })
  ElMessage.success(`已${val === '1' ? '设置' : '取消'}必填（${selectedFields.value.length} 个字段）`)
}

function batchSetIsList(val: string) {
  if (!selectedFields.value.length) return
  selectedFields.value.forEach((f) => {
    f.isList = val
  })
  ElMessage.success(`已${val === '1' ? '设置' : '取消'}列表展示（${selectedFields.value.length} 个字段）`)
}

const batchSpanValue = ref<number | undefined>(undefined)

function batchSetSpan(span: number) {
  if (!selectedFields.value.length) return
  selectedFields.value.forEach((f) => {
    let ext: any = {}
    try {
      ext = f.fieldExt ? JSON.parse(f.fieldExt) : {}
    } catch {
      ext = {}
    }
    ext.span = span
    f.fieldExt = JSON.stringify(ext)
  })
  ElMessage.success(`已设置栅格宽度 ${span}（${selectedFields.value.length} 个字段）`)
}

function onBatchSpanChange(val: number | undefined) {
  if (val == null) return
  batchSetSpan(val)
  nextTick(() => {
    batchSpanValue.value = undefined
  })
}

const selectedFieldSpan = computed({
  get: () => {
    if (!selectedField.value) return 12
    const ext = selectedField.value.fieldExt ? JSON.parse(selectedField.value.fieldExt) : {}
    return ext.span || 12
  },
  set: (val) => {
    if (!selectedField.value) return
    const ext = selectedField.value.fieldExt ? JSON.parse(selectedField.value.fieldExt) : {}
    ext.span = val
    selectedField.value.fieldExt = JSON.stringify(ext)
  },
})

const selectedShowLngLat = computed({
  get: () => {
    if (!selectedField.value) return false
    try {
      const ext = selectedField.value.fieldExt ? JSON.parse(selectedField.value.fieldExt) : {}
      return ext.showLngLat === true
    } catch {
      return false
    }
  },
  set: (val) => {
    if (!selectedField.value) return
    const ext = selectedField.value.fieldExt ? JSON.parse(selectedField.value.fieldExt) : {}
    ext.showLngLat = val
    selectedField.value.fieldExt = JSON.stringify(ext)
  },
})

const subFieldTypeOptions = [
  { type: 'string', label: '文本框' },
  { type: 'number', label: '数字' },
  { type: 'decimal', label: '小数' },
  { type: 'date', label: '日期' },
  { type: 'dict', label: '字典' },
  { type: 'boolean', label: '开关' },
]

const selectedSubFields = computed<any[]>({
  get() {
    if (!selectedField.value) return []
    try {
      const ext = selectedField.value.fieldExt ? JSON.parse(selectedField.value.fieldExt) : {}
      return ext.subFields || []
    } catch {
      return []
    }
  },
  set(val) {
    if (!selectedField.value) return
    const ext = selectedField.value.fieldExt ? JSON.parse(selectedField.value.fieldExt) : {}
    ext.subFields = val
    selectedField.value.fieldExt = JSON.stringify(ext)
  },
})

function addSubField() {
  const list = [...selectedSubFields.value]
  list.push({ fieldKey: '', fieldLabel: '字段' + (list.length + 1), fieldType: 'string', required: '0' })
  selectedSubFields.value = list
}

function removeSubField(idx: number) {
  const list = [...selectedSubFields.value]
  list.splice(idx, 1)
  selectedSubFields.value = list
}

const TYPE_LABEL_MAP: Record<string, string> = {
  string: '文本框',
  textarea: '多行文本',
  number: '数字',
  decimal: '小数',
  percent: '百分比',
  computed: '计算字段',
  boolean: '开关',
  date: '日期',
  datetime: '日期时间',
  'date-range': '日期范围',
  time: '时间',
  'time-range': '时间范围',
  dict: '字典',
  select: '下拉框',
  'multi-select': '多选',
  'sys-ref': '系统引用',
  region: '省市区',
  address: '地址',
  geo: '地理位置',
  file: '附件',
  image: '图片',
  richtext: '富文本',
  subform: '子表单',
  divider: '分隔线',
  title: '标题',
}

function typeLabel(type: string) {
  return TYPE_LABEL_MAP[type] || type
}

function isLayoutField(field: DesignerField | null): boolean {
  if (!field) return false
  return field.fieldType === 'divider' || field.fieldType === 'title'
}

function fieldSpan(field: DesignerField): number {
  try {
    const ext = field.fieldExt ? JSON.parse(field.fieldExt) : {}
    const span = Number(ext.span)
    return span >= 4 && span <= 24 ? span : 12
  } catch {
    return 12
  }
}

const controlList = [
  { type: 'string', label: '文本框', icon: 'Document' },
  { type: 'textarea', label: '多行文本', icon: 'Document' },
  { type: 'number', label: '数字', icon: 'Money' },
  { type: 'decimal', label: '小数', icon: 'Money' },
  { type: 'percent', label: '百分比', icon: 'Money' },
  { type: 'computed', label: '计算字段', icon: 'Money' },
  { type: 'boolean', label: '开关', icon: 'Switch' },
  { type: 'date', label: '日期', icon: 'Calendar' },
  { type: 'datetime', label: '日期时间', icon: 'Calendar' },
  { type: 'date-range', label: '日期范围', icon: 'Calendar' },
  { type: 'time', label: '时间', icon: 'Clock' },
  { type: 'time-range', label: '时间范围', icon: 'Clock' },
  { type: 'dict', label: '字典', icon: 'Files' },
  { type: 'select', label: '下拉框', icon: 'Files' },
  { type: 'multi-select', label: '多选', icon: 'Files' },
  { type: 'sys-ref', label: '系统引用', icon: 'User' },
  { type: 'region', label: '省市区', icon: 'Location' },
  { type: 'address', label: '地址', icon: 'Location' },
  { type: 'geo', label: '地理位置', icon: 'Location' },
  { type: 'file', label: '附件', icon: 'Document' },
  { type: 'image', label: '图片', icon: 'Picture' },
  { type: 'richtext', label: '富文本', icon: 'Document' },
  { type: 'subform', label: '子表单', icon: 'Grid' },
  { type: 'divider', label: '分隔线', icon: 'Minus' },
  { type: 'title', label: '标题', icon: 'Document' },
]

// 左侧控件库拖拽
function handleControlDragStart(ctrl: any) {
  draggedControl.value = ctrl
}

function handleControlDragEnd() {
  draggedControl.value = null
}

// 画布接收拖拽（从左侧控件库拖入新字段）
function handleCanvasDrop(_e: DragEvent) {
  if (draggedControl.value) {
    addFieldFromControl(draggedControl.value)
    draggedControl.value = null
  }
}

function addFieldFromControl(ctrl: any) {
  const wideTypes = ['address', 'subform', 'richtext', 'textarea', 'geo', 'image', 'file', 'date-range', 'time-range', 'divider', 'title']
  const defaultSpan = wideTypes.includes(ctrl.type) ? 24 : 12
  const newField: DesignerField = {
    __id: 'field_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7),
    fieldKey: '',
    fieldLabel: ctrl.label,
    fieldType: ctrl.type,
    required: '0',
    isList: '1',
    isQuery: '0',
    isDetail: '1',
    isProcessVar: '0',
    orderNum: fields.value.length,
    fieldExt: JSON.stringify({ span: defaultSpan }),
    bizCode: '',
  }
  if (ctrl.type === 'dict') {
    newField.dictType = ''
  }
  fields.value = [...fields.value, newField]
  selectedIndex.value = fields.value.length - 1
}

function selectField(index: number) {
  selectedIndex.value = index
}

function removeField(index: number) {
  const newFields = [...fields.value]
  newFields.splice(index, 1)
  fields.value = newFields
  if (selectedIndex.value === index) {
    selectedIndex.value = -1
  } else if (selectedIndex.value > index) {
    selectedIndex.value--
  }
}

function duplicateField() {
  if (!selectedField.value) return
  const src = selectedField.value
  const copy: DesignerField = {
    ...src,
    __id: 'field_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7),
    fieldKey: src.fieldKey ? src.fieldKey + '_copy' : '',
    fieldLabel: (src.fieldLabel || '字段') + '_副本',
    orderNum: fields.value.length,
  }
  fields.value = [...fields.value, copy]
  selectedIndex.value = fields.value.length - 1
  ElMessage.success('已复制')
}

function onTypeChange(newType: string) {
  if (!selectedField.value) return
  if (newType === 'dict' && !selectedField.value.dictType) {
    selectedField.value.dictType = ''
  }
}

function previewValue(field: DesignerField): any {
  const type = field.fieldType
  if (type === 'boolean') return false
  if (type === 'number' || type === 'decimal' || type === 'percent') return null
  if (type === 'multi-select' || type === 'subform' || type === 'date-range' || type === 'time-range') return []
  return ''
}

function initSortable() {
  nextTick(() => {
    const el = canvasListRef.value?.$el || canvasListRef.value
    if (el && !sortableInstance) {
      sortableInstance = Sortable.create(el, {
        handle: '.field-drag-handle',
        animation: 150,
        ghostClass: 'sortable-ghost',
        chosenClass: 'sortable-chosen',
        onEnd: (evt) => {
          if (evt.oldIndex === undefined || evt.newIndex === undefined) return
          if (evt.oldIndex === evt.newIndex) return
          const newFields = [...fields.value]
          const [moved] = newFields.splice(evt.oldIndex, 1)
          newFields.splice(evt.newIndex, 0, moved)
          fields.value = newFields
          if (selectedIndex.value === evt.oldIndex) {
            selectedIndex.value = evt.newIndex
          } else if (evt.oldIndex < selectedIndex.value && evt.newIndex >= selectedIndex.value) {
            selectedIndex.value--
          } else if (evt.oldIndex > selectedIndex.value && evt.newIndex <= selectedIndex.value) {
            selectedIndex.value++
          }
        },
      })
    }
  })
}

watch(
  () => fields.value.length,
  () => {
    initSortable()
  },
  { immediate: true },
)

// 字段增删/重排时清理失效的选中 id
watch(
  () => fields.value.map((f) => f.__id).join(','),
  () => {
    const idSet = new Set(fields.value.map((f) => f.__id))
    selectedFieldIds.value = selectedFieldIds.value.filter((id) => idSet.has(id))
  },
)
</script>

<style scoped>
.form-designer {
  display: flex;
  height: calc(100vh - 160px);
  gap: 12px;
}

.designer-sidebar {
  width: 220px;
  flex-shrink: 0;
}

.designer-sidebar--right {
  width: 280px;
}

.subfield-config {
  width: 100%;
}

.subfield-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
  max-height: 240px;
  overflow-y: auto;
}

.subfield-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.subfield-input-label {
  flex: 1.4;
}

.subfield-input-type {
  width: 84px;
  flex-shrink: 0;
}

.subfield-input-key {
  flex: 1;
}

.subfield-add-btn {
  width: 100%;
}

.designer-card {
  height: 100%;
}

.designer-card :deep(.el-card__body) {
  height: calc(100% - 50px);
  overflow-y: auto;
}

.control-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.control-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px 8px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: grab;
  font-size: 12px;
  color: var(--el-text-color-regular);
  transition: all 0.2s;
}

.control-item:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.designer-canvas {
  flex: 1;
  background: #f5f7fa;
  border: 2px dashed var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
  overflow-y: auto;
}

.canvas-content {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.canvas-col {
  margin-bottom: 8px;
}

.canvas-field {
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
  height: 100%;
}

.canvas-field:hover {
  border-color: var(--el-color-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.canvas-field.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

.canvas-field.sortable-ghost {
  opacity: 0.4;
  background: var(--el-color-primary-light-9);
}

.canvas-field.sortable-chosen {
  cursor: grabbing;
}

.field-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.field-drag-handle {
  cursor: grab;
  color: var(--el-text-color-placeholder);
  display: flex;
  align-items: center;
}

.field-drag-handle:hover {
  color: var(--el-color-primary);
}

.field-drag-handle:active {
  cursor: grabbing;
}

.field-index {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-radius: 50%;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.field-label-text {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.field-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-type-tag {
  flex-shrink: 0;
}

.field-actions {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.canvas-field:hover .field-actions {
  opacity: 1;
}

.field-preview {
  padding: 12px 16px;
}

.field-preview :deep(.el-form-item) {
  margin-bottom: 0;
}

.field-select-checkbox {
  flex-shrink: 0;
}

.batch-toolbar {
  position: fixed;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 18px;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.batch-toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-count {
  font-size: 13px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.batch-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
