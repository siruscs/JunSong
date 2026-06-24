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
      ? rawName
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

function isExternalLink(path?: string) {
  return /^(https?:|mailto:|tel:)/.test(path || '')
}

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<any[]>([])
  const addRoutes = ref<any[]>([])
  const defaultRoutes = ref<any[]>([])
  const topbarRouters = ref<any[]>([])
  const sidebarRouters = ref<any[]>([])

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
    normalizeRouteNames(sdata)
    normalizeRouteNames(rdata)
    const sidebarRoutes = filterAsyncRouter(sdata)
    const rewriteRoutes = filterAsyncRouter(rdata, false, true)
    const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
    rewriteRoutes.push({ path: '/:pathMatch(.*)*', redirect: '/404', hidden: true })
    routes.value = constantRoutes.concat(rewriteRoutes)
    addRoutes.value = rewriteRoutes
    sidebarRouters.value = constantRoutes.concat(sidebarRoutes)
    defaultRoutes.value = sidebarRoutes
    topbarRouters.value = sidebarRoutes
    return { asyncRoutes, rewriteRoutes }
  }

  return { routes, addRoutes, defaultRoutes, topbarRouters, sidebarRouters, generateRoutes }
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
