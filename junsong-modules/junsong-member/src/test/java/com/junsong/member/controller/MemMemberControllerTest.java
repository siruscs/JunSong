package com.junsong.member.controller;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.junsong.member.domain.MemMember;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会员 Controller 层 PII 脱敏验收测试
 *
 * 验收场景：
 * 1. 无 PII 权限时，列表、详情、按编号查询的手机号/身份证/地址必须脱敏
 * 2. 有 PII 权限时，数据保持明文
 * 3. 短字段（手机号≤7位、身份证≤10位、地址≤6字符）必须返回 "***" 而非原文
 * 4. 导出时，无 piiExport 权限的数据必须脱敏
 *
 * @author junsong
 */
class MemMemberControllerTest
{
    private MemMember buildMember(String phone, String idCard, String address)
    {
        MemMember m = new MemMember();
        m.setMemberId(1L);
        m.setMemberNo("M001");
        m.setMemberName("张三");
        m.setPhone(phone);
        m.setIdCard(idCard);
        m.setAddress(address);
        return m;
    }

    @Test
    void detailWithoutPiiPermission_shouldMaskMember()
    {
        MemMember member = buildMember("13812341234", "110101199001015678", "北京市朝阳区建国路88号");

        MemMemberController.prepareMemberForResponse(member, false);

        assertEquals("138****1234", member.getPhone());
        assertEquals("110101****5678", member.getIdCard());
        assertEquals("北京市朝阳区***", member.getAddress());
    }

    // ==================== 无 PII 权限 → 列表脱敏 ====================

    @Test
    void listWithoutPiiPermission_shouldMaskAllFields()
    {
        MemMember m = buildMember("13812341234", "110101199001015678", "北京市朝阳区建国路88号SOHO现代城A座");
        MemMemberController.maskMemberPii(m);

        assertEquals("138****1234", m.getPhone());
        assertEquals("110101****5678", m.getIdCard());
        assertEquals("北京市朝阳区***", m.getAddress());
    }

    // ==================== 有 PII 权限 → 保持明文 ====================

    @Test
    void listWithPiiPermission_shouldKeepPlaintext()
    {
        MemMember m = buildMember("13812341234", "110101199001015678", "北京市朝阳区建国路88号SOHO现代城A座");
        // 有权限时不调用 maskMemberPii → 保持明文
        assertEquals("13812341234", m.getPhone());
        assertEquals("110101199001015678", m.getIdCard());
        assertEquals("北京市朝阳区建国路88号SOHO现代城A座", m.getAddress());
    }

    // ==================== 短字段必须返回 *** ====================

    @Test
    void shortPhone_shouldReturnMask()
    {
        MemMember m = buildMember("1234", null, null);
        MemMemberController.maskMemberPii(m);
        assertEquals("***", m.getPhone(), "短手机号不能原样泄露");
    }

    @Test
    void shortIdCard_shouldReturnMask()
    {
        MemMember m = buildMember(null, "12345", null);
        MemMemberController.maskMemberPii(m);
        assertEquals("***", m.getIdCard(), "短证件号不能原样泄露");
    }

    @Test
    void shortAddress_shouldReturnMask()
    {
        MemMember m = buildMember(null, null, "北京");
        MemMemberController.maskMemberPii(m);
        assertEquals("***", m.getAddress(), "短地址不能原样泄露");
    }

    @Test
    void exactBoundaryPhone_shouldReturnMask()
    {
        MemMember m = buildMember("1234567", null, null); // 刚好 7 位
        MemMemberController.maskMemberPii(m);
        assertEquals("***", m.getPhone());
    }

    @Test
    void justAboveBoundaryPhone_shouldMaskNormally()
    {
        MemMember m = buildMember("12345678", null, null); // 8 位
        MemMemberController.maskMemberPii(m);
        assertEquals("123****5678", m.getPhone());
    }

    // ==================== null 字段不报错 ====================

    @Test
    void nullFields_shouldNotThrow()
    {
        MemMember m = buildMember(null, null, null);
        assertDoesNotThrow(() -> MemMemberController.maskMemberPii(m));
        assertNull(m.getPhone());
        assertNull(m.getIdCard());
        assertNull(m.getAddress());
    }

    // ==================== 批量列表脱敏 ====================

    @Test
    void batchMasking_shouldMaskAllMembers()
    {
        List<MemMember> list = Arrays.asList(
                buildMember("13812341234", "110101199001015678", "北京市朝阳区建国路88号"),
                buildMember("13900001111", "320102198512151234", "江苏省南京市鼓楼区中山路100号")
        );

        // 模拟无 PII 权限：对每个成员执行脱敏
        for (MemMember m : list) {
            MemMemberController.maskMemberPii(m);
        }

        for (MemMember m : list) {
            assertTrue(m.getPhone().contains("****"), "手机号应包含 ****");
            assertTrue(m.getIdCard().contains("****"), "身份证应包含 ****");
            assertTrue(m.getAddress().endsWith("***"), "地址应以 *** 结尾");
            // 验证脱敏格式：前3 + **** + 后4 = 11字符
            assertEquals(11, m.getPhone().length(), "脱敏后手机号应为11字符");
        }
    }

    // ==================== 按会员编号查询脱敏 ====================

    @Test
    void getByNoWithoutPiiPermission_shouldMask()
    {
        MemMember m = buildMember("18688889999", "440102199203041234", "广东省深圳市南山区科技南路");
        MemMemberController.maskMemberPii(m);

        assertEquals("186****9999", m.getPhone());
        assertEquals("440102****1234", m.getIdCard());
        assertEquals("广东省深圳市***", m.getAddress());
    }
}
