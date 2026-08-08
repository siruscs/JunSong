<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **JunSong** (31464 symbols, 79453 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "master"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/JunSong/context` | Codebase overview, check index freshness |
| `gitnexus://repo/JunSong/clusters` | All functional areas |
| `gitnexus://repo/JunSong/processes` | All execution flows |
| `gitnexus://repo/JunSong/process/{name}` | Step-by-step execution trace |

## CLI

Use the current agent environment's GitNexus MCP tools or installed GitNexus skills for architecture, impact analysis, debugging, refactoring, guide, and index commands.

<!-- gitnexus:end -->

# JunSong-Cloud Agent Contract

Read this file first, then load only the task-specific document below. Preserve user-owned dirty changes.

## Project Map and Stack

JunSong-Cloud is a multi-tenant retail/member/finance platform.

| Area | Path |
|---|---|
| Gateway/auth/shared APIs | `junsong-gateway/`, `junsong-auth/`, `junsong-common/`, `junsong-api/` |
| Services | `junsong-modules/{finance,member,system,workflow,open,gen,file,job}/` |
| PC / mini-program | `junsong-ui-v3/`, `junsong-miniprogram/` |
| SQL / checks / deployment | `sql/`, `scripts/`, `bin/`, `docker/`, `k8s/` |

- Backend: Java 17, Spring Boot 4, Spring Cloud 2025.1, MyBatis 4, Maven.
- PC: Vue 3, TypeScript, Vite, Pinia, Element Plus.
- Mini-program: uni-app, Vue 3, Pinia, WeChat.
- Infra: MySQL, Redis, Nacos V3, Sentinel, gateway routing.
- Do not upgrade frameworks or introduce a second persistence/UI pattern during unrelated work.

## Universal Security and Finance Boundaries

1. Backend authorization is authoritative; UI hiding never replaces `@RequiresPermissions` plus service ownership checks. Keep operation permissions separate from CRUD.
2. Scope every business read/write to the current tenant and authorized department set. Validate IDs, status, totals and ownership from authoritative rows; fail closed if context is missing.
3. Financial multi-row operations are atomic and auditable. Use `BigDecimal`, scale 2, `HALF_UP`; check affected rows and generated IDs.
4. Protect transitions with conditional updates/versions and deterministic `SELECT ... FOR UPDATE` order. Never expose raw SQL/internal errors.
5. `ACTIVE` accounting periods are editable. Expense reversal is forbidden when any related row or snapshot is locked or carried-forward; reversal/carry-forward must lock and revalidate period rows in ascending ID order.
6. Verification uses independent `finance:expense:verify` / `finance:expense:unverify` permissions. Reversal is whole-batch, reasoned, snapshot-validated, rejects downstream use, and is atomic. `LEGACY` batches are never auto-reversed.

## Database and Nacos V3

- SQL must be repeatable, tenant-aware and non-destructive, with reconciliation output.
- Non-ASCII SQL starts with `SET NAMES utf8mb4;`; MySQL commands use `--default-character-set=utf8mb4`. Use `bin/deploy-sql.sh`. Verify text plus `HEX(column)` after menu/config/dictionary changes.
- DEV and PROD use **Nacos V3**. 禁止调用 `/nacos/v1/` 及任何 V1 API; never fall back to V1.
- Before using Nacos APIs, verify the V3 endpoint against the running instance or V3 documentation. On 401, 403, or 404, stop and diagnose; do not guess another endpoint or edit the DB automatically.
- PROD config changes require backup of `junsong-config.config_info`, narrow audited changes, content/MD5 verification, affected-service restart and real API/domain validation. Direct DB edits require explicit approval and a rollback record.

## PC and Mini-Program

- PC and mini-program share backend permission codes, endpoints and state rules. Server capabilities control data-dependent actions; clients fail closed and show the server reason.
- Preserve idempotency keys/payloads across ambiguous retries; refresh affected list/detail/summary/capability data after success.
- `junsong-miniprogram/` is a nested Git repository: commit/test there first, then deliberately update the parent gitlink. Never hand-edit generated `dist/`.

### UI data-entry and display conventions

- Amounts must use the shared `junsong-ui-v3/src/utils/money.ts` formatter and display as `¥8000.00` (two decimal places, with the `¥` prefix). Do not render raw amount fields or duplicate local money formatters.
- Quantity inputs use three decimal places (`0.000`); amount inputs use two decimal places (`0.00`). Empty input fields must start empty and use placeholders, never a numeric zero default unless zero is a valid business value.
- Storage codes must be converted to user-facing labels in tables and detail views; do not expose values such as `WECHAT`, `MEMBER`, or numeric status codes directly to users.
- Direct purchases use the price entered for the current order and do not depend on a product default sale price; both client and server must reject a unit price less than or equal to zero.

### Mini-program architecture: MUST reuse the shared framework & pages. 禁止为每个新功能再造一套页面/组件。

**这条是强制要求（MUST / 禁止），如果违反会直接被判定为架构污染，必须整改后才能交付。**

#### 1）新功能默认走通用列表页 `junsong-miniprogram/src/pages/list/index.vue`

- **不要新建单页 vue**：绝大多数"列表 + 详情/增删改查"类业务（如：采购单、退货单、库存流水、等级配置、销售政策、库存调整……）**必须**通过在 `junsong-miniprogram/src/config/modules.js` 中**注册模块元数据**（列定义、详情字段、操作按钮、权限码、CRUD endpoint 等）来接入，页面框架、空/错/加载态、分页刷新、批量操作、部门上下文、分享、明细渲染等**直接复用 `pages/list/index.vue`**。
- 新建功能若不满足以下（2）的"通用页不适用"条件，却又新建了独立 `pages/*/index.vue`，视为架构错误，必须在独立 review 中给出合理解释否则退回重写。
- 模块注册完成后，必须通过 `list-work-scope.test.mjs` / `list-work-context-ui.test.mjs` 中相关契约测试（页面元数据→权限→部门范围联动），并执行 `cd junsong-miniprogram && npm run build:mp-weixin` 构建。

#### 2）只有当 `pages/list/index.vue` 确实不满足时，才允许新建独立页面，并且必须满足：

> 先在 Plan 文档里写明"为什么通用 list 页不适用"、"将复用哪些共享组件/工具"，不写清楚禁止开工。

允许新建独立页面的典型场景（且须逐条满足）：
- 页面包含**专用汇总栏 / 多列表联动 / 固定表头报表 / 非标准数据看板**等复杂交互结构，通用 list 页的 module schema 无法表达。
- 存在**跨接口多步骤工作流**（如：购买下单 → 绑定会员 → 登记收款 → 登记领取），单个 CRUD schema 无法承载。
- 存在**重型地图/可视化/自定义表单布局**等非标准内容。

新建独立页面时**强制复用**以下公用能力（禁止复制粘贴重写一遍，否则直接不通过 review）：

| 能力 | 必须复用的位置 | 禁止 |
|---|---|---|
| 部门显示条（"当前部门 · 共 N 个部门"） | `junsong-miniprogram/src/utils/listWorkScope.js` 的 `resolveListWorkScope` / `applyWorkScopeToPage`，模板完全对齐 `work-scope` 样式 | 每个页面自己拼 `scopeLabel` / 自己加 CSS |
| 点击切换部门（底部 sheet 选择列表） | `src/components/DeptSwitcher.vue` + `openDeptSwitcher()` + `handleDeptChanged()`（同上 listWorkScope.js） | 自己弹 ActionSheet、用 showModal 让用户输入编号、任何输入式切换 |
| 列表空/错/加载态 | `src/components/StateView.vue` + 统一的状态判断（`loading / loadError / rows.length`） | 每个页面自己画一套空状态和错误提示 |
| 会员搜索选择 | `src/components/MemberSearch/index.vue` | 自己画会员搜索弹窗/输入 |
| 部门上下文/切换后刷新 | `src/utils/workContext.js` + onShow 中 `applyWorkScopeToPage` 判断 `departmentChanged` → 重拉数据 | 自己存 `deptId/deptName`、自己调 `/system/user/getInfo` 拿部门、自己写 switchDept 请求 |
| 请求/权限/字典 | `@/api/index.js` 的 `request`、`@/utils/permission.js`、`@/utils/dictCache.js` | 页面里手写 `uni.request`、自己复制一份权限判断 |
| 金额/数量格式化 | `src/utils/money.ts`（小程序侧对应已有通用工具） | 页面里 `toFixed(2)` 各写各的 |

#### 3）验收清单（新功能 review 必须逐项打勾）

- [ ] 是否首先尝试了 `pages/list/index.vue` + module 注册？如果没有，plan 中是否列出了明确不适用的 3 条以上具体原因？
- [ ] 部门显示条是否用 `applyWorkScopeToPage(this)` 注入 `scopeLabel / currentDeptId / currentDeptName`，而不是自己拼字符串？
- [ ] 部门切换是否嵌入了 `<dept-switcher>` 组件（通过 `v-model:visible` + `@change`），并在 methods 中走 `openDeptSwitcher(this)` / `handleDeptChanged(this, reload)`？
- [ ] 列表加载/空/错态是否复用了 `StateView`？
- [ ] 如果页面需要会员搜索，是否引入了 `MemberSearch` 组件？
- [ ] onShow 中是否使用 `applyWorkScopeToPage(this)` 的返回 `departmentChanged` 来决定重拉数据，而不是每次 onShow 都刷 or 从不刷？
- [ ] 是否通过了 `node --test junsong-miniprogram/test/*.test.mjs` 以及 `npm run build:mp-weixin`？

#### 4）历史遗留页面的处理方向

本次之前已存在的 7 个独立页面（member-purchase、member-purchase-return、stock、stock-ledger、stock-adjustment、campaign-policy、member-level）已补齐共享组件接入（DeptSwitcher、applyWorkScopeToPage、handleDeptChanged、StateView）。
**后续任何改动/新增功能，都不允许再新增"独立造轮子"的单页；老页在下次重大改需求时应优先评估迁回 `pages/list/index.vue` schema。**

## Required Workflow

1. Read nested `AGENTS.md`; inspect `git status --short` before edits.
1.5 **Mini-program new feature MANDATORY check.** 新建小程序功能页时：
   - 先阅读 `AGENTS.md` 中的 "Mini-program architecture: MUST reuse the shared framework & pages" 强制规则全文（**必须**读，不要跳过）。
   - 第一步先去 `junsong-miniprogram/src/config/modules.js` 评估是否能用通用 `pages/list/index.vue` + module schema 注册实现。如果能就**不要新建页面**。
   - 如果通用页确实不适用 → 必须先在 Plan 里写出至少 3 条"为什么通用 list 页不满足" + "将复用哪些共享组件/工具"清单（DeptSwitcher / applyWorkScopeToPage / StateView / MemberSearch / workContext / dictCache / permission / request）。没有这份 Plan，禁止开始新建独立页面。
   - 独立页面写完后必须对照上方的验收清单（3）逐条自查。
2. Use GitNexus `query`/`context` for unfamiliar flows. Run upstream `impact` before editing an existing symbol and warn before HIGH/CRITICAL risk.
3. For behavior changes, write and observe a focused failing test before implementation. Keep edits local; do not overwrite unrelated changes.
4. Run narrow tests first, then relevant regression/build checks. Prove and report pre-existing failures rather than hiding or fixing them without scope.
5. Obtain independent review; resolve all Critical/Important findings.
6. Before commit, stage only task-owned files, run `detect_changes(scope="staged")`, inspect staged names, and run `git diff --cached --check`.
7. Never use destructive Git cleanup or commit secrets/runtime data.

## Task-Specific Reading

| Task | Required document |
|---|---|
| Expense verification/reversal | `docs/superpowers/specs/2026-07-11-expense-verification-reversal-design.md` |
| Its implementation/tests | `docs/superpowers/plans/2026-07-11-expense-verification-reversal.md` |
| DEV/PROD/Nacos operations | `bin/2026-07-01-dev-prod-deployment-agent-handoff.zh-CN.md` |
| General deployment | `部署运维手册.md` |

## Commands

```bash
cd junsong-modules/<module> && mvn -Dtest=SpecificTest test
cd junsong-ui-v3 && npm run build
node --test scripts/<relevant-test>.mjs
cd junsong-miniprogram && node --test test/*.test.mjs && npm run build:mp-weixin
```

Done means authorization and tenant/department/period/concurrency rules are enforced, focused tests and relevant builds pass, migrations reconcile safely, independent review is clean, staged impact is expected, and documentation matches verified behavior.


<claude-mem-context>
# Memory Context

# [JunSong-Cloud] recent context, 2026-07-23 8:41pm GMT+8

No previous sessions found.
</claude-mem-context>
