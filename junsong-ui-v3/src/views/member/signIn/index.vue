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
      <el-form-item label="签到日期">
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
          icon="Edit"
          size="small"
          @click="handleSignIn"
          v-hasPermi="['member:signIn:add']"
        >会员签到</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Calendar"
          size="small"
          @click="handleBackfill"
          v-hasPermi="['member:signIn:backfill']"
        >签到补录</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:signIn:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="signInList">
      <el-table-column label="签到日期" align="center" prop="signDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.signDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="会员编号" align="center" prop="memberNo" width="120" />
      <el-table-column label="会员姓名" align="center" prop="memberName" width="100" />
      <el-table-column label="连续签到天数" align="center" prop="continuousDays" width="130">
        <template #default="scope">
          <span style="color: #E6A23C;">{{ scope.row.continuousDays || 0 }} 天</span>
        </template>
      </el-table-column>
      <el-table-column label="获得积分" align="center" prop="pointsEarned" width="120">
        <template #default="scope">
          <span style="color: #67C23A;">+{{ scope.row.pointsEarned || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="获得成长值" align="center" prop="growthEarned" width="120">
        <template #default="scope">
          <span style="color: #409EFF;">+{{ scope.row.growthEarned || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="签到时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template #default="scope">
          <el-button
            size="small"
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:signIn:remove']"
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

    <!-- 会员签到对话框 -->
    <el-dialog title="会员签到" v-model="signInOpen" width="500px" append-to-body>
      <el-form ref="signInForm" :model="signInForm" :rules="signInRules" label-width="100px">
        <el-form-item label="选择会员" prop="memberId">
          <MemberSelect
            v-model="signInForm.memberId"
            placeholder="请输入会员编号或姓名搜索"
            @change="onMemberChange"
          />
        </el-form-item>
        <el-form-item label="会员编号">
          <span>{{ signInForm.memberNo || '-' }}</span>
        </el-form-item>
        <el-form-item label="会员姓名">
          <span>{{ signInForm.memberName || '-' }}</span>
        </el-form-item>
        <el-form-item label="今日状态">
          <el-tag v-if="todaySignedIn" type="success">今日已签到</el-tag>
          <el-tag v-else type="info">今日未签到</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button
            type="primary"
            :disabled="!signInForm.memberId || todaySignedIn"
            @click="submitSignIn"
          >签 到</el-button>
          <el-button @click="cancelSignIn">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 签到补录对话框 -->
    <el-dialog title="签到补录" v-model="backfillOpen" width="720px" append-to-body>
      <el-form ref="backfillForm" :model="backfillForm" :rules="backfillRules" label-width="110px">
        <el-form-item label="选择会员" prop="memberId">
          <MemberSelect
            v-model="backfillForm.memberId"
            placeholder="请输入会员编号或姓名搜索"
            @change="onBackfillMemberChange"
          />
        </el-form-item>
        <el-form-item label="会员等级">
          <el-tag v-if="backfillForm.levelName" type="primary">{{ backfillForm.levelName }}</el-tag>
          <span v-else>-</span>
        </el-form-item>
        <el-form-item label="目标月份" prop="targetMonth">
          <el-date-picker
            v-model="backfillForm.targetMonth"
            type="month"
            placeholder="请选择目标月份"
            value-format="YYYY-MM"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="补录模式" prop="fillMode">
          <el-radio-group v-model="backfillForm.fillMode">
            <el-radio value="SELECT_DATES">选择日期</el-radio>
            <el-radio value="COUNT_ONLY">输入次数</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="backfillForm.fillMode === 'SELECT_DATES'" label="签到日期" prop="signDates">
          <el-date-picker
            v-model="backfillForm.signDates"
            type="dates"
            placeholder="请选择签到日期（可多选）"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item v-if="backfillForm.fillMode === 'COUNT_ONLY'" label="补签次数" prop="signCount">
          <el-input-number v-model="backfillForm.signCount" :min="1" :max="31" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="backfillForm.remark" type="textarea" placeholder="请输入备注" :rows="2" />
        </el-form-item>

        <el-descriptions :column="3" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="单次签到积分">
            <span style="color: #67C23A;">{{ backfillForm.pointsPerSign != null ? backfillForm.pointsPerSign : '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="单次成长值">
            <span style="color: #409EFF;">{{ backfillForm.growthPerSign != null ? backfillForm.growthPerSign : '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预计补录次数">
            <span style="color: #E6A23C;">{{ estimatedCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预计总积分">
            <span style="color: #67C23A;">{{ estimatedPoints }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预计总成长值">
            <span style="color: #409EFF;">{{ estimatedGrowth }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="backfillResult" type="success" :closable="false" show-icon style="margin-bottom: 12px;">
          <template #title>补录成功</template>
        </el-alert>
        <el-descriptions v-if="backfillResult" :column="2" border>
          <el-descriptions-item label="实际补录次数">{{ backfillResult.actualCount }}</el-descriptions-item>
          <el-descriptions-item label="请求补录次数">{{ backfillResult.requestedCount }}</el-descriptions-item>
          <el-descriptions-item label="获得总积分">
            <span style="color: #67C23A;">+{{ backfillResult.totalPoints }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="获得总成长值">
            <span style="color: #409EFF;">+{{ backfillResult.totalGrowth }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="已补录日期">
            <template v-if="backfillResult.filledDates && backfillResult.filledDates.length">
              <el-tag v-for="d in backfillResult.filledDates" :key="d" type="success" size="small" style="margin: 2px;">{{ d }}</el-tag>
            </template>
            <span v-else>无</span>
          </el-descriptions-item>
          <el-descriptions-item label="跳过日期">
            <template v-if="backfillResult.skippedDates && backfillResult.skippedDates.length">
              <el-tag v-for="d in backfillResult.skippedDates" :key="d" type="warning" size="small" style="margin: 2px;">{{ d }}</el-tag>
            </template>
            <span v-else>无</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button
            type="primary"
            :loading="backfillSubmitting"
            :disabled="!canSubmitBackfill"
            @click="submitBackfill"
          >补 录</el-button>
          <el-button @click="cancelBackfill">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from "element-plus"
import { parseTime } from "@/utils/junsong"
import { useDownload } from "@/composables/useDownload"
import { listSignIn, doSignIn, getTodaySignIn, backfillSignIn, previewSignIn, delSignIn } from "@/api/member/signIn"
import MemberSelect from "@/components/MemberSelect/index.vue"
const { download } = useDownload()

export default {
  name: "SignIn",
  components: { MemberSelect },
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      signInList: [],
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        memberNo: undefined,
        memberName: undefined
      },
      signInOpen: false,
      signInForm: {
        memberId: undefined,
        memberNo: undefined,
        memberName: undefined
      },
      signInRules: {
        memberId: [
          { required: true, message: "请选择会员", trigger: "change" }
        ]
      },
      todaySignedIn: false,
      backfillOpen: false,
      backfillSubmitting: false,
      backfillResult: null,
      backfillForm: {
        memberId: undefined,
        levelName: undefined,
        targetMonth: undefined,
        fillMode: "SELECT_DATES",
        signDates: [],
        signCount: 1,
        remark: undefined,
        pointsPerSign: undefined,
        growthPerSign: undefined
      },
      backfillRules: {
        memberId: [
          { required: true, message: "请选择会员", trigger: "change" }
        ],
        targetMonth: [
          { required: true, message: "请选择目标月份", trigger: "change" }
        ],
        fillMode: [
          { required: true, message: "请选择补录模式", trigger: "change" }
        ]
      }
    }
  },
  computed: {
    estimatedCount() {
      if (this.backfillForm.fillMode === "SELECT_DATES") {
        return Array.isArray(this.backfillForm.signDates) ? this.backfillForm.signDates.length : 0
      }
      return this.backfillForm.signCount || 0
    },
    estimatedPoints() {
      if (this.backfillForm.pointsPerSign == null) return "-"
      return this.estimatedCount * this.backfillForm.pointsPerSign
    },
    estimatedGrowth() {
      if (this.backfillForm.growthPerSign == null) return "-"
      return this.estimatedCount * this.backfillForm.growthPerSign
    },
    canSubmitBackfill() {
      if (this.backfillSubmitting) return false
      if (!this.backfillForm.memberId || !this.backfillForm.targetMonth) return false
      if (this.backfillForm.fillMode === "SELECT_DATES") {
        return Array.isArray(this.backfillForm.signDates) && this.backfillForm.signDates.length > 0
      }
      return this.backfillForm.signCount > 0
    }
  },
  created() {
    this.getList()
  },
  methods: {
    parseTime,
    resetForm(formName) {
      this.$refs[formName]?.resetFields?.()
    },
    download,
    getList() {
      this.loading = true
      const queryData = {
        ...this.queryParams
      }
      if (this.dateRange && this.dateRange.length === 2) {
        queryData.params = {
          beginTime: this.dateRange[0],
          endTime: this.dateRange[1]
        }
      }
      listSignIn(queryData).then(response => {
        this.signInList = response.rows
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
      const queryData = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        queryData.params = {
          beginTime: this.dateRange[0],
          endTime: this.dateRange[1]
        }
      }
      this.download('member/signIn/export', queryData, `signIn_${new Date().getTime()}.xlsx`)
    },
    handleDelete(row) {
      const signDate = this.parseTime(row.signDate, '{y}-{m}-{d}')
      ElMessageBox.confirm(
        `确认删除 ${row.memberName || row.memberNo || ''} 在 ${signDate} 的签到记录？删除后会同步冲回本次获得的积分和成长值。`,
        "删除确认",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).then(() => {
        return delSignIn(row.signId)
      }).then(() => {
        ElMessage.success("删除成功")
        this.getList()
      }).catch(() => {})
    },
    handleSignIn() {
      this.signInForm = {
        memberId: undefined,
        memberNo: undefined,
        memberName: undefined
      }
      this.todaySignedIn = false
      this.signInOpen = true
    },
    onMemberChange(member) {
      if (member) {
        this.signInForm.memberNo = member.memberNo
        this.signInForm.memberName = member.memberName
        this.checkTodaySignIn(member.memberId)
      } else {
        this.signInForm.memberNo = undefined
        this.signInForm.memberName = undefined
        this.todaySignedIn = false
      }
    },
    checkTodaySignIn(memberId) {
      getTodaySignIn(memberId).then(response => {
        this.todaySignedIn = response.signedIn === true
      })
    },
    submitSignIn() {
      this.$refs["signInForm"].validate(valid => {
        if (valid) {
          doSignIn({ memberId: this.signInForm.memberId }).then(response => {
            ElMessage.success("签到成功")
            this.signInOpen = false
            this.getList()
          })
        }
      })
    },
    cancelSignIn() {
      this.signInOpen = false
      this.resetForm("signInForm")
    },
    handleBackfill() {
      this.backfillForm = {
        memberId: undefined,
        levelName: undefined,
        targetMonth: undefined,
        fillMode: "SELECT_DATES",
        signDates: [],
        signCount: 1,
        remark: undefined,
        pointsPerSign: undefined,
        growthPerSign: undefined
      }
      this.backfillResult = null
      this.backfillSubmitting = false
      this.backfillOpen = true
    },
    onBackfillMemberChange(member) {
      if (member) {
        this.backfillForm.memberNo = member.memberNo
        this.backfillForm.memberName = member.memberName
        previewSignIn(member.memberId).then(response => {
          if (response.code === 200 && response.data) {
            this.backfillForm.levelName = response.data.levelName
            this.backfillForm.pointsPerSign = response.data.pointsPerSign
            this.backfillForm.growthPerSign = response.data.growthPerSign
          }
        })
      } else {
        this.backfillForm.memberNo = undefined
        this.backfillForm.memberName = undefined
        this.backfillForm.levelName = undefined
        this.backfillForm.pointsPerSign = undefined
        this.backfillForm.growthPerSign = undefined
      }
    },
    submitBackfill() {
      this.$refs["backfillForm"].validate(valid => {
        if (valid) {
          this.backfillSubmitting = true
          const data = {
            memberId: this.backfillForm.memberId,
            targetMonth: this.backfillForm.targetMonth,
            fillMode: this.backfillForm.fillMode,
            remark: this.backfillForm.remark
          }
          if (this.backfillForm.fillMode === "SELECT_DATES") {
            data.signDates = this.backfillForm.signDates
          } else {
            data.signCount = this.backfillForm.signCount
          }
          backfillSignIn(data).then(response => {
            this.backfillResult = response.data
            if (response.data) {
              this.backfillForm.pointsPerSign = response.data.pointsPerSign
              this.backfillForm.growthPerSign = response.data.growthPerSign
            }
            ElMessage.success("补录成功")
            this.getList()
          }).finally(() => {
            this.backfillSubmitting = false
          })
        }
      })
    },
    cancelBackfill() {
      this.backfillOpen = false
      this.resetForm("backfillForm")
      this.backfillResult = null
    }
  }
}
</script>

<style scoped>
</style>
