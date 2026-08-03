<template>
  <view v-if="status !== 'normal'" class="state-view">
    <view v-if="status === 'loading'" class="state-icon loading">…</view>
    <view v-else-if="status === 'error'" class="state-icon error">!</view>
    <view v-else class="state-icon empty">○</view>
    <text class="state-title">{{ titleText }}</text>
    <text v-if="status === 'error'" class="state-retry" @tap="$emit('retry')">重新加载</text>
  </view>
</template>

<script>
export default {
  name: 'StateView',
  emits: ['retry'],
  props: { status: { type: String, default: 'normal' }, message: { type: String, default: '' } },
  computed: { titleText() { return this.message || ({ loading: '加载中', empty: '暂无数据', error: '加载失败' }[this.status] || '') } }
}
</script>

<style scoped>
.state-view{display:flex;flex-direction:column;align-items:center;justify-content:center;padding:100rpx 30rpx;color:#94a3b8}.state-icon{display:flex;align-items:center;justify-content:center;width:76rpx;height:76rpx;border-radius:50%;font-size:42rpx;font-weight:700}.state-icon.loading{background:#eaf4ff;color:#087cf0}.state-icon.error{background:#fff1f2;color:#dc2626}.state-icon.empty{background:#f1f5f9;color:#94a3b8}.state-title{margin-top:20rpx;font-size:26rpx}.state-retry{margin-top:18rpx;color:#087cf0;font-size:25rpx}
</style>
