<template>
  <div class="app-container segment-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">会员分层清单</h2>
        <p>按分层类型查看会员清单，手机号已脱敏，仅显示授权门店会员。</p>
      </div>
      <el-button icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <div class="filter-panel">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="分层">
          <el-select v-model="query.segmentType" placeholder="选择分层" @change="loadData" style="width: 160px">
            <el-option v-for="opt in segmentOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="门店">
          <el-select v-model="query.deptId" placeholder="全部门店" clearable @change="loadData" style="width: 180px">
            <el-option v-for="dept in depts" :key="dept.id" :label="dept.label" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="query.beginTime" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" @change="loadData" style="width: 160px" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="query.endTime" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" @change="loadData" style="width: 160px" />
        </el-form-item>
      </el-form>
    </div>

    <el-alert v-if="loadError" :title="loadError" type="error" show-icon closable style="margin-bottom: 14px" @close="loadError = ''" />

    <el-table :data="rows" stripe style="width: 100%" v-loading="loading" empty-text="暂无会员数据">
      <el-table-column type="index" label="#" width="56" />
      <el-table-column prop="memberNo" label="会员编号" min-width="130" show-overflow-tooltip />
      <el-table-column prop="memberName" label="会员姓名" min-width="100" show-overflow-tooltip />
      <el-table-column prop="maskedPhone" label="手机号" min-width="130" />
      <el-table-column prop="deptName" label="门店" min-width="120" show-overflow-tooltip />
      <el-table-column label="累计消费" min-width="110">
        <template #default="{ row }">{{ money(row.totalSalesAmount) }}</template>
      </el-table-column>
      <el-table-column prop="orderCount" label="订单数" width="80" />
      <el-table-column prop="availablePoints" label="可用积分" width="100" />
      <el-table-column prop="suggestedAction" label="建议动作" min-width="150" show-overflow-tooltip />
    </el-table>

    <div class="pagination-bar" v-if="total > 0">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 30, 50, 100, 200]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getSegmentList, type MemberSegmentQuery } from '@/api/member/segment'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')
const rows = ref<any[]>([])
const total = ref(0)

const query = reactive<MemberSegmentQuery>({
  segmentType: '',
  deptId: undefined,
  beginTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 20,
})

const segmentOptions = [
  { value: 'NEW', label: '新会员（30天内注册）' },
  { value: 'ACTIVE', label: '活跃会员（30天内消费）' },
  { value: 'SILENT', label: '沉默会员（30天无消费）' },
  { value: 'HIGH_VALUE', label: '高价值会员（累计≥1000元）' },
  { value: 'LOW_POINTS', label: '低积分会员（<100积分）' },
  { value: 'HIGH_POINTS', label: '高积分会员（>1000积分）' },
]

const depts = computed(() =>
  (userStore.depts || []).map((dept: any) => ({
    id: dept.deptId,
    label: dept.deptName,
  }))
)

function money(value: any) {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return '¥0.00'
  return `¥${num.toFixed(2)}`
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getSegmentList(query)
    const data = res.data || {}
    rows.value = data.rows || []
    total.value = data.total || 0
  } catch (e: any) {
    loadError.value = e?.message || '加载分层清单失败，请稍后重试'
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.segmentType,
  (val) => {
    if (val && typeof val === 'string') {
      query.segmentType = val
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.segment-page {
  min-height: calc(100vh - 84px);
  background: #f5f7fb;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  .page-title {
    margin: 0 0 6px;
    color: #18202f;
    font-size: 22px;
    font-weight: 700;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 13px;
  }
}

.filter-panel {
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
