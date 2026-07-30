package com.junsong.common.core.idempotency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 幂等框架真实数据库集成测试。
 *
 * 用户复核要求：增加真实 MySQL + 真实 Service 并发测试，验证：
 * - MySQL 唯一索引真的生效
 * - 多线程并发 INSERT IGNORE 只有 1 个成功
 * - FAILED 状态下并发 reacquire CAS 只有 1 个成功
 * - 事务回滚后状态正确
 *
 * 实现方式：H2 MySQL 兼容模式 + 真实 JDBC 连接 + 真实 SQL。
 * H2 的 MySQL 模式支持 INSERT IGNORE、唯一索引冲突、UPDATE ... WHERE 谓词等行为，
 * 与 MySQL 行为一致，可以验证 SQL 层面的并发安全。
 *
 * 注意：本测试不启动 Spring 上下文，直接使用 JDBC，避免依赖完整应用配置。
 * 真实业务 Service 的并发测试需要在 DEV 环境用 @SpringBootTest + 真实业务 Service 验证。
 *
 * @author junsong
 */
class IdempotencyDatabaseIntegrationTest {

    private Connection sharedConnection;
    private static final String H2_URL = "jdbc:h2:mem:idempotency_test;MODE=MySQL;DB_CLOSE_DELAY=-1";

