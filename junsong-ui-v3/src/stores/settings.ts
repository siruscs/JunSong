import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getThemePreset, applyTheme, saveThemeKey, getSavedThemeKey } from '@/utils/theme'

export const useSettingsStore = defineStore('settings', () => {
  const title = ref<string>('峻松管理系统')
  const theme = ref<string>(getThemePreset(getSavedThemeKey()).primary)
  const sideTheme = ref<string>('theme-dark')
  const showSettings = ref<boolean>(false)
  const topNav = ref<boolean>(false)
  const tagsView = ref<boolean>(true)
  const fixedHeader = ref<boolean>(false)
  const sidebarLogo = ref<boolean>(true)
  const dynamicTitle = ref<boolean>(false)
  const themePreset = ref<string>(getSavedThemeKey())

  function changeSetting(data: { key: string; value: any }) {
    const { key, value } = data
    switch (key) {
      case 'theme':
        theme.value = value
        break
      case 'sideTheme':
        sideTheme.value = value
        break
      case 'showSettings':
        showSettings.value = value
        break
      case 'topNav':
        topNav.value = value
        break
      case 'tagsView':
        tagsView.value = value
        break
      case 'fixedHeader':
        fixedHeader.value = value
        break
      case 'sidebarLogo':
        sidebarLogo.value = value
        break
      case 'dynamicTitle':
        dynamicTitle.value = value
        break
    }
  }

  function setTitle(val: string) {
    title.value = val
  }

  function changeThemePreset(key: string) {
    const preset = getThemePreset(key)
    themePreset.value = key
    theme.value = preset.primary
    applyTheme(preset)
    saveThemeKey(key)
  }

  return {
    title, theme, sideTheme, showSettings, topNav,
    tagsView, fixedHeader, sidebarLogo, dynamicTitle, themePreset,
    changeSetting, setTitle, changeThemePreset,
  }
})
