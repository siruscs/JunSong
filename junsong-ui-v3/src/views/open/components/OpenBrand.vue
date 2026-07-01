<template>
  <RouterLink class="open-brand" to="/open-platform" aria-label="Genesis Cloud 开放平台首页">
    <span class="open-brand__mark" v-html="brandMarkSvg" />
    <span class="open-brand__copy">
      <strong>Genesis Cloud</strong>
      <small>开放平台</small>
    </span>
  </RouterLink>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import brandMarkSvg from '@/assets/logo/junsong-mark-theme.svg?raw'
</script>

<style scoped lang="scss">
.open-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: var(--ink, #172033);
  text-decoration: none;
}

.open-brand__mark {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  color: var(--theme-primary, #0071e3);
  border-radius: 12px;
  box-shadow: 0 14px 30px rgba(var(--theme-primary-rgb, 44, 105, 117), 0.18);
}

.open-brand__mark :deep(svg) {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 12px;
}

.open-brand__copy {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.open-brand__copy strong {
  color: var(--ink, #172033);
  font-family: 'Cormorant Garamond', 'Source Han Serif SC', serif;
  font-size: 20px;
  font-weight: 650;
  letter-spacing: 0.02em;
  line-height: 1;
  white-space: nowrap;
}

.open-brand__copy small {
  color: var(--ink-soft, #667085);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1.25;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .open-brand__copy small {
    display: none;
  }
}
</style>
