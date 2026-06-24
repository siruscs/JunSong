<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="机构" prop="deptId">
        <el-select v-model="queryParams.deptId" placeholder="请选择机构" filterable clearable style="width: 200px;">
          <el-option v-for="dept in deptOptions" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item label="分润单号" prop="shareNo">
        <el-input v-model="queryParams.shareNo" placeholder="请输入分润单号" clearable style="width: 180px;" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px;">
          <el-option label="已结转" value="1" />
          <el-option label="已作废" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="handleQuery">搜索</el-button>
        <el-button size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="shareList">
      <el-table-column label="分润单号" align="center" prop="shareNo" min-width="170" />
      <el-table-column label="机构" align="center" min-width="150">
        <template #default="scope">{{ getDeptName(scope.row.deptId) }}</template>
      </el-table-column>
      <el-table-column label="周期ID" align="center" prop="periodId" width="90" />
      <el-table-column label="净利" align="center" prop="netProfit" width="120">
        <template #default="scope">¥{{ formatMoney(scope.row.netProfit) }}</template>
      </el-table-column>
      <el-table-column label="店长比例" align="center" prop="managerProfitRate" width="100">
        <template #default="scope">{{ formatRate(scope.row.managerProfitRate) }}</template>
      </el-table-column>
      <el-table-column label="店长分润" align="center" prop="managerProfitAmount" width="120">
        <template #default="scope">¥{{ formatMoney(scope.row.managerProfitAmount) }}</template>
      </el-table-column>
      <el-table-column label="投资人分润" align="center" prop="investorProfitAmount" width="120">
        <template #default="scope">¥{{ formatMoney(scope.row.investorProfitAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '1' ? 'success' : 'info'">{{ scope.row.status === '1' ? '已结转' : '已作废' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分润时间" align="center" prop="shareTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.shareTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right" width="140">
        <template #default="scope">
          <el-button size="small" type="text" @click="handleView(scope.row)" v-hasPermi="['finance:profitShare:query']">查看</el-button>
          <el-button v-if="scope.row.status === '1'" size="small" type="text" @click="handleCancel(scope.row)" v-hasPermi="['finance:profitShare:remove']">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="分润详情" v-model="viewOpen" width="860px" append-to-body>
      <el-descriptions :column="2" border class="mb20">
        <el-descriptions-item label="分润单号">{{ viewForm.shareNo }}</el-descriptions-item>
        <el-descriptions-item label="机构">{{ getDeptName(viewForm.deptId) }}</el-descriptions-item>
        <el-descriptions-item label="净利">¥{{ formatMoney(viewForm.netProfit) }}</el-descriptions-item>
        <el-descriptions-item label="分润时间">{{ parseTime(viewForm.shareTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
        <el-descriptions-item label="店长分润">¥{{ formatMoney(viewForm.managerProfitAmount) }} / {{ formatRate(viewForm.managerProfitRate) }}</el-descriptions-item>
        <el-descriptions-item label="投资人分润">¥{{ formatMoney(viewForm.investorProfitAmount) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewForm.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="viewForm.details || []" border>
        <el-table-column label="收款类型" align="center" prop="receiverType" width="100">
          <template #default="scope">{{ scope.row.receiverType === '1' ? '店长' : '投资人' }}</template>
        </el-table-column>
        <el-table-column label="收款人" align="center" prop="receiverName" min-width="140" />
        <el-table-column label="投资金额" align="center" prop="investAmount" width="120">
          <template #default="scope">¥{{ formatMoney(scope.row.investAmount) }}</template>
        </el-table-column>
        <el-table-column label="占比" align="center" prop="investRatio" width="100">
          <template #default="scope">{{ formatRate(scope.row.investRatio) }}</template>
        </el-table-column>
        <el-table-column label="分润金额" align="center" prop="shareAmount" width="120">
          <template #default="scope">¥{{ formatMoney(scope.row.shareAmount) }}</template>
        </el-table-column>
        <el-table-column label="自动返款ID" align="center" prop="paymentId" width="120">
          <template #default="scope">{{ scope.row.paymentId || '-' }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProfitShare, getProfitShare, cancelProfitShare } from '@/api/finance/profitShare'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

export default {
  name: 'ProfitShare',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      shareList: [],
      deptOptions: [],
      viewOpen: false,
      viewForm: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deptId: undefined,
        shareNo: undefined,
        status: undefined
      }
    }
  },
  created() {
    this.getDeptOptions()
    this.getList()
  },
  methods: {
    getDeptOptions() {
      this.deptOptions = userStore.depts || []
    },
    getList() {
      this.loading = true
      listProfitShare({ ...this.queryParams, deptId: userStore.currentDeptId }).then(response => {
        this.shareList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleView(row) {
      getProfitShare(row.shareId).then(response => {
        this.viewForm = response.data || {}
        this.viewOpen = true
      })
    },
    handleCancel(row) {
      ElMessageBox.confirm('作废后会删除自动生成的投资人返款，并将周期退回已回本待结转，是否继续？').then(() => cancelProfitShare(row.shareId)).then(() => {
        this.getList()
        ElMessage.success('作废成功')
      }).catch(() => {})
    },
    getDeptName(deptId) {
      const dept = this.deptOptions.find(item => item.deptId === deptId)
      return dept ? dept.deptName : deptId || '-'
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    formatRate(rate) {
      const value = Number(rate || 0)
      return value <= 1 ? (value * 100).toFixed(2) + '%' : value.toFixed(2) + '%'
    }
  }
}
</script>
