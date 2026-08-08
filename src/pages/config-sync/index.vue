<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">系统管理</text>
      <text class="hero-title">配置同步</text>
      <view class="hero-subtitle">当前源机构：{{ currentDeptName }}</view>
    </view>

    <view class="work-scope">
      <view class="work-scope-mark"></view>
      <view class="work-scope-copy">
        <text class="work-scope-label">跨机构配置同步 · </text>
        <text class="work-scope-name">选择配置类型和目标机构</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll">
      <view class="section-card" v-if="availableTypes.length">
        <view class="section-title">选择配置类型</view>
        <picker :range="availableTypes" range-key="title" :value="typeIndex" @change="changeType">
          <view class="picker-control"><text>{{ activeType.title }}</text><text class="arrow">›</text></view>
        </picker>
      </view>

      <view class="section-card" v-if="activeType && activeType.key !== 'LEVEL'">
        <view class="section-title">选择源配置</view>
        <picker :range="sourceRows" :range-key="activeType.labelKey" :value="sourceIndex" @change="changeSource">
          <view class="picker-control"><text>{{ sourceLabel || `请选择${activeType.title}` }}</text><text class="arrow">›</text></view>
        </picker>
      </view>

      <view class="section-card" v-if="activeType && activeType.key === 'LEVEL'">
        <view class="section-title">会员等级（全量同步）</view>
        <view class="hint-info">已选择同步<strong>{{ sourceRows.length }}</strong>个会员等级，将一次性同步到所有目标机构。</view>
        <view class="hint" style="margin-top:16rpx;">会员等级同步会将当前机构的全部等级一次性同步到目标机构。</view>
      </view>

      <view class="section-card" v-if="activeType && (activeType.key === 'LEVEL' || sourceRecordId)">
        <view class="section-title">
          <text>选择目标机构</text>
          <text class="link-btn" v-if="activeType.key === 'LEVEL' && selectedDeptIds.length !== targetDepts.length" @tap="selectAllDepts">全选</text>
          <text class="link-btn link-btn-secondary" v-else-if="selectedDeptIds.length === targetDepts.length" @tap="clearAllDepts">清空</text>
        </view>
        <view class="dept-list">
          <label class="dept-option" v-for="dept in targetDepts" :key="dept.id">
            <checkbox :value="String(dept.id)" :checked="selectedDeptIds.includes(String(dept.id))" @tap="toggleDept(String(dept.id))" />
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
          <view v-if="item.operation === 'PRODUCT_MISSING' && item.diffSnapshot" class="product-missing-hint">
            <text>{{ item.diffSnapshot.reason || item.diffSnapshot.productName ? '目标机构缺少「' + item.diffSnapshot.productName + '」商品，将同时同步该商品' : '' }}</text>
          </view>
          <view v-if="item.sourceSnapshot?.packages?.length" class="package-preview">
            <text v-for="pkg in item.sourceSnapshot.packages" :key="`${item.detailId}-${pkg.sortNo || pkg.packageName}`">买{{ quantity(pkg.purchaseQuantity) }}送{{ quantity(pkg.giftQuantity) }} · {{ money(pkg.packagePrice) }}</text>
          </view>
          <picker v-if="item.operation === 'DIFF'" :range="decisionOptions" :value="decisionIndex(item)" @change="changeDecision(item, $event.detail.value)">
            <view class="decision-picker">{{ decisionLabel(item.decision) }} ›</view>
          </picker>
          <picker v-else-if="item.operation === 'PRODUCT_MISSING'" :range="['同步商品并创建政策', '跳过']" :value="item.decision === 'SKIP' ? 1 : 0" @change="changeProductMissingDecision(item, $event.detail.value)">
            <view class="decision-picker">{{ item.decision === 'SKIP' ? '跳过' : '同步商品并创建政策' }} ›</view>
          </picker>
          <text v-else class="decision-text">{{ decisionLabel(item.decision) }}</text>
        </view>
      </view>

      <view class="empty" v-if="!availableTypes.length">当前账号没有配置同步权限，请在"小程序权限"中勾选配置同步及对应的同步操作权限。</view>

      <view class="scroll-pad" v-if="activeType"></view>
    </scroll-view>
    <view class="bottom-bar" v-if="activeType">
      <button class="refresh-button" v-if="details.length" @tap="resetPreview">重新预检</button>
      <button class="add-button" :disabled="loading" @tap="details.length ? execute() : preview()">{{ loading ? '处理中…' : (details.length ? '确认同步' : '预检差异') }}</button>
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
function toSnake(camelKey) { return String(camelKey || '').replace(/[A-Z]/g, (m, i) => (i ? '_' : '') + m.toLowerCase()) }
function normalizeKeys(row, camelKeys) { if (!row || typeof row !== 'object') return row; (camelKeys || []).forEach((camelKey) => { if ((row[camelKey] == null || row[camelKey] === '')) { const snake = toSnake(camelKey); if (snake !== camelKey && row[snake] != null && row[snake] !== '') row[camelKey] = row[snake] } }); return row }
function pick(row, camelKey) { if (row == null) return undefined; if (row[camelKey] != null && row[camelKey] !== '') return row[camelKey]; const snake = toSnake(camelKey); if (snake !== camelKey && row[snake] != null && row[snake] !== '') return row[snake]; return row[camelKey] }
const sourceRecordId = computed(() => pick(sourceRows.value[sourceIndex.value], activeType.value?.idKey))
const sourceLabel = computed(() => pick(sourceRows.value[sourceIndex.value], activeType.value?.labelKey) || '')

