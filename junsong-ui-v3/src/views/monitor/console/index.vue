<template>
  <div class="console-container">
    <iframe
      v-if="targetUrl"
      :src="targetUrl"
      frameborder="no"
      class="console-iframe"
      scrolling="auto"
    />
    <div v-else class="console-empty">
      <el-result icon="warning" title="控制台未部署" sub-title="该监控控制台服务未启动，请联系管理员部署对应服务。">
        <template #extra>
          <el-button type="primary" @click="goBack">返回</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const consoleMap: Record<string, string> = {
  nacos: '/nacos/',
  admin: '/admin/',
  sentinel: '/sentinel/',
  druid: '/druid/',
}

const targetUrl = computed(() => {
  const link = (route.meta?.link as string) || ''
  if (link) {
    const key = link.replace(/^\//, '').toLowerCase()
    if (consoleMap[key]) return consoleMap[key]
  }
  const segments = (route.path || '').split('/').filter(Boolean)
  const lastSegment = segments[segments.length - 1] || ''
  const key = lastSegment.toLowerCase()
  return consoleMap[key] || ''
})

function goBack() {
  router.back()
}
</script>

<style scoped>
.console-container {
  height: calc(100vh - 84px);
  width: 100%;
}
.console-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
.console-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
