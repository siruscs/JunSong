<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="订阅名称" prop="subscriptionName">
        <el-input v-model="queryParams.subscriptionName" placeholder="请输入订阅名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="事件类型" prop="eventType">
        <el-input v-model="queryParams.eventType" placeholder="请输入事件类型" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 200px">
          <el-option v-for="d in dict.type.sys_normal_disable" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-hasPermi="['system:webhook:add']">新增</el-button>
      <el-button type="success" :icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:webhook:edit']">修改</el-button>
      <el-button type="danger" :icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:webhook:remove']">删除</el-button>
      <el-button type="warning" :icon="Promotion" @click="handleDelivery" v-hasPermi="['system:webhook:edit']">投递记录</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="subscriptionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="订阅名称" align="center" prop="subscriptionName" :show-overflow-tooltip="true" />
      <el-table-column label="事件类型" align="center" prop="eventType" width="180">
        <template #default="scope">
          <el-tag>{{ scope.row.eventType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="回调地址" align="center" prop="callbackUrl" :show-overflow-tooltip="true" min-width="200" />
      <el-table-column label="签名密钥" align="center" prop="secretToken" width="120">
        <template #default="scope">
          <span v-if="scope.row.secretToken">{{ scope.row.secretToken.substring(0, 8) }}...</span>
        </template>
      </el-table-column>
      <el-table-column label="最大重试" align="center" prop="maxRetries" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" v-hasPermi="['system:webhook:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" :icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:webhook:edit']">修改</el-button>
          <el-button link type="success" :icon="Promotion" @click="handleTest(scope.row)">测试</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:webhook:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="subscriptionFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="订阅名称" prop="subscriptionName">
          <el-input v-model="form.subscriptionName" placeholder="请输入订阅名称" />
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-input v-model="form.eventType" placeholder="如 approval.completed, refund.created" />
        </el-form-item>
        <el-form-item label="回调地址" prop="callbackUrl">
          <el-input v-model="form.callbackUrl" placeholder="https://your-server.com/webhook/callback" />
        </el-form-item>
        <el-form-item label="签名密钥" prop="secretToken">
          <el-input v-model="form.secretToken" placeholder="留空将自动生成" readonly>
            <template #append>
              <el-button @click="handleGenerateToken">生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="最大重试" prop="maxRetries">
          <el-input-number v-model="form.maxRetries" :min="0" :max="10" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="d in dict.type.sys_normal_disable" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="投递记录" v-model="deliveryOpen" width="900px" append-to-body>
      <el-table v-loading="deliveryLoading" :data="deliveryList" border>
        <el-table-column label="ID" align="center" prop="id" width="60" />
        <el-table-column label="事件类型" align="center" prop="eventType" width="160">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.eventType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="事件ID" align="center" prop="eventId" width="160" :show-overflow-tooltip="true" />
        <el-table-column label="HTTP状态" align="center" prop="httpStatus" width="90">
          <template #default="scope">
            <el-tag v-if="scope.row.httpStatus && scope.row.httpStatus >= 200 && scope.row.httpStatus < 300" type="success">{{ scope.row.httpStatus }}</el-tag>
            <el-tag v-else-if="scope.row.httpStatus" type="danger">{{ scope.row.httpStatus }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="重试次数" align="center" prop="retryCount" width="80" />
        <el-table-column label="状态" align="center" prop="status" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'SUCCESS'" type="success">成功</el-tag>
            <el-tag v-else-if="scope.row.status === 'PENDING'" type="info">待投递</el-tag>
            <el-tag v-else-if="scope.row.status === 'FAILED'" type="warning">待重试</el-tag>
            <el-tag v-else-if="scope.row.status === 'DEAD'" type="danger">死信</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
        <el-table-column label="完成时间" align="center" prop="completeTime" width="160" />
      </el-table>
      <pagination v-show="deliveryTotal > 0" :total="deliveryTotal" v-model:page="deliveryQuery.pageNum" v-model:limit="deliveryQuery.pageSize" @pagination="getDeliveryList" />
    </el-dialog>
  </div>
</template>

<script setup name="Webhook">
import { listSubscription, getSubscription, addSubscription, updateSubscription, delSubscription, changeStatus, generateToken, listDelivery, testEvent } from '@/api/system/webhook'
import { Plus, Edit, Delete, Promotion } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict('sys_normal_disable')

const subscriptionList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const deliveryOpen = ref(false)
const deliveryLoading = ref(false)
const deliveryList = ref([])
const deliveryTotal = ref(0)

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, subscriptionName: undefined, eventType: undefined, status: undefined },
  deliveryQuery: { pageNum: 1, pageSize: 10, subscriptionId: undefined },
  rules: {
    subscriptionName: [{ required: true, message: '订阅名称不能为空', trigger: 'blur' }],
    eventType: [{ required: true, message: '事件类型不能为空', trigger: 'blur' }],
    callbackUrl: [{ required: true, message: '回调地址不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules, deliveryQuery } = toRefs(data)

function getList() {
  loading.value = true
  listSubscription(queryParams.value).then(response => {
    subscriptionList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryFormRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = { id: undefined, subscriptionName: undefined, eventType: undefined, callbackUrl: undefined, secretToken: undefined, status: '0', maxRetries: 3, remark: undefined }
  proxy.resetForm('subscriptionFormRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加Webhook订阅'
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value[0]
  getSubscription(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改Webhook订阅'
  })
}

function submitForm() {
  proxy.$refs['subscriptionFormRef'].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateSubscription(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addSubscription(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  proxy.$modal.confirm('确认删除选中的订阅?').then(() => {
    return delSubscription(delIds.join(','))
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleStatusChange(row) {
  const text = row.status === '0' ? '启用' : '停用'
  proxy.$modal.confirm('确认' + text + '订阅"' + row.subscriptionName + '"?').then(() => {
    return changeStatus(row.id, row.status)
  }).then(() => {
    proxy.$modal.msgSuccess(text + '成功')
  }).catch(() => {
    row.status = row.status === '0' ? '1' : '0'
  })
}

function handleGenerateToken() {
  generateToken().then(response => {
    form.value.secretToken = response.msg
  })
}

function handleTest(row) {
  proxy.$modal.confirm('确认发送测试事件到"' + row.callbackUrl + '"?').then(() => {
    return testEvent(row.eventType)
  }).then(() => {
    proxy.$modal.msgSuccess('测试事件已触发')
  }).catch(() => {})
}

function handleDelivery() {
  if (ids.value.length !== 1) {
    proxy.$modal.msgWarning('请选择一条记录查看投递记录')
    return
  }
  deliveryQuery.value.subscriptionId = ids.value[0]
  deliveryOpen.value = true
  getDeliveryList()
}

function getDeliveryList() {
  deliveryLoading.value = true
  listDelivery(deliveryQuery.value).then(response => {
    deliveryList.value = response.rows
    deliveryTotal.value = response.total
    deliveryLoading.value = false
  })
}

getList()
</script>
