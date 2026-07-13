<template>
  <el-drawer
    :model-value="visible"
    :title="drawerTitle"
    size="60%"
    @update:model-value="(val) => $emit('update:visible', val)"
  >
    <div v-loading="loading" class="ledger-drawer-body">
      <el-table :data="ledgerRows" stripe border style="width: 100%" empty-text="暂无流水记录">
        <el-table-column label="变动时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="变动类型" width="110">
          <template #default="{ row }">
            <el-tag :type="changeTypeTag(row.changeType)" size="small">
              {{ changeTypeLabel(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="beforeQuantity" label="变动前数量" width="110" align="right" />
        <el-table-column label="变动数量" width="100" align="right">
          <template #default="{ row }">
            <span :class="row.changeQuantity >= 0 ? 'qty-in' : 'qty-out'">
              {{ row.changeQuantity >= 0 ? '+' : '' }}{{ row.changeQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="afterQuantity" label="变动后数量" width="110" align="right" />
        <el-table-column label="来源类型" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ referenceTypeLabel(row.referenceType) }}</template>
        </el-table-column>
        <el-table-column prop="referenceNo" label="来源单号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createBy" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      </el-table>

      <div class="ledger-pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchLedger"
          @current-change="fetchLedger"
        />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { parseTime } from '@/utils/junsong'
import { getStockLedgerPage, type StockLedgerRow } from '@/api/finance/stockreport'

const props = defineProps<{
  visible: boolean
  deptId: number
  productId: number
  startDate?: string
  endDate?: string
  productName?: string
}>()

defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

const loading = ref(false)
const ledgerRows = ref<StockLedgerRow[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const drawerTitle = computed(() => `库存流水明细${props.productName ? ' - ' + props.productName : ''}`)

const changeTypeLabels: Record<string, string> = {
  PURCHASE_IN: '采购入库',
  PURCHASE_REVERSE: '采购冲销',
  SALE_OUT: '销售出库',
  SALE_REVERSE: '销售冲销',
}

function changeTypeLabel(type: string) {
  return changeTypeLabels[type] || type || '-'
}

function changeTypeTag(type: string): 'success' | 'warning' | 'info' {
  if (type === 'PURCHASE_IN') return 'success'
  if (type === 'SALE_OUT') return 'warning'
  if (type === 'PURCHASE_REVERSE' || type === 'SALE_REVERSE') return 'info'
  return 'info'
}

const referenceTypeLabels: Record<string, string> = {
  PURCHASE: '采购单',
  SALE: '销售单',
}

function referenceTypeLabel(type: string) {
  return referenceTypeLabels[type] || type || '-'
}

function formatDateTime(val: string | undefined | null): string {
  if (!val) return '-'
  return parseTime(val, '{y}-{m}-{d} {h}:{i}:{s}') || val.replace('T', ' ')
}

function fetchLedger() {
  if (!props.deptId || !props.productId) {
    ledgerRows.value = []
    total.value = 0
    return
  }
  loading.value = true
  getStockLedgerPage({
    deptId: props.deptId,
    productId: props.productId,
    startDate: props.startDate,
    endDate: props.endDate,
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  })
    .then((res: any) => {
      const data = res.data || {}
      ledgerRows.value = data.rows || []
      total.value = data.total || 0
    })
    .catch(() => {
      // fail closed: clear old data
      ledgerRows.value = []
      total.value = 0
      ElMessage.error('库存流水查询失败，请稍后重试')
    })
    .finally(() => {
      loading.value = false
    })
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      pageNum.value = 1
      fetchLedger()
    }
  },
)
</script>

<style scoped>
.ledger-drawer-body {
  padding: 0 4px;
}

.qty-in {
  color: #67c23a;
  font-weight: 600;
}

.qty-out {
  color: #f56c6c;
  font-weight: 600;
}

.ledger-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
