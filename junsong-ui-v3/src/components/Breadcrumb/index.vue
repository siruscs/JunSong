<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="item.path">
        <span v-if="item.redirect === 'noredirect' || index === breadcrumbs.length - 1" class="no-redirect">
          {{ item.meta?.title }}
        </span>
        <a v-else @click.prevent="handleLink(item)">{{ item.meta?.title }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const breadcrumbs = ref<any[]>([])

function getBreadcrumb() {
  const matched = route.matched.filter((item) => item.meta && item.meta.title)
  breadcrumbs.value = matched.filter((item) => item.meta && item.meta.title && item.meta.breadcrumb !== false)
}

function handleLink(item: any) {
  const { redirect, path } = item
  if (redirect) {
    router.push(redirect)
    return
  }
  router.push(path)
}

watch(route, () => getBreadcrumb(), { immediate: true })
</script>

<style scoped>
.app-breadcrumb {
  font-size: 14px;
  line-height: 50px;
}
.no-redirect {
  color: #97a8be;
  cursor: text;
}
</style>
