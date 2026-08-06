<template>
  <view class="form-item">
    <view class="label-row">
      <text class="label">{{ field.label }}</text>
      <text v-if="required" class="required-tag">*</text>
    </view>
    <picker v-if="field.type === 'date'" mode="date" :value="value || ''" @change="setValue">
      <view class="control picker" :class="{ 'has-value': value }">
        <text class="picker-text">{{ value || '请选择日期' }}</text>
        <text class="picker-arrow">›</text>
      </view>
    </picker>
    <picker v-else-if="field.type === 'select'" :range="options" :value="selectedIndex" @change="$emit('select-value', $event.detail.value)">
      <view class="control picker" :class="{ 'has-value': value }">
        <text class="picker-text">{{ displayText }}</text>
        <text class="picker-arrow">›</text>
      </view>
    </picker>
    <picker v-else-if="field.type === 'region'" mode="multiSelector" :range="regionRange" :value="regionIndex" @columnchange="regionColumnChange" @change="regionChange">
      <view class="control picker" :class="{ 'has-value': regionText }">
        <text class="picker-text">{{ regionText || '请选择省市区街道' }}</text>
        <text class="picker-arrow">›</text>
      </view>
    </picker>
    <textarea v-else-if="field.type === 'textarea'" class="control textarea" v-model="localValue" maxlength="-1" :placeholder="'请输入' + field.label" @input="inputValue" />
    <view v-else-if="field.type === 'image'" class="control image-upload">
      <view class="image-preview" v-if="value" @tap="$emit('choose-image')">
        <image :src="imageUrl" mode="aspectFit" class="upload-image" />
        <text class="image-replace-text">点击更换</text>
      </view>
      <view class="image-picker" v-else @tap="$emit('choose-image')">
        <text class="image-picker-icon">+</text>
        <text class="image-picker-text">上传图片</text>
      </view>
    </view>
    <input v-else class="control input" :value="value || ''" :type="inputType" :disabled="readonly" :placeholder="placeholder" :maxlength="precision ? precision + 20 : -1" @input="inputValue" />
  </view>
</template>

<script>
export default {
  props: {
    field: { type: Object, required: true },
    required: { type: Boolean, default: false },
    value: { type: [String, Number], default: '' },
    options: { type: Array, default: () => [] },
    selectedIndex: { type: Number, default: 0 },
    displayText: { type: String, default: '' },
    regionRange: { type: Array, default: () => [[], [], [], []] },
    regionIndex: { type: Array, default: () => [0, 0, 0, 0] },
    regionText: { type: String, default: '' },
    inputType: { type: String, default: 'text' },
    precision: { type: Number, default: null },
    readonly: { type: Boolean, default: false },
    placeholder: { type: String, default: '' },
    imageUrl: { type: String, default: '' }
  },
  computed: {
    localValue: {
      get() { return this.value || '' },
      set(value) { this.$emit('input-value', value) }
    }
  },
  methods: {
    setValue(event) { this.$emit('set-value', event.detail.value) },
    regionColumnChange(event) { this.$emit('region-column-change', event) },
    regionChange(event) { this.$emit('region-change', event) },
    inputValue(event) { this.$emit('input-value', event.detail.value) }
  }
}
</script>

<style scoped>
.form-item{margin-bottom:28rpx}.label-row{display:flex;align-items:center;margin-bottom:12rpx}.label{color:#334155;font-size:26rpx}.required-tag{margin-left:6rpx;color:#ef4444}.control{box-sizing:border-box;width:100%;border:1rpx solid #dbe4ef;border-radius:12rpx;background:#fff;color:#1e293b;font-size:28rpx}.picker{display:flex;align-items:center;justify-content:space-between;padding:22rpx}.picker.has-value{color:#1e293b}.picker-text{flex:1}.picker-arrow{margin-left:12rpx;color:#94a3b8;font-size:34rpx}.input{display:block;height:84rpx;line-height:84rpx;padding:0 24rpx;box-sizing:border-box;overflow:hidden;white-space:nowrap}.textarea{min-height:130rpx;padding:20rpx}.image-upload{padding:12rpx}.image-preview,.image-picker{display:flex;align-items:center;justify-content:center;min-height:180rpx}.image-preview{position:relative;flex-direction:column}.upload-image{width:100%;height:180rpx}.image-replace-text,.image-picker-text{margin-top:8rpx;color:#64748b;font-size:23rpx}.image-picker{flex-direction:column;border:2rpx dashed #cbd5e1}.image-picker-icon{color:#94a3b8;font-size:56rpx;font-weight:300}.image-picker-text{margin-top:0}.image-replace-text{position:absolute;right:12rpx;bottom:8rpx;padding:4rpx 10rpx;border-radius:8rpx;background:rgba(15,23,42,.62);color:#fff}
</style>
