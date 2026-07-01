package com.junsong.member.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PII 脱敏工具方法单元测试
 */
class PiiMaskingTest
{
    // ==================== 手机号脱敏 ====================

    @Test
    void maskPhone_normal11Digit() {
        assertEquals("138****1234", MemMemberController.maskPhone("13812341234"));
    }

    @Test
    void maskPhone_shortString() {
        assertEquals("***", MemMemberController.maskPhone("1234567"));
    }

    @Test
    void maskPhone_null() {
        assertNull(MemMemberController.maskPhone(null));
    }

    @Test
    void maskPhone_8chars() {
        assertEquals("123****5678", MemMemberController.maskPhone("12345678"));
    }

    // ==================== 身份证脱敏 ====================

    @Test
    void maskIdCard_18digit() {
        assertEquals("110101****5678", MemMemberController.maskIdCard("110101199001015678"));
    }

    @Test
    void maskIdCard_null() {
        assertNull(MemMemberController.maskIdCard(null));
    }

    @Test
    void maskIdCard_shortString() {
        assertEquals("***", MemMemberController.maskIdCard("1234567890"));
    }

    // ==================== 地址脱敏 ====================

    @Test
    void maskAddress_longAddress() {
        assertEquals("北京市朝阳区***", MemMemberController.maskAddress("北京市朝阳区建国路88号SOHO现代城A座"));
    }

    @Test
    void maskAddress_shortAddress() {
        assertEquals("***", MemMemberController.maskAddress("北京"));
    }

    @Test
    void maskAddress_null() {
        assertNull(MemMemberController.maskAddress(null));
    }

    @Test
    void maskAddress_exact6chars() {
        assertEquals("***", MemMemberController.maskAddress("北京市朝阳区"));
    }
}