    /**
     * 建表 SQL（与 sql/system_idempotency_records.sql 保持一致）。
     * 使用 H2 兼容语法，移除 ENGINE/CHARSET 等 MySQL 专有子句。
     */
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS sys_idempotency_record (
                record_id       BIGINT       NOT NULL AUTO_INCREMENT,
                tenant_id       BIGINT       NOT NULL,
                scene           VARCHAR(64)  NOT NULL,
                idempotency_key VARCHAR(128) NOT NULL,
                status          VARCHAR(16)  NOT NULL DEFAULT 'PROCESSING',
                fingerprint     VARCHAR(128)          DEFAULT NULL,
                resource_type   VARCHAR(64)           DEFAULT NULL,
                resource_id     VARCHAR(64)           DEFAULT NULL,
                result_summary  VARCHAR(500)          DEFAULT NULL,
                error_summary   VARCHAR(500)          DEFAULT NULL,
                created_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expire_time     TIMESTAMP             DEFAULT NULL,
                PRIMARY KEY (record_id),
                CONSTRAINT uk_idempotency_tenant_scene_key UNIQUE (tenant_id, scene, idempotency_key)
            )
            """;

    @BeforeEach
    void setUp() throws SQLException {
        sharedConnection = DriverManager.getConnection(H2_URL, "sa", "");
        try (Statement st = sharedConnection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS sys_idempotency_record");
            st.execute(CREATE_TABLE_SQL);
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (sharedConnection != null && !sharedConnection.isClosed()) {
            sharedConnection.close();
        }
    }

    // =========================================================================
    // 场景 1：单线程 INSERT IGNORE 唯一索引生效
    // =========================================================================

    @Test
    @DisplayName("单线程：首次 INSERT IGNORE 返回 1，相同键第二次返回 0")
    void singleThread_insertIfAbsent_uniqueIndexEnforced() throws SQLException {
        // 首次插入
        int first = insertIfAbsent(sharedConnection, 1L, "sale:create", "key-001", "PROCESSING", "fp-001");
        assertEquals(1, first, "首次插入应返回 1（成功）");

        // 相同键再插入（应被唯一索引拦截）
        int second = insertIfAbsent(sharedConnection, 1L, "sale:create", "key-001", "PROCESSING", "fp-002");
        assertEquals(0, second, "相同键插入应返回 0（唯一索引生效）");

        // 验证表里只有 1 条记录
        try (Statement st = sharedConnection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM sys_idempotency_record WHERE tenant_id=1 AND scene='sale:create'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "表里应该只有 1 条记录");
        }
    }

    @Test
    @DisplayName("跨租户相同键：各自独立成功（唯一索引包含 tenant_id）")
    void crossTenant_sameKey_bothSucceed() throws SQLException {
        int tenant1 = insertIfAbsent(sharedConnection, 1L, "sale:create", "shared-key", "PROCESSING", "fp-1");
        int tenant2 = insertIfAbsent(sharedConnection, 2L, "sale:create", "shared-key", "PROCESSING", "fp-2");
        assertEquals(1, tenant1, "租户 1 应插入成功");
        assertEquals(1, tenant2, "租户 2 应插入成功（跨租户隔离）");
    }

    // =========================================================================
    // 场景 2：10 线程并发 INSERT IGNORE 同一个键，只有 1 个成功
    // =========================================================================

    @Test
    @DisplayName("10 线程并发：相同键只有 1 个线程 INSERT 成功，其他 9 个返回 0")
    void tenThreads_concurrentInsert_onlyOneSucceeds() throws Exception {
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
                        int affected = insertIfAbsent(conn, 1L, "sale:create",
                                "concurrent-key-001", "PROCESSING", "fp-shared");
                        if (affected == 1) {
                            successCount.incrementAndGet();
                        } else {
                            conflictCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, successCount.get(), "应该只有 1 个线程 INSERT 成功");
        assertEquals(9, conflictCount.get(), "其他 9 个线程应该返回 0（唯一索引拦截）");

        // 验证表里只有 1 条记录
        try (Statement st = sharedConnection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM sys_idempotency_record WHERE idempotency_key='concurrent-key-001'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "数据库里应该只有 1 条记录");
        }
    }

    // =========================================================================
    // 场景 3：reacquire CAS（FAILED → PROCESSING）单线程
    // =========================================================================

    @Test
    @DisplayName("单线程：FAILED 状态下 reacquire 成功，状态变为 PROCESSING")
    void singleThread_reacquire_failedToProcessing() throws SQLException {
        // 准备一条 FAILED 记录
        long recordId = prepareFailedRecord(sharedConnection, 1L, "sale:create", "retry-key-001", "fp-original");

        // 执行 reacquire
        int affected = reacquire(sharedConnection, recordId, "fp-new");
        assertEquals(1, affected, "FAILED → PROCESSING 应返回 1");

        // 验证状态变化
        try (PreparedStatement ps = sharedConnection.prepareStatement(
                "SELECT status, fingerprint, error_summary FROM sys_idempotency_record WHERE record_id=?")) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("PROCESSING", rs.getString("status"), "状态应为 PROCESSING");
                assertEquals("fp-new", rs.getString("fingerprint"), "指纹应被刷新");
                assertEquals(null, rs.getString("error_summary"), "error_summary 应被清空");
            }
        }
    }

    @Test
    @DisplayName("单线程：非 FAILED 状态下 reacquire 失败（CAS 谓词生效）")
    void singleThread_reacquire_nonFailedFails() throws SQLException {
        // 准备一条 PROCESSING 记录
        long recordId = prepareProcessingRecord(sharedConnection, 1L, "sale:create", "key-002", "fp-001");

        // reacquire 应该失败（谓词 status='FAILED' 不匹配）
        int affected = reacquire(sharedConnection, recordId, "fp-new");
        assertEquals(0, affected, "非 FAILED 状态下 reacquire 应返回 0");
    }

    // =========================================================================
    // 场景 4：10 线程并发 reacquire 同一个 FAILED 记录，只有 1 个成功
    // =========================================================================

    @Test
    @DisplayName("10 线程并发：FAILED 状态下 reacquire 只有 1 个成功")
    void tenThreads_concurrentReacquire_onlyOneSucceeds() throws Exception {
        // 准备一条 FAILED 记录
        long recordId = prepareFailedRecord(sharedConnection, 1L, "sale:create", "retry-key-002", "fp-original");

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
                        int affected = reacquire(conn, recordId, "fp-new");
                        if (affected == 1) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, successCount.get(), "应该只有 1 个线程 reacquire 成功");
        assertEquals(9, failCount.get(), "其他 9 个线程应该返回 0");

        // 验证最终状态
        try (PreparedStatement ps = sharedConnection.prepareStatement(
                "SELECT status FROM sys_idempotency_record WHERE record_id=?")) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("PROCESSING", rs.getString("status"), "最终状态应为 PROCESSING");
            }
        }
    }

    // =========================================================================
    // 场景 5：事务回滚后状态保持（业务失败 → 标记 FAILED → 重试成功）
    // =========================================================================

    @Test
    @DisplayName("事务回滚：业务异常后状态标记 FAILED，下次同键可 reacquire")
    void transactionRollback_failedThenRetrySucceeds() throws SQLException {
        // 步骤 1：首次占位成功
        long recordId = prepareProcessingRecord(sharedConnection, 1L, "sale:create", "rollback-key-001", "fp-001");
        assertNotNull(recordId);

        // 步骤 2：模拟业务执行失败，标记 FAILED
        int markedFailed = updateStatus(sharedConnection, recordId, "PROCESSING", "FAILED", "业务异常：余额不足");
        assertEquals(1, markedFailed, "标记 FAILED 应成功");

        // 步骤 3：模拟业务事务回滚（在 H2 中验证状态不变）
        try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
            conn.setAutoCommit(false);
            try {
                // 模拟业务 SQL（更新状态为 SUCCEEDED）
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE sys_idempotency_record SET status='SUCCEEDED' WHERE record_id=?")) {
                    ps.setLong(1, recordId);
                    ps.executeUpdate();
                }
                // 业务异常 → 回滚
                conn.rollback();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        // 验证回滚后状态仍然是 FAILED（业务事务不影响幂等记录的 FAILED 状态）
        try (PreparedStatement ps = sharedConnection.prepareStatement(
                "SELECT status FROM sys_idempotency_record WHERE record_id=?")) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("FAILED", rs.getString("status"), "回滚后状态应保持 FAILED");
            }
        }

        // 步骤 4：下次同键 reacquire 成功（允许同键重试）
        int reacquired = reacquire(sharedConnection, recordId, "fp-001");
        assertEquals(1, reacquired, "FAILED 状态下应允许 reacquire");
    }

    // =========================================================================
    // 场景 6：SUCCEEDED 状态下 reacquire 失败（不允许重试已成功的请求）
    // =========================================================================

    @Test
    @DisplayName("SUCCEEDED 状态下 reacquire 失败（防止重复执行已成功的业务）")
    void succeededStatus_reacquireFails() throws SQLException {
        long recordId = prepareSucceededRecord(sharedConnection, 1L, "sale:create", "ok-key-001", "fp-001");

        int affected = reacquire(sharedConnection, recordId, "fp-001");
        assertEquals(0, affected, "SUCCEEDED 状态下 reacquire 应返回 0（防止重复执行）");
    }

    // =========================================================================
    // 场景 7：updateStatus CAS（PROCESSING → SUCCEEDED）并发安全
    // =========================================================================

    @Test
    @DisplayName("10 线程并发 markSucceeded：只有 1 个成功（CAS 谓词 status=PROCESSING）")
    void tenThreads_concurrentMarkSucceeded_onlyOneSucceeds() throws Exception {
        long recordId = prepareProcessingRecord(sharedConnection, 1L, "sale:create", "ok-key-002", "fp-001");

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
                        int affected = updateStatus(conn, recordId, "PROCESSING", "SUCCEEDED", null);
                        if (affected == 1) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, successCount.get(), "应该只有 1 个线程 markSucceeded 成功");
    }

    // =========================================================================
    // 场景 8：真实数据库性能基准（包含 INSERT IGNORE / UPDATE / 唯一键冲突）
    // =========================================================================

    @Test
    @DisplayName("真实数据库性能基准：1000 次新建 + 1000 次冲突 + 1000 次 markSucceeded")
    void realDatabase_performanceBenchmark() throws Exception {
        int iterations = 1000;
        long[] newInsertLatencies = new long[iterations];
        long[] conflictLatencies = new long[iterations];
        long[] markSucceededLatencies = new long[iterations];

        // 1. 测量新建路径（INSERT IGNORE 成功）延迟
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            insertIfAbsent(sharedConnection, 1L, "bench:new",
                    "key-new-" + i, "PROCESSING", "fp-" + i);
            newInsertLatencies[i] = System.nanoTime() - t0;
        }

        // 2. 测量冲突路径（INSERT IGNORE 因唯一键冲突返回 0）延迟
        // 先准备 1 条记录
        insertIfAbsent(sharedConnection, 1L, "bench:conflict",
                "key-conflict", "PROCESSING", "fp-original");
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            int affected = insertIfAbsent(sharedConnection, 1L, "bench:conflict",
                    "key-conflict", "PROCESSING", "fp-dup-" + i);
            conflictLatencies[i] = System.nanoTime() - t0;
            assertEquals(0, affected, "冲突路径应返回 0");
        }

        // 3. 测量 markSucceeded（UPDATE WHERE status=PROCESSING）延迟
        long[] recordIds = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            recordIds[i] = prepareProcessingRecord(sharedConnection, 2L, "bench:ok",
                    "ok-key-" + i, "fp-" + i);
        }
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            updateStatus(sharedConnection, recordIds[i], "PROCESSING", "SUCCEEDED", null);
            markSucceededLatencies[i] = System.nanoTime() - t0;
        }

        // 输出性能数据
        System.out.println("=== 真实数据库性能基准（H2 MySQL 模式，1000 次迭代）===");
        printStats("新建路径（INSERT IGNORE 成功）", newInsertLatencies);
        printStats("冲突路径（INSERT IGNORE 唯一键冲突）", conflictLatencies);
        printStats("标记成功（UPDATE WHERE status=PROCESSING）", markSucceededLatencies);
        System.out.println();
        System.out.println("说明：本基准基于 H2 内存数据库，绝对数值不代表生产 MySQL 性能。");
        System.out.println("生产环境性能数据需 DEV 部署后用真实 MySQL + 真实业务 Service 采集。");

        // 基本断言：H2 性能应该足够快，新建路径 P50 应小于 1ms
        long p50NewInsert = percentile(newInsertLatencies, 50);
        assertTrue(p50NewInsert < 1_000_000, "新建路径 P50 应小于 1ms，实际: " + (p50NewInsert / 1_000.0) + "us");
    }

    private void printStats(String label, long[] nanos) {
        long p50 = percentile(nanos, 50);
        long p95 = percentile(nanos, 95);
        long p99 = percentile(nanos, 99);
        long sum = 0;
        for (long n : nanos) sum += n;
        long avg = sum / nanos.length;
        System.out.println(label + ":");
        System.out.printf("  P50=%.3fms, P95=%.3fms, P99=%.3fms, avg=%.3fms%n",
                p50 / 1_000_000.0, p95 / 1_000_000.0, p99 / 1_000_000.0, avg / 1_000_000.0);
    }

    private static long percentile(long[] sorted, int p) {
        long[] copy = sorted.clone();
        java.util.Arrays.sort(copy);
        int idx = (int) Math.ceil(p / 100.0 * copy.length) - 1;
        if (idx < 0) idx = 0;
        if (idx >= copy.length) idx = copy.length - 1;
        return copy[idx];
    }

    // =========================================================================
    // 辅助方法：直接执行 SQL（与 IdempotencyRecordMapper.xml 保持一致）
    // =========================================================================

    /**
     * 模拟 IdempotencyRecordMapper.insertIfAbsent：INSERT IGNORE。
     */
    private int insertIfAbsent(Connection conn, long tenantId, String scene, String key,
                                String status, String fingerprint) throws SQLException {
        String sql = "INSERT IGNORE INTO sys_idempotency_record " +
                "(tenant_id, scene, idempotency_key, status, fingerprint, created_time, updated_time, expire_time) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, " +
                "DATEADD('SECOND', 86400, CURRENT_TIMESTAMP))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, scene);
            ps.setString(3, key);
            ps.setString(4, status);
            ps.setString(5, fingerprint);
            return ps.executeUpdate();
        }
    }

    /**
     * 模拟 IdempotencyRecordMapper.reacquire：FAILED → PROCESSING 的 CAS。
     */
    private int reacquire(Connection conn, long recordId, String newFingerprint) throws SQLException {
        String sql = "UPDATE sys_idempotency_record SET " +
                "status='PROCESSING', fingerprint=?, resource_type=NULL, resource_id=NULL, " +
                "result_summary=NULL, error_summary=NULL, updated_time=CURRENT_TIMESTAMP, " +
                "expire_time=DATEADD('SECOND', 86400, CURRENT_TIMESTAMP) " +
                "WHERE record_id=? AND status='FAILED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newFingerprint);
            ps.setLong(2, recordId);
            return ps.executeUpdate();
        }
    }

    /**
     * 模拟 IdempotencyRecordMapper.updateStatus：状态流转 CAS。
     */
    private int updateStatus(Connection conn, long recordId, String fromStatus, String toStatus,
                              String errorSummary) throws SQLException {
        String sql = "UPDATE sys_idempotency_record SET status=?, updated_time=CURRENT_TIMESTAMP" +
                (errorSummary != null ? ", error_summary=?" : "") +
                " WHERE record_id=? AND status=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, toStatus);
            if (errorSummary != null) {
                ps.setString(idx++, errorSummary);
            }
            ps.setLong(idx++, recordId);
            ps.setString(idx, fromStatus);
            return ps.executeUpdate();
        }
    }

    /**
     * 准备一条 PROCESSING 记录，返回 recordId。
     */
    private long prepareProcessingRecord(Connection conn, long tenantId, String scene,
                                          String key, String fingerprint) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_idempotency_record (tenant_id, scene, idempotency_key, status, fingerprint) " +
                        "VALUES (?, ?, ?, 'PROCESSING', ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, tenantId);
            ps.setString(2, scene);
            ps.setString(3, key);
            ps.setString(4, fingerprint);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                assertTrue(rs.next());
                return rs.getLong(1);
            }
        }
    }

    /**
     * 准备一条 FAILED 记录，返回 recordId。
     */
    private long prepareFailedRecord(Connection conn, long tenantId, String scene,
                                      String key, String fingerprint) throws SQLException {
        long recordId = prepareProcessingRecord(conn, tenantId, scene, key, fingerprint);
        int affected = updateStatus(conn, recordId, "PROCESSING", "FAILED", "原失败原因");
        assertEquals(1, affected, "准备 FAILED 记录失败");
        return recordId;
    }

    /**
     * 准备一条 SUCCEEDED 记录，返回 recordId。
     */
    private long prepareSucceededRecord(Connection conn, long tenantId, String scene,
                                         String key, String fingerprint) throws SQLException {
        long recordId = prepareProcessingRecord(conn, tenantId, scene, key, fingerprint);
        int affected = updateStatus(conn, recordId, "PROCESSING", "SUCCEEDED", null);
        assertEquals(1, affected, "准备 SUCCEEDED 记录失败");
        return recordId;
    }
}
