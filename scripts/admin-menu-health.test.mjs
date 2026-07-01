import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

import {
  checkMenuHealth,
  componentToViewFile,
  parseMysqlTsv,
  parseOverviewMenuSql,
} from './admin-menu-health.mjs'

test('componentToViewFile maps route component to Vue view file', () => {
  const viewsRoot = path.join('junsong-ui-v3', 'src', 'views')

  assert.equal(
    componentToViewFile('system/overview/index', viewsRoot),
    path.join(viewsRoot, 'system', 'overview', 'index.vue'),
  )

  assert.equal(componentToViewFile('Layout', viewsRoot), null)
  assert.equal(componentToViewFile('', viewsRoot), null)
})

test('parseOverviewMenuSql extracts overview menu definitions and role grants', () => {
  const sql = `
    SELECT @nextMenuId, '系统概览', @systemRootId, 0, 'overview', 'system/overview/index', '', '',
      1, 0, 'C', '0', '0', 'system:overview:list', 'dashboard',
      'admin', NOW(), '', NULL, '系统管理概览入口';
    INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT 1, @systemOverviewId
    WHERE @systemOverviewId IS NOT NULL;
  `

  const parsed = parseOverviewMenuSql(sql)

  assert.deepEqual(parsed.menus, [
    {
      menuName: '系统概览',
      component: 'system/overview/index',
      perms: 'system:overview:list',
    },
  ])
  assert.deepEqual(parsed.roleGrants, [{ roleId: 1, variableName: '@systemOverviewId' }])
})

test('checkMenuHealth reports missing components, missing overview menus, and missing admin grants', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'admin-menu-health-'))
  const viewsRoot = path.join(tmp, 'views')
  fs.mkdirSync(path.join(viewsRoot, 'system', 'overview'), { recursive: true })
  fs.mkdirSync(path.join(viewsRoot, 'finance', 'overview'), { recursive: true })
  fs.writeFileSync(path.join(viewsRoot, 'system', 'overview', 'index.vue'), '<template />')
  fs.writeFileSync(path.join(viewsRoot, 'finance', 'overview', 'index.vue'), '<template />')

  const result = checkMenuHealth({
    viewsRoot,
    menus: [
      { menuId: 10, menuName: '系统概览', menuType: 'C', component: 'system/overview/index' },
      { menuId: 11, menuName: '财务概览', menuType: 'C', component: 'finance/overview/index' },
      { menuId: 12, menuName: '错误菜单', menuType: 'C', component: 'system/missing/index' },
    ],
    roleMenus: [{ roleId: 1, menuId: 10 }],
    requiredOverviewComponents: [
      'system/overview/index',
      'finance/overview/index',
      'member/overview/index',
    ],
    requiredRoleId: 1,
  })

  assert.deepEqual(
    result.findings.map((item) => item.code),
    ['MISSING_COMPONENT_FILE', 'MISSING_OVERVIEW_MENU', 'MISSING_ROLE_GRANT'],
  )
})

test('checkMenuHealth reports platform-only menus granted to non-admin roles', () => {
  const result = checkMenuHealth({
    viewsRoot: path.join('junsong-ui-v3', 'src', 'views'),
    menus: [
      { menuId: 20, menuName: '租户管理', menuType: 'C', component: 'system/tenant/index' },
      { menuId: 21, menuName: '开放平台', menuType: 'M', component: '' },
      { menuId: 22, menuName: '用户管理', menuType: 'C', component: 'system/user/index' },
      { menuId: 23, menuName: '应用管理', parentId: 21, menuType: 'C', component: 'open/app/index' },
    ],
    roleMenus: [
      { roleId: 1, roleName: '超级管理员', menuId: 20 },
      { roleId: 2, roleName: '普通租户管理员', menuId: 20 },
      { roleId: 3, roleName: '门店运营', menuId: 21 },
      { roleId: 2, roleName: '普通租户管理员', menuId: 22 },
      { roleId: 2, roleName: '普通租户管理员', menuId: 23 },
    ],
    requiredOverviewComponents: [],
    protectedMenuNames: ['租户管理', '开放平台'],
    platformRoleIds: [1],
  })

  assert.deepEqual(
    result.findings.map((item) => item.code),
    ['PROTECTED_MENU_GRANTED', 'PROTECTED_MENU_GRANTED', 'PROTECTED_MENU_GRANTED'],
  )
  assert.deepEqual(
    result.findings.map((item) => item.message),
    [
      '平台级菜单「租户管理」被授权给非平台角色「普通租户管理员」',
      '平台级菜单「开放平台」被授权给非平台角色「门店运营」',
      '平台级菜单「应用管理」被授权给非平台角色「普通租户管理员」',
    ],
  )
})

test('checkMenuHealth reports PROTECTED_MENU_GRANTED for sub-menus of both protected parents', () => {
  const result = checkMenuHealth({
    viewsRoot: path.join('junsong-ui-v3', 'src', 'views'),
    menus: [
      { menuId: 130, menuName: '租户管理', menuType: 'M', component: '' },
      { menuId: 131, menuName: '租户查询', parentId: 130, menuType: 'C', component: 'system/tenant/query' },
      { menuId: 132, menuName: '租户新增', parentId: 130, menuType: 'C', component: 'system/tenant/add' },
      { menuId: 2000, menuName: '开放平台', menuType: 'M', component: '' },
      { menuId: 2001, menuName: '应用管理', parentId: 2000, menuType: 'C', component: 'open/app/index' },
      { menuId: 2002, menuName: '应用查询', parentId: 2001, menuType: 'C', component: 'open/app/query' },
    ],
    roleMenus: [
      { roleId: 1, roleName: '超级管理员', menuId: 130 },
      { roleId: 1, roleName: '超级管理员', menuId: 2000 },
      { roleId: 2, roleName: '普通管理员', menuId: 131 },
      { roleId: 2, roleName: '普通管理员', menuId: 132 },
      { roleId: 3, roleName: '运营', menuId: 2001 },
      { roleId: 3, roleName: '运营', menuId: 2002 },
    ],
    requiredOverviewComponents: [],
    protectedMenuNames: ['租户管理', '开放平台'],
    platformRoleIds: [1],
  })

  const protectedFindings = result.findings.filter(f => f.code === 'PROTECTED_MENU_GRANTED')
  assert.equal(protectedFindings.length, 4, '应有 4 个 PROTECTED_MENU_GRANTED (租户查询+租户新增+应用管理+应用查询)')
  assert.deepEqual(
    protectedFindings.map(f => f.message),
    [
      '平台级菜单「租户查询」被授权给非平台角色「普通管理员」',
      '平台级菜单「租户新增」被授权给非平台角色「普通管理员」',
      '平台级菜单「应用管理」被授权给非平台角色「运营」',
      '平台级菜单「应用查询」被授权给非平台角色「运营」',
    ],
  )
})

test('parseMysqlTsv converts mysql batch output into records', () => {
  const text = [
    'menuId\tmenuName\tmenuType\tcomponent',
    '1\t系统概览\tC\tsystem/overview/index',
    '2\t财务概览\tC\tfinance/overview/index',
    '3\t系统管理\tM\tNULL',
    '',
  ].join('\n')

  assert.deepEqual(parseMysqlTsv(text), [
    { menuId: 1, menuName: '系统概览', menuType: 'C', component: 'system/overview/index' },
    { menuId: 2, menuName: '财务概览', menuType: 'C', component: 'finance/overview/index' },
    { menuId: 3, menuName: '系统管理', menuType: 'M', component: '' },
  ])
})
