<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="秒杀活动" prop="seckillId">
        <el-select v-model="queryParams.seckillId" placeholder="请选择秒杀活动" style="width: 240px;" @change="handleQuery">
          <el-option
            v-for="item in seckillOptions"
            :key="item.seckillId"
            :label="item.seckillName"
            :value="item.seckillId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="会员编号" prop="memberNo">
        <el-input
          v-model="queryParams.memberNo"
          placeholder="请输入会员编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="秒杀日期">
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
          v-hasPermi="['member:seckillRecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="UserFilled"
          size="small"
          @click="handleBatchAll"
          v-hasPermi="['member:seckillRecord:add']"
        >全员秒杀</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['member:seckillRecord:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['member:seckillRecord:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:seckillRecord:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          class="stats-btn"
          icon="DataAnalysis"
          size="small"
          @click="handleStatistics"
          v-hasPermi="['member:seckillRecord:list']"
        >统计</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="秒杀活动" align="center" prop="seckillName" width="180" />
      <el-table-column label="会员编号" align="center" prop="memberNo" width="120" />
      <el-table-column label="会员姓名" align="center" prop="memberName" width="100" />
      <el-table-column label="秒杀日期" align="center" prop="seckillDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.seckillDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="秒杀份额" align="center" prop="shares" width="100" />
      <el-table-column label="已领取" align="center" prop="claimedShares" width="90" />
      <el-table-column label="待领取" align="center" prop="remainingShares" width="90" />
      <el-table-column label="付款方式" align="center" prop="paymentMethod" width="120">
        <template #default="scope">
          <dict-tag :options="paymentMethodDictOptions" :value="scope.row.paymentMethod" />
        </template>
      </el-table-column>
      <el-table-column label="实付金额" align="center" prop="totalAmount" width="120">
        <template #default="scope">
          <span style="color: #67C23A;">¥{{ scope.row.totalAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="领取状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag class="status-link" :type="getStatusTagType(scope.row.status)" @click="openRecordClaims(scope.row)">
            {{ getStatusName(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button
            size="small"
            link type="primary"
            icon="View"
            @click="handleView(scope.row)"
          >查看</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['member:seckillRecord:edit']"
          >修改</el-button>
          <el-button
            v-if="canReceive(scope.row)"
            size="small"
            link type="primary"
            icon="Present"
            @click="handleReceive(scope.row)"
            v-hasPermi="['member:seckillRecord:receive']"
          >领取</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:seckillRecord:remove']"
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

    <el-divider content-position="left">领取记录</el-divider>
    <el-table v-loading="claimLoading" :data="claimList" size="small" border>
      <el-table-column label="会员编号" align="center" prop="memberNo" width="120" />
      <el-table-column label="会员姓名" align="center" prop="memberName" width="120" />
      <el-table-column label="领取份额" align="center" prop="claimShares" width="100" />
      <el-table-column label="领取人" align="center" prop="claimBy" width="120" />
      <el-table-column label="领取时间" align="center" prop="claimTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.claimTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
    </el-table>

    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="秒杀活动" prop="seckillId">
              <el-select v-model="form.seckillId" placeholder="请选择秒杀活动" style="width: 100%;" @change="handleSeckillChange">
                <el-option
                  v-for="item in seckillActiveOptions"
                  :key="item.seckillId"
                  :label="item.seckillName"
                  :value="item.seckillId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="秒杀日期" prop="seckillDate">
              <el-date-picker
                v-model="form.seckillDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会员编号" prop="memberNo">
              <el-input v-model="form.memberNo" placeholder="请输入会员编号" @blur="handleMemberSearch" @keyup.enter="handleMemberSearch">
                <template #append><el-button icon="Search" @click="handleMemberSearch"></el-button></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会员姓名" prop="memberName">
              <el-input v-model="form.memberName" placeholder="自动获取" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="秒杀份额" prop="shares">
              <el-input-number v-model="form.shares" :min="1" style="width: 100%;" @change="calculateAmount" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式" prop="paymentMethod">
              <el-select v-model="form.paymentMethod" placeholder="请选择" style="width: 100%;">
                <el-option v-for="dict in dict.type.finance_payment_method" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="秒杀单价">
              <el-input v-model="form.seckillPrice" disabled>
                <template #prepend>¥</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实付金额">
              <el-input v-model="form.totalAmount" disabled>
                <template #prepend>¥</template>
              </el-input>
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

    <el-dialog title="秒杀记录详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="秒杀活动">{{ viewForm.seckillName }}</el-descriptions-item>
        <el-descriptions-item label="秒杀日期">{{ parseTime(viewForm.seckillDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="会员编号">{{ viewForm.memberNo }}</el-descriptions-item>
        <el-descriptions-item label="会员姓名">{{ viewForm.memberName }}</el-descriptions-item>
        <el-descriptions-item label="秒杀份额">{{ viewForm.shares }}</el-descriptions-item>
        <el-descriptions-item label="已领取">{{ viewForm.claimedShares || 0 }}</el-descriptions-item>
        <el-descriptions-item label="待领取">{{ viewForm.remainingShares || 0 }}</el-descriptions-item>
        <el-descriptions-item label="秒杀单价">
          <span style="color: #F56C6C;">¥{{ viewForm.seckillPrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ getPaymentMethodName(viewForm.paymentMethod) }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">
          <span style="color: #67C23A;">¥{{ viewForm.totalAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="领取状态">
          <el-tag :type="getStatusTagType(viewForm.status)">
            {{ getStatusName(viewForm.status) }}
          </el-tag>
        </el-descriptions-item>
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

    <el-dialog title="领取秒杀记录" v-model="claimOpen" width="460px" append-to-body>
      <el-form ref="claimFormRef" :model="claimForm" :rules="claimRules" label-width="110px">
        <el-form-item label="会员">
          <span>{{ claimTarget.memberName }}（{{ claimTarget.memberNo }}）</span>
        </el-form-item>
        <el-form-item label="剩余可领">
          <span>{{ claimTarget.remainingShares || 0 }} 份</span>
        </el-form-item>
        <el-form-item label="本次领取" prop="claimShares">
          <el-input-number v-model="claimForm.claimShares" :min="1" :max="claimTarget.remainingShares || 1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="领取时间" prop="claimTime">
          <el-date-picker
            v-model="claimForm.claimTime"
            type="datetime"
            placeholder="选择领取时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="claimForm.remark" type="textarea" :rows="3" placeholder="可不填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitClaim">确 定</el-button>
          <el-button @click="claimOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog :title="recordClaimTitle" v-model="recordClaimOpen" width="720px" append-to-body>
      <el-table v-loading="recordClaimLoading" :data="recordClaimList" size="small" border>
        <el-table-column label="领取份额" align="center" prop="claimShares" width="100" />
        <el-table-column label="领取人" align="center" prop="claimBy" width="120" />
        <el-table-column label="领取时间" align="center" prop="claimTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.claimTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="recordClaimOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 统计弹窗 -->
    <el-dialog title="秒杀记录统计" v-model="statsOpen" width="750px" append-to-body>
      <el-divider content-position="left">总体统计</el-divider>
      <el-row :gutter="16" class="stats-cards">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">¥{{ statsData.totalAmount || '0.00' }}</div>
            <div class="stat-label">总金额</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ statsData.totalPeople || 0 }}</div>
            <div class="stat-label">参与人数</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ statsData.totalShares || 0 }}</div>
            <div class="stat-label">总份额</div>
          </el-card>
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-top: 16px;">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card claimed">
            <div class="stat-value">{{ statsData.claimedShares || 0 }}</div>
            <div class="stat-label">已领取份额</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card claimed">
            <div class="stat-value">{{ statsData.claimedPeople || 0 }}</div>
            <div class="stat-label">已领取人数</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card pending">
            <div class="stat-value">{{ (statsData.totalShares || 0) - (statsData.claimedShares || 0) }}</div>
            <div class="stat-label">待领取份额</div>
          </el-card>
        </el-col>
      </el-row>

      <el-divider content-position="left">按付款方式统计</el-divider>
      <el-table :data="statsData.paymentMethodStats || []" border stripe show-summary :summary-method="getSummaries">
        <el-table-column label="付款方式" prop="paymentMethodName" align="center" min-width="120" />
        <el-table-column label="记录数" prop="recordCount" align="center" width="90" />
        <el-table-column label="份额合计" prop="shares" align="center" width="110" />
        <el-table-column label="金额合计" prop="amount" align="center" width="130">
          <template #default="scope">
            <span style="color: #67C23A; font-weight: bold;">¥{{ scope.row.amount }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
        <el-button @click="statsOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 全员秒杀弹窗 -->
    <el-dialog title="全员秒杀" v-model="batchAllOpen" width="520px" append-to-body>
      <el-alert
        type="info"
        :closable="false"
        title="将为当前部门下所有有效会员（状态正常且未过期）批量生成秒杀记录，已生成当日记录的会员会自动跳过。"
        style="margin-bottom: 16px;"
      />
      <el-form :model="batchAllForm" :rules="batchAllRules" ref="batchAllFormRef" label-width="120px">
        <el-form-item label="秒杀活动" prop="seckillId">
          <el-select v-model="batchAllForm.seckillId" placeholder="请选择秒杀活动" style="width: 100%;">
            <el-option
              v-for="item in seckillActiveOptions"
              :key="item.seckillId"
              :label="item.seckillName"
              :value="item.seckillId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀参与日期" prop="seckillDate">
          <el-date-picker
            v-model="batchAllForm.seckillDate"
            type="date"
            placeholder="请选择秒杀参与日期"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="秒杀份额" prop="shares">
          <el-input-number v-model="batchAllForm.shares" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="付款方式" prop="paymentMethod">
          <el-select v-model="batchAllForm.paymentMethod" placeholder="请选择付款方式" style="width: 100%;">
            <el-option
              v-for="dictItem in paymentMethodDictOptions"
              :key="dictItem.value"
              :label="dictItem.label"
              :value="dictItem.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="batchAllForm.remark" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="batchAllLoading" @click="submitBatchAll">确 定</el-button>
          <el-button @click="batchAllOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from "element-plus"
import { parseTime } from "@/utils/junsong"
import { useDownload } from "@/composables/useDownload"
import { useDict } from "@/composables/useDict"
import { getDictDefaultValue } from "@/composables/useDict"
import { listSeckillRecord, getSeckillRecord, delSeckillRecord, addSeckillRecord, updateSeckillRecord, receiveRecord, listSeckill, listActiveSeckill, getSeckillRecordStatistics, listSeckillClaimRecord, batchSeckillForAll } from "@/api/member/seckillRecord"
import { getMemberByNo } from "@/api/member/member"
const { download } = useDownload()
const dict = useDict('finance_payment_method')

export default {
  name: "SeckillRecord",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      recordList: [],
      seckillOptions: [],
      seckillActiveOptions: [],
      title: "",
      open: false,
      viewOpen: false,
      statsOpen: false,
      claimOpen: false,
      claimLoading: false,
      recordClaimOpen: false,
      recordClaimLoading: false,
      recordClaimTitle: "领取记录",
      batchAllOpen: false,
      batchAllLoading: false,
      batchAllForm: {
        seckillId: undefined,
        seckillDate: undefined,
        paymentMethod: undefined,
        shares: 1,
        remark: undefined
      },
      batchAllRules: {
        seckillId: [
          { required: true, message: "请选择秒杀活动", trigger: "change" }
        ],
        seckillDate: [
          { required: true, message: "请选择秒杀参与日期", trigger: "change" }
        ]
      },
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        seckillId: undefined,
        memberNo: undefined,
        beginSeckillDate: undefined,
        endSeckillDate: undefined
      },
      form: {},
      viewForm: {},
      claimTarget: {},
      claimForm: {
        claimShares: 1,
        claimTime: undefined,
        remark: undefined
      },
      claimList: [],
      recordClaimList: [],
      statsData: {},
      rules: {
        seckillId: [
          { required: true, message: "秒杀活动不能为空", trigger: "change" }
        ],
        memberNo: [
          { required: true, message: "会员编号不能为空", trigger: "blur" }
        ],
        shares: [
          { required: true, message: "秒杀份额不能为空", trigger: "blur" }
        ],
        paymentMethod: [
          { required: true, message: "付款方式不能为空", trigger: "change" }
        ]
      },
      claimRules: {
        claimShares: [
          { required: true, message: "领取份额不能为空", trigger: "blur" }
        ],
        claimTime: [
          { required: true, message: "领取时间不能为空", trigger: "change" }
        ]
      },
      dict,
      legacyPaymentMethodOptions: {
        '1': '现金',
        '2': '微信支付',
        '3': '支付宝',
        '4': '银行卡',
        '5': '会员卡'
      }
    }
  },
  created() {
    this.getSeckillOptions()
    this.getSeckillActiveOptions()
  },
  computed: {
    paymentMethodDictOptions() {
      const options = [...(this.dict.type.finance_payment_method || [])]
      const existingLabels = new Set(options.map((item) => item.label))
      Object.entries(this.legacyPaymentMethodOptions).forEach(([value, label]) => {
        if (!options.some((item) => String(item.value) === String(value)) && existingLabels.has(label)) {
          options.push({ label, value })
        }
      })
      return options
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
        params.beginSeckillDate = this.dateRange[0]
        params.endSeckillDate = this.dateRange[1]
      }
      listSeckillRecord(params).then(response => {
        this.recordList = response.rows
        this.total = response.total
        this.loading = false
        this.getClaimList()
      })
    },
    getClaimList() {
      if (!this.queryParams.seckillId) {
        this.claimList = []
        return
      }
      this.claimLoading = true
      listSeckillClaimRecord({ pageNum: 1, pageSize: 1000, seckillId: this.queryParams.seckillId }).then(response => {
        this.claimList = response.rows || []
        this.claimLoading = false
      }).catch(() => {
        this.claimLoading = false
      })
    },
    nowDateTime() {
      const d = new Date()
      return d.getFullYear() + '-' +
        String(d.getMonth() + 1).padStart(2, '0') + '-' +
        String(d.getDate()).padStart(2, '0') + ' ' +
        String(d.getHours()).padStart(2, '0') + ':' +
        String(d.getMinutes()).padStart(2, '0') + ':' +
        String(d.getSeconds()).padStart(2, '0')
    },
    getSeckillOptions() {
      listSeckill({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.seckillOptions = response.rows
        if (!this.queryParams.seckillId && this.seckillOptions.length) {
          this.queryParams.seckillId = this.seckillOptions[0].seckillId
        }
        this.getList()
      })
    },
    getSeckillActiveOptions() {
      listActiveSeckill().then(response => {
        this.seckillActiveOptions = response.data || []
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        recordId: undefined,
        seckillId: undefined,
        seckillName: undefined,
        seckillDate: undefined,
        seckillPrice: undefined,
        memberNo: undefined,
        memberName: undefined,
        shares: 1,
        paymentMethod: undefined,
        totalAmount: undefined,
        status: '0',
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
      // 自动带入查询条件中选择的秒杀活动
      if (this.queryParams.seckillId) {
        this.form.seckillId = this.queryParams.seckillId
        this.handleSeckillChange(this.queryParams.seckillId)
      }
      this.open = true
      this.title = "添加秒杀记录"
    },
    handleBatchAll() {
      const dictOptions = this.paymentMethodDictOptions || []
      const defaultPayment = getDictDefaultValue(dictOptions, dictOptions[0]?.value)
      this.batchAllForm = {
        seckillId: this.queryParams.seckillId || undefined,
        seckillDate: this.parseTime(new Date(), '{y}-{m}-{d}'),
        paymentMethod: defaultPayment,
        shares: 1,
        remark: undefined
      }
      this.batchAllOpen = true
    },
    submitBatchAll() {
      this.$refs.batchAllFormRef.validate(valid => {
        if (!valid) return
        this.batchAllLoading = true
        batchSeckillForAll(this.batchAllForm).then(res => {
          const data = res.data || {}
          ElMessageBox.alert(
            `共匹配 ${data.totalNum || 0} 名有效会员，成功生成 ${data.successNum || 0} 条秒杀记录，跳过 ${data.skippedNum || 0} 条（已存在当日记录）。`,
            '全员秒杀完成',
            { type: 'success' }
          )
          this.batchAllOpen = false
          this.getList()
        }).finally(() => {
          this.batchAllLoading = false
        })
      })
    },
    handleView(row) {
      getSeckillRecord(row.recordId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    handleUpdate(row) {
      this.reset()
      const recordId = row.recordId || this.ids
      getSeckillRecord(recordId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改秒杀记录"
      })
    },
    handleSeckillChange(seckillId) {
      const seckill = this.seckillActiveOptions.find(item => item.seckillId === seckillId)
      if (seckill) {
        this.form.seckillName = seckill.seckillName
        this.form.seckillDate = seckill.seckillDate
        this.form.seckillPrice = seckill.seckillPrice
        this.calculateAmount()
      }
    },
    handleMemberSearch() {
      if (this.form.memberNo) {
        getMemberByNo(this.form.memberNo).then(response => {
          if (response.data) {
            this.form.memberName = response.data.memberName
          } else {
            ElMessage.warning("未找到该会员编号（仅限当前机构会员）")
            this.form.memberId = undefined
            this.form.memberName = ''
          }
        }).catch(() => {
          ElMessage.error("未找到该会员编号（仅限当前机构会员）")
          this.form.memberId = undefined
          this.form.memberName = ''
        })
      }
    },
    calculateAmount() {
      if (this.form.seckillPrice && this.form.shares) {
        this.form.totalAmount = (this.form.seckillPrice * this.form.shares).toFixed(2)
      }
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != undefined) {
            updateSeckillRecord(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addSeckillRecord(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleReceive(row) {
      this.claimTarget = row
      this.claimForm = {
        claimShares: row.remainingShares || 1,
        claimTime: this.nowDateTime(),
        remark: undefined
      }
      this.claimOpen = true
    },
    openRecordClaims(row) {
      this.recordClaimOpen = true
      this.recordClaimTitle = `${row.memberName || row.memberNo || '会员'}的领取记录`
      this.recordClaimLoading = true
      listSeckillClaimRecord({ pageNum: 1, pageSize: 1000, recordId: row.recordId }).then(response => {
        this.recordClaimList = response.rows || []
        this.recordClaimLoading = false
      }).catch(() => {
        this.recordClaimLoading = false
      })
    },
    submitClaim() {
      this.$refs["claimFormRef"].validate(valid => {
        if (!valid) return
        receiveRecord(this.claimTarget.recordId, this.claimForm).then(() => {
          this.claimOpen = false
          this.getList()
          ElMessage.success("领取成功")
        })
      })
    },
    handleDelete(row) {
      const recordIds = row.recordId || this.ids
      ElMessageBox.confirm('是否确认删除秒杀记录编号为"' + recordIds + '"的数据项？').then(function() {
        return delSeckillRecord(recordIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('member/seckillRecord/export', {
        ...this.queryParams
      }, `seckillRecord_${new Date().getTime()}.xlsx`)
    },
    handleStatistics() {
      const params = {
        ...this.queryParams
      }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginTime = this.dateRange[0]
        params.endTime = this.dateRange[1]
      }
      getSeckillRecordStatistics(params).then(response => {
        this.statsData = response.data
        // 处理数值类型（BigDecimal转字符串）
        if (this.statsData.totalAmount != null) {
          this.statsData.totalAmount = Number(this.statsData.totalAmount).toFixed(2)
        }
        if (this.statsData.paymentMethodStats) {
          this.statsData.paymentMethodStats.forEach(item => {
            if (item.amount != null) item.amount = Number(item.amount).toFixed(2)
            if (item.shares != null) item.shares = Number(item.shares)
            if (item.recordCount != null) item.recordCount = Number(item.recordCount)
          })
        }
        this.statsOpen = true
      })
    },
    getSummaries(param) {
      const { columns, data } = param
      const sums = []
      columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = '合计'
          return
        }
        if (index === 1) {
          const values = data.map(item => Number(item.recordCount))
          sums[index] = values.reduce((prev, curr) => prev + curr, 0)
        } else if (index === 2) {
          const values = data.map(item => Number(item.shares))
          sums[index] = values.reduce((prev, curr) => prev + curr, 0)
        } else if (index === 3) {
          const values = data.map(item => parseFloat(item.amount))
          sums[index] = '¥' + values.reduce((prev, curr) => prev + curr, 0).toFixed(2)
        } else {
          sums[index] = ''
        }
      })
      return sums
    },
    getPaymentMethodName(method) {
      const hit = this.paymentMethodDictOptions.find((item) => String(item.value) === String(method))
      return hit?.label || method || '-'
    },
    getStatusName(value) {
      const map = { '0': '待领取', '1': '已领取', '2': '已取消', '3': '部分领取' }
      return map[value] || value
    },
    getStatusTagType(value) {
      const map = { '0': 'warning', '1': 'success', '2': 'danger', '3': 'primary' }
      return map[value] || 'info'
    },
    canReceive(row) {
      return row.status !== '2' && Number(row.remainingShares || 0) > 0
    }
  }
}
</script>

<style scoped>
.stats-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  font-weight: bold;
  letter-spacing: 1px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.45);
  transition: all 0.3s ease;
}
.stats-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.6);
  background: linear-gradient(135deg, #5a6fd6 0%, #6a4296 100%);
}
.stat-card {
  text-align: center;
  padding: 10px 0;
}
.stat-card .stat-value {
  font-size: 26px;
  font-weight: bold;
  color: #303133;
}
.stat-card .stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}
.stat-card.claimed .stat-value {
  color: #67C23A;
}
.stat-card.pending .stat-value {
  color: #E6A23C;
}
.stats-cards .el-card {
  margin-bottom: 0;
}
.status-link {
  cursor: pointer;
}
</style>
