import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLockStore = defineStore('lock', () => {
  const isLock = ref(false)
  const lockPath = ref('')

  function lockScreen(path: string) {
    isLock.value = true
    lockPath.value = path
  }

  function unlockScreen() {
    isLock.value = false
    lockPath.value = ''
  }

  return { isLock, lockPath, lockScreen, unlockScreen }
})
