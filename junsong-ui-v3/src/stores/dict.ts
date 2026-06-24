import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDictDataByType } from '@/api/system/dict/data'

export const useDictStore = defineStore('dict', () => {
  const dict = ref<Record<string, any[]>>({})

  async function getDicts(dictType: string) {
    if (dict.value[dictType]) {
      return dict.value[dictType]
    }
    const res: any = await getDictDataByType(dictType)
    dict.value[dictType] = res.data
    return res.data
  }

  function removeDict(dictType: string) {
    delete dict.value[dictType]
  }

  function cleanDict() {
    dict.value = {}
  }

  return { dict, getDicts, removeDict, cleanDict }
})
