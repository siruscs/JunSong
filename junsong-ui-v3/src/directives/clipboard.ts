import type { DirectiveBinding, ObjectDirective } from 'vue'
import { ElMessage } from 'element-plus'

async function copyToClipboard(text: string) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
      return true
    }
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.clipPath = 'circle(0)'
    document.body.appendChild(textarea)
    textarea.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(textarea)
    return ok
  } catch {
    return false
  }
}

export const vClipboard: ObjectDirective = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const handler = async (e: Event) => {
      let text: string = ''
      if (binding.value) {
        text = String(binding.value)
      } else {
        text = (el.innerText || el.textContent || '').trim()
      }
      if (!text) {
        ElMessage.warning('无可复制的内容')
        return
      }
      const ok = await copyToClipboard(text)
      if (ok) {
        ElMessage.success('复制成功')
      } else {
        ElMessage.error('复制失败')
      }
    }
    el.addEventListener('click', handler)
    ;(el as any).__vClipboardHandler = handler
  },
  unmounted(el: HTMLElement) {
    const handler = (el as any).__vClipboardHandler
    if (handler) {
      el.removeEventListener('click', handler)
    }
  },
}

export default vClipboard
