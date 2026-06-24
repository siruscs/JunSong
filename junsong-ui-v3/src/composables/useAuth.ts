import { useUserStore } from '@/stores/user'

export function useAuth() {
  const userStore = useUserStore()

  function authPermission(permission: string): boolean {
    const all_permission = '*:*:*'
    const permissions = userStore.permissions
    return permissions.some((v) => all_permission === v || v === permission)
  }

  function authRole(role: string): boolean {
    const super_admin = 'admin'
    const roles = userStore.roles
    return roles.some((v) => super_admin === v || v === role)
  }

  function hasPermi(permission: string): boolean {
    return authPermission(permission)
  }

  function hasPermiOr(permissions: string[]): boolean {
    return permissions.some((item) => authPermission(item))
  }

  function hasPermiAnd(permissions: string[]): boolean {
    return permissions.every((item) => authPermission(item))
  }

  function hasRole(role: string): boolean {
    return authRole(role)
  }

  function hasRoleOr(roles: string[]): boolean {
    return roles.some((item) => authRole(item))
  }

  function hasRoleAnd(roles: string[]): boolean {
    return roles.every((item) => authRole(item))
  }

  return { hasPermi, hasPermiOr, hasPermiAnd, hasRole, hasRoleOr, hasRoleAnd }
}
