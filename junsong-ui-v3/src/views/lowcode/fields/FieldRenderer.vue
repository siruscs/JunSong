<template>
  <!-- 条件不可见时完全移除 -->
  <div v-if="isVisible">

  <!-- 只读展示模式（详情页） -->
  <template v-if="readonly">
    <span class="lc-field-readonly">{{ displayText }}</span>
  </template>

  <!-- 字符串 / 文本域 -->
  <el-input
    v-else-if="field.fieldType === 'string'"
    v-model="proxyValue"
    :type="ext.textarea ? 'textarea' : 'text'"
    :rows="ext.rows || 3"
    :maxlength="ext.maxlength"
    :show-word-limit="!!ext.maxlength"
    :placeholder="placeholder"
    clearable
  />

  <!-- 数字 / 小数 -->
  <el-input-number
    v-else-if="field.fieldType === 'number' || field.fieldType === 'decimal'"
    v-model="proxyValue"
    :min="ext.min"
    :max="ext.max"
    :precision="field.fieldType === 'decimal' ? (ext.scale ?? 2) : 0"
    :step="ext.step || 1"
    :controls="ext.controls !== false"
    style="width: 100%"
  />

  <!-- 百分比 -->
  <el-input-number
    v-else-if="field.fieldType === 'percent'"
    v-model="proxyValue"
    :min="0"
    :max="100"
    :precision="ext.scale ?? 2"
    style="width: 100%"
  >
    <template #suffix>%</template>
  </el-input-number>

  <!-- 布尔开关 -->
  <el-switch
    v-else-if="field.fieldType === 'boolean'"
    v-model="proxyValue"
    :active-text="ext.activeText || '是'"
    :inactive-text="ext.inactiveText || '否'"
  />

  <!-- 字典下拉 -->
  <el-select
    v-else-if="field.fieldType === 'dict'"
    v-model="proxyValue"
    :placeholder="placeholder"
    clearable
    filterable
    style="width: 100%"
  >
    <el-option v-for="opt in dictOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
  </el-select>

  <!-- 多选 -->
  <el-select
    v-else-if="field.fieldType === 'multi-select'"
    v-model="proxyValue"
    multiple
    collapse-tags
    collapse-tags-tooltip
    :placeholder="placeholder"
    clearable
    filterable
    style="width: 100%"
  >
    <el-option v-for="opt in staticOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
  </el-select>

  <!-- 系统数据引用：人员 -->
  <el-select
    v-else-if="field.fieldType === 'sys-ref' && ext.source === 'user'"
    v-model="proxyValue"
    :multiple="!!ext.multiple"
    filterable
    remote
    :remote-method="searchUsers"
    :loading="refLoading"
    :placeholder="placeholder || '输入姓名搜索'"
    clearable
    style="width: 100%"
  >
    <el-option v-for="opt in refOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
  </el-select>

  <!-- 系统数据引用：部门树 -->
  <el-tree-select
    v-else-if="field.fieldType === 'sys-ref' && ext.source === 'dept'"
    v-model="proxyValue"
    :data="deptTree"
    :multiple="!!ext.multiple"
    check-strictly
    :render-after-expand="false"
    node-key="id"
    :props="{ label: 'label', children: 'children' }"
    :placeholder="placeholder || '请选择部门'"
    clearable
    style="width: 100%"
  />

  <!-- 系统数据引用：角色 / 岗位 -->
  <el-select
    v-else-if="field.fieldType === 'sys-ref' && (ext.source === 'role' || ext.source === 'post')"
    v-model="proxyValue"
    :multiple="!!ext.multiple"
    filterable
    :placeholder="placeholder"
    clearable
    style="width: 100%"
  >
    <el-option v-for="opt in refOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
  </el-select>

  <!-- 计算字段（只读，由依赖自动算出；editable 时允许覆盖） -->
  <el-input
    v-else-if="field.fieldType === 'computed'"
    v-model="proxyValue"
    :disabled="!ext.editable"
    placeholder="自动计算"
  />

  <!-- 日期 / 日期时间 -->
  <el-date-picker
    v-else-if="field.fieldType === 'date' || field.fieldType === 'datetime'"
    v-model="proxyValue"
    :type="field.fieldType === 'datetime' ? 'datetime' : 'date'"
    :value-format="field.fieldType === 'datetime' ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD'"
    :placeholder="placeholder"
    style="width: 100%"
  />

  <!-- 日期区间 -->
  <el-date-picker
    v-else-if="field.fieldType === 'date-range'"
    v-model="proxyValue"
    type="daterange"
    value-format="YYYY-MM-DD"
    range-separator="至"
    start-placeholder="开始日期"
    end-placeholder="结束日期"
    style="width: 100%"
  />

  <!-- 时间 -->
  <el-time-picker
    v-else-if="field.fieldType === 'time'"
    v-model="proxyValue"
    value-format="HH:mm:ss"
    :placeholder="placeholder"
    style="width: 100%"
  />

  <!-- 时间区间 -->
  <el-time-picker
    v-else-if="field.fieldType === 'time-range'"
    v-model="proxyValue"
    is-range
    value-format="HH:mm:ss"
    range-separator="至"
    start-placeholder="开始时间"
    end-placeholder="结束时间"
    style="width: 100%"
  />

  <!-- 省市区级联 -->
  <el-cascader
    v-else-if="field.fieldType === 'region'"
    :model-value="regionCodeValue"
    :options="regionOptions"
    :props="{ checkStrictly: false }"
    clearable
    filterable
    style="width: 100%"
    @update:model-value="onRegionChange"
  />

  <!-- 完整地址：省市区 + 详细地址 -->
  <div v-else-if="field.fieldType === 'address'" class="lc-address">
    <el-cascader
      :model-value="addressRegionValue"
      :options="regionOptions"
      clearable
      filterable
      style="width: 100%"
      @update:model-value="onAddressRegionChange"
    />
    <el-input
      :model-value="addressDetailValue"
      placeholder="请输入详细地址"
      style="margin-top: 8px"
      @update:model-value="onAddressDetailChange"
    />
  </div>

  <!-- 经纬度坐标（阶段 2 简化为手动输入，地图选点属阶段 3 接入） -->
  <div v-else-if="field.fieldType === 'geo'" class="lc-geo">
    <el-input
      :model-value="geoLngValue"
      placeholder="经度"
      @update:model-value="(v: string) => onGeoChange('longitude', v)"
    />
    <el-input
      :model-value="geoLatValue"
      placeholder="纬度"
      @update:model-value="(v: string) => onGeoChange('latitude', v)"
    />
  </div>

  <!-- 文件上传 -->
  <FileUpload
    v-else-if="field.fieldType === 'file'"
    v-model="proxyValue"
    :limit="uploadConf.limit"
    :file-size="uploadConf.fileSize"
    :file-type="uploadConf.fileType"
  />

  <!-- 图片上传 -->
  <ImageUpload
    v-else-if="field.fieldType === 'image'"
    v-model="proxyValue"
    :limit="uploadConf.limit"
    :file-size="uploadConf.fileSize"
    :file-type="uploadConf.fileType"
  />

  <!-- 富文本（复用项目 Editor） -->
  <Editor
    v-else-if="field.fieldType === 'richtext'"
    v-model="proxyValue"
    :rows="ext.rows || 6"
    :placeholder="placeholder"
  />

  <!-- 兜底 -->
  <el-input v-else v-model="proxyValue" :placeholder="placeholder" clearable />

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import FileUpload from '@/components/FileUpload/index.vue'
import ImageUpload from '@/components/ImageUpload/index.vue'
import Editor from '@/components/Editor/index.vue'
import { useDict } from '@/composables/useDict'
import { getRegionTree } from '@/api/system/region'
import { listUser } from '@/api/system/user'
import { treeselect as deptTreeSelect } from '@/api/system/dept'
import { listRole } from '@/api/system/role'
import { postOptionSelect } from '@/api/system/post'
import type { LcBizField } from '@/api/lowcode'
import { parseFieldExt } from '../schema'

