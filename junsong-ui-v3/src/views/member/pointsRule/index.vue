<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="规则名称" prop="ruleName">
        <el-input
          v-model="queryParams.ruleName"
          placeholder="请输入规则名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="规则代码" prop="ruleCode">
        <el-input
          v-model="queryParams.ruleCode"
          placeholder="请输入规则代码"
          clearable
          @keyup.enter="handleQuery"
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
          v-hasPermi="['member:pointsRule:add']"
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
          v-hasPermi="['member:pointsRule:edit']"
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
          v-hasPermi="['member:pointsRule:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:pointsRule:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="规则代码" align="center" prop="ruleCode" width="150" />
      <el-table-column label="规则名称" align="center" prop="ruleName" width="200" />
      <el-table-column label="计算方式" align="center" prop="ruleType" width="120">
        <template #default="scope">
          <span>{{ getRuleTypeName(scope.row.ruleType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="每积分消费" align="center" prop="pointsPerYuan" width="130">
        <template #default="scope">
          <span style="color: #E6A23C;">{{ scope.row.pointsPerYuan }}元/积分</span>
        </template>
      </el-table-column>
      <el-table-column label="积分有效期" align="center" prop="validityDays" width="120">
        <template #default="scope">
          <span>{{ scope.row.validityDays }}天</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
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
            v-hasPermi="['member:pointsRule:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:pointsRule:remove']"
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

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则代码" prop="ruleCode">
          <el-input v-model="form.ruleCode" placeholder="请输入规则代码" />
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="计算方式" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择计算方式" style="width: 100%;">
            <el-option label="进一法" value="1" />
            <el-option label="四舍五入" value="2" />
            <el-option label="舍零取整" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="每积分消费" prop="pointsPerYuan">
          <el-input-number v-model="form.pointsPerYuan" :min="0" :precision="2" :step="1" style="width: 100%;" />
          <span style="color: #909399; margin-left: 10px;">元/积分（如：1000表示消费1000元积1分）</span>
        </el-form-item>
        <el-form-item label="积分有效期" prop="validityDays">
          <el-input-number v-model="form.validityDays" :min="0" style="width: 100%;" />
          <span style="color: #909399; margin-left: 10px;">天（0表示永久有效）</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="积分规则详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="规则代码">{{ viewForm.ruleCode }}</el-descriptions-item>
        <el-descriptions-item label="规则名称">{{ viewForm.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="计算方式">{{ getRuleTypeName(viewForm.ruleType) }}</el-descriptions-item>
        <el-descriptions-item label="每积分消费">
          <span style="color: #E6A23C;">{{ viewForm.pointsPerYuan }}元/积分</span>
        </el-descriptions-item>
        <el-descriptions-item label="积分有效期">
          <span>{{ viewForm.validityDays }}天</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="dict.type.sys_normal_disable" :value="viewForm.status"/>
        </el-descriptions-item>
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
import { useDict, getDictDefaultValue } from "@/composables/useDict"
import { listPointsRule, getPointsRule, delPointsRule, addPointsRule, updatePointsRule } from "@/api/member/pointsRule"
const { download } = useDownload()

export default {
  name: "PointsRule",
  setup() {
    const dict = useDict('sys_normal_disable')
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
      ruleList: [],
      title: "",
      open: false,
      viewOpen: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ruleName: undefined,
        ruleCode: undefined
      },
      form: {},
      viewForm: {},
      rules: {
        ruleCode: [
          { required: true, message: "规则代码不能为空", trigger: "blur" }
        ],
        ruleName: [
          { required: true, message: "规则名称不能为空", trigger: "blur" }
        ],
        ruleType: [
          { required: true, message: "计算方式不能为空", trigger: "change" }
        ],
        pointsPerYuan: [
          { required: true, message: "每积分消费不能为空", trigger: "blur" }
        ],
        validityDays: [
          { required: true, message: "积分有效期不能为空", trigger: "blur" }
        ]
      },
      ruleTypeOptions: {
        '1': '进一法',
        '2': '四舍五入',
        '3': '舍零取整'
      }
    }
  },
  created() {
    this.getList()
  },
  watch: {
    'dict.type.sys_normal_disable': {
      handler() {
        if (this.open && this.title === "添加积分规则" && !this.form.status) {
          this.form.status = getDictDefaultValue(this.dict.type.sys_normal_disable, "0")
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
    getList() {
      this.loading = true
      listPointsRule(this.queryParams).then(response => {
        this.ruleList = response.rows
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
        ruleId: undefined,
        ruleCode: undefined,
        ruleName: undefined,
        ruleType: undefined,
        pointsPerYuan: undefined,
        validityDays: undefined,
        status: "0"
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
      this.ids = selection.map(item => item.ruleId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.form.status = getDictDefaultValue(this.dict.type.sys_normal_disable, "0")
      this.open = true
      this.title = "添加积分规则"
    },
    handleView(row) {
      getPointsRule(row.ruleId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    handleUpdate(row) {
      this.reset()
      const ruleId = row.ruleId || this.ids
      getPointsRule(ruleId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改积分规则"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.ruleId != undefined) {
            updatePointsRule(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPointsRule(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const ruleIds = row.ruleId || this.ids
      ElMessageBox.confirm('是否确认删除规则代码为"' + ruleIds + '"的数据项？').then(function() {
        return delPointsRule(ruleIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('member/pointsRule/export', {
        ...this.queryParams
      }, `pointsRule_${new Date().getTime()}.xlsx`)
    },
    getRuleTypeName(type) {
      return this.ruleTypeOptions[type] || '-'
    }
  }
}
</script>

<style scoped>
</style>
