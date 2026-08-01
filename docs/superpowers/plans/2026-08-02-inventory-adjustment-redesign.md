# 库存调整重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将期初库存重构为可配置类型的库存调整，并修复 PROD 小程序权限与财务总览统计口径。

**Architecture:** 后端保留现有批次审批/过账框架，新增调整类型和方向的服务端规则解析，最终写入真实库存流水类型；PC 与小程序共享服务端模块能力，PC 提供调整操作，小程序只读展示。财务总览直接复用首页当前核算周期统计数据源。

**Tech Stack:** Java 17/Spring/MyBatis、Vue 3/TypeScript、uni-app/Vue 3、MySQL、Node test、Maven、Vite。

---

### Task 1: 建立失败验收测试与根因诊断

**Files:**
- Modify: `scripts/finance-stock-miniprogram-repair.test.mjs`
- Test: `scripts/finance-stock-miniprogram-repair.test.mjs`

- [ ] **Step 1: 写失败测试**：增加对 `stockAdjustment` 模块、调整类型字典、`adjustmentDate`、单位成本精度、非 `STOCK_INIT` 流水映射和首页统计组件复用的断言。
- [ ] **Step 2: 运行专项测试确认失败**：运行 `node scripts/finance-stock-miniprogram-repair.test.mjs`，确认失败原因来自尚未实现的字段、模块或页面。
- [ ] **Step 3: 完成 GitNexus 影响分析**：对将要修改的服务方法、过账方法和模块能力方法执行上游影响分析，记录调用方与风险。

### Task 2: 后端字典、请求模型和库存流水映射

**Files:**
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockInitCreateRequest.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StockInitItemInput.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinStockInitServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinStockLedgerMapper.xml`
- Create: `sql/finance_stock_adjustment_dict.sql`
- Test: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinStockInitServiceImplTest.java`

- [ ] **Step 1: 写服务端失败测试**：覆盖六种类型、正数数量、单位成本两位、数量三位、`OTHER` 必须有方向，以及过账写入类型编码而非 `STOCK_INIT`。
- [ ] **Step 2: 运行目标测试确认失败**：运行 `mvn -pl junsong-modules/junsong-finance -Dtest=FinStockInitServiceImplTest test`，记录失败断言。
- [ ] **Step 3: 增加可重复字典 SQL**：创建六种类型及方向配置，带 `utf8mb4`、租户安全和核对查询。
- [ ] **Step 4: 实现服务端规则**：服务端解析字典编码，校验调整日期、数量和金额精度，映射库存流水类型，保持审批/过账幂等。
- [ ] **Step 5: 运行目标测试确认通过**：重新运行目标测试，确认新增断言通过。

### Task 3: PC 库存调整页面与权限配置

**Files:**
- Modify: `junsong-ui-v3/src/views/finance/stockInit/index.vue`
- Modify: `junsong-ui-v3/src/views/finance/stockInit/detail.vue`
- Modify: `junsong-ui-v3/src/views/member/mpPerm/index.vue`
- Modify: `junsong-ui-v3/src/api/finance/stockInit.ts`
- Test: `scripts/finance-stock-miniprogram-repair.test.mjs`

- [ ] **Step 1: 写页面失败断言**：断言页面标题、调整类型、调整日历、库存方向提示、进价回填、精度限制和 `stockAdjustment` 权限入口存在。
- [ ] **Step 2: 实现最小页面改动**：将用户可见名称改为“库存调整”，保留旧路由兼容；接入字典、商品进价回填和提交前提示。
- [ ] **Step 3: 增加表单校验**：数量正数三位、单位成本两位、金额两位；`OTHER` 无方向时禁止提交。
- [ ] **Step 4: 运行专项测试和 PC 构建**：运行 `node scripts/finance-stock-miniprogram-repair.test.mjs` 与 `cd junsong-ui-v3 && npm run build`。

### Task 4: 小程序库存调整权限与只读展示

**Files:**
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemMpController.java`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemMpPermController.java`
- Modify: `junsong-miniprogram/src/config/modules.js`
- Modify: `junsong-miniprogram/src/pages.json`
- Create/Modify: `junsong-miniprogram/src/pages/stock-adjustment/index.vue`
- Test: `junsong-modules/junsong-member/src/test/java/com/junsong/member/controller/MemMpDashboardControllerTest.java`

- [ ] **Step 1: 写模块下发失败测试**：验证授权用户得到 `stockAdjustment`，非授权用户不得到；覆盖登录后刷新模块缓存。
- [ ] **Step 2: 实现会员服务模块定义**：统一模块 key、标题、分组和授权接口返回。
- [ ] **Step 3: 实现小程序入口和只读页**：按服务端能力显示入口，读取库存调整/库存报表数据，失败时显示服务端原因。
- [ ] **Step 4: 构建并验证小程序**：运行 `cd junsong-miniprogram && npm run build:mp-weixin` 与专项测试。

### Task 5: 财务总览复用系统首页核算周期统计

**Files:**
- Modify: `junsong-ui-v3/src/views/finance/overview/index.vue`
- Modify: `junsong-ui-v3/src/views/index.vue` 或当前系统首页统计组件实际文件
- Test: `scripts/finance-stock-miniprogram-repair.test.mjs`

- [ ] **Step 1: 写失败断言**：断言财务总览引用与系统首页相同的核算周期统计组件/数据源。
- [ ] **Step 2: 实现复用**：提取或直接复用首页组件，移除财务总览重复统计计算。
- [ ] **Step 3: 运行前端专项检查与构建**：确认统计口径一致且构建通过。

### Task 6: 文档、验收、部署与本地提交

**Files:**
- Modify: `docs/superpowers/tracking/2026-08-02-finance-stock-miniprogram-repair-progress.md`
- Modify: `docs/superpowers/specs/2026-08-02-inventory-adjustment-redesign-design.md`
- Modify: `docs/superpowers/plans/2026-08-02-inventory-adjustment-redesign.md`

- [ ] **Step 1: 更新验收报告**：记录测试结果、字典 SQL 核对、WJS PROD 模块响应、库存流水样例、部署结果和回滚信息。
- [ ] **Step 2: DEV 部署**：串行构建并部署 finance、member、PC 和 SQL，避免共享 Maven target 清理冲突。
- [ ] **Step 3: PROD 部署**：备份数据库和 JAR，部署 finance、member、PC，执行字典/菜单 SQL，验证容器、页面和接口。
- [ ] **Step 4: 提交前检查**：运行 `git diff --check`、专项测试和 `git status`，确认只包含任务文件。
- [ ] **Step 5: 本地提交**：创建 Git commit，不 push；记录 commit 哈希和部署结果。