const props = defineProps<{
  field: LcBizField
  modelValue: any
  readonly?: boolean
  formValues?: Record<string, any>  // 所有字段当前值，用于条件可见性求值
}>()

const emit = defineEmits<{ (e: 'update:modelValue', val: any): void }>()

const proxyValue = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const ext = computed(() => parseFieldExt(props.field))
const placeholder = computed(() => `请输入${props.field.fieldLabel}`)

// ===== 条件可见性 =====
const isVisible = computed(() => {
  const fieldExt = props.field.fieldExt
  if (!fieldExt) return true
  try {
    const extObj = typeof fieldExt === 'string' ? JSON.parse(fieldExt) : fieldExt
    const cond = extObj?.visibleCondition
    if (!cond) return true

    const when = cond.when
    if (!when) return true

    // 单条件
    if (when.field) {
      const actual = props.formValues?.[when.field]
      return evaluateOp(actual, when.op || 'eq', when.value)
    }

    // 组合条件
    const conditions = when.conditions
    const groupOp = when.op || 'AND'
    if (!conditions || !Array.isArray(conditions)) return true
    if (groupOp === 'AND') {
      return conditions.every((c: any) => {
        const actual = props.formValues?.[c.field]
        return evaluateOp(actual, c.op || 'eq', c.value)
      })
    } else {
      return conditions.some((c: any) => {
        const actual = props.formValues?.[c.field]
        return evaluateOp(actual, c.op || 'eq', c.value)
      })
    }
  } catch {
    return true
  }
})

