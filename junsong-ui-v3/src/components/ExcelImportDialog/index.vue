<template>
  <el-dialog v-model="visible" :title="title" :width="width" append-to-body @close="handleClose">
    <el-upload
      ref="uploadRef"
      drag
      :limit="1"
      accept=".xlsx,.xls"
      :headers="headers"
      :action="uploadUrl"
      :disabled="isUploading"
      :on-progress="handleProgress"
      :on-success="handleSuccess"
      :on-error="handleError"
      :auto-upload="false"
      :file-list="fileList"
      :on-change="handleFileChange"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">
          <el-checkbox v-model="updateSupport">{{ updateSupportLabel }}</el-checkbox>
          <span class="import-tip">仅允许导入 xls、xlsx 格式文件。</span>
          <el-link v-if="templateAction" type="primary" :underline="false" @click="handleDownloadTemplate">下载模板</el-link>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" :loading="isUploading" @click="handleSubmit">确 定</el-button>
        <el-button @click="visible = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import { useDownload } from '@/composables/useDownload'

const props = withDefaults(defineProps<{
  title?: string
  width?: string
  action: string
  templateAction?: string
  templateFileName?: string
  updateSupportLabel?: string
}>(), {
  title: '数据导入',
  width: '420px',
  templateAction: '',
  templateFileName: 'template',
  updateSupportLabel: '是否更新已经存在的数据',
})

const emit = defineEmits<{
  (event: 'success'): void
}>()

const { download } = useDownload()
const visible = ref(false)
const isUploading = ref(false)
const updateSupport = ref(false)
const uploadRef = ref<InstanceType<typeof import('element-plus').ElUpload> | null>(null)
const fileList = ref<any[]>([])

const headers = computed(() => ({ Authorization: 'Bearer ' + getToken() }))
const uploadUrl = computed(() => {
  const baseURL = import.meta.env.VITE_APP_BASE_API || ''
  return `${baseURL}${props.action}?updateSupport=${updateSupport.value ? 1 : 0}`
})

function open() {
  updateSupport.value = false
  isUploading.value = false
  visible.value = true
  fileList.value = []
  setTimeout(() => uploadRef.value?.clearFiles?.())
}

function handleClose() {
  isUploading.value = false
  fileList.value = []
  uploadRef.value?.clearFiles?.()
}

function handleFileChange(file: any, files: any[]) {
  fileList.value = files
}

function handleDownloadTemplate() {
  download(props.templateAction, {}, `${props.templateFileName}_${new Date().getTime()}.xlsx`)
}

function handleProgress() {
  isUploading.value = true
}

function handleSuccess(response: any) {
  visible.value = false
  isUploading.value = false
  fileList.value = []
  uploadRef.value?.clearFiles?.()
  ElMessageBox.alert(
    `<div style="overflow:auto;overflow-x:hidden;max-height:70vh;padding:10px 20px 0;">${response?.msg || '导入完成'}</div>`,
    '导入结果',
    { dangerouslyUseHTMLString: true },
  )
  emit('success')
}

function handleError() {
  isUploading.value = false
  ElMessage.error('导入失败，请稍后重试')
}

function handleSubmit() {
  if (!fileList.value.length) {
    ElMessage.error('请选择要上传的文件。')
    return
  }
  const name = String(fileList.value[0].name || '').toLowerCase()
  if (!name.endsWith('.xls') && !name.endsWith('.xlsx')) {
    ElMessage.error('请选择后缀为 xls 或 xlsx 的文件。')
    return
  }
  uploadRef.value?.submit?.()
}

defineExpose({ open })
</script>

<style scoped>
.import-tip {
  margin: 0 10px;
}
</style>
