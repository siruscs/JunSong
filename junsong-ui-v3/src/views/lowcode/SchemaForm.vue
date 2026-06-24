<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="760px"
    destroy-on-close
    @closed="onClosed"
  >
    <el-skeleton v-if="loading" :rows="6" animated />

    <el-form
      v-else
      ref="formRef"
      :model="formModel"
      :rules="rules"
      label-width="120px"
      label-position="right"
    >
      <el-row :gutter="16">
        <el-col v-for="field in formFields" :key="field.fieldKey" :span="fieldSpan(field)">
          <el-form-item :label="field.fieldLabel" :prop="field.fieldKey">
            <FieldRenderer
              v-model="formModel[field.fieldKey]"
              :field="field"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-empty v-if="!formFields.length" description="该业务暂无可填写字段，请先在元数据中配置" />
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!formFields.length" @click="handleSave">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import FieldRenderer from './fields/FieldRenderer.vue'
import {
  getBizInstance,
  listBizFields,
  saveBizInstance,
  updateBizInstance,
  type LcBizField,
} from '@/api/lowcode'
import { buildFieldRules, defaultValueFor, evalComputed, parseFieldExt } from './schema'

const props = defineProps<{
  bizCode: string
  modelValue: boolean
  recordId?: number | null
  stage?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const allFields = ref<LcBizField[]>([])
const formModel = reactive<Record<string, any>>({})

const stageFilter = computed(() => props.stage || 'APPLY')

const formFields = computed(() =>
  allFields.value
    .filter((f) => (f.stage || 'APPLY') === stageFilter.value)
    .slice()
    .sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0)),
)

const title = computed(() => (props.recordId ? '编辑单据' : '新增单据'))

const rules = computed<FormRules>(() => {
  const result: FormRules = {}
  formFields.value.forEach((field) => {
    const fieldRules = buildFieldRules(field)
    if (fieldRules.length) result[field.fieldKey] = fieldRules
  })
  return result
})

function fieldSpan(field: LcBizField): number {
  const ext = parseFieldExt(field)
  if (ext.span) return ext.span
  if (['richtext', 'address', 'file', 'image'].includes(field.fieldType) || ext.textarea) return 24
  return 12
}

function buildEmptyModel() {
  Object.keys(formModel).forEach((key) => delete formModel[key])
  allFields.value.forEach((field) => {
    formModel[field.fieldKey] = defaultValueFor(field)
  })
}

async function loadFields() {
  loading.value = true
  try {
    const res: any = await listBizFields(props.bizCode)
    allFields.value = res.data || res.rows || []
    buildEmptyModel()
    if (props.recordId) {
      const detail: any = await getBizInstance(props.bizCode, props.recordId)
      const data = detail.data || {}
      const parsed = data.formData ? JSON.parse(data.formData) : {}
      Object.assign(formModel, parsed)
    }
    recalcComputed()
  } finally {
    loading.value = false
  }
}

const computedFields = computed(() => allFields.value.filter((f) => f.fieldType === 'computed'))

function recalcComputed() {
  computedFields.value.forEach((field) => {
    const ext = parseFieldExt(field)
    if (!ext.expression) return
    const value = evalComputed(ext.expression, formModel, ext.scale ?? 2, ext.roundMode)
    formModel[field.fieldKey] = value
  })
}

watch(
  () => formFields.value.map((f) => formModel[f.fieldKey]),
  () => recalcComputed(),
  { deep: true },
)

watch(
  () => [visible.value, props.bizCode, props.recordId],
  ([show]) => {
    if (show) loadFields()
  },
)

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = JSON.parse(JSON.stringify(formModel))
    if (props.recordId) {
      await updateBizInstance(props.bizCode, props.recordId, payload)
    } else {
      await saveBizInstance(props.bizCode, payload)
    }
    ElMessage.success('保存成功')
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}

function onClosed() {
  buildEmptyModel()
}

</script>
