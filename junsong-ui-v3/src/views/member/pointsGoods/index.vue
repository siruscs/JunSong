<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="物品名称" prop="goodsName">
        <el-input
          v-model="queryParams.goodsName"
          placeholder="请输入物品名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物品编码" prop="goodsCode">
        <el-input
          v-model="queryParams.goodsCode"
          placeholder="请输入物品编码"
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
          v-hasPermi="['member:pointsGoods:add']"
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
          v-hasPermi="['member:pointsGoods:edit']"
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
          v-hasPermi="['member:pointsGoods:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          size="small"
          @click="handleExport"
          v-hasPermi="['member:pointsGoods:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @query="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="goodsList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="物品编码" align="center" prop="goodsCode" width="120" />
      <el-table-column label="物品名称" align="center" prop="goodsName" width="200" />
      <el-table-column label="物品图片" align="center" prop="goodsImage" width="120">
        <template #default="scope">
          <el-image
            v-if="scope.row.goodsImage"
            style="width: 80px; height: 80px"
            :src="scope.row.goodsImage"
            :preview-src-list="[scope.row.goodsImage]"
            fit="cover"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="物品价值" align="center" prop="goodsValue" width="120">
        <template #default="scope">
          <span style="color: #409EFF;">¥{{ scope.row.goodsValue }}</span>
        </template>
      </el-table-column>
      <el-table-column label="积分价格" align="center" prop="pointsPrice" width="120">
        <template #default="scope">
          <span style="color: #E6A23C;">{{ scope.row.pointsPrice }}积分</span>
        </template>
      </el-table-column>
      <el-table-column label="库存" align="center" prop="stock" width="100">
        <template #default="scope">
          <span :style="{ color: scope.row.stock < 10 ? '#F56C6C' : '' }">{{ scope.row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已兑换" align="center" prop="exchangedNum" width="100">
        <template #default="scope">
          <span style="color: #67C23A;">{{ scope.row.exchangedNum || 0 }}</span>
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
            v-hasPermi="['member:pointsGoods:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['member:pointsGoods:remove']"
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
            <el-form-item label="物品编码">
              <el-input v-model="form.goodsCode" placeholder="系统自动生成" disabled>
                <template #prefix><el-icon><Key /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物品名称" prop="goodsName">
              <el-input v-model="form.goodsName" placeholder="请输入物品名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物品价值" prop="goodsValue">
              <el-input-number v-model="form.goodsValue" :precision="2" :step="0.01" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="积分价格" prop="pointsPrice">
              <el-input-number v-model="form.pointsPrice" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="物品图片" prop="goodsImage">
          <el-upload
            class="avatar-uploader"
            action="#"
            :http-request="uploadImage"
            :show-file-list="false"
            :before-upload="beforeImageUpload"
          >
            <img v-if="form.goodsImage" :src="form.goodsImage" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
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

    <el-dialog title="积分物品详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="物品编码">{{ viewForm.goodsCode }}</el-descriptions-item>
        <el-descriptions-item label="物品名称">{{ viewForm.goodsName }}</el-descriptions-item>
        <el-descriptions-item label="物品图片" :span="2">
          <el-image
            v-if="viewForm.goodsImage"
            style="width: 100px; height: 100px"
            :src="viewForm.goodsImage"
            :preview-src-list="[viewForm.goodsImage]"
            fit="cover"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="物品价值">
          <span style="color: #409EFF;">¥{{ viewForm.goodsValue }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="积分价格">
          <span style="color: #E6A23C;">{{ viewForm.pointsPrice }}积分</span>
        </el-descriptions-item>
        <el-descriptions-item label="库存">
          <span :style="{ color: viewForm.stock < 10 ? '#F56C6C' : '' }">{{ viewForm.stock }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="已兑换">
          <span style="color: #67C23A;">{{ viewForm.exchangedNum || 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <dict-tag :options="dict.type.sys_normal_disable" :value="viewForm.status"/>
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
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from "element-plus"
import { parseTime } from "@/utils/junsong"
import { useDownload } from "@/composables/useDownload"
import { useDict, getDictDefaultValue } from "@/composables/useDict"
import request from "@/api/request"
import { listPointsGoods, getPointsGoods, delPointsGoods, addPointsGoods, updatePointsGoods } from "@/api/member/pointsGoods"
const { download } = useDownload()

export default {
  name: "PointsGoods",
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
      goodsList: [],
      title: "",
      open: false,
      viewOpen: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        goodsName: undefined,
        goodsCode: undefined
      },
      form: {},
      viewForm: {},
      rules: {
        goodsName: [
          { required: true, message: "物品名称不能为空", trigger: "blur" }
        ],
        goodsValue: [
          { required: true, message: "物品价值不能为空", trigger: "blur" }
        ],
        pointsPrice: [
          { required: true, message: "积分价格不能为空", trigger: "blur" }
        ],
        stock: [
          { required: true, message: "库存不能为空", trigger: "blur" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  watch: {
    'dict.type.sys_normal_disable': {
      handler() {
        if (this.open && this.title === "添加积分物品" && !this.form.status) {
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
      listPointsGoods(this.queryParams).then(response => {
        this.goodsList = response.rows
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
        goodsId: undefined,
        goodsCode: undefined,
        goodsName: undefined,
        goodsImage: undefined,
        goodsValue: undefined,
        pointsPrice: undefined,
        stock: 0,
        exchangedNum: 0,
        status: "0",
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
      this.ids = selection.map(item => item.goodsId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.form.status = getDictDefaultValue(this.dict.type.sys_normal_disable, "0")
      this.open = true
      this.title = "添加积分物品"
    },
    handleView(row) {
      getPointsGoods(row.goodsId).then(response => {
        this.viewForm = response.data
        this.viewOpen = true
      })
    },
    handleUpdate(row) {
      this.reset()
      const goodsId = row.goodsId || this.ids
      getPointsGoods(goodsId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改积分物品"
      })
    },
    beforeImageUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        ElMessage.error('上传图片只能是 JPG/PNG/GIF 格式!')
      }
      if (!isLt2M) {
        ElMessage.error('上传图片大小不能超过 2MB!')
      }
      return isJPG && isLt2M
    },
    uploadImage(param) {
      const formData = new FormData()
      formData.append('file', param.file)
      request.post('/common/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then(response => {
        this.form.goodsImage = response.url
        ElMessage.success("上传成功")
      }).catch(() => {
        ElMessage.error("上传失败")
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.goodsId != undefined) {
            updatePointsGoods(this.form).then(() => {
              ElMessage.success("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPointsGoods(this.form).then(() => {
              ElMessage.success("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const goodsIds = row.goodsId || this.ids
      ElMessageBox.confirm('是否确认删除物品编码为"' + goodsIds + '"的数据项？').then(function() {
        return delPointsGoods(goodsIds)
      }).then(() => {
        this.getList()
        ElMessage.success("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('member/pointsGoods/export', {
        ...this.queryParams
      }, `pointsGoods_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style lang="scss" scoped>
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 148px;
  height: 148px;
  line-height: 148px;
  text-align: center;
}

.avatar {
  width: 148px;
  height: 148px;
  display: block;
}
</style>
