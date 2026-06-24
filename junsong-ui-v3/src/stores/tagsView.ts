import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref<any[]>([])
  const cachedViews = ref<string[]>([])

  function addView(view: any) {
    addVisitedView(view)
    addCachedView(view)
  }

  function addVisitedView(view: any) {
    if (visitedViews.value.some((v) => v.path === view.path)) return
    visitedViews.value.push(Object.assign({}, view, { title: view.meta?.title || 'no-name' }))
  }

  function addCachedView(view: any) {
    if (cachedViews.value.includes(view.name!)) return
    if (!view.meta?.noCache) {
      cachedViews.value.push(view.name!)
    }
  }

  function delView(view: any) {
    return new Promise<{ visitedViews: any[]; cachedViews: string[] }>((resolve) => {
      delVisitedView(view)
      delCachedView(view)
      resolve({ visitedViews: [...visitedViews.value], cachedViews: [...cachedViews.value] })
    })
  }

  function delVisitedView(view: any) {
    return new Promise((resolve) => {
      for (const [i, v] of visitedViews.value.entries()) {
        if (v.path === view.path) {
          visitedViews.value.splice(i, 1)
          break
        }
      }
      resolve([...visitedViews.value])
    })
  }

  function delCachedView(view: any) {
    return new Promise((resolve) => {
      const index = cachedViews.value.indexOf(view.name!)
      if (index > -1) cachedViews.value.splice(index, 1)
      resolve([...cachedViews.value])
    })
  }

  function delOthersViews(view: any) {
    return new Promise((resolve) => {
      visitedViews.value = visitedViews.value.filter((v) => {
        return v.meta?.affix || v.path === view.path
      })
      cachedViews.value = cachedViews.value.filter((name) => name === view.name)
      resolve({ visitedViews: [...visitedViews.value], cachedViews: [...cachedViews.value] })
    })
  }

  function delAllViews() {
    return new Promise((resolve) => {
      const affixTags = visitedViews.value.filter((tag) => tag.meta?.affix)
      visitedViews.value = affixTags
      cachedViews.value = []
      resolve({ visitedViews: [...visitedViews.value], cachedViews: [...cachedViews.value] })
    })
  }

  function delLeftTags(view: any) {
    return new Promise((resolve) => {
      const currentIndex = visitedViews.value.findIndex((v) => v.path === view.path)
      if (currentIndex === -1) return resolve({ visitedViews: [...visitedViews.value] })
      visitedViews.value = visitedViews.value.filter((item, index) => {
        return item.meta?.affix || index >= currentIndex
      })
      resolve({ visitedViews: [...visitedViews.value] })
    })
  }

  function delRightTags(view: any) {
    return new Promise((resolve) => {
      const currentIndex = visitedViews.value.findIndex((v) => v.path === view.path)
      if (currentIndex === -1) return resolve({ visitedViews: [...visitedViews.value] })
      visitedViews.value = visitedViews.value.filter((item, index) => {
        return item.meta?.affix || index <= currentIndex
      })
      resolve({ visitedViews: [...visitedViews.value] })
    })
  }

  function updateVisitedView(view: any) {
    for (let v of visitedViews.value) {
      if (v.path === view.path) {
        v = Object.assign(v, view)
        break
      }
    }
  }

  return {
    visitedViews, cachedViews,
    addView, addVisitedView, addCachedView,
    delView, delVisitedView, delCachedView,
    delOthersViews, delAllViews, delLeftTags, delRightTags,
    updateVisitedView,
  }
})
