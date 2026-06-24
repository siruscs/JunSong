<template>
  <div :style="{ height: height, width: width }">
    <iframe :src="url" frameborder="no" style="width: 100%; height: 100%" scrolling="auto" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

defineProps<{ url: string }>()

const height = ref(document.documentElement.clientHeight - 94.5 + 'px')
const width = ref('100%')

onMounted(() => {
  window.addEventListener('resize', () => {
    height.value = document.documentElement.clientHeight - 94.5 + 'px'
  })
})
</script>
