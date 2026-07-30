export const dynamicRoutes: any[] = [
  {
    path: '/system/user-auth',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['system:user:edit'] },
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole.vue'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' },
      },
    ],
  },
  {
    path: '/system/role-auth',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['system:role:edit'] },
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser.vue'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' },
      },
    ],
  },
  {
    path: '/system/dict-data',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['system:dict:list'] },
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data.vue'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' },
      },
    ],
  },
  {
    path: '/monitor/job-log',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['monitor:job:list'] },
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/index.vue'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' },
      },
    ],
  },
  {
    path: '/tool/gen-edit',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['tool:gen:edit'] },
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/index.vue'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' },
      },
    ],
  },
  {
    path: '/workflow/designer',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['workflow:definition:list'] },
    children: [
      {
        path: ':mode(new|edit)/:definitionId?',
        component: () => import('@/views/workflow/designer/index.vue'),
        name: 'WorkflowDesigner',
        meta: { title: '流程设计器', activeMenu: '/workflow/definition' },
      },
    ],
  },
  {
    path: '/system/delegate',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['system:delegate:list'] },
    children: [
      {
        path: '',
        component: () => import('@/views/system/delegate/index.vue'),
        name: 'SysDelegate',
        meta: { title: '委托代理', activeMenu: '/system/user' },
      },
    ],
  },
  {
    path: '/workflow/timeout',
    component: () => import('@/layout/index.vue'),
    meta: { permissions: ['workflow:timeout:list'] },
    children: [
      {
        path: '',
        component: () => import('@/views/workflow/timeout/index.vue'),
        name: 'WorkflowTimeout',
        meta: { title: '超时配置', activeMenu: '/workflow/definition' },
      },
    ],
  },
  {
    path: '/workflow/field-permission',
    component: () => import('@/layout/index.vue'),
    meta: { permissions: ['workflow:fieldPermission:list'] },
    children: [
      {
        path: '',
        component: () => import('@/views/workflow/field-permission/index.vue'),
        name: 'WorkflowFieldPermission',
        meta: { title: '字段权限', activeMenu: '/workflow/definition' },
      },
    ],
  },
  {
    path: '/workflow/analytics',
    component: () => import('@/layout/index.vue'),
    meta: { permissions: ['workflow:analytics:list'] },
    children: [
      {
        path: '',
        component: () => import('@/views/workflow/analytics/index.vue'),
        name: 'WorkflowAnalytics',
        meta: { title: '流程分析', activeMenu: '/workflow/definition' },
      },
    ],
  },
  {
    path: '/workflow/monitor',
    component: () => import('@/layout/index.vue'),
    meta: { permissions: ['workflow:task:list'] },
    children: [
      {
        path: '',
        component: () => import('@/views/workflow/monitor/index.vue'),
        name: 'WorkflowMonitor',
        meta: { title: '流程监控', activeMenu: '/workflow/instance' },
      },
    ],
  },
  {
    path: '/workflow/intervene',
    component: () => import('@/layout/index.vue'),
    meta: { permissions: ['workflow:instance:intervene'] },
    children: [
      {
        path: '',
        component: () => import('@/views/workflow/intervene/index.vue'),
        name: 'WorkflowIntervene',
        meta: { title: '实例干预', activeMenu: '/workflow/instance' },
      },
    ],
  },
  {
    path: '/workflow/version',
    component: () => import('@/layout/index.vue'),
    meta: { permissions: ['workflow:definition:list'] },
    children: [
      {
        path: '',
        component: () => import('@/views/workflow/version/index.vue'),
        name: 'WorkflowVersion',
        meta: { title: '版本管理', activeMenu: '/workflow/definition' },
      },
    ],
  },
  {
    path: '/lowcode',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['lowcode:biz:list'] },
    children: [
      {
        path: ':bizCode',
        component: () => import('@/views/lowcode/SchemaList.vue'),
        name: 'LowcodeBiz',
        meta: { title: '低代码单据' },
      },
    ],
  },
  {
    path: '/lowcode/admin-edit',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['lowcode:meta:list'] },
    children: [
      {
        path: ':bizCode?',
        component: () => import('@/views/lowcode/admin/edit.vue'),
        name: 'LowcodeConfigEdit',
        meta: { title: '业务配置', activeMenu: '/lowcode/admin' },
      },
    ],
  },
  {
    path: '/lowcode/admin-template',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['lowcode:meta:list'] },
    children: [
      {
        path: '',
        component: () => import('@/views/lowcode/admin/template.vue'),
        name: 'LowcodeTemplateCenter',
        meta: { title: '模板中心', activeMenu: '/lowcode/admin' },
      },
    ],
  },
  {
    path: '/system/operatingTask',
    component: () => import('@/layout/index.vue'),
    meta: { permissions: ['system:operatingTask:list'] },
    children: [
      {
        path: '',
        component: () => import('@/views/system/operatingTask/index.vue'),
        name: 'SystemOperatingTask',
        meta: { title: '经营任务', icon: 'Bell' },
      },
    ],
  },
  {
    // Task 9: 库存盘点详情页（隐藏路由，从列表页跳转）
    path: '/finance/stocktake',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['finance:stocktake:query'] },
    children: [
      {
        path: 'detail/:id(\\d+)',
        component: () => import('@/views/finance/stocktake/detail.vue'),
        name: 'FinanceStocktakeDetail',
        meta: { title: '盘点详情', activeMenu: '/finance/report/stock/stocktake' },
      },
    ],
  },
  {
    // 期初库存详情页（隐藏路由，从列表页跳转）
    path: '/finance/stockInit',
    component: () => import('@/layout/index.vue'),
    meta: { hidden: true, permissions: ['finance:stockInit:query'] },
    children: [
      {
        path: 'detail/:id(\\d+)',
        component: () => import('@/views/finance/stockInit/detail.vue'),
        name: 'FinanceStockInitDetail',
        meta: { title: '期初库存详情', activeMenu: '/finance/stockInit/index' },
      },
    ],
  },
]
