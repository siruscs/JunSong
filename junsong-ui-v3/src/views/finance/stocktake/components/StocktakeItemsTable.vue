<template>
  <el-table :data="items" border stripe style="width: 100%">
    <el-table-column label="商品名称" prop="productName" min-width="160" show-overflow-tooltip fixed="left" />
    <el-table-column v-if="!hideExpected" label="期望数量" width="110" align="right">
      <template #default="scope">{{ formatNum(scope.row.expectedQuantity) }}</template>
    </el-table-column>
    <el-table-column label="冻结后移动" prop="movementQuantityAfterFreeze" width="120" align="right">
      <template #default="scope">{{ formatNum(scope.row.movementQuantityAfterFreeze) }}</template>
    </el-table-column>
    <el-table-column label="实际数量" width="160" align="center">
      <template #default="scope">
        <el-input-number
          v-if="status === 'COUNTING'"
          v-model="editMap[scope.row.itemId].actualQuantity"
          :min="0"
          :precision="3"
          :step="1"
          size="small"
          controls-position="right"
          style="width: 140px"
        />
        <span v-else>{{ formatNum(scope.row.actualQuantity) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="复盘数量" width="160" align="center">
      <template #default="scope">
        <el-input-number
          v-if="status === 'RECOUNTING'"
          v-model="editMap[scope.row.itemId].recountQuantity"
          :min="0"
          :precision="3"
          :step="1"
          size="small"
          controls-position="right"
          style="width: 140px"
        />
        <span v-else>{{ formatNum(scope.row.recountQuantity) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="最终数量" width="110" align="right">
      <template #default="scope">{{ formatNum(scope.row.finalQuantity) }}</template>
    </el-table-column>
    <el-table-column v-if="!hideExpected" label="方差数量" width="110" align="right">
      <template #default="scope">
        <span :class="varianceClass(scope.row.varianceQuantity)">{{ formatNum(scope.row.varianceQuantity) }}</span>
      </template>
    </el-table-column>
    <el-table-column v-if="!hideExpected" label="单位成本" width="110" align="right">
      <template #default="scope">{{ formatMoney(scope.row.unitCost) }}</template>
    </el-table-column>
    <el-table-column v-if="!hideExpected" label="方差金额" width="120" align="right">
      <template #default="scope">
        <span :class="varianceClass(scope.row.varianceAmount)">{{ formatMoney(scope.row.varianceAmount) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="原因代码" width="160">
      <template #default="scope">
        <el-select
          v-if="canEditReason(scope.row)"
          v-model="editMap[scope.row.itemId].reasonCode"
          placeholder="选择原因"
          size="small"
          clearable
          style="width: 130px"
        >
          <el-option v-for="opt in reasonOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <span v-else>{{ reasonLabel(scope.row.reasonCode) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="原因说明" min-width="160">
      <template #default="scope">
        <el-input
          v-if="canEditReason(scope.row)"
          v-model="editMap[scope.row.itemId].reason"
          size="small"
          placeholder="原因说明"
          maxlength="200"
        />
        <span v-else>{{ scope.row.reason || '-' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="附件" min-width="140">
      <template #default="scope">
        <template v-if="scope.row.attachments && scope.row.attachments.length > 0">
          <el-link
            v-for="(att, idx) in scope.row.attachments"
            :key="idx"
            type="primary"
            :href="att.url"
            target="_blank"
            style="margin-right: 8px"
          >
            {{ att.name || '附件' + (idx + 1) }}
          </el-link>
        </template>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column
      v-if="status === 'COUNTING' || status === 'RECOUNTING'"
      label="操作"
      width="90"
      fixed="right"
      align="center"
    >
      <template #default="scope">
        <el-button
          v-if="status === 'COUNTING'"
          type="primary"
          link
          size="small"
          :loading="!!savingMap[scope.row.itemId]"
          @click="handleSaveCount(scope.row)"
        >保存</el-button>
        <el-button
          v-if="status === 'RECOUNTING'"
          type="primary"
          link
          size="small"
          :loading="!!savingMap[scope.row.itemId]"
          @click="handleSaveRecount(scope.row)"
        >保存</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  StocktakeCountRequest,
  StocktakeItemVO,
  StocktakeRecountRequest,
} from '@/api/finance/stocktake'

interface EditState {
  actualQuantity: number | null
  recountQuantity: number | null
  reasonCode: string | null
  reason: string | null
  version: number
}

const props = defineProps<{
  items: StocktakeItemVO[]
  hideExpected: boolean
  status: string
  stocktakeId: number
}>()

const emit = defineEmits<{
  (e: 'count', payload: { itemId: number; request: StocktakeCountRequest }): void
  (e: 'recount', payload: { itemId: number; request: StocktakeRecountRequest }): void
}>()

const reasonOptions = [
  { label: '过期', value: 'EXPIRED' },
  { label: '破损', value: 'DAMAGED' },
  { label: '盗窃', value: 'THEFT' },
  { label: '称重损耗', value: 'WEIGHING' },
  { label: '操作损耗', value: 'OPERATION' },
  { label: '漏记交易', value: 'MISSING_TRANSACTION' },
  { label: '其他', value: 'OTHER' },
]

const editMap = reactive<Record<number, EditState>>({})
const savingMap = reactive<Record<number, boolean>>({})

watch(
  () => props.items,
  (list) => {
    list.forEach((item) => {
      const existing = editMap[item.itemId]
      if (!existing || existing.version !== item.version) {
        editMap[item.itemId] = {
          actualQuantity: item.actualQuantity ?? null,
          recountQuantity: item.recountQuantity ?? null,
          reasonCode: item.reasonCode ?? null,
          reason: item.reason ?? null,
          version: item.version,
        }
      }
    })
  },
  { immediate: true, deep: true }
)

function canEditReason(_row: StocktakeItemVO) {
  return props.status === 'COUNTING' || props.status === 'RECOUNTING'
}

function formatNum(value: number | null | undefined) {
  if (value === null || value === undefined) return '-'
  return Number(value).toFixed(3)
}

function formatMoney(value: number | null | undefined) {
  if (value === null || value === undefined) return '-'
  return '¥' + Number(value).toFixed(2)
}

function varianceClass(value: number | null | undefined) {
  if (value === null || value === undefined || Number(value) === 0) return ''
  return Number(value) > 0 ? 'text-success' : 'text-danger'
}

function reasonLabel(value: string | null | undefined) {
  if (!value) return '-'
  return reasonOptions.find((o) => o.value === value)?.label || value
}

function buildIdempotencyKey(productId: number, version: number, action: 'count' | 'recount') {
  return `${props.stocktakeId}-${productId}-${action}-${version}`
}

function validateReasonRequired(row: StocktakeItemVO, reasonCode: string | null): boolean {
  // 盲盘时不强制（方差不可见，由服务端校验）
  if (props.hideExpected) return true
  const variance = Number(row.varianceQuantity ?? 0)
  if (variance !== 0 && !reasonCode) {
    return false
  }
  return true
}

function handleSaveCount(row: StocktakeItemVO) {
  const state = editMap[row.itemId]
  if (!state || state.actualQuantity === null || state.actualQuantity === undefined) {
    ElMessage.warning('请输入实际数量')
    return
  }
  if (!validateReasonRequired(row, state.reasonCode)) {
    ElMessage.warning('方差不为 0 时请选择原因代码')
    return
  }
  savingMap[row.itemId] = true
  const request: StocktakeCountRequest = {
    actualQuantity: state.actualQuantity,
    reasonCode: state.reasonCode || undefined,
    reason: state.reason || undefined,
    idempotencyKey: buildIdempotencyKey(row.productId, row.version, 'count'),
    version: row.version,
  }
  emit('count', { itemId: row.itemId, request })
}

function handleSaveRecount(row: StocktakeItemVO) {
  const state = editMap[row.itemId]
  if (!state || state.recountQuantity === null || state.recountQuantity === undefined) {
    ElMessage.warning('请输入复盘数量')
    return
  }
  savingMap[row.itemId] = true
  const request: StocktakeRecountRequest = {
    recountQuantity: state.recountQuantity,
    reason: state.reason || undefined,
    idempotencyKey: buildIdempotencyKey(row.productId, row.version, 'recount'),
    version: row.version,
  }
  emit('recount', { itemId: row.itemId, request })
}

function clearSaving(itemId: number) {
  savingMap[itemId] = false
}

defineExpose({ clearSaving })
</script>

<style scoped>
.text-success {
  color: #67c23a;
  font-weight: 600;
}
.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
