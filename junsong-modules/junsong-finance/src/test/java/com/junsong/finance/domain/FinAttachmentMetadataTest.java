package com.junsong.finance.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 财务附件元数据测试
 *
 * 验证 FinExpense 和 FinAdvance 的 attachments 字段能正确
 * 存储和返回 JSON 格式的附件元数据。
 */
class FinAttachmentMetadataTest
{
    private static final String SAMPLE_JSON =
        "[{\"fileName\":\"发票.jpg\",\"fileUrl\":\"/files/2026/06/29/abc.jpg\",\"fileSize\":102400}]";

    // ── FinExpense 附件字段 ──

    @Test
    void expenseShouldStoreAndReturnAttachments()
    {
        FinExpense expense = new FinExpense();
        expense.setAttachments(SAMPLE_JSON);

        assertEquals(SAMPLE_JSON, expense.getAttachments());
    }

    @Test
    void expenseAttachmentsShouldDefaultToNull()
    {
        FinExpense expense = new FinExpense();
        assertNull(expense.getAttachments(), "新建费用记录附件应为 null");
    }

    @Test
    void expenseShouldAcceptEmptyArrayJson()
    {
        FinExpense expense = new FinExpense();
        expense.setAttachments("[]");
        assertEquals("[]", expense.getAttachments());
    }

    @Test
    void expenseShouldAcceptMultipleAttachments()
    {
        String multiJson = "[" +
            "{\"fileName\":\"发票1.jpg\",\"fileUrl\":\"/f/1.jpg\",\"fileSize\":50000}," +
            "{\"fileName\":\"发票2.pdf\",\"fileUrl\":\"/f/2.pdf\",\"fileSize\":200000}" +
            "]";
        FinExpense expense = new FinExpense();
        expense.setAttachments(multiJson);

        String stored = expense.getAttachments();
        assertTrue(stored.contains("发票1.jpg"), "应包含第一个附件");
        assertTrue(stored.contains("发票2.pdf"), "应包含第二个附件");
    }

    @Test
    void expenseShouldPreserveOtherFieldsWithAttachments()
    {
        FinExpense expense = new FinExpense();
        expense.setExpenseId(1L);
        expense.setExpenseNo("FY202606290001");
        expense.setAttachments(SAMPLE_JSON);

        assertEquals(1L, expense.getExpenseId(), "附件不应影响其他字段");
        assertEquals("FY202606290001", expense.getExpenseNo());
        assertEquals(SAMPLE_JSON, expense.getAttachments());
    }

    // ── FinAdvance 附件字段 ──

    @Test
    void advanceShouldStoreAndReturnAttachments()
    {
        FinAdvance advance = new FinAdvance();
        advance.setAttachments(SAMPLE_JSON);

        assertEquals(SAMPLE_JSON, advance.getAttachments());
    }

    @Test
    void advanceAttachmentsShouldDefaultToNull()
    {
        FinAdvance advance = new FinAdvance();
        assertNull(advance.getAttachments(), "新建借支记录附件应为 null");
    }

    @Test
    void advanceShouldAcceptEmptyArrayJson()
    {
        FinAdvance advance = new FinAdvance();
        advance.setAttachments("[]");
        assertEquals("[]", advance.getAttachments());
    }

    @Test
    void advanceShouldPreserveOtherFieldsWithAttachments()
    {
        FinAdvance advance = new FinAdvance();
        advance.setAdvanceId(42L);
        advance.setAdvanceNo("JZ202606290001");
        advance.setAttachments(SAMPLE_JSON);

        assertEquals(42L, advance.getAdvanceId(), "附件不应影响其他字段");
        assertEquals("JZ202606290001", advance.getAdvanceNo());
        assertEquals(SAMPLE_JSON, advance.getAttachments());
    }

    // ── JSON 格式验证 ──

    @Test
    void attachmentJsonShouldContainRequiredFields()
    {
        assertTrue(SAMPLE_JSON.contains("fileName"), "附件 JSON 应包含 fileName");
        assertTrue(SAMPLE_JSON.contains("fileUrl"), "附件 JSON 应包含 fileUrl");
        assertTrue(SAMPLE_JSON.contains("fileSize"), "附件 JSON 应包含 fileSize");
    }
}
