package com.junsong.finance.mapper;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinStockLedgerMapper.xml 租户契约测试。
 *
 * 直接解析 Mapper XML，断言对账相关的每个读写语句都显式带上租户过滤/写入
 * （tenant_id = #{tenantId} 或 insert 写入 tenant_id），
 * 防止后续维护误删租户键导致跨租户串库。
 */
class FinStockLedgerMapperContractTest {

    private static String loadMapperXml() throws Exception {
        try (InputStream in = FinStockLedgerMapperContractTest.class.getClassLoader()
                .getResourceAsStream("mapper/finance/FinStockLedgerMapper.xml")) {
            assertNotNull(in, "未能在 classpath 找到 mapper/finance/FinStockLedgerMapper.xml");
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

    @Test
    void insertPositionIfAbsent_writesTenantId() throws Exception {
        String body = statementBody(loadMapperXml(), "insertPositionIfAbsent");
        assertTrue(body.contains("tenant_id"), "insertPositionIfAbsent 必须写入 tenant_id 列");
        assertTrue(body.contains("#{tenantId}"), "insertPositionIfAbsent 必须写入 #{tenantId} 值");
    }

    @Test
    void selectPositionQuantityForUpdate_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "selectPositionQuantityForUpdate");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "selectPositionQuantityForUpdate 必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void updatePositionQuantity_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "updatePositionQuantity");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "updatePositionQuantity 必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void sumRecordedNet_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "sumRecordedNet");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "sumRecordedNet 必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void selectRecordedProductIds_filtersByTenant() throws Exception {
        String body = statementBody(loadMapperXml(), "selectRecordedProductIds");
        assertTrue(body.replaceAll("\\s+", " ").contains("tenant_id = #{tenantId}"),
                "selectRecordedProductIds 必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void insertFinStockLedger_writesTenantId() throws Exception {
        String body = statementBody(loadMapperXml(), "insertFinStockLedger");
        assertTrue(body.contains("tenant_id"), "insertFinStockLedger 必须映射 tenant_id 列");
        assertTrue(body.contains("#{tenantId}"), "insertFinStockLedger 必须写入 #{tenantId} 值");
    }
}
