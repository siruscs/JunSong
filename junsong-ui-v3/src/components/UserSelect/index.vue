<template>
  <el-select
    v-model="innerValue"
    filterable
    remote
    reserve-keyword
    clearable
    :remote-method="searchUsers"
    :loading="loading"
    :placeholder="placeholder"
    :disabled="disabled"
    style="width: 100%"
    @change="handleChange"
    @clear="handleClear"
  >
    <el-option
      v-for="u in options"
      :key="u.userId"
      :label="formatLabel(u)"
      :value="u.userName"
    >
      <div class="user-option">
        <span class="user-option-main">{{ formatLabel(u) }}</span>
      </div>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { listUser } from '@/api/system/user'

interface UserInfo {
  userId: number
  userName: string
  nickName?: string
  [key: string]: any
}

const props = withDefaults(defineProps<{
  modelValue?: string
  disabled?: boolean
  placeholder?: string
  pageSize?: number
}>(), {
  modelValue: '',
  disabled: false,
  placeholder: '请选择用户',
  pageSize: 20,
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'change', user: UserInfo | null): void
}>()

const innerValue = ref<string>(props.modelValue)
const loading = ref(false)
const options = ref<UserInfo[]>([])

watch(() => props.modelValue, (val) => {
  innerValue.value = val
})

function formatLabel(u: UserInfo) {
  if (!u) return ''
  return u.nickName ? `${u.nickName}（${u.userName}）` : u.userName || `用户 ${u.userId}`
}

async function searchUsers(query: string) {
  const kw = String(query || '').trim()
  if (!kw) {
    options.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await listUser({ userName: kw, pageNum: 1, pageSize: props.pageSize })
    options.value = res.rows || res.data?.rows || []
  } catch {
    options.value = []
  } finally {
    loading.value = false
  }
}

function handleChange(val: string) {
  emit('update:modelValue', val || '')
  const selected = val ? options.value.find((u) => u.userName === val) || null : null
  emit('change', selected)
}

function handleClear() {
  options.value = []
  emit('update:modelValue', '')
  emit('change', null)
}

onMounted(async () => {
  if (props.modelValue) {
    try {
      const query = /^\d+$/.test(String(props.modelValue))
        ? { userId: props.modelValue, pageNum: 1, pageSize: 1 }
        : { userName: props.modelValue, pageNum: 1, pageSize: 1 }
      const res: any = await listUser(query)
      options.value = res.rows || res.data?.rows || []
    } catch {
      options.value = []
    }
  }
})
</script>

<style scoped>
.user-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.user-option-main {
  font-weight: 500;
}
</style>
