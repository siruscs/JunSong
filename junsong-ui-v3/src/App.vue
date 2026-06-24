<template>
  <el-config-provider :locale="zhCn" :size="appStore.size as any">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>
