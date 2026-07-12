package com.junsong.finance.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.Test;

class FinExpenseVerifyBatchMapperContractTest
{
    @Test
    void mapperDefinesRequiredBatchOperations() throws Exception
    {
        String xml = Files.readString(Path.of("src/main/resources/mapper/finance/FinExpenseVerifyBatchMapper.xml"));
        String periodXml = Files.readString(Path.of("src/main/resources/mapper/finance/FinAccountingPeriodMapper.xml"));
        assertTrue(periodXml.contains("id=\"selectPeriodForUpdate\""));
        assertTrue(periodXml.contains("p.period_id=#{periodId}"));
        assertTrue(periodXml.contains("p.tenant_id=#{tenantId}"));
        assertTrue(periodXml.contains("p.dept_id=#{deptId}"));
        assertTrue(periodXml.contains("FOR UPDATE"));
        String periodService = Files.readString(Path.of("src/main/java/com/junsong/finance/service/impl/FinAccountingPeriodServiceImpl.java"));
        assertTrue(periodService.contains("selectPeriodForUpdate(period.getPeriodId(), TenantContext.getTenantId(), deptId)"));

        assertTrue(xml.contains("id=\"selectByRequestId\""));
        assertTrue(xml.matches("(?s).*id=\"selectByRequestId\".*?dept_id = #\\{deptId}.*?</select>.*"));
        assertTrue(xml.contains("id=\"selectBatchForUpdate\""));
        assertScoped(xml, "selectBatchForUpdate");
        assertTrue(xml.contains("id=\"selectCurrentExpensesForUpdate\""));
        assertTrue(xml.contains("id=\"selectCurrentAdvancesForUpdate\""));
        assertTrue(xml.contains("ORDER BY e.expense_id FOR UPDATE"));
        assertTrue(xml.contains("ORDER BY a.advance_id FOR UPDATE"));
        assertTrue(xml.contains("FOR UPDATE"));
        assertTrue(xml.contains("id=\"markBatchReversed\""));
        assertTrue(xml.contains("status = 'VERIFIED'"));
        assertTrue(xml.contains("id=\"ExpenseDetailResult\""));
        assertTrue(xml.contains("id=\"AdvanceDetailResult\""));
        assertTrue(xml.contains("id=\"selectExpenseDetails\" resultMap=\"ExpenseDetailResult\""));
        assertTrue(xml.contains("id=\"selectAdvanceDetails\" resultMap=\"AdvanceDetailResult\""));
        assertScoped(xml, "selectExpenseDetails");
        assertScoped(xml, "selectAdvanceDetails");
        assertScoped(xml, "markBatchReversed");
        assertTrue(xml.contains("version = version + 1"));
        assertTrue(xml.contains("reverse_request_id = #{requestId}"));
        assertMapperParams("selectExpenseDetails", "batchId", "tenantId", "deptId");
        assertMapperParams("selectAdvanceDetails", "batchId", "tenantId", "deptId");
        assertMapperParams("selectBatchForUpdate", "batchId", "tenantId", "deptId");
        assertMapperParams("selectCurrentExpensesForUpdate", "batchId", "tenantId", "deptId");
        assertMapperParams("selectCurrentAdvancesForUpdate", "batchId", "tenantId", "deptId");
        assertMapperParams("markBatchReversed", "batchId", "tenantId", "deptId", "version",
            "reverseBy", "reverseTime", "reason", "requestId");

        // 核销记录列表/详情查询（审计用只读查询）
        assertTrue(xml.contains("id=\"selectBatchList\""));
        assertTrue(xml.contains("id=\"selectBatchById\""));
        assertTrue(xml.contains("id=\"selectExpenseDetailsWithDisplay\""));
        assertTrue(xml.contains("id=\"selectAdvanceDetailsWithDisplay\""));
        assertScoped(xml, "selectBatchById");
        assertScopedDetail(xml, "selectExpenseDetailsWithDisplay");
        assertScopedDetail(xml, "selectAdvanceDetailsWithDisplay");
        assertMapperParams("selectBatchList", "tenantId", "deptId", "query");
        assertMapperParams("selectBatchById", "batchId", "tenantId", "deptId");
        assertMapperParams("selectExpenseDetailsWithDisplay", "batchId", "tenantId", "deptId");
        assertMapperParams("selectAdvanceDetailsWithDisplay", "batchId", "tenantId", "deptId");
    }

    private static void assertScopedDetail(String xml, String statementId)
    {
        int start = xml.indexOf("id=\"" + statementId + "\"");
        int end = xml.indexOf("</select>", start);
        String statement = xml.substring(start, end);
        assertTrue(statement.contains("batch_id = #{batchId}"));
        assertTrue(statement.contains("tenant_id = #{tenantId}"));
        assertTrue(statement.contains("dept_id = #{deptId}"));
    }

    private static void assertScoped(String xml, String statementId)
    {
        int start = xml.indexOf("id=\"" + statementId + "\"");
        int end = xml.indexOf(statementId.startsWith("mark") ? "</update>" : "</select>", start);
        String statement = xml.substring(start, end);
        assertTrue(statement.contains("batch_id = #{batchId}"));
        assertTrue(statement.contains("tenant_id = #{tenantId}"));
        assertTrue(statement.contains("dept_id = #{deptId}"));
    }

    private static void assertMapperParams(String methodName, String... expected) throws Exception
    {
        Method method = java.util.Arrays.stream(FinExpenseVerifyBatchMapper.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        java.util.List<String> actual = java.util.Arrays.stream(method.getParameters())
            .map(parameter -> parameter.getAnnotation(Param.class))
            .map(annotation -> annotation == null ? null : annotation.value()).toList();
        assertTrue(actual.equals(java.util.List.of(expected)));
    }
}
