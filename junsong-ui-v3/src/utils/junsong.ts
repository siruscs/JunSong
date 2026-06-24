export function parseTime(time: any, cFormat?: string): string | null {
  if (arguments.length === 0 || !time) return null
  const format = cFormat || '{y}-{m}-{d} {h}:{i}:{s}'
  let date: Date
  if (typeof time === 'object') {
    date = time
  } else {
    if (typeof time === 'string') {
      if (/^[0-9]+$/.test(time)) {
        time = parseInt(time)
      }
    }
    if (typeof time === 'number' && time.toString().length === 10) {
      time = time * 1000
    }
    date = new Date(time)
  }
  const formatObj: Record<string, number> = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay(),
  }
  const time_str = format.replace(/{([ymdhisa])+}/g, (result, key) => {
    const value = formatObj[key]
    if (key === 'a') return ['日', '一', '二', '三', '四', '五', '六'][value]
    return value.toString().padStart(2, '0')
  })
  return time_str
}

export function resetForm(formEl: any) {
  if (formEl) {
    formEl.resetFields()
  }
}

export function addDateRange(params: any, dateRange: any, propName?: string) {
  const search = params
  const prop = propName || 'params'
  if (dateRange != null && dateRange !== '') {
    search[prop + 'BeginTime'] = dateRange[0]
    search[prop + 'EndTime'] = dateRange[1]
  } else {
    search[prop + 'BeginTime'] = ''
    search[prop + 'EndTime'] = ''
  }
  return search
}

export function selectDictLabel(dicts: any[], value: any): string {
  const item = dicts.find((d) => String(d.dictValue ?? d.value) === String(value))
  return item ? (item.dictLabel ?? item.label ?? '') : String(value ?? '')
}

export function selectDictLabels(dicts: any[], values: any, separator?: string): string {
  const sep = separator || ','
  const labels: string[] = []
  dicts.forEach((d) => {
    const dictValue = d.dictValue ?? d.value
    if (values.includes(dictValue)) {
      labels.push(d.dictLabel ?? d.label ?? '')
    }
  })
  return labels.join(sep)
}

export function handleTree(data: any[], id?: string, parentId?: string, children?: string): any[] {
  const _id = id || 'id'
  const _parentId = parentId || 'parentId'
  const _children = children || 'children'
  const result: any[] = []
  const map: Record<string, any> = {}
  data.forEach((item) => {
    map[item[_id]] = item
  })
  data.forEach((item) => {
    const parent = map[item[_parentId]]
    if (parent) {
      if (!parent[_children]) parent[_children] = []
      parent[_children].push(item)
    } else {
      result.push(item)
    }
  })
  return result
}

export function tansParams(params: any): string {
  let result = ''
  for (const propName of Object.keys(params)) {
    const value = params[propName]
    const part = encodeURIComponent(propName) + '='
    if (value !== null && value !== '' && typeof value !== 'undefined') {
      if (typeof value === 'object') {
        for (const key of Object.keys(value)) {
          if (value[key] !== null && value[key] !== '' && typeof value[key] !== 'undefined') {
            const params2 = propName + '[' + key + ']'
            const subPart = encodeURIComponent(params2) + '='
            result += subPart + encodeURIComponent(value[key]) + '&'
          }
        }
      } else {
        result += part + encodeURIComponent(value) + '&'
      }
    }
  }
  return result
}

export function blobValidate(data: Blob): boolean {
  return data.type !== 'application/json'
}
