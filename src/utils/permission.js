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
  const aliases = [moduleKey, config?.authKey, ...(config?.altAuthKeys || [])].filter(Boolean)
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

export function filterAuthorizedGroups(groups, grants, groupOrder) {
  // 入口展示只以“后端 /mp/userinfo -> storage.modules 下发的模块授权”为准。
  // 不再用 action 权限做兜底，保证契约（permission.test.mjs 里 'action permission cannot grant access to a module entry'）成立。
  // 如果 BY 用户需要看到更多模块，修复点在 MemMpController#getAccessibleModules：
  // 当 mem_mp_role_module 漏配时，会按 MpModuleCatalog 的 view 权限做兜底再合并返回。
  //
  // 顺序权威同样来自后端：
  // grants（storage.modules / this.modules）由 MemMpController#getAccessibleModules
  // 通过 MpModuleCatalogSupplier#sortModuleKeys 按 sys_mp_module_sort 排好序后返回，
  // 这里按 grants 的下标对每个 group.items 重排，保证 PC「功能模块调整」的顺序立即生效。
  //
  // groupOrder 由后端 MpModuleCatalogSupplier#sortedGroupNames（sys_mp_module_sort 的 @GROUP@ 哨兵行）
  // 下发，用于重排整个大分组（会员服务/会员运营/财务/系统/移动办公）的顺序。
  //
  // 实现刻意用了保守写法（普通 object 作下标表、不用 Map/Number.MAX_SAFE_INTEGER），
  // 避免微信开发者工具早期启动阶段的沙箱兼容性问题。
  var grantArr = Array.isArray(grants) ? grants : []
  var orderIdx = {}
  var i
  for (i = 0; i < grantArr.length; i++) {
    var k = grantArr[i]
    if (k !== undefined && k !== null) {
      orderIdx[String(k)] = i
    }
  }
  var hasOrder = grantArr.length > 0
  var res = []
  for (i = 0; i < groups.length; i++) {
    var group = groups[i]
    var src = (group && group.items) ? group.items : []
    var items = []
    for (var j = 0; j < src.length; j++) {
      if (hasModulePermission(src[j].key, grants)) items.push(src[j])
    }
    if (hasOrder && items.length > 1) {
      items.sort(function (a, b) {
        var ia = orderIdx[a.key]
        var ib = orderIdx[b.key]
        var ra = (ia === undefined || ia === null) ? 1000000000 : ia
        var rb = (ib === undefined || ib === null) ? 1000000000 : ib
        if (ra === rb) return 0
        return ra < rb ? -1 : 1
      })
    }
    if (items.length > 0) {
      var outGroup = {}
      for (var kk in group) {
        if (Object.prototype.hasOwnProperty.call(group, kk)) outGroup[kk] = group[kk]
      }
      outGroup.items = items
      res.push(outGroup)
    }
  }
  // 按 groupOrder 对整个分组重排（兼容老版本未传时保持原有顺序）
  var goArr = Array.isArray(groupOrder) ? groupOrder : []
  if (goArr.length > 0) {
    var groupIdx = {}
    for (i = 0; i < goArr.length; i++) {
      var gn = goArr[i]
      if (gn !== undefined && gn !== null) {
        groupIdx[String(gn)] = i
      }
    }
    var missingOrder = 1000000000
    res.sort(function (a, b) {
      var na = a ? String(a.name || '') : ''
      var nb = b ? String(b.name || '') : ''
      var ia = groupIdx[na]
      var ib = groupIdx[nb]
      var ra = (ia === undefined || ia === null) ? missingOrder : ia
      var rb = (ib === undefined || ib === null) ? missingOrder : ib
      if (ra === rb) return 0
      return ra < rb ? -1 : 1
    })
  }
  return res
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
