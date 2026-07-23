<template>
  <div class="app-container report-page">
    <!-- 查询栏 -->
    <div class="report-filter-panel">
      <el-form :model="queryParams" label-position="top" class="report-query-form">
        <el-form-item label="门店">
          <el-select
            v-model="queryParams.deptIds"
            placeholder="请选择门店"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            class="report-query-control"
          >
            <el-option v-for="dept in depts" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="report-query-control"
          />
        </el-form-item>
        <el-form-item label="商品">
          <el-input
            v-model="queryParams.keyword"
            placeholder="商品编码/名称"
            clearable
            class="report-query-control"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="report-query-control">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作" class="report-query-actions">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button
            type="warning"
            plain
            icon="Download"
            :loading="exportLoading"
            @click="handleExport"
            v-hasPermi="['finance:report:stock:export']"
          >
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 报表模式切换 -->
    <el-tabs v-model="reportMode" class="report-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="数量报表" name="quantity">
        <!-- 汇总卡片 -->
        <el-row :gutter="12" class="summary-row">
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card primary">
              <div class="metric-label">期初</div>
              <div class="metric-value">{{ formatQty(summary?.openingQuantity) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card success">
              <div class="metric-label">采购净入库</div>
              <div class="metric-value">{{ formatQty(summary?.purchaseNetInQuantity) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card warning">
              <div class="metric-label">销售净出库</div>
              <div class="metric-value">{{ formatQty(summary?.saleNetOutQuantity) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card primary">
              <div class="metric-label">期末</div>
              <div class="metric-value">{{ formatQty(summary?.closingQuantity) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card danger">
              <div class="metric-label">负库存数</div>
              <div class="metric-value">{{ formatQty(summary?.negativeStockCount) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card warning">
              <div class="metric-label">低库存数</div>
              <div class="metric-value">{{ formatQty(summary?.lowStockCount) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card warning">
              <div class="metric-label">滞销数</div>
              <div class="metric-value">{{ formatQty(summary?.staleStockCount) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card danger">
              <div class="metric-label">异常数</div>
              <div class="metric-value">{{ formatQty(summary?.anomalyCount) }}</div>
            </div>
          </el-col>
        </el-row>

        <!-- 口径说明 -->
        <el-collapse class="caliber-notes" v-model="caliberActive">
          <el-collapse-item title="口径说明" name="caliber">
            <p class="caliber-text">
              进货赠品计入入库数量，销售赠品计入出库数量，但赠品不计采购/销售金额。采购冲销计入采购净入库负向变动，销售冲销计入销售净出库负向变动。滞销阈值为30天。
            </p>
          </el-collapse-item>
        </el-collapse>

        <!-- 明细表格 -->
        <el-card class="section-card" shadow="never">
          <template #header><span>库存明细</span></template>
          <el-table v-loading="loading" :data="items" stripe border style="width: 100%" empty-text="暂无数据">
            <el-table-column label="门店" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.deptName || deptNameById(row.deptId) }}</template>
            </el-table-column>
            <el-table-column prop="productCode" label="商品编码" min-width="120" show-overflow-tooltip />
            <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column prop="minStock" label="最低库存" width="90" align="right" />
            <el-table-column prop="openingQuantity" label="期初" width="80" align="right" />
            <el-table-column prop="purchaseNetInQuantity" label="采购净入库" width="100" align="right" />
            <el-table-column prop="saleNetOutQuantity" label="销售净出库" width="100" align="right" />
            <el-table-column prop="closingQuantity" label="期末" width="80" align="right" />
            <el-table-column label="最近入库" min-width="150">
              <template #default="{ row }">{{ formatDateTime(row.lastInboundTime) }}</template>
            </el-table-column>
            <el-table-column label="最近出库" min-width="150">
              <template #default="{ row }">{{ formatDateTime(row.lastOutboundTime) }}</template>
            </el-table-column>
            <el-table-column prop="daysWithoutSale" label="无出库天数" width="100" align="right" />
            <el-table-column label="库存状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="stockStatusTag(row.stockStatus)" size="small">
                  {{ stockStatusLabel(row.stockStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="对账状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="reconciliationTag(row.reconciliationStatus)" size="small">
                  {{ reconciliationLabel(row.reconciliationStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openLedger(row)">流水</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="report-pagination">
            <el-pagination
              v-model:current-page="queryParams.pageNum"
              v-model:page-size="queryParams.pageSize"
              :page-sizes="[10, 20, 30, 50, 100, 200]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleQuery"
              @current-change="handleQuery"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="价值报表" name="value">
        <!-- 期间状态指示 -->
        <div class="period-status-bar" v-if="valueReport">
          <el-tag :type="periodStatusTag(valueReport.periodStatus)" size="default" effect="plain">
            会计期间：{{ periodStatusLabel(valueReport.periodStatus) }}
          </el-tag>
          <span v-if="valueReport.periodStatus !== 'ACTIVE'" class="period-lock-hint">
            非ACTIVE期间禁止成本调整回写
          </span>
        </div>

        <!-- costReady 门禁告警 -->
        <el-alert
          v-if="valueReport && !valueReport.costReady"
          title="成本层未初始化"
          type="warning"
          :closable="false"
          show-icon
          class="cost-ready-alert"
        >
          <template #default>
            当前租户尚未初始化库存成本层，金额和毛利暂不可用。请先完成采购入库计价后再查看价值报表。
          </template>
        </el-alert>

        <!-- 价值汇总卡片（仅 costReady=true 时展示金额） -->
        <el-row :gutter="12" class="summary-row" v-if="valueReport && valueReport.costReady">
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card primary">
              <div class="metric-label">期初金额</div>
              <div class="metric-value">{{ formatAmount(valueReport.openingAmount) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card success">
              <div class="metric-label">采购入库金额</div>
              <div class="metric-value">{{ formatAmount(valueReport.inboundAmount) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card warning">
              <div class="metric-label">销售成本</div>
              <div class="metric-value">{{ formatAmount(valueReport.saleCost) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card primary">
              <div class="metric-label">期末金额</div>
              <div class="metric-value">{{ formatAmount(valueReport.closingAmount) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card success">
              <div class="metric-label">销售收入</div>
              <div class="metric-value">{{ formatAmount(valueReport.saleRevenue) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card" :class="grossProfitClass(valueReport.grossProfit)">
              <div class="metric-label">毛利</div>
              <div class="metric-value">{{ formatAmount(valueReport.grossProfit) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card" :class="grossProfitClass(valueReport.grossProfit)">
              <div class="metric-label">毛利率(%)</div>
              <div class="metric-value">{{ formatRate(valueReport.grossProfitRate) }}</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6" :lg="3">
            <div class="metric-card warning">
              <div class="metric-label">成本调整</div>
              <div class="metric-value">{{ formatAmount(valueReport.adjustmentAmount) }}</div>
            </div>
          </el-col>
        </el-row>

        <!-- 价值口径说明 -->
        <el-collapse class="caliber-notes" v-model="caliberActive" v-if="valueReport && valueReport.costReady">
          <el-collapse-item title="价值口径说明" name="caliber">
            <p class="caliber-text">
              移动加权平均法：新平均成本=(入库前库存金额+本次入库金额)/入库后数量。赠品入库计入数量但不计金额，从而摊薄平均成本。销售出库按出库瞬间固化成本，销售冲销按原成本回补。恒等式：期初+入库-销售成本+调整=期末。毛利=销售收入-销售成本。LOCKED/CARRIED_FORWARD期间禁止成本调整回写。
            </p>
          </el-collapse-item>
        </el-collapse>

        <!-- 价值明细表格 -->
        <el-card class="section-card" shadow="never" v-if="valueReport && valueReport.costReady">
          <template #header><span>库存价值明细</span></template>
          <el-table v-loading="loading" :data="valueReport.items" stripe border style="width: 100%" empty-text="暂无数据">
            <el-table-column prop="deptName" label="门店" min-width="120" show-overflow-tooltip />
            <el-table-column prop="productCode" label="商品编码" min-width="120" show-overflow-tooltip />
            <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="closingQuantity" label="期末数量" width="100" align="right" />
            <el-table-column prop="avgUnitCost" label="平均单位成本" width="130" align="right" :formatter="formatCostCell" />
            <el-table-column prop="closingAmount" label="期末金额" width="120" align="right" :formatter="formatAmountCell" />
            <el-table-column prop="inboundAmount" label="入库金额" width="120" align="right" :formatter="formatAmountCell" />
            <el-table-column prop="saleCost" label="销售成本" width="120" align="right" :formatter="formatAmountCell" />
            <el-table-column prop="saleRevenue" label="销售收入" width="120" align="right" :formatter="formatAmountCell" />
            <el-table-column prop="grossProfit" label="毛利" width="120" align="right" :formatter="formatAmountCell" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 流水下钻抽屉 -->
    <StockLedgerDrawer
      v-model:visible="ledgerVisible"
      :dept-id="ledgerDeptId"
      :product-id="ledgerProductId"
      :product-name="ledgerProductName"
      :start-date="queryParams.startDate"
      :end-date="queryParams.endDate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { parseTime } from '@/utils/junsong'
import {
  getStockReport,
  exportStockReport,
  getStockValueReport,
  type StockReportSummary,
  type StockReportItem,
  type StockValueReportVO,
} from '@/api/finance/stockreport'
import { useUserStore } from '@/stores/user'
import StockLedgerDrawer from './components/StockLedgerDrawer.vue'

const userStore = useUserStore()
const depts = ref<any[]>(userStore.depts || [])

const statusOptions = [
  { value: 'NORMAL', label: '正常' },
  { value: 'LOW_STOCK', label: '低库存' },
  { value: 'ZERO_STOCK', label: '零库存' },
  { value: 'NEGATIVE_STOCK', label: '负库存' },
  { value: 'STALE', label: '滞销' },
  { value: 'SNAPSHOT_ANOMALY', label: '快照异常' },
]

const stockStatusLabels: Record<string, string> = {
  NORMAL: '正常',
  LOW_STOCK: '低库存',
  ZERO_STOCK: '零库存',
  NEGATIVE_STOCK: '负库存',
  STALE: '滞销',
  SNAPSHOT_ANOMALY: '快照异常',
}

function stockStatusLabel(status: string) {
  return stockStatusLabels[status] || status || '-'
}

function stockStatusTag(status: string): 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'NORMAL') return 'success'
  if (status === 'LOW_STOCK') return 'warning'
  if (status === 'ZERO_STOCK') return 'info'
  if (status === 'NEGATIVE_STOCK') return 'danger'
  if (status === 'STALE') return 'warning'
  if (status === 'SNAPSHOT_ANOMALY') return 'danger'
  return 'info'
}

function reconciliationLabel(status: string) {
  if (status === 'OK') return '正常'
  if (status === 'ANOMALY') return '异常'
  return status || '-'
}

function reconciliationTag(status: string): 'success' | 'danger' | 'info' {
  if (status === 'OK') return 'success'
  if (status === 'ANOMALY') return 'danger'
  return 'info'
}

function defaultDateRange(): string[] {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 30)
  return [formatDate(start), formatDate(end)]
}

function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const dateRange = ref<string[]>(defaultDateRange())
const caliberActive = ref<string[]>([])

const queryParams = reactive({
  deptIds: [] as number[],
  startDate: dateRange.value[0],
  endDate: dateRange.value[1],
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const loading = ref(false)
const exportLoading = ref(false)
const summary = ref<StockReportSummary | null>(null)
const items = ref<StockReportItem[]>([])
const total = ref(0)

// 第二期：价值报表模式
const reportMode = ref<'quantity' | 'value'>('quantity')
const valueReport = ref<StockValueReportVO | null>(null)

const ledgerVisible = ref(false)
const ledgerDeptId = ref(0)
const ledgerProductId = ref(0)
const ledgerProductName = ref('')

function syncDateRange() {
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.startDate = dateRange.value[0]
    queryParams.endDate = dateRange.value[1]
  } else {
    queryParams.startDate = undefined
    queryParams.endDate = undefined
  }
}

function formatQty(val: number | undefined | null) {
  if (val === null || val === undefined) return '-'
  return Number(val)
}

function formatDateTime(val: string | undefined | null): string {
  if (!val) return '-'
  return parseTime(val, '{y}-{m}-{d} {h}:{i}:{s}') || val.replace('T', ' ')
}

function deptNameById(deptId: number | undefined | null): string {
  if (!deptId) return '-'
  const dept = depts.value.find((item: any) => Number(item.deptId) === Number(deptId))
  return dept?.deptName || `门店${deptId}`
}

function formatAmount(val: number | undefined | null): string {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toFixed(2)
}

function formatRate(val: number | undefined | null): string {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toFixed(2)
}

function formatAmountCell(_row: any, _column: any, cellValue: any) {
  if (cellValue === null || cellValue === undefined) return '0.00'
  return Number(cellValue).toFixed(2)
}

function formatCostCell(_row: any, _column: any, cellValue: any) {
  if (cellValue === null || cellValue === undefined) return '0.000000'
  return Number(cellValue).toFixed(6)
}

function grossProfitClass(val: number | undefined | null): string {
  if (val === null || val === undefined) return ''
  return Number(val) >= 0 ? 'success' : 'danger'
}

function periodStatusLabel(status: string | undefined): string {
  if (status === 'ACTIVE') return '进行中（可调整）'
  if (status === 'LOCKED') return '已回本待结转（禁止调整）'
  if (status === 'CARRIED_FORWARD') return '已结转（禁止调整）'
  return status || '-'
}

function periodStatusTag(status: string | undefined): 'success' | 'warning' | 'info' {
  if (status === 'ACTIVE') return 'success'
  if (status === 'LOCKED') return 'warning'
  if (status === 'CARRIED_FORWARD') return 'info'
  return 'info'
}

function clearData() {
  summary.value = null
  items.value = []
  total.value = 0
  valueReport.value = null
}

function handleTabChange() {
  handleQuery()
}

function handleQuery() {
  syncDateRange()
  loading.value = true
  if (reportMode.value === 'value') {
    getStockValueReport(queryParams)
      .then((res: any) => {
        valueReport.value = res.data || null
      })
      .catch(() => {
        valueReport.value = null
        ElMessage.error('库存价值报表查询失败，请稍后重试')
      })
      .finally(() => {
        loading.value = false
      })
    return
  }
  getStockReport(queryParams)
    .then((res: any) => {
      const data = res.data || {}
      summary.value = data.summary || null
      items.value = data.items || []
      total.value = data.total || 0
    })
    .catch(() => {
      // fail closed: clear old data, show safe business message
      clearData()
      ElMessage.error('库存报表查询失败，请稍后重试')
    })
    .finally(() => {
      loading.value = false
    })
}

function resetQuery() {
  queryParams.deptIds = []
  dateRange.value = defaultDateRange()
  queryParams.startDate = dateRange.value[0]
  queryParams.endDate = dateRange.value[1]
  queryParams.keyword = ''
  queryParams.status = ''
  queryParams.pageNum = 1
  queryParams.pageSize = 20
  handleQuery()
}

function handleExport() {
  syncDateRange()
  exportLoading.value = true
  exportStockReport(queryParams)
    .then((data: any) => {
      const blob = new Blob([data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      saveAs(blob, '库存报表.xlsx')
      ElMessage.success('导出成功')
    })
    .catch(() => {
      ElMessage.error('库存报表导出失败，请稍后重试')
    })
    .finally(() => {
      exportLoading.value = false
    })
}

function openLedger(row: StockReportItem) {
  ledgerDeptId.value = row.deptId
  ledgerProductId.value = row.productId
  ledgerProductName.value = row.productName
  ledgerVisible.value = true
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.report-page {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.report-filter-panel {
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.report-query-form {
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) minmax(260px, 1.4fr) minmax(180px, 1fr) minmax(160px, 0.8fr) auto;
  gap: 14px 18px;
  align-items: end;
}

.report-query-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.report-query-form :deep(.el-form-item__label) {
  justify-content: flex-start;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 600;
  line-height: 1.2;
}

.report-query-control {
  width: 100%;
}

.report-query-actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
}

@media (max-width: 1100px) {
  .report-query-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .report-query-form {
    grid-template-columns: 1fr;
  }
}

.summary-row {
  margin-bottom: 16px;
}

.summary-row .el-col {
  margin-bottom: 12px;
}

.metric-card {
  position: relative;
  min-height: 84px;
  padding: 14px 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(var(--theme-primary-rgb), 0.14);
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(var(--theme-primary-rgb), 0.06);
}

.metric-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 4px;
  border-radius: 0 8px 8px 0;
  background: var(--theme-primary);
}

.metric-card.success::before {
  background: #67c23a;
}

.metric-card.warning::before {
  background: #e6a23c;
}

.metric-card.danger::before {
  background: #f56c6c;
}

.metric-card::after {
  content: '';
  position: absolute;
  right: -34px;
  top: -48px;
  width: 116px;
  height: 116px;
  border-radius: 999px;
  background: rgba(var(--theme-primary-rgb), 0.08);
}

.metric-label {
  position: relative;
  z-index: 1;
  color: #606266;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
}

.metric-value {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  color: var(--theme-primary-dark);
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.caliber-notes {
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.caliber-notes :deep(.el-collapse-item__header) {
  padding: 0 16px;
  font-weight: 700;
  color: #303133;
}

.caliber-notes :deep(.el-collapse-item__content) {
  padding: 0 16px 12px;
}

.caliber-text {
  margin: 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}

.section-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-shadow: 0 8px 24px rgba(24, 39, 75, 0.04);
}

.section-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid #edf0f5;
  color: #303133;
  font-weight: 700;
}

.section-card :deep(.el-card__body) {
  padding: 14px 16px 18px;
}

.report-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.report-tabs {
  margin-bottom: 16px;
}

.report-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.period-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 0;
}

.period-lock-hint {
  color: #909399;
  font-size: 13px;
}

.cost-ready-alert {
  margin-bottom: 16px;
}
</style>
