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

## Required Workflow

1. Read nested `AGENTS.md`; inspect `git status --short` before edits.
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
