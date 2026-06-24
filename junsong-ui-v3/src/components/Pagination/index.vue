<template>
  <div v-if="!hidden" class="pagination-container">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="pageSizes"
      :pager-count="pagerCount"
      :layout="layout"
      :total="total"
      :background="background"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  total: number
  page?: number
  limit?: number
  pageSizes?: number[]
  pagerCount?: number
  layout?: string
  background?: boolean
  autoScroll?: boolean
  hidden?: boolean
}>(), {
  page: 1,
  limit: 20,
  pageSizes: () => [10, 20, 30, 50],
  pagerCount: 7,
  layout: 'total, sizes, prev, pager, next, jumper',
  background: true,
  autoScroll: true,
  hidden: false,
})

const emit = defineEmits<{
  (e: 'update:page', val: number): void
  (e: 'update:limit', val: number): void
  (e: 'pagination', val: { page: number; limit: number }): void
}>()

const currentPage = ref(props.page)
const pageSize = ref(props.limit)

watch(
  () => props.page,
  (val) => {
    currentPage.value = val
  },
)
watch(
  () => props.limit,
  (val) => {
    pageSize.value = val
  },
)

function handleSizeChange(val: number) {
  pageSize.value = val
  emit('update:limit', val)
  emit('pagination', { page: currentPage.value, limit: val })
  if (props.autoScroll) scrollToTop()
}

function handleCurrentChange(val: number) {
  currentPage.value = val
  emit('update:page', val)
  emit('pagination', { page: val, limit: pageSize.value })
  if (props.autoScroll) scrollToTop()
}

function scrollToTop() {
  nextTick(() => {
    document.getElementsByClassName('app-main')[0]?.scrollTo({ top: 0, behavior: 'smooth' })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  })
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
</style>
