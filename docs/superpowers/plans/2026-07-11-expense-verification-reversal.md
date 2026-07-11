# Expense Verification and Reversal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify PC and mini-program expense verification, add advance selection and batch verification to the mini-program, and provide auditable batch-level reversal that is forbidden for locked or carried-forward accounting periods.

**Architecture:** Introduce a verification-batch aggregate with expense and advance detail snapshots. Route both single and batch verification through one transactional domain service, and reverse only a complete batch after permission, ownership, period and downstream-reference checks. PC and mini-program use the same permission codes and APIs; backend validation remains authoritative.

**Tech Stack:** Java 17+, Spring Boot, MyBatis XML, Jakarta Validation, MySQL, Vue 3/Element Plus, uni-app/Vue, JUnit 5, Node health tests.

---

## Safety preflight

The repository currently has unrelated uncommitted changes, including finance backend, PC frontend and the `junsong-miniprogram` nested repository. Before every task:

- Run `git status --short` and inspect the exact target files.
- Preserve all pre-existing edits; do not reset, overwrite or stage unrelated work.
- Before modifying any Java/TypeScript/Vue symbol, run GitNexus `impact({target: "<symbol>", direction: "upstream"})` and report direct callers, affected processes and risk. Stop for user confirmation on HIGH or CRITICAL risk.
- Before each commit, run `detect_changes({scope: "staged"})` and confirm only expected flows are affected.
- For `junsong-miniprogram`, run status, tests and commits from that nested repository; do not accidentally record only a parent-repository gitlink change without the nested commit.

## File map

### Backend files to create

- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinExpenseVerifyBatch.java` — batch header and reversal metadata.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinExpenseVerifyDetail.java` — expense snapshot per batch.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinAdvanceVerifyDetail.java` — source/generated advance snapshot per batch.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/ExpenseUnverifyVO.java` — validated reversal request.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/ExpenseOperationCapabilityVO.java` — server-computed action capability and disabled reason.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinExpenseVerifyBatchMapper.java` — persistence contract for the aggregate.
- `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinExpenseVerifyBatchMapper.xml` — locking, batch and detail SQL.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/IFinExpenseVerificationService.java` — transactional verification boundary.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImpl.java` — verification/reversal rules.
- `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImplTest.java` — service-level behavior tests.
- `sql/finance_expense_verification_batch.sql` — schema, indexes, permissions and safe role grants.
- `sql/finance_expense_verification_history_migration.sql` — repeatable history classification/backfill.

### Backend files to modify

- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/ExpenseVerifyVO.java` — add required idempotency request ID.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinExpenseController.java` — new permissions, unified endpoints and compatibility delegation.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/IFinExpenseService.java` — remove verification ownership after compatibility delegation is established.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinExpenseServiceImpl.java` — delegate old verification methods and retain CRUD/statistics only.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinExpenseMapper.java` and `src/main/resources/mapper/finance/FinExpenseMapper.xml` — conditional state updates and batch lookup fields.
- `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinAdvanceMapper.java` and `src/main/resources/mapper/finance/FinAdvanceMapper.xml` — conditional state updates and generated-record invalidation.

### PC files to modify

- `junsong-ui-v3/src/api/finance/expense.ts` — typed verify/unverify/capability APIs.
- `junsong-ui-v3/src/views/finance/expense/index.vue` — independent permissions, shared dialog and reversal flow.
- `scripts/expense-verified-edit-health.test.mjs` — extend permission and reversal regression checks.

### Mini-program files to modify

- `junsong-miniprogram/src/config/modules.js` — independent verify/unverify permissions and remove direct simple-verify action.
- `junsong-miniprogram/src/pages/list/index.vue` — selectable batch mode and totals.
- `junsong-miniprogram/src/pages/detail/index.vue` — shared single verification and reversal entry.
- `junsong-miniprogram/src/pages/expense-verify/index.vue` — new expense/advance selection and confirmation page.
- `junsong-miniprogram/src/pages.json` — register verification page.
- `junsong-miniprogram/test/expense-verification-permission.test.mjs` — UI permission/route contract tests.

## Task 1: Add schema and independent permissions

**Files:**

- Create: `sql/finance_expense_verification_batch.sql`
- Test: `scripts/expense-verification-schema-health.test.mjs`

- [ ] **Step 1: Write the failing schema health test**

Create a Node test that reads the SQL file and asserts all required constraints and permission codes:

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const sql = fs.readFileSync('sql/finance_expense_verification_batch.sql', 'utf8')

test('verification schema has batch and detail integrity', () => {
  assert.match(sql, /CREATE TABLE IF NOT EXISTS `fin_expense_verify_batch`/)
  assert.match(sql, /UNIQUE KEY `uk_verify_batch_request` \(`tenant_id`, `request_id`\)/)
  assert.match(sql, /CREATE TABLE IF NOT EXISTS `fin_expense_verify_detail`/)
  assert.match(sql, /UNIQUE KEY `uk_verify_expense` \(`batch_id`, `expense_id`\)/)
  assert.match(sql, /CREATE TABLE IF NOT EXISTS `fin_advance_verify_detail`/)
})

test('verification and reversal use separate permissions', () => {
  assert.match(sql, /finance:expense:verify/)
  assert.match(sql, /finance:expense:unverify/)
  assert.doesNotMatch(sql, /费用核销[^;]+finance:expense:edit/s)
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `node --test scripts/expense-verification-schema-health.test.mjs`

Expected: FAIL because the SQL file does not exist.

- [ ] **Step 3: Create the migration SQL**

Use explicit unique keys, tenant/dept indexes and non-destructive permission updates. The essential DDL is:

```sql
CREATE TABLE IF NOT EXISTS `fin_expense_verify_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) NOT NULL,
  `request_id` varchar(64) NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `dept_id` bigint NOT NULL,
  `total_expense_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `total_advance_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `difference_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `status` varchar(16) NOT NULL DEFAULT 'VERIFIED',
  `source_type` varchar(16) NOT NULL DEFAULT 'NORMAL',
  `verify_by` varchar(64) NOT NULL,
  `verify_time` datetime NOT NULL,
  `reverse_by` varchar(64) DEFAULT NULL,
  `reverse_time` datetime DEFAULT NULL,
  `reverse_reason` varchar(500) DEFAULT NULL,
  `reverse_request_id` varchar(64) DEFAULT NULL,
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_verify_batch_no` (`tenant_id`, `batch_no`),
  UNIQUE KEY `uk_verify_batch_request` (`tenant_id`, `request_id`),
  UNIQUE KEY `uk_verify_reverse_request` (`tenant_id`, `reverse_request_id`),
  KEY `idx_verify_batch_dept_status` (`tenant_id`, `dept_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用核销批次';
```

Create both detail tables with snapshot fields from the design, foreign-key-style indexes without database foreign keys (consistent with the existing project), and `generated_flag`/`relation_type` on advance details. Insert/update menu buttons by permission code, and grant permissions only to intended finance roles; do not infer role IDs from names without a guarded `SELECT`.

- [ ] **Step 4: Run the schema health test**

Run: `node --test scripts/expense-verification-schema-health.test.mjs`

Expected: PASS.

- [ ] **Step 5: Commit only schema and its test**

```bash
git add sql/finance_expense_verification_batch.sql scripts/expense-verification-schema-health.test.mjs
git commit -m "feat(finance): add expense verification batch schema"
```

## Task 2: Add batch aggregate models and mapper

**Files:**

- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinExpenseVerifyBatch.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinExpenseVerifyDetail.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/FinAdvanceVerifyDetail.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinExpenseVerifyBatchMapper.java`
- Create: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinExpenseVerifyBatchMapper.xml`
- Test: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/mapper/FinExpenseVerifyBatchMapperContractTest.java`

- [ ] **Step 1: Write a failing mapper contract test**

The test reads the mapper XML and ensures lock/idempotency/reversal statements exist:

