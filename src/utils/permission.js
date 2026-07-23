import { modules } from '@/config/modules.js'

function rawGrants(grants) {
  if (grants === undefined) {
    grants = uni.getStorageSync('modules') || []
  }
  return Array.isArray(grants) ? grants : []
}

function rawActionGrants(grants) {
  if (grants === undefined) {
    grants = uni.getStorageSync('permissions') || []
  }
  return Array.isArray(grants) ? grants : []
}

function normalizeGrant(grant) {
  if (typeof grant === 'string') return grant
  if (grant && typeof grant === 'object') {
    return grant.key || grant.moduleKey || grant.perms || grant.permission || ''
  }
  return ''
}

function collectPermissions(config) {
  const values = Object.values(config?.permissions || {})
  return values.reduce((list, item) => {
    if (Array.isArray(item)) list.push(...item)
    else if (item) list.push(item)
    return list
  }, [])
}

export function getPermissionGrants(grants) {
  return rawGrants(grants).map(normalizeGrant).filter(Boolean)
}

export function hasModulePermission(moduleKey, grants) {
  grants = getPermissionGrants(grants)
  if (grants.length === 0) return false
  const config = modules[moduleKey]
  const aliases = [moduleKey, config?.authKey].filter(Boolean)
  return grants.some((grant) => aliases.includes(grant))
}

export function hasActionPermission(moduleKey, action, grants) {
  const config = modules[moduleKey]
  if (!config) return false
  grants = rawActionGrants(grants).map(normalizeGrant).filter(Boolean)
  if (grants.length === 0) return false
  const moduleKeys = [moduleKey, config.authKey].filter(Boolean)
  const permissions = []
  const actionPerms = config.permissions?.[action]
  if (Array.isArray(actionPerms)) permissions.push(...actionPerms)
  else if (actionPerms) permissions.push(actionPerms)
  if (permissions.length === 0 && action) {
    moduleKeys.forEach((key) => permissions.push(`${key}:${action}`))
  }
  return grants.some((grant) => {
    return grant === '*:*:*' || permissions.includes(grant)
  })
}

export function hasExactPermission(permission, grants) {
  grants = rawActionGrants(grants).map(normalizeGrant).filter(Boolean)
  return grants.some((grant) => grant === '*:*:*' || grant === permission)
}

export function filterAuthorizedGroups(groups, grants) {
  return groups.map((group) => ({
    ...group,
    items: group.items.filter((item) => hasModulePermission(item.key, grants))
  })).filter((group) => group.items.length > 0)
}

export function requireModulePermission(moduleKey, grants) {
  if (hasModulePermission(moduleKey, grants)) return true
  uni.showToast({ title: '暂无该功能权限', icon: 'none' })
  setTimeout(() => uni.navigateBack(), 500)
  return false
}

// 判断当前用户是否为管理员（拥有 system:user 权限）
export function isAdmin(grants) {
  grants = getPermissionGrants(grants)
  return grants.some((grant) => grant.startsWith('system:user') || grant === 'admin' || grant === 'userManage')
}
