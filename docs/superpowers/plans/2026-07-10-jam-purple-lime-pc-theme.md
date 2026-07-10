# 酱紫青柠 PC 主题实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Vue 3 PC 管理端新增一套可切换、可持久化且不影响旧主题的“酱紫青柠”浅色办公主题。

**Architecture:** 继续使用现有 `ThemePreset` 和根 CSS 变量体系，在主题模型中增加可选动作色字段，并由 `applyTheme()` 写入语义令牌和根主题属性。新建独立 SCSS 文件，仅在 `data-theme-preset="jam-purple-lime"` 下覆盖侧边栏、顶栏和 Element Plus 视觉，旧主题通过字段回退保持原样。

**Tech Stack:** Vue 3、TypeScript、Pinia、Element Plus、SCSS、Node.js `node:test`、Vite

---

## 文件结构

- 创建 `scripts/jam-purple-lime-theme.test.mjs`：主题注册、令牌、样式隔离和旧主题兼容契约。
- 修改 `junsong-ui-v3/src/utils/theme.ts`：扩展主题模型、注册新预设、应用动作色和根主题属性。
- 创建 `junsong-ui-v3/src/assets/styles/jam-purple-lime.scss`：只属于新主题的低圆角、硬边线、青柠动作样式。
- 修改 `junsong-ui-v3/src/assets/styles/index.scss`：加载新主题样式文件。
- 不修改业务页面、路由、表单字段和表格结构。

### Task 1: 建立主题契约测试

**Files:**
- Create: `scripts/jam-purple-lime-theme.test.mjs`
- Read: `junsong-ui-v3/src/utils/theme.ts`
- Read: `junsong-ui-v3/src/assets/styles/index.scss`

- [ ] **Step 1: 写入失败的静态契约测试**

```js
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
  assert.doesNotMatch(theme, /#000000|#ffffff\b/i)
})
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `node --test scripts/jam-purple-lime-theme.test.mjs`

Expected: FAIL，提示找不到 `jam-purple-lime`、动作色令牌和主题 SCSS。

- [ ] **Step 3: 提交测试**

```bash
git add scripts/jam-purple-lime-theme.test.mjs
git commit -m "test: define jam purple lime theme contract"
```

### Task 2: 扩展主题模型并注册主题

**Files:**
- Modify: `junsong-ui-v3/src/utils/theme.ts`
- Test: `scripts/jam-purple-lime-theme.test.mjs`

- [ ] **Step 1: 对 `ThemePreset` 和 `applyTheme` 执行 GitNexus upstream impact**

Run GitNexus:

```text
impact({ target: "ThemePreset", direction: "upstream", file_path: "junsong-ui-v3/src/utils/theme.ts" })
impact({ target: "applyTheme", direction: "upstream", file_path: "junsong-ui-v3/src/utils/theme.ts" })
```

Expected: 记录直接调用者、受影响流程和风险等级。若为 HIGH 或 CRITICAL，先向用户报告再继续。

- [ ] **Step 2: 增加可选动作令牌字段**

在 `ThemePreset` 中加入：

```ts
  action?: string
  actionHover?: string
  actionText?: string
  actionRgb?: string
  radiusScale?: 'soft' | 'compact'
```

- [ ] **Step 3: 在预设数组中增加酱紫青柠主题**

```ts
  {
    key: 'jam-purple-lime',
    name: '酱紫青柠',
    primary: '#3B1B8B',
    primaryLight: '#7355C6',
    primaryDark: '#24104F',
    primaryRgb: '59, 27, 139',
    action: '#9DE805',
    actionHover: '#86C900',
    actionText: '#24104F',
    actionRgb: '157, 232, 5',
    radiusScale: 'compact',
    sidebarBg: '#24104F',
    sidebarSubmenuBg: '#2D155F',
    sidebarActiveStart: 'rgba(59, 27, 139, 0.82)',
    sidebarActiveEnd: 'rgba(59, 27, 139, 0.82)',
    sidebarActiveText: '#9DE805',
    sidebarHoverText: '#B8F34B',
    sidebarText: '#C9C0DD',
    sidebarHoverBg: 'rgba(115, 85, 198, 0.18)',
    sidebarSubmenuActiveBg: 'rgba(157, 232, 5, 0.1)',
    sidebarActiveShadow: 'none',
    sidebarLogoText: '#9DE805',
    appBg: '#F7F7F8',
    appBgSoft: '#F0EFF3',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#FAFAFB',
    appBorder: '#DDD9E5',
    appText: '#211C2B',
    appMuted: '#6C6675',
    appHeaderBg: 'rgba(255, 255, 255, 0.94)',
    appHeaderShadow: '0 6px 18px rgba(36, 16, 79, 0.06)',
    loginBgStart: '#190B38',
    loginBgEnd: '#3B1B8B',
    loginPanelStart: '#24104F',
    loginPanelMid: '#3B1B8B',
    loginPanelEnd: '#9DE805',
  },
