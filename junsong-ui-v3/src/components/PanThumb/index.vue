<template>
  <el-image :src="url" fit="cover" style="width: 100%; height: 100%" />
</template>

<script setup lang="ts">
withDefaults(defineProps<{ url?: string }>(), {
  url: '',
})
</script>
