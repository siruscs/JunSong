<template>
  <view class="detail-page">
    <view v-if="loading" class="loading-wrap"><uni-load-more status="loading" /></view>

    <!-- ════════ 查看模式 ════════ -->
    <template v-if="detail && detail.returnId && mode === 'view'">
      <view class="detail-hero">
        <view class="detail-hero-bg"></view>
        <view class="detail-hero-content">
          <view class="detail-hero-eyebrow">购买退货 · {{ statusText(detail.status) }}</view>
          <view class="detail-hero-title">{{ detail.returnNo || `退货单 #${detail.returnId}` }}</view>
          <view class="detail-hero-value">¥{{ money(detail.refundAmount) }}</view>
          <view class="detail-hero-meta">退货日期 {{ dateText(detail.returnDate) }}</view>
        </view>
      </view>

      <view class="detail-section">
        <view class="detail-section-title">概要信息</view>
        <view class="detail-highlight-grid">
          <view class="detail-highlight-item"><view class="detail-highlight-label">顾客姓名</view><view class="detail-highlight-value">{{ detail.customerName || '未登记顾客' }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">手机号</view><view class="detail-highlight-value">{{ detail.customerPhone || '-' }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">退货状态</view><view class="detail-highlight-value" :class="statusClass(detail.status)">{{ statusText(detail.status) }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">原购买单</view><view class="detail-highlight-value">{{ detail.purchaseNo || detail.purchaseId }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">应退金额</view><view class="detail-highlight-value tone-warning">¥{{ money(detail.refundAmount) }}</view></view>
          <view class="detail-highlight-item"><view class="detail-highlight-label">已退金额</view><view class="detail-highlight-value tone-success">¥{{ money(detail.refundedAmount) }}</view></view>
          <view class="detail-highlight-item">
            <view class="detail-highlight-label">退货办理周期</view>
            <view class="detail-highlight-value">{{ periodLabel(detail.returnPeriodId, returnPeriod) }}</view>
            <view class="detail-highlight-sub" v-if="periodDate(returnPeriod)">{{ periodDate(returnPeriod) }}</view>
          </view>
          <view class="detail-highlight-item">
            <view class="detail-highlight-label">原核算周期</view>
            <view class="detail-highlight-value">{{ periodLabel(detail.originalPeriodId, originalPeriod) }}</view>
            <view class="detail-highlight-sub" v-if="periodDate(originalPeriod)">{{ periodDate(originalPeriod) }}</view>
          </view>
        </view>
      </view>

      <view class="detail-section" v-if="(detail.items || []).length">
        <view class="detail-section-title">退货明细</view>
        <view class="detail-item" v-for="(item, idx) in detail.items || []" :key="idx">
          <view class="detail-item-header"><text class="detail-item-title">{{ item.productNameSnapshot || `商品${idx + 1}` }}</text></view>
          <view class="detail-row"><text class="detail-label">退正品数量</text><text class="detail-value-text">{{ quantity(item.returnSaleQuantity) }}</text></view>
          <view class="detail-row"><text class="detail-label">退赠品数量</text><text class="detail-value-text">{{ quantity(item.returnGiftQuantity) }}</text></view>
          <view class="detail-row"><text class="detail-label">退货总数</text><text class="detail-value-text">{{ quantity(item.returnTotalQuantity) }}</text></view>
          <view class="detail-row"><text class="detail-label">退款单价</text><text class="detail-value-text">¥{{ money(item.refundUnitPrice) }}</text></view>
          <view class="detail-row"><text class="detail-label">退款金额</text><text class="detail-value-text amount">¥{{ money(item.refundAmount) }}</text></view>
        </view>
        <view class="detail-summary">
          <view class="detail-summary-row"><text class="detail-summary-label">应退总额</text><text class="detail-summary-value">¥{{ money(detail.refundAmount) }}</text></view>
          <view class="detail-summary-row"><text class="detail-summary-label">已退总额</text><text class="detail-summary-value">¥{{ money(detail.refundedAmount) }}</text></view>
        </view>
      </view>

      <view class="detail-section" v-if="detail.reason">
        <view class="detail-section-title">退货原因</view>
        <view class="detail-remark">{{ detail.reason }}</view>
      </view>

      <view class="detail-section" v-if="detail.remark">
        <view class="detail-section-title">备注</view>
        <view class="detail-remark">{{ detail.remark }}</view>
      </view>

      <view class="detail-footer-placeholder"></view>
      <view class="detail-footer-bar" v-if="canEdit() && can('edit')">
        <button class="detail-action-btn edit-btn" @tap="enterEdit">编辑</button>
        <button v-if="can('complete')" class="detail-action-btn primary-btn" @tap="complete">完成退货</button>
      </view>
    </template>

    <!-- ════════ 编辑模式 ════════ -->
    <template v-if="detail && detail.returnId && mode === 'edit'">
      <view class="edit-header">
        <view class="edit-header-title">编辑退货单</view>
      </view>

      <view class="edit-hero">
        <view class="edit-hero-title">{{ detail.returnNo || `退货单 #${detail.returnId}` }}</view>
        <view class="edit-hero-meta">草稿状态 · 可修改退货数量、顾客信息及备注</view>
      </view>

      <view class="edit-section">
        <view class="edit-section-title">顾客信息</view>
        <view class="edit-form-item">
          <view class="edit-form-label">顾客姓名</view>
          <input class="edit-form-input" v-model="editForm.customerName" placeholder="请输入顾客姓名" />
        </view>
        <view class="edit-form-item">
          <view class="edit-form-label">手机号</view>
          <input class="edit-form-input" v-model="editForm.customerPhone" placeholder="请输入手机号" />
        </view>
      </view>

      <view class="edit-section">
        <view class="edit-section-title">退货明细</view>
        <view class="edit-item-block" v-for="(item, idx) in editForm.items" :key="idx">
          <view class="edit-item-title">{{ item.productNameSnapshot || `商品${idx + 1}` }}</view>
          <view class="edit-item-row">
            <view class="edit-item-field">
              <view class="edit-item-label">退正品</view>
              <input class="edit-item-input" type="digit" v-model="item.returnSaleQuantity" placeholder="0" />
            </view>
            <view class="edit-item-field">
              <view class="edit-item-label">退赠品</view>
              <input class="edit-item-input" type="digit" v-model="item.returnGiftQuantity" placeholder="0" />
            </view>
            <view class="edit-item-field wide">
              <view class="edit-item-label">退款单价</view>
              <input class="edit-item-input" type="digit" v-model="item.refundUnitPrice" placeholder="0.00" />
            </view>
          </view>
          <view class="edit-item-total">
            <text class="edit-item-total-label">小计</text>
            <text class="edit-item-total-value">¥{{ money(calcItemAmount(item)) }}</text>
          </view>
        </view>
        <view class="edit-total-row">
          <text class="edit-total-label">应退总额</text>
          <text class="edit-total-value">¥{{ money(calcTotal()) }}</text>
        </view>
      </view>

      <view class="edit-section">
        <view class="edit-section-title">其他信息</view>
        <view class="edit-form-item">
          <view class="edit-form-label">退货原因</view>
          <textarea class="edit-form-textarea" v-model="editForm.reason" placeholder="请填写退货原因" />
        </view>
        <view class="edit-form-item">
          <view class="edit-form-label">备注</view>
          <textarea class="edit-form-textarea" v-model="editForm.remark" placeholder="请填写备注信息" />
        </view>
      </view>

      <view class="edit-footer-placeholder"></view>
      <view class="edit-footer-bar">
        <button class="edit-footer-btn cancel-btn" @tap="exitEdit">取消</button>
        <button class="edit-footer-btn save-btn" @tap="saveEdit">保存修改</button>
      </view>
    </template>
  </view>
</template>

<script>
import { request } from '@/api/index.js'
import { getMemberPurchaseReturn, updateMemberPurchaseReturn } from '@/api/memberPurchaseReturn.js'
import { hasActionPermission } from '@/utils/permission.js'

export default {
  data() { return { loading: true, returnId: null, detail: {}, returnPeriod: null, originalPeriod: null, mode: 'view', editForm: {} } },
  onLoad(options) {
    this.returnId = options.returnId
    this.loadDetail(options?.edit === '1' || options?.edit === 1)
  },
  methods: {
    can(action) { return hasActionPermission('memberPurchaseReturn', action) },
    canEdit() { return this.detail && this.detail.status !== 'COMPLETED' && this.detail.status !== 'REFUNDED' && this.detail.status !== 'CANCELLED' },
    dateText(v) { return v ? String(v).replace('T',' ').slice(0,19) : '-' },
    money(v) { return Number(v || 0).toFixed(2) },
    quantity(v) { return Number(v || 0).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1') },
    statusText(v) { return ({ DRAFT: '草稿', PENDING: '待审核', APPROVED: '已批准', COMPLETED: '已完成', REFUNDED: '已退款', CANCELLED: '已作废' })[v] || v || '-' },
    statusClass(v) { const s = String(v ?? ''); return s === 'COMPLETED' || s === 'REFUNDED' ? 'status-ok' : s === 'CANCELLED' ? 'status-danger' : s === 'DRAFT' ? 'status-warn' : 'status-info' },
    periodLabel(id, period) {
      if (!id) return '-'
      if (period && period.periodNo) return period.periodNo
      return `周期#${id}`
    },
    periodDate(period) {
      if (!period || !period.startTime) return ''
      const end = period.endTime ? this.dateText(period.endTime) : '当前'
      return `${this.dateText(period.startTime)} 至 ${end}`
    },
    calcItemAmount(item) {
      const sale = Number(item.returnSaleQuantity || 0)
      const gift = Number(item.returnGiftQuantity || 0)
      const total = sale + gift
      const price = Number(item.refundUnitPrice || 0)
      return total * price
    },
    calcTotal() {
      if (!this.editForm.items) return 0
      return this.editForm.items.reduce((s, item) => s + this.calcItemAmount(item), 0)
    },
    async loadDetail(autoEdit) {
      try {
        const res = await getMemberPurchaseReturn(this.returnId)
        this.detail = res.data || res
        this.loadPeriods()
        if (autoEdit && this.canEdit() && this.can('edit')) {
          this.enterEdit()
        }
      } catch (e) { this.detail = {} }
      this.loading = false
    },
    async loadPeriods() {
      try {
        if (this.detail.returnPeriodId) {
          const r = await request({ url: `/finance/accountingPeriod/${this.detail.returnPeriodId}`, method: 'GET' })
          this.returnPeriod = r.data || r
        }
        if (this.detail.originalPeriodId) {
          const r = await request({ url: `/finance/accountingPeriod/${this.detail.originalPeriodId}`, method: 'GET' })
          this.originalPeriod = r.data || r
        }
      } catch (e) {}
    },
    enterEdit() {
      const items = (this.detail.items || []).map(item => ({
        returnItemId: item.returnItemId,
        itemId: item.itemId,
        productNameSnapshot: item.productNameSnapshot,
        returnSaleQuantity: String(item.returnSaleQuantity || ''),
        returnGiftQuantity: String(item.returnGiftQuantity || ''),
        refundUnitPrice: String(item.refundUnitPrice || ''),
        refundAmount: item.refundAmount
      }))
      this.editForm = {
        customerName: this.detail.customerName || '',
        customerPhone: this.detail.customerPhone || '',
        reason: this.detail.reason || '',
        remark: this.detail.remark || '',
        items
      }
      this.mode = 'edit'
    },
    exitEdit() { this.mode = 'view' },
    async saveEdit() {
      const items = this.editForm.items.map(item => ({
        returnItemId: item.returnItemId,
        itemId: item.itemId,
        returnSaleQuantity: Number(item.returnSaleQuantity || 0),
        returnGiftQuantity: Number(item.returnGiftQuantity || 0),
        refundUnitPrice: Number(item.refundUnitPrice || 0),
        refundAmount: this.calcItemAmount(item)
      }))
      const totalAmount = items.reduce((s, i) => s + i.refundAmount, 0)
      try {
        await updateMemberPurchaseReturn(this.detail.returnId, {
          customerName: this.editForm.customerName,
          customerPhone: this.editForm.customerPhone,
          reason: this.editForm.reason,
          remark: this.editForm.remark,
          refundAmount: totalAmount,
          items,
          version: this.detail.version
        })
        uni.showToast({ title: '保存成功', icon: 'success' })
        this.mode = 'view'
        await this.loadDetail()
      } catch (e) {}
    },
    async complete() {
      const ok = await new Promise(resolve => uni.showModal({
        title: '操作确认',
        content: `确认完成退货单 ${this.detail.returnNo || this.detail.returnId} 吗？`,
        success: r => resolve(r.confirm)
      }))
      if (!ok) return
      try {
        await request({ url: `/member/purchase-return/${this.detail.returnId}/complete`, method: 'PUT', data: { refundAmount: this.detail.refundAmount } })
        uni.showToast({ title: '退货单已完成', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 300)
      } catch (e) {}
    }
  }
}
</script>

<style scoped>
.loading-wrap{display:flex;align-items:center;justify-content:center;padding:200rpx 0}
.detail-page{height:100vh;background:#E8EEF5;padding-bottom:calc(40rpx + env(safe-area-inset-bottom));box-sizing:border-box;overflow-y:auto;-webkit-overflow-scrolling:touch}

/* ════════ 查看模式 ════════ */
.detail-hero{position:relative;margin:24rpx 28rpx;border-radius:20rpx;overflow:hidden}
.detail-hero-bg{position:absolute;inset:0;background:linear-gradient(135deg,#087CF0,#5AA9E8,#A8C7E5);border-radius:20rpx}
.detail-hero-content{position:relative;z-index:1;padding:40rpx 36rpx}
.detail-hero-eyebrow{font-size:22rpx;color:rgba(255,255,255,.7);margin-bottom:12rpx;letter-spacing:2rpx}
.detail-hero-title{font-size:36rpx;font-weight:600;color:#fff;margin-bottom:16rpx;line-height:1.4;overflow-wrap:anywhere;word-break:break-word}
.detail-hero-value{font-size:52rpx;font-weight:700;color:#fff;margin-bottom:12rpx}
.detail-hero-meta{font-size:24rpx;color:rgba(255,255,255,.7)}

.detail-section{margin:20rpx 28rpx 0;background:#fff;border-radius:20rpx;padding:28rpx 32rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,.06);box-sizing:border-box}
.detail-section-title{font-size:28rpx;font-weight:600;color:#1A2332;margin-bottom:20rpx;padding-left:16rpx;border-left:4rpx solid #087CF0}

.detail-highlight-grid{display:flex;flex-wrap:wrap;gap:12rpx}
.detail-highlight-item{flex:1;min-width:45%;background:#F5F8FA;border-radius:12rpx;padding:18rpx 20rpx;box-sizing:border-box}
.detail-highlight-label{font-size:22rpx;color:#94A3B8;margin-bottom:6rpx}
.detail-highlight-value{font-size:28rpx;font-weight:600;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}
.detail-highlight-sub{font-size:20rpx;color:#94A3B8;margin-top:6rpx;line-height:1.4}
.detail-highlight-value.status-ok{display:inline-block;background:#E0F2FE;color:#075985;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-warn{display:inline-block;background:#FEF3C7;color:#92400E;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-danger{display:inline-block;background:#FEE2E2;color:#991B1B;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.status-info{display:inline-block;background:#E0F2FE;color:#075985;font-size:24rpx;font-weight:500;padding:4rpx 16rpx;border-radius:20rpx}
.detail-highlight-value.tone-success{color:#0F766E}
.detail-highlight-value.tone-warning{color:#C65A4A}

.detail-item{background:#F5F8FA;border-radius:16rpx;padding:24rpx;margin-bottom:20rpx;box-sizing:border-box}
.detail-item:last-child{margin-bottom:0}
.detail-item-header{display:flex;align-items:center;justify-content:space-between;padding-bottom:16rpx;border-bottom:1rpx solid #E2E8F0;margin-bottom:16rpx}
.detail-item-title{font-size:28rpx;font-weight:700;color:#1A2332;overflow-wrap:anywhere;word-break:break-word}
.detail-row{display:flex;align-items:center;min-height:72rpx;padding:16rpx 0;box-sizing:border-box;line-height:34rpx}
.detail-label{width:140rpx;font-size:26rpx;color:#64748B;flex-shrink:0}
.detail-value-text{font-size:26rpx;color:#1A2332;flex:1;overflow-wrap:anywhere;word-break:break-word}
.detail-value-text.amount{font-weight:700;color:#C65A4A}

.detail-summary{margin-top:24rpx;padding-top:24rpx;border-top:2rpx solid #E2E8F0}
.detail-summary-row{display:flex;align-items:center;padding:12rpx 0}
.detail-summary-label{flex:1;font-size:28rpx;color:#1A2332;font-weight:500}
.detail-summary-value{font-size:32rpx;color:#C65A4A;font-weight:700}

.detail-remark{font-size:26rpx;color:#475569;line-height:1.6;white-space:pre-wrap;word-break:break-word;background:#F5F8FA;border-radius:12rpx;padding:20rpx}

.detail-footer-placeholder{height:140rpx}
.detail-footer-bar{position:fixed;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;gap:16rpx;padding:16rpx 24rpx;padding-bottom:calc(16rpx + env(safe-area-inset-bottom));background:#fff;box-shadow:0 -2rpx 16rpx rgba(8,124,240,.06);z-index:100}
.detail-action-btn{flex:1;height:76rpx;line-height:76rpx;font-size:28rpx;font-weight:500;border-radius:16rpx;text-align:center;border:none;margin:0;padding:0}
.detail-action-btn::after{border:none}
.detail-action-btn.primary-btn{background:#087CF0;color:#fff}
.detail-action-btn.edit-btn{background:#E8EEF5;color:#087CF0}

/* ════════ 编辑模式 ════════ */
.edit-header{position:sticky;top:0;z-index:20;display:flex;align-items:center;height:calc(88rpx + env(safe-area-inset-top));padding-top:env(safe-area-inset-top);padding-right:24rpx;background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#fff}
.edit-header-title{flex:1;text-align:center;font-size:32rpx;font-weight:600}

.edit-hero{margin:24rpx 28rpx 0;padding:32rpx;background:linear-gradient(135deg,#C7DCF2 0%,#EAF3FC 100%);border-radius:20rpx;border:1rpx solid #B7D1EB}
.edit-hero-title{font-size:34rpx;font-weight:700;color:#1F2D3D;margin-bottom:8rpx}
.edit-hero-meta{font-size:24rpx;color:#6E8197}

.edit-section{margin:20rpx 28rpx 0;background:#fff;border-radius:20rpx;padding:28rpx 32rpx;box-shadow:0 2rpx 16rpx rgba(8,124,240,.06);box-sizing:border-box}
.edit-section-title{font-size:28rpx;font-weight:600;color:#1A2332;margin-bottom:20rpx;padding-left:16rpx;border-left:4rpx solid #087CF0}

.edit-form-item{padding:16rpx 0;border-top:1rpx solid #E8EEF5}
.edit-form-item:first-of-type{border-top:0;padding-top:0}
.edit-form-label{font-size:26rpx;color:#475569;margin-bottom:10rpx;font-weight:500}
.edit-form-input{width:100%;height:84rpx;border:2rpx solid #E2E8F0;border-radius:14rpx;padding:0 24rpx;font-size:28rpx;color:#1A2332;background:#F5F8FA;box-sizing:border-box}
.edit-form-textarea{width:100%;min-height:180rpx;border:2rpx solid #E2E8F0;border-radius:14rpx;padding:20rpx 24rpx;font-size:28rpx;color:#1A2332;background:#F5F8FA;box-sizing:border-box}

/* 编辑明细 */
.edit-item-block{background:#F5F8FA;border-radius:16rpx;padding:24rpx;margin-bottom:20rpx;box-sizing:border-box}
.edit-item-block:last-child{margin-bottom:0}
.edit-item-title{font-size:28rpx;font-weight:600;color:#1A2332;margin-bottom:16rpx}
.edit-item-row{display:flex;gap:12rpx;margin-bottom:16rpx}
.edit-item-field{flex:1;min-width:0}
.edit-item-field.wide{flex:1.4}
.edit-item-label{font-size:22rpx;color:#64748B;margin-bottom:8rpx}
.edit-item-input{width:100%;height:72rpx;border:2rpx solid #E2E8F0;border-radius:12rpx;padding:0 20rpx;font-size:26rpx;color:#1A2332;background:#fff;box-sizing:border-box;text-align:right}
.edit-item-total{display:flex;align-items:center;justify-content:space-between;padding-top:12rpx;border-top:1rpx dashed #CBD5E1}
.edit-item-total-label{font-size:24rpx;color:#64748B}
.edit-item-total-value{font-size:28rpx;color:#C65A4A;font-weight:700}

.edit-total-row{display:flex;align-items:center;justify-content:space-between;margin-top:16rpx;padding-top:16rpx;border-top:2rpx solid #E2E8F0}
.edit-total-label{font-size:28rpx;color:#1A2332;font-weight:500}
.edit-total-value{font-size:34rpx;color:#C65A4A;font-weight:700}

.edit-footer-placeholder{height:160rpx}
.edit-footer-bar{position:fixed;left:0;right:0;bottom:0;display:flex;gap:16rpx;padding:18rpx 24rpx;padding-bottom:calc(18rpx + env(safe-area-inset-bottom));background:#fff;box-shadow:0 -2rpx 16rpx rgba(8,124,240,.06);z-index:100}
.edit-footer-btn{flex:1;height:88rpx;line-height:88rpx;font-size:28rpx;border-radius:999rpx;text-align:center;border:none;margin:0;padding:0}
.edit-footer-btn::after{border:none}
.edit-footer-btn.cancel-btn{background:#E2E8F0;color:#475569}
.edit-footer-btn.save-btn{background:linear-gradient(135deg,#087CF0,#5AA9E8);color:#fff;box-shadow:0 6rpx 20rpx rgba(8,124,240,.25)}
</style>
