# 小程序库存工作台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以核算周期首页为视觉基准，将库存查询、库存流水、库存调整统一为适合店长快速操作的库存工作台视觉和交互层级。

**Architecture:** 仅在小程序现有 Vue 页面和 scoped CSS 内做局部重构，复用核算周期首页的 header、KPI、section-card 视觉语言以及现有请求、权限、部门上下文、字典缓存和 `StateView`。库存查询补齐统一状态组件，流水调整筛选与卡片层级，库存调整统一列表卡片和底部操作区，不修改业务接口或状态机。

**Tech Stack:** uni-app、Vue 3、微信小程序原生视图组件、现有 `StateView` 和 CSS。

---

### Task 1: 建立库存工作台视觉回归基线

**Files:**
- Modify: `junsong-miniprogram/test/inventory-workbench-ui.test.mjs`

- [x] **Step 1: 编写页面结构断言**

断言三个页面包含统一工作台标题、部门提示、摘要/筛选/状态区域，以及库存查询使用 `StateView`。

- [x] **Step 2: 运行基线测试确认失败**

Run: `cd junsong-miniprogram && node --test test/inventory-workbench-ui.test.mjs`

Expected: FAIL，现有库存查询尚未使用 `StateView`，三个页面也没有统一的工作台结构标识。

### Task 2: 优化库存查询页

**Files:**
- Modify: `junsong-miniprogram/src/pages/stock/index.vue`

- [x] **Step 1: 保持现有数据口径，补齐工作台结构**

保留 `getStockValueReport` 和当前部门过滤，只调整模板为统一头部、摘要指标、商品卡片和 `StateView` 四态；新增商品数量统计，库存金额与数量使用 tabular 数字样式。

- [x] **Step 2: 统一 scoped CSS**

使用与库存调整页一致的浅灰背景、蓝色头部、分区卡片、间距、圆角和底部安全区；不引入新依赖。

- [x] **Step 3: 运行库存查询相关回归**

Run: `cd junsong-miniprogram && node --test test/inventory-workbench-ui.test.mjs test/stock-ledger-pagination.test.mjs`

Expected: 页面结构断言通过，库存流水既有分页测试通过。

### Task 3: 优化库存流水页

**Files:**
- Modify: `junsong-miniprogram/src/pages/stock-ledger/index.vue`

- [x] **Step 1: 调整筛选区结构**

保留类型、开始日期、结束日期和查询逻辑，将筛选区拆成类型选择行与日期查询行，避免窄屏横向溢出。

- [x] **Step 2: 调整流水卡片信息层级**

保留变动类型、数量、余额、时间和备注，增加入库/出库视觉标识类名，统一卡片头、元信息和来源行。

- [x] **Step 3: 运行流水回归**

Run: `cd junsong-miniprogram && node --test test/inventory-workbench-ui.test.mjs test/stock-ledger-pagination.test.mjs`

Expected: UI 结构和分页、筛选、部门切换相关测试通过。

### Task 4: 优化库存调整页

**Files:**
- Modify: `junsong-miniprogram/src/pages/stock-adjustment/index.vue`

- [x] **Step 1: 调整单列表统一摘要布局**

保留所有状态动作和权限判断，统一单号、状态、类型、日期、部门和操作链接的层级与间距。

- [x] **Step 2: 调整新增/编辑抽屉**

保留字段、校验、草稿、幂等和提交逻辑，统一分区标题、商品明细卡片、方向选择、输入框、备注和底部提交按钮。

- [x] **Step 3: 调整详情抽屉和底部新增操作**

保留详情展示与权限控制，确保底部主按钮避让安全区且不覆盖滚动内容。

- [x] **Step 4: 运行库存调整回归**

Run: `cd junsong-miniprogram && node --test test/inventory-workbench-ui.test.mjs test/stock-adjustment-v11.test.mjs test/form-v11.test.mjs`

Expected: 结构断言、多商品、权限动作和表单回归通过。

### Task 5: 构建与变更检查

**Files:**
- Modify: `docs/superpowers/specs/2026-08-04-miniprogram-inventory-workbench-design.zh-CN.md`
- Modify: `docs/superpowers/plans/2026-08-04-miniprogram-inventory-workbench-plan.zh-CN.md`

- [x] **Step 1: 运行定向测试**

Run: `cd junsong-miniprogram && node --test test/inventory-workbench-ui.test.mjs test/stock-ledger-pagination.test.mjs test/stock-adjustment-v11.test.mjs test/form-v11.test.mjs`

Expected: 所有本次相关测试通过。

- [x] **Step 2: 构建微信小程序**

Run: `cd junsong-miniprogram && npm run build:mp-weixin`

Expected: 构建成功；仅保留源代码和文档变更，不提交生成的 `dist` 变化。

- [x] **Step 3: 更新跟踪文档并检查差异**

记录库存工作台 UI 完成、定向测试和构建结果，执行 `git diff --check`，确认没有接口、数据库和权限文件变更。
