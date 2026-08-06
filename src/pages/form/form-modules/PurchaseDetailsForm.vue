<template>
  <view class="section-card">
    <view class="section-header"><view class="section-dot required"></view><text class="section-title">商品明细</text><text class="section-count" v-if="details.length">{{ details.length }}项</text><view class="add-detail-btn" @tap="$emit('add')"><text class="add-detail-icon">+</text><text class="add-detail-text">添加商品</text></view></view>
    <view class="detail-list" v-if="details.length">
      <view class="detail-item" v-for="(detail, index) in details" :key="index">
        <view class="detail-item-header"><text class="detail-item-title">商品{{ index + 1 }}</text><text class="detail-delete" @tap="$emit('remove', index)">删除</text></view>
        <view class="detail-row"><text class="detail-label">商品</text><picker class="detail-picker" :range="productLabels" :value="productIndex(detail.productId)" @change="$emit('product-change', { index, value: $event.detail.value })"><view class="detail-control" :class="{ 'has-value': detail.productId }"><text class="detail-picker-text">{{ detail.productName || '请选择商品' }}</text><text class="picker-arrow">›</text></view></picker></view>
        <view class="detail-row"><text class="detail-label">单位</text><text class="detail-value-text">{{ detail.unit || '-' }}</text></view>
        <view class="detail-row"><text class="detail-label">数量</text><input class="detail-input" type="digit" v-model="detail.quantity" @input="$emit('quantity-input', index)" @blur="$emit('quantity-blur', index)" placeholder="0.000" /></view>
        <view class="detail-row"><text class="detail-label">单价</text><input class="detail-input" v-model="detail.price" type="digit" :disabled="detail.isGift === '1'" @input="$emit('amount', index)" placeholder="0.00" /></view>
        <view class="detail-row"><text class="detail-label">金额</text><text class="detail-value-text amount">{{ detail.isGift === '1' ? '赠品' : '¥' + (detail.amount || 0) }}</text></view>
        <view class="detail-row"><text class="detail-label">赠品</text><switch :checked="detail.isGift === '1'" @change="$emit('gift', { index, value: $event.detail.value })" color="#087CF0" /></view>
      </view>
    </view>
    <view class="detail-empty" v-else><text class="detail-empty-text">请添加商品明细</text></view>
    <view class="detail-summary" v-if="details.length"><view class="detail-summary-row"><text class="detail-summary-label">总数量</text><text class="detail-summary-value">{{ totalQuantity || 0 }}</text></view><view class="detail-summary-row"><text class="detail-summary-label">总金额</text><text class="detail-summary-value">¥{{ totalAmount || 0 }}</text></view></view>
  </view>
</template>

<script>
export default {
  name: 'PurchaseDetailsForm',
  emits: ['add', 'remove', 'product-change', 'quantity-input', 'quantity-blur', 'amount', 'gift'],
  props: { details: { type: Array, default: () => [] }, products: { type: Array, default: () => [] }, totalQuantity: { type: [Number, String], default: 0 }, totalAmount: { type: [Number, String], default: 0 } },
  computed: { productLabels() { return this.products.map(item => item.productName || '') } },
  methods: { productIndex(productId) { return Math.max(0, this.products.findIndex(item => String(item.productId) === String(productId))) } }
}
</script>

<style scoped>
.section-card{margin-top:20rpx;padding:24rpx;background:#fff;border-radius:18rpx}.section-header{display:flex;align-items:center;gap:12rpx}.section-dot{width:12rpx;height:12rpx;border-radius:50%;background:#94a3b8}.section-dot.required{background:#087CF0}.section-title{font-size:29rpx;font-weight:700;color:#1e293b}.section-count{margin-left:auto;color:#94a3b8;font-size:23rpx}.add-detail-btn{display:flex;align-items:center;margin-left:12rpx;padding:10rpx 14rpx;border-radius:10rpx;background:#eaf4ff;color:#087CF0}.add-detail-icon{font-size:30rpx}.add-detail-text{margin-left:4rpx;font-size:23rpx}.detail-item{margin-top:18rpx;padding:18rpx;border:1rpx solid #e2e8f0;border-radius:14rpx}.detail-item-header,.detail-row,.detail-summary-row{display:flex;justify-content:space-between;align-items:center}.detail-item-header{margin-bottom:12rpx}.detail-item-title{font-size:26rpx;font-weight:600}.detail-delete{color:#dc2626;font-size:23rpx}.detail-row{min-height:76rpx;padding:16rpx 0;box-sizing:border-box;color:#64748b;font-size:24rpx;line-height:34rpx}.detail-label{flex:none;min-width:86rpx}.detail-picker{flex:1;width:0;min-width:0;margin-left:24rpx}.detail-control{display:flex;align-items:center;justify-content:space-between;min-height:64rpx;padding:14rpx;border:1rpx solid #dbe4ef;border-radius:10rpx;color:#94a3b8;box-sizing:border-box}.detail-control.has-value{color:#1e293b}.detail-input{flex:1;width:0;min-width:0;height:64rpx;padding:12rpx;border:1rpx solid #dbe4ef;border-radius:10rpx;text-align:right;box-sizing:border-box}.detail-value-text{color:#1e293b;line-height:34rpx}.amount{color:#087CF0}.detail-empty{text-align:center;padding:50rpx;color:#94a3b8}.detail-summary{margin-top:18rpx;padding-top:16rpx;border-top:1rpx solid #e2e8f0}.detail-summary-row{padding:6rpx 0;color:#64748b}.detail-summary-value{color:#1e293b;font-weight:600}
</style>
