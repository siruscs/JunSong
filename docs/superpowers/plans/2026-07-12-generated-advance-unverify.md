# Generated Advance Unverify Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow reversal of a verification batch that generated an unused unverified surplus advance, while preserving strict supplement and source validation.

**Architecture:** Normalize blank verification users only for unverified surplus semantics, and persist generated advance verification metadata explicitly. Keep transaction, period locking, downstream-use checks, and conditional mutations unchanged.

**Tech Stack:** Java 17, Spring transaction services, MyBatis XML, JUnit 5.

---

### Task 1: Reproduce the surplus reversal defect

**Files:**
- Modify: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImplTest.java`

- [ ] Add a test that creates the reversal harness, adds a generated `SURPLUS` advance with status `0`, `verifyBy=""`, and `verifyTime=null`, calls `unverify`, and asserts source/expense restoration, generated invalidation, and batch reversal.
- [ ] Run `mvn -Dtest=FinExpenseVerificationServiceImplTest#unverifyAllowsGeneratedUnverifiedSurplusWithBlankVerifyBy test` and confirm it fails with `借支核销信息已变化，请刷新后重试`.

### Task 2: Implement relation-aware validation and persistence

**Files:**
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinAdvanceMapper.xml`

- [ ] Change surplus validation so status `0`, blank-or-null `verifyBy`, and null `verifyTime` is accepted; keep all other states fail-closed.
- [ ] Add `verify_by` and `verify_time` columns and values to `insertFinAdvance` so generated supplements persist batch metadata and generated surplus rows persist SQL NULL.
- [ ] Run the focused failing test and confirm it passes.

### Task 3: Regression and review

**Files:**
- Test: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImplTest.java`

- [ ] Run `mvn -Dtest=FinExpenseVerificationServiceImplTest,FinExpenseControllerContractTest test` and require all tests to pass.
- [ ] Run the relevant finance package build and mapper/static health checks.
- [ ] Request independent review and resolve every Critical or Important finding.
- [ ] Stage only task-owned hunks, run GitNexus `detect_changes(scope="staged")` when available, and run `git diff --cached --check` before commit.
