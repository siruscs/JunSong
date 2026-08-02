<template>
  <el-form-item label="核算周期">
    <el-select :model-value="modelValue" placeholder="全部周期" clearable filterable class="period-filter" @update:model-value="onChange">
      <el-option v-for="period in periods" :key="period.periodId" :label="formatPeriod(period)" :value="period.periodId" />
    </el-select>
  </el-form-item>
</template>

<script>
import { listAccountingPeriod } from '@/api/finance/accountingPeriod'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: 'AccountingPeriodFilter',
  props: { modelValue: { type: [Number, String], default: null } },
  emits: ['update:modelValue'],
  data() { return { periods: [], deptNames: new Map(), authorizedDeptIds: [] } },
  mounted() { this.loadPeriods() },
  methods: {
    loadPeriods() {
      const depts = (userStore.depts || []).filter(dept => dept.deptId != null)
      this.authorizedDeptIds = depts.map(dept => String(dept.deptId))
      this.deptNames = new Map(depts.map(dept => [String(dept.deptId), dept.deptName]))
      if (this.authorizedDeptIds.length === 0) { this.periods = []; return }
      listAccountingPeriod({ pageNum: 1, pageSize: 200, status: undefined, deptIds: this.authorizedDeptIds }).then(res => {
        const rows = res.rows || res.data?.rows || res.data || []
        this.periods = (Array.isArray(rows) ? rows : []).filter(period => this.authorizedDeptIds.includes(String(period.deptId)))
      }).catch(() => { this.periods = [] })
    },
    onChange(value) { this.$emit('update:modelValue', value || null) },
    formatPeriod(period) {
      const status = period.status === '2' ? '已结转' : '进行中'
      const deptName = this.deptNames.get(String(period.deptId)) || `机构${period.deptId || ''}`
      return `${deptName} · ${period.periodNo || '周期'} · ${period.startTime || ''} 至 ${period.endTime || '当前'} · ${status}`
    }
  }
}
</script>

<style scoped>
.period-filter { width: 280px; }
</style>
