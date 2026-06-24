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

const whiteList = ['/login', '/register']

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
          const { asyncRoutes, rewriteRoutes } = await permissionStore.generateRoutes()
          asyncRoutes.forEach((route: any) => {
            try {
              router.addRoute(route)
            } catch (err) {
              throw new Error(`动态路由注册失败：${route?.path || route?.name || '未知路由'}，${err instanceof Error ? err.message : err}`)
            }
          })
          rewriteRoutes.forEach((route: any) => {
            try {
              router.addRoute(route)
            } catch (err) {
              throw new Error(`菜单路由注册失败：${route?.path || route?.name || '未知路由'}，${err instanceof Error ? err.message : err}`)
            }
          })
          next({ ...to, replace: true })
        } catch (err) {
          isRelogin.show = false
          showBootstrapError('登录成功，但初始化用户菜单失败，请联系管理员检查菜单路由配置。', err)
          await userStore.logout()
          next({ path: '/' })
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
