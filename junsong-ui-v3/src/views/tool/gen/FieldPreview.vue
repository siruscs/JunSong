<template>
  <div class="gen-field-preview">
    <el-input v-if="htmlType === 'input'" v-model="demoValue" :placeholder="label ? '请输入' + label : '请输入'" />
    <el-input v-else-if="htmlType === 'textarea'" v-model="demoValue" type="textarea" :rows="3" :placeholder="label ? '请输入' + label : '请输入内容'" />
    <el-select v-else-if="htmlType === 'select'" v-model="demoValue" :placeholder="label ? '请选择' + label : '请选择'" clearable style="width: 100%;">
      <el-option v-for="opt in dictOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      <el-option v-if="!dictOptions.length" label="示例选项1" value="1" />
      <el-option v-if="!dictOptions.length" label="示例选项2" value="2" />
    </el-select>
    <el-radio-group v-else-if="htmlType === 'radio'" v-model="demoValue">
      <el-radio v-for="opt in dictOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
      <template v-if="!dictOptions.length">
        <el-radio value="1">选项1</el-radio>
        <el-radio value="2">选项2</el-radio>
      </template>
    </el-radio-group>
    <el-checkbox-group v-else-if="htmlType === 'checkbox'" v-model="demoValue">
      <el-checkbox v-for="opt in dictOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-checkbox>
      <template v-if="!dictOptions.length">
        <el-checkbox value="1">选项A</el-checkbox>
        <el-checkbox value="2">选项B</el-checkbox>
      </template>
    </el-checkbox-group>
    <el-date-picker v-else-if="htmlType === 'datetime'" v-model="demoValue" value-format="YYYY-MM-DD" type="date" :placeholder="label ? '请选择' + label : '请选择日期'" style="width: 100%;" />
    <div v-else-if="htmlType === 'imageUpload'" class="preview-placeholder">
      <el-upload :show-file-list="false" :auto-upload="false" action="#" :before-upload="() => false">
        <el-button type="primary" :icon="Plus">上传图片</el-button>
      </el-upload>
    </div>
    <div v-else-if="htmlType === 'upload'" class="preview-placeholder">
      <el-upload :show-file-list="false" :auto-upload="false" action="#" :before-upload="() => false">
        <el-button type="primary" :icon="Upload">上传文件</el-button>
      </el-upload>
    </div>
    <div v-else-if="htmlType === 'editor'" class="preview-placeholder">
      <div class="preview-editor-hint">
        <el-icon><Document /></el-icon>
        <span>富文本编辑器</span>
      </div>
    </div>
    <el-input v-else v-model="demoValue" placeholder="未知类型" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Plus, Upload, Document } from '@element-plus/icons-vue'
import { useDict } from '@/composables/useDict'

const props = defineProps<{
  htmlType: string
  dictType?: string
  label?: string
}>()

const demoValue = ref<any>(
  props.htmlType === 'checkbox' ? [] : props.htmlType === 'radio' || props.htmlType === 'select' ? '' : props.htmlType === 'datetime' ? null : ''
)

watch(
  () => props.htmlType,
  (newType) => {
    if (newType === 'checkbox') demoValue.value = []
    else if (newType === 'datetime') demoValue.value = null
    else if (newType === 'radio' || newType === 'select') demoValue.value = ''
    else demoValue.value = ''
  }
)

const dictData = props.dictType ? useDict(props.dictType) : null
const dictOptions = ref<{ label: string; value: any }[]>([])
watch(
  () => [props.dictType, props.htmlType],
  () => {
    if (dictData && props.dictType) {
      const list = (dictData.type as any)[props.dictType] || []
      dictOptions.value = list.map((d: any) => ({ label: d.label, value: d.value }))
    } else {
      dictOptions.value = []
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.gen-field-preview {
  width: 100%;
}
.preview-placeholder {
  display: flex;
  align-items: center;
  padding: 8px 0;
}
.preview-editor-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
  font-size: 13px;
  width: 100%;
}
</style>
