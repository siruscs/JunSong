<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="会员编号" prop="memberNo">
        <el-input
          v-model="queryParams.memberNo"
          placeholder="请输入会员编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="姓名" prop="memberName">
        <el-input
          v-model="queryParams.memberName"
          placeholder="请输入姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="会员卡类型" prop="cardType">
        <el-select v-model="queryParams.cardType" placeholder="请选择会员卡类型" clearable style="width: 150px;">
          <el-option
            v-for="dict in dict.type.mem_card_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['member:member:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['member:member:edit']"
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
          v-hasPermi="['member:member:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:member:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Upload"
          size="small"
          @click="handleImport"
          v-hasPermi="['member:member:import']"
        >导入</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="memberList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="会员编号" align="center" prop="memberNo" width="120" />
      <el-table-column label="姓名" align="center" prop="memberName" width="100" />
      <el-table-column label="手机号" align="center" prop="phone" width="120" />
      <el-table-column label="年龄" align="center" prop="age" width="80" />
      <el-table-column label="会员卡类型" align="center" prop="cardType" width="120">
        <template #default="scope">
          <span>{{ getCardTypeName(scope.row.cardType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="积分" align="center" prop="availablePoints" width="100">
        <template #default="scope">
          <span style="color: #E6A23C;">{{ scope.row.availablePoints || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期至" align="center" prop="expireDate" width="120">
        <template #default="scope">
          <span :style="{ color: isExpiringSoon(scope.row.expireDate) ? '#F56C6C' : '' }">
            {{ parseTime(scope.row.expireDate, '{y}-{m}-{d}') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusName(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="入会日期" align="center" prop="joinDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.joinDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
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
            v-hasPermi="['member:member:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Refresh"
            @click="handleRenew(scope.row)"
            v-hasPermi="['member:member:renew']"
          >续卡</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="CircleClose"
            @click="handleCancel(scope.row)"
            v-hasPermi="['member:member:cancel']"
          >退卡</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Remove"
            @click="handleInvalid(scope.row)"
            v-hasPermi="['member:member:invalid']"
          >设置无效</el-button>
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

    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会员编号">
              <el-input v-model="form.memberNo" placeholder="请输入会员编号" readonly>
                <template #prefix><el-icon><Key /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="memberName">
              <el-input v-model="form.memberName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="150" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会员卡类型" prop="cardType">
              <el-select v-model="form.cardType" placeholder="请选择" style="width: 100%;">
                <el-option
                  v-for="dict in dict.type.mem_card_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="入会日期" prop="joinDate">
              <el-date-picker
                v-model="form.joinDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期至" prop="expireDate">
              <el-date-picker
                v-model="form.expireDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="住址" prop="address">
          <el-input v-model="form.address" placeholder="请输入住址" />
        </el-form-item>
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

    <el-dialog title="会员详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="会员编号">{{ viewForm.memberNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ viewForm.memberName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ viewForm.phone }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ viewForm.age }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ viewForm.idCard }}</el-descriptions-item>
        <el-descriptions-item label="住址">{{ viewForm.address }}</el-descriptions-item>
        <el-descriptions-item label="会员卡类型">{{ getCardTypeName(viewForm.cardType) }}</el-descriptions-item>
        <el-descriptions-item label="积分">
          <span style="color: #E6A23C;">{{ viewForm.availablePoints || 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="入会日期">{{ parseTime(viewForm.joinDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="有效期至">
          <span :style="{ color: isExpiringSoon(viewForm.expireDate) ? '#F56C6C' : '' }">
            {{ parseTime(viewForm.expireDate, '{y}-{m}-{d}') }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(viewForm.status)">{{ getStatusName(viewForm.status) }}</el-tag>
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

    <el-dialog title="续卡" v-model="renewOpen" width="500px" append-to-body>
      <el-form ref="renewForm" :model="renewForm" :rules="renewRules" label-width="100px">
        <el-form-item label="会员编号">
          <el-input v-model="renewForm.memberNo" disabled />
        </el-form-item>
        <el-form-item label="会员姓名">
          <el-input v-model="renewForm.memberName" disabled />
        </el-form-item>
        <el-form-item label="当前有效期至">
          <el-input v-model="renewForm.currentValidityDate" disabled />
        </el-form-item>
        <el-form-item label="续卡时长" prop="renewMonths">
          <el-select v-model="renewForm.renewMonths" placeholder="请选择续卡时长" style="width: 100%;">
            <el-option label="1个月" :value="1" />
            <el-option label="3个月" :value="3" />
            <el-option label="6个月" :value="6" />
            <el-option label="12个月" :value="12" />
          </el-select>
        </el-form-item>
        <el-form-item label="新有效期至">
          <el-input v-model="renewForm.newValidityDate" disabled />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="renewForm.remark" type="textarea" placeholder="请输入备注" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitRenew">确 定</el-button>
        <el-button @click="renewOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <ExcelImportDialog
      ref="importMemberRef"
      title="会员信息导入"
      action="/member/member/importData"
      template-action="/member/member/importTemplate"
      template-file-name="member_template"
      update-support-label="是否更新已经存在的会员数据"
      @success="getList"
    />
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from "element-plus"
import { parseTime } from "@/utils/junsong"
import { useDownload } from "@/composables/useDownload"
import { useDict, getDictDefaultValue } from "@/composables/useDict"
import { listMember, getMember, delMember, addMember, updateMember, renewMember, cancelMember, invalidMember, getNextMemberNo } from "@/api/member/member"
import ExcelImportDialog from '@/components/ExcelImportDialog/index.vue'
const { download } = useDownload()

export default {
  name: "Member",
  components: {
    ExcelImportDialog
  },
  setup() {
    const dict = useDict('mem_card_type')
    return { dict }
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      memberList: [],
      title: "",
      open: false,
      viewOpen: false,
      renewOpen: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        memberNo: undefined,
        memberName: undefined,
        phone: undefined
      },
      form: {},
      viewForm: {},
      renewForm: {},
      importMemberRef: null,
      rules: {
        memberName: [
          { required: true, message: "姓名不能为空", trigger: "blur" }
        ],
        memberNo: [
          { required: true, message: "会员编号不能为空", trigger: "blur" }
        ],
        phone: [
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ],
        cardType: [
          { required: true, message: "会员卡类型不能为空", trigger: "change" }
        ],
        joinDate: [
          { required: true, message: "入会日期不能为空", trigger: "change" }
        ]
      },
      renewRules: {
        renewMonths: [
          { required: true, message: "续卡时长不能为空", trigger: "change" }
        ]
      },
      cardTypeOptions: {},
      statusOptions: {
        '0': '正常',
        '1': '已无效',
        '2': '已退卡'
      }
    }
  },
  created() {
    this.getList()
  },
  mounted() {
    // 初始化cardTypeOptions
    this.initCardTypeOptions()
  },
  watch: {
    // 监听字典数据变化
    'dict.type.mem_card_type': {
      handler() {
        this.initCardTypeOptions()
        if (this.open && this.title === "添加会员" && !this.form.cardType) {
          this.form.cardType = getDictDefaultValue(this.dict.type.mem_card_type, undefined)
        }
      },
      immediate: true
    }
  },
  methods: {
    parseTime,
    resetForm(formName) {
      this.$refs[formName]?.resetFields?.()
    },
    download,
    getTodayDate() {
      const today = new Date()
      const year = today.getFullYear()
      const month = String(today.getMonth() + 1).padStart(2, '0')
      const day = String(today.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    // 初始化会员卡类型选项
    initCardTypeOptions() {
      if (this.dict && this.dict.type && this.dict.type.mem_card_type) {
        this.cardTypeOptions = {}
        this.dict.type.mem_card_type.forEach(dict => {
          this.cardTypeOptions[dict.value] = dict.label
        })
      }
    },
    getList() {
      this.loading = true
      listMember(this.queryParams).then(response => {
        this.memberList = response.rows
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
        memberId: undefined,
        memberNo: undefined,
        memberName: undefined,
        phone: undefined,
        age: undefined,
        idCard: undefined,
        address: undefined,
        cardType: undefined,
        totalPoints: 0,
        availablePoints: 0,
        joinDate: undefined,
        expireDate: undefined,
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
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.memberId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.form.cardType = getDictDefaultValue(this.dict.type.mem_card_type, undefined)
      this.form.joinDate = this.getTodayDate()
      this.title = "添加会员"
      // 自动获取下一个会员编号
      this.fetchNextMemberNo()
      this.open = true
    },
    // 获取下一个会员编号
    fetchNextMemberNo() {
      getNextMemberNo().then(response => {
        this.form.memberNo = response.data
      })
    },
    handleView(row) {
      getMember(row.memberId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    handleUpdate(row) {
      this.reset()
      const memberId = row.memberId || this.ids
      getMember(memberId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改会员"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.memberId != undefined) {
            updateMember(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addMember(this.form).then((response) => {
              const memberName = response.data.memberName || this.form.memberName
              const memberNo = response.data.memberNo || this.form.memberNo
              this.$alert(
                '<div style="text-align:center;padding:10px 0;">' +
                '<p style="font-size:18px;font-weight:bold;color:#409EFF;margin-bottom:16px;">会员创建成功</p>' +
                '<p style="font-size:16px;margin-bottom:8px;">会员姓名：<b>' + memberName + '</b></p>' +
                '<p style="font-size:16px;margin-bottom:8px;">会员编号：<b style="color:#F56C6C;font-size:20px;letter-spacing:2px;">' + memberNo + '</b></p>' +
                '</div>',
                '创建成功',
                { dangerouslyUseHTMLString: true, confirmButtonText: '我知道了', type: 'success' }
              ).then(() => {
                this.open = false
                this.getList()
              })
            })
          }
        }
      })
    },
    handleDelete(row) {
      const memberIds = row.memberId || this.ids
      ElMessageBox.confirm('是否确认删除会员编号为"' + memberIds + '"的数据项？').then(function() {
        return delMember(memberIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('member/member/export', {
        ...this.queryParams
      }, `member_${new Date().getTime()}.xlsx`)
    },
    handleImport() {
      this.$refs.importMemberRef.open()
    },
    handleRenew(row) {
      this.renewForm = {
        memberId: row.memberId,
        memberNo: row.memberNo,
        memberName: row.memberName,
        currentValidityDate: this.parseTime(row.expireDate, '{y}-{m}-{d}'),
        renewMonths: 12,
        newValidityDate: '',
        remark: ''
      }
      this.updateNewValidityDate()
      this.renewOpen = true
    },
    updateNewValidityDate() {
      if (this.renewForm.currentValidityDate && this.renewForm.renewMonths) {
        const currentDate = new Date(this.renewForm.currentValidityDate)
        currentDate.setMonth(currentDate.getMonth() + this.renewForm.renewMonths)
        this.renewForm.newValidityDate = this.parseTime(currentDate, '{y}-{m}-{d}')
      }
    },
    submitRenew() {
      this.$refs["renewForm"].validate(valid => {
        if (valid) {
          renewMember(this.renewForm).then(() => {
            ElMessage.success("续卡成功")
            this.renewOpen = false
            this.getList()
          })
        }
      })
    },
    handleCancel(row) {
      ElMessageBox.confirm('是否确认退卡会员编号为"' + row.memberNo + '"的会员？').then(() => {
        return cancelMember(row.memberId)
      }).then(() => {
        this.getList()
        ElMessage.success("退卡成功")
      }).catch(() => {})
    },
    handleInvalid(row) {
      ElMessageBox.confirm('是否确认设置会员编号为"' + row.memberNo + '"的会员为无效状态？').then(() => {
        return invalidMember(row.memberId)
      }).then(() => {
        this.getList()
        ElMessage.success("设置无效成功")
      }).catch(() => {})
    },
    getCardTypeName(cardType) {
      return this.cardTypeOptions[cardType] || '-'
    },
    getStatusName(status) {
      return this.statusOptions[status] || '-'
    },
    getStatusType(status) {
      const types = {
        '0': 'success',
        '1': 'danger',
        '2': 'warning'
      }
      return types[status] || 'info'
    },
    isExpiringSoon(validityDate) {
      if (!validityDate) return false
      const now = new Date()
      const expDate = new Date(validityDate)
      const diffDays = (expDate - now) / (1000 * 60 * 60 * 24)
      return diffDays <= 30 && diffDays >= 0
    }
  }
}
</script>

<style scoped>
</style>
