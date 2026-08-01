<template>
  <view class="page">
    <view class="card"><text class="title">库存调整</text><text class="hint">仅展示已过账库存调整结果</text></view>
    <view v-for="item in rows" :key="item.productId" class="card row">
      <view class="line"><text>{{ item.productName || '-' }}</text><text>{{ item.closingQuantity || 0 }}</text></view>
      <text class="hint">期末金额：{{ item.closingAmount || 0 }} · 调整金额：{{ item.adjustmentAmount || 0 }}</text>
    </view>
    <view v-if="!loading && !rows.length" class="empty">暂无库存调整记录</view>
  </view>
</template>

<script>
import { getStockValueReport } from '@/api/stock.js'
import { requireModulePermission } from '@/utils/permission.js'

export default {
  data() { return { rows: [], loading: false } },
  onLoad() { if (requireModulePermission('stockAdjustment')) this.load() },
  methods: {
    load() {
      this.loading = true
      getStockValueReport({}).then(res => { this.rows = res.data?.items || res.items || [] }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.page { padding: 24rpx; background: #f5f7fb; min-height: 100vh; }
.card { padding: 28rpx; margin-bottom: 20rpx; background: #fff; border-radius: 16rpx; }
.title { display: block; font-size: 32rpx; font-weight: 600; }
.hint { display: block; margin-top: 12rpx; color: #8a94a6; font-size: 24rpx; }
.line { display: flex; justify-content: space-between; font-size: 28rpx; }
.empty { padding: 80rpx 0; text-align: center; color: #8a94a6; }
</style>