```java
@Test
void mapperDefinesRequiredBatchOperations() throws Exception {
    String xml = Files.readString(Path.of("src/main/resources/mapper/finance/FinExpenseVerifyBatchMapper.xml"));
    assertTrue(xml.contains("id=\"selectByRequestId\""));
    assertTrue(xml.contains("id=\"selectBatchForUpdate\""));
    assertTrue(xml.contains("FOR UPDATE"));
    assertTrue(xml.contains("id=\"markBatchReversed\""));
    assertTrue(xml.contains("status = 'VERIFIED'"));
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run from `junsong-modules/junsong-finance`:

`mvn -Dtest=FinExpenseVerifyBatchMapperContractTest test`

Expected: FAIL because the mapper XML is missing.

- [ ] **Step 3: Implement focused domain classes**

Use JavaBeans matching existing domain conventions. Define constants on the batch:

```java
public static final String STATUS_VERIFIED = "VERIFIED";
public static final String STATUS_REVERSED = "REVERSED";
public static final String SOURCE_NORMAL = "NORMAL";
public static final String SOURCE_LEGACY = "LEGACY";
```

Define advance relation constants:

```java
public static final String RELATION_SOURCE = "SOURCE";
public static final String RELATION_SUPPLEMENT = "SUPPLEMENT";
public static final String RELATION_SURPLUS = "SURPLUS";
```

Keep the three classes separate; do not add verification behavior to `FinExpense` or `FinAdvance`.

- [ ] **Step 4: Implement mapper interface and XML**

Required interface methods:

```java
FinExpenseVerifyBatch selectByRequestId(@Param("tenantId") Long tenantId,
                                        @Param("requestId") String requestId);
FinExpenseVerifyBatch selectBatchForUpdate(@Param("batchId") Long batchId,
                                           @Param("tenantId") Long tenantId);
int insertBatch(FinExpenseVerifyBatch batch);
int insertExpenseDetails(@Param("items") List<FinExpenseVerifyDetail> items);
int insertAdvanceDetails(@Param("items") List<FinAdvanceVerifyDetail> items);
List<FinExpenseVerifyDetail> selectExpenseDetails(Long batchId);
List<FinAdvanceVerifyDetail> selectAdvanceDetails(Long batchId);
int markBatchReversed(@Param("batchId") Long batchId,
                      @Param("version") Integer version,
                      @Param("reverseBy") String reverseBy,
                      @Param("reverseTime") Date reverseTime,
                      @Param("reason") String reason,
                      @Param("requestId") String requestId);
```

`markBatchReversed` must update only when `status='VERIFIED' AND version=#{version}` and increment `version`.

- [ ] **Step 5: Run mapper contract test and module compile**

Run:

```bash
mvn -Dtest=FinExpenseVerifyBatchMapperContractTest test
mvn -DskipTests compile
```

Expected: PASS and BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/Fin*Verify*.java \
  junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinExpenseVerifyBatchMapper.java \
  junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinExpenseVerifyBatchMapper.xml \
  junsong-modules/junsong-finance/src/test/java/com/junsong/finance/mapper/FinExpenseVerifyBatchMapperContractTest.java
git commit -m "feat(finance): add verification batch aggregate"
```

## Task 3: Implement transactional unified verification

**Files:**

- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/ExpenseVerifyVO.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/IFinExpenseVerificationService.java`
- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinExpenseMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinExpenseMapper.xml`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinAdvanceMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinAdvanceMapper.xml`
- Test: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImplTest.java`

- [ ] **Step 1: Write failing service tests**

Use hand-written fake mappers, consistent with existing finance tests. Cover at minimum:

```java
@Test void verifyWithoutAdvanceCreatesBatchAndVerifiesExpenses() { /* assert one batch, no advance detail */ }
@Test void equalAmountsVerifySourceAdvancesWithoutGeneratedRecord() { /* 100 == 100 */ }
@Test void expenseGreaterThanAdvanceCreatesSupplementDetail() { /* 150 > 100 */ }
@Test void expenseLessThanAdvanceCreatesSurplusDetail() { /* 80 < 100 */ }
@Test void duplicateRequestReturnsExistingResultWithoutWritingAgain() { /* same requestId twice */ }
@Test void verifiedExpenseRejectsEntireBatch() { /* assert no mapper update */ }
@Test void recordsFromDifferentDepartmentsAreRejected() { /* assert ServiceException */ }
@Test void lockedPeriodRejectsBeforeAnyWrite() { /* fake assertPeriodEditable throws */ }
@Test void conditionalUpdateConflictRollsBackBatch() { /* update count 0 -> conflict */ }
```

- [ ] **Step 2: Run tests and verify they fail**

Run: `mvn -Dtest=FinExpenseVerificationServiceImplTest test`

Expected: FAIL because the service does not exist.

- [ ] **Step 3: Extend the request contract**

Add:

```java
@NotBlank(message = "请求编号不能为空")
@Size(max = 64, message = "请求编号长度不能超过64个字符")
private String requestId;
```

Keep `advanceIds` optional. Reject duplicates in the service with `new HashSet<>(ids).size() != ids.size()`.

- [ ] **Step 4: Add conditional state mapper methods**

```java
int markExpenseVerified(@Param("expenseId") Long expenseId,
                        @Param("advanceId") Long advanceId,
                        @Param("verifyBy") String verifyBy,
                        @Param("verifyTime") Date verifyTime);
