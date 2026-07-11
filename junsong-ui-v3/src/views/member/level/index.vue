<template>
  <div class="app-container">
    <!-- 等级配置 -->
    <el-card shadow="never" class="mb8">
      <template #header>
        <div class="card-header">
          <span>等级配置</span>
          <el-button
            type="primary"
            plain
            icon="Plus"
            size="small"
            @click="handleAdd"
            v-hasPermi="['member:level:add']"
          >新增等级</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="levelList">
        <el-table-column label="等级名称" align="center" prop="typeName" width="140" />
        <el-table-column label="等级编码" align="center" prop="typeCode" width="140" />
        <el-table-column label="办卡费用" align="center" prop="cardFee" width="120">
          <template #default="scope">
            <span style="color: #409EFF;">¥{{ scope.row.cardFee || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="折扣率" align="center" prop="discountRate" width="120">
          <template #default="scope">
            <span>{{ formatDiscount(scope.row.discountRate) }}折</span>
          </template>
        </el-table-column>
        <el-table-column label="积分倍率" align="center" prop="pointsRate" width="120">
          <template #default="scope">
            <span style="color: #E6A23C;">{{ scope.row.pointsRate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="升级成长值" align="center" prop="minGrowth" width="130">
          <template #default="scope">
            <span style="color: #67C23A;">{{ scope.row.minGrowth || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="签到积分" align="center" prop="signInPoints" width="120">
          <template #default="scope">
            <span style="color: #409EFF;">{{ scope.row.signInPoints ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
          <template #default="scope">
            <el-button
              size="small"
              link type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['member:level:edit']"
            >修改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 成长规则配置 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>成长规则配置</span>
        </div>
      </template>
      <el-form ref="ruleForm" :model="ruleForm" :rules="ruleRules" label-width="120px" style="max-width: 600px;">
        <el-form-item label="签到成长值" prop="signInGrowth">
          <el-input-number v-model="ruleForm.signInGrowth" :min="0" :precision="0" :step="1" style="width: 100%;" />
          <span class="form-tip">每次签到获得的成长值</span>
        </el-form-item>
        <el-form-item label="消费成长倍率" prop="saleGrowthRatio">
          <el-input-number v-model="ruleForm.saleGrowthRatio" :min="0" :precision="2" :step="0.1" style="width: 100%;" />
          <span class="form-tip">每消费1元获得的成长值</span>
        </el-form-item>
        <el-form-item label="是否启用衰减" prop="decayEnabled">
          <el-radio-group v-model="ruleForm.decayEnabled">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="不活跃天数" prop="inactiveDays">
          <el-input-number v-model="ruleForm.inactiveDays" :min="0" :precision="0" :step="1" style="width: 100%;" />
          <span class="form-tip">超过该天数未消费开始衰减</span>
        </el-form-item>
        <el-form-item label="衰减比例" prop="decayRatio">
          <el-input-number v-model="ruleForm.decayRatio" :min="0" :max="1" :precision="2" :step="0.05" style="width: 100%;" />
          <span class="form-tip">衰减比例(0-1，如0.5表示衰减50%)</span>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="submitRuleForm"
            v-hasPermi="['member:growth:edit']"
          >保 存</el-button>
          <el-button @click="resetRuleForm">重 置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 等级配置编辑弹窗 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="等级名称" prop="typeName">
          <el-input v-model="form.typeName" placeholder="请输入等级名称" />
        </el-form-item>
        <el-form-item label="等级编码" prop="typeCode">
          <el-input v-model="form.typeCode" placeholder="请输入等级编码" :disabled="form.typeId != null" />
        </el-form-item>
        <el-form-item label="办卡费用" prop="cardFee">
          <el-input-number v-model="form.cardFee" :min="0" :precision="2" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0" :max="10" :precision="1" :step="0.5" style="width: 100%;" />
          <span class="form-tip">如：8.5表示8.5折</span>
        </el-form-item>
        <el-form-item label="积分倍率" prop="pointsRate">
          <el-input-number v-model="form.pointsRate" :min="0" :precision="2" :step="0.1" style="width: 100%;" />
          <span class="form-tip">如：1.5表示获得1.5倍积分</span>
        </el-form-item>
        <el-form-item label="升级成长值" prop="minGrowth">
          <el-input-number v-model="form.minGrowth" :min="0" :precision="0" :step="100" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="签到积分" prop="signInPoints">
          <el-input-number v-model="form.signInPoints" :min="0" :precision="2" :step="1" style="width: 100%;" />
          <span class="form-tip">该等级会员每次签到获得的积分，如 STAR1=2，STAR3=3</span>
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
  </div>
</template>

<script>
import { ElMessage } from "element-plus"
import { useDict, getDictDefaultValue } from "@/composables/useDict"
import { listLevel, getLevel, addLevel, updateLevel } from "@/api/member/level"
import { getGrowthRule, updateGrowthRule } from "@/api/member/growth"

export default {
  name: "Level",
  setup() {
    const dict = useDict('sys_normal_disable')
    return { dict }
  },
  data() {
    return {
      loading: true,
      levelList: [],
      title: "",
      open: false,
      form: {},
      ruleForm: {
        signInGrowth: 0,
        saleGrowthRatio: 0,
        decayEnabled: '0',
        inactiveDays: 0,
        decayRatio: 0
      },
      rules: {
        typeName: [
          { required: true, message: "等级名称不能为空", trigger: "blur" }
        ],
        typeCode: [
          { required: true, message: "等级编码不能为空", trigger: "blur" }
        ],
        cardFee: [
          { required: true, message: "办卡费用不能为空", trigger: "blur" }
        ],
        discountRate: [
          { required: true, message: "折扣率不能为空", trigger: "blur" }
        ],
        pointsRate: [
          { required: true, message: "积分倍率不能为空", trigger: "blur" }
        ],
        minGrowth: [
          { required: true, message: "升级成长值不能为空", trigger: "blur" }
        ],
        signInPoints: [
          { required: true, message: "签到积分不能为空", trigger: "blur" }
        ]
      },
      ruleRules: {
        signInGrowth: [
          { required: true, message: "签到成长值不能为空", trigger: "blur" }
        ],
        saleGrowthRatio: [
          { required: true, message: "消费成长倍率不能为空", trigger: "blur" }
        ]
      }
    }
  },
  watch: {
    'dict.type.sys_normal_disable': {
      handler() {
        if (this.open && !this.form.status) {
          this.form.status = getDictDefaultValue(this.dict.type.sys_normal_disable, "0")
        }
      },
      immediate: true
    }
  },
  created() {
    this.getLevelList()
    this.getRule()
  },
  methods: {
    resetForm(formName) {
      this.$refs[formName]?.resetFields?.()
    },
    formatDiscount(rate) {
      if (rate == null) return '10.0'
      return Number((rate * 10).toFixed(1))
    },
    getLevelList() {
      this.loading = true
      listLevel({}).then(response => {
        this.levelList = response.data || []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    getRule() {
      getGrowthRule().then(response => {
        if (response.code === 200 && response.data) {
          this.ruleForm = { ...this.ruleForm, ...response.data }
        }
      })
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增等级配置"
    },
    handleUpdate(row) {
      this.reset()
      getLevel(row.typeCode).then(response => {
        const data = response.data
        if (data.discountRate != null) {
          data.discountRate = Number((data.discountRate * 10).toFixed(1))
        }
        this.form = data
        this.open = true
        this.title = "修改等级配置"
      })
    },
    reset() {
      this.form = {
        typeId: undefined,
        typeCode: undefined,
        typeName: undefined,
        cardFee: 0,
        discountRate: 10,
        pointsRate: 1,
        minGrowth: 0,
        signInPoints: 1,
        status: "0"
      }
      this.resetForm("form")
    },
    cancel() {
      this.open = false
      this.reset()
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const payload = { ...this.form }
          if (payload.discountRate != null) {
            payload.discountRate = Number((payload.discountRate / 10).toFixed(2))
          }
          if (this.form.typeId != null) {
            updateLevel(payload).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getLevelList()
            })
          } else {
            addLevel(payload).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getLevelList()
            })
          }
        }
      })
    },
    submitRuleForm() {
      this.$refs["ruleForm"].validate(valid => {
        if (valid) {
          const { signInPoints, ...payload } = this.ruleForm
          updateGrowthRule(payload).then(() => {
            ElMessage.success("保存成功")
            this.getRule()
          })
        }
      })
    },
    resetRuleForm() {
      this.getRule()
    }
  }
}
</script>

<style scoped>
.card-header {
  font-weight: bold;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}
.mb8 {
  margin-bottom: 12px;
}
</style>
