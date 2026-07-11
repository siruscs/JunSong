<template>
  <el-select
    v-model="innerValue"
    filterable
    remote
    reserve-keyword
    clearable
    :remote-method="remoteSearch"
    :loading="loading"
    :placeholder="placeholder"
    :disabled="disabled"
    style="width: 100%"
    @change="handleChange"
    @clear="handleClear"
  >
    <el-option
      v-for="item in options"
      :key="item.memberId"
      :label="`${item.memberNo} ${item.memberName}`"
      :value="item.memberId"
    >
      <div class="member-option">
        <span class="member-option-main">{{ item.memberNo }} {{ item.memberName }}</span>
        <span class="member-option-sub">{{ item.phone || '-' }}</span>
      </div>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { listMember, getMember } from '@/api/member/member'

interface MemberInfo {
  memberId: number
  memberNo: string
  memberName: string
  phone?: string
  cardType?: string
  availablePoints?: number
  [key: string]: any
}

const props = withDefaults(defineProps<{
  modelValue?: number | null
  disabled?: boolean
  placeholder?: string
  defaultLabel?: string
  pageSize?: number
}>(), {
  modelValue: null,
  disabled: false,
  placeholder: '输入会员编号或姓名搜索',
  defaultLabel: '',
  pageSize: 20,
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: number | null): void
  (e: 'change', member: MemberInfo | null): void
}>()

const innerValue = ref<number | null>(props.modelValue)
const loading = ref(false)
const options = ref<MemberInfo[]>([])

watch(() => props.modelValue, (val) => {
  innerValue.value = val
})

onMounted(async () => {
  if (props.modelValue) {
    if (props.defaultLabel) {
      options.value = [{ memberId: props.modelValue, memberNo: '', memberName: props.defaultLabel } as MemberInfo]
    } else {
      try {
        const res: any = await getMember(props.modelValue)
        const m = res.data || res
        if (m && m.memberId) {
          options.value = [m]
        }
      } catch {
        options.value = [{ memberId: props.modelValue, memberNo: String(props.modelValue), memberName: '已选择会员' } as MemberInfo]
      }
    }
  }
})

async function remoteSearch(keyword: string) {
  const kw = String(keyword || '').trim()
  if (!kw) {
    options.value = []
    return
  }
  loading.value = true
  try {
    const isNoLike = /^[A-Za-z0-9]/.test(kw)
    const noRes: any = isNoLike
      ? await listMember({ pageNum: 1, pageSize: props.pageSize, memberNo: kw })
      : { rows: [] }
    const nameRes: any = await listMember({ pageNum: 1, pageSize: props.pageSize, memberName: kw })
    const noList: MemberInfo[] = noRes.rows || []
    const nameList: MemberInfo[] = nameRes.rows || []
    const seen = new Set<number>()
    const merged: MemberInfo[] = []
    for (const m of [...noList, ...nameList]) {
      if (m.memberId && !seen.has(m.memberId)) {
        seen.add(m.memberId)
        merged.push(m)
      }
    }
    options.value = merged.slice(0, props.pageSize)
  } catch {
    options.value = []
  } finally {
    loading.value = false
  }
}

function handleChange(val: number | null) {
  emit('update:modelValue', val)
  const selected = val ? options.value.find(m => m.memberId === val) || null : null
  emit('change', selected)
}

function handleClear() {
  options.value = []
  emit('update:modelValue', null)
  emit('change', null)
}
</script>

<style scoped>
.member-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.member-option-main {
  font-weight: 500;
}
.member-option-sub {
  color: #94a3b8;
  font-size: 12px;
  margin-left: 12px;
}
</style>