function evaluateOp(actual: any, op: string, expected: any): boolean {
  if (actual == null) actual = ''
  switch (op) {
    case 'eq': return String(actual) === String(expected)
    case 'ne': return String(actual) !== String(expected)
    case 'gt': return Number(actual) > Number(expected)
    case 'ge': return Number(actual) >= Number(expected)
    case 'lt': return Number(actual) < Number(expected)
    case 'le': return Number(actual) <= Number(expected)
    case 'contains': return String(actual).includes(String(expected))
    default: return String(actual) === String(expected)
  }
}

const uploadConf = computed(() => {
  if (!props.field.uploadConfig) return {}
  try {
    return JSON.parse(props.field.uploadConfig) || {}
  } catch {
    return {}
  }
})

const staticOptions = computed<{ label: string; value: any }[]>(() => {
  const opts = ext.value.options
  if (Array.isArray(opts)) {
    return opts.map((o: any) =>
      typeof o === 'object' ? { label: o.label, value: o.value } : { label: String(o), value: o },
    )
  }
  return []
})

const dictData = props.field.fieldType === 'dict' && props.field.dictType ? useDict(props.field.dictType) : null
const dictOptions = computed<{ label: string; value: any }[]>(() => {
  if (dictData && props.field.dictType) return dictData.type[props.field.dictType] || []
  return staticOptions.value
})

const refOptions = ref<{ label: string; value: any }[]>([])
const refLoading = ref(false)
const deptTree = ref<any[]>([])
const regionOptions = ref<any[]>([])

async function loadRegionOptions() {
  if (regionOptions.value.length > 0) return
  try {
    const res: any = await getRegionTree()
    regionOptions.value = res.data || []
  } catch (e) {
    console.error('加载地址数据失败', e)
  }
}

async function searchUsers(keyword?: string) {
  if (props.field.fieldType !== 'sys-ref' || ext.value.source !== 'user') return
  refLoading.value = true
  try {
    const res: any = await listUser({ userName: keyword, pageNum: 1, pageSize: 20 })
    const rows = res.rows || res.data || []
    const valueField = ext.value.valueField || 'userName'
    const labelField = ext.value.labelField || 'nickName'
    refOptions.value = rows.map((r: any) => ({ label: r[labelField] ?? r.userName, value: r[valueField] }))
  } finally {
    refLoading.value = false
  }
}

async function loadRoleOptions() {
  const res: any = await listRole({ pageNum: 1, pageSize: 100 })
  const rows = res.rows || res.data || []
  refOptions.value = rows.map((r: any) => ({ label: r.roleName, value: ext.value.valueField === 'roleId' ? r.roleId : r.roleKey }))
}

