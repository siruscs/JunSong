import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const IGNORED_COMPONENTS = new Set(['', '#', 'Layout', 'ParentView', 'InnerLink'])

const DEFAULT_REQUIRED_OVERVIEWS = [
  'system/overview/index',
  'finance/overview/index',
  'member/overview/index',
]

const DEFAULT_PROTECTED_MENU_NAMES = ['租户管理', '开放平台']
const DEFAULT_PLATFORM_ROLE_IDS = [1]

export function componentToViewFile(component, viewsRoot) {
  if (!component || IGNORED_COMPONENTS.has(component)) {
    return null
  }

  return path.join(viewsRoot, `${component}.vue`)
}

export function parseOverviewMenuSql(sql) {
  const menus = []
  const roleGrants = []
  const overviewVariables = new Map()

  const menuPattern =
    /SELECT\s+@nextMenuId,\s*'([^']+)',\s*@\w+,\s*0,\s*'overview',\s*'([^']+)',[\s\S]*?'([^']+:overview:list)'/g

  for (const match of sql.matchAll(menuPattern)) {
    menus.push({
      menuName: match[1],
      component: match[2],
      perms: match[3],
    })
  }

  const variablePattern = /SET\s+(@\w+OverviewId)\s*:=\s*\([\s\S]*?component\s*=\s*'([^']+)'[\s\S]*?\);/g
  for (const match of sql.matchAll(variablePattern)) {
    overviewVariables.set(match[1], match[2])
  }

  const grantPattern = /SELECT\s+(\d+),\s*(@\w+OverviewId)/g
  for (const match of sql.matchAll(grantPattern)) {
    const grant = {
      roleId: Number(match[1]),
      variableName: match[2],
    }
    const component = overviewVariables.get(match[2])
    if (component) {
      grant.component = component
    }
    roleGrants.push(grant)
  }

  return { menus, roleGrants }
}

export function parseMysqlTsv(text) {
  const lines = text.split(/\r?\n/).filter((line) => line.trim().length > 0)
  if (lines.length === 0) {
    return []
  }

  const headers = lines[0].split('\t')
  return lines.slice(1).map((line) => {
    const values = line.split('\t')
    const record = {}
    headers.forEach((header, index) => {
      const rawValue = values[index] ?? ''
      const value = rawValue === 'NULL' ? '' : rawValue
      record[header] = /^\d+$/.test(value) ? Number(value) : value
    })
    return record
  })
}

export function checkMenuHealth({
  viewsRoot,
  menus,
  roleMenus = [],
  requiredOverviewComponents = DEFAULT_REQUIRED_OVERVIEWS,
  requiredRoleId = 1,
  protectedMenuNames = DEFAULT_PROTECTED_MENU_NAMES,
  platformRoleIds = DEFAULT_PLATFORM_ROLE_IDS,
}) {
  const findings = []
  const menuByComponent = new Map()
  const menuById = new Map()
  const protectedMenuNameSet = new Set(protectedMenuNames)
  const platformRoleIdSet = new Set(platformRoleIds.map((item) => Number(item)))
  const protectedMenuIds = new Set()

  for (const menu of menus) {
    if (menu.menuId != null) {
      const menuId = Number(menu.menuId)
      menuById.set(menuId, menu)
      if (protectedMenuNameSet.has(menu.menuName)) {
        protectedMenuIds.add(menuId)
      }
    }

    if (menu.component) {
      menuByComponent.set(menu.component, menu)
    }

    const viewFile = componentToViewFile(menu.component, viewsRoot)
    if (viewFile && !fs.existsSync(viewFile)) {
      findings.push({
        code: 'MISSING_COMPONENT_FILE',
        message: `菜单「${menu.menuName}」组件文件不存在：${viewFile}`,
        menu,
      })
    }
  }

  let changed = true
  while (changed) {
    changed = false
    for (const menu of menus) {
      if (menu.menuId == null || menu.parentId == null) {
        continue
      }
      const menuId = Number(menu.menuId)
      const parentId = Number(menu.parentId)
      if (!protectedMenuIds.has(menuId) && protectedMenuIds.has(parentId)) {
        protectedMenuIds.add(menuId)
        changed = true
      }
    }
  }

  for (const roleMenu of roleMenus) {
    const roleId = Number(roleMenu.roleId)
    if (platformRoleIdSet.has(roleId)) {
      continue
    }

    const menu = menuById.get(Number(roleMenu.menuId))
    if (!menu || !protectedMenuIds.has(Number(menu.menuId))) {
      continue
    }

    findings.push({
      code: 'PROTECTED_MENU_GRANTED',
      message: `平台级菜单「${menu.menuName}」被授权给非平台角色「${roleMenu.roleName || roleId}」`,
      menu,
      roleMenu,
    })
  }

  const grantCheckMenus = []
  for (const component of requiredOverviewComponents) {
    const menu = menuByComponent.get(component)
    if (!menu) {
      findings.push({
        code: 'MISSING_OVERVIEW_MENU',
        message: `缺少概览菜单组件：${component}`,
        component,
      })
      continue
    }

    if (menu.menuId == null) {
      continue
    }

    grantCheckMenus.push(menu)
  }

  for (const menu of grantCheckMenus) {
    const hasGrant = roleMenus.some(
      (item) => Number(item.roleId) === Number(requiredRoleId) && Number(item.menuId) === Number(menu.menuId),
    )
    if (!hasGrant) {
      findings.push({
        code: 'MISSING_ROLE_GRANT',
        message: `角色 ${requiredRoleId} 缺少菜单「${menu.menuName}」授权`,
        menu,
      })
    }
  }

  return {
    ok: findings.length === 0,
    checkedMenus: menus.length,
    findings,
  }
}

