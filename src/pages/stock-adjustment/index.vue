<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="eyebrow">库存与成本</text>
        <text class="hero-title">库存调整</text>
        <text class="hero-note">当前部门：{{ currentDeptName || '未选择部门' }}</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="refresh">
      <view v-for="item in rows" :key="item.batchId" class="card row-card" @tap="openDetail(item)">
        <view class="row-head"><text class="batch-no">{{ item.batchNo || '库存调整单' }}</text><text class="status" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</text></view>
        <view class="row-meta"><text>{{ adjustmentTypeLabel(item.adjustmentType) }}</text><text>{{ item.adjustmentDate || item.initDate || '-' }}</text></view>
        <view class="row-foot"><text>{{ currentDeptName }}</text><view class="actions"><text v-if="canEdit(item) && canUpdateAdjustment" class="link" @tap.stop="openEditFromRow(item)">编辑</text><text v-if="canEdit(item) && canRemoveAdjustment" class="link danger" @tap.stop="remove(item)">删除</text><text v-if="item.status === 'DRAFT' && canUpdateAdjustment" class="link" @tap.stop="validate(item)">校验</text><text v-if="item.status === 'VALIDATED' && canUpdateAdjustment" class="link" @tap.stop="submit(item)">提交</text><text v-if="item.status === 'SUBMITTED' && canApproveAdjustment" class="link" @tap.stop="approve(item)">审批</text><text v-if="item.status === 'APPROVED' && canPostAdjustment" class="link" @tap.stop="post(item)">过账</text></view></view>
      </view>
      <StateView v-if="stateStatus !== 'normal'" :status="stateStatus" @retry="load" />
    </scroll-view>
    <view class="bottom-action" v-if="canCreateAdjustment"><button class="primary add-button" @tap="openCreate">新增调整</button></view>

    <view v-if="editorVisible" class="mask" @tap="closeEditor">
      <view class="sheet" @tap.stop>
        <view class="sheet-head"><text>{{ editingId ? '编辑库存调整' : '新增库存调整' }}</text><text class="close" @tap="closeEditor">×</text></view>
        <scroll-view scroll-y class="form-scroll">
          <view class="field"><text class="label">调整类型</text><picker :range="typeOptions" range-key="label" :value="typeIndex" @change="changeType"><view class="picker">{{ selectedType.label || '请选择调整类型' }} <text>⌄</text></view></picker><text v-if="selectedType.remark" class="rule-warning">规则：{{ selectedType.remark }}</text></view>
          <view v-if="form.adjustmentType === 'OTHER'" class="field"><text class="label">库存方向</text><view class="direction-options"><view class="direction-option" :class="{ active: form.adjustmentDirection === 'INCREASE' }" @tap="selectDirection('INCREASE')">增加库存</view><view class="direction-option" :class="{ active: form.adjustmentDirection === 'DECREASE' }" @tap="selectDirection('DECREASE')">减少库存</view></view></view>
          <view class="field"><text class="label">调整日历</text><picker mode="date" :value="form.adjustmentDate" @change="e => form.adjustmentDate = e.detail.value"><view class="picker">{{ form.adjustmentDate }} <text>⌄</text></view></picker></view>
          <view class="items-head"><text class="label">商品明细</text><text class="link" @tap="addItem">＋添加商品</text></view>
          <view v-for="(item, index) in form.items" :key="item.rowKey" class="item-editor">
            <view class="item-editor-head"><text>商品 {{ index + 1 }}</text><text v-if="form.items.length > 1" class="link danger" @tap="removeItem(index)">删除</text></view>
            <view class="field"><picker :range="products" range-key="productName" :value="item.productIndex" @change="e => changeProduct(index, e)"><view class="picker">{{ products[item.productIndex]?.productName || '请选择商品' }} <text>⌄</text></view></picker></view>
            <view class="grid"><view class="field"><text class="label">调整数量</text><input class="input" type="digit" v-model="item.quantity" placeholder="" /><text class="field-hint">最多三位小数</text></view><view class="field"><text class="label">单位成本（元）</text><input class="input" type="digit" v-model="item.unitCost" placeholder="" /><text class="field-hint">最多两位小数</text></view></view>
          </view>
          <view class="field"><text class="label">备注</text><textarea class="textarea" v-model="form.remark" placeholder="可选" /></view>
          <button class="primary submit" :loading="submitting" @tap="save">保存调整单</button>
        </scroll-view>
      </view>
    </view>
    <view v-if="detailVisible" class="mask" @tap="detailVisible = false">
      <view class="sheet detail-sheet" @tap.stop>
        <view class="sheet-head"><text>库存调整明细</text><text class="close" @tap="detailVisible = false">×</text></view>
        <scroll-view scroll-y class="form-scroll"><view class="detail-summary"><view class="detail-line"><text>批次号</text><strong>{{ detailBatch.batchNo || '-' }}</strong></view><view class="detail-line"><text>调整类型</text><strong>{{ adjustmentTypeLabel(detailBatch.adjustmentType) }}</strong></view><view class="detail-line"><text>调整日历</text><strong>{{ detailBatch.adjustmentDate || detailBatch.initDate || '-' }}</strong></view><view class="detail-line"><text>状态</text><strong>{{ statusLabel(detailBatch.status) }}</strong></view></view><view class="detail-section-title">商品明细</view><view v-for="row in detailItems" :key="row.itemId || row.productId" class="detail-item-card"><view class="detail-item-head"><strong>{{ row.productName || '-' }}</strong><text>{{ row.quantity || 0 }}</text></view><view class="detail-item-meta"><text>单位成本 ¥{{ Number(row.unitCost || 0).toFixed(2) }}</text><text>金额 ¥{{ (Number(row.quantity || 0) * Number(row.unitCost || 0)).toFixed(2) }}</text></view></view></scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { workContext } from '@/utils/workContext.js'
