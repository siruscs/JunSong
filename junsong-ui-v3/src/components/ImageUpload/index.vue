<template>
  <div class="component-upload-image">
    <el-upload
      :action="uploadAction"
      :limit="limit"
      :data="data"
      :headers="headers"
      :file-list="fileList"
      :before-upload="handleBeforeUpload"
      :on-exceed="handleExceed"
      :on-success="handleSuccess"
      :on-remove="handleRemove"
      list-type="picture-card"
      :disabled="disabled"
      multiple
    >
      <el-icon><Plus /></el-icon>
    </el-upload>
    <div v-if="isShowTip" class="el-upload__tip">
      只能上传 {{ fileType.join('/') }} 格式，且不超过 {{ fileSize }}MB
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

const props = withDefaults(defineProps<{
  modelValue?: string | object | Array<any>
  action?: string
  data?: object
  limit?: number
  fileSize?: number
  fileType?: string[]
  isShowTip?: boolean
  disabled?: boolean
}>(), {
  modelValue: '',
  action: '/file/upload',
  data: () => ({}),
  limit: 5,
  fileSize: 5,
  fileType: () => ['png', 'jpg', 'jpeg'],
  isShowTip: true,
  disabled: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string | object | Array<any>): void
}>()

const uploadAction = computed(() => import.meta.env.VITE_APP_BASE_API + props.action)
const headers = computed(() => ({ Authorization: 'Bearer ' + (getToken() || '') }))

const fileList = ref<any[]>([])

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      if (Array.isArray(val)) {
        fileList.value = val.map((url) => ({ name: url, url }))
      } else if (typeof val === 'string') {
        fileList.value = [{ name: val, url: val }]
      }
    } else {
      fileList.value = []
    }
  },
  { immediate: true },
)

function handleBeforeUpload(file: File) {
  const name = file.name.toLowerCase()
  const ext = name.split('.').pop() || ''
  const isTypeOk = props.fileType.includes(ext)
  const isSizeOk = file.size / 1024 / 1024 < props.fileSize
  if (!isTypeOk) {
    ElMessage.error('仅支持 ' + props.fileType.join('/') + ' 格式')
    return false
  }
  if (!isSizeOk) {
    ElMessage.error('上传文件大小不能超过 ' + props.fileSize + 'MB')
    return false
  }
  return true
}

function handleExceed() {
  ElMessage.warning('上传文件数量不能超过 ' + props.limit + ' 个')
}

function handleSuccess(response: any, _file: any, files: any[]) {
  if (Array.isArray(props.modelValue)) {
    const urls = files.map((f: any) => (f.response ? f.response.url : f.url))
    emit('update:modelValue', urls)
  } else {
    const url = response?.url || response
    emit('update:modelValue', url)
  }
}

function handleRemove(_file: any, files: any[]) {
  if (Array.isArray(props.modelValue)) {
    const urls = files.map((f: any) => f.url || (f.response && f.response.url))
    emit('update:modelValue', urls)
  } else {
    emit('update:modelValue', '')
  }
}
</script>

<style scoped>
.component-upload-image .el-upload--picture-card {
  width: 100px;
  height: 100px;
}
</style>