int restoreExpenseUnverified(@Param("expenseId") Long expenseId);
int markAdvanceVerified(@Param("advanceId") Long advanceId,
                        @Param("verifyBy") String verifyBy,
                        @Param("verifyTime") Date verifyTime);
int restoreAdvanceStatus(@Param("advanceId") Long advanceId,
                         @Param("status") String originalStatus,
                         @Param("verifyBy") String verifyBy,
                         @Param("verifyTime") Date verifyTime);
```

The verification SQL must include `AND status='0' AND del_flag='0'`; a returned count of zero is a concurrency conflict.

- [ ] **Step 5: Implement the service boundary**

```java
public interface IFinExpenseVerificationService {
    Long verify(ExpenseVerifyVO request, String operator);
    int unverify(Long batchId, ExpenseUnverifyVO request, String operator);
    ExpenseOperationCapabilityVO getCapability(Long expenseId);
}
```

Implement `verify` with `@Transactional(rollbackFor = Exception.class)`. It must:

- Return the existing batch ID when the same tenant/request ID is replayed.
- Require every requested ID to be returned from tenant/data-scoped queries.
- Require a single department per batch.
- Call `finAccountingPeriodService.assertPeriodEditable(periodId)` for every non-null involved period.
- Insert header and snapshots before state mutation.
- Use conditional updates and throw `ServiceException("费用状态已变化，请刷新后重试")` on zero updates.
- Insert generated supplement/surplus advances and capture the generated primary key in advance detail.
- Record an audit event containing batch number, selected IDs and totals.

- [ ] **Step 6: Run focused tests**

Run: `mvn -Dtest=FinExpenseVerificationServiceImplTest test`

Expected: all verification tests PASS.

- [ ] **Step 7: Run finance module tests**

Run: `mvn test`

Expected: BUILD SUCCESS with no existing finance regression.

- [ ] **Step 8: Commit**

Stage only files listed in this task and commit:

`git commit -m "feat(finance): unify expense verification service"`

## Task 4: Implement batch-level reversal and locked-period prohibition

**Files:**

- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/ExpenseUnverifyVO.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImpl.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinExpenseMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinExpenseMapper.xml`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinAdvanceMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinAdvanceMapper.xml`
- Test: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinExpenseVerificationServiceImplTest.java`

- [ ] **Step 1: Add failing reversal tests**

```java
@Test void unverifyRestoresExpenseAndSourceAdvanceSnapshots() { /* assert status and verify fields */ }
@Test void unverifyInvalidatesGeneratedSupplement() { /* generated record becomes del_flag=2 */ }
@Test void unverifyInvalidatesGeneratedSurplus() { /* generated record becomes del_flag=2 */ }
@Test void lockedExpensePeriodForbidsUnverifyWithoutWrites() { /* assert exact error */ }
@Test void carriedForwardAdvancePeriodForbidsUnverifyWithoutWrites() { /* assert exact error */ }
@Test void downstreamUseOfGeneratedAdvanceForbidsUnverify() { /* reference count > 0 */ }
@Test void legacyBatchForbidsAutomaticUnverify() { /* sourceType LEGACY */ }
@Test void alreadyReversedBatchIsIdempotentForSameRequest() { /* no second writes */ }
@Test void optimisticConflictRollsBackAllRestores() { /* markBatchReversed=0 */ }
```

- [ ] **Step 2: Run reversal tests and verify failure**

Run: `mvn -Dtest=FinExpenseVerificationServiceImplTest test`

Expected: new reversal cases FAIL.

- [ ] **Step 3: Implement the validated request**

```java
public class ExpenseUnverifyVO {
    @NotBlank(message = "反核销原因不能为空")
    @Size(max = 500, message = "反核销原因长度不能超过500个字符")
    private String reason;

    @NotBlank(message = "请求编号不能为空")
    @Size(max = 64, message = "请求编号长度不能超过64个字符")
    private String requestId;
    // getters and setters
}
```

- [ ] **Step 4: Add downstream-reference checks**

