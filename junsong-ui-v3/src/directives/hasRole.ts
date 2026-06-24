import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

function checkRole(el: HTMLElement, binding: DirectiveBinding) {
  const { value } = binding
  const userStore = useUserStore()
  const super_admin = 'admin'
  const roles = userStore.roles

  if (value && value instanceof Array && value.length > 0) {
    if (!roles.length) {
      return
    }
    const roleFlag = value
    const hasRole = roles.some((role) => {
      return super_admin === role || roleFlag.includes(role)
    })
    if (!hasRole) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  } else {
    throw new Error(`need roles! Like v-hasRole="['admin']"`)
  }
}

export const vHasRole: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    checkRole(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    checkRole(el, binding)
  },
}
