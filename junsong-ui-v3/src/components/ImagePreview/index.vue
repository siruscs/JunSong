<template>
  <div class="image-preview">
    <el-image
      :src="src"
      :preview-src-list="previewSrcList"
      :initial-index="index"
      fit="cover"
      @click.stop="onClick"
    />
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  src?: string
  srcList?: string[]
  previewSrcList?: string[]
  index?: number
}>(), {
  src: '',
  srcList: () => [],
  previewSrcList: () => [],
  index: 0,
})

function onClick() {
  if (props.srcList && props.srcList.length > 0) {
    return
  }
}
</script>

<style scoped>
.image-preview {
  display: inline-block;
}
</style>
