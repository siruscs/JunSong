# 小程序核算周期经营 V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在独立分支中按 V1.0、V1.1、V1.2 三个门禁阶段完成小程序经营核心、库存表单、权限与技术债升级，通过 DEV 人工验收后再合并主分支。

**Architecture:** 会话恢复集中在 `authSession.js`、`foregroundSession.js` 和统一请求层，页面不自行处理 token 过期。周期首页通过财务服务现有周期摘要接口按角色展示，前端不重新计算业务口径。库存单据以确定性幂等键和服务端状态机为准，表单以模块组件和统一状态组件承载，权限由页面守卫和后端能力共同决定。

**Tech Stack:** uni-app/Vue 3、小程序 `uni.request`、Java Spring Boot、MyBatis、MySQL、Node.js 静态测试。

## 版本执行顺序和门禁

1. V1.0：经营核心与稳定性。完成后部署 DEV，验证 FILE 服务和小程序核心链路，再进入 V1.1。
2. V1.1：库存和表单能力。必须保留 V1.0 周期利润口径，不得把库存成本重新替代核算周期成本。
3. V1.2：权限与技术债。必须在 V1.1 回归通过后开发，最终统一执行合并前审查。

任何阶段新增需求先登记到对应阶段，不直接改动其他阶段的任务；每阶段独立提交，父仓库只在子仓库提交后更新 gitlink。

---

### Task 1: 固化当前基线和失败场景

**Files:**
- Test: `scripts/miniprogram-period-v1.test.mjs`
- Modify: `docs/superpowers/tracking/2026-08-03-miniprogram-period-v1-progress.zh-CN.md`

- [ ] **Step 1: 写登录恢复和周期首页基线断言**

断言覆盖：分支名称、会话恢复入口、周期字段、角色关键词、不得默认调用传统现金流入口。

- [ ] **Step 2: 执行基线测试并记录失败项**

Run: `node scripts/miniprogram-period-v1.test.mjs`

Expected: 在实现前至少暴露 token 恢复入口和周期首页缺口。

- [ ] **Step 3: 更新跟踪文档**

记录失败输出、涉及文件和本阶段暂不实施的 V1.1/V1.2 任务；这些任务已在本计划后续章节登记，不得遗失或临时改写阶段边界。

- [ ] **Step 4: Commit**

```bash
git add scripts/miniprogram-period-v1.test.mjs docs/superpowers/tracking/2026-08-03-miniprogram-period-v1-progress.zh-CN.md
git commit -m "test(mp): add period v1 baseline"
```

### Task 2: 修复会话恢复和 Token 刷新

**Files:**
- Modify: `junsong-miniprogram/src/utils/authSession.js`
- Modify: `junsong-miniprogram/src/utils/foregroundSession.js`
- Modify: `junsong-miniprogram/src/api/index.js`
- Modify: `junsong-miniprogram/src/App.vue`
- Test: `junsong-miniprogram/test/auth-session.test.mjs`

- [ ] **Step 1: 写失败测试**

覆盖本地 access token 恢复、即将过期刷新、refresh 失败清理会话、并发请求只允许一次 refresh。

- [ ] **Step 2: 实现统一会话状态**

统一保存 access token、refresh token、过期时间和最近用户信息；冷启动与回前台调用同一个 `restoreSession()`。

- [ ] **Step 3: 在请求层处理 401**

首个 401 进入单例 refresh Promise；其他请求复用该 Promise。刷新成功重放原请求一次，失败清理会话并跳转登录。

- [ ] **Step 4: 加入前后台恢复**

`App.vue` 的 `onShow` 调用会话恢复；恢复期间不重复发起业务首页请求。

- [ ] **Step 5: 运行测试**

Run: `node --test junsong-miniprogram/test/auth-session.test.mjs`

Expected: PASS，覆盖 WJS 中午登录后下午回到前台的生命周期场景。

- [ ] **Step 6: Commit**

```bash
git add junsong-miniprogram/src/utils junsong-miniprogram/src/api/index.js junsong-miniprogram/src/App.vue junsong-miniprogram/test/auth-session.test.mjs
git commit -m "fix(mp): restore session after foreground resume"
```

### Task 3: 设计并实现周期摘要后端接口

