<template>
  <el-select :model-value="size" @change="handleChange" style="width: 100px">
    <el-option label="大尺寸" value="large" />
    <el-option label="默认" value="default" />
    <el-option label="小尺寸" value="small" />
  </el-select>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{ size?: string }>(), {
  size: 'default',
})

const emit = defineEmits<{
  (e: 'change', val: string): void
}>()

function handleChange(val: string) {
  emit('change', val)
}
</script>
