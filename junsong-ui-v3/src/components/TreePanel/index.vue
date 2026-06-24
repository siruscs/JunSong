<template>
  <el-card class="tree-panel" shadow="hover">
    <div class="tree-header">
      <span class="tree-title">{{ title }}</span>
      <el-input
        v-model="filterText"
        size="small"
        placeholder="输入关键字过滤"
        clearable
        style="width: 100%; margin-top: 8px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <el-tree
      ref="treeRef"
      class="filter-tree"
      :data="treeData"
      :props="treeProps"
      :filter-node-method="filterNode"
      node-key="id"
      default-expand-all
      highlight-current
      @node-click="handleNodeClick"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  title?: string
  treeData?: any[]
  treeProps?: { children?: string; label?: string }
}>(), {
  title: '树',
  treeData: () => [],
  treeProps: () => ({ children: 'children', label: 'label' }),
})

const emit = defineEmits<{
  (e: 'node-click', node: any): void
}>()

const treeRef = ref<any>(null)
const filterText = ref('')

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

function filterNode(value: string, data: any) {
  if (!value) return true
  const labelKey = props.treeProps?.label || 'label'
  return (data[labelKey] || '').indexOf(value) !== -1
}

function handleNodeClick(data: any) {
  emit('node-click', data)
}
</script>

<style scoped>
.tree-panel {
  height: 100%;
  box-sizing: border-box;
}
.tree-header {
  margin-bottom: 12px;
}
.tree-title {
  font-weight: 600;
  color: #303133;
}
.filter-tree {
  background: transparent;
}
</style>