```

- [ ] **Step 4: 在 `applyTheme()` 写入带回退的动作令牌**

```ts
  const action = preset.action || preset.primary
  const actionHover = preset.actionHover || preset.primaryDark
  const actionText = preset.actionText || '#FFFFFF'
  const actionRgb = preset.actionRgb || preset.primaryRgb

  root.dataset.themePreset = preset.key
  root.style.setProperty('--theme-action', action)
  root.style.setProperty('--theme-action-hover', actionHover)
  root.style.setProperty('--theme-action-text', actionText)
  root.style.setProperty('--theme-action-rgb', actionRgb)
  root.style.setProperty('--theme-radius-control', preset.radiusScale === 'compact' ? '6px' : '10px')
  root.style.setProperty('--theme-radius-surface', preset.radiusScale === 'compact' ? '5px' : '12px')
```

继续让 `--el-color-primary` 指向 `preset.primary`，不要改为青柠。

- [ ] **Step 5: 运行主题契约测试**

Run: `node --test scripts/jam-purple-lime-theme.test.mjs`

Expected: 主题模型相关断言通过，SCSS 文件相关断言仍失败。

- [ ] **Step 6: 提交主题模型**

```bash
git add junsong-ui-v3/src/utils/theme.ts scripts/jam-purple-lime-theme.test.mjs
git commit -m "feat: register jam purple lime theme tokens"
```

### Task 3: 实现隔离的全局主题样式

**Files:**
- Create: `junsong-ui-v3/src/assets/styles/jam-purple-lime.scss`
- Modify: `junsong-ui-v3/src/assets/styles/index.scss`
- Test: `scripts/jam-purple-lime-theme.test.mjs`

- [ ] **Step 1: 对全局样式入口执行 GitNexus impact**

Run GitNexus:

```text
impact({ target: "junsong-ui-v3/src/assets/styles/index.scss", direction: "upstream" })
```

Expected: 记录全局样式消费者和风险。若 GitNexus 未索引 SCSS，记录 UNKNOWN 并继续人工验证。

- [ ] **Step 2: 创建仅对新主题生效的样式文件**

文件必须以根选择器包裹全部规则：

```scss
html[data-theme-preset='jam-purple-lime'] {
  .navbar {
    border-bottom-color: var(--theme-app-border);
    box-shadow: var(--theme-app-header-shadow);
  }

  .dept-selector .dept-display,
  .toolbar-icon-button,
  .right-menu-item {
    border-radius: var(--theme-radius-control) !important;
  }

  .sidebar-container {
    box-shadow: 4px 0 18px rgba(36, 16, 79, 0.16) !important;

    .el-menu-item,
    .el-sub-menu__title {
      border-radius: var(--theme-radius-control) !important;
    }

    .el-menu-item.is-active {
      color: var(--theme-action) !important;
      background: rgba(var(--theme-primary-rgb), 0.72) !important;
      box-shadow: none !important;

      &::before {
        display: block !important;
        width: 3px;
        background: var(--theme-action);
      }
    }
  }

  .el-button--primary:not(.is-plain):not(.is-link):not(.is-text) {
    color: var(--theme-action-text) !important;
    border-color: var(--theme-action) !important;
    background: var(--theme-action) !important;
    box-shadow: none !important;

    &:hover,
    &:focus {
      color: var(--theme-action-text) !important;
      border-color: var(--theme-action-hover) !important;
      background: var(--theme-action-hover) !important;
    }
  }

  .el-card,
  .el-dialog,
  .el-message-box,
  .el-table,
  .search-form,
  .table-toolbar {
    border-color: var(--theme-app-border) !important;
    border-radius: var(--theme-radius-surface) !important;
    box-shadow: none;
  }

  .el-input__wrapper,
  .el-select__wrapper,
  .el-textarea__inner,
  .el-pagination button,
  .el-pagination .el-pager li {
    border-radius: var(--theme-radius-control) !important;
  }

  .el-table th.el-table__cell {
    background: var(--theme-app-bg-soft) !important;
  }

  .el-table__body tr:hover > td.el-table__cell {
    background: rgba(var(--theme-primary-rgb), 0.045) !important;
  }
}

