<template>
  <div :class="classObj" class="app-wrapper">
    <div v-if="device === 'mobile' && sidebar.opened" class="drawer-bg" @click="handleClickOutside" />
    <Sidebar class="sidebar-container" />
    <div :class="{ hasTagsView: needTagsView }" class="main-container">
      <div :class="{ 'fixed-header': fixedHeader }">
        <Navbar />
        <TagsView v-if="needTagsView" />
      </div>
      <AppMain />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useSettingsStore } from '@/stores/settings'
import Sidebar from './components/Sidebar/index.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView/index.vue'
import AppMain from './components/AppMain.vue'

const route = useRoute()
const appStore = useAppStore()
const settingsStore = useSettingsStore()

const device = computed(() => appStore.device)
const sidebar = computed(() => appStore.sidebar)
const fixedHeader = computed(() => settingsStore.fixedHeader)
const needTagsView = computed(() => settingsStore.tagsView)

const classObj = computed(() => ({
  hideSidebar: !sidebar.value.opened,
  openSidebar: sidebar.value.opened,
  withoutAnimation: sidebar.value.withoutAnimation,
  mobile: device.value === 'mobile',
}))

function handleClickOutside() {
  if (device.value === 'mobile') {
    appStore.closeSideBar(false)
  }
}

const WIDTH = 992

function isMobile() {
  const rect = document.body.getBoundingClientRect()
  return rect.width - 1 < WIDTH
}

function resizeHandler() {
  if (!document.hidden) {
    const mobile = isMobile()
    appStore.toggleDevice(mobile ? 'mobile' : 'desktop')
    if (mobile) {
      appStore.closeSideBar(true)
    }
  }
}

watch(route, () => {
  if (device.value === 'mobile' && sidebar.value.opened) {
    appStore.closeSideBar(false)
  }
})

onMounted(() => {
  const mobile = isMobile()
  if (mobile) {
    appStore.toggleDevice('mobile')
    appStore.closeSideBar(true)
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped lang="scss">
.app-wrapper {
  position: relative;
  height: 100%;
  width: 100%;
  overflow: visible;
  background:
    radial-gradient(circle at 14% 10%, rgba(var(--theme-primary-rgb), 0.09), transparent 28%),
    linear-gradient(180deg, var(--theme-app-bg, #f5f8fb), var(--theme-app-bg-soft, #eef3f8));
  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}
.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}
.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - 240px);
  transition: width 0.28s;
  box-shadow: var(--theme-app-header-shadow, 0 8px 24px rgba(31, 45, 82, 0.08));
}
.hideSidebar .fixed-header {
  width: calc(100% - 72px);
}
.mobile .fixed-header {
  width: 100%;
}
.fixed-header + .app-main {
  padding-top: 122px;
}
</style>
