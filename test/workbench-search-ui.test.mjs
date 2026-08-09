import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/pages/workbench/index.vue', import.meta.url), 'utf8')

test('builds searchable groups from authorized modules with descriptions', () => {
  assert.match(source, /filterModuleGroups/)
  assert.match(source, /filterAuthorizedGroups\(groups,\s*this\.modules(,\s*this\.groupOrder)?\)/)
  assert.match(source, /desc:\s*this\.getModuleDesc\(item\.key\)/)
  assert.match(source, /filterModuleGroups\(this\.authorizedGroups,\s*this\.searchQuery\)/)
})

test('renders native search, clear control, and recent modules without favorites', () => {
  assert.match(source, /<input[\s\S]*v-model="searchQuery"/)
  assert.match(source, /@confirm="[^\"]+"/)
  assert.match(source, /v-if="searchQuery"[\s\S]*@tap="clearSearch"/)
  assert.match(source, />最近</)
  assert.doesNotMatch(source, />常用</)
  assert.doesNotMatch(source, /favorite-button/)
  assert.doesNotMatch(source, /toggleModuleFavorite/)
})

test('sanitizes recent modules on show and records recent only after permission succeeds', () => {
  assert.doesNotMatch(source, /miniProgramFavorites/)
  assert.match(source, /miniProgramRecent/)
  assert.match(source, /this\.recent\s*=\s*sanitizeModuleKeys/)
  assert.match(source, /uni\.setStorageSync\('miniProgramRecent',\s*this\.recent\)/)

  const openModule = source.match(/openModule\(key\)\s*\{([\s\S]*?)\n\s*\},\n\s*\/\/ 跳转会员运营子页面/)?.[1]
  assert.ok(openModule, 'openModule method should remain present')
  const permissionIndex = openModule.indexOf("hasModulePermission(key, this.modules)")
  const recentIndex = openModule.indexOf('recordRecent(')
  assert.ok(permissionIndex >= 0, 'openModule should verify permission')
  assert.ok(recentIndex > permissionIndex, 'recent should be recorded only after permission passes')
})

test('keeps member custom entry navigation intact', () => {
  assert.match(source, /openMemberPage\(key\)/)
  assert.match(source, /uni\.navigateTo\(\{ url:\s*'\/pages\/member\/'\s*\+\s*key \}\)/)
  assert.match(source, /mod\s*&&\s*mod\.customPage/)
})

test('all module groups (including 会员运营) are rendered via filteredGroups, no duplicated hardcoded operation block', () => {
  // 所有业务分组都应走统一的 groups dictionary + filterAuthorizedGroups 渲染：
  // ① scroll-view 里只存在一套 v-for="group in filteredGroups"
  // ② 不再存在单独的"会员运营快捷入口"硬编码块（即不得使用 filteredMemberGrowthEntries 作为独立 section 的 v-if/v-for 条件）
  // ③ hasSearchResults 仅以 filteredGroups 长度为权威（加上经营任务是附加区块，不计入）
  assert.match(source, /<view class="section" v-for="group in filteredGroups" :key="group\.name">/)
  assert.match(source, /<text class="section-title">\{\{ group\.name \}\}<\/text>/)
  assert.doesNotMatch(source, /v-if="filteredMemberGrowthEntries\.length"/)
  assert.doesNotMatch(source, /v-for="entry in filteredMemberGrowthEntries"/)
  assert.match(source, /hasSearchResults\(\)\s*\{[\s\S]*this\.filteredGroups\.length/)
  assert.match(source, /<scroll-view[^>]*v-if="hasSearchResults"/)
})

test('shows search-specific empty copy and keeps no-permission copy for empty unsearched workbench', () => {
  assert.match(source, /searchQuery\s*\?\s*'未找到匹配功能'\s*:\s*'暂无可用功能'/)
  assert.match(source, /searchQuery[\s\S]*换个关键词试试/)
  assert.match(source, /账号已分配小程序模块权限/)
})

test('only renders the empty state when the filtered workbench has no results', () => {
  assert.match(source, /<view class="empty" v-if="!hasSearchResults">/)
  assert.doesNotMatch(source, /<view class="section" v-if="canViewOperatingTask && !searchQuery">[\s\S]*<\/view>\s*\n\s*<view class="empty" v-else>/)
})
