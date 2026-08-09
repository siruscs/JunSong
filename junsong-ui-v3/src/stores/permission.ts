import { defineStore } from 'pinia'
import { ref } from 'vue'
import { constantRoutes } from '@/router/constantRoutes'
import { dynamicRoutes } from '@/router/dynamicRoutes'
import { getRouters } from '@/api/menu'
import { useUserStore } from './user'
import Layout from '@/layout/index.vue'

const modules = import.meta.glob('/src/views/**/*.vue')

function toRouteName(input: string) {
  return String(input)
    .replace(/^\/+/, '')
    .replace(/\.vue$/, '')
    .split(/[\/_\-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('') || 'Route'
}

function normalizeRouteNames(routeList: any[], used = new Set<string>()) {
  routeList.forEach((route) => {
    const rawName = route.name != null ? String(route.name) : ''
    const baseName = rawName && !/^\d+$/.test(rawName)
      ? baseNameSanitize(rawName)
      : toRouteName(route.component || route.path || route.meta?.title || 'route')
    let routeName = baseName
    let index = 1
    while (used.has(routeName)) {
      routeName = `${baseName}${index++}`
    }
    route.name = routeName
    used.add(routeName)
    if (route.children && route.children.length) {
      normalizeRouteNames(route.children, used)
    }
  })
}

function baseNameSanitize(input: string): string {
  // 过滤掉菜单路由名称中可能导致 Vue Router 命名异常的字符/空白
  const cleaned = String(input || '').trim().replace(/[^\w\u4e00-\u9fa5$-]/g, '')
  return cleaned || 'Route'
}

function normalizeRouterPath(input: unknown, fallback?: string): string {
  const raw = String(input ?? '').trim()
  if (!raw) return fallback || '/placeholder-route'
  // 把多段连续斜杠合并为单段，首尾保留语义（外部链接原样返回即可，isExternalLink 会过滤）
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(raw)) return raw
  const collapsed = raw.replace(/\/{2,}/g, '/')
  return collapsed || (fallback || '/placeholder-route')
}

/**
 * 菜单路由清洗与 fail-soft 防御：
 *  - 过滤 null/undefined 节点
 *  - 按钮级 / path/component 全非法且无 children 的叶子节点自动跳过，不阻塞登录流程
 *  - 空 path / 重复 '//' 的路径自动兜底为 placeholder-route
 * 返回值：{ sanitized, skipped: [{title, path, reason}] }
 */
function sanitizeRouteTree(routeList: any[]): { sanitized: any[]; skipped: any[] } {
  const skipped: any[] = []
  const sanitized: any[] = []
  for (const raw of routeList || []) {
    if (raw == null) continue
    const route = { ...raw }
    const title = route.meta?.title ?? route.name ?? route.path ?? '(unknown)'
    // 路径归一：含 '//' / 空字符串都要修正，避免 Vue Router addRoute 抛 "invalid route path"
    route.path = normalizeRouterPath(route.path, undefined)
    if (route.meta) route.meta = { ...route.meta }
    // 递归 children
    if (Array.isArray(route.children) && route.children.length > 0) {
      const nested = sanitizeRouteTree(route.children)
      skipped.push(...nested.skipped)
      route.children = nested.sanitized
    }
    // 没有 component 且没有 children 且不是外链的叶子 → 跳过（Vue Router 无法注册）
    const componentMissing = !route.component && !route.children?.length
    const hasRedirect = !!route.redirect || route.redirect === 0
    if (componentMissing && !hasRedirect && !isExternalLink(route.path) && !isExternalLink(route.meta?.link)) {
      skipped.push({ title, path: route.path, component: route.component, reason: 'component 为空且没有 children 也没有 redirect，已跳过' })
      continue
    }
    sanitized.push(route)
  }
  return { sanitized, skipped }
}

function mergeRouteTrees(routeList: any[]): any[] {
  const merged = new Map<string, any>()
  routeList.forEach((route) => {
    const key = `${route.path || ''}__${route.component || ''}`
    const current = merged.get(key)
    if (!current) {
      const cloned = { ...route }
      if (route.children && route.children.length) {
        cloned.children = mergeRouteTrees(route.children)
      }
      merged.set(key, cloned)
      return
    }

    current.name = current.name || route.name
    current.component = current.component || route.component
    current.redirect = current.redirect || route.redirect
    current.meta = { ...(route.meta || {}), ...(current.meta || {}) }
    current.hidden = current.hidden ?? route.hidden
    current.alwaysShow = current.alwaysShow ?? route.alwaysShow
    current.permissions = current.permissions || route.permissions
    current.roles = current.roles || route.roles

    const currentChildren = current.children || []
    const nextChildren = route.children || []
    current.children = mergeRouteTrees([...currentChildren, ...nextChildren])
  })
  return Array.from(merged.values())
}

function normalizeMenuIcons(routeList: any[]) {
  routeList.forEach((route) => {
    const path = String(route.path || '').toLowerCase()
    const title = String(route.meta?.title || '')
    route.meta = { ...(route.meta || {}) }
    if (path === 'index' || path === '/index' || title === '首页') {
      route.meta.icon = 'dashboard'
    } else if (path === 'monitor' || path.startsWith('/monitor') || title === '系统监控') {
      route.meta.icon = 'system'
    }
    if (route.meta.icon) {
      route.meta.icon = route.meta.icon.toLowerCase()
    }
    if (route.children?.length) normalizeMenuIcons(route.children)
  })
}

function isExternalLink(path?: string) {
  return /^(https?:|mailto:|tel:)/.test(path || '')
}

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<any[]>([])
  const addRoutes = ref<any[]>([])
  const defaultRoutes = ref<any[]>([])
  const topbarRouters = ref<any[]>([])
  const sidebarRouters = ref<any[]>([])
  const skippedRoutes = ref<any[]>([])
  const addRouteErrors = ref<any[]>([])

  function recordSkipped(items: any[]) {
    if (!Array.isArray(items) || !items.length) return
    skippedRoutes.value = skippedRoutes.value.concat(items)
    console.warn('[permission] 菜单路由跳过项：', items)
  }

  function recordAddRouteError(errors: any[]) {
    if (!Array.isArray(errors) || !errors.length) return
    addRouteErrors.value = addRouteErrors.value.concat(errors)
    console.error('[permission] addRoute 失败项：', errors)
  }

  function filterAsyncRouter(asyncRouterMap: any[], lastRouter: any = false, type: boolean = false): any[] {
    return asyncRouterMap.filter((route) => {
      if (type && (isExternalLink(route.path) || isExternalLink(route.meta?.link))) {
        return false
      }
      if (type && route.children) {
        route.children = filterChildren(route.children)
      }
      if (!route.component && route.children && route.children.length) {
        route.component = Layout
      } else if (route.component) {
        if (route.component === 'Layout') {
          route.component = Layout
        } else if (route.component === 'ParentView') {
          route.component = () => import('@/components/ParentView/index.vue')
        } else if (route.component === 'InnerLink') {
          route.component = () => import('@/layout/components/InnerLink/index.vue')
        } else {
          route.component = loadView(route.component)
        }
      }
      if (route.children != null && route.children && route.children.length) {
        route.children = filterAsyncRouter(route.children, route, type)
      } else {
        delete route['children']
        delete route['redirect']
      }
      return true
    })
  }

  function filterChildren(childrenMap: any[], lastRouter: any = false): any[] {
    const children: any[] = []
    childrenMap.forEach((el) => {
      el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
      if (el.children && el.children.length && el.component === 'ParentView') {
        children.push(...filterChildren(el.children, el))
      } else {
        children.push(el)
      }
    })
    return children
  }

  function filterDynamicRoutes(routes: any[]): any[] {
    const userStore = useUserStore()
    const res: any[] = []
    routes.forEach((route) => {
      const permissions = route.permissions || route.meta?.permissions
      const roles = route.roles || route.meta?.roles
      if (permissions) {
        if (permissions.some((p: string) => checkPermi(p))) {
          res.push(route)
        }
      } else if (roles) {
        if (roles.some((r: string) => checkRole(r))) {
          res.push(route)
        }
      }
    })
    return res
  }

  function checkPermi(permission: string): boolean {
    const userStore = useUserStore()
    const all_permission = '*:*:*'
    const permissions = userStore.permissions
    return permissions.some((v) => all_permission === v || v === permission)
  }

  function checkRole(role: string): boolean {
    const userStore = useUserStore()
    const super_admin = 'admin'
    const roles = userStore.roles
    return roles.some((v) => super_admin === v || v === role)
  }

  async function generateRoutes() {
    const res: any = await getRouters()
    const sdata = mergeRouteTrees(JSON.parse(JSON.stringify(res.data)))
    const rdata = mergeRouteTrees(JSON.parse(JSON.stringify(res.data)))
    normalizeMenuIcons(sdata)
    normalizeMenuIcons(rdata)
    normalizeRouteNames(sdata)
    normalizeRouteNames(rdata)
    // BUG FIX: 空 component / 空 path / '//' 重复斜杠 的菜单，在前端侧先 sanitize；
    // 有缺陷的菜单跳过 + 留痕（skippedRoutes），保证 ADMIN 不因单条异常菜单导致登录完全失败。
    const sdataSanitized = sanitizeRouteTree(sdata)
    const rdataSanitized = sanitizeRouteTree(rdata)
    recordSkipped([...sdataSanitized.skipped, ...rdataSanitized.skipped])
    const sidebarRoutes = filterAsyncRouter(sdataSanitized.sanitized)
    const rewriteRoutes = filterAsyncRouter(rdataSanitized.sanitized, false, true)
    const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
    rewriteRoutes.push({ path: '/:pathMatch(.*)*', redirect: '/404', hidden: true })
    routes.value = constantRoutes.concat(rewriteRoutes)
    addRoutes.value = rewriteRoutes
    sidebarRouters.value = constantRoutes.concat(sidebarRoutes)
    defaultRoutes.value = sidebarRoutes
    topbarRouters.value = sidebarRoutes
    return { asyncRoutes, rewriteRoutes, skippedRoutes: skippedRoutes.value, recordAddRouteError }
  }

  return {
    routes, addRoutes, defaultRoutes, topbarRouters, sidebarRouters,
    skippedRoutes, addRouteErrors, generateRoutes, recordAddRouteError,
  }
})