import { getActionCapabilities, requireModulePermission } from '@/utils/permission.js'
import { request } from '@/api/index.js'
import { dictCache } from '@/utils/dictCache.js'
import { getStockValueReport } from '@/api/stock.js'
import { listStockInit, getStockInitDetail, createStockInit, updateStockInit, deleteStockInit, validateStockInit, submitStockInit, approveStockInit, postStockInit } from '@/api/stockInit.js'
import StateView from '@/components/StateView.vue'

function createEmptyForm() {
  return { deptId: null, adjustmentDate: new Date().toISOString().slice(0, 10), adjustmentType: '', adjustmentDirection: 'INCREASE', remark: '', items: [createEmptyItem()] }
}

function createEmptyItem(item = {}) {
  return { rowKey: item.itemId || `row-${Date.now()}-${Math.random()}`, productId: item.productId || null, productIndex: -1, quantity: item.quantity ?? '', unitCost: item.unitCost ?? '' }
}

export default {
  components: { StateView },
  data() { return { rows: [], loading: false, loadError: '', refreshing: false, currentDeptId: null, currentDeptName: '', editorVisible: false, detailVisible: false, submitting: false, editingId: null, detail: null, products: [], typeOptions: [], typeIndex: 0, capabilities: {}, form: createEmptyForm() } },
  computed: {
    selectedType() { return this.typeOptions[this.typeIndex] || {} },
    canCreateAdjustment() { return this.capabilities.add === true },
    canUpdateAdjustment() { return this.capabilities.add === true },
    canRemoveAdjustment() { return this.capabilities.remove === true },
    canApproveAdjustment() { return this.capabilities.approve === true },
    canPostAdjustment() { return this.capabilities.post === true },
    detailBatch() { const detail = this.detail || {}; const data = detail.data || {}; return data.batch || detail.batch || {} },
    detailItems() { const detail = this.detail || {}; const data = detail.data || {}; return data.items || detail.items || [] },
    stateStatus() { if (this.loading && !this.rows.length) return 'loading'; if (this.loadError && !this.rows.length) return 'error'; if (!this.rows.length) return 'empty'; return 'normal' },
  },
  onLoad() { this.syncContext(); this.capabilities = getActionCapabilities('stockAdjustment', ['add', 'remove', 'approve', 'post']); if (requireModulePermission('stockAdjustment')) this.load() },
  onShow() { if (this.currentDeptId) this.syncContext() },
  methods: {
    emptyForm() { return createEmptyForm() },
    syncContext() { const s = workContext.snapshot(); this.currentDeptId = s.currentDeptId; this.currentDeptName = s.currentDept?.name || ''; if (this.currentDeptId && this.editorVisible === false) this.load() },
    async load() { if (!this.currentDeptId) { this.rows = []; return } this.loading = true; this.loadError = ''; try { const res = await listStockInit({ deptId: this.currentDeptId, pageNum: 1, pageSize: 50 }); const rows = res.rows || res.data?.rows || []; this.rows = rows.filter(item => String(item.deptId) === String(this.currentDeptId)) } catch (e) { this.loadError = e.msg || '调整单加载失败'; uni.showToast({ title: this.loadError, icon: 'none' }) } finally { this.loading = false; this.refreshing = false } },
    refresh() { this.refreshing = true; this.syncContext() },
    async loadOptions() { const [dict, productRes] = await Promise.all([dictCache.get('finance_stock_adjustment_type', async () => { const dictRes = await request({ url: '/system/dict/data/type/finance_stock_adjustment_type', method: 'GET' }); return dictRes.data || dictRes.rows || [] }), request({ url: '/finance/product/selector', method: 'GET' })]); this.typeOptions = dict.map(x => ({ label: x.dictLabel, value: x.dictValue, direction: x.dictValue === 'OTHER' || x.remark?.includes('选择方向') || x.remark?.includes('增减') ? 'BOTH' : x.remark?.includes('减少') ? 'DECREASE' : 'INCREASE', remark: x.remark || '' })); this.products = productRes.data || productRes.rows || [] },
    async openCreate() { this.editingId = null; this.form = this.emptyForm(); this.typeIndex = 0; try { await this.loadOptions(); if (this.typeOptions.length) this.form.adjustmentType = this.typeOptions[0].value; this.editorVisible = true } catch (e) { uni.showToast({ title: '基础数据加载失败', icon: 'none' }) } },
    async openDetail(item) { try { this.detail = await getStockInitDetail(item.batchId); const batch = this.detail.data?.batch || this.detail.batch || item; if (this.canEdit(batch)) await this.openEdit(batch); else this.detailVisible = true } catch (e) { uni.showToast({ title: e.msg || '加载详情失败', icon: 'none' }) } },
    openEditFromRow(item) { this.openDetail(item) },
    async openEdit(item) { await this.loadOptions(); this.editingId = item.batchId; const detailItems = this.detail?.data?.items || this.detail?.items || []; this.form = { ...this.emptyForm(), ...item, deptId: this.currentDeptId, items: (detailItems.length ? detailItems : [item]).map(row => createEmptyItem(row)) }; this.form.items.forEach(row => { row.productIndex = Math.max(0, this.products.findIndex(x => String(x.productId) === String(row.productId))) }); this.typeIndex = Math.max(0, this.typeOptions.findIndex(x => x.value === this.form.adjustmentType)); this.editorVisible = true },
    closeEditor() { if (!this.submitting) this.editorVisible = false },
    changeType(e) { this.typeIndex = Number(e.detail.value); this.form.adjustmentType = this.selectedType.value; this.form.adjustmentDirection = this.selectedType.direction === 'DECREASE' ? 'DECREASE' : 'INCREASE' },
    selectDirection(direction) { this.form.adjustmentDirection = direction },
    addItem() { this.form.items.push(createEmptyItem()) },
    removeItem(index) { if (this.form.items.length > 1) this.form.items.splice(index, 1) },
    changeProduct(index, e) { const productIndex = Number(e.detail.value); const p = this.products[productIndex] || {}; const item = this.form.items[index]; item.productIndex = productIndex; item.productId = p.productId; item.unitCost = Number(p.purchasePrice ?? p.buyPrice ?? p.price ?? 0).toFixed(2) },
    adjustmentTypeLabel(value) { const item = this.typeOptions.find(x => x.value === value); return item?.label || ({ OPENING_STOCK: '期初库存录入', HISTORY_REPLENISH: '历史数据补录', TRIAL_CONSUMPTION: '试用消耗', STORE_USE: '店面自用', DAMAGE_LOSS: '报损', OTHER: '其他' })[value] || value || '库存调整' },
    canEdit(item) { return ['DRAFT', 'VALIDATED', 'SUBMITTED', 'APPROVED'].includes(item.status) },
    statusLabel(s) { return ({ DRAFT: '草稿', VALIDATED: '已校验', SUBMITTED: '已提交', APPROVED: '已审批', POSTED: '已过账' })[s] || s || '-' },
    statusClass(s) { return s === 'POSTED' ? 'posted' : s === 'APPROVED' ? 'approved' : 'draft' },
    async save() { if (!this.currentDeptId || !this.form.adjustmentType || !this.form.items.length) return uni.showToast({ title: '请完整填写调整类型和商品', icon: 'none' }); const items = []; for (const item of this.form.items) { const quantity = Number(item.quantity); const unitCost = Number(item.unitCost); if (!item.productId) return uni.showToast({ title: '请为每一行选择商品', icon: 'none' }); if (!Number.isFinite(quantity) || quantity <= 0 || String(item.quantity).split('.')[1]?.length > 3) return uni.showToast({ title: '调整数量必须大于0且最多三位小数', icon: 'none' }); if (!Number.isFinite(unitCost) || unitCost < 0 || String(item.unitCost).split('.')[1]?.length > 2) return uni.showToast({ title: '单位成本最多两位小数', icon: 'none' }); items.push({ productId: item.productId, quantity, unitCost }) } this.submitting = true; const data = { deptId: this.currentDeptId, adjustmentDate: this.form.adjustmentDate, initDate: this.form.adjustmentDate, adjustmentType: this.form.adjustmentType, adjustmentDirection: this.form.adjustmentDirection, remark: this.form.remark, items }; try { if (this.editingId) await updateStockInit(this.editingId, { ...data, version: this.form.version }); else await createStockInit(data); uni.showToast({ title: '保存成功', icon: 'success' }); this.editorVisible = false; await this.load() } catch (e) { uni.showToast({ title: e.msg || '保存失败', icon: 'none' }) } finally { this.submitting = false } },
    async remove(item) { if (!this.canEdit(item)) return; const ok = await new Promise(resolve => uni.showModal({ title: '删除调整单', content: '未过账单据可以删除，确定继续吗？', success: r => resolve(r.confirm) })); if (!ok) return; try { await deleteStockInit(item.batchId, item.version); uni.showToast({ title: '已删除', icon: 'success' }); this.load() } catch (e) { uni.showToast({ title: e.msg || '删除失败', icon: 'none' }) } },
    async submit(item) { if (!this.canEdit(item)) return; try { await submitStockInit(item.batchId, item.version); uni.showToast({ title: '已提交' }); this.load() } catch (e) { uni.showToast({ title: e.msg || '提交失败', icon: 'none' }) } }
    ,async validate(item) { try { await validateStockInit(item.batchId, item.version); uni.showToast({ title: '校验通过' }); this.load() } catch (e) { uni.showToast({ title: e.msg || '校验失败', icon: 'none' }) } }
    ,async approve(item) { try { await approveStockInit(item.batchId, item.version); uni.showToast({ title: '审批通过' }); this.load() } catch (e) { uni.showToast({ title: e.msg || '审批失败', icon: 'none' }) } }
    ,async post(item) { try { await postStockInit(item.batchId, item.version); uni.showToast({ title: '过账成功' }); this.load() } catch (e) { uni.showToast({ title: e.msg || '过账失败', icon: 'none' }) } }
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#f4f7fb}.hero{display:flex;justify-content:space-between;align-items:center;padding:30rpx 28rpx 26rpx;background:#087cf0;color:#fff}.eyebrow{display:block;font-size:23rpx;opacity:.8}.hero-title{display:block;margin-top:7rpx;font-size:42rpx;font-weight:700}.hero-note{display:block;margin-top:9rpx;font-size:23rpx;opacity:.85}.primary{border:0;background:#087cf0;color:#fff;border-radius:10rpx;font-size:28rpx}.primary.small{margin:0;padding:0 22rpx;height:70rpx;line-height:70rpx;background:#fff;color:#087cf0}.scroll{height:calc(100vh - 190rpx);padding:22rpx 24rpx;box-sizing:border-box}.card{background:#fff;border-radius:16rpx}.row-card{padding:24rpx;margin-bottom:18rpx}.row-head,.row-meta,.row-foot{display:flex;justify-content:space-between;align-items:center}.batch-no{font-size:29rpx;font-weight:600}.status{padding:6rpx 14rpx;border-radius:20rpx;font-size:22rpx}.status.draft{color:#b7791f;background:#fff8e6}.status.approved{color:#197341;background:#eaf8ef}.status.posted{color:#1769aa;background:#eaf4ff}.row-meta{margin-top:18rpx;color:#718096;font-size:24rpx}.row-foot{margin-top:20rpx;color:#94a3b8;font-size:23rpx}.actions{display:flex;gap:18rpx}.link{color:#087cf0}.danger{color:#dc5b5b}.empty{text-align:center;padding:120rpx 0;color:#94a3b8;font-size:27rpx}.mask{position:fixed;inset:0;background:rgba(15,23,42,.42);z-index:10}.sheet{position:absolute;left:0;right:0;bottom:0;max-height:88vh;background:#f7f9fc;border-radius:26rpx 26rpx 0 0}.sheet-head{display:flex;justify-content:space-between;padding:30rpx;font-size:34rpx;font-weight:700;background:#fff;border-radius:26rpx 26rpx 0 0}.close{font-size:42rpx;font-weight:400;color:#94a3b8}.form-scroll{max-height:calc(88vh - 100rpx);padding:0 28rpx calc(38rpx + env(safe-area-inset-bottom));box-sizing:border-box}.field{margin-top:22rpx}.label{display:block;margin-bottom:10rpx;color:#475569;font-size:24rpx}.picker,.input,.textarea{box-sizing:border-box;width:100%;padding:22rpx;border:1rpx solid #dbe4ef;border-radius:12rpx;background:#fff;color:#1e293b;font-size:28rpx}.input{display:block;height:78rpx;line-height:34rpx;padding:18rpx 20rpx;overflow:hidden;white-space:nowrap}.picker{display:flex;justify-content:space-between}.textarea{height:130rpx}.grid{display:flex;gap:16rpx}.grid .field{flex:1;min-width:0}.rule,.field-hint{display:block;margin-top:10rpx;color:#64748b;font-size:22rpx}.direction-options{display:flex;gap:16rpx}.direction-option{flex:1;padding:18rpx;text-align:center;border:1rpx solid #dbe4ef;border-radius:12rpx;background:#fff;color:#64748b;font-size:26rpx}.direction-option.active{border-color:#087cf0;background:#eaf4ff;color:#087cf0;font-weight:600}.notice{margin-top:22rpx;padding:18rpx;border-radius:12rpx;font-size:24rpx;line-height:34rpx}.notice.increase{background:#edf9f1;color:#197341}.notice.decrease{background:#fff4ed;color:#b45309}.submit{width:100%;height:88rpx;line-height:88rpx;margin-top:28rpx;margin-bottom:30rpx}
.rule-warning{display:block;margin-top:10rpx;padding:14rpx 16rpx;border:2rpx solid #ef4444;border-radius:10rpx;background:#fff1f2;color:#dc2626;font-size:22rpx;font-weight:700}
.detail-summary{padding:6rpx 0 18rpx}.detail-line{display:flex;justify-content:space-between;align-items:center;padding:22rpx 0;border-bottom:1rpx solid #e5eaf1;color:#64748b;font-size:25rpx}.detail-line strong{color:#1e293b;font-weight:600}.detail-section-title{padding:24rpx 0 12rpx;color:#1e293b;font-size:28rpx;font-weight:700}.detail-item-card{padding:22rpx;margin-bottom:14rpx;border:1rpx solid #dbe4ef;border-radius:14rpx;background:#fff}.detail-item-head,.detail-item-meta{display:flex;justify-content:space-between;align-items:center}.detail-item-head{color:#1e293b;font-size:27rpx}.detail-item-meta{margin-top:14rpx;color:#64748b;font-size:23rpx}
.bottom-action{position:fixed;left:0;right:0;bottom:0;z-index:5;display:flex;justify-content:center;padding:16rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));background:rgba(244,247,251,.96);box-sizing:border-box}.add-button{width:82%;height:82rpx;line-height:82rpx;margin:0;box-shadow:0 8rpx 22rpx rgba(8,124,240,.2)}
</style>
