package com.junsong.finance.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.junsong.finance.service.impl.FinExpenseVerificationServiceImpl;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Modifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;

class FinExpenseControllerContractTest
{
    @Test
    void verificationServiceMarksItsProductionConstructorForSpringInjection()
    {
        var productionConstructor = java.util.Arrays.stream(FinExpenseVerificationServiceImpl.class.getDeclaredConstructors())
            .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
            .findFirst()
            .orElseThrow();

        assertNotNull(productionConstructor.getAnnotation(Autowired.class));
    }

    @Test
    void exposesIndependentSecureVerificationEndpoints() throws Exception
    {
        String source = Files.readString(Path.of("src/main/java/com/junsong/finance/controller/FinExpenseController.java"));
        assertTrue(source.contains("@RequiresPermissions(\"finance:expense:verify\")"));
        assertTrue(source.contains("@PutMapping(\"/batchVerify\")"));
        assertTrue(source.contains("@RequiresPermissions(\"finance:expense:unverify\")"));
        assertTrue(source.contains("@PutMapping(\"/unverify/{batchId}\")"));
        assertTrue(source.contains("@GetMapping(\"/{expenseId}/capability\")"));
        assertTrue(source.contains("finExpenseVerificationService.verify("));
        assertTrue(source.contains("finExpenseVerificationService.unverify("));
        assertTrue(source.contains("finExpenseVerificationService.getCapability("));
        assertTrue(method(source, "capability").contains("@RequiresPermissions(\"finance:expense:unverify\")"));
        assertTrue(source.contains("@GetMapping(\"/{expenseId}/verificationCandidate\")"));
        assertTrue(method(source, "verificationCandidate").contains("@RequiresPermissions(\"finance:expense:verify\")"));
        assertTrue(method(source, "verificationCandidate").contains("finExpenseVerificationService.getVerificationCandidate(expenseId)"));
        assertFalse(source.contains("getUnverifiedAdvances(@RequestParam"));
        assertTrue(source.contains("Long deptId = SecurityUtils.getDeptId()"));
        assertTrue(source.contains("query.setDeptId(deptId)"));
        assertTrue(source.contains("无法确定当前门店，禁止查询借支单"));
        assertFalse(source.contains("finExpenseService.batchVerifyExpense(verifyVO"));
        assertFalse(source.contains("finExpenseService.verifyExpense(expenseId"));
        assertTrue(method(source, "edit").contains("@RequiresPermissions(\"finance:expense:edit\")"));
        assertFalse(method(source, "edit").contains("@Deprecated"));
        assertTrue(method(source, "edit").contains("finExpenseService.updateFinExpense(finExpense)"));
        assertTrue(method(source, "verify").contains("@Deprecated"));
        assertTrue(method(source, "verify").contains("@RequiresPermissions(\"finance:expense:verify\")"));
        assertTrue(method(source, "unverify").contains("return success(finExpenseVerificationService.unverify("));
    }

    private static String method(String source, String name)
    {
        var matcher = Pattern.compile("(?s)((?:\\s*@[^\\n]+\\n)+\\s*public AjaxResult " + name + "\\(.*?\\n    })\\n(?=\\n    @|\\n    /\\*\\*)").matcher(source);
        assertTrue(matcher.find(), "missing method " + name);
        return matcher.group(1);
    }
}
