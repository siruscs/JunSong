<template>
  <div class="subform-renderer">
    <div v-for="(row, index) in rows" :key="index" class="subform-row">
      <el-card shadow="never" class="subform-card">
        <template #header>
          <div class="subform-header">
            <span>第 {{ index + 1 }} 行</span>
            <el-button
              v-if="!readonly"
              type="danger"
              link
              size="small"
              :icon="Delete"
              @click="removeRow(index)"
            >
              删除
            </el-button>
          </div>
        </template>
        <el-row :gutter="12">
          <el-col
            v-for="subField in subFields"
            :key="subField.fieldKey"
            :span="subFieldSpan(subField)"
          >
            <el-form-item
              :label="subField.fieldLabel"
              :prop="`rows.${index}.${subField.fieldKey}`"
              :rules="buildRules(subField)"
              :required="subField.required === '1'"
              class="subform-field-item"
            >
              <FieldRenderer
                v-model="row[subField.fieldKey]"
                :field="subField"
                :readonly="readonly"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>
    </div>
    <el-row v-if="rows.length" :gutter="12" class="subform-summary">
      <el-col
        v-for="subField in subFields"
        :key="subField.fieldKey"
        :span="subFieldSpan(subField)"
      >
        <div v-if="isNumericField(subField)" class="summary-item">
          <span class="summary-label">{{ subField.fieldLabel }}合计</span>
          <span class="summary-value">{{ formatTotal(fieldTotal(subField)) }}</span>
        </div>
      </el-col>
    </el-row>
    <el-button
      v-if="!readonly"
      type="primary"
      plain
      :icon="Plus"
      class="subform-add-btn"
      @click="addRow"
    >
      添加一行
    </el-button>
    <el-empty v-if="rows.length === 0" description="暂无数据，点击添加" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import FieldRenderer from './FieldRenderer.vue'

const props = defineProps<{
  modelValue?: any[]
  subFields?: any[]
  readonly?: boolean
}>()

const emit = defineEmits<{ (e: 'update:modelValue', val: any[]): void }>()

const rows = computed({
  get: () => props.modelValue || [],
  set: (val) => emit('update:modelValue', val),
})

const subFields = computed(() => props.subFields || [])

function addRow() {
  const newRow: Record<string, any> = {}
  subFields.value.forEach((f: any) => {
    newRow[f.fieldKey] = f.fieldType === 'number' || f.fieldType === 'decimal' ? null : ''
  })
  rows.value = [...rows.value, newRow]
}

function removeRow(index: number) {
  const newRows = [...rows.value]
  newRows.splice(index, 1)
  rows.value = newRows
}

function subFieldSpan(field: any) {
  const ext = field.fieldExt ? (typeof field.fieldExt === 'string' ? JSON.parse(field.fieldExt) : field.fieldExt) : {}
  return ext.span || 12
}

function isNumericField(field: any) {
  return field.fieldType === 'number' || field.fieldType === 'decimal'
}

function buildRules(field: any) {
  const rules: any[] = []
  if (field.required === '1') {
    rules.push({ required: true, message: `请输入${field.fieldLabel}`, trigger: 'blur' })
  }
  return rules
}

function fieldTotal(field: any): number {
  if (!isNumericField(field)) return 0
  return rows.value.reduce((sum, row) => {
    const val = Number(row[field.fieldKey])
    return sum + (isNaN(val) ? 0 : val)
  }, 0)
}

function formatTotal(val: number): string {
  if (val === 0) return '0'
  return Number(val.toFixed(2)).toString()
}
</script>

<style scoped>
.subform-renderer {
  width: 100%;
}

.subform-row {
  margin-bottom: 12px;
}

.subform-card {
  border: 1px solid var(--el-border-color-lighter);
}

.subform-card :deep(.el-card__header) {
  padding: 8px 12px;
}

.subform-card :deep(.el-card__body) {
  padding: 12px;
}

.subform-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
}

.subform-field-item {
  margin-bottom: 8px;
}

.subform-add-btn {
  margin-top: 8px;
}

.subform-summary {
  margin-top: 8px;
  padding: 10px 12px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 4px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  line-height: 24px;
}

.summary-label {
  color: var(--el-text-color-secondary);
}

.summary-value {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
