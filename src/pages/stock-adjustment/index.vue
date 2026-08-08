<template>
  <view class="page">
    <view class="hero"><view><text class="eyebrow">库存管理</text><text class="hero-title">库存调整</text></view></view>
    <view class="work-scope" hover-class="work-scope-hover" hover-stay-time="80" hover-start-time="30" @tap="openDeptSwitcher"><view class="work-scope-mark"></view><view class="work-scope-copy"><text class="work-scope-label">{{ scopeLabel }}</text><text class="work-scope-name">{{ currentDeptName || '未选择部门' }}</text></view></view>
    <view class="permission-note"><text>调整单 {{ rows.length }} 笔</text><text :class="capabilities.add ? 'can-edit' : 'read-only'">{{ capabilities.add ? '可编辑' : '只读' }}</text></view>

    <scroll-view scroll-y class="scroll adjustment-scroll" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="refresh">
      <view class="section-card adjustment-list-card"><view class="section-header"><view class="section-dot" style="background:#F59E0B"></view><text class="section-title">调整单列表</text><text class="section-link">点击单据查看明细</text></view>
      <view class="record-card" v-for="item in rows" :key="item.batchId" @tap="openDetail(item)">
        <view class="card-bar"></view>
        <view class="card-body">
          <view class="card-header">
            <view class="record-title">库存调整单</view>
            <view class="record-id">NO. {{ item.batchNo || '未命名单据' }}</view>
          </view>
          <view class="summary-grid">
            <view class="summary-item">
              <text class="summary-label">调整类型</text>
              <text class="summary-value">{{ adjustmentTypeLabel(item.adjustmentType) }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">调整日期</text>
              <text class="summary-value">{{ item.adjustmentDate || item.initDate || '-' }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">商品数</text>
              <text class="summary-value">{{ (item.items && item.items.length) || 0 }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">状态</text>
              <text class="summary-value" :class="statusTone(item.status)">{{ statusLabel(item.status) }}</text>
            </view>
          </view>
          <view class="card-footer">
            <text class="meta-text">归属部门 {{ currentDeptName || '-' }}</text>
            <text class="arrow-icon">›</text>
          </view>
        </view>
      </view>
      <StateView v-if="stateStatus !== 'normal'" :status="stateStatus" @retry="load" />
      </view>
    </scroll-view>
    <view class="bottom-action" v-if="canCreateAdjustment"><button class="primary add-button" @tap="openCreate">新增调整</button></view>

    <view v-if="editorVisible" class="mask" @tap="closeEditor">
      <view class="sheet adjustment-sheet" @tap.stop>
        <view class="sheet-head"><text>{{ editingId ? '编辑库存调整' : '新增库存调整' }}</text><text class="close" @tap="closeEditor">×</text></view>
        <view v-if="editorLoading" class="sheet-loading">加载调整单...</view>
        <scroll-view v-else scroll-y class="form-scroll">
          <view class="field"><text class="label">调整类型</text><picker :range="typeOptions" range-key="label" :value="typeIndex" @change="changeType"><view class="picker">{{ selectedType.label || '请选择调整类型' }} <text>⌄</text></view></picker><text v-if="selectedType.remark" class="rule-warning">规则：{{ selectedType.remark }}</text></view>
          <view v-if="form.adjustmentType === 'OTHER'" class="field"><text class="label">库存方向</text><view class="direction-options"><view class="direction-option" :class="{ active: form.adjustmentDirection === 'INCREASE' }" @tap="selectDirection('INCREASE')">增加库存</view><view class="direction-option" :class="{ active: form.adjustmentDirection === 'DECREASE' }" @tap="selectDirection('DECREASE')">减少库存</view></view></view>
          <view class="field"><text class="label">调整日历</text><picker mode="date" :value="form.adjustmentDate" @change="e => form.adjustmentDate = e.detail.value"><view class="picker">{{ form.adjustmentDate }} <text>⌄</text></view></picker></view>
          <view class="items-head"><text class="label">商品明细</text><text class="link" @tap="addItem">＋添加商品</text></view>
          <view v-for="(item, index) in form.items" :key="item.rowKey" class="item-editor">
            <view class="item-editor-head"><text>商品 {{ index + 1 }}</text><text v-if="form.items.length > 1" class="link danger" @tap="removeItem(index)">删除</text></view>
            <view class="field"><picker :range="products" range-key="productName" :value="item.productIndex" @change="e => changeProduct(index, e)"><view class="picker">{{ products[item.productIndex]?.productName || '请选择商品' }} <text>⌄</text></view></picker></view>
            <view class="grid"><view class="field"><text class="label">调整数量</text><input class="input" type="digit" v-model="item.quantity" placeholder="0.000" /><text class="field-hint">最多三位小数</text></view><view class="field"><text class="label">单位成本（元）</text><input class="input" type="digit" v-model="item.unitCost" placeholder="0.00" /><text class="field-hint">最多两位小数</text></view></view>
          </view>
          <view class="field"><text class="label">备注</text><textarea class="textarea" v-model="form.remark" placeholder="可选" /></view>
          <button class="btn-primary submit" :loading="submitting" @tap="save">保存调整单</button>
        </scroll-view>
      </view>
    </view>
    <view v-if="detailVisible" class="mask" @tap="detailVisible = false">
      <view class="sheet adjustment-sheet detail-sheet" @tap.stop>
        <view class="sheet-head"><text>库存调整明细</text></view>
        <view v-if="detailLoading" class="sheet-loading">加载调整单详情...</view>
        <scroll-view v-else scroll-y class="form-scroll detail-form-scroll"><view class="detail-summary"><view class="detail-line"><text>批次号</text><strong>{{ detailBatch.batchNo || '-' }}</strong></view><view class="detail-line"><text>调整类型</text><strong>{{ adjustmentTypeLabel(detailBatch.adjustmentType) }}</strong></view><view class="detail-line"><text>调整日历</text><strong>{{ detailBatch.adjustmentDate || detailBatch.initDate || '-' }}</strong></view><view class="detail-line"><text>状态</text><strong>{{ statusLabel(detailBatch.status) }}</strong></view></view><view class="detail-section-title">商品明细</view><view v-for="row in detailItems" :key="row.itemId || row.productId" class="detail-item-card"><view class="detail-item-head"><strong>{{ row.productName || '-' }}</strong><text>{{ row.quantity || 0 }}</text></view><view class="detail-item-meta"><text>单位成本 ¥{{ Number(row.unitCost || 0).toFixed(2) }}</text><text>金额 ¥{{ (Number(row.quantity || 0) * Number(row.unitCost || 0)).toFixed(2) }}</text></view></view></scroll-view><view v-if="!detailLoading && hasDetailActions" class="detail-bottom-actions"><button v-if="detailBatch.status === 'DRAFT' && canUpdateAdjustment" class="detail-btn" @tap="editFromDetail">编辑</button><button v-if="detailBatch.status === 'DRAFT' && canRemoveAdjustment" class="detail-btn danger" @tap="remove(detailBatch)">删除</button><button v-if="detailBatch.status === 'DRAFT' && canUpdateAdjustment" class="detail-btn" @tap="validate(detailBatch)">校验</button><button v-if="detailBatch.status === 'VALIDATED' && canUpdateAdjustment" class="detail-btn" @tap="submit(detailBatch)">提交</button><button v-if="detailBatch.status === 'SUBMITTED' && canApproveAdjustment" class="detail-btn" @tap="approve(detailBatch)">审批</button><button v-if="detailBatch.status === 'APPROVED' && canPostAdjustment" class="detail-btn" @tap="post(detailBatch)">过账</button></view>
      </view>
    </view>
    <dept-switcher
      v-model:visible="showDeptSwitcher"
      :current-dept-id="currentDeptId"
      :request-fn="request"
      @change="onDeptSwitcherChanged"
    />
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
import DeptSwitcher from '@/components/DeptSwitcher.vue'
import { getStatusBarHeight } from '@/utils/systemInfo.js'
import { applyWorkScopeToPage, openDeptSwitcher, handleDeptChanged } from '@/utils/listWorkScope.js'

function createEmptyForm() {
  return { deptId: null, adjustmentDate: new Date().toISOString().slice(0, 10), adjustmentType: '', adjustmentDirection: 'INCREASE', remark: '', items: [createEmptyItem()] }
}

function createEmptyItem(item = {}) {
  return { rowKey: item.itemId || `row-${Date.now()}-${Math.random()}`, productId: item.productId || null, productIndex: -1, quantity: item.quantity ?? '', unitCost: item.unitCost ?? '' }
}

export default {
  components: { StateView, DeptSwitcher },
  data() { return { rows: [], loading: false, loadError: '', refreshing: false, showDeptSwitcher: false, scopeLabel: '暂无可用数据范围', contextVersion: 0, currentDeptId: null, currentDeptName: '未选择部门', statusBarH: 0, menuButton: null, editorVisible: false, detailVisible: false, editorLoading: false, detailLoading: false, submitting: false, editingId: null, detail: null, products: [], typeOptions: [], typeIndex: 0, capabilities: {}, form: createEmptyForm() } },
  computed: {
    selectedType() { return this.typeOptions[this.typeIndex] || {} },
    canCreateAdjustment() { return this.capabilities.add === true },
    canUpdateAdjustment() { return this.capabilities.add === true },
    canRemoveAdjustment() { return this.capabilities.remove === true },
    canApproveAdjustment() { return this.capabilities.approve === true },
    canPostAdjustment() { return this.capabilities.post === true },
    detailBatch() { const detail = this.detail || {}; const data = detail.data || {}; return data.batch || detail.batch || {} },
    detailItems() { const detail = this.detail || {}; const data = detail.data || {}; return data.items || detail.items || [] }, hasDetailActions() { const s = this.detailBatch.status; if (!s) return false; if (s === 'DRAFT' && (this.canUpdateAdjustment || this.canRemoveAdjustment)) return true; if (s === 'VALIDATED' && this.canUpdateAdjustment) return true; if (s === 'SUBMITTED' && this.canApproveAdjustment) return true; if (s === 'APPROVED' && this.canPostAdjustment) return true; return false },
    headerContentStyle() { const top = this.menuButton?.bottom ? this.menuButton.bottom + 8 : this.statusBarH + 48; return { paddingTop: top + 'px' } },
    stateStatus() { if (this.loading && !this.rows.length) return 'loading'; if (this.loadError && !this.rows.length) return 'error'; if (!this.rows.length) return 'empty'; return 'normal' },
  },
  onLoad() { this.statusBarH = getStatusBarHeight(); try { this.menuButton = uni.getMenuButtonBoundingClientRect() } catch (_) { this.menuButton = null }; applyWorkScopeToPage(this); this.capabilities = getActionCapabilities('stockAdjustment', ['add', 'remove', 'approve', 'post']); if (requireModulePermission('stockAdjustment')) this.load() },
  onShow() { const { departmentChanged } = applyWorkScopeToPage(this); if (departmentChanged && this.currentDeptId) this.load() },
  methods: {
    openDeptSwitcher() { return openDeptSwitcher(this) },
    onDeptSwitcherChanged() { return handleDeptChanged(this, () => this.load()) },
    emptyForm() { return createEmptyForm() },
    syncContext() { applyWorkScopeToPage(this); if (this.currentDeptId && this.editorVisible === false) this.load() },
    async load() { if (!this.currentDeptId) { this.rows = []; return } this.loading = true; this.loadError = ''; try { const res = await listStockInit({ deptId: this.currentDeptId, pageNum: 1, pageSize: 50 }); const rows = res.rows || res.data?.rows || []; this.rows = rows.filter(item => String(item.deptId) === String(this.currentDeptId)) } catch (e) { this.loadError = e.msg || '调整单加载失败'; uni.showToast({ title: this.loadError, icon: 'none' }) } finally { this.loading = false; this.refreshing = false } },
    refresh() { this.refreshing = true; this.syncContext() },
    async loadOptions() { const [dict, productRes] = await Promise.all([dictCache.get('finance_stock_adjustment_type', async () => { const dictRes = await request({ url: '/system/dict/data/type/finance_stock_adjustment_type', method: 'GET' }); return dictRes.data || dictRes.rows || [] }), request({ url: '/finance/product/selector', method: 'GET' })]); this.typeOptions = dict.map(x => ({ label: x.dictLabel, value: x.dictValue, direction: x.dictValue === 'OTHER' || x.remark?.includes('选择方向') || x.remark?.includes('增减') ? 'BOTH' : x.remark?.includes('减少') ? 'DECREASE' : 'INCREASE', remark: x.remark || '' })); this.products = productRes.data || productRes.rows || [] },
    async openCreate() { this.editingId = null; this.form = this.emptyForm(); this.typeIndex = 0; this.editorVisible = true; this.editorLoading = true; try { await this.loadOptions(); if (this.typeOptions.length) this.form.adjustmentType = this.typeOptions[0].value } catch (e) { uni.showToast({ title: '基础数据加载失败', icon: 'none' }) } finally { this.editorLoading = false } },
    async openDetail(item) { this.detailVisible = true; this.detailLoading = true; try { const response = await getStockInitDetail(item.batchId); const detail = response?.data || response; if (!detail || typeof detail !== 'object' || Array.isArray(detail) || !Object.keys(detail).length) throw new Error('调整单详情为空'); this.detail = detail } catch (e) { this.detailVisible = false; uni.showToast({ title: e.msg || e.message || '加载详情失败', icon: 'none' }) } finally { this.detailLoading = false } },
    openEditFromRow(item) { this.openDetail(item) },
    editFromDetail() { const batch = this.detailBatch; if (!batch.batchId || !this.canEdit(batch)) return; this.detailVisible = false; this.openEdit(batch) },
    async reloadDetail() { if (!this.detailBatch?.batchId) { this.detailVisible = false; return } try { const response = await getStockInitDetail(this.detailBatch.batchId); const detail = response?.data || response; if (!detail || typeof detail !== 'object' || Array.isArray(detail) || !Object.keys(detail).length) { this.detailVisible = false; return } this.detail = detail } catch (e) { this.detailVisible = false; uni.showToast({ title: e.msg || e.message || '刷新详情失败', icon: 'none' }) } },
    async openEdit(item) { this.editorVisible = true; this.editorLoading = true; try { await this.loadOptions(); this.editingId = item.batchId; const detailItems = this.detail?.data?.items || this.detail?.items || []; this.form = { ...this.emptyForm(), ...item, deptId: this.currentDeptId, items: (detailItems.length ? detailItems : [item]).map(row => createEmptyItem(row)) }; this.form.items.forEach(row => { row.productIndex = Math.max(0, this.products.findIndex(x => String(x.productId) === String(row.productId))) }); this.typeIndex = Math.max(0, this.typeOptions.findIndex(x => x.value === this.form.adjustmentType)) } catch (e) { this.editorVisible = false; throw e } finally { this.editorLoading = false } },
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
    statusTone(s) { if (s === 'APPROVED' || s === 'POSTED') return 'tone-ok'; if (s === 'REJECTED' || s === 'CANCELLED') return 'tone-danger'; return 'tone-warn' },
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
.page{display:flex;flex-direction:column;height:100vh;width:100vw;max-width:750rpx;margin:0 auto;background:#e7eff7;box-sizing:border-box;overflow:hidden}
.hero{margin:22rpx 30rpx 0;padding:28rpx 30rpx 30rpx;border-left:5rpx solid #1687f5;border-radius:20rpx;background:linear-gradient(110deg,#d9eaff,#f7faff);box-shadow:0 8rpx 22rpx rgba(46,82,120,.08);color:#1e293b}
.eyebrow{display:block;color:#1687f5;font-size:24rpx;font-weight:600}
.hero-title{display:block;margin-top:10rpx;color:#1e293b;font-size:38rpx;font-weight:700}
.work-scope{display:flex;align-items:center;margin:20rpx 30rpx 0;min-height:44rpx;padding:4rpx 12rpx;border-radius:12rpx;box-sizing:border-box}
.work-scope-hover{background:#eaf3ff;border-radius:12rpx}
.work-scope-mark{width:14rpx;height:14rpx;margin-right:16rpx;border-radius:50%;background:#1687f5}
.work-scope-copy{display:flex;align-items:baseline;color:#8192a6;font-size:24rpx}
.work-scope-name{margin-left:4rpx;color:#26384d;font-size:27rpx;font-weight:700}
.permission-note{display:flex;justify-content:space-between;align-items:center;margin:14rpx 30rpx 0;padding:16rpx 20rpx;background:#fff;border:1rpx solid #dbe6f1;border-radius:14rpx;color:#8192a6;font-size:22rpx}
.can-edit{color:#1687f5;font-weight:600}
.read-only{color:#98a9ba}
.scroll{flex:1;width:100%;min-height:0;padding:0 30rpx 160rpx!important;box-sizing:border-box}
.adjustment-scroll{padding-top:4rpx}
.section-card{background:#fff;border-radius:20rpx;padding:20rpx 28rpx!important;margin-top:16rpx!important;border:1rpx solid #D5E0EC;box-shadow:0 5rpx 18rpx rgba(45,72,98,.07);box-sizing:border-box;overflow:hidden}
.section-header{display:flex;align-items:center;gap:12rpx;margin-bottom:18rpx}
.section-dot{width:12rpx;height:12rpx;border-radius:50%;flex-shrink:0}
.section-title{font-size:28rpx;font-weight:700;color:#1A2332;flex:1}
.section-link{font-size:22rpx;color:#94A3B8}
.record-card{display:flex;margin-bottom:16rpx;background:#FFFFFF;border-radius:20rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,0.06);overflow:hidden}
.card-bar{width:4rpx;background:linear-gradient(180deg,#087CF0,#A8C7E5);flex-shrink:0}
.card-body{flex:1;padding:24rpx 28rpx;box-sizing:border-box}
.card-header{display:flex;align-items:flex-start;justify-content:space-between;gap:20rpx}
.record-title{flex:1;font-size:30rpx;line-height:42rpx;font-weight:700;color:#1A2332}
.record-id{padding:4rpx 14rpx;background:#E8EEF5;color:#5A6B7F;font-size:20rpx;border-radius:999rpx;flex-shrink:0}
.summary-grid{display:grid;grid-template-columns:1fr 1fr;gap:12rpx;margin-top:16rpx}
.summary-item{display:flex;flex-direction:column;gap:6rpx;min-width:0}
.summary-label{font-size:22rpx;color:#94A3B8}
.summary-value{font-size:26rpx;color:#1A2332;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.summary-value.tone-ok{color:#047857;font-weight:700}
.summary-value.tone-warn{color:#B45309;font-weight:700}
.summary-value.tone-danger{color:#B91C1C;font-weight:700}
.card-footer{display:flex;justify-content:space-between;align-items:center;margin-top:16rpx;padding-top:14rpx;border-top:1rpx solid #E8EEF5}
.meta-text{font-size:24rpx;color:#94A3B8;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.arrow-icon{font-size:34rpx;color:#CBD5E1;line-height:1;flex-shrink:0}
.bottom-action{position:fixed;left:0;right:0;bottom:0;z-index:5;display:flex;justify-content:center;padding:20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));background:rgba(255,255,255,.96);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-top:1rpx solid #e2e8f0;box-sizing:border-box}
.add-button{width:320rpx;height:84rpx;line-height:84rpx;margin:0;background:linear-gradient(135deg,#087cf0,#5aa9e8);color:#fff;font-size:28rpx;border-radius:999rpx;text-align:center;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25);border:0}
.mask{position:fixed;inset:0;background:rgba(15,23,42,.42);z-index:10}
.sheet{position:absolute;left:0;right:0;bottom:0;max-height:88vh;background:#F3F6FA;border-radius:26rpx 26rpx 0 0}
.adjustment-sheet{background:#F3F6FA}
.sheet-head{display:flex;justify-content:space-between;padding:30rpx;font-size:34rpx;font-weight:700;background:#fff;border-radius:26rpx 26rpx 0 0}
.close{font-size:42rpx;font-weight:400;color:#94a3b8}
.form-scroll{max-height:calc(88vh - 100rpx);padding:0 28rpx calc(38rpx + env(safe-area-inset-bottom));box-sizing:border-box}
.sheet-loading{display:flex;align-items:center;justify-content:center;min-height:360rpx;color:#64748B;font-size:27rpx}
.field{margin-top:22rpx}
.label{display:block;margin-bottom:10rpx;color:#475569;font-size:24rpx}
.picker,.input,.textarea{box-sizing:border-box;width:100%;padding:22rpx;border:1rpx solid #dbe4ef;border-radius:12rpx;background:#fff;color:#1e293b;font-size:28rpx}
.input{display:block;height:78rpx;line-height:34rpx;padding:18rpx 20rpx;overflow:hidden;white-space:nowrap}
.picker{display:flex;justify-content:space-between}
.textarea{height:130rpx}
.grid{display:flex;gap:16rpx}
.grid .field{flex:1;min-width:0}
.field-hint{display:block;margin-top:10rpx;color:#64748b;font-size:22rpx}
.rule-warning{display:block;margin-top:10rpx;padding:14rpx 16rpx;border:2rpx solid #ef4444;border-radius:10rpx;background:#fff1f2;color:#dc2626;font-size:22rpx;font-weight:700}
.direction-options{display:flex;gap:16rpx}
.direction-option{flex:1;padding:18rpx;text-align:center;border:1rpx solid #dbe4ef;border-radius:12rpx;background:#fff;color:#64748b;font-size:26rpx}
.direction-option.active{border-color:#087cf0;background:#eaf4ff;color:#087cf0;font-weight:600}
.items-head{display:flex;justify-content:space-between;align-items:center;margin-top:26rpx;padding:0 2rpx}
.items-head .label{margin-bottom:0}
.link{color:#087cf0;font-size:24rpx}
.link.danger{color:#dc5b5b}
.item-editor{padding:18rpx;background:#f8fafc;border:1rpx solid #eef2f7;border-radius:14rpx;margin-top:14rpx}
.item-editor-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:12rpx;font-size:25rpx;color:#475569;font-weight:600}
.submit.btn-primary{width:100%;height:88rpx;line-height:88rpx;margin-top:28rpx;margin-bottom:30rpx;padding:0;border:0;background:linear-gradient(135deg,#087cf0,#5aa9e8);color:#fff;font-size:28rpx;border-radius:999rpx;text-align:center;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25)}
.detail-summary{padding:6rpx 0 18rpx}
.detail-line{display:flex;justify-content:space-between;align-items:center;padding:22rpx 0;border-bottom:1rpx solid #e5eaf1;color:#64748b;font-size:25rpx}
.detail-line strong{color:#1e293b;font-weight:600}
.detail-section-title{padding:24rpx 0 12rpx;color:#1e293b;font-size:28rpx;font-weight:700}
.detail-item-card{padding:22rpx;margin-bottom:14rpx;border:1rpx solid #dbe4ef;border-radius:14rpx;background:#fff}
.detail-item-head,.detail-item-meta{display:flex;justify-content:space-between;align-items:center}
.detail-item-head{color:#1e293b;font-size:27rpx}
.detail-item-meta{margin-top:14rpx;color:#64748b;font-size:23rpx}
.detail-bottom-actions{display:flex;gap:14rpx;padding:18rpx 28rpx calc(22rpx + env(safe-area-inset-bottom));background:#fff;border-top:1rpx solid #e5eaf1}
.detail-btn{flex:1;height:76rpx;line-height:76rpx;padding:0;border:1rpx solid #dbe4ef;border-radius:14rpx;background:#fff;color:#475569;font-size:26rpx;font-weight:600}
.detail-btn.danger{border-color:#fecaca;color:#dc2626;background:#fff5f5}
.detail-form-scroll{padding-bottom:0!important}
</style>