function checkStaticOverviewSql({ rootDir }) {
  const sqlPath = path.join(rootDir, 'sql', 'admin_module_overview_menus.sql')
  const viewsRoot = path.join(rootDir, 'junsong-ui-v3', 'src', 'views')
  const sql = fs.readFileSync(sqlPath, 'utf8')
  const parsed = parseOverviewMenuSql(sql)

  const result = checkMenuHealth({
    viewsRoot,
    menus: parsed.menus,
    requiredOverviewComponents: DEFAULT_REQUIRED_OVERVIEWS,
  })

  const grantedComponents = new Set(parsed.roleGrants.filter((grant) => grant.roleId === 1).map((grant) => grant.component))
  for (const component of DEFAULT_REQUIRED_OVERVIEWS) {
    if (!grantedComponents.has(component)) {
      result.findings.push({
        code: 'MISSING_SQL_ROLE_GRANT',
        message: `SQL 缺少 admin 角色对 ${component} 的授权语句`,
        component,
      })
    }
  }

  result.ok = result.findings.length === 0
  return result
}

function queryDevMysql(sql) {
  return execFileSync(
    'docker',
    [
      'exec',
      'junsong-mysql',
      'mysql',
      '-uroot',
      '-proot_123',
      '--batch',
      '--raw',
      'junsong-cloud',
      '-e',
      sql,
    ],
    { encoding: 'utf8' },
  )
}

function checkDevDatabase({ rootDir }) {
  const viewsRoot = path.join(rootDir, 'junsong-ui-v3', 'src', 'views')
  const menus = parseMysqlTsv(
    queryDevMysql(`
      SELECT
        menu_id AS menuId,
        menu_name AS menuName,
        parent_id AS parentId,
        menu_type AS menuType,
        component AS component
      FROM sys_menu
      WHERE visible = '0'
        AND status = '0'
        AND menu_type IN ('C', 'M')
      ORDER BY menu_id;
    `),
  )
  const roleMenus = parseMysqlTsv(
    queryDevMysql(`
      SELECT
        rm.role_id AS roleId,
        r.role_name AS roleName,
        rm.menu_id AS menuId
      FROM sys_role_menu rm
      LEFT JOIN sys_role r ON r.role_id = rm.role_id
      ORDER BY rm.role_id, rm.menu_id;
    `),
  )

  return checkMenuHealth({
    viewsRoot,
    menus,
    roleMenus,
    requiredOverviewComponents: DEFAULT_REQUIRED_OVERVIEWS,
    requiredRoleId: 1,
  })
}

function printReport(result) {
  if (result.ok) {
    console.log(`Admin menu health passed. Checked ${result.checkedMenus} menus.`)
    return
  }

  console.error('Admin menu health failed:')
  for (const finding of result.findings) {
    console.error(`- [${finding.code}] ${finding.message}`)
  }
}

function isCliEntry() {
  return process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
}

if (isCliEntry()) {
  const result = process.argv.includes('--dev')
    ? checkDevDatabase({ rootDir: process.cwd() })
    : checkStaticOverviewSql({ rootDir: process.cwd() })
  printReport(result)
  if (!result.ok) {
    process.exit(1)
  }
}
