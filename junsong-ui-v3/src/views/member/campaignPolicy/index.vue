<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="mb8">
      <el-form-item label="核算周期"><el-select v-model="query.periodId" placeholder="全部周期" clearable filterable style="width: 280px"><el-option v-for="period in periods" :key="period.periodId" :label="periodLabel(period)" :value="period.periodId" /></el-select></el-form-item>
      <el-form-item label="商品"><el-select v-model="query.productId" placeholder="全部商品" clearable filterable style="width: 240px"><el-option v-for="product in products" :key="product.productId" :label="product.productName" :value="product.productId" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
    </el-form>
    <div class="mb8"><el-button type="primary" @click="openCreate">新增销售政策</el-button><el-button @click="load">刷新</el-button></div>
    <el-table v-loading="loading" :data="policies" border>
      <el-table-column prop="policyNo" label="政策编号" width="150" />
      <el-table-column prop="policyName" label="政策名称" min-width="180" />
      <el-table-column prop="periodId" label="核算周期" width="100" />
      <el-table-column label="商品" min-width="180"><template #default="scope">{{ productName(scope.row.productId) }}</template></el-table-column>
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="status" label="状态" width="90"><template #default="scope"><el-tag :type="String(scope.row.status) === '1' ? 'success' : 'info'">{{ String(scope.row.status) === '1' ? '生效' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="套餐档位" min-width="330"><template #default="scope"><el-tag v-for="item in (scope.row.packages || [])" :key="item.packageId" class="package-tag">买{{ item.purchaseQuantity }}送{{ item.giftQuantity }} / {{ item.packagePrice ?? '按单价' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" fixed="right" width="330"><template #default="scope"><el-button link type="primary" @click="openEdit(scope.row)">查看/编辑</el-button><el-button link type="warning" @click="toggleStatus(scope.row)">{{ String(scope.row.status) === '1' ? '停用' : '启用' }}</el-button><el-button link type="success" @click="openSync(scope.row)" v-hasPermi="['member:campaignPolicy:sync']">同步到其他机构</el-button><el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['member:campaignPolicy:remove']">删除</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="dialogOpen" :title="editing ? '查看/编辑销售政策' : '新增销售政策'" width="820px">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16"><el-col :span="12"><el-form-item label="政策编号"><el-input v-model="form.policyNo" disabled placeholder="保存后自动生成" /></el-form-item></el-col><el-col :span="12"><el-form-item label="政策名称"><el-input v-model="form.policyName" placeholder="自动生成，可修改" /></el-form-item></el-col></el-row>
        <el-row :gutter="16"><el-col :span="12"><el-form-item label="核算周期"><el-select v-model="form.periodId" placeholder="请选择核算周期" filterable style="width: 100%"><el-option v-for="period in periods" :key="period.periodId" :label="periodLabel(period)" :value="period.periodId" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="商品"><el-select v-model="form.productId" filterable placeholder="请选择商品" style="width: 100%"><el-option v-for="product in products" :key="product.productId" :label="product.productName" :value="product.productId" /></el-select></el-form-item></el-col></el-row>
        <el-form-item label="套餐档位"><div class="package-editor"><div v-for="(item, index) in form.packages" :key="item._key" class="package-row"><el-input v-model="item.packageName" placeholder="档位名称" /><el-input-number v-model="item.purchaseQuantity" :min="1" :precision="3" controls-position="right" placeholder="购买数量" /><span>送</span><el-input-number v-model="item.giftQuantity" :min="0" :precision="3" controls-position="right" placeholder="赠送数量" /><el-input-number v-model="item.packagePrice" :min="0" :precision="2" controls-position="right" placeholder="套餐价" /><el-button link type="danger" @click="removePackage(Number(index))">删除</el-button></div><el-button type="primary" link @click="addPackage">+ 添加档位</el-button></div></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <ConfigSyncDialog v-model="syncOpen" sync-type="CAMPAIGN_POLICY" :source-record-id="syncRecordId" @completed="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProductSelector } from '@/api/finance/product'
import { getCurrentAccountingPeriod, listAccountingPeriod } from '@/api/finance/accountingPeriod'
import { addCampaignPolicy, changeCampaignPolicyStatus, deleteCampaignPolicy, listCampaignPolicies, updateCampaignPolicy } from '@/api/member/campaignPolicy'
import { useUserStore } from '@/stores/user'
import ConfigSyncDialog from '@/components/ConfigSyncDialog/index.vue'
import { formatDateTime } from '@/utils/junsong'

