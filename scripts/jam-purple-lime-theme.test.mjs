import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import test from 'node:test'

const root = join(import.meta.dirname, '..')
const read = (path) => readFileSync(join(root, path), 'utf8')

test('registers the jam purple lime theme with separate brand and action colors', () => {
  const source = read('junsong-ui-v3/src/utils/theme.ts')
  assert.match(source, /key:\s*'jam-purple-lime'/)
  assert.match(source, /name:\s*'酱紫青柠'/)
  assert.match(source, /primary:\s*'#3B1B8B'/)
  assert.match(source, /action:\s*'#9DE805'/)
  assert.match(source, /actionText:\s*'#24104F'/)
})

test('applies action tokens and the selected theme key to the root element', () => {
  const source = read('junsong-ui-v3/src/utils/theme.ts')
  assert.match(source, /--theme-action/)
  assert.match(source, /--theme-action-hover/)
  assert.match(source, /--theme-action-text/)
  assert.match(source, /dataset\.themePreset\s*=\s*preset\.key/)
})

test('keeps old presets compatible by falling back action colors to primary', () => {
  const source = read('junsong-ui-v3/src/utils/theme.ts')
  assert.match(source, /preset\.action\s*\|\|\s*preset\.primary/)
  assert.match(source, /preset\.actionText\s*\|\|\s*'#FFFFFF'/)
})

test('loads styles scoped to the jam purple lime root theme', () => {
  const index = read('junsong-ui-v3/src/assets/styles/index.scss')
  const theme = read('junsong-ui-v3/src/assets/styles/jam-purple-lime.scss')
  assert.match(index, /jam-purple-lime/)
  assert.match(theme, /data-theme-preset=['"]jam-purple-lime['"]/)
  assert.match(theme, /var\(--theme-action\)/)
  assert.doesNotMatch(theme, /#000000/i)
})

test('keeps the jam theme overrides visible and semantically safe', () => {
  const theme = read('junsong-ui-v3/src/assets/styles/jam-purple-lime.scss')
  assert.match(theme, /--jam-surface:\s*var\(--theme-app-surface,\s*#FFFFFF\)/)
  assert.match(theme, /\.el-card[\s\S]*?background:\s*var\(--theme-app-surface,\s*#FFFFFF\)/)
  assert.match(theme, /\.el-menu-item\.is-active::before[\s\S]*?display:\s*block\s*!important/)
  assert.match(theme, /\.el-button--primary:not\([\s\S]*?background:\s*var\(--theme-action\)\s*!important/)
  assert.match(theme, /\.el-button--success[\s\S]*?--el-button-bg-color:\s*var\(--el-color-success\)/)
  assert.match(theme, /\.el-button--warning[\s\S]*?--el-button-bg-color:\s*var\(--el-color-warning\)/)
  assert.match(theme, /\.el-button--danger[\s\S]*?--el-button-bg-color:\s*var\(--el-color-danger\)/)
  assert.match(theme, /\.el-pager li\.is-active[\s\S]*?background:\s*var\(--theme-primary\)/)
})

test('shows action color in the new theme swatch while preserving old presets', () => {
  const navbar = read('junsong-ui-v3/src/layout/components/Navbar.vue')
  assert.match(navbar, /theme\.action\s*\|\|\s*theme\.primaryLight/)
})

test('keeps solid primary toolbar actions lime despite global important rules', () => {
  const theme = read('junsong-ui-v3/src/assets/styles/jam-purple-lime.scss')
  assert.match(theme, /\.mb8 \.el-button--primary:not\(\.is-plain\)[\s\S]*?background:\s*var\(--theme-action\)\s*!important/)
  assert.match(theme, /\.table-toolbar \.toolbar-left \.el-button--primary:not\(\.is-plain\)[\s\S]*?background:\s*var\(--theme-action\)\s*!important/)
})

test('gives solid primary actions a visible purple keyboard focus ring', () => {
  const theme = read('junsong-ui-v3/src/assets/styles/jam-purple-lime.scss')
  assert.match(theme, /\.el-button--primary:not\([\s\S]*?&:focus-visible[\s\S]*?(?:outline|box-shadow):[^;]*var\(--theme-primary\)/)
  assert.match(theme, /\.mb8 \.el-button--primary:not\(\.is-plain\)[\s\S]*?&:focus-visible[\s\S]*?(?:outline|box-shadow):[^;]*var\(--theme-primary\)/)
  assert.match(theme, /\.table-toolbar \.toolbar-left \.el-button--primary:not\(\.is-plain\)[\s\S]*?&:focus-visible[\s\S]*?(?:outline|box-shadow):[^;]*var\(--theme-primary\)/)
})
