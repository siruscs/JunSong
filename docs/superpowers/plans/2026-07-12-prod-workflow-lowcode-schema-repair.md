# PROD Workflow Lowcode Schema Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create every missing tenant-aware workflow and low-code extension table required by the currently deployed workflow module.

**Architecture:** A single non-destructive UTF-8 migration defines 17 tables from current mapper/domain contracts, with tenant-first indexes and fail-closed structural reconciliation. A Node contract test enforces table coverage and migration safety.

**Tech Stack:** MySQL 9.2, MyBatis, tenant SQL interceptor, Node `node:test`, audited PROD SQL deployment.

---

### Task 1: Add the failing migration contract

**Files:**
- Create: `scripts/prod-workflow-lowcode-schema-repair.test.mjs`

- [ ] Assert the migration starts with UTF-8 setup, creates all 17 required tables with `IF NOT EXISTS`, contains `tenant_id`, and contains no `DROP TABLE` or `TRUNCATE`.
- [ ] Assert reconciliation queries use `information_schema` to verify table and tenant-column counts.
- [ ] Run the test and confirm it fails because the migration does not exist.

### Task 2: Build the consolidated schema migration

**Files:**
- Create: `sql/prod_workflow_lowcode_schema_repair.sql`

- [ ] Define the six `wf_*` tables from current mapper/domain fields with tenant-first uniqueness/indexes.
- [ ] Define the eleven `lc_*` tables from current mapper/domain fields with tenant-first uniqueness/indexes.
- [ ] Add fail-closed postconditions requiring exactly 17 target tables and 17 `tenant_id` columns.
- [ ] Add result sets listing table engines, collations, tenant columns and indexes.
- [ ] Run the focused contract and relevant MyBatis/schema health tests.

### Task 3: Review, deploy, verify and commit

- [ ] Request independent review for schema compatibility, tenant safety, destructive statements and repeatability.
- [ ] Execute through `bin/deploy-sql.sh ... prod` after automatic backup.
- [ ] Verify all target tables and field-permission query prerequisites in PROD.
- [ ] Stage only migration/test/docs, run GitNexus staged detection and `git diff --cached --check`, then commit.
