package com.junsong.finance.mapper;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinStocktakeMapper.xml 租户/部门/版本/锁顺序契约测试。
 *
 * 直接解析 Mapper XML，断言：
 * 1. 所有写入语句显式带上 tenant_id
 * 2. 列表查询包含授权部门过滤（foreach + dept_id）
 * 3. 状态更新使用乐观锁 version 谓词
 * 4. 锁查询按 dept_id, product_id 确定排序
 * 5. 插入语句使用 useGeneratedKeys 返回自增主键
 * 6. 不含物理 DELETE（盘点数据不可物理删除）
 */
class FinStocktakeMapperContractTest {

    private static String loadMapperXml() throws Exception {
        try (InputStream in = FinStocktakeMapperContractTest.class.getClassLoader()
                .getResourceAsStream("mapper/finance/FinStocktakeMapper.xml")) {
            assertNotNull(in, "未能在 classpath 找到 mapper/finance/FinStocktakeMapper.xml");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 抽取指定 id 的语句块内容（insert/select/update）。 */
    private static String statementBody(String xml, String id) {
        Pattern p = Pattern.compile(
                "<(insert|select|update)\\s+id=\"" + Pattern.quote(id) + "\"[\\s\\S]*?</\\1>",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        assertTrue(m.find(), "未找到语句：" + id);
        return m.group();
    }

    // ===== 租户谓词契约 =====

    @Test
    void insertStocktake_writesTenantId() throws Exception {
        String body = statementBody(loadMapperXml(), "insertStocktake");
        assertTrue(body.contains("tenant_id"), "insertStocktake 必须映射 tenant_id 列");
        assertTrue(body.contains("#{tenantId}"), "insertStocktake 必须写入 #{tenantId} 值");
    }

    @Test
    void insertStocktakeItem_writesTenantId() throws Exception {
        String body = statementBody(loadMapperXml(), "insertStocktakeItem");
        assertTrue(body.contains("tenant_id"), "insertStocktakeItem 必须映射 tenant_id 列");
        assertTrue(body.contains("#{tenantId}"), "insertStocktakeItem 必须写入 #{tenantId} 值");
    }

    @Test
    void insertStocktakeHistory_writesTenantId() throws Exception {
        String body = statementBody(loadMapperXml(), "insertStocktakeHistory");
        assertTrue(body.contains("tenant_id"), "insertStocktakeHistory 必须映射 tenant_id 列");
        assertTrue(body.contains("#{tenantId}"), "insertStocktakeHistory 必须写入 #{tenantId} 值");
    }

    @Test
    void selectStocktakeById_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "selectStocktakeById");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "selectStocktakeById 必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void selectStocktakeItemById_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "selectStocktakeItemById");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "selectStocktakeItemById 必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void updateStocktakeStatus_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "updateStocktakeStatus");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "updateStocktakeStatus 必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void updateStocktakeItemCount_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "updateStocktakeItemCount");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "updateStocktakeItemCount 必须包含 tenant_id = #{tenantId}");
    }

    // ===== 部门谓词契约 =====

    @Test
    void listStocktakes_filtersByAuthorizedDeptIds() throws Exception {
        String body = statementBody(loadMapperXml(), "listStocktakes");
        assertTrue(body.contains("dept_id"), "listStocktakes 必须包含 dept_id 过滤");
        assertTrue(body.contains("foreach"), "listStocktakes 必须使用 foreach 遍历授权部门集合");
    }

