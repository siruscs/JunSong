<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive>
          <component :is="Component" :key="route.path" />
        </keep-alive>
      </transition>
    </router-view>
  </section>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.app-main {
  height: calc(100vh - 104px);
  min-height: 0;
  width: 100%;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px 10px 16px;
  box-sizing: border-box;
  background:
    linear-gradient(135deg, rgba(var(--theme-primary-rgb), 0.06), transparent 30%),
    var(--theme-app-bg, #f5f8fb);
}

:global(.fixed-header + .app-main) {
  height: calc(100vh - 122px);
}

@media (max-width: 768px) {
  .app-main {
    padding: 8px;
  }
}

</style>