**Files:**
- Modify/Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinAccountingPeriodController.java`
- Modify/Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinAccountingPeriodServiceImpl.java`
- Modify/Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/AccountingPeriodSummaryVO.java`
- Modify/Create: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinAccountingPeriodMapper.xml`
- Test: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinAccountingPeriodServiceImplTest.java`

- [ ] **Step 1: 写失败测试**

验证摘要包含实际缴款、已核销费用、进货款、未核销借支、周期净利、回本差额和回本进度；无部门授权时返回空授权结果，不查询全门店。

- [ ] **Step 2: 实现授权范围查询**

服务层以当前租户和授权部门集合为边界；管理员也只有在显式选择部门时查询指定部门，缺失部门上下文时 fail-closed。

- [ ] **Step 3: 实现统一周期公式**

复用已有周期实时刷新逻辑，使用 `BigDecimal`、scale 2、`HALF_UP`，不使用销售额替代实际缴款。

- [ ] **Step 4: 增加接口契约测试**

验证字段命名、空值默认、授权范围和周期结束后的历史数据读取。

- [ ] **Step 5: 运行后端构建和测试**

Run: `mvn -Dtest=FinAccountingPeriodServiceImplTest test` in `junsong-modules/junsong-finance`

Expected: PASS；若仓库既有测试编译问题，记录为前置失败并单独修复，不隐藏失败。

- [ ] **Step 6: Commit**

```bash
git add junsong-modules/junsong-finance
git commit -m "feat(finance): expose accounting period summary"
```

### Task 4: 改造小程序首页为周期经营看板

**Files:**
- Modify: `junsong-miniprogram/src/pages/index/index.vue`
- Modify: `junsong-miniprogram/src/pages/workbench/index.vue`
- Create/Modify: `junsong-miniprogram/src/api/accountingPeriod.js`
- Create/Modify: `junsong-miniprogram/src/components/StateView.vue`
- Test: `junsong-miniprogram/test/period-dashboard.test.mjs`

- [ ] **Step 1: 写首页展示基线测试**

验证店长/主管/投资人首屏包含净利、回本差额、实际缴款、周期成本，并隐藏传统现金流主卡片。

- [ ] **Step 2: 接入周期摘要 API**

首页只消费标准化摘要，不在页面中重新计算费用、进货和借支。

- [ ] **Step 3: 实现角色视图**

店长显示本店；主管显示授权门店汇总和异常；投资人显示授权范围净利与分润；记账员保留待办入口。

- [ ] **Step 4: 实现空、错、加载状态**

没有周期或部门时显示明确说明；首次加载失败提供重试；已有成功数据保留并显示更新时间。

- [ ] **Step 5: 运行小程序测试和构建**

Run: `node --test junsong-miniprogram/test/period-dashboard.test.mjs && npm run build:mp-weixin` in `junsong-miniprogram`

Expected: PASS，生成微信小程序构建产物。

- [ ] **Step 6: Commit**

```bash
git add junsong-miniprogram/src junsong-miniprogram/test
git commit -m "feat(mp): add accounting period business dashboard"
```

### Task 5: DEV 部署、回归和人工验收准备

**Files:**
- Modify: `docs/superpowers/tracking/2026-08-03-miniprogram-period-v1-progress.zh-CN.md`

- [ ] **Step 1: 运行全部 V1 检查**

Run: `node scripts/miniprogram-period-v1.test.mjs`

Run: `node scripts/finance-accounting-period-evolution.test.mjs`

Run: `node scripts/finance-stock-miniprogram-repair.test.mjs`

- [ ] **Step 2: 构建前后端**

Run: `mvn -Dmaven.test.skip=true package` for finance module.

Run: `npm run build:mp-weixin` in `junsong-miniprogram`.

- [ ] **Step 3: 部署 DEV**

部署财务服务、PC 依赖接口和小程序构建产物；记录构建哈希和服务重启结果。

- [ ] **Step 4: 执行验收场景**

使用 WJS 记账员、店长主管、投资人分别验证：冷启动、后台停留后回前台、Token 刷新、无周期、单部门、多部门、无权部门和周期净利数值。

- [ ] **Step 5: 更新跟踪文档并暂停合并**

只有人工验收通过后，才进入主分支合并评审；本任务不自动合并、不部署 PROD。

### Task 6: 合并前审查

- [ ] **Step 1: 检查父仓库和子仓库状态**
- [ ] **Step 2: 执行 `git diff --check` 和专项测试**
- [ ] **Step 3: 检查租户、部门、权限和并发边界**
- [ ] **Step 4: 记录独立 review 结论**
- [ ] **Step 5: 由用户确认后合并到 `main`，不自动 PUSH**

## V1.1：库存和表单能力

### Task 7: 库存流水分页筛选

**Files:**
- Modify: `junsong-miniprogram/src/pages/stock-ledger/index.vue`
- Modify: `junsong-miniprogram/src/api/stocktake.js`
- Test: `junsong-miniprogram/test/stock-ledger-pagination.test.mjs`

- [ ] **Step 1: 写失败测试**：验证 `pageNum/pageSize`、日期范围、变动类型和刷新游标均进入请求参数，加载更多不会覆盖已有流水。
- [ ] **Step 2: 运行测试确认失败**：`node --test junsong-miniprogram/test/stock-ledger-pagination.test.mjs`，预期分页状态和筛选参数断言失败。
- [ ] **Step 3: 实现分页筛选**：增加筛选栏、`finished` 和 `loadingMore` 状态；部门变化时清空旧流水并从第一页加载；服务端返回的总数作为结束判断依据。
- [ ] **Step 4: 验证**：运行该测试、库存专项测试和 `npm run build:mp-weixin`。
- [ ] **Step 5: Commit**：`git add junsong-miniprogram/src/pages/stock-ledger junsong-miniprogram/src/api/stocktake.js junsong-miniprogram/test/stock-ledger-pagination.test.mjs && git commit -m "feat(mp): paginate stock ledger"`

### Task 8: 多商品库存调整和全流程幂等

**Files:**
- Modify: `junsong-miniprogram/src/pages/stock-adjustment/index.vue`
- Modify: `junsong-miniprogram/src/api/stockInit.js`
- Modify: `junsong-miniprogram/src/utils/idempotency.js`
- Test: `junsong-miniprogram/test/stock-adjustment-v11.test.mjs`

- [ ] **Step 1: 写失败测试**：验证一个调整单可以保存多个商品行、数量和成本逐行校验；创建、更新、校验、提交、审批、过账、删除均携带同一业务动作的确定性幂等键。
- [ ] **Step 2: 运行测试确认失败**：`node --test junsong-miniprogram/test/stock-adjustment-v11.test.mjs`。
- [ ] **Step 3: 实现**：复用盘点模块确定性幂等键生成器；按 `batchId/action/version` 生成键；成功后刷新详情和列表，网络重试不生成第二张单；前端只展示服务端能力允许的动作。
- [ ] **Step 4: 验证**：运行库存调整专项测试、后端库存测试和微信构建；检查重复点击不会出现重复单据。
- [ ] **Step 5: Commit**：`git add junsong-miniprogram/src/pages/stock-adjustment junsong-miniprogram/src/api/stockInit.js junsong-miniprogram/src/utils/idempotency.js junsong-miniprogram/test/stock-adjustment-v11.test.mjs && git commit -m "feat(mp): support idempotent multi-item stock adjustments"`

### Task 9: 草稿自动保存、表单组件化和统一状态组件

**Files:**
- Modify: `junsong-miniprogram/src/pages/form/index.vue`
- Create/Modify: `junsong-miniprogram/src/pages/form/form-modules/*.vue`
- Modify: `junsong-miniprogram/src/utils/draftStore.js`
- Create: `junsong-miniprogram/src/components/StateView.vue`
- Modify: `junsong-miniprogram/src/pages/stock-ledger/index.vue`
- Modify: `junsong-miniprogram/src/pages/stock-adjustment/index.vue`
- Test: `junsong-miniprogram/test/form-v11.test.mjs`

- [ ] **Step 1: 写失败测试**：验证费用、进货、库存调整至少三类表单能自动保存、恢复和提交后清理；`StateView` 覆盖 loading/empty/error/normal 四态。
- [ ] **Step 2: 运行测试确认失败**：`node --test junsong-miniprogram/test/form-v11.test.mjs`。
- [ ] **Step 3: 实现**：按 `moduleKey + deptId + userId` 隔离草稿；编辑已有单据不覆盖新建草稿；提交成功后清理；先将字段和校验拆成模块组件，再由表单壳动态装载，避免一次重写全部表单。
- [ ] **Step 4: 接入状态组件**：库存流水、库存调整和费用表单统一显示加载、空数据、错误重试，错误信息保留服务端原因。
- [ ] **Step 5: 验证并提交**：运行小程序全量专项测试、微信构建和人工草稿恢复检查；提交 `git commit -m "feat(mp): improve form drafts and state views"`。

## V1.2：权限与技术债

### Task 10: 页面权限统一守卫和后端能力驱动按钮

**Files:**
- Modify: `junsong-miniprogram/src/utils/permission.js`
- Modify: `junsong-miniprogram/src/App.vue`
- Modify: `junsong-miniprogram/src/pages.json`
- Modify: `junsong-miniprogram/src/pages/stock-adjustment/index.vue`
- Modify: `junsong-miniprogram/src/pages/form/index.vue`
- Test: `junsong-miniprogram/test/permission-guard-v12.test.mjs`

- [ ] **Step 1: 写失败测试**：无模块权限不得进入页面；有页面权限但无后端操作能力时按钮隐藏或禁用；服务端拒绝时展示真实权限码。
- [ ] **Step 2: 运行测试确认失败**：`node --test junsong-miniprogram/test/permission-guard-v12.test.mjs`。
- [ ] **Step 3: 实现路由守卫**：集中读取页面元数据并在 `navigateTo/switchTab` 前校验模块权限；保留页面级二次校验，防止深链绕过。
- [ ] **Step 4: 实现能力驱动按钮**：页面加载能力集合并按 `canCreate/canUpdate/canSubmit/canApprove/canPost` 控制按钮；能力缺失时不通过前端猜测放行。
- [ ] **Step 5: 验证并提交**：用 WJS 记账员、店长主管、ADMIN 验证授权差异，提交 `feat(mp): centralize permission guards and capabilities`。

### Task 11: 秒杀统计批量接口

**Files:**
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemberSeckillController.java`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberSeckillServiceImpl.java`
- Modify: `junsong-miniprogram/src/pages/index/index.vue`
- Test: `scripts/miniprogram-seckill-batch.test.mjs`

- [ ] **Step 1: 写失败测试**：多个秒杀活动首页只调用一次批量接口，结果按活动 ID 映射，越权活动不返回统计。
- [ ] **Step 2: 实现服务端批量查询**：校验租户和授权范围，使用单次批量查询代替 N+1；空 ID 集合直接返回空结果。
- [ ] **Step 3: 接入首页并节流**：首页首次进入加载，回前台 60 秒内不重复请求低频统计，核心周期 KPI 仍可刷新。
- [ ] **Step 4: 验证并提交**：运行会员服务测试、静态检查和微信构建，提交 `feat(member): batch seckill statistics`。

### Task 12: Vite patch 文档化和统一错误上报

**Files:**
- Modify: `junsong-miniprogram/vite.config.js`
- Modify: `junsong-miniprogram/package.json`
- Create: `junsong-miniprogram/docs/runtime-patch.md`
- Create/Modify: `junsong-miniprogram/src/utils/errorReporter.js`
- Modify: `junsong-miniprogram/src/api/index.js`
- Test: `junsong-miniprogram/test/error-reporter-v12.test.mjs`

- [ ] **Step 1: 写失败测试**：网络错误、超时、业务错误和未捕获异常按统一结构上报，脱敏 token、手机号和请求体敏感字段。
- [ ] **Step 2: 文档化 patch**：记录 patch 原因、影响版本、验证命令和移除条件；锁定 uni-app 版本，升级只能通过专项构建验证。
- [ ] **Step 3: 接入错误上报**：统一记录 requestId、页面、模块、错误分类和 contextVersion；离线时本地限量缓存，恢复网络后批量发送。
- [ ] **Step 4: 验证并提交**：运行错误上报测试、微信构建、全量专项测试，提交 `chore(mp): document runtime patch and unify error reporting`。
