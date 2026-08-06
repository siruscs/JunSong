<template>
  <el-dialog v-model="visible" title="同步到其他机构" width="680px" append-to-body>
    <el-form label-width="110px">
      <el-form-item label="目标机构">
        <el-select v-model="targetDeptIds" multiple filterable clearable placeholder="请选择有权限的目标机构" style="width: 100%">
          <el-option v-for="dept in targetDepts" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" />
        </el-select>
      </el-form-item>
      <template v-if="syncType === 'CAMPAIGN_POLICY'">
        <el-form-item v-for="deptId in targetDeptIds" :key="deptId" :label="deptName(deptId) + '周期'">
          <el-select v-model="targetPeriodIds[deptId]" filterable placeholder="请选择目标核算周期" style="width: 100%" @focus="loadPeriods(deptId)">
            <el-option v-for="period in periods[deptId] || []" :key="period.periodId" :label="periodLabel(period)" :value="period.periodId" />
          </el-select>
        </el-form-item>
      </template>
    </el-form>
    <el-alert v-if="details.length" :title="`预览结果：${details.length} 个目标机构`" type="info" :closable="false" class="mb12" />
    <el-table v-if="details.length" :data="details" border max-height="260">
      <el-table-column prop="targetDeptId" label="目标机构" width="110"><template #default="scope">{{ deptName(scope.row.targetDeptId) }}</template></el-table-column>
      <el-table-column label="预检结果" min-width="300"><template #default="scope"><el-tag :type="operationType(scope.row.operation)">{{ operationLabel(scope.row.operation) }}</el-tag><div v-if="scope.row.impactText" class="impact-text">{{ scope.row.impactText }}</div></template></el-table-column>
      <el-table-column label="处理" width="160"><template #default="scope"><el-radio-group v-if="scope.row.operation === 'DIFF'" v-model="scope.row.decision" size="small"><el-radio-button label="OVERWRITE">覆盖</el-radio-button><el-radio-button label="SKIP">跳过</el-radio-button></el-radio-group><span v-else>{{ scope.row.operation === 'CREATE' ? '自动新增' : '无需处理' }}</span></template></el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button v-if="!details.length" type="primary" :loading="loading" @click="preview">预览差异</el-button>
      <el-button v-else type="primary" :loading="loading" @click="execute">确认同步</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listAccountingPeriod } from '@/api/finance/accountingPeriod'
import { executeConfigSync, previewConfigSync } from '@/api/member/configSync'
import { formatDateTime } from '@/utils/junsong'

const props = defineProps<{ modelValue: boolean; syncType: string; sourceRecordId?: number }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void; (e: 'completed'): void }>()
const userStore = useUserStore(); const loading = ref(false); const targetDeptIds = ref<number[]>([]); const details = ref<any[]>([]); const batch = ref<any>(null); const periods = reactive<Record<number, any[]>>({}); const targetPeriodIds = reactive<Record<number, number>>({})
const visible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const targetDepts = computed(() => (userStore.depts || []).filter((dept: any) => Number(dept.deptId) !== Number(userStore.currentDeptId)))
function deptName(id: number) { return targetDepts.value.find((dept: any) => Number(dept.deptId) === Number(id))?.deptName || id }
function operationLabel(operation: string) { return ({ CREATE: '目标机构没有此配置，将新增', DIFF: '目标机构已有配置，内容有差异', NOOP: '目标机构已有相同配置，无需处理', CONFLICT: '编码已被其他机构占用，无法新增', IMPACT_BLOCKED: '目标等级已被会员使用，禁止自动覆盖' } as Record<string, string>)[operation] || '需要人工确认' }
function operationType(operation: string) { return operation === 'NOOP' ? 'success' : operation === 'CONFLICT' || operation === 'IMPACT_BLOCKED' ? 'danger' : 'warning' }
function periodLabel(period: any) { return `${period.periodNo || `周期${period.periodId}`}（${formatDateTime(period.startTime) || '-'} 至 ${formatDateTime(period.endTime) || '当前'}）` }
async function loadPeriods(deptId: number) { if (periods[deptId]) return; const response: any = await listAccountingPeriod({ pageNum: 1, pageSize: 200, deptId }); const rows = response.rows || response.data?.rows || response.data || []; periods[deptId] = rows.filter((period: any) => String(period.deptId) === String(deptId)) }
async function preview() { if (!props.sourceRecordId || !targetDeptIds.value.length) return ElMessage.warning('请选择目标机构'); if (props.syncType === 'CAMPAIGN_POLICY' && targetDeptIds.value.some(id => !targetPeriodIds[id])) return ElMessage.warning('请选择每个目标机构的核算周期'); loading.value = true; try { const response: any = await previewConfigSync({ syncType: props.syncType, sourceRecordId: props.sourceRecordId, targetDeptIds: targetDeptIds.value, targetPeriodIds: props.syncType === 'CAMPAIGN_POLICY' ? targetPeriodIds : undefined, idempotencyKey: `${props.syncType}-${props.sourceRecordId}-${Date.now()}` }); const data = response.data || response; batch.value = data.batch; details.value = (data.details || []).map((item: any) => { let impactText = ''; try { const snapshot = item.diffSnapshot ? JSON.parse(item.diffSnapshot) : null; if (snapshot?.memberUsageCount != null) impactText = `当前已有 ${snapshot.memberUsageCount} 位会员使用此等级`; } catch (_) {} return { ...item, impactText, decision: item.operation === 'CREATE' ? 'CREATE' : item.operation === 'CONFLICT' || item.operation === 'IMPACT_BLOCKED' || item.operation === 'NOOP' ? 'SKIP' : 'OVERWRITE' } }) } catch (error: any) { ElMessage.error(error?.message || '预检失败，请稍后重试') } finally { loading.value = false } }
async function execute() { if (!batch.value) return; loading.value = true; try { await executeConfigSync({ batchId: batch.value.batchId, previewVersion: batch.value.previewVersion, decisions: details.value.map(item => ({ detailId: item.detailId, decision: item.decision })) }); ElMessage.success('配置同步完成'); emit('completed'); close() } catch (error: any) { ElMessage.error(error?.message || '同步失败，可以直接重试') } finally { loading.value = false } }
function close() { details.value = []; batch.value = null; targetDeptIds.value = []; visible.value = false }
watch(() => props.modelValue, (open) => { if (!open) { details.value = []; batch.value = null } })
</script>
<style scoped>
.impact-text { margin-top: 4px; color: var(--el-color-danger); font-size: 12px; line-height: 1.4; }
</style>
