package com.junsong.finance.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinanceReportController 库存报表端点契约测试。
 *
 * <p>读取控制器源码，断言库存报表相关端点使用正确的权限码：
 * <ul>
 *   <li>库存报表端点使用 {@code finance:report:stock}</li>
 *   <li>导出端点使用 {@code finance:report:stock:export}</li>
 *   <li>对账端点使用 {@code finance:stock:reconciliation}</li>
 *   <li>库存端点接受 {@code StockReportQuery}（非 {@code ReportQueryParams}）</li>
 * </ul>
 */
class FinanceReportControllerContractTest {

    private static final String CONTROLLER_PATH =
            "src/main/java/com/junsong/finance/controller/FinanceReportController.java";

    private static String loadSource() throws Exception {
        return Files.readString(Path.of(CONTROLLER_PATH));
    }

    /** 提取指定方法名对应的代码块（包含注解）。 */
    private static String methodBlock(String source, String methodName) {
        Pattern p = Pattern.compile(
                "(?s)((?:\\s*@[^\\n]+\\n)+\\s*public AjaxResult " + Pattern.quote(methodName) + "\\(.*?\\n    })");
        Matcher m = p.matcher(source);
        assertTrue(m.find(), "未找到方法：" + methodName);
        return m.group(1);
    }

    @Test
    void stockEndpointUsesStockReportPermission() throws Exception {
        String source = loadSource();
        String block = methodBlock(source, "getStockReport");
        assertTrue(block.contains("@RequiresPermissions(\"finance:report:stock\")"),
                "/stock 端点必须使用 finance:report:stock 权限");
        assertTrue(block.contains("@PostMapping(\"/stock\")"),
                "必须映射到 /stock");
    }

    @Test
    void stockSummaryEndpointUsesStockReportPermission() throws Exception {
        String source = loadSource();
        String block = methodBlock(source, "getStockReportSummary");
        assertTrue(block.contains("@RequiresPermissions(\"finance:report:stock\")"),
                "/stock/summary 端点必须使用 finance:report:stock 权限");
        assertTrue(block.contains("@PostMapping(\"/stock/summary\")"),
                "必须映射到 /stock/summary");
    }

    @Test
    void stockPageEndpointUsesStockReportPermission() throws Exception {
        String source = loadSource();
        String block = methodBlock(source, "getStockReportPage");
        assertTrue(block.contains("@RequiresPermissions(\"finance:report:stock\")"),
                "/stock/page 端点必须使用 finance:report:stock 权限");
        assertTrue(block.contains("@PostMapping(\"/stock/page\")"),
                "必须映射到 /stock/page");
    }

    @Test
    void stockLedgerPageEndpointUsesStockReportPermission() throws Exception {
        String source = loadSource();
        String block = methodBlock(source, "getStockLedgerPage");
        assertTrue(block.contains("@RequiresPermissions(\"finance:report:stock\")"),
                "/stock/ledger/page 端点必须使用 finance:report:stock 权限");
        assertTrue(block.contains("@PostMapping(\"/stock/ledger/page\")"),
                "必须映射到 /stock/ledger/page");
    }

    @Test
    void stockExportEndpointUsesExportPermission() throws Exception {
        String source = loadSource();
        String block = methodBlock(source, "exportStockReport");
        assertTrue(block.contains("@RequiresPermissions(\"finance:report:stock:export\")"),
                "/stock/export 端点必须使用 finance:report:stock:export 权限");
        assertTrue(block.contains("@PostMapping(\"/stock/export\")"),
                "必须映射到 /stock/export");
    }

    @Test
    void stockReconciliationEndpointUsesReconciliationPermission() throws Exception {
        String source = loadSource();
        String block = methodBlock(source, "getStockReconciliation");
        assertTrue(block.contains("@RequiresPermissions(\"finance:stock:reconciliation\")"),
                "/stock/reconciliation 端点必须使用 finance:stock:reconciliation 权限");
        assertTrue(block.contains("@PostMapping(\"/stock/reconciliation\")"),
                "必须映射到 /stock/reconciliation");
    }

    @Test
    void stockEndpointsAcceptStockReportQueryNotReportQueryParams() throws Exception {
        String source = loadSource();
        // 库存端点应接受 StockReportQuery，而非 ReportQueryParams
        String stockBlock = methodBlock(source, "getStockReport");
        assertTrue(stockBlock.contains("StockReportQuery"),
                "/stock 端点必须接受 StockReportQuery 参数");
        assertFalse(stockBlock.contains("ReportQueryParams"),
                "/stock 端点不应接受 ReportQueryParams 参数");

        String summaryBlock = methodBlock(source, "getStockReportSummary");
        assertTrue(summaryBlock.contains("StockReportQuery"),
                "/stock/summary 端点必须接受 StockReportQuery 参数");

        String pageBlock = methodBlock(source, "getStockReportPage");
        assertTrue(pageBlock.contains("StockReportQuery"),
                "/stock/page 端点必须接受 StockReportQuery 参数");

        String exportBlock = methodBlock(source, "exportStockReport");
        assertTrue(exportBlock.contains("StockReportQuery"),
                "/stock/export 端点必须接受 StockReportQuery 参数");

        String reconciliationBlock = methodBlock(source, "getStockReconciliation");
        assertTrue(reconciliationBlock.contains("StockReportQuery"),
                "/stock/reconciliation 端点必须接受 StockReportQuery 参数");
    }

    @Test
    void oldStockEndpointWithReportQueryParamsRemoved() throws Exception {
        String source = loadSource();
        // 旧版 getStockReport(@RequestBody ReportQueryParams params) 应已移除
        assertFalse(source.contains("getStockReport(@RequestBody ReportQueryParams"),
                "旧的 getStockReport(ReportQueryParams) 签名应已移除");
    }
}
