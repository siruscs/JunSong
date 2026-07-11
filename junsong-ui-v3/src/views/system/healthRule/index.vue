<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="规则领域" prop="ruleDomain">
        <el-select v-model="queryParams.ruleDomain" placeholder="请选择" clearable style="width: 200px" @change="handleQuery">
          <el-option label="财务" value="FINANCE" />
          <el-option label="会员" value="MEMBER" />
          <el-option label="系统" value="SYSTEM" />
          <el-option label="门店" value="STORE" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重级别" prop="severity">
        <el-select v-model="queryParams.severity" placeholder="请选择" clearable style="width: 200px" @change="handleQuery">
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态" prop="enabled">
        <el-select v-model="queryParams.enabled" placeholder="请选择" clearable style="width: 200px" @change="handleQuery">
          <el-option label="已启用" value="1" />
          <el-option label="已停用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <RightToolbar v-model:showSearch="showSearch" @query="getList">
      <el-button type="warning" :icon="Refresh" @click="getList">刷新</el-button>
    </RightToolbar>

    <el-table v-loading="loading" :data="rules">
      <el-table-column label="规则名称" align="center" prop="ruleName" :show-overflow-tooltip="true" min-width="160" />
      <el-table-column label="领域" align="center" prop="ruleDomain" width="80">
        <template #default="scope">
          <el-tag>{{ domainLabel(scope.row.ruleDomain) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="指标" align="center" prop="metricKey" :show-overflow-tooltip="true" width="160" />
      <el-table-column label="比较符" align="center" prop="compareOp" width="80" />
      <el-table-column label="阈值" align="center" width="120">
        <template #default="{ row }">
          <el-input-number v-if="editingId === row.ruleId" v-model="editForm.thresholdValue" :precision="2" size="small" controls-position="right" style="width: 100px" />
          <span v-else>{{ row.thresholdValue }}</span>
        </template>
      </el-table-column>
      <el-table-column label="严重级别" align="center" width="100">
        <template #default="{ row }">
          <el-select v-if="editingId === row.ruleId" v-model="editForm.severity" size="small" style="width: 90px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
          <el-tag v-else :type="severityType(row.severity)">{{ severityLabel(row.severity) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="通知" align="center" width="80">
        <template #default="{ row }">
          <el-switch
            v-if="editingId === row.ruleId"
            v-model="editForm.notifyEnabled"
            active-value="1"
            inactive-value="0"
          />
          <el-tag v-else :type="row.notifyEnabled === '1' ? 'success' : 'info'" size="small">
            {{ row.notifyEnabled === '1' ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled === '1'" @change="(val: boolean) => handleToggle(row, val)" />
        </template>
      </el-table-column>
      <el-table-column label="处理建议" align="center" prop="suggestion" :show-overflow-tooltip="true" min-width="200" />
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button v-if="editingId !== row.ruleId" link type="primary" @click="startEdit(row)" v-hasPermi="['system:healthRule:edit']">编辑</el-button>
          <template v-else>
            <el-button link type="primary" @click="saveEdit(row)">保存</el-button>
            <el-button link type="info" @click="cancelEdit">取消</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listHealthRules, updateHealthRule, toggleHealthRule } from '@/api/system/healthRule'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import RightToolbar from '@/components/RightToolbar/index.vue'

const loading = ref(false)
const rules = ref<any[]>([])
const total = ref(0)
const showSearch = ref(true)
const queryFormRef = ref()
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ruleDomain: '',
  severity: '',
  enabled: '',
})
const editingId = ref<number | null>(null)
const editForm = reactive({ thresholdValue: 0, severity: '', notifyEnabled: '', suggestion: '' })

function severityType(s: string) {
  if (s === 'HIGH') return 'danger'
  if (s === 'MEDIUM') return 'warning'
  return 'info'
}

function severityLabel(s: string) {
  if (s === 'HIGH') return '高'
  if (s === 'MEDIUM') return '中'
  return '低'
}

function domainLabel(d: string) {
  const map: Record<string, string> = { FINANCE: '财务', MEMBER: '会员', SYSTEM: '系统', STORE: '门店' }
  return map[d] || d
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryFormRef.value?.resetFields?.()
  handleQuery()
}

async function getList() {
  loading.value = true
  try {
    const params: any = { pageNum: queryParams.pageNum, pageSize: queryParams.pageSize }
    if (queryParams.ruleDomain) params.ruleDomain = queryParams.ruleDomain
    if (queryParams.severity) params.severity = queryParams.severity
    if (queryParams.enabled) params.enabled = queryParams.enabled
    const res: any = await listHealthRules(params)
    rules.value = res.rows || res.data?.rows || []
    total.value = res.total || res.data?.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function startEdit(row: any) {
  editingId.value = row.ruleId
  editForm.thresholdValue = row.thresholdValue
  editForm.severity = row.severity
  editForm.notifyEnabled = row.notifyEnabled
  editForm.suggestion = row.suggestion
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(row: any) {
  try {
    await updateHealthRule({
      ruleId: row.ruleId,
      thresholdValue: editForm.thresholdValue,
      severity: editForm.severity,
      notifyEnabled: editForm.notifyEnabled,
      suggestion: editForm.suggestion,
    })
    ElMessage.success('保存成功')
    editingId.value = null
    getList()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleToggle(row: any, val: boolean) {
  try {
    await toggleHealthRule(row.ruleId, val ? '1' : '0')
    ElMessage.success(val ? '已启用' : '已停用')
    getList()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(getList)
</script>
