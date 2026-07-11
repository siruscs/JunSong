# PROD Workflow Lowcode Menu and Config Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore complete PROD workflow and low-code menu trees, grant them only to the administrator role, and repair six mojibake system parameters.

**Architecture:** A single repeatable UTF-8 SQL migration uses stable menu IDs and permission keys, narrow upserts, role-key authorization, exact config-key updates, and reconciliation result sets. A Node contract test statically enforces the migration boundaries before PROD execution.

**Tech Stack:** MySQL 9.2, `sys_menu`, `sys_role_menu`, `sys_role`, `sys_config`, Node `node:test`, audited SQL deployment scripts.

---

### Task 1: Add a failing migration contract

**Files:**
- Create: `scripts/prod-workflow-lowcode-menu-config-repair.test.mjs`

- [ ] Assert the migration starts with `SET NAMES utf8mb4;`, uses root IDs 2220/2280, contains current workflow/lowcode components and permission codes, grants through `role_key='admin'`, updates exactly the six approved config keys, and contains reconciliation/HEX queries.
- [ ] Run the test and confirm failure because the migration file does not yet exist.

### Task 2: Implement the repeatable SQL repair

**Files:**
- Create: `sql/prod_workflow_lowcode_menu_config_repair.sql`

- [ ] Insert or update the stable workflow and low-code menu trees without deleting unrelated rows.
- [ ] Insert missing admin role mappings with `INSERT IGNORE` and a `role_key='admin'` join.
- [ ] Update only the six approved `sys_config.config_key` rows with correct Chinese names and remarks.
- [ ] Add reconciliation queries for menu counts, parent relationships, permissions, role mappings, parameter values, and UTF-8 HEX.
- [ ] Run the focused contract test and relevant menu/SQL health tests.

### Task 3: Review, deploy, verify, and commit

**Files:**
- Test: `scripts/prod-workflow-lowcode-menu-config-repair.test.mjs`
- Deploy: `sql/prod_workflow_lowcode_menu_config_repair.sql`

- [ ] Request independent review and resolve all Critical/Important findings.
- [ ] Execute through `bin/deploy-sql.sh ... prod` so PROD is backed up and the MySQL client uses utf8mb4.
- [ ] Verify exact menu trees, admin grants, parameter text and HEX; clear only menu/config caches.
- [ ] Stage only the migration, contract, design and plan; run GitNexus staged detection and `git diff --cached --check`; commit.
