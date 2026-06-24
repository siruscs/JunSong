<template>
  <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'">
    <el-icon :size="20" class="screen-full" @click="toggle">
      <FullScreen v-if="!isFullscreen" />
      <Aim v-else />
    </el-icon>
  </el-tooltip>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { FullScreen, Aim } from '@element-plus/icons-vue'

const isFullscreen = ref(false)

function toggle() {
  if (!isFullscreen.value) {
    const el: any = document.documentElement
    if (el.requestFullscreen) {
      el.requestFullscreen()
    } else if (el.webkitRequestFullscreen) {
      el.webkitRequestFullscreen()
    } else if (el.mozRequestFullScreen) {
      el.mozRequestFullScreen()
    } else if (el.msRequestFullscreen) {
      el.msRequestFullscreen()
    }
  } else {
    const doc: any = document
    if (doc.exitFullscreen) {
      doc.exitFullscreen()
    } else if (doc.webkitExitFullscreen) {
      doc.webkitExitFullscreen()
    } else if (doc.mozCancelFullScreen) {
      doc.mozCancelFullScreen()
    } else if (doc.msExitFullscreen) {
      doc.msExitFullscreen()
    }
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!(document.fullscreenElement || (document as any).webkitFullscreenElement)
}

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('webkitfullscreenchange', onFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', onFullscreenChange)
})
</script>

<style scoped>
.screen-full {
  cursor: pointer;
  vertical-align: -6px;
}
</style>
