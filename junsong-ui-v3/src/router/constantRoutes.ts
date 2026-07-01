import Layout from '@/layout/index.vue'

export const constantRoutes: any[] = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect.vue'),
      },
    ],
  },
  {
    path: '/login',
    component: () => import('@/views/login.vue'),
    hidden: true,
  },
  {
    path: '/register',
    component: () => import('@/views/register.vue'),
    hidden: true,
  },
  {
    path: '/open-platform',
    component: () => import('@/views/open/portal/index.vue'),
    hidden: true,
    meta: { title: '开放平台' },
  },
  {
    path: '/open-platform/docs',
    component: () => import('@/views/open/developer/index.vue'),
    hidden: true,
    meta: { title: '开发文档' },
  },
  {
    path: '/open-platform/apply',
    component: () => import('@/views/open/apply/index.vue'),
    hidden: true,
    meta: { title: '申请接入' },
  },
  {
    path: '/open-platform/debug',
    component: () => import('@/views/open/debug/index.vue'),
    hidden: true,
    meta: { title: '签名调试' },
  },
  {
    path: '/open-platform/samples',
    component: () => import('@/views/open/samples/index.vue'),
    hidden: true,
    meta: { title: '接入样例' },
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    hidden: true,
  },
  {
    path: '/401',
    component: () => import('@/views/error/401.vue'),
    hidden: true,
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: 'index',
        component: () => import('@/views/index.vue'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true },
      },
    ],
  },
  {
    path: '/lock',
    component: () => import('@/views/lock.vue'),
    hidden: true,
    meta: { title: '锁定屏幕' },
  },
  {
    path: '/member',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'segment',
        component: () => import('@/views/member/segment/index.vue'),
        name: 'MemberSegment',
        meta: { title: '会员分层清单', icon: 'peoples' },
      },
    ],
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/user/profile/index.vue'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' },
      },
      {
        path: 'notification',
        component: () => import('@/views/system/notification/index.vue'),
        name: 'Notification',
        meta: { title: '通知中心', icon: 'message' },
      },
    ],
  },
]
