import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { ElMessage } from 'element-plus'
import { constantRoutes } from './constantRoutes'
import { getToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/api/request'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { useLockStore } from '@/stores/lock'
import { useSettingsStore } from '@/stores/settings'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register', '/open-platform', '/open-platform/docs', '/open-platform/apply', '/open-platform/debug', '/open-platform/samples']

const isWhiteList = (path: string) => {
  return whiteList.some((pattern) => isPathMatch(pattern, path))
}

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

function showBootstrapError(message: string, err: unknown) {
  console.error(message, err)
  ElMessage.error(message)
}

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title as string)
    const lockStore = useLockStore()
    if (to.path === '/login' || to.path === '/register') {
      await useUserStore().logout()
      next()
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      next()
    } else if (lockStore.isLock && to.path !== '/lock') {
      next({ path: '/lock' })
      NProgress.done()
    } else if (!lockStore.isLock && to.path === '/lock') {
      next({ path: '/' })
      NProgress.done()
    } else {
            const userStore = useUserStore()
            if (userStore.roles.length === 0) {
              isRelogin.show = true
              try {
                await userStore.getInfo()
                isRelogin.show = false
                const permissionStore = usePermissionStore()
                const { asyncRoutes, rewriteRoutes, skippedRoutes, recordAddRouteError } = await permissionStore.generateRoutes()
                const addRouteFails: any[] = []
                asyncRoutes.forEach((route: any) => {
                  try {
                    router.addRoute(route)
                  } catch (err) {
                    addRouteFails.push({ route: route?.path || route?.name || 'unknown', err: err instanceof Error ? err.message : String(err) })
                  }
                })
                rewriteRoutes.forEach((route: any) => {
                  try {
                    router.addRoute(route)
                  } catch (err) {
                    addRouteFails.push({ route: route?.path || route?.name || 'unknown', err: err instanceof Error ? err.message : String(err) })
                  }
                })
                if (recordAddRouteError && addRouteFails.length) {
                  recordAddRouteError(addRouteFails)
                }
                if (skippedRoutes?.length || addRouteFails.length) {
                  const msgs: string[] = []
                  if (skippedRoutes?.length) {
                    msgs.push(`已跳过 ${skippedRoutes.length} 条异常菜单（path/component 缺失等）`)
                  }
                  if (addRouteFails.length) {
                    msgs.push(`${addRouteFails.length} 条动态路由注册失败`)
                  }
                  ElMessage.warning({
                    message: `菜单加载存在异常，已自动跳过异常项：${msgs.join('；')}。详细请查看控制台或联系管理员。`,
                    duration: 8000,
                    showClose: true,
                  })
                }
                next({ ...to, replace: true })
              } catch (err) {
                isRelogin.show = false
                // BUG FIX: 菜单初始化异常不再强退 logout，让用户至少进入工作台看到可访问菜单；把错误以警告方式暴露。
                console.error('[router] 登录后菜单初始化失败', err)
                ElMessage.warning({
                  message: `登录成功，但初始化用户菜单失败：${err instanceof Error ? err.message : String(err)}。已跳过异常菜单，请尝试访问顶部菜单或联系管理员修复。`,
                  duration: 12000,
                  showClose: true,
                })
                next({ path: to.path === '/' || !to.path ? '/' : to.path, replace: true })
              }
            } else {
              next()
            }
          }
  } else {
    if (isWhiteList(to.path)) {
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
