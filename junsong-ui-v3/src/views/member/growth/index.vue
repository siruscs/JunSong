<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="会员编号" prop="memberNo">
        <el-input
          v-model="queryParams.memberNo"
          placeholder="请输入会员编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员姓名" prop="memberName">
        <el-input
          v-model="queryParams.memberName"
          placeholder="请输入会员姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="请选择" clearable style="width: 160px;">
          <el-option
            v-for="item in sourceTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:growth:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="growthList">
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="会员编号" align="center" prop="memberNo" width="120" />
      <el-table-column label="会员姓名" align="center" prop="memberName" width="100" />
      <el-table-column label="来源类型" align="center" prop="sourceType" width="120">
        <template #default="scope">
          <el-tag :type="getSourceTypeTag(scope.row.sourceType)">
            {{ getSourceTypeName(scope.row.sourceType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="成长值变动" align="center" prop="growthChange" width="120">
        <template #default="scope">
          <span :style="{ color: scope.row.growthChange > 0 ? '#67C23A' : '#F56C6C', fontWeight: 'bold' }">
            {{ scope.row.growthChange > 0 ? '+' : '' }}{{ scope.row.growthChange }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变动后成长值" align="center" prop="balance" width="130">
        <template #default="scope">
          <span style="color: #E6A23C; font-weight: bold;">{{ scope.row.balance }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变动前等级" align="center" prop="beforeLevel" width="120">
        <template #default="scope">
          <span>{{ getLevelName(scope.row.beforeLevel) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变动后等级" align="center" prop="afterLevel" width="120">
        <template #default="scope">
          <span>{{ getLevelName(scope.row.afterLevel) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { parseTime } from "@/utils/junsong"
import { useDownload } from "@/composables/useDownload"
import { listGrowthRecord } from "@/api/member/growth"
import { listLevel } from "@/api/member/level"
const { download } = useDownload()

export default {
  name: "Growth",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      growthList: [],
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        memberNo: undefined,
        memberName: undefined,
        sourceType: undefined,
        beginCreateTime: undefined,
        endCreateTime: undefined
      },
      sourceTypeOptions: [
        { value: 'SALE', label: '消费' },
        { value: 'SIGN_IN', label: '签到' },
        { value: 'SIGN_IN_BACKFILL', label: '补签到' },
        { value: 'SIGN_IN_DELETE', label: '删除签到' },
        { value: 'MANUAL', label: '手动调整' },
        { value: 'DECAY', label: '衰减' }
      ],
      levelMap: {}
    }
  },
  created() {
    this.loadLevelMap()
    this.getList()
  },
  methods: {
    parseTime,
    resetForm(formName) {
      this.$refs[formName]?.resetFields?.()
    },
    download,
    loadLevelMap() {
      listLevel({}).then(response => {
        const map = {}
        const list = response.data || []
        list.forEach(item => {
          map[item.typeCode] = item.typeName
        })
        this.levelMap = map
      })
    },
    getLevelName(code) {
      if (!code) return '-'
      return this.levelMap[code] || code
    },
    getList() {
      this.loading = true
      const params = {
        ...this.queryParams
      }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginCreateTime = this.dateRange[0]
        params.endCreateTime = this.dateRange[1]
      }
      listGrowthRecord(params).then(response => {
        this.growthList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleExport() {
      this.download('member/growth/export', {
        ...this.queryParams
      }, `growth_${new Date().getTime()}.xlsx`)
    },
    getSourceTypeName(type) {
      const item = this.sourceTypeOptions.find(o => o.value === type)
      return item ? item.label : '-'
    },
    getSourceTypeTag(type) {
      const types = {
        'SALE': 'success',
        'SIGN_IN': 'primary',
        'SIGN_IN_BACKFILL': 'primary',
        'SIGN_IN_DELETE': 'danger',
        'MANUAL': 'warning',
        'DECAY': 'info'
      }
      return types[type] || 'info'
    }
  }
}
</script>

<style scoped>
</style>