export const loadView = (view: string) => {
  const normalizedView = view.replace(/^\/+/, '').replace(/\.vue$/, '')
  const normalizedKey = normalizedView.toLowerCase()
  const aliases: Record<string, string> = {
    'system/operlog/index': 'monitor/log/operlog/index',
    'system/operlog': 'monitor/log/operlog/index',
    'system/logininfor/index': 'monitor/log/logininfor/index',
    'system/logininfor': 'monitor/log/logininfor/index',
  }
  const moduleKeys = Object.keys(modules)
  const candidates = [
    aliases[normalizedView],
    aliases[normalizedKey],
    normalizedView,
    normalizedView.replace(/\/index$/, ''),
    `${normalizedView}/index`,
  ].filter(Boolean) as string[]
  const matchedKey = candidates
    .flatMap((candidate) => [`/src/views/${candidate}.vue`, `/src/views/${candidate}/index.vue`])
    .find((key) => modules[key])
    || moduleKeys.find((key) => key.toLowerCase() === `/src/views/${normalizedKey}.vue`)
    || moduleKeys.find((key) => key.toLowerCase() === `/src/views/${normalizedKey}/index.vue`)
  const res = matchedKey ? modules[matchedKey] : undefined
  if (!res) {
    console.warn(`[loadView] Component not found: "${view}"`)
    return () => import('@/views/error/404.vue')
  }
  return res
}