const loading = ref(false); const policies = ref<any[]>([]); const products = ref<any[]>([]); const periods = ref<any[]>([]); const dialogOpen = ref(false); const editing = ref(false)
const userStore = useUserStore()
const syncOpen = ref(false); const syncRecordId = ref<number | undefined>()
const query = reactive<any>({ periodId: undefined, productId: undefined })
const form = reactive<any>({ policyId: undefined, policyNo: '', policyName: '', periodId: undefined, productId: undefined, version: 1, customerScope: 'MEMBER', effectiveStart: undefined, effectiveEnd: undefined, status: '1', packages: [] })
function unwrap(response: any) { return response.rows || response.data?.rows || response.data || [] }
function productName(id: number) { return products.value.find(item => item.productId === id)?.productName || id || '-' }
function periodLabel(period: any) { return `${period.periodNo || `周期${period.periodId}`}（${formatDateTime(period.startTime) || ''} 至 ${formatDateTime(period.endTime) || '当前'}）` }
async function load() { loading.value = true; try { const response: any = await listCampaignPolicies(query); policies.value = unwrap(response) } finally { loading.value = false } }
async function loadProducts() { const response: any = await listProductSelector(); products.value = unwrap(response) }
async function loadPeriods() { const deptId = userStore.currentDeptId; const response: any = await listAccountingPeriod({ pageNum: 1, pageSize: 200, status: undefined, deptId: userStore.currentDeptId }); periods.value = unwrap(response).filter((item: any) => deptId == null || String(item.deptId) === String(userStore.currentDeptId)); const current = deptId ? await getCurrentAccountingPeriod(deptId) : null; const currentPeriod: any = current && ((current as any).data || current); if (currentPeriod?.periodId && !periods.value.some(item => item.periodId === currentPeriod.periodId) && String(currentPeriod.deptId) === String(deptId)) periods.value.unshift(currentPeriod) }
function reset() { query.periodId = undefined; query.productId = undefined; load() }
function blankPackage() { return { _key: `${Date.now()}-${Math.random()}`, packageName: '', purchaseQuantity: undefined, giftQuantity: undefined, totalQuantity: undefined, packagePrice: undefined, sortNo: 1 } }
function openCreate() { editing.value = false; Object.assign(form, { policyId: undefined, policyNo: '', policyName: '', periodId: query.periodId || periods.value.find(item => String(item.status) === '1')?.periodId, productId: query.productId, version: 1, customerScope: 'MEMBER', effectiveStart: undefined, effectiveEnd: undefined, status: '1', packages: [blankPackage()] }); syncEffectiveDates(); dialogOpen.value = true }
function openEdit(row: any) { editing.value = true; Object.assign(form, JSON.parse(JSON.stringify(row))); form.packages = (row.packages || []).map((item: any) => ({ ...item, _key: item.packageId || `${Date.now()}-${Math.random()}` })); dialogOpen.value = true }
function openSync(row: any) { syncRecordId.value = row.policyId; syncOpen.value = true }
function addPackage() { form.packages.push(blankPackage()) }
function removePackage(index: number) { if (form.packages.length <= 1) return ElMessage.warning('至少保留一个套餐档位'); form.packages.splice(index, 1) }
function syncEffectiveDates() { const period = periods.value.find(item => String(item.periodId) === String(form.periodId)); if (period) { form.effectiveStart = period.startTime; form.effectiveEnd = period.endTime || undefined } }
async function save() { if (!form.policyName || !form.periodId || !form.productId || !form.packages.length) return ElMessage.warning('请先选择核算周期、商品并填写套餐档位'); if (form.packages.some((item: any) => item.purchaseQuantity == null || Number(item.purchaseQuantity) <= 0)) return ElMessage.warning('请填写大于0的购买数量'); syncEffectiveDates(); if (!form.effectiveStart) return ElMessage.warning('所选核算周期缺少开始时间，无法保存政策'); const packages = form.packages.map((item: any, index: number) => ({ ...item, giftQuantity: item.giftQuantity == null ? 0 : Number(item.giftQuantity), packageName: item.packageName || `买${item.purchaseQuantity}送${item.giftQuantity || 0}`, totalQuantity: Number(item.purchaseQuantity) + Number(item.giftQuantity || 0), sortNo: index + 1 })); try { const payload = { ...form, packages }; if (editing.value && form.policyId) await updateCampaignPolicy(form.policyId, payload); else await addCampaignPolicy(payload); ElMessage.success('销售政策已保存'); dialogOpen.value = false; await load() } catch (error: any) { ElMessage.error(error?.message || '销售政策保存失败，请检查核算周期和商品') } }
async function toggleStatus(row: any) { const next = String(row.status) === '1' ? '0' : '1'; await ElMessageBox.confirm(`确认${next === '1' ? '启用' : '停用'}政策"${row.policyName}"吗？`, '操作确认'); await changeCampaignPolicyStatus(row.policyId, next); ElMessage.success('状态已更新'); await load() }
async function handleDelete(row: any) { await ElMessageBox.confirm(`确认删除销售政策"${row.policyName}"吗？删除后不可恢复。`, '删除确认', { type: 'warning' }); await deleteCampaignPolicy(row.policyId); ElMessage.success('销售政策已删除'); await load() }
onMounted(async () => { await Promise.all([loadProducts(), loadPeriods(), load()]) })
</script>

<style scoped>
.package-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.package-row .el-input { width: 150px; }
.package-row .el-input-number { width: 125px; }
.package-tag { margin: 2px 6px 2px 0; }
</style>
