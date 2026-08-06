<template>
  <view class="page">
    <view class="hero">
      <view class="eyebrow">系统管理 · 跨机构配置</view>
      <view class="title">配置同步</view>
      <view class="subtitle">当前源机构：{{ currentDeptName }}</view>
    </view>

    <view class="section-card" v-if="availableTypes.length">
      <view class="section-title">选择配置类型</view>
      <picker :range="availableTypes" range-key="title" :value="typeIndex" @change="changeType">
        <view class="picker-control"><text>{{ activeType.title }}</text><text class="arrow">›</text></view>
      </picker>
    </view>

    <view class="section-card" v-if="activeType">
      <view class="section-title">选择源配置</view>
      <picker :range="sourceRows" :range-key="activeType.labelKey" :value="sourceIndex" @change="changeSource">
        <view class="picker-control"><text>{{ sourceLabel || `请选择${activeType.title}` }}</text><text class="arrow">›</text></view>
      </picker>
      <view class="hint">会员等级同步会将当前机构的全部等级一次性同步到目标机构。</view>
    </view>

    <view class="section-card" v-if="activeType && sourceRecordId">
      <view class="section-title">选择目标机构</view>
      <view class="dept-list">
        <label class="dept-option" v-for="dept in targetDepts" :key="dept.id">
          <checkbox :value="String(dept.id)" :checked="selectedDeptIds.includes(dept.id)" @tap="toggleDept(dept.id)" />
          <text>{{ dept.name }}</text>
        </label>
      </view>
      <view v-for="deptId in selectedDeptIds" :key="deptId" class="period-row" v-if="activeType.key === 'CAMPAIGN_POLICY'">
        <text>{{ deptName(deptId) }}核算周期</text>
        <picker :range="periodOptions(deptId)" range-key="label" :value="periodIndex(deptId)" @change="changePeriod(deptId, $event.detail.value)">
          <view class="picker-control compact"><text>{{ selectedPeriodLabel(deptId) }}</text><text class="arrow">›</text></view>
        </picker>
      </view>
    </view>

    <view class="section-card" v-if="details.length">
      <view class="section-title">同步预检结果（{{ details.length }}项）</view>
      <view class="detail-row" v-for="item in details" :key="item.detailId">
        <view class="detail-main"><text>{{ deptName(item.targetDeptId) }}</text><text class="detail-message">{{ operationLabel(item.operation) }}</text></view>
        <view v-if="item.sourceSnapshot?.packages?.length" class="package-preview">
          <text v-for="pkg in item.sourceSnapshot.packages" :key="`${item.detailId}-${pkg.sortNo || pkg.packageName}`">买{{ quantity(pkg.purchaseQuantity) }}送{{ quantity(pkg.giftQuantity) }} · {{ money(pkg.packagePrice) }}</text>
        </view>
        <picker v-if="item.operation === 'DIFF'" :range="decisionOptions" :value="decisionIndex(item)" @change="changeDecision(item, $event.detail.value)">
          <view class="decision-picker">{{ decisionLabel(item.decision) }} ›</view>
        </picker>
        <text v-else class="decision-text">{{ decisionLabel(item.decision) }}</text>
      </view>
    </view>

    <view class="empty" v-if="!availableTypes.length">当前账号没有配置同步权限，请在“小程序权限”中勾选配置同步及对应的同步操作权限。</view>
    <view class="footer" v-if="activeType">
      <button class="secondary" v-if="details.length" @tap="resetPreview">重新预检</button>
      <button class="primary" :disabled="loading" @tap="details.length ? execute() : preview()">{{ loading ? '处理中…' : (details.length ? '确认同步' : '预检差异') }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request } from '@/api/index.js'
import { hasExactPermission, requireModulePermission } from '@/utils/permission.js'
import { workContext } from '@/utils/workContext.js'
import { formatDateTime } from '@/utils/date.js'

