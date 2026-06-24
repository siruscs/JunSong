<template>
  <iframe :src="url" frameborder="0" class="iframe-container" />
</template>

<script setup lang="ts">
withDefaults(defineProps<{ url?: string }>(), {
  url: '',
})
</script>

<style scoped>
.iframe-container {
  width: 100%;
  height: calc(100vh - 84px);
}
</style>
