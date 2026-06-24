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
      <el-form-item label="记录类型" prop="recordType">
        <el-select v-model="queryParams.recordType" placeholder="请选择" clearable>
          <el-option label="消费得积分" value="1" />
          <el-option label="兑换扣积分" value="2" />
          <el-option label="过期清零" value="3" />
          <el-option label="手动调整" value="4" />
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
          type="primary"
          plain
          icon="Plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['member:pointsRecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:pointsRecord:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="会员编号" align="center" prop="memberNo" width="120" />
      <el-table-column label="会员姓名" align="center" prop="memberName" width="100" />
      <el-table-column label="记录类型" align="center" prop="recordType" width="120">
        <template #default="scope">
          <el-tag :type="getRecordTypeTag(scope.row.recordType)">
            {{ getRecordTypeName(scope.row.recordType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="消费金额" align="center" prop="consumeAmount" width="120">
        <template #default="scope">
          <span style="color: #409EFF;">¥{{ scope.row.consumeAmount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="积分变动" align="center" prop="points" width="120">
        <template #default="scope">
          <span :style="{ color: scope.row.points > 0 ? '#67C23A' : '#F56C6C' }">
            {{ scope.row.points > 0 ? '+' : '' }}{{ scope.row.points }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变动后余额" align="center" prop="balance" width="120">
        <template #default="scope">
          <span style="color: #E6A23C;">{{ scope.row.balance }}</span>
        </template>
      </el-table-column>
      <el-table-column label="记录时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作人" align="center" prop="createBy" width="120" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button
            size="small"
            link type="primary"
            icon="View"
            @click="handleView(scope.row)"
          >查看</el-button>
          <el-button
            v-if="isLatestRecord(scope.row) && scope.row.recordType !== '2'"
            size="small"
            link type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['member:pointsRecord:edit']"
          >修改</el-button>
          <el-button
            v-if="isLatestRecord(scope.row) && scope.row.recordType !== '2'"
            size="small"
            link type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:pointsRecord:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会员编号" prop="memberNo">
              <el-input v-model="form.memberNo" placeholder="请输入会员编号" @blur="handleMemberSearch" @keyup.enter="handleMemberSearch">
                <template #append><el-button icon="Search" @click="handleMemberSearch"></el-button></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会员姓名">
              <el-input v-model="form.memberName" placeholder="自动获取" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="当前积分">
              <el-input v-model="form.currentPoints" disabled>
                <template #prepend>当前积分</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录类型" prop="recordType">
              <el-select v-model="form.recordType" placeholder="请选择" style="width: 100%;">
                <el-option label="消费得积分" value="1" />
                <el-option label="兑换扣积分" value="2" />
                <el-option label="过期清零" value="3" />
                <el-option label="手动调整" value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="消费金额" prop="consumeAmount">
              <el-input-number v-model="form.consumeAmount" :precision="2" :min="0" style="width: 100%;" @change="calculatePoints" @blur="handleConsumeBlur" />
              <div v-if="effectiveRule" style="font-size: 12px; color: #909399; margin-top: 4px;">
                当前规则：{{ effectiveRule.ruleName }}（每积分消费 {{ effectiveRule.pointsPerYuan }} 元，{{ getRuleTypeName(effectiveRule.ruleType) }}）
              </div>
              <div v-else style="font-size: 12px; color: #F56C6C; margin-top: 4px;">
                未配置生效的积分规则
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="积分变动" prop="pointsChange">
              <el-input-number v-model="form.pointsChange" :step="1" :precision="0" :min="allowNegativePoints ? undefined : 0" style="width: 100%;" />
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                {{ form.recordType === '1' ? '输入消费金额后自动计算，结果为整数' :
                   form.recordType === '3' || form.recordType === '4' ? '可输入负数' :
                   '结果为正整数' }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="积分记录详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="会员编号">{{ viewForm.memberNo }}</el-descriptions-item>
        <el-descriptions-item label="会员姓名">{{ viewForm.memberName }}</el-descriptions-item>
        <el-descriptions-item label="记录类型">
          <el-tag :type="getRecordTypeTag(viewForm.recordType)">
            {{ getRecordTypeName(viewForm.recordType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="消费金额">
          <span style="color: #409EFF;">¥{{ viewForm.consumeAmount || 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="积分变动">
          <span :style="{ color: viewForm.points > 0 ? '#67C23A' : '#F56C6C', fontWeight: 'bold' }">
            {{ viewForm.points > 0 ? '+' : '' }}{{ viewForm.points }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="变动后余额">
          <span style="color: #E6A23C; font-weight: bold;">{{ viewForm.balance }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="记录时间" :span="2">{{ parseTime(viewForm.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ viewForm.createBy }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-if="viewForm.remark" style="margin-top: 20px;" :column="1" border>
        <el-descriptions-item label="备注">{{ viewForm.remark }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from "element-plus"
import { parseTime } from "@/utils/junsong"
import { useDownload } from "@/composables/useDownload"
import { listPointsRecord, getPointsRecord, addPointsRecord, updatePointsRecord, delPointsRecord } from "@/api/member/pointsRecord"
import { getMemberByNo } from "@/api/member/member"
import { getEffectiveRule } from "@/api/member/pointsRule"
const { download } = useDownload()

export default {
  name: "PointsRecord",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      recordList: [],
      title: "",
      open: false,
      viewOpen: false,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        memberNo: undefined,
        recordType: undefined,
        beginRecordTime: undefined,
        endRecordTime: undefined
      },
      form: {},
      viewForm: {},
      effectiveRule: null,
      rules: {
        memberNo: [
          { required: true, message: "会员编号不能为空", trigger: "blur" }
        ],
        recordType: [
          { required: true, message: "记录类型不能为空", trigger: "change" }
        ],
        pointsChange: [
          { required: true, message: "积分变动不能为空", trigger: "blur" }
        ]
      },
      recordTypeOptions: {
        '1': '消费得积分',
        '2': '兑换扣积分',
        '3': '过期清零',
        '4': '手动调整'
      }
    }
  },
  created() {
    this.getList()
  },
  computed: {
    /** 过期清零或手动调整时允许负数 */
    allowNegativePoints() {
      return this.form.recordType === '3' || this.form.recordType === '4'
    }
  },
  methods: {
    parseTime,
    resetForm(formName) {
      this.$refs[formName]?.resetFields?.()
    },
    download,
    getList() {
      this.loading = true
      const params = {
        ...this.queryParams
      }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginRecordTime = this.dateRange[0]
        params.endRecordTime = this.dateRange[1]
      }
      listPointsRecord(params).then(response => {
        this.recordList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        recordId: undefined,
        memberId: undefined,
        memberNo: undefined,
        memberName: undefined,
        currentPoints: 0,
        recordType: undefined,
        consumeAmount: undefined,
        pointsChange: undefined,
        balancePoints: 0,
        remark: undefined
      }
      this.resetForm("form")
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
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加积分记录"
      getEffectiveRule().then(response => {
        if (response.code === 200 && response.data) {
          this.effectiveRule = response.data
        } else {
          this.effectiveRule = null
        }
      }).catch(() => {
        this.effectiveRule = null
      })
    },
    /** 根据消费金额和规则自动计算积分 */
    calculatePoints(val) {
      if (!val || val <= 0 || !this.effectiveRule) return
      const rule = this.effectiveRule
      // 公式：积分 = 消费金额 / 每积分消费金额
      // 例：消费120元，规则每积分消费60元 → 120/60=2分；规则每积分消费100元 → 120/100=1.2→进一法取整为2分
      let rawPoints = Number((val / rule.pointsPerYuan))
      if (isNaN(rawPoints) || !isFinite(rawPoints)) return
      // 根据规则取整方式转换：ruleType 1=进一法(向上取整) 2=四舍五入 3=舍零取整(向下取整)
      if (rule.ruleType === '1' || rule.ruleType === '进一法') {
        rawPoints = Math.ceil(rawPoints)
      } else if (rule.ruleType === '2' || rule.ruleType === '四舍五入') {
        rawPoints = Math.round(rawPoints)
      } else if (rule.ruleType === '3' || rule.ruleType === '舍零取整') {
        rawPoints = Math.floor(rawPoints)
      }
      this.form.pointsChange = rawPoints
    },
    /** 消费金额失焦时也触发计算 */
    handleConsumeBlur() {
      this.calculatePoints(this.form.consumeAmount)
    },
    /** 判断是否为该会员最新的一条记录（可编辑删除） */
    isLatestRecord(row) {
      if (!this.recordList || this.recordList.length === 0) return false
      // 找到该会员最新的记录（列表按create_time降序排列）
      const memberRecords = this.recordList.filter(r => r.memberId === row.memberId)
      if (memberRecords.length === 0) return false
      // 第一条就是最新的
      return memberRecords[0].recordId === row.recordId
    },
    /** 修改积分记录 */
    handleUpdate(row) {
      this.reset()
      getPointsRecord(row.recordId).then(response => {
        const data = response.data
        this.form = {
          recordId: data.recordId,
          memberId: data.memberId,
          memberNo: data.memberNo,
          memberName: data.memberName,
          recordType: data.recordType,
          consumeAmount: data.consumeAmount || 0,
          pointsChange: data.points || 0,
          currentPoints: data.balance ? Number(data.balance) - Number(data.points || 0) : 0,
          remark: data.remark
        }
        this.open = true
        this.title = "修改积分记录"
      })
    },
    /** 删除积分记录 */
    handleDelete(row) {
      ElMessageBox.confirm('确认删除该条积分记录？').then(() => {
        return delPointsRecord(row.recordId)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleView(row) {
      getPointsRecord(row.recordId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    handleMemberSearch() {
      if (this.form.memberNo) {
        getMemberByNo(this.form.memberNo).then(response => {
          if (response.data) {
            this.form.memberId = response.data.memberId
            this.form.memberName = response.data.memberName
            // 获取该会员最新一条记录的余额作为当前积分（保证累计正确）
            this.fetchLatestBalance(response.data.memberId)
          } else {
            ElMessage.warning("未找到该会员编号（仅限当前机构会员）")
            this.form.memberId = undefined
            this.form.memberName = ''
            this.form.currentPoints = 0
          }
        }).catch(() => {
          ElMessage.error("未找到该会员编号（仅限当前机构会员）")
          this.form.memberId = undefined
          this.form.memberName = ''
          this.form.currentPoints = 0
        })
      }
    },
    /** 获取会员最新积分余额 */
    fetchLatestBalance(memberId) {
      listPointsRecord({ memberId: memberId, pageNum: 1, pageSize: 1 }).then(res => {
        if (res.rows && res.rows.length > 0) {
          // 列表按create_time降序，第一条就是最新记录的balance
          this.form.currentPoints = res.rows[0].balance || 0
        } else {
          // 没有记录则使用会员表的积分
          this.form.currentPoints = 0
        }
      }).catch(() => {
        this.form.currentPoints = 0
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 映射前端字段名到后端实体字段名
          const postData = { ...this.form }
          postData.points = this.form.pointsChange
          postData.balance = (this.form.currentPoints || 0) + (this.form.pointsChange || 0)
          delete postData.pointsChange
          delete postData.balancePoints
          delete postData.currentPoints
          // 根据是否有recordId判断是新增还是修改
          if (this.form.recordId) {
            updatePointsRecord(postData).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPointsRecord(postData).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleExport() {
      this.download('member/pointsRecord/export', {
        ...this.queryParams
      }, `pointsRecord_${new Date().getTime()}.xlsx`)
    },
    getRecordTypeName(type) {
      return this.recordTypeOptions[type] || '-'
    },
    getRecordTypeTag(type) {
      const types = {
        '1': 'success',
        '2': 'warning',
        '3': 'info',
        '4': 'primary'
      }
      return types[type] || 'info'
    },
    getRuleTypeName(type) {
      const map = { '1': '进一法', '2': '四舍五入', '3': '舍零取整' }
      return map[type] || '-'
    }
  }
}
</script>

<style scoped>
</style>
