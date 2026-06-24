import { useRouter } from 'vue-router'
import { useTagsViewStore } from '@/stores/tagsView'

export function useTab() {
  const router = useRouter()
  const tagsViewStore = useTagsViewStore()

  function openPage(url: string) {
    router.push(url)
  }

  function closePage(obj: any) {
    tagsViewStore.delView(obj).then(({ visitedViews }) => {
      if (isCurrentPage(obj)) {
        toLastView(visitedViews, obj)
      }
    })
  }

  function closeAll() {
    tagsViewStore.delAllViews().then(({ visitedViews }) => {
      toLastView(visitedViews)
    })
  }

  function closeLeft(obj: any) {
    tagsViewStore.delLeftTags(obj).then(({ visitedViews }) => {
      if (!visitedViews.find((v) => v.path === router.currentRoute.value.path)) {
        toLastView(visitedViews, obj)
      }
    })
  }

  function closeRight(obj: any) {
    tagsViewStore.delRightTags(obj).then(({ visitedViews }) => {
      if (!visitedViews.find((v) => v.path === router.currentRoute.value.path)) {
        toLastView(visitedViews, obj)
      }
    })
  }

  function closeOther(obj: any) {
    tagsViewStore.delOthersViews(obj).then(() => {
      router.push(obj)
    })
  }

  function isCurrentPage(obj: any): boolean {
    return obj.path === router.currentRoute.value.path
  }

  function toLastView(visitedViews: any[], obj?: any) {
    const latestView = visitedViews.slice(-1)[0]
    if (latestView) {
      router.push(latestView.fullPath || latestView.path)
    } else {
      if (obj && obj.name === 'Dashboard') {
        router.replace({ path: '/redirect' + obj.fullPath })
      } else {
        router.push('/')
      }
    }
  }

  return { openPage, closePage, closeAll, closeLeft, closeRight, closeOther }
}