const allTypes = [
  { key: 'PRODUCT', title: '商品', labelKey: 'productName', idKey: 'productId', url: '/finance/product/list', permission: 'finance:product:sync' },
  { key: 'SUPPLIER', title: '供应商', labelKey: 'supplierName', idKey: 'supplierId', url: '/finance/supplier/list', permission: 'finance:supplier:sync' },
  { key: 'LEVEL', title: '会员等级', labelKey: 'typeName', idKey: 'typeId', url: '/member/level/list', permission: 'member:level:sync' },
  { key: 'CAMPAIGN_POLICY', title: '销售政策', labelKey: 'policyName', idKey: 'policyId', url: '/member/campaign/policy/list', permission: 'member:campaignPolicy:sync' }
]
const decisionOptions = ['自动新增', '覆盖', '跳过']
const decisionValues = ['CREATE', 'OVERWRITE', 'SKIP']
const loading = ref(false); const typeIndex = ref(0); const sourceIndex = ref(0); const sourceRows = ref([]); const selectedDeptIds = ref([]); const details = ref([]); const batch = ref(null); const periods = reactive({}); const targetPeriodIds = reactive({})
const requestedType = ref(''); const requestedSourceRecordId = ref('')
const snapshot = workContext.snapshot()
const currentDeptId = snapshot.currentDeptId
const currentDeptName = snapshot.currentDept?.name || snapshot.currentDept?.deptName || '未选择机构'
const targetDepts = computed(() => (snapshot.depts || []).filter((dept) => String(dept.id) !== String(currentDeptId)))
const availableTypes = computed(() => allTypes.filter((item) => hasExactPermission(item.permission)))
const activeType = computed(() => availableTypes.value[typeIndex.value] || availableTypes.value[0])
const sourceRecordId = computed(() => sourceRows.value[sourceIndex.value]?.[activeType.value?.idKey])
const sourceLabel = computed(() => sourceRows.value[sourceIndex.value]?.[activeType.value?.labelKey] || '')

