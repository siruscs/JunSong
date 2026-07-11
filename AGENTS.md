<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **JunSong** (30021 symbols, 75166 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

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

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->

# JunSong-Cloud Project Operating Guide

This section is the shared onboarding contract for every agent working in this repository. Read it before exploring, planning, editing, testing, or committing.

## Product and Repository Shape

JunSong-Cloud is a multi-tenant retail/member/finance operations platform. The root repository contains a Java microservice backend, a PC web application, deployment assets, SQL migrations, tests, and a nested mini-program repository.

| Area | Path | Responsibility |
|---|---|---|
| Gateway | `junsong-gateway/` | External routing, authentication entry, API-key/open-platform filters |
| Authentication | `junsong-auth/` | Login and token-related services |
| Shared libraries | `junsong-common/` | Core, security, logging, Redis, datasource, tenant and distributed support |
| Service APIs | `junsong-api/` | Cross-service API models/contracts |
| Finance | `junsong-modules/junsong-finance/` | Expenses, advances, sales, purchases, accounting periods, reports and audit |
| Member | `junsong-modules/junsong-member/` | Members, levels, growth, sign-in and member operations |
| System | `junsong-modules/junsong-system/` | Users, roles, menus, permissions and governance |
| Other services | `junsong-modules/{file,job,workflow,open,gen}/` | File, jobs, workflow, open platform and code generation |
| PC frontend | `junsong-ui-v3/` | Vue 3 + TypeScript + Element Plus management UI |
| Mini-program | `junsong-miniprogram/` | uni-app/Vue WeChat mini-program; this is a nested Git repository |
| Database changes | `sql/` | Repeatable or release-specific MySQL migrations |
| Contract/health tests | `scripts/` | Node-based static and cross-file regression checks |
| Runtime/deployment | `docker/`, `bin/`, `k8s/` | Local/prod orchestration and deployment scripts |

## Core Technology Baseline

- Backend: Java 17, Spring Boot 4.0.x, Spring Cloud 2025.1.x, Spring Cloud Alibaba, MyBatis 4, Maven.
- Service infrastructure: Nacos discovery/config, Sentinel, Redis, Seata support, gateway-based routing.
- Database: MySQL with MyBatis mapper interfaces and XML SQL.
- PC: Vue 3, TypeScript, Vite, Pinia, Element Plus.
- Mini-program: uni-app, Vue 3, Pinia, WeChat mini-program build target.
- Backend tests: JUnit 5; many finance tests use hand-written fake mappers instead of Mockito.
- Cross-file rules: Node `node:test` health checks under `scripts/` and mini-program `test/`.

Do not silently upgrade framework versions, rewrite build systems, or introduce a second persistence/UI pattern as part of an unrelated feature.

## Non-Negotiable Security and Data Boundaries

1. Backend authorization is authoritative. Hiding a PC or mini-program button is never a substitute for `@RequiresPermissions` and service-level ownership checks.
2. Keep operation permissions independent from CRUD permissions. Editing, verifying, reversing, deleting, exporting and importing must not share a permission merely for convenience.
3. Every business read and mutation must be scoped to the current tenant and the effective authorized department set. Store-local mutations use the currently selected department; authorized cross-store reporting may use an explicitly validated department set. Never fetch by an ID and assume a later UI check is enough.
4. Fail closed when tenant, department, period, ownership, status or permission context cannot be established.
5. Do not trust IDs, totals, status, department, tenant, capability flags or calculated values supplied by a client. Reload and validate authoritative rows in the service transaction.
6. Protect concurrent financial transitions with conditional updates, optimistic versions and/or deterministic `SELECT ... FOR UPDATE` locking. Define and preserve a consistent lock order.
7. All multi-row financial operations must be atomic. A partial success is a defect.
8. Use `BigDecimal` for money and explicitly define scale and rounding. Current finance verification uses scale 2 and `RoundingMode.HALF_UP`.
9. Preserve auditability: business reversals invalidate or restore through recorded transactions; do not physically erase audit batches/details.
10. Never expose raw SQL/internal exception messages to clients. Log unexpected errors server-side and return controlled business messages.

## Accounting-Period Invariants

- `ACTIVE` periods are editable; locked/break-even-waiting and carried-forward periods are historical.
- Any mutation of period-bound finance rows must enforce the operation-appropriate period policy. A normal editable check may be sufficient for a simple mutation; a concurrent transition such as reversal or carry-forward must also lock and validate the period row inside the transaction.
- Expense reversal is absolutely forbidden if any related snapshot or current row belongs to a locked or carried-forward period.
- For reversal/carry-forward concurrency, lock the same accounting-period row inside the transaction. A read-only status check is not enough.
- When several period rows are locked, use ascending period ID order.
- Do not modify the widely used `assertPeriodEditable` without impact analysis and explicit HIGH/CRITICAL review; prefer a narrow operation-specific locking method when appropriate.

## Expense Verification and Reversal Rules

The authoritative design and implementation plan are:

- `docs/superpowers/specs/2026-07-11-expense-verification-reversal-design.md`
- `docs/superpowers/plans/2026-07-11-expense-verification-reversal.md`

Agents must preserve these invariants:

- `finance:expense:edit` edits an unverified expense only.
- `finance:expense:verify` authorizes single and batch verification.
- `finance:expense:unverify` authorizes batch-level reversal.
- Store-manager/supervisor roles do not receive verify/unverify by default.
- Single verification is a one-expense batch and uses the same service/API as batch verification.
- Advances are optional, but when selected they must belong to the same tenant/current department, be unverified, and be in an editable period.
- A verification batch records immutable expense/advance snapshots and generated supplement/surplus relationships.
- Request IDs are idempotency keys bound to tenant, department and the immutable input ID sets. Retry the same payload with the same key.
- Reversal operates on a complete batch, requires a reason, validates current rows against snapshots, locks batch/business/period rows, rejects downstream use, and restores atomically.
- `LEGACY` historical batches are audit-only and can never be automatically reversed.
- Historical migrations must not infer source/supplement/surplus relations from current `advance_id` or status alone.

## Backend Implementation Conventions

- Controllers validate transport input, declare exact permissions and delegate; keep accounting logic in transactional services.
- Service methods enforce tenant/department ownership, state transitions, period rules, idempotency and audit.
- Mapper mutations for financial state should include tenant, department, current status and delete-flag predicates.
- Check affected-row counts and generated primary keys; zero/unexpected counts are concurrency or integrity failures.
- For new mapper methods, remember that hand-written fake mapper classes in tests must compile. Add minimal neutral overrides without changing existing test semantics.
- Use explicit MyBatis `resultMap` mappings for important snake_case financial snapshots rather than relying on environment-specific camel-case settings.
- SQL migrations must be repeatable, tenant-aware and non-destructive. Include preview/reconciliation/exception result sets for financial backfills.

## Database Character-Set Safety

- Every SQL file containing Chinese or other non-ASCII text must begin with `SET NAMES utf8mb4;`. Do not rely on the server, container image or interactive client default.
- Every scripted MySQL invocation must pass `--default-character-set=utf8mb4`, including DEV, PROD, dry-run examples and one-off verification commands.
- Before executing production SQL, verify `character_set_client`, `character_set_connection` and `character_set_results`; all three must be `utf8mb4`.
- After menu/config/dictionary migrations, query both displayed text and `HEX(column)`. UTF-8 Chinese must not appear as mojibake such as `è´¹ç”¨` or as replacement characters.
- If mojibake is found, back up first, identify rows by stable keys such as permission/config/dictionary keys, and use a narrow audited UTF-8 correction. Never run broad text-conversion updates over an entire table.
- Use `bin/deploy-sql.sh` as the canonical SQL entrypoint; do not bypass its UTF-8 enforcement with ad-hoc `mysql < file.sql` commands.

## PC and Mini-Program Consistency

- PC and mini-program must use the same backend permission codes, endpoints and state rules.
- UI permission checks improve discoverability only; the backend must reject unauthorized direct calls.
- Server capability responses govern data-dependent availability such as reversal. Clients must fail closed, disable the action and persistently show the server-provided reason.
- Keep idempotency request ID and payload stable across ambiguous network failures. Clear them only on success, explicit cancel or record/batch change.
- Refresh list, summary, detail and capability state after successful mutations and when returning from child pages.
- Mini-program expense work may use module-specific UI, but must not break the generic configuration-driven behavior of unrelated modules.
- `junsong-miniprogram/` is a nested Git repository. Run its status/tests/commit there first; update the parent gitlink only deliberately.
- Treat `junsong-miniprogram/src/` as source. Do not hand-edit generated `junsong-miniprogram/dist/`; regenerate it with the build and include generated output only when the release policy explicitly requires it.

## Required Working Method

1. Read this file and any more-specific nested `AGENTS.md` before acting.
2. Inspect `git status --short` before edits. This repository may have extensive user-owned dirty changes; preserve them.
3. For unfamiliar behavior, use GitNexus `query`/`context` before raw grep loops.
4. Before editing every existing symbol, run upstream `impact` and tell the user the blast radius. Stop and warn before HIGH/CRITICAL changes.
5. For features and bug fixes, use TDD: write a focused failing test, observe the expected failure, implement the minimum behavior, then rerun focused and relevant regression suites.
6. Keep edits localized. Do not reformat large existing files or overwrite overlapping user changes.
7. After each logical task, request an independent code review. Fix every Critical and Important finding before continuing.
8. Before any commit, stage only task-owned files/hunks, run `detect_changes(scope="staged")`, inspect the staged file list and run `git diff --cached --check`.
9. Never use destructive Git cleanup to resolve a dirty tree. Do not commit secrets, generated runtime data or unrelated user changes.

## Test and Build Commands

Run the narrowest relevant command first, then broaden:

```bash
# Finance backend
cd junsong-modules/junsong-finance
mvn -Dtest=SpecificTest test
mvn test

# PC frontend
cd junsong-ui-v3
npm run build

# Root static health checks
node --test scripts/<relevant-test>.mjs

# Mini-program (nested repository)
cd junsong-miniprogram
node --test test/*.test.mjs
npm run build:mp-weixin
```

If a full suite has a pre-existing failure, rerun that exact test alone, prove it is unrelated using current diff/history, and report it. Do not hide it or fix unrelated work without authorization.

## Definition of Done

A change is complete only when:

- permissions are correct at UI, controller and service boundaries;
- tenant/department/period/concurrency rules are tested;
- focused tests pass and relevant builds succeed;
- regression results and any proven pre-existing failures are reported;
- independent review has no unresolved Critical or Important findings;
- migrations include reconciliation and safe repeat execution;
- staged impact contains only intended symbols and flows;
- documentation reflects verified behavior rather than assumptions.

<claude-mem-context>
# Memory Context

# [JunSong-Cloud] recent context, 2026-07-11 7:19pm GMT+8

No previous sessions found.
</claude-mem-context>
