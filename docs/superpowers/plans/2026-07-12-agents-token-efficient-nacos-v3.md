# AGENTS Token-Efficient Nacos V3 Implementation Plan

> **For agentic workers:** Execute inline, preserve unrelated dirty changes, and stage only task-owned files.

**Goal:** Reduce always-loaded AGENTS guidance by at least 30% while preserving core boundaries and enforcing Nacos V3-only operations.

**Architecture:** Keep universal rules in root `AGENTS.md`; route task-specific detail to authoritative documents. Add a static contract that rejects Nacos V1 guidance and checks required safety phrases and the root word budget.

**Tech Stack:** Markdown, Node `node:test`, GitNexus staged change detection.

---

### Task 1: Add the failing guidance contract

- [ ] Create `scripts/agents-guidance-contract.test.mjs`.
- [ ] Assert both documents declare Nacos V3 and ban `/nacos/v1/`.
- [ ] Assert `AGENTS.md` retains security, finance, UTF-8, nested-repository and GitNexus boundaries.
- [ ] Assert root guidance contains no more than 1,450 whitespace-delimited words.
- [ ] Run the test and observe failure against current guidance.

### Task 2: Refactor root guidance

- [ ] Preserve the managed GitNexus block.
- [ ] Replace verbose project and specialist sections with compact universal rules and a task-routing table.
- [ ] Add explicit Nacos V3 stop/fallback rules.
- [ ] Run the contract and word-count check.

### Task 3: Update deployment handoff

- [ ] Add a Nacos V3-only operations section with backup, endpoint verification, failure behavior, validation and emergency database-edit requirements.
- [ ] Remove or correct any V1 API examples.
- [ ] Run contract and Markdown diff checks.

### Task 4: Review and commit

- [ ] Request independent review for missing boundaries and ambiguous instructions.
- [ ] Fix all Critical/Important findings.
- [ ] Stage only the plan, contract and two guidance documents.
- [ ] Run GitNexus staged detection and commit.