function unwrap(response) { return response?.rows || response?.data?.rows || response?.data || [] }
function deptName(id) { return targetDepts.value.find((dept) => String(dept.id) === String(id))?.name || String(id) }
function operationLabel(operation) { return ({ CREATE: '目标机构无此配置，将新增', DIFF: '目标机构已有差异配置', NOOP: '配置相同，无需处理', CONFLICT: '编码冲突，无法新增', IMPACT_BLOCKED: '存在会员引用，已阻止覆盖', PRODUCT_MISSING: '目标机构缺少商品，将同时同步' })[operation] || '需要人工处理' }
function decisionLabel(value) { return ({ CREATE: '自动新增', OVERWRITE: '覆盖', SKIP: '跳过' })[value] || '跳过' }
function quantity(value) { return Number(value || 0).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1') }
function money(value) { return `¥${Number(value || 0).toFixed(2)}` }
function decisionIndex(item) { return Math.max(0, decisionValues.indexOf(item.decision)) }
function periodIndex(deptId) { return Math.max(0, (periods[deptId] || []).findIndex((item) => String(item.periodId) === String(targetPeriodIds[deptId]))) }
function periodLabel(period) { return `${period.periodNo || `周期${period.periodId}`}（${formatDateTime(period.startTime)} 至 ${period.endTime ? formatDateTime(period.endTime) : '当前'}）` }
function periodOptions(deptId) { return (periods[deptId] || []).map((period) => ({ ...period, label: periodLabel(period) })) }
function selectedPeriodLabel(deptId) { const item = (periods[deptId] || []).find((row) => String(row.periodId) === String(targetPeriodIds[deptId])); return item ? periodLabel(item) : '请选择核算周期' }
async function loadSources() { if (!activeType.value) return; loading.value = true; try { const params = { pageNum: 1, pageSize: 200, deptId: currentDeptId }; if (activeType.value.key === 'CAMPAIGN_POLICY') params.status = '1'; const rows = unwrap(await request({ url: activeType.value.url, method: 'GET', data: params, silent: true })); sourceRows.value = rows.map((row) => normalizeKeys({ ...row }, [activeType.value.idKey, activeType.value.labelKey, 'productCode', 'productName', 'policyNo', 'policyId', 'policyName', 'supplierId', 'supplierName', 'productId'])); const targetId = String(requestedSourceRecordId.value); const targetIndex = sourceRows.value.findIndex((row) => String(pick(row, activeType.value.idKey)) === targetId); sourceIndex.value = targetIndex >= 0 ? targetIndex : 0 } finally { loading.value = false } }
async function changeType(event) { typeIndex.value = Number(event.detail.value); sourceIndex.value = 0; sourceRows.value = []; resetPreview(); await loadSources() }
function changeSource(event) { sourceIndex.value = Number(event.detail.value); resetPreview() }
function toggleDept(id) { const next = selectedDeptIds.value.includes(id) ? selectedDeptIds.value.filter((item) => item !== id) : [...selectedDeptIds.value, id]; selectedDeptIds.value = next; if (activeType.value.key === 'CAMPAIGN_POLICY') next.filter((deptId) => !periods[deptId]).forEach(loadPeriods); resetPreview() }
function selectAllDepts() { selectedDeptIds.value = targetDepts.value.map((d) => String(d.id)); if (activeType.value?.key === 'CAMPAIGN_POLICY') selectedDeptIds.value.filter((deptId) => !periods[deptId]).forEach(loadPeriods); resetPreview() }
function clearAllDepts() { selectedDeptIds.value = []; resetPreview() }
async function loadPeriods(deptId) { const response = await request({ url: '/finance/accountingPeriod/list', method: 'GET', data: { pageNum: 1, pageSize: 200, deptId }, silent: true }); periods[deptId] = unwrap(response).filter((period) => String(period.deptId) === String(deptId)) }
function changePeriod(deptId, index) { targetPeriodIds[deptId] = periods[deptId]?.[Number(index)]?.periodId; resetPreview() }
function resetPreview() { details.value = []; batch.value = null }
async function preview() { const hasSource = activeType.value?.key === 'LEVEL' ? sourceRows.value.length > 0 : !!sourceRecordId.value; if (!hasSource || !selectedDeptIds.value.length) return uni.showToast({ title: activeType.value?.key === 'LEVEL' ? '请加载会员等级并选择目标机构' : '请选择源配置和目标机构', icon: 'none' }); if (activeType.value.key === 'CAMPAIGN_POLICY' && selectedDeptIds.value.some((id) => !targetPeriodIds[id])) return uni.showToast({ title: '请选择每个目标机构的核算周期', icon: 'none' }); loading.value = true; try { const response = await request({ url: '/member/config-sync/preview', method: 'POST', data: { syncType: activeType.value.key, sourceRecordId: activeType.value?.key === 'LEVEL' ? null : sourceRecordId.value, targetDeptIds: selectedDeptIds.value, targetPeriodIds: activeType.value.key === 'CAMPAIGN_POLICY' ? targetPeriodIds : undefined, idempotencyKey: `${activeType.value.key}-${Date.now()}` } }); const data = response?.data || response; batch.value = data.batch; details.value = (data.details || []).map((item) => { let parsedDiff = item.diffSnapshot; if (typeof parsedDiff === 'string') { try { parsedDiff = JSON.parse(parsedDiff) } catch (_) { parsedDiff = null } } return { ...item, diffSnapshot: parsedDiff, decision: item.operation === 'CREATE' || item.operation === 'PRODUCT_MISSING' ? 'CREATE' : item.operation === 'DIFF' ? 'OVERWRITE' : 'SKIP' } }) } catch (error) { uni.showToast({ title: error?.msg || '预检失败', icon: 'none' }) } finally { loading.value = false } }
function changeDecision(item, event) { item.decision = decisionValues[Number(event.detail.value)] || 'SKIP' }
function changeProductMissingDecision(item, event) { item.decision = Number(event.detail.value) === 1 ? 'SKIP' : 'CREATE' }
async function execute() { if (!batch.value) return; if (details.value.some((item) => item.operation === 'IMPACT_BLOCKED')) return uni.showToast({ title: '存在影响会员的等级配置，不能直接覆盖，请先处理', icon: 'none' }); loading.value = true; try { await request({ url: '/member/config-sync/execute', method: 'POST', data: { batchId: batch.value.batchId, previewVersion: batch.value.previewVersion, decisions: details.value.map((item) => ({ detailId: item.detailId, decision: item.decision })) } }); uni.showToast({ title: '配置同步完成', icon: 'success' }); resetPreview() } catch (error) { uni.showToast({ title: error?.msg || '同步失败，可以重试', icon: 'none' }) } finally { loading.value = false } }
onLoad((options = {}) => { requestedType.value = options.type || ''; requestedSourceRecordId.value = options.sourceRecordId || '' })
onMounted(async () => { if (!requireModulePermission('configSync')) return; const index = availableTypes.value.findIndex((item) => item.key === requestedType.value); if (index >= 0) typeIndex.value = index; await loadSources() })
</script>

<style scoped>
/* ──────────────────────────────────────────────
 * 配置同步页皮肤：与购买记录/等级配置保持一致
 * ────────────────────────────────────────────── */
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:#e7eff7;color:#1e293b;box-sizing:border-box;overflow:hidden}

/* ── 顶部标题栏（左边框 + 浅蓝渐变） ── */
.hero{margin:22rpx 30rpx 0;padding:28rpx 30rpx 30rpx;border-left:5rpx solid #1687f5;border-radius:20rpx;background:linear-gradient(110deg,#d9eaff,#f7faff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08)}
.eyebrow{display:block;color:#1687f5;font-size:24rpx;font-weight:600}
.hero-title{display:block;margin-top:10rpx;color:#1e293b;font-size:38rpx;font-weight:700}
.hero-subtitle{display:block;margin-top:12rpx;color:#5A6B7F;font-size:24rpx}

/* ── 部门范围条 ── */
.work-scope{display:flex;align-items:center;margin:8rpx 30rpx 0;min-height:44rpx}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:16rpx;border-radius:50%;background:#1687f5}
.work-scope-copy{display:flex;align-items:baseline;color:#8192a6;font-size:24rpx}
.work-scope-label{color:#5A6B7F}
.work-scope-name{margin-left:4rpx;color:#26384d;font-size:27rpx;font-weight:700}

/* ── 通用卡片容器（section-card） ── */
.section-card{background:#fff;border-radius:20rpx;padding:28rpx;margin:16rpx 30rpx 0;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box}
.section-title{margin-bottom:18rpx;color:#1e293b;font-size:28rpx;font-weight:700}

/* ── 下拉选择控件 ── */
.picker-control{display:flex;align-items:center;justify-content:space-between;min-height:84rpx;padding:0 24rpx;border:1rpx solid #D5E0EC;border-radius:14rpx;background:#F5F8FA;color:#1A2332;font-size:28rpx}
.compact{min-height:66rpx;flex:1;margin-left:16rpx}
.arrow{color:#94a3b8;font-size:34rpx}
.hint{margin-top:16rpx;color:#64748b;font-size:23rpx;line-height:34rpx}

/* ── 机构选择列表 ── */
.dept-list{display:flex;flex-wrap:wrap;gap:18rpx 26rpx}
.dept-option{display:flex;align-items:center;color:#334155;font-size:26rpx}
.period-row{display:flex;align-items:center;margin-top:20rpx;color:#475569;font-size:24rpx}

/* ── 预检结果 ── */
.detail-row{padding:18rpx 0;border-bottom:1rpx solid #edf2f7}
.detail-main{display:flex;justify-content:space-between;gap:14rpx;color:#334155;font-size:25rpx}
.detail-message{flex:1;text-align:right;color:#64748b}
.package-preview{display:flex;flex-wrap:wrap;gap:8rpx;margin-top:12rpx}
.package-preview text{padding:6rpx 10rpx;border-radius:8rpx;background:#f1f6ff;color:#2866bd;font-size:21rpx}
.decision-picker,.decision-text{margin-top:12rpx;color:#087cf0;font-size:24rpx;text-align:right}
.product-missing-hint{margin-top:12rpx;padding:16rpx 20rpx;border-radius:12rpx;background:linear-gradient(135deg,#FFF7ED,#FFFBF5);border:1rpx solid #FED7AA;color:#92400E;font-size:23rpx;line-height:32rpx}

/* ── 空状态 ── */
.empty{margin:40rpx 30rpx;padding:30rpx;color:#64748b;text-align:center;font-size:25rpx;line-height:38rpx}

/* ── 浮动底部操作栏 ── */
.scroll{flex:1;width:100%;min-height:0;padding:8rpx 0 200rpx;box-sizing:border-box;overflow-x:hidden}
.scroll-pad{height:8rpx}
.bottom-bar{position:fixed;left:0;right:0;bottom:0;display:flex;justify-content:center;gap:16rpx;padding:20rpx 24rpx;padding-bottom:calc(20rpx + env(safe-area-inset-bottom));background:rgba(255,255,255,.96);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-top:1rpx solid #E2E8F0;z-index:10}
.bottom-bar .add-button{width:320rpx;height:84rpx;line-height:84rpx;background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#FFF;font-size:28rpx;border-radius:999rpx;text-align:center;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25);border:0;padding:0}
.bottom-bar .add-button::after{border:none}
.bottom-bar .add-button[disabled]{opacity:.5}
.bottom-bar .refresh-button{width:160rpx;height:84rpx;line-height:84rpx;background:#EEF3F8;color:#334155;font-size:28rpx;border-radius:999rpx;border:0;padding:0}
.bottom-bar .refresh-button::after{border:none}
.section-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:18rpx;color:#1e293b;font-size:28rpx;font-weight:700}
.link-btn{font-size:24rpx;font-weight:500;color:#087CF0;background:#edf5ff;padding:8rpx 20rpx;border-radius:999rpx}
.link-btn-secondary{color:#64748b;background:#EEF2F7}
.hint-info{padding:20rpx 24rpx;border-radius:14rpx;background:linear-gradient(135deg,#F0F7FF,#FAFCFF);border:1rpx solid #D6E8FF;color:#334155;font-size:24rpx;line-height:34rpx}
.hint-info strong{color:#087CF0;font-weight:700;margin:0 6rpx}
</style>
