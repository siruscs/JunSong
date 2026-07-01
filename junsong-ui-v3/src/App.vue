<template>
  <el-config-provider :locale="zhCn" :size="appStore.size as any">
    <router-view />
    <template #empty>
      <div class="global-empty-state">
        <el-icon :size="40" color="#c0c4cc"><DocumentRemove /></el-icon>
        <p>暂无数据</p>
      </div>
    </template>
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { DocumentRemove } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<style>
.global-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: #909399;
  font-size: 13px;
  gap: 8px;
}
.global-empty-state p {
  margin: 0;
}
</style>
