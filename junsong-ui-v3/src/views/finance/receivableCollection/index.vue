<template>
  <div class="app-container receivable-command">
    <div class="page-head">
      <div>
        <h2>应收催收作战台</h2>
        <p>按账龄分层、承诺回款和下次跟进时间组织应收催收动作。</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="warning" :icon="Connection" :loading="syncLoading" @click="handleSync" v-hasPermi="['finance:receivableCollection:sync']">同步应收</el-button>
      </div>
    </div>

    <el-row :gutter="12" class="summary-row">
      <el-col :xs="12" :sm="8" :md="5">
        <div class="summary-card">
          <span>待跟进</span>
          <strong>{{ summary.pendingCount || 0 }}</strong>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="5">
        <div class="summary-card promise">
          <span>承诺回款</span>
          <strong>&yen;{{ money(summary.promisedAmount) }}</strong>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="5">
        <div class="summary-card danger">
          <span>逾期承诺</span>
          <strong>{{ summary.overduePromiseCount || 0 }}</strong>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="5">
        <div class="summary-card warning">
          <span>30天以上应收</span>
          <strong>{{ summary.age30PlusCount || 0 }}</strong>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="summary-card">
          <span>未缴金额</span>
          <strong>&yen;{{ money(summary.totalUnpaidAmount) }}</strong>
        </div>
      </el-col>
    </el-row>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="销售单/客户" clearable style="width: 220px" @keyup.enter="handleQuery" />
      <el-select v-model="query.collectionStatus" placeholder="催收状态" clearable style="width: 160px">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.ageBucket" placeholder="账龄分层" clearable style="width: 160px">
        <el-option label="0-7天" value="AGE_0_7" />
        <el-option label="8-14天" value="AGE_8_14" />
        <el-option label="15-30天" value="AGE_15_30" />
        <el-option label="30天以上" value="AGE_30_PLUS" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
    </div>

    <el-tabs v-model="activeTab" class="command-tabs">
      <el-tab-pane label="今日催收" name="today">
        <collection-table :rows="dashboard.todayFollowUps || []" @follow="openFollow" />
      </el-tab-pane>
      <el-tab-pane label="逾期承诺" name="overdue">
        <collection-table :rows="dashboard.overduePromises || []" @follow="openFollow" />
      </el-tab-pane>
      <el-tab-pane label="高风险应收" name="risk">
        <collection-table :rows="dashboard.highRiskReceivables || []" @follow="openFollow" />
      </el-tab-pane>
      <el-tab-pane label="全部应收" name="all">
        <collection-table :rows="rows" @follow="openFollow" />
      </el-tab-pane>
    </el-tabs>

    <pagination
      v-show="total > 0 && activeTab === 'all'"
      :total="total"
      v-model:page="query.pageNum"
      v-model:limit="query.pageSize"
      @pagination="loadList"
    />

    <el-dialog v-model="followVisible" title="催收跟进" width="520px" append-to-body>
      <el-form :model="followForm" label-width="110px">
        <el-form-item label="催收状态">
          <el-select v-model="followForm.collectionStatus" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="承诺回款日期">
          <el-date-picker v-model="followForm.promisedPayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="承诺回款金额">
          <el-input-number v-model="followForm.promisedAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="followForm.nextFollowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="跟进备注">
          <el-input v-model="followForm.followNote" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followVisible = false">取 消</el-button>
        <el-button type="primary" :loading="followLoading" @click="submitFollow">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Refresh, Search } from '@element-plus/icons-vue'
import { followReceivableCollection, getReceivableCollectionDashboard, listReceivableCollections, syncReceivableCollections } from '@/api/finance/receivableCollection'
import CollectionTable from './components/CollectionTable.vue'

const loading = ref(false)
const syncLoading = ref(false)
const followLoading = ref(false)
const followVisible = ref(false)
const activeTab = ref('today')
const total = ref(0)
const rows = ref<any[]>([])
const selectedRow = ref<any>(null)
const dashboard = ref<any>({})
const summary = computed(() => dashboard.value.summary || {})

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  collectionStatus: '',
  ageBucket: '',
})

const followForm = reactive({
  collectionStatus: 'CONTACTED',
  promisedPayDate: '',
  promisedAmount: 0,
  nextFollowTime: '',
  followNote: '',
})

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

function loadDashboard() {
  return getReceivableCollectionDashboard({}).then((res: any) => {
    dashboard.value = res.data || {}
  })
}

function loadList() {
  loading.value = true
  return listReceivableCollections({ ...query })
    .then((res: any) => {
      rows.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function loadData() {
  return Promise.all([loadDashboard(), loadList()])
}

function handleQuery() {
  query.pageNum = 1
  loadList()
}

function handleSync() {
  syncLoading.value = true
  syncReceivableCollections({})
    .then((res: any) => {
      ElMessage.success(res.msg || '同步应收完成')
      return loadData()
    })
    .finally(() => {
      syncLoading.value = false
    })
}

function openFollow(row: any) {
  selectedRow.value = row
  followForm.collectionStatus = row.collectionStatus || 'CONTACTED'
  followForm.promisedPayDate = row.promisedPayDate || ''
  followForm.promisedAmount = Number(row.promisedAmount || 0)
  followForm.nextFollowTime = row.nextFollowTime || ''
  followForm.followNote = ''
  followVisible.value = true
}

function submitFollow() {
  if (!selectedRow.value?.collectionId) return
  followLoading.value = true
  followReceivableCollection(selectedRow.value.collectionId, { ...followForm })
    .then(() => {
      ElMessage.success('催收跟进已保存')
      followVisible.value = false
      return loadData()
    })
    .finally(() => {
      followLoading.value = false
    })
}

onMounted(loadData)
</script>

<style scoped>
.receivable-command {
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

.page-head,
.filter-bar,
.summary-card {
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fff;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  margin-bottom: 12px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.page-head p {
  margin: 0;
  color: #667085;
}

.head-actions,
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-row {
  margin-bottom: 12px;
}

.summary-card {
  min-height: 84px;
  padding: 14px;
}

.summary-card span {
  display: block;
  margin-bottom: 8px;
  color: #667085;
  font-size: 13px;
}

.summary-card strong {
  color: #18202f;
  font-size: 22px;
}

.summary-card.promise strong {
  color: #0f8f72;
}

.summary-card.danger strong {
  color: #d92d20;
}

.summary-card.warning strong {
  color: #b54708;
}

.filter-bar {
  align-items: center;
  padding: 12px;
  margin-bottom: 12px;
}

.command-tabs {
  padding: 12px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fff;
}
</style>
