import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/pages/workbench/index.vue', import.meta.url), 'utf8')

test('builds searchable groups from authorized modules with descriptions', () => {
  assert.match(source, /filterModuleGroups/)
  assert.match(source, /filterAuthorizedGroups\(groups,\s*this\.modules\)/)
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

test('filters member custom entries and uses true search results for scroll and empty state', () => {
  assert.match(source, /filteredMemberGrowthEntries\(\)\s*\{[\s\S]*filterEntries\(this\.memberGrowthEntries,\s*this\.searchQuery\)/)
  assert.match(source, /hasSearchResults\(\)\s*\{[\s\S]*this\.filteredGroups\.length[\s\S]*this\.filteredMemberGrowthEntries\.length/)
  assert.match(source, /<scroll-view[^>]*v-if="hasSearchResults"/)
  assert.match(source, /v-if="filteredMemberGrowthEntries\.length"/)
  assert.match(source, /v-for="entry in filteredMemberGrowthEntries"/)
  assert.match(source, /\{\{ filteredMemberGrowthEntries\.length \}\}\s*个功能/)
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
