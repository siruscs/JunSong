package com.junsong.finance.mapper;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockReportMapper.xml 租户与口径契约测试。
 *
 * <p>直接解析 Mapper XML 并递归展开 {@code <include>} 片段，断言：
 * <ul>
 *   <li>summary / items / ledger 三条核心查询都显式过滤 {@code tenant_id = #{tenantId}}；</li>
 *   <li>汇总按 {@code change_type} 分类（PURCHASE_IN/PURCHASE_REVERSE 计入采购净入库，
 *       SALE_OUT/SALE_REVERSE 计入销售净出库），而非简单按正负号；</li>
 *   <li>日期区间使用半开区间 {@code create_time < DATE_ADD(..., INTERVAL 1 DAY)}，
 *       严禁 {@code DATE(create_time)}；</li>
 *   <li>deptIds 使用 {@code <foreach>}；</li>
 *   <li>XML 内不得出现 UPDATE/DELETE/INSERT。</li>
 * </ul>
 */
class StockReportMapperContractTest {

    private static final String MAPPER_PATH = "mapper/finance/StockReportMapper.xml";

    private static String loadMapperXml() throws Exception {
        try (InputStream in = StockReportMapperContractTest.class.getClassLoader()
                .getResourceAsStream(MAPPER_PATH)) {
            assertNotNull(in, "未能在 classpath 找到 " + MAPPER_PATH);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 抽取所有 <sql id="X">...</sql> 片段。 */
    private static Map<String, String> extractFragments(String xml) {
        Map<String, String> fragments = new LinkedHashMap<>();
        Pattern p = Pattern.compile(
                "<sql\\s+id=\"([^\"]+)\"\\s*>([\\s\\S]*?)</sql>",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        while (m.find()) {
            fragments.put(m.group(1), m.group(2));
        }
        return fragments;
    }

    /** 抽取指定 id 的 select 语句块。 */
    private static String statementBody(String xml, String id) {
        Pattern p = Pattern.compile(
                "<select\\s+id=\"" + Pattern.quote(id) + "\"[\\s\\S]*?</select>",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        assertTrue(m.find(), "未找到语句：" + id);
        return m.group();
    }

    /** 递归展开 <include refid="X"/>，返回完全展开后的语句文本。 */
    private static String resolveIncludes(String body, Map<String, String> fragments) {
        Pattern include = Pattern.compile(
                "<include\\s+refid=\"([^\"]+)\"\\s*/>",
                Pattern.CASE_INSENSITIVE);
        String resolved = body;
        for (int i = 0; i < 10; i++) {
            Matcher m = include.matcher(resolved);
            if (!m.find()) {
                break;
            }
            StringBuffer sb = new StringBuffer();
            m.reset();
            while (m.find()) {
                String ref = m.group(1);
                String frag = fragments.get(ref);
                assertNotNull(frag, "未找到被引用的 sql 片段：" + ref);
                m.appendReplacement(sb, Matcher.quoteReplacement(frag));
            }
            m.appendTail(sb);
            resolved = sb.toString();
        }
        return resolved;
    }

    private static String resolvedStatement(String xml, String id) {
        return resolveIncludes(statementBody(xml, id), extractFragments(xml));
    }

    private static String collapse(String s) {
        return s.replaceAll("\\s+", " ");
    }

    @Test
    void summary_filtersByTenant() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectStockReportSummary");
        assertTrue(collapse(body).contains("tenant_id = #{tenantId}"),
                "汇总查询必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void items_filtersByTenant() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectStockReportItems");
        assertTrue(collapse(body).contains("tenant_id = #{tenantId}"),
                "分页查询必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void ledger_filtersByTenant() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectStockLedgerRows");
        assertTrue(collapse(body).contains("tenant_id = #{tenantId}"),
                "流水下钻查询必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void summary_classifiesByChangeType() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectStockReportSummary");
        assertTrue(body.contains("change_type"), "汇总必须按 change_type 分类");
        assertTrue(body.contains("CASE change_type"),
                "汇总必须使用 CASE change_type 分类，而非简单按正负号");
    }

    @Test
    void summary_purchaseNetInClassifiesPurchaseTypes() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectStockReportSummary");
        assertTrue(body.contains("purchase_net_in"), "汇总必须包含采购净入库口径 purchase_net_in");
        assertTrue(body.contains("'PURCHASE_IN'"), "PURCHASE_IN 必须计入采购净入库");
        assertTrue(body.contains("'PURCHASE_REVERSE'"), "PURCHASE_REVERSE 必须计入采购净入库");
        assertTrue(collapse(body).contains("WHEN 'PURCHASE_REVERSE' THEN change_quantity"),
                "采购冲销流水 change_quantity 已为负数，采购净入库不得二次取反");
    }

    @Test
    void summary_saleNetOutClassifiesSaleTypes() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectStockReportSummary");
        assertTrue(body.contains("sale_net_out"), "汇总必须包含销售净出库口径 sale_net_out");
        assertTrue(body.contains("'SALE_OUT'"), "SALE_OUT 必须计入销售净出库");
        assertTrue(body.contains("'SALE_REVERSE'"), "SALE_REVERSE 必须计入销售净出库");
        assertTrue(collapse(body).contains("WHEN 'SALE_OUT' THEN -change_quantity"),
                "销售出库流水 change_quantity 已为负数，销售净出库展示应转为正向出库量");
        assertTrue(collapse(body).contains("WHEN 'SALE_REVERSE' THEN -change_quantity"),
                "销售冲销流水 change_quantity 已为正数，销售净出库应负向抵减");
    }

    @Test
    void openingQuantityUsesSignedLedgerDeltaDirectly() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectStockReportSummary");
        assertTrue(collapse(body).contains("COALESCE(SUM(change_quantity), 0) AS opening_quantity"),
                "期初数量应直接累加带符号的库存流水 change_quantity");
    }

    @Test
    void dateRangeUsesHalfOpenInterval() throws Exception {
        String xml = loadMapperXml();
        assertTrue(xml.contains("DATE_ADD(#{query.endDate}, INTERVAL 1 DAY)"),
                "区间结束必须使用半开区间 DATE_ADD(#{query.endDate}, INTERVAL 1 DAY)");
        assertTrue(xml.contains("DATE_ADD(#{endDate}, INTERVAL 1 DAY)"),
                "流水下钻区间结束必须使用半开区间 DATE_ADD(#{endDate}, INTERVAL 1 DAY)");
        assertFalse(xml.toLowerCase().contains("date(create_time)"),
                "禁止使用 DATE(create_time) 截断时间列，应使用半开区间");
    }

    @Test
    void deptIdsForeachPresent() throws Exception {
        String xml = loadMapperXml();
        assertTrue(xml.contains("query.deptIds"), "必须支持 query.deptIds 门店过滤");
        assertTrue(xml.contains("<foreach"), "deptIds 必须使用 foreach 展开 IN 列表");
    }

    @Test
    void noMutationStatements() throws Exception {
        String xml = loadMapperXml().toLowerCase();
        assertFalse(xml.contains("<update"), "经营库存报表 Mapper 不得包含 UPDATE 语句");
        assertFalse(xml.contains("<delete"), "经营库存报表 Mapper 不得包含 DELETE 语句");
        assertFalse(xml.contains("<insert"), "经营库存报表 Mapper 不得包含 INSERT 语句");
    }

    @Test
    void exportQuery_hasNoLimitClause() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectAllStockReportItems");
        assertFalse(body.toLowerCase().contains("limit"),
                "导出查询不得包含 LIMIT 子句，必须返回全部数据");
    }

    @Test
    void exportQuery_filtersByTenant() throws Exception {
        String body = resolvedStatement(loadMapperXml(), "selectAllStockReportItems");
        assertTrue(collapse(body).contains("tenant_id = #{tenantId}"),
                "导出查询必须包含 tenant_id = #{tenantId}");
    }

    @Test
    void snapshotAnomalyStatusFiltersByReconciliationStatus() throws Exception {
        String xml = loadMapperXml();
        assertTrue(xml.contains("SNAPSHOT_ANOMALY"),
                "状态筛选必须处理 SNAPSHOT_ANOMALY 特殊值");
        assertTrue(xml.contains("reconciliationStatus = 'ANOMALY'"),
                "SNAPSHOT_ANOMALY 应映射到 reconciliationStatus = 'ANOMALY'，而非 stockStatus");
    }
}