Add mapper queries that answer whether a generated advance is referenced by another active verification detail or later business record. Exclude the current batch and logically deleted rows. A positive count must produce:

```java
throw new ServiceException("核销生成的借支记录已被后续业务使用，不能反核销");
```

- [ ] **Step 5: Implement reversal in strict order**

Inside `@Transactional(rollbackFor = Exception.class)`:

1. Load batch with `FOR UPDATE` and tenant constraint.
2. Handle same reverse request ID idempotently; reject a different request on an already reversed batch.
3. Reject `SOURCE_LEGACY`.
4. Load all expense and advance details.
5. Call `assertPeriodEditable` for every distinct period before the first update. This call is the mandatory locked/carried-forward prohibition.
6. Validate current statuses and downstream references.
7. Restore expenses and source advances from snapshots.
8. Logically invalidate generated supplement/surplus rows.
9. Optimistically mark the batch reversed and write audit history.

- [ ] **Step 6: Run focused and full module tests**

```bash
mvn -Dtest=FinExpenseVerificationServiceImplTest test
mvn test
```

Expected: PASS.

- [ ] **Step 7: Commit**

`git commit -m "feat(finance): add auditable expense reversal"`

## Task 5: Expose secure APIs and operation capability

**Files:**

- Create: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/ExpenseOperationCapabilityVO.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinExpenseController.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/IFinExpenseService.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinExpenseServiceImpl.java`
- Test: `junsong-modules/junsong-finance/src/test/java/com/junsong/finance/controller/FinExpenseControllerContractTest.java`

- [ ] **Step 1: Write a failing controller contract test**

Assert the source contains exact independent permissions and endpoints:

```java
assertTrue(source.contains("@RequiresPermissions(\"finance:expense:verify\")"));
assertTrue(source.contains("@PutMapping(\"/batchVerify\")"));
assertTrue(source.contains("@RequiresPermissions(\"finance:expense:unverify\")"));
assertTrue(source.contains("@PutMapping(\"/unverify/{batchId}\")"));
assertTrue(source.contains("@GetMapping(\"/{expenseId}/capability\")"));
```

Also assert the method bodies delegate to `finExpenseVerificationService` rather than the old state-only implementation.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -Dtest=FinExpenseControllerContractTest test`

Expected: FAIL on missing permission/endpoints.

- [ ] **Step 3: Add capability VO and service logic**

```java
public class ExpenseOperationCapabilityVO {
    private boolean canVerify;
    private boolean canUnverify;
    private Long batchId;
    private String operationDisabledReason;
    // getters and setters
}
```

The service combines current status, batch source/status, period editability and downstream-reference checks. It must not replace submission-time validation.

- [ ] **Step 4: Update controller endpoints**

```java
@RequiresPermissions("finance:expense:verify")
@PutMapping("/batchVerify")
public AjaxResult batchVerify(@Validated @RequestBody ExpenseVerifyVO request) {
    return success(finExpenseVerificationService.verify(request, SecurityUtils.getUsername()));
}

@RequiresPermissions("finance:expense:unverify")
@PutMapping("/unverify/{batchId}")
public AjaxResult unverify(@PathVariable Long batchId,
                           @Validated @RequestBody ExpenseUnverifyVO request) {
    return toAjax(finExpenseVerificationService.unverify(batchId, request, SecurityUtils.getUsername()));
}
```

Keep `/verify/{expenseId}` only as a temporary compatibility endpoint protected by `finance:expense:verify`; generate a server request ID and delegate to the unified service. Mark it deprecated in code and API documentation.

- [ ] **Step 5: Run controller and module tests**

```bash
mvn -Dtest=FinExpenseControllerContractTest test
mvn test
```

Expected: PASS.

- [ ] **Step 6: Commit**

`git commit -m "feat(finance): expose secure verification APIs"`

## Task 6: Update PC verification and reversal UI

**Files:**

- Modify: `junsong-ui-v3/src/api/finance/expense.ts`
- Modify: `junsong-ui-v3/src/views/finance/expense/index.vue`
- Modify: `scripts/expense-verified-edit-health.test.mjs`

- [ ] **Step 1: Extend the failing health test**

Assert:

```js
assert.match(view, /finance:expense:verify/)
assert.match(view, /finance:expense:unverify/)
assert.doesNotMatch(view, /handleBatchVerify[^]*finance:expense:edit/)
assert.match(api, /url: '\/finance\/expense\/unverify\/'/)
assert.match(view, /反核销原因/)
```

