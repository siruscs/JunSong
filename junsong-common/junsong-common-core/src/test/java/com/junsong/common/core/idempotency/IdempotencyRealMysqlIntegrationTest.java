package com.junsong.common.core.idempotency;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 幂等框架真实 MySQL 集成测试。
 *
 * <p>用户第二轮复核明确要求："H2 不能替代真实 MySQL + 真实 Service 验证"。
 * 本测试连接真实 MySQL（非 H2），验证：
 * <ul>
 *   <li>MySQL 唯一索引在并发 INSERT IGNORE 下的真实行为</li>
 *   <li>CAS UPDATE 状态机的真实行为</li>
 *   <li>事务回滚后幂等记录的真实状态</li>
 *   <li>真实 MySQL 性能基准</li>
 * </ul>
 *
 * <h2>执行前提</h2>
 * <ol>
 *   <li>真实 MySQL 可访问（默认 localhost:3306，root/root_123）</li>
 *   <li>已创建测试数据库 junsong_idempotency_test</li>
 * </ol>
 *
 * <h2>执行方式</h2>
 * <pre>
 * # 1. 创建测试数据库
 * docker exec junsong-mysql mysql -uroot -proot_123 -e "CREATE DATABASE IF NOT EXISTS junsong_idempotency_test DEFAULT CHARSET utf8mb4"
 *
 * # 2. 运行测试
 * cd junsong-common/junsong-common-core
 * mvn -Dtest=IdempotencyRealMysqlIntegrationTest -Dtest.realMysql=true test
 * </pre>
 *
 * <h2>与 H2 测试的区别</h2>
 * <ul>
 *   <li>H2 MySQL 模式：模拟 MySQL SQL 行为，但内存数据库，无真实网络 IO、无真实锁竞争</li>
 *   <li>真实 MySQL：真实网络 IO、真实 InnoDB 锁、真实 MVCC、真实唯一索引冲突处理</li>
 * </ul>
 *
 * @author junsong
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("real-mysql")
@EnabledIfSystemProperty(named = "test.realMysql", matches = "true")
@DisplayName("幂等框架真实 MySQL 集成测试")
class IdempotencyRealMysqlIntegrationTest {

    private static final String MYSQL_URL =
            System.getProperty("test.mysql.url",
                    "jdbc:mysql://localhost:3306/junsong_idempotency_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");
    private static final String MYSQL_USER = System.getProperty("test.mysql.user", "root");
    private static final String MYSQL_PASSWORD = System.getProperty("test.mysql.password", "root_123");

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
                created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                expire_time     DATETIME              DEFAULT NULL,
                PRIMARY KEY (record_id),
                UNIQUE KEY uk_idempotency_tenant_scene_key (tenant_id, scene, idempotency_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    @BeforeAll
    void setUpDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS sys_idempotency_record");
            st.execute(CREATE_TABLE_SQL);
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
    }