@media (prefers-reduced-motion: reduce) {
  html[data-theme-preset='jam-purple-lime'] * {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
```

同一文件还必须包含以下明确规则，并保持全部位于同一根选择器内：

- 青柠主按钮的 disabled、active 和 loading 状态。
- 分页当前页使用酱紫，不使用青柠底白字。
- 非语义青柠标签使用 `#EEFACF` 背景和深酱紫文字。
- 保持 `.el-button--success`、`.el-button--warning`、`.el-button--danger` 的原始语义色，不允许覆盖成品牌色。
- 卡片圆角最大 6px，交互控件圆角最大 8px。

- [ ] **Step 3: 在样式入口导入新文件**

在 `junsong-ui-v3/src/assets/styles/index.scss` 的主题相关导入附近加入：

```scss
@use './jam-purple-lime.scss';
```

- [ ] **Step 4: 运行主题契约测试并确认 GREEN**

Run: `node --test scripts/jam-purple-lime-theme.test.mjs`

Expected: PASS，4 个测试全部通过。

- [ ] **Step 5: 运行前端生产构建**

Run: `npm run build`

Working directory: `junsong-ui-v3`

Expected: `vue-tsc -b && vite build` 成功，无 TypeScript 或 Sass 错误。

- [ ] **Step 6: 提交主题样式**

```bash
git add junsong-ui-v3/src/assets/styles/jam-purple-lime.scss junsong-ui-v3/src/assets/styles/index.scss scripts/jam-purple-lime-theme.test.mjs
git commit -m "feat: style jam purple lime pc theme"
```

### Task 4: 真实页面视觉与交互验收

**Files:**
- Verify: `junsong-ui-v3/src/utils/theme.ts`
- Verify: `junsong-ui-v3/src/assets/styles/jam-purple-lime.scss`
- Verify: representative PC routes in the running app

- [ ] **Step 1: 启动本地前端**

Run: `npm run dev -- --host 127.0.0.1`

Working directory: `junsong-ui-v3`

Expected: Vite 返回本地访问地址。

- [ ] **Step 2: 在浏览器选择“酱紫青柠”主题**

操作：登录后在顶栏主题切换器选择 `酱紫青柠`，刷新页面。

Expected: 刷新后主题仍为 `jam-purple-lime`，浏览器存储中的 `theme-preset` 值正确。

- [ ] **Step 3: 检查代表性页面**

至少检查：

- `/finance/sale`：筛选表单、主按钮、语义按钮、表格、分页。
- `/finance/overview`：指标卡片、图表容器、关键数字。
- `/system/user`：树、弹窗、表单、分页。
- `/member/index`：标签、操作栏、状态色。

Expected:

- 青柠主按钮使用深酱紫文字且不换行。
- 侧边栏当前项为青柠文字和左侧强调线。
- 成功、警告、危险仍使用各自语义色。
- 表格列宽、固定列、筛选和弹窗行为未变化。
- 1280px 与 1440px 宽度均无横向溢出。

- [ ] **Step 4: 验证键盘与降低动效**

操作：用 Tab 遍历顶栏、筛选区和主操作；在浏览器中模拟 `prefers-reduced-motion: reduce`。

Expected: 焦点环清楚可见，所有操作可用键盘完成，降低动效时无明显位移动画。

- [ ] **Step 5: 运行完整回归命令**

```bash
node --test scripts/jam-purple-lime-theme.test.mjs
cd junsong-ui-v3 && npm run build
```

Expected: 契约测试和生产构建全部通过。

- [ ] **Step 6: 运行 GitNexus 变更检测**

Run GitNexus:

```text
detect_changes({ scope: "unstaged", repo: "JunSong" })
```

Expected: 本次变更仅涉及主题模型、全局主题样式和测试；若报告出现业务流程变化，暂停并检查非预期修改。

- [ ] **Step 7: 请求独立代码审查**

审查重点：旧主题兼容、CSS 选择器隔离、Element Plus 语义色、主按钮对比度、低圆角一致性、构建产物。

- [ ] **Step 8: 提交视觉验收修正**

若验收产生修正：

```bash
git add junsong-ui-v3/src/utils/theme.ts junsong-ui-v3/src/assets/styles/jam-purple-lime.scss junsong-ui-v3/src/assets/styles/index.scss scripts/jam-purple-lime-theme.test.mjs
git commit -m "fix: polish jam purple lime theme states"
```

若没有修正，不创建空提交。

## 预检约束

- 页面保持单一浅色主题，不做分区反色。
- 酱紫是品牌与焦点色，青柠是唯一动作点缀色。
- 青柠按钮始终配深酱紫文字，禁止白字。
- 卡片圆角不超过 6px，控件圆角不超过 8px。
- 不增加装饰性状态点、外发光、无意义渐变或滚动动画。
- 不修改业务信息架构、菜单标签、表单字段、表格结构和权限逻辑。
- 不引入新组件库或图标库。
