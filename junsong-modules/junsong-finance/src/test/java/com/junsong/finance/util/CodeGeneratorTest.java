package com.junsong.finance.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 编码生成器测试
 *
 * 验证所有业务编号的格式、长度、前缀、后缀生成规则，
 * 以及 count >= 10000 时随机后缀的字符集和唯一性。
 */
class CodeGeneratorTest
{
    // ── 商品编码 ──

    @Test
    void productCodeShouldBe8Digits()
    {
        String code = CodeGenerator.generateProductCode();
        assertEquals(8, code.length(), "商品编码应为8位");
        assertTrue(code.matches("\\d{8}"), "商品编码应全为数字");
    }

    // ── 供应商编码 ──

    @Test
    void supplierCodeShouldBe6Digits()
    {
        String code = CodeGenerator.generateSupplierCode();
        assertEquals(6, code.length(), "供应商编码应为6位");
        assertTrue(code.matches("\\d{6}"), "供应商编码应全为数字");
    }

    // ── 进货单号 ──

    @Test
    void purchaseNoShouldHaveDatePrefixAndNumericSuffix()
    {
        String no = CodeGenerator.generatePurchaseNo(1);
        assertEquals(12, no.length(), "进货单号应为12位（8位日期+4位后缀）");
        assertTrue(no.matches("\\d{12}"), "进货单号应全为数字");
        assertEquals("0001", no.substring(8), "count=1 时后缀应为 0001");
    }

    @Test
    void purchaseNoShouldUseAlphanumericWhenCountExceeds9999()
    {
        String no = CodeGenerator.generatePurchaseNo(10000);
        assertEquals(12, no.length());
        String suffix = no.substring(8);
        assertTrue(suffix.matches("[0-9A-Z]{4}"), "count>=10000 时后缀应为字母数字组合");
    }

    // ── 销售单号 ──

    @Test
    void saleNoShouldHaveSXPrefix()
    {
        String no = CodeGenerator.generateSaleNo(5);
        assertTrue(no.startsWith("SX"), "销售单号应以 SX 开头");
        assertEquals(14, no.length(), "销售单号应为14位（SX+8位日期+4位后缀）");
        assertEquals("0005", no.substring(10), "count=5 时后缀应为 0005");
    }

    // ── 缴款单号 ──

    @Test
    void paymentNoShouldHaveJKPrefix()
    {
        String no = CodeGenerator.generatePaymentNo(10);
        assertTrue(no.startsWith("JK"), "缴款单号应以 JK 开头");
        assertEquals(14, no.length());
        assertEquals("0010", no.substring(10));
    }

    // ── 费用单号 ──

    @Test
    void expenseNoShouldHaveFYPrefix()
    {
        String no = CodeGenerator.generateExpenseNo(0);
        assertTrue(no.startsWith("FY"), "费用单号应以 FY 开头");
        assertEquals(14, no.length());
        assertEquals("0000", no.substring(10));
    }

    // ── 借支单号 ──

    @Test
    void advanceNoShouldHaveJZPrefix()
    {
        String no = CodeGenerator.generateAdvanceNo(99);
        assertTrue(no.startsWith("JZ"), "借支单号应以 JZ 开头");
        assertEquals(14, no.length());
        assertEquals("0099", no.substring(10));
    }

    // ── 成本核算单号 ──

    @Test
    void costAccountingNoShouldHaveHSPrefix()
    {
        String no = CodeGenerator.generateCostAccountingNo(42);
        assertTrue(no.startsWith("HS"), "核算单号应以 HS 开头");
        assertEquals(14, no.length());
        assertEquals("0042", no.substring(10));
    }

    // ── 投资人返款单号 ──

    @Test
    void investorPaymentNoShouldHaveFKPrefix()
    {
        String no = CodeGenerator.generateInvestorPaymentNo(7);
        assertTrue(no.startsWith("FK"), "返款单号应以 FK 开头");
        assertEquals(14, no.length());
        assertEquals("0007", no.substring(10));
    }

    // ── 后缀递增 ──

    @Test
    void suffixShouldIncrementWithCount()
    {
        String no1 = CodeGenerator.generateExpenseNo(1);
        String no2 = CodeGenerator.generateExpenseNo(2);
        String no3 = CodeGenerator.generateExpenseNo(3);
        assertEquals("0001", no1.substring(10));
        assertEquals("0002", no2.substring(10));
        assertEquals("0003", no3.substring(10));
    }

    @Test
    void suffixShouldPadTo4Digits()
    {
        String no = CodeGenerator.generateExpenseNo(9999);
        assertEquals("9999", no.substring(10), "count=9999 时后缀应为 9999");
    }

    // ── 随机后缀唯一性 ──

    @Test
    void randomSuffixShouldGenerateDistinctCodes()
    {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 50; i++)
        {
            codes.add(CodeGenerator.generateExpenseNo(10000 + i));
        }
        assertTrue(codes.size() >= 40, "50 次生成应至少有 40 个不同编码（实际: " + codes.size() + "）");
    }

    // ── 所有类型长度一致性 ──

    @Test
    void allTypesShouldHaveConsistentLength()
    {
        assertEquals(8, CodeGenerator.generateProductCode().length());
        assertEquals(6, CodeGenerator.generateSupplierCode().length());
        assertEquals(12, CodeGenerator.generatePurchaseNo(1).length());
        assertEquals(14, CodeGenerator.generateSaleNo(1).length());
        assertEquals(14, CodeGenerator.generatePaymentNo(1).length());
        assertEquals(14, CodeGenerator.generateExpenseNo(1).length());
        assertEquals(14, CodeGenerator.generateAdvanceNo(1).length());
        assertEquals(14, CodeGenerator.generateCostAccountingNo(1).length());
        assertEquals(14, CodeGenerator.generateInvestorPaymentNo(1).length());
    }
}
