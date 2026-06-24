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
]
