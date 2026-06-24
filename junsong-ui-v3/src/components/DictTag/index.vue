<template>
  <template v-if="Array.isArray(propValue)">
    <template v-for="(val, i) in propValue" :key="i">
      <template v-for="item in options" :key="String(item.value)">
        <template v-if="String(item.value) === String(val)">
          <el-tag
            v-if="isShowTag(item)"
            :type="item.raw?.listClass || 'primary'"
            :class="item.raw?.cssClass"
            disable-transitions
          >
            {{ showValue ? item.label : '' }}
          </el-tag>
          <span v-else class="dict-text">{{ showValue ? item.label : '' }}</span>
          <span v-if="i < propValue.length - 1 && separator">{{ separator }}</span>
        </template>
      </template>
    </template>
  </template>
  <template v-else>
    <template v-for="item in options" :key="String(item.value)">
      <template v-if="String(item.value) === String(propValue)">
        <el-tag
          v-if="isShowTag(item)"
          :type="item.raw?.listClass || 'primary'"
          :class="item.raw?.cssClass"
          disable-transitions
        >
          {{ showValue ? item.label : '' }}
        </el-tag>
        <span v-else class="dict-text">{{ showValue ? item.label : '' }}</span>
      </template>
    </template>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface DictRaw {
  dictLabel?: string
  dictValue?: string | number
  listClass?: string
  cssClass?: string
  [key: string]: any
}
interface DictOption {
  label: string
  value: string | number
  raw?: DictRaw
  elTagType?: string
  elTagClass?: string
  [key: string]: any
}

const props = withDefaults(defineProps<{
  options?: DictOption[]
  value?: number | string | Array<number | string>
  showValue?: boolean
  separator?: string
}>(), {
  options: () => [],
  value: '',
  showValue: true,
  separator: ',',
})

const propValue = computed(() => props.value)

function isShowTag(item: DictOption): boolean {
  const listClass = item.raw?.listClass || item.elTagType
  return !!listClass && listClass !== 'default' && listClass !== ''
}
</script>

<style scoped>
.dict-text {
  display: inline;
}
</style>
