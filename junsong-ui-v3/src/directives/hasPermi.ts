import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

function checkPermission(el: HTMLElement, binding: DirectiveBinding) {
  const { value } = binding
  const userStore = useUserStore()
  const all_permission = '*:*:*'
  const permissions = userStore.permissions

  if (value && value instanceof Array && value.length > 0) {
    if (!permissions.length) {
      return
    }
    const permissionFlag = value
    const hasPermissions = permissions.some((permission) => {
      return all_permission === permission || permissionFlag.includes(permission)
    })
    if (!hasPermissions) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  } else {
    throw new Error(`need permissions! Like v-hasPermi="['system:user:add']"`)
  }
}

export const vHasPermi: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    checkPermission(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    checkPermission(el, binding)
  },
}