function unwrap(response) { return response?.rows || response?.data?.rows || response?.data || [] }
function deptName(id) { return targetDepts.value.find((dept) => String(dept.id) === String(id))?.name || String(id) }
function operationLabel(operation) { return ({ CREATE: '目标机构无此配置，将新增', DIFF: '目标机构已有差异配置', NOOP: '配置相同，无需处理', CONFLICT: '编码冲突，无法新增', IMPACT_BLOCKED: '存在会员引用，已阻止覆盖' })[operation] || '需要人工处理' }
function decisionLabel(value) { return ({ CREATE: '自动新增', OVERWRITE: '覆盖', SKIP: '跳过' })[value] || '跳过' }
function quantity(value) { return Number(value || 0).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1') }
function money(value) { return `¥${Number(value || 0).toFixed(2)}` }
function decisionIndex(item) { return Math.max(0, decisionValues.indexOf(item.decision)) }
function periodIndex(deptId) { return Math.max(0, (periods[deptId] || []).findIndex((item) => String(item.periodId) === String(targetPeriodIds[deptId]))) }
function periodLabel(period) { return `${period.periodNo || `周期${period.periodId}`}（${formatDateTime(period.startTime)} 至 ${period.endTime ? formatDateTime(period.endTime) : '当前'}）` }
function periodOptions(deptId) { return (periods[deptId] || []).map((period) => ({ ...period, label: periodLabel(period) })) }
function selectedPeriodLabel(deptId) { const item = (periods[deptId] || []).find((row) => String(row.periodId) === String(targetPeriodIds[deptId])); return item ? periodLabel(item) : '请选择核算周期' }
async function loadSources() { if (!activeType.value) return; loading.value = true; try { const params = { pageNum: 1, pageSize: 200, deptId: currentDeptId }; if (activeType.value.key === 'CAMPAIGN_POLICY') params.status = '1'; sourceRows.value = unwrap(await request({ url: activeType.value.url, method: 'GET', data: params, silent: true })); const targetIndex = sourceRows.value.findIndex((row) => String(row[activeType.value.idKey]) === String(requestedSourceRecordId.value)); sourceIndex.value = targetIndex >= 0 ? targetIndex : 0 } finally { loading.value = false } }
async function changeType(event) { typeIndex.value = Number(event.detail.value); sourceIndex.value = 0; sourceRows.value = []; resetPreview(); await loadSources() }
function changeSource(event) { sourceIndex.value = Number(event.detail.value); resetPreview() }
function toggleDept(id) { const next = selectedDeptIds.value.includes(id) ? selectedDeptIds.value.filter((item) => item !== id) : [...selectedDeptIds.value, id]; selectedDeptIds.value = next; if (activeType.value.key === 'CAMPAIGN_POLICY') next.filter((deptId) => !periods[deptId]).forEach(loadPeriods); resetPreview() }
async function loadPeriods(deptId) { const response = await request({ url: '/finance/accountingPeriod/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId }, silent: true }); periods[deptId] = unwrap(response).filter((period) => String(period.deptId) === String(deptId)) }
function changePeriod(deptId, index) { targetPeriodIds[deptId] = periods[deptId]?.[Number(index)]?.periodId; resetPreview() }
function resetPreview() { details.value = []; batch.value = null }
async function preview() { if (!sourceRecordId.value || !selectedDeptIds.value.length) return uni.showToast({ title: '请选择源配置和目标机构', icon: 'none' }); if (activeType.value.key === 'CAMPAIGN_POLICY' && selectedDeptIds.value.some((id) => !targetPeriodIds[id])) return uni.showToast({ title: '请选择每个目标机构的核算周期', icon: 'none' }); loading.value = true; try { const response = await request({ url: '/member/config-sync/preview', method: 'POST', data: { syncType: activeType.value.key, sourceRecordId: sourceRecordId.value, targetDeptIds: selectedDeptIds.value, targetPeriodIds: activeType.value.key === 'CAMPAIGN_POLICY' ? targetPeriodIds : undefined, idempotencyKey: `${activeType.value.key}-${sourceRecordId.value}-${Date.now()}` } }); const data = response?.data || response; batch.value = data.batch; details.value = (data.details || []).map((item) => ({ ...item, decision: item.operation === 'CREATE' ? 'CREATE' : item.operation === 'DIFF' ? 'OVERWRITE' : 'SKIP' })) } catch (error) { uni.showToast({ title: error?.msg || '预检失败', icon: 'none' }) } finally { loading.value = false } }
function changeDecision(item, event) { item.decision = decisionValues[Number(event.detail.value)] || 'SKIP' }
async function execute() { if (!batch.value) return; if (details.value.some((item) => item.operation === 'IMPACT_BLOCKED')) return uni.showToast({ title: '存在影响会员的等级配置，不能直接覆盖，请先处理', icon: 'none' }); loading.value = true; try { await request({ url: '/member/config-sync/execute', method: 'POST', data: { batchId: batch.value.batchId, previewVersion: batch.value.previewVersion, decisions: details.value.map((item) => ({ detailId: item.detailId, decision: item.decision })) } }); uni.showToast({ title: '配置同步完成', icon: 'success' }); resetPreview() } catch (error) { uni.showToast({ title: error?.msg || '同步失败，可以重试', icon: 'none' }) } finally { loading.value = false } }
onLoad((options = {}) => { requestedType.value = options.type || ''; requestedSourceRecordId.value = options.sourceRecordId || '' })
onMounted(async () => { if (!requireModulePermission('configSync')) return; const index = availableTypes.value.findIndex((item) => item.key === requestedType.value); if (index >= 0) typeIndex.value = index; await loadSources() })
</script>

<style scoped>
.page{min-height:100vh;padding:28rpx 24rpx 150rpx;background:#f5f8fc;box-sizing:border-box}.hero{padding:32rpx;background:linear-gradient(135deg,#087cf0,#2563eb);border-radius:24rpx;color:#fff}.eyebrow{font-size:24rpx;opacity:.8}.title{margin-top:10rpx;font-size:42rpx;font-weight:700}.subtitle{margin-top:16rpx;font-size:25rpx;opacity:.9}.section-card{margin-top:20rpx;padding:26rpx;background:#fff;border-radius:20rpx;box-shadow:0 8rpx 24rpx rgba(15,23,42,.05)}.section-title{margin-bottom:18rpx;color:#1e293b;font-size:29rpx;font-weight:700}.picker-control{display:flex;align-items:center;justify-content:space-between;min-height:78rpx;padding:0 22rpx;border:1rpx solid #dbe4ef;border-radius:12rpx;color:#1e293b}.compact{min-height:66rpx;flex:1;margin-left:16rpx}.arrow{color:#94a3b8;font-size:34rpx}.hint{margin-top:16rpx;color:#64748b;font-size:23rpx;line-height:34rpx}.dept-list{display:flex;flex-wrap:wrap;gap:18rpx 26rpx}.dept-option{display:flex;align-items:center;color:#334155;font-size:26rpx}.period-row{display:flex;align-items:center;margin-top:20rpx;color:#475569;font-size:24rpx}.detail-row{padding:18rpx 0;border-bottom:1rpx solid #edf2f7}.detail-main{display:flex;justify-content:space-between;gap:14rpx;color:#334155;font-size:25rpx}.detail-message{flex:1;text-align:right;color:#64748b}.package-preview{display:flex;flex-wrap:wrap;gap:8rpx;margin-top:12rpx}.package-preview text{padding:6rpx 10rpx;border-radius:8rpx;background:#f1f6ff;color:#2866bd;font-size:21rpx}.decision-picker,.decision-text{margin-top:12rpx;color:#087cf0;font-size:24rpx;text-align:right}.empty{margin-top:40rpx;padding:30rpx;color:#64748b;text-align:center;font-size:25rpx;line-height:38rpx}.footer{position:fixed;left:0;right:0;bottom:0;display:flex;gap:18rpx;padding:22rpx 24rpx calc(22rpx + env(safe-area-inset-bottom));background:#fff;box-shadow:0 -6rpx 20rpx rgba(15,23,42,.08)}button{flex:1;border-radius:14rpx;font-size:28rpx}.primary{background:#087cf0;color:#fff}.secondary{background:#eef4fb;color:#334155}
</style>
