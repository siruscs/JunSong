<template>
  <div class="table-toolbar">
    <div class="toolbar-left">
      <slot />
    </div>
    <div class="top-right-btn" :style="options.style">
      <el-tooltip :content="showSearch ? '隐藏搜索' : '显示搜索'">
        <el-button :icon="showSearch ? Hide : Plus" circle @click="toggleSearch" />
      </el-tooltip>
      <el-tooltip content="刷新">
        <el-button :icon="Refresh" circle @click="handleQuery" />
      </el-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Hide, Plus, Refresh } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  showSearch?: boolean
  options?: { style?: Record<string, any> }
}>(), {
  showSearch: true,
  options: () => ({ style: {} }),
})

const emit = defineEmits<{
  (e: 'update:showSearch', val: boolean): void
  (e: 'query'): void
}>()

function toggleSearch() {
  emit('update:showSearch', !props.showSearch)
}

function handleQuery() {
  emit('query')
}
</script>

<style scoped>
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left :deep(.el-button + .el-button) {
  margin-left: 0;
}
.top-right-btn {
  position: static;
  z-index: 1;
  display: flex;
  align-items: center;
}
.top-right-btn :deep(.el-button + .el-button) {
  margin-left: 6px;
}
</style>
