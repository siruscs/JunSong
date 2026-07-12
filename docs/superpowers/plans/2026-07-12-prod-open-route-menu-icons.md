# PROD Open Route and Menu Icons Implementation Plan

> **For agentic workers:** Execute the checked steps inline; preserve unrelated dirty changes.

**Goal:** Restore the missing PROD Nacos Open gateway routes, confirm the deployed PC bundle uses `/prod-api`, and assign icons to five specified menus.

**Architecture:** Use a repeatable, key-scoped SQL migration for menu metadata and the existing audited PC deployment pipeline for static assets. Verify through production database reconciliation and domain-level HTTP behavior.

**Tech Stack:** MySQL 8, Vue 3/Vite, Nginx, project deployment scripts.

---

### Task 1: Capture failing contracts

- [ ] Add a Node contract asserting the migration is UTF-8, scoped to five stable menu keys, and does not change permissions or routes.
- [ ] Run the contract and confirm it fails because the migration is absent.

### Task 2: Add menu icon migration

- [ ] Create `sql/prod_menu_icon_repair.sql` with guarded updates and before/after reconciliation.
- [ ] Run the focused contract and SQL static checks.

### Task 3: Rebuild and deploy PC frontend

- [ ] Run the production frontend build and confirm `/prod-api` is embedded.
- [ ] Deploy with `bin/deploy-ui.sh prod`.

### Task 4: Deploy SQL and verify PROD

- [ ] Deploy with `bin/deploy-sql.sh sql/prod_menu_icon_repair.sql prod`.
- [ ] Verify five menu rows have expected icons and the domain request no longer returns static-resource 404.
- [ ] Compare PROD Nacos `junsong-gateway-prod.yml` with `docker/nacos/conf/junsong-gateway-prod.yml`; back up `config_info`, restore the four Open routes if absent, restart Nacos and gateway, then require a 401 authentication response instead of 404.

### Task 5: Review and commit

- [ ] Request independent review and resolve Critical/Important findings.
- [ ] Stage only task-owned files, run GitNexus staged change detection and commit.