- [ ] **Step 2: Run and verify failure**

Run: `node --test scripts/expense-verified-edit-health.test.mjs`

Expected: FAIL because independent permissions and reversal UI are missing.

- [ ] **Step 3: Add typed API methods**

```ts
export interface ExpenseVerifyRequest {
  expenseIds: number[]
  advanceIds: number[]
  requestId: string
}

export interface ExpenseUnverifyRequest {
  reason: string
  requestId: string
}

export function unverifyExpense(batchId: number, data: ExpenseUnverifyRequest) {
  return request({ url: `/finance/expense/unverify/${batchId}`, method: 'put', data })
}
```

Use `crypto.randomUUID()` when available with a timestamp/random fallback already compatible with supported browsers.

- [ ] **Step 4: Replace permission directives and share the verification dialog**

- Single verification initializes `expenseIds: [row.expenseId]` and opens the same advance-selection dialog as batch verification.
- Both actions use `v-hasPermi="['finance:expense:verify']"`.
- Do not invoke the legacy `verifyExpense(expenseId)` API.
- Preserve existing selected-expense validation and refresh summary/list after success.

- [ ] **Step 5: Add reversal dialog**

Show “反核销” only for verified rows with `finance:expense:unverify`. Fetch capability before opening; if `canUnverify` is false, show `operationDisabledReason`. Require a trimmed reason and call `unverifyExpense(capability.batchId, request)`.

- [ ] **Step 6: Run tests and production build**

```bash
node --test scripts/expense-verified-edit-health.test.mjs
cd junsong-ui-v3 && npm run build
```

Expected: health tests PASS and build succeeds.

- [ ] **Step 7: Commit only PC changes**

`git commit -m "feat(ui): unify expense verification and reversal"`

## Task 7: Add mini-program permissions, advance selection and batch verification

**Files:**

- Modify: `junsong-miniprogram/src/config/modules.js`
- Modify: `junsong-miniprogram/src/pages/list/index.vue`
- Modify: `junsong-miniprogram/src/pages/detail/index.vue`
- Create: `junsong-miniprogram/src/pages/expense-verify/index.vue`
- Modify: `junsong-miniprogram/src/pages.json`
- Create: `junsong-miniprogram/test/expense-verification-permission.test.mjs`

- [ ] **Step 1: Write the failing mini-program contract test**

```js
test('expense permissions are independent', () => {
  assert.match(modules, /verify:\s*'finance:expense:verify'/)
  assert.match(modules, /unverify:\s*'finance:expense:unverify'/)
  assert.doesNotMatch(modules, /verify:\s*'finance:expense:edit'/)
})

test('expense verification page uses batch API and advance list', () => {
  assert.match(page, /\/finance\/expense\/batchVerify/)
  assert.match(page, /\/finance\/expense\/unverifiedAdvances/)
  assert.match(page, /requestId/)
})
```

- [ ] **Step 2: Run and verify failure**

From `junsong-miniprogram` run:

`node --test test/expense-verification-permission.test.mjs`

Expected: FAIL.

- [ ] **Step 3: Split permissions in module configuration**

```js
permissions: {
  ...crudPermissions('expense'),
  verify: 'finance:expense:verify',
  unverify: 'finance:expense:unverify'
}
```

Remove the direct `/verify/{id}` custom action so the detail page cannot bypass advance selection.

- [ ] **Step 4: Implement list batch-selection mode**

Add state `batchSelecting`, `selectedExpenseIds` and computed selected total. The batch entry is visible only when `hasActionPermission('expense', 'verify')`. Disable verified cards. Navigate with encoded IDs:

```js
uni.navigateTo({
  url: `/pages/expense-verify/index?expenseIds=${this.selectedExpenseIds.join(',')}`
})
```

- [ ] **Step 5: Implement shared verification page**

On load:

- Parse and de-duplicate numeric expense IDs.
- Fetch each expense or a batch-summary endpoint and reject any verified item.
- Fetch `/finance/expense/unverifiedAdvances` using the current department.
- Render selectable advances, expense total, advance total, signed difference and exact supplement/surplus explanation.
- Submit `{ expenseIds, advanceIds, requestId }` to `/finance/expense/batchVerify`.
- Disable the submit button while pending and preserve state on business failure.