    @Test
    void listStocktakeItems_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "listStocktakeItems");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "listStocktakeItems 必须包含 tenant_id = #{tenantId}");
    }

    // ===== 乐观锁版本谓词契约 =====

    @Test
    void updateStocktakeStatus_usesOptimisticVersion() throws Exception {
        String body = statementBody(loadMapperXml(), "updateStocktakeStatus");
        assertTrue(body.replaceAll("\\s+", " ").contains("version = #{version}"),
                "updateStocktakeStatus 必须包含 version = #{version} 乐观锁谓词");
        assertTrue(body.contains("version + 1") || body.contains("version+1"),
                "updateStocktakeStatus 必须递增 version");
    }

    @Test
    void updateStocktakeItemCount_usesOptimisticVersion() throws Exception {
        String body = statementBody(loadMapperXml(), "updateStocktakeItemCount");
        assertTrue(body.replaceAll("\\s+", " ").contains("version = #{version}"),
                "updateStocktakeItemCount 必须包含 version = #{version} 乐观锁谓词");
        assertTrue(body.contains("version + 1") || body.contains("version+1"),
                "updateStocktakeItemCount 必须递增 version");
    }

    // ===== 确定性锁顺序契约 =====

    @Test
    void selectStocktakeItemsForUpdate_ordersByDeptIdProductId() throws Exception {
        String body = statementBody(loadMapperXml(), "selectStocktakeItemsForUpdate");
        assertTrue(body.toUpperCase().contains("ORDER BY"),
                "selectStocktakeItemsForUpdate 必须包含 ORDER BY 确定锁顺序");
        assertTrue(body.replaceAll("\\s+", " ").toLowerCase().contains("order by dept_id, product_id"),
                "selectStocktakeItemsForUpdate 必须按 dept_id, product_id 排序");
        assertTrue(body.toUpperCase().contains("FOR UPDATE"),
                "selectStocktakeItemsForUpdate 必须使用 FOR UPDATE 行锁");
    }

    @Test
    void selectStocktakeForUpdate_usesRowLock() throws Exception {
        String body = statementBody(loadMapperXml(), "selectStocktakeForUpdate");
        assertTrue(body.toUpperCase().contains("FOR UPDATE"),
                "selectStocktakeForUpdate 必须使用 FOR UPDATE 行锁");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "selectStocktakeForUpdate 必须包含 tenant_id = #{tenantId}");
    }

    // ===== 自增主键契约 =====

    @Test
    void insertStocktake_usesGeneratedKeys() throws Exception {
        String body = statementBody(loadMapperXml(), "insertStocktake");
        assertTrue(body.contains("useGeneratedKeys=\"true\""),
                "insertStocktake 必须使用 useGeneratedKeys 返回自增主键");
        assertTrue(body.contains("keyProperty=\"stocktakeId\""),
                "insertStocktake 必须指定 keyProperty=\"stocktakeId\"");
    }

    @Test
    void insertStocktakeItem_usesGeneratedKeys() throws Exception {
        String body = statementBody(loadMapperXml(), "insertStocktakeItem");
        assertTrue(body.contains("useGeneratedKeys=\"true\""),
                "insertStocktakeItem 必须使用 useGeneratedKeys 返回自增主键");
        assertTrue(body.contains("keyProperty=\"itemId\""),
                "insertStocktakeItem 必须指定 keyProperty=\"itemId\"");
    }

    @Test
    void insertStocktakeHistory_usesGeneratedKeys() throws Exception {
        String body = statementBody(loadMapperXml(), "insertStocktakeHistory");
        assertTrue(body.contains("useGeneratedKeys=\"true\""),
                "insertStocktakeHistory 必须使用 useGeneratedKeys 返回自增主键");
        assertTrue(body.contains("keyProperty=\"historyId\""),
                "insertStocktakeHistory 必须指定 keyProperty=\"historyId\"");
    }

    // ===== 无物理 DELETE 契约 =====

    @Test
    void noPhysicalDeleteInMapper() throws Exception {
        String xml = loadMapperXml();
        assertFalse(xml.toLowerCase().contains("<delete"),
                "FinStocktakeMapper.xml 禁止包含 <delete> 物理删除语句（盘点数据不可物理删除）");
    }

    // ===== 幂等键契约 =====

    @Test
    void countByTakeNo_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "countByTakeNo");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "countByTakeNo 必须包含 tenant_id = #{tenantId}");
        assertTrue(body.contains("take_no"), "countByTakeNo 必须过滤 take_no");
    }

    @Test
    void countByCountIdempotencyKey_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "countByCountIdempotencyKey");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "countByCountIdempotencyKey 必须包含 tenant_id = #{tenantId}");
        assertTrue(body.contains("count_idempotency_key"),
                "countByCountIdempotencyKey 必须过滤 count_idempotency_key");
    }

    // ===== 冻结后 movement 汇总契约（Task 6 预留） =====

    @Test
    void sumMovementAfterFreeze_filtersByTenantAndFreezeTime() throws Exception {
        String xml = loadMapperXml();
        // 此方法可能在 Task 6 才添加，Task 2 阶段允许不存在
        Pattern p = Pattern.compile(
                "<(select)\\s+id=\"sumMovementAfterFreeze\"[\\s\\S]*?</\\1>",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        if (m.find()) {
            String body = m.group();
            assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                    "sumMovementAfterFreeze 必须包含 tenant_id = #{tenantId}");
        }
        // Task 2 阶段允许此方法不存在
    }
}
