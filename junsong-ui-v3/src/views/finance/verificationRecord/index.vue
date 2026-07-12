<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="批次号" prop="batchNo">
        <el-input v-model="queryParams.batchNo" placeholder="请输入批次号" clearable style="width: 180px;" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px;">
          <el-option label="已核销" value="VERIFIED" />
          <el-option label="已反核销" value="REVERSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="批次类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="请选择类型" clearable style="width: 160px;">
          <el-option label="正常" value="NORMAL" />
          <el-option label="历史" value="LEGACY" />
        </el-select>
      </el-form-item>
      <el-form-item label="核销人" prop="verifyBy">
        <el-input v-model="queryParams.verifyBy" placeholder="请输入核销人" clearable style="width: 160px;" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="核销时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px;"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button plain :icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="batchList" border>
      <el-table-column label="批次号" prop="batchNo" min-width="160" />
      <el-table-column label="核销时间" prop="verifyTime" width="160">
        <template #default="scope">
          {{ parseTime(scope.row.verifyTime) }}
        </template>
      </el-table-column>
      <el-table-column label="核销人" prop="verifyBy" width="100" />
      <el-table-column label="费用合计" prop="totalExpenseAmount" width="120" align="right">
        <template #default="scope">
          <span class="amount expense-amount">¥{{ scope.row.totalExpenseAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="借支合计" prop="totalAdvanceAmount" width="120" align="right">
        <template #default="scope">
          <span class="amount">¥{{ scope.row.totalAdvanceAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="差额" prop="differenceAmount" width="100" align="right">
        <template #default="scope">
          <span :class="['amount', scope.row.differenceAmount > 0 ? 'positive' : scope.row.differenceAmount < 0 ? 'negative' : '']">
            ¥{{ scope.row.differenceAmount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'VERIFIED' ? 'success' : 'info'">
            {{ scope.row.status === 'VERIFIED' ? '已核销' : '已反核销' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="sourceType" width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.sourceType === 'NORMAL' ? '' : 'warning'" size="small">
            {{ scope.row.sourceType === 'NORMAL' ? '正常' : '历史' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="反核销时间" prop="reverseTime" width="160">
        <template #default="scope">
          {{ scope.row.reverseTime ? parseTime(scope.row.reverseTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="scope">
          <el-button size="small" type="primary" link @click="handleDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 批次详情抽屉 -->
    <el-drawer v-model="detailOpen" :title="'核销批次详情 - ' + (detail.batch ? detail.batch.batchNo : '')" size="70%">
      <div v-loading="detailLoading" style="padding: 0 16px 16px;">
        <!-- 批次基本信息 -->
        <el-card class="mb8" shadow="never" v-if="detail.batch">
          <template #header><span>批次信息</span></template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="批次号">{{ detail.batch.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="detail.batch.status === 'VERIFIED' ? 'success' : 'info'" size="small">
                {{ detail.batch.status === 'VERIFIED' ? '已核销' : '已反核销' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="类型">
              <el-tag :type="detail.batch.sourceType === 'NORMAL' ? '' : 'warning'" size="small">
                {{ detail.batch.sourceType === 'NORMAL' ? '正常' : '历史' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="核销人">{{ detail.batch.verifyBy }}</el-descriptions-item>
            <el-descriptions-item label="核销时间">{{ parseTime(detail.batch.verifyTime) }}</el-descriptions-item>
            <el-descriptions-item label="差额">¥{{ detail.batch.differenceAmount }}</el-descriptions-item>
            <el-descriptions-item label="费用合计">
              <span class="amount expense-amount">¥{{ detail.batch.totalExpenseAmount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="借支合计">¥{{ detail.batch.totalAdvanceAmount }}</el-descriptions-item>
            <el-descriptions-item label="批次ID">{{ detail.batch.batchId }}</el-descriptions-item>
          </el-descriptions>
          <el-descriptions :column="1" border v-if="detail.batch.status === 'REVERSED'" style="margin-top: 12px;">
            <el-descriptions-item label="反核销人">{{ detail.batch.reverseBy }}</el-descriptions-item>
            <el-descriptions-item label="反核销时间">{{ parseTime(detail.batch.reverseTime) }}</el-descriptions-item>
            <el-descriptions-item label="反核销原因">{{ detail.batch.reverseReason }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 费用明细 -->
        <el-card class="mb8" shadow="never">
          <template #header><span>费用明细（{{ detail.expenseDetails ? detail.expenseDetails.length : 0 }}笔）</span></template>
          <el-table :data="detail.expenseDetails" border size="small">
            <el-table-column label="费用单号" prop="expenseNo" min-width="140" />
            <el-table-column label="费用类别" prop="expenseType" width="100">
              <template #default="scope">
                {{ selectDictLabel(dict.type.finance_expense_type, scope.row.expenseType) || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="费用日期" prop="expenseDate" width="120" />
            <el-table-column label="费用内容" prop="expenseContent" min-width="120" show-overflow-tooltip />
            <el-table-column label="金额" prop="expenseAmount" width="120" align="right">
              <template #default="scope">
                <span class="amount expense-amount">¥{{ scope.row.expenseAmount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="费用ID" prop="expenseId" width="80" />
          </el-table>
        </el-card>

        <!-- 借支明细 -->
        <el-card shadow="never">
          <template #header><span>借支明细（{{ detail.advanceDetails ? detail.advanceDetails.length : 0 }}笔）</span></template>
          <el-table :data="detail.advanceDetails" border size="small">
            <el-table-column label="借支单号" prop="advanceNo" min-width="140" />
            <el-table-column label="关系" prop="relationType" width="90" align="center">
              <template #default="scope">
                <el-tag :type="relationTagType(scope.row.relationType)" size="small">
                  {{ relationLabel(scope.row.relationType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="生成" prop="generatedFlag" width="60" align="center">
              <template #default="scope">
                <el-tag v-if="scope.row.generatedFlag === '1'" type="warning" size="small">是</el-tag>
                <span v-else>否</span>
              </template>
            </el-table-column>
            <el-table-column label="借支日期" prop="advanceDate" width="120" />
            <el-table-column label="用途" prop="purpose" min-width="120" show-overflow-tooltip />
            <el-table-column label="金额" prop="advanceAmount" width="120" align="right">
              <template #default="scope">
                <span class="amount">¥{{ scope.row.advanceAmount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="借支ID" prop="advanceId" width="80" />
          </el-table>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { useDict } from '@/composables/useDict'
import { parseTime, selectDictLabel } from '@/utils/junsong'
import { listVerificationBatches, getVerificationBatchDetail } from '@/api/finance/expense'
import { Search, Refresh } from '@element-plus/icons-vue'

const dict = useDict('finance_expense_type')

export default {
  name: "VerificationRecord",
  data() {
    return {
      dict,
      Search,
      Refresh,
      loading: true,
      showSearch: true,
      total: 0,
      batchList: [],
      dateRange: [],
      detailOpen: false,
      detailLoading: false,
      detail: {
        batch: null,
        expenseDetails: [],
        advanceDetails: []
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        batchNo: undefined,
        status: undefined,
        sourceType: undefined,
        verifyBy: undefined,
        params: {}
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    parseTime,
    selectDictLabel,
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        params.params = params.params || {}
        params.params.beginTime = this.dateRange[0]
        params.params.endTime = this.dateRange[1]
      }
      listVerificationBatches(params).then(response => {
        this.batchList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        batchNo: undefined,
        status: undefined,
        sourceType: undefined,
        verifyBy: undefined,
        params: {}
      }
      this.handleQuery()
    },
    handleDetail(row) {
      this.detailOpen = true
      this.detailLoading = true
      this.detail = { batch: null, expenseDetails: [], advanceDetails: [] }
      getVerificationBatchDetail(row.batchId).then(response => {
        this.detail = response.data
        this.detailLoading = false
      }).catch(() => {
        this.detailLoading = false
      })
    },
    relationLabel(type) {
      const map = { SOURCE: '原始借支', SUPPLEMENT: '补款', SURPLUS: '节余' }
      return map[type] || type
    },
    relationTagType(type) {
      const map = { SOURCE: '', SUPPLEMENT: 'warning', SURPLUS: 'success' }
      return map[type] || ''
    }
  }
}
</script>

<style scoped>
.amount {
  font-variant-numeric: tabular-nums;
}
.expense-amount {
  color: #e6a23c;
  font-weight: 500;
}
.positive {
  color: #f56c6c;
}
.negative {
  color: #67c23a;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
