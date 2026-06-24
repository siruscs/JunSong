import { ref, reactive, toRefs } from 'vue'
import { useDictStore } from '@/stores/dict'

export function useDict(...args: string[]) {
  const res: any = reactive({ type: {} })
  args.forEach((dictType) => {
    res.type[dictType] = []
    const dictStore = useDictStore()
    dictStore.getDicts(dictType).then((data: any[]) => {
      res.type[dictType] = data.map((item: any) => ({
        label: item.dictLabel,
        value: item.dictValue,
        elTagType: item.listClass,
        elTagClass: item.cssClass,
        raw: item,
      }))
    })
  })
  return res
}

export function getDictDefaultValue(options: any[] = [], fallback: any = undefined) {
  const defaultOption = options.find((item: any) => item.raw?.isDefault === 'Y')
  return defaultOption?.value ?? fallback
}
