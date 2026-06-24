<template>
  <el-drawer v-model="visible" :title="drawerTitle" size="560px" destroy-on-close>
    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="record">
      <el-descriptions :column="1" border class="lc-detail-head">
        <el-descriptions-item label="单据编号">{{ record.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程状态">
          <el-tag :type="statusMeta(record.workflowStatus).type as any">
            {{ statusMeta(record.workflowStatus).label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前节点">{{ record.currentTaskName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交人">{{ record.submitter || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(record.updateTime || record.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">单据内容</el-divider>

      <el-descriptions :column="1" border>
        <el-descriptions-item v-for="field in detailFields" :key="field.fieldKey" :label="field.fieldLabel">
          <FieldRenderer :field="field" :model-value="formData[field.fieldKey]" readonly />
        </el-descriptions-item>
      </el-descriptions>

      <!-- 履约阶段：可补录材料并完成履约 -->
      <template v-if="showFulfillForm">
        <el-divider content-position="left">履约补录</el-divider>
        <el-form ref="fulfillRef" :model="fulfillModel" :rules="fulfillRules" label-width="120px">
          <el-form-item
            v-for="field in fulfillFields"
            :key="field.fieldKey"
            :label="field.fieldLabel"
            :prop="field.fieldKey"
          >
            <FieldRenderer v-model="fulfillModel[field.fieldKey]" :field="field" />
          </el-form-item>
        </el-form>
        <div class="lc-detail-actions">
          <el-button type="primary" :loading="fulfilling" @click="handleFulfill">提交履约</el-button>
        </div>
      </template>
    </template>

    <el-empty v-else description="未找到单据" />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import FieldRenderer from './fields/FieldRenderer.vue'
import {
  fulfillBizInstance,
  getBizInstance,
  listBizFields,
  type LcBizField,
  type LcBizInstance,
} from '@/api/lowcode'
import { buildFieldRules, defaultValueFor, lcCanFulfill, lcFormatDateTime, lcStatusMeta } from './schema'

const props = defineProps<{
  bizCode: string
  modelValue: boolean
  recordId?: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'fulfilled'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const fulfilling = ref(false)
const record = ref<LcBizInstance | null>(null)
const allFields = ref<LcBizField[]>([])
const formData = reactive<Record<string, any>>({})
const fulfillModel = reactive<Record<string, any>>({})
const fulfillRef = ref<FormInstance>()

const statusMeta = lcStatusMeta
const formatDateTime = lcFormatDateTime

const drawerTitle = computed(() => `单据详情${record.value?.orderNo ? ' · ' + record.value.orderNo : ''}`)

const detailFields = computed(() =>
  allFields.value
    .filter((f) => f.isDetail === '1' || f.isDetail === undefined || f.isDetail === null || f.isDetail === '')
    .slice()
    .sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0)),
)

const fulfillFields = computed(() =>
  allFields.value
    .filter((f) => (f.stage || 'APPLY') === 'FULFILLMENT')
    .slice()
    .sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0)),
)

const showFulfillForm = computed(() => lcCanFulfill(record.value?.workflowStatus) && fulfillFields.value.length > 0)

const fulfillRules = computed<FormRules>(() => {
  const result: FormRules = {}
  fulfillFields.value.forEach((field) => {
    const fieldRules = buildFieldRules(field)
    if (fieldRules.length) result[field.fieldKey] = fieldRules
  })
  return result
})

async function loadDetail() {
  if (!props.recordId) return
  loading.value = true
  try {
    const [fieldsRes, detailRes]: any[] = await Promise.all([
      listBizFields(props.bizCode),
      getBizInstance(props.bizCode, props.recordId),
    ])
    allFields.value = fieldsRes.data || fieldsRes.rows || []
    record.value = detailRes.data || null
    Object.keys(formData).forEach((key) => delete formData[key])
    const parsed = record.value?.formData ? JSON.parse(record.value.formData) : {}
    Object.assign(formData, parsed)
    Object.keys(fulfillModel).forEach((key) => delete fulfillModel[key])
    fulfillFields.value.forEach((field) => {
      fulfillModel[field.fieldKey] = formData[field.fieldKey] ?? defaultValueFor(field)
    })
  } finally {
    loading.value = false
  }
}

async function handleFulfill() {
  if (!props.recordId || !fulfillRef.value) return
  await fulfillRef.value.validate()
  fulfilling.value = true
  try {
    await fulfillBizInstance(props.bizCode, props.recordId, JSON.parse(JSON.stringify(fulfillModel)))
    ElMessage.success('履约已提交')
    visible.value = false
    emit('fulfilled')
  } finally {
    fulfilling.value = false
  }
}

watch(
  () => [visible.value, props.recordId],
  ([show]) => {
    if (show) loadDetail()
  },
)
</script>

<style scoped>
.lc-detail-head {
  margin-bottom: 8px;
}
.lc-detail-actions {
  margin-top: 16px;
  text-align: right;
}
</style>
