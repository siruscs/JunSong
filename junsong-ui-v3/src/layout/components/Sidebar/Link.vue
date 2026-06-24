<template>
  <a v-if="isExternalLink(to)" :href="to" target="_blank" rel="noopener">
    <slot />
  </a>
  <router-link v-else :to="to">
    <slot />
  </router-link>
</template>

<script setup lang="ts">
defineProps<{ to: string }>()

function isExternalLink(path: string): boolean {
  return /^(https?:|mailto:|tel:)/.test(path)
}
</script>
