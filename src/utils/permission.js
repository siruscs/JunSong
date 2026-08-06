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

// 页面入口既可能由模块授权驱动，也可能由后端下发的操作权限驱动。
// 操作权限只解决“能否进入/看到页面”，具体接口仍由服务端权限校验。
export function hasModuleOrActionPermission(moduleKey, moduleGrants, actionGrants) {
  if (hasModulePermission(moduleKey, moduleGrants)) return true
  const actions = Object.keys(modules[moduleKey]?.permissions || {})
  return actions.some((action) => hasActionPermission(moduleKey, action, actionGrants))
}

export function hasExactPermission(permission, grants) {
  grants = rawActionGrants(grants).map(normalizeGrant).filter(Boolean)
  return grants.some((grant) => grant === '*:*:*' || grant === permission)
}

export function getActionCapabilities(moduleKey, actions = [], grants) {
  const names = Array.isArray(actions) ? actions : Object.keys(actions || {})
  return names.reduce((result, action) => {
    result[action] = hasActionPermission(moduleKey, action, grants)
    return result
  }, {})
}

function queryValue(url, name) {
  const query = String(url || '').split('?')[1] || ''
  const pair = query.split('&').find((item) => item.startsWith(name + '='))
  return pair ? decodeURIComponent(pair.slice(name.length + 1)) : ''
}

export function routeModuleKey(url = '') {
  const path = String(url).split('?')[0].replace(/^pages\//, '/pages/')
  const queryModule = queryValue(url, 'module')
  if (queryModule) return queryModule
  const customPage = Object.entries(modules).find(([, config]) => config?.customPage?.split('?')[0] === path)
  if (customPage) return customPage[0]
  if (path === '/pages/user/index' || path === '/pages/user/detail' || path === '/pages/user/form') return 'userManage'
  if (path === '/pages/dept/index' || path === '/pages/dept/detail' || path === '/pages/dept/form') return 'deptManage'
  if (path.startsWith('/pages/member/')) return 'member'
  if (path === '/pages/operating-task/index') return '__operatingTask__'
  return ''
}

export function guardNavigation(options = {}, grants) {
  const moduleKey = routeModuleKey(options.url || '')
  if (!moduleKey) return true
  const allowed = moduleKey === '__operatingTask__'
    ? hasExactPermission('system:operatingTask:list', grants)
    : hasModulePermission(moduleKey, grants)
  if (allowed) return true
  uni.showToast({ title: '暂无该功能权限', icon: 'none' })
  options.fail?.({ errMsg: '当前操作没有权限' })
  return false
}

export function installNavigationGuard() {
  if (uni.__mpPermissionGuardInstalled) return
  uni.__mpPermissionGuardInstalled = true
  for (const method of ['navigateTo', 'redirectTo', 'reLaunch', 'switchTab']) {
    const original = uni[method]
    if (typeof original !== 'function') continue
    uni[method] = function guardedNavigation(options = {}) {
      if (!guardNavigation(options)) return { errMsg: '当前操作没有权限' }
      return original.call(this, options)
    }
  }
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

export function requireModuleOrActionPermission(moduleKey, moduleGrants, actionGrants) {
  if (hasModuleOrActionPermission(moduleKey, moduleGrants, actionGrants)) return true
  uni.showToast({ title: '暂无该功能权限', icon: 'none' })
  setTimeout(() => uni.navigateBack(), 500)
  return false
}

// 判断当前用户是否为管理员（拥有 system:user 权限）
export function isAdmin(grants) {
  grants = getPermissionGrants(grants)
  return grants.some((grant) => grant.startsWith('system:user') || grant === 'admin' || grant === 'userManage')
}