async function loadPostOptions() {
  const res: any = await postOptionSelect()
  const rows = res.data || []
  refOptions.value = rows.map((r: any) => ({ label: r.postName, value: ext.value.valueField === 'postId' ? r.postId : r.postCode }))
}

async function loadDeptTree() {
  const res: any = await deptTreeSelect()
  deptTree.value = res.data || []
}

onMounted(() => {
  if (props.field.fieldType === 'region' || props.field.fieldType === 'address') {
    loadRegionOptions()
  }
  if (props.readonly || props.field.fieldType !== 'sys-ref') return
  const source = ext.value.source
  if (source === 'dept') loadDeptTree()
  else if (source === 'role') loadRoleOptions()
  else if (source === 'post') loadPostOptions()
  else if (source === 'user' && props.modelValue) searchUsers()
})

// ===== 复合字段：region / address / geo =====
const regionCodeValue = computed<string[]>(() => props.modelValue?.code || [])
function onRegionChange(code: any) {
  const text = labelPathFromCascader(regionOptions.value, code)
  emit('update:modelValue', { code: code || [], text })
}

const addressRegionValue = computed<string[]>(() => props.modelValue?.regionCode || [])
const addressDetailValue = computed<string>(() => props.modelValue?.detail || '')
function onAddressRegionChange(code: any) {
  const text = labelPathFromCascader(regionOptions.value, code)
  const parts = (text || '').split('/')
  emit('update:modelValue', {
    ...(props.modelValue || {}),
    regionCode: code || [],
    province: parts[0] || '',
    city: parts[1] || '',
    district: parts[2] || '',
  })
}
function onAddressDetailChange(detail: string) {
  emit('update:modelValue', { ...(props.modelValue || {}), detail })
}

const geoLngValue = computed(() => (props.modelValue?.longitude ?? '').toString())
const geoLatValue = computed(() => (props.modelValue?.latitude ?? '').toString())
function onGeoChange(key: 'longitude' | 'latitude', val: string) {
  const num = val === '' ? null : Number(val)
  emit('update:modelValue', { ...(props.modelValue || {}), [key]: num })
}

function labelPathFromCascader(options: any[], codes: string[] | null): string {
  if (!codes || !codes.length) return ''
  const labels: string[] = []
  let level = options
  for (const code of codes) {
    const node = level?.find((o) => o.value === code)
    if (!node) break
    labels.push(node.label)
    level = node.children || []
  }
  return labels.join('/')
}

// ===== 只读展示文本 =====
const displayText = computed(() => formatDisplay(props.field, props.modelValue, dictOptions.value, refOptions.value))

function formatDisplay(field: LcBizField, value: any, dicts: any[], refs: any[]): string {
  if (value === null || value === undefined || value === '') return '-'
  switch (field.fieldType) {
    case 'boolean':
      return value ? '是' : '否'
    case 'percent':
      return `${value}%`
    case 'dict': {
      const hit = dicts.find((d) => String(d.value) === String(value))
      return hit ? hit.label : String(value)
    }
    case 'multi-select':
      return Array.isArray(value) ? value.join('、') : String(value)
    case 'date-range':
    case 'time-range':
      return Array.isArray(value) ? value.join(' 至 ') : String(value)
    case 'region':
      return value?.text || (Array.isArray(value?.code) ? value.code.join('/') : '-')
    case 'address':
      return [value?.province, value?.city, value?.district, value?.detail].filter(Boolean).join('') || '-'
    case 'geo':
      return value?.address || (value?.longitude != null ? `${value.longitude}, ${value.latitude}` : '-')
    case 'file':
    case 'image':
      return Array.isArray(value) ? `${value.length} 个文件` : '1 个文件'
    case 'sys-ref': {
      const hit = refs.find((r) => String(r.value) === String(value))
      return hit ? hit.label : String(value)
    }
    default:
      return String(value)
  }
}
</script>

<style scoped>
.lc-field-readonly {
  color: var(--el-text-color-primary);
  word-break: break-word;
}
.lc-address,
.lc-geo {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.lc-geo {
  flex-direction: row;
}
</style>
