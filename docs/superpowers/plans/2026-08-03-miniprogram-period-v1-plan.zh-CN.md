# 小程序核算周期经营 V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在独立分支中完成小程序登录稳定性和核算周期经营首页 V1，并通过 DEV 人工验收后再合并主分支。

**Architecture:** 会话恢复集中在 `authSession.js`、`foregroundSession.js` 和统一请求层，页面不自行处理 token 过期。周期首页通过财务服务提供的授权范围周期摘要接口，按角色展示，前端不重新计算业务口径。

**Tech Stack:** uni-app/Vue 3、小程序 `uni.request`、Java Spring Boot、MyBatis、MySQL、Node.js 静态测试。

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

记录失败输出、涉及文件和本次不处理的 V1.1/V1.2 项。

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
