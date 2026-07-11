<template>
  <el-table :data="rows" border stripe style="width: 100%">
    <el-table-column label="销售单号" prop="saleNo" min-width="140" />
    <el-table-column label="客户/会员" prop="customerName" min-width="140" />
    <el-table-column label="未缴金额" prop="unpaidAmount" width="120">
      <template #default="scope">¥{{ money(scope.row.unpaidAmount) }}</template>
    </el-table-column>
    <el-table-column label="账龄分层" width="110">
      <template #default="scope">
        <el-tag :type="scope.row.ageBucket === 'AGE_30_PLUS' ? 'danger' : 'warning'" size="small">
          {{ ageBucketLabel(scope.row.ageBucket) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="催收状态" width="110">
      <template #default="scope">
        <el-tag :type="statusTag(scope.row.collectionStatus)" size="small">
          {{ statusLabel(scope.row.collectionStatus) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="承诺回款" width="150">
      <template #default="scope">
        <span v-if="scope.row.promisedPayDate">
          {{ parseTime(scope.row.promisedPayDate, '{y}-{m}-{d}') }} / ¥{{ money(scope.row.promisedAmount) }}
        </span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="下次跟进" width="160">
      <template #default="scope">
        <span v-if="scope.row.nextFollowTime">{{ parseTime(scope.row.nextFollowTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="90" fixed="right">
      <template #default="scope">
        <el-button link type="primary" @click="emit('follow', scope.row)">跟进</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { parseTime } from '@/utils/junsong'

defineProps<{ rows: any[] }>()
const emit = defineEmits<{ (e: 'follow', row: any): void }>()

const statusOptions = [
  { label: '未跟进', value: 'PENDING' },
  { label: '已联系', value: 'CONTACTED' },
  { label: '承诺付款', value: 'PROMISED' },
  { label: '部分回款', value: 'PARTIAL_PAID' },
  { label: '已回款', value: 'PAID' },
  { label: '无法联系', value: 'UNREACHABLE' },
  { label: '争议中', value: 'DISPUTED' },
]

function money(value: any) {
  return Number(value || 0).toFixed(2)
}

function statusLabel(value: string) {
  return statusOptions.find(item => item.value === value)?.label || value || '-'
}

function statusTag(value: string) {
  if (value === 'PAID') return 'success'
  if (value === 'PROMISED') return 'warning'
  if (value === 'UNREACHABLE' || value === 'DISPUTED') return 'danger'
  return 'info'
}

function ageBucketLabel(value: string) {
  const labels: Record<string, string> = {
    AGE_0_7: '0-7天',
    AGE_8_14: '8-14天',
    AGE_15_30: '15-30天',
    AGE_30_PLUS: '30天以上',
  }
  return labels[value] || value || '-'
}
</script>
