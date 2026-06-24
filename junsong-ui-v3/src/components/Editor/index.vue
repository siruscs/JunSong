<template>
  <div class="editor-container">
    <el-input
      v-model="editorValue"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      @input="handleInput"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  rows?: number
  placeholder?: string
}>(), {
  modelValue: '',
  rows: 6,
  placeholder: '请输入内容',
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
}>()

const editorValue = ref(props.modelValue)

watch(
  () => props.modelValue,
  (val) => {
    editorValue.value = val
  },
)

function handleInput(val: string) {
  emit('update:modelValue', val)
}
</script>

<style scoped>
.editor-container {
  width: 100%;
}
</style>
