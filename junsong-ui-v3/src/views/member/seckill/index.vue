<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="秒杀名称" prop="seckillName">
        <el-input
          v-model="queryParams.seckillName"
          placeholder="请输入秒杀名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类型" prop="seckillType">
        <el-select v-model="queryParams.seckillType" placeholder="请选择" clearable>
          <el-option label="秒杀" value="1" />
          <el-option label="团购" value="2" />
        </el-select>
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
          v-hasPermi="['member:seckill:add']"
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
          v-hasPermi="['member:seckill:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="SwitchButton"
          size="small"
          :disabled="single"
          @click="handleClose"
          v-hasPermi="['member:seckill:edit']"
        >结束</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['member:seckill:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:seckill:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="seckillList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="秒杀编号" align="center" prop="seckillNo" width="120" />
      <el-table-column label="秒杀名称" align="center" prop="seckillName" width="180" />
      <el-table-column label="类型" align="center" prop="seckillType" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.seckillType === '1' ? 'danger' : 'success'">
            {{ scope.row.seckillType === '1' ? '秒杀' : '团购' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="秒杀日期" align="center" prop="seckillDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.seckillDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时间段" align="center" prop="timeSlot" width="120" />
      <el-table-column label="秒杀金额" align="center" prop="seckillAmount" width="120">
        <template #default="scope">
          <span style="color: #F56C6C;">¥{{ scope.row.seckillAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="秒杀单价" align="center" prop="seckillPrice" width="120">
        <template #default="scope">
          <span style="color: #67C23A;">¥{{ scope.row.seckillPrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总份额" align="center" prop="totalShares" width="100" />
      <el-table-column label="剩余份额" align="center" prop="remainShares" width="100">
        <template #default="scope">
          <span style="color: #E6A23C;">{{ scope.row.remainShares }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusName(scope.row.status) }}</el-tag>
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
            v-hasPermi="['member:seckill:edit']"
          >修改</el-button>
          <el-button
            v-if="scope.row.status === '0'"
            size="small"
            link type="primary"
            icon="SwitchButton"
            @click="handleClose(scope.row)"
            v-hasPermi="['member:seckill:edit']"
          >结束</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:seckill:remove']"
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

    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="秒杀编号">
              <el-input v-model="form.seckillNo" placeholder="系统自动生成" disabled>
                <template #prefix><el-icon><Key /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="秒杀名称" prop="seckillName">
              <el-input v-model="form.seckillName" placeholder="请输入秒杀名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类型" prop="seckillType">
              <el-select v-model="form.seckillType" placeholder="请选择" style="width: 100%;">
                <el-option label="秒杀" value="1" />
                <el-option label="团购" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="秒杀日期" prop="seckillDateRange">
              <el-date-picker
                v-model="form.seckillDateRange"
                type="daterange"
                range-separator="-"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="时间段" prop="timeSlot">
              <el-time-picker
                is-range
                v-model="form.timeSlotRange"
                range-separator="-"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                placeholder="选择时间范围"
                style="width: 100%;"
                format="HH:mm"
                value-format="HH:mm"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="秒杀金额" prop="seckillAmount">
              <el-input-number v-model="form.seckillAmount" :precision="2" :step="0.01" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="秒杀单价" prop="seckillPrice">
              <el-input-number v-model="form.seckillPrice" :precision="2" :step="0.01" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总份额" prop="totalShares">
              <el-input-number v-model="form.totalShares" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="政策" prop="policy">
          <el-input v-model="form.policy" type="textarea" placeholder="请输入政策" :rows="3" />
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

    <el-dialog title="秒杀详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="秒杀编号">{{ viewForm.seckillNo }}</el-descriptions-item>
        <el-descriptions-item label="秒杀名称">{{ viewForm.seckillName }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="viewForm.seckillType === '1' ? 'danger' : 'success'">
            {{ viewForm.seckillType === '1' ? '秒杀' : '团购' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="秒杀日期">{{ parseTime(viewForm.seckillDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="时间段">{{ viewForm.timeSlot }}</el-descriptions-item>
        <el-descriptions-item label="秒杀金额">
          <span style="color: #F56C6C;">¥{{ viewForm.seckillAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="秒杀单价">
          <span style="color: #67C23A;">¥{{ viewForm.seckillPrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="总份额">{{ viewForm.totalShares }}</el-descriptions-item>
        <el-descriptions-item label="剩余份额">
          <span style="color: #E6A23C;">{{ viewForm.remainShares }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(viewForm.status)">{{ getStatusName(viewForm.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-if="viewForm.policy" style="margin-top: 20px;" :column="1" border>
        <el-descriptions-item label="政策">{{ viewForm.policy }}</el-descriptions-item>
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
import { listSeckill, getSeckill, delSeckill, addSeckill, updateSeckill, closeSeckill } from "@/api/member/seckill"
const { download } = useDownload()

export default {
  name: "Seckill",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      seckillList: [],
      title: "",
      open: false,
      viewOpen: false,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        seckillName: undefined,
        seckillType: undefined,
        beginSeckillDate: undefined,
        endSeckillDate: undefined
      },
      form: {},
      viewForm: {},
      rules: {
        seckillName: [
          { required: true, message: "秒杀名称不能为空", trigger: "blur" }
        ],
        seckillType: [
          { required: true, message: "类型不能为空", trigger: "change" }
        ],
        seckillDateRange: [
          { required: true, message: "秒杀日期不能为空", trigger: 'change',
            validator: (rule, value, callback) => {
              if (!value || value.length !== 2) {
                return callback(new Error('请选择秒杀日期'))
              }
              callback()
            } }
        ],
        seckillAmount: [
          { required: true, message: "秒杀金额不能为空", trigger: "blur" }
        ],
        seckillPrice: [
          { required: true, message: "秒杀单价不能为空", trigger: "blur" }
        ],
        totalShares: [
          { required: true, message: "总份额不能为空", trigger: "blur" }
        ]
      },
      statusOptions: {
        '0': '进行中',
        '1': '已结束',
        '2': '已取消'
      }
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
      const params = {
        ...this.queryParams
      }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginSeckillDate = this.dateRange[0]
        params.endSeckillDate = this.dateRange[1]
      }
      listSeckill(params).then(response => {
        this.seckillList = response.rows
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
        seckillId: undefined,
        seckillNo: undefined,
        seckillName: undefined,
        seckillType: undefined,
        seckillDate: undefined,
        endDate: undefined,
        seckillDateRange: null,
        timeSlot: undefined,
        timeSlotRange: null,
        seckillAmount: undefined,
        seckillPrice: undefined,
        totalShares: undefined,
        remainShares: undefined,
        policy: undefined,
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
      this.ids = selection.map(item => item.seckillId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加秒杀活动"
    },
    handleView(row) {
      getSeckill(row.seckillId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    handleUpdate(row) {
      this.reset()
      const seckillId = row.seckillId || this.ids
      getSeckill(seckillId).then(response => {
        this.form = response.data
        if (this.form.seckillDate && this.form.endDate) {
          this.form.seckillDateRange = [this.form.seckillDate, this.form.endDate]
        } else if (this.form.seckillDate) {
          this.form.seckillDateRange = [this.form.seckillDate, this.form.seckillDate]
        }
        if (this.form.timeSlot) {
          const times = this.form.timeSlot.split('-')
          if (times.length === 2) {
            this.form.timeSlotRange = [times[0], times[1]]
          }
        }
        this.open = true
        this.title = "修改秒杀活动"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.seckillDateRange && this.form.seckillDateRange.length === 2) {
            this.form.seckillDate = this.form.seckillDateRange[0]
            this.form.endDate = this.form.seckillDateRange[1]
          }
          if (this.form.timeSlotRange && this.form.timeSlotRange.length === 2) {
            this.form.timeSlot = this.form.timeSlotRange[0] + '-' + this.form.timeSlotRange[1]
          }
          if (this.form.seckillId != undefined) {
            updateSeckill(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addSeckill(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const seckillIds = row.seckillId || this.ids
      ElMessageBox.confirm('是否确认删除秒杀编号为"' + seckillIds + '"的数据项？').then(function() {
        return delSeckill(seckillIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('member/seckill/export', {
        ...this.queryParams
      }, `seckill_${new Date().getTime()}.xlsx`)
    },
    handleClose(row) {
      const seckillId = row.seckillId || this.ids[0]
      const seckillName = row.seckillName || ''
      ElMessageBox.confirm('是否确认结束秒杀活动"' + seckillName + '"？结束后将无法新增该活动的秒杀记录。').then(() => {
        return closeSeckill(seckillId)
      }).then(() => {
        this.getList()
        ElMessage.success("已结束")
      }).catch(() => {})
    },
    getStatusName(status) {
      return this.statusOptions[status] || '-'
    },
    getStatusType(status) {
      const types = {
        '0': 'success',
        '1': 'info',
        '2': 'danger'
      }
      return types[status] || 'info'
    }
  }
}
</script>

<style scoped>
</style>
