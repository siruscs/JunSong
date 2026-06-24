<template>
  <div class="sidebar-logo-container" :class="{ collapse: collapse }">
    <router-link class="sidebar-logo-link" to="/">
      <span class="sidebar-logo-mark" v-html="markSvg" />
      <div class="sidebar-title-wrap">
        <div class="sidebar-title-row">
          <span class="sidebar-title-en">Genesis</span>
          <span class="sidebar-version" v-if="!collapse">v26.6.100</span>
        </div>
        <span class="sidebar-title-cn" v-if="!collapse">峻松管理系统</span>
      </div>
    </router-link>
  </div>
</template>

<script setup lang="ts">
import markSvg from '@/assets/logo/junsong-mark-theme.svg?raw'
defineProps<{ collapse: boolean }>()
</script>

<style scoped lang="scss">
.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 72px;
  display: flex;
  align-items: center;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent),
    var(--theme-sidebar-bg);
  overflow: hidden;
  &.collapse .sidebar-title-cn,
  &.collapse .sidebar-version {
    display: none;
  }

  &.collapse .sidebar-logo-link {
    grid-template-columns: 40px;
    justify-content: center;
    padding: 0;
  }
}
.sidebar-logo-link {
  height: 100%;
  width: 100%;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  align-items: center;
  justify-content: start;
  gap: 12px;
  padding: 0 16px 0 20px;
  text-decoration: none;
  white-space: nowrap;
  min-width: 0;
}
.sidebar-logo-mark {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 11px;
  overflow: hidden;
  color: var(--theme-primary, #0071e3);
  box-shadow: 0 10px 22px rgba(var(--theme-primary-rgb, 0, 113, 227), 0.32);

  :deep(svg) {
    width: 100%;
    height: 100%;
    display: block;
    border-radius: 11px;
  }
}
.sidebar-title-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
}

.sidebar-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  width: 100%;
}

.sidebar-title-en {
  font-family: 'Cormorant Garamond', 'Source Han Serif SC', serif;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 1.2px;
  line-height: 1;
  color: var(--theme-sidebar-logo-text);
  white-space: nowrap;
}

.sidebar-title-cn {
  font-family: 'PingFang SC', sans-serif;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2.5px;
  line-height: 1;
  color: var(--theme-sidebar-logo-text);
  opacity: 0.78;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-version {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  font-size: 9px;
  font-weight: 700;
  color: var(--theme-primary, #0071e3);
  background: rgba(var(--theme-primary-rgb, 0, 113, 227), 0.12);
  border: 1px solid rgba(var(--theme-primary-rgb, 0, 113, 227), 0.22);
  padding: 2px 6px;
  border-radius: 6px;
  letter-spacing: 0.4px;
  line-height: 1;
  white-space: nowrap;
}
</style>