    private int insertIfAbsent(Connection conn, long tenantId, String scene,
                                String key, String status, String fingerprint) throws SQLException {
        String sql = "INSERT IGNORE INTO sys_idempotency_record " +
                "(tenant_id, scene, idempotency_key, status, fingerprint, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, scene);
            ps.setString(3, key);
            ps.setString(4, status);
            ps.setString(5, fingerprint);
            return ps.executeUpdate();
        }
    }

    private int casUpdateStatus(Connection conn, long recordId, String fromStatus, String toStatus,
                                 String errorSummary) throws SQLException {
        String sql = "UPDATE sys_idempotency_record SET status=?, error_summary=?, updated_time=NOW() " +
                "WHERE record_id=? AND status=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, toStatus);
            ps.setString(2, errorSummary);
            ps.setLong(3, recordId);
            ps.setString(4, fromStatus);
            return ps.executeUpdate();
        }
    }

    private int reacquire(Connection conn, long recordId, String fingerprint) throws SQLException {
        String sql = "UPDATE sys_idempotency_record SET status='PROCESSING', fingerprint=?, " +
                "error_summary=NULL, resource_type=NULL, resource_id=NULL, result_summary=NULL, updated_time=NOW() " +
                "WHERE record_id=? AND status='FAILED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fingerprint);
            ps.setLong(2, recordId);
            return ps.executeUpdate();
        }
    }

    private long prepareRecord(Connection conn, long tenantId, String scene,
                                String key, String status, String fingerprint) throws SQLException {
        String sql = "INSERT INTO sys_idempotency_record " +
                "(tenant_id, scene, idempotency_key, status, fingerprint, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, tenantId);
            ps.setString(2, scene);
            ps.setString(3, key);
            ps.setString(4, status);
            ps.setString(5, fingerprint);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                assertTrue(rs.next());
                return rs.getLong(1);
            }
        }
    }

    // =========================================================================
    // 场景 1：单线程 INSERT IGNORE 唯一索引生效
    // =========================================================================

    @Test
    @DisplayName("真实 MySQL：首次 INSERT IGNORE 返回 1，相同键第二次返回 0")
    void singleThread_insertIfAbsent_uniqueIndexEnforced() throws SQLException {
        try (Connection conn = createConnection()) {
            conn.createStatement().execute("DELETE FROM sys_idempotency_record WHERE idempotency_key='mysql-test-001'");

            int first = insertIfAbsent(conn, 1L, "sale:create", "mysql-test-001", "PROCESSING", "fp-001");
            assertEquals(1, first, "首次插入应返回 1（成功）");

            int second = insertIfAbsent(conn, 1L, "sale:create", "mysql-test-001", "PROCESSING", "fp-002");
            assertEquals(0, second, "相同键插入应返回 0（MySQL 唯一索引生效）");
        }
    }

    @Test
    @DisplayName("真实 MySQL：跨租户相同键各自独立成功")
    void crossTenant_sameKey_bothSucceed() throws SQLException {
        try (Connection conn = createConnection()) {
            conn.createStatement().execute("DELETE FROM sys_idempotency_record WHERE idempotency_key='mysql-shared-key'");

            int tenant1 = insertIfAbsent(conn, 1L, "sale:create", "mysql-shared-key", "PROCESSING", "fp-1");
            int tenant2 = insertIfAbsent(conn, 2L, "sale:create", "mysql-shared-key", "PROCESSING", "fp-2");
            assertEquals(1, tenant1, "租户 1 应插入成功");
            assertEquals(1, tenant2, "租户 2 应插入成功（跨租户隔离）");
        }
    }

    // =========================================================================
    // 场景 2：10 线程并发 INSERT IGNORE 同一个键，只有 1 个成功
    // =========================================================================

    @Test
    @DisplayName("真实 MySQL：10 线程并发 INSERT IGNORE 同键，只有 1 个成功")
    void tenThreads_concurrentInsert_onlyOneSucceeds() throws Exception {
        // 先清理
        try (Connection conn = createConnection()) {
            conn.createStatement().execute("DELETE FROM sys_idempotency_record WHERE idempotency_key='mysql-concurrent-001'");
        }

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
                    try (Connection conn = createConnection()) {
                        int affected = insertIfAbsent(conn, 1L, "sale:create",
                                "mysql-concurrent-001", "PROCESSING", "fp-shared");
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

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, successCount.get(), "真实 MySQL 下应该只有 1 个线程 INSERT 成功");
        assertEquals(9, conflictCount.get(), "其他 9 个线程应该返回 0（MySQL 唯一索引拦截）");

        // 验证表里只有 1 条记录
        try (Connection conn = createConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM sys_idempotency_record WHERE idempotency_key='mysql-concurrent-001'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "真实 MySQL 数据库里应该只有 1 条记录");
        }
    }

    // =========================================================================
    // 场景 3：CAS UPDATE 状态机
    // =========================================================================

    @Test
    @DisplayName("真实 MySQL：CAS UPDATE PROCESSING → SUCCEEDED 成功")
    void casUpdate_processToSucceeded() throws SQLException {
        try (Connection conn = createConnection()) {
            long recordId = prepareRecord(conn, 1L, "sale:create", "mysql-cas-001", "PROCESSING", "fp-001");
            int affected = casUpdateStatus(conn, recordId, "PROCESSING", "SUCCEEDED", null);
            assertEquals(1, affected, "CAS UPDATE 应返回 1");

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status FROM sys_idempotency_record WHERE record_id=?")) {
                ps.setLong(1, recordId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("SUCCEEDED", rs.getString("status"));
                }
            }
        }
    }

    @Test
    @DisplayName("真实 MySQL：CAS UPDATE 状态不匹配时影响行数为 0")
    void casUpdate_failsWhenStatusMismatch() throws SQLException {
        try (Connection conn = createConnection()) {
            long recordId = prepareRecord(conn, 1L, "sale:create", "mysql-cas-002", "SUCCEEDED", "fp-002");
            int affected = casUpdateStatus(conn, recordId, "PROCESSING", "FAILED", "error");
            assertEquals(0, affected, "状态不匹配时 CAS UPDATE 应返回 0");
        }
    }

    @Test
    @DisplayName("真实 MySQL：reacquire CAS FAILED → PROCESSING 成功")
    void reacquire_failedToProcessing() throws SQLException {
        try (Connection conn = createConnection()) {
            long recordId = prepareRecord(conn, 1L, "sale:create", "mysql-retry-001", "FAILED", "fp-original");
            int affected = reacquire(conn, recordId, "fp-new");
            assertEquals(1, affected, "FAILED → PROCESSING 应返回 1");

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status, fingerprint, error_summary FROM sys_idempotency_record WHERE record_id=?")) {
                ps.setLong(1, recordId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("PROCESSING", rs.getString("status"));
                    assertEquals("fp-new", rs.getString("fingerprint"));
                    assertEquals(null, rs.getString("error_summary"));
                }
            }
        }
    }

    // =========================================================================
    // 场景 4：事务回滚后幂等记录也被回滚
    // =========================================================================

    @Test
    @DisplayName("真实 MySQL：事务回滚后 INSERT 也被回滚")
    void transactionRollback_releasesIdempotencyLock() throws SQLException {
        try (Connection conn = createConnection()) {
            conn.createStatement().execute("DELETE FROM sys_idempotency_record WHERE idempotency_key='mysql-rollback-001'");

            // 开启事务，插入后回滚
            conn.setAutoCommit(false);
            try {
                int affected = insertIfAbsent(conn, 1L, "sale:create", "mysql-rollback-001", "PROCESSING", "fp-001");
                assertEquals(1, affected, "事务内插入应成功");
                conn.rollback();

                // 回滚后再次插入应成功（说明回滚释放了锁）
                int affected2 = insertIfAbsent(conn, 1L, "sale:create", "mysql-rollback-001", "PROCESSING", "fp-001");
                assertEquals(1, affected2, "事务回滚后再次插入应成功（锁已释放）");
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // =========================================================================
    // 场景 5：真实 MySQL 性能基准
    // =========================================================================

    @Test
    @DisplayName("真实 MySQL 性能基准：1000 次 INSERT IGNORE")
    void realMysql_performanceBenchmark() throws Exception {
        try (Connection conn = createConnection()) {
            conn.createStatement().execute("DELETE FROM sys_idempotency_record WHERE idempotency_key LIKE 'mysql-bench-%'");

            int iterations = 1000;
            long[] latencies = new long[iterations];

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                insertIfAbsent(conn, 1L, "bench", "mysql-bench-" + i, "PROCESSING", "fp-" + i);
                long elapsed = System.nanoTime() - start;
                latencies[i] = elapsed / 1000; // 转微秒
            }

            // 计算统计量
            java.util.Arrays.sort(latencies);
            long p50 = latencies[iterations / 2];
            long p95 = latencies[(int) (iterations * 0.95)];
            long p99 = latencies[(int) (iterations * 0.99)];
            long total = 0;
            for (long l : latencies) total += l;
            long avg = total / iterations;

            System.out.println("=== 真实 MySQL 性能基准（INSERT IGNORE 新建路径）===");
            System.out.println("迭代次数: " + iterations);
            System.out.println("P50 延迟: " + (p50 / 1000.0) + " ms");
            System.out.println("P95 延迟: " + (p95 / 1000.0) + " ms");
            System.out.println("P99 延迟: " + (p99 / 1000.0) + " ms");
            System.out.println("平均延迟: " + (avg / 1000.0) + " ms");

            // 清理
            conn.createStatement().execute("DELETE FROM sys_idempotency_record WHERE idempotency_key LIKE 'mysql-bench-%'");

            // 断言：P50 应小于 50ms（本地 MySQL，宽松阈值）
            assertTrue(p50 < 50000, "P50 延迟应小于 50ms，实际: " + (p50 / 1000.0) + "ms");
        }
    }

    @Test
    @DisplayName("真实 MySQL 性能基准：1000 次唯一键冲突")
    void realMysql_conflictBenchmark() throws Exception {
        try (Connection conn = createConnection()) {
            conn.createStatement().execute("DELETE FROM sys_idempotency_record WHERE idempotency_key='mysql-conflict-bench'");
            // 先插入一条
            insertIfAbsent(conn, 1L, "bench", "mysql-conflict-bench", "PROCESSING", "fp-original");

            int iterations = 1000;
            long[] latencies = new long[iterations];

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                insertIfAbsent(conn, 1L, "bench", "mysql-conflict-bench", "PROCESSING", "fp-" + i);
                long elapsed = System.nanoTime() - start;
                latencies[i] = elapsed / 1000;
            }

            java.util.Arrays.sort(latencies);
            long p50 = latencies[iterations / 2];
            long p95 = latencies[(int) (iterations * 0.95)];
            long p99 = latencies[(int) (iterations * 0.99)];

            System.out.println("=== 真实 MySQL 性能基准（INSERT IGNORE 唯一键冲突路径）===");
            System.out.println("迭代次数: " + iterations);
            System.out.println("P50 延迟: " + (p50 / 1000.0) + " ms");
            System.out.println("P95 延迟: " + (p95 / 1000.0) + " ms");
            System.out.println("P99 延迟: " + (p99 / 1000.0) + " ms");

            conn.createStatement().execute("DELETE FROM sys_idempotency_record WHERE idempotency_key='mysql-conflict-bench'");
        }
    }
}
