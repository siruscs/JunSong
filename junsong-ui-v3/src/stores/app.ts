import { defineStore } from 'pinia'
import { ref } from 'vue'
import Cookies from 'js-cookie'

export const useAppStore = defineStore('app', () => {
  const sidebar = ref({
    opened: Cookies.get('sidebarStatus') ? !!+Cookies.get('sidebarStatus')! : true,
    withoutAnimation: false,
    hide: false,
  })
  const device = ref<string>('desktop')
  const size = ref<string>(Cookies.get('size') || 'default')

  function toggleSideBar() {
    if (sidebar.value.hide) return
    sidebar.value.opened = !sidebar.value.opened
    sidebar.value.withoutAnimation = false
    if (sidebar.value.opened) {
      Cookies.set('sidebarStatus', '1')
    } else {
      Cookies.set('sidebarStatus', '0')
    }
  }

  function closeSideBar(withoutAnimation: boolean) {
    Cookies.set('sidebarStatus', '0')
    sidebar.value.opened = false
    sidebar.value.withoutAnimation = withoutAnimation
  }

  function toggleDevice(val: string) {
    device.value = val
  }

  function setSize(val: string) {
    size.value = val
    Cookies.set('size', val)
  }

  function toggleSideBarHide(status: boolean) {
    sidebar.value.hide = status
  }

  return { sidebar, device, size, toggleSideBar, closeSideBar, toggleDevice, setSize, toggleSideBarHide }
})
