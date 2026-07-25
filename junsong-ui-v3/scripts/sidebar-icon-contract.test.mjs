import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(new URL('..', import.meta.url).pathname)
const read = (file) => readFileSync(resolve(root, file), 'utf8')

const homeRoutes = read('src/router/constantRoutes.ts')
assert.match(homeRoutes, /title:\s*'首页',\s*icon:\s*'dashboard'/)

for (const icon of ['dashboard', 'monitor', 'system']) {
  const file = `src/assets/icons/svg/${icon}.svg`
  assert.ok(existsSync(resolve(root, file)), `${icon}.svg must exist`)
  assert.match(read(file), /viewBox=/, `${icon}.svg must define viewBox for sprite rendering`)
  assert.doesNotMatch(read(file), /chrome-extension|@font-face/, `${icon}.svg must not reference chrome-extension or @font-face`)
}

const sidebarItem = read('src/layout/components/Sidebar/SidebarItem.vue')
const permissionStore = read('src/stores/permission.ts')
assert.match(sidebarItem, /<svg-icon[^>]+:icon-class=/)
assert.match(sidebarItem, /item\.meta\?\.icon/)
assert.match(sidebarItem, /resolveMenuIcon\(/)
assert.match(sidebarItem, /title === '首页'.*return 'dashboard'/s)
assert.match(sidebarItem, /title === '系统监控'.*return 'system'/s)
assert.match(sidebarItem, /path.*monitor.*return 'system'/s)
assert.match(sidebarItem, /fallbackIcon\s*\|\|\s*'system'/)
assert.match(sidebarItem, /\.toLowerCase\(\)/)
assert.doesNotMatch(sidebarItem, /<svg-icon v-if=/)
assert.match(permissionStore, /normalizeMenuIcons\(/)
assert.match(permissionStore, /dashboard/)
assert.match(permissionStore, /system/)

const sidebarStyles = read('src/assets/styles/sidebar.scss')
assert.match(sidebarStyles, /\.el-menu-item[\s\S]*width: 56px !important/)
assert.match(sidebarStyles, /\.el-menu-item[\s\S]*\.svg-icon[\s\S]*width: 24px !important/)

console.log('sidebar icon contract: 4 assertions passed')