- [ ] **Step 6: Route detail actions through shared page**

For an unverified expense with verify permission, navigate using the single expense ID. For a verified expense with unverify permission, fetch capability, require a reason in a modal/form, and call the reversal endpoint only when `canUnverify` is true.

- [ ] **Step 7: Run mini-program tests and build**

Use the scripts declared in `junsong-miniprogram/package.json`; first inspect them with `npm run`. Then run the exact available test and WeChat build commands. Expected: contract test PASS and uni-app build completes without template/compiler errors.

- [ ] **Step 8: Commit in nested repository**

From `junsong-miniprogram`:

```bash
git add src/config/modules.js src/pages/list/index.vue src/pages/detail/index.vue \
  src/pages/expense-verify/index.vue src/pages.json test/expense-verification-permission.test.mjs
git commit -m "feat(miniprogram): add secure batch expense verification"
```

Then, only if the parent intentionally tracks the nested-repository commit, stage the `junsong-miniprogram` gitlink separately and commit it in the parent.

## Task 8: Backfill history, integrate and verify end to end

**Files:**

- Create: `sql/finance_expense_verification_history_migration.sql`
- Create: `scripts/expense-verification-history-health.test.mjs`
- Modify: `docs/superpowers/specs/2026-07-11-expense-verification-reversal-design.md` only if implementation decisions differ and user approves the amendment.

- [ ] **Step 1: Write failing migration safety test**

Assert the script:

- Uses a deterministic migration request ID.
- Inserts unambiguous historical relationships as `source_type='NORMAL'`.
- Inserts ambiguous relationships as `source_type='LEGACY'`.
- Uses `NOT EXISTS` for repeat execution.
- Produces before/after reconciliation result sets for verified expense amount and unverified advance amount.

- [ ] **Step 2: Run and verify failure**

Run: `node --test scripts/expense-verification-history-health.test.mjs`

Expected: FAIL because migration is missing.

- [ ] **Step 3: Implement repeatable history migration**

Backfill only relationships that can be proven from existing IDs and status. Do not guess multi-expense/multi-advance grouping. Classify every unresolved verified expense into a `LEGACY` batch so the service can explicitly deny automatic reversal.

- [ ] **Step 4: Run static migration tests**

Run: `node --test scripts/expense-verification-history-health.test.mjs scripts/expense-verification-schema-health.test.mjs`

Expected: PASS.

- [ ] **Step 5: Run all focused regression suites**

```bash
cd junsong-modules/junsong-finance && mvn test
cd ../../../junsong-ui-v3 && npm run build
cd ../junsong-miniprogram && node --test test/expense-verification-permission.test.mjs
cd .. && node --test scripts/expense-verification-*.test.mjs scripts/expense-verified-edit-health.test.mjs
```

Expected: all tests/builds PASS.

- [ ] **Step 6: Perform API acceptance checks in a test environment**

Verify with separate store-manager and finance accounts:

1. Store manager receives 403/permission error for verify and unverify APIs.
2. Finance user can verify one expense with no advance.
3. Finance user can batch verify with equal, supplement and surplus cases.
4. Same request ID creates only one batch.
5. Finance supervisor can reverse an editable-period batch.
6. Locked and carried-forward period batches return a business error and retain all statuses.
7. Generated advance with a downstream reference blocks reversal.
8. PC and mini-program show the same operation capability for the same record.

- [ ] **Step 7: Run GitNexus change detection before final commit**

Run `detect_changes({scope: "compare", base_ref: "master"})`. Review every affected finance execution flow and verify no unrelated symbol is included.

- [ ] **Step 8: Commit migration and verification assets**

```bash
git add sql/finance_expense_verification_history_migration.sql \
  scripts/expense-verification-history-health.test.mjs
git commit -m "feat(finance): backfill expense verification history"
```

## Final release checklist

- [ ] Apply schema migration before deploying backend.
- [ ] Deploy backend before PC and mini-program clients so old clients remain compatible.
- [ ] Run migration in preview/report mode and reconcile totals before applying writes.
- [ ] Review role grants; store-manager and supervisor roles must not receive verify/unverify by default.
- [ ] Confirm locked and carried-forward periods reject reversal at API level.
- [ ] Confirm audit records contain batch, operator, reason, request ID and before/after snapshots.
- [ ] Observe one release window, then schedule removal of `/verify/{expenseId}` compatibility endpoint.
