<template>
  <view>
    <view class="ocr-btn" @tap="$emit('choose-ocr-image')">
      <text class="ocr-icon">📷</text>
      <text class="ocr-label">拍照识别</text>
    </view>
    <view class="ocr-preview" v-if="imageUrl">
      <image class="ocr-image" :src="imageUrl" mode="aspectFit" />
      <view class="ocr-overlay" v-if="loading">
        <text class="ocr-loading-text">识别中...</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  props: {
    imageUrl: { type: String, default: '' },
    loading: { type: Boolean, default: false }
  }
}
</script>

<style scoped>
.ocr-btn{display:flex;align-items:center;gap:6rpx;margin-left:auto;padding:6rpx 16rpx;background:#087cf0;border-radius:999rpx}.ocr-icon{font-size:22rpx}.ocr-label{font-size:22rpx;color:#fff}.ocr-preview{position:relative;margin-bottom:20rpx;border-radius:14rpx;overflow:hidden;background:#f5f8fa}.ocr-image{width:100%;height:300rpx}.ocr-overlay{position:absolute;top:0;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,.4)}.ocr-loading-text{color:#fff;font-size:28rpx;font-weight:500}
</style>
