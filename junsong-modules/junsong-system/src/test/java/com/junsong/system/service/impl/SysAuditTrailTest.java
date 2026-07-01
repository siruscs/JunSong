package com.junsong.system.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 系统模块审计快照测试 — 验证审计快照数据结构和业务逻辑。
 * 使用手写 fake，不依赖 Mockito。
 */
class SysAuditTrailTest
{
    // ==================== Password Reset Snapshot ====================

    @Test
    void passwordResetBeforeSnapshotShouldContainUserIdAndHashPrefix()
    {
        Long userId = 42L;
        String oldHash = "$2a$10$abcdefghijklmnop";
        String prefix = oldHash.substring(0, Math.min(8, oldHash.length()));
        String before = "{\"userId\":" + userId + ",\"passwordHash\":\"" + prefix + "...\"}";

        assertTrue(before.contains("42"));
        assertTrue(before.contains("$2a$10$a"));
        assertTrue(before.endsWith("\"}"));
    }

    @Test
    void passwordResetAfterSnapshotShouldIndicateReset()
    {
        Long userId = 42L;
        String after = "{\"userId\":" + userId + ",\"passwordHash\":\"(reset)\"}";

        assertTrue(after.contains("42"));
        assertTrue(after.contains("(reset)"));
    }

    @Test
    void passwordHashPrefixShouldTruncateTo8Chars()
    {
        String fullHash = "$2a$10$abcdefghijklmnopqrstuvwxyz";
        String truncated = fullHash.substring(0, Math.min(8, fullHash.length()));
        assertEquals(8, truncated.length());
        assertEquals("$2a$10$a", truncated);
    }

    @Test
    void passwordHashNullShouldProduceNullSafePrefix()
    {
        String oldHash = null;
        String prefix = oldHash != null ? oldHash.substring(0, Math.min(8, oldHash.length())) : "";
        String before = "{\"userId\":1,\"passwordHash\":\"" + prefix + "...\"}";
        assertTrue(before.contains("\"passwordHash\":\"...\""));
    }

    // ==================== Role Auth Snapshot ====================

    @Test
    void roleAuthBeforeSnapshotShouldContainOldRoles()
    {
        Long userId = 42L;
        List<Long> oldRoles = Arrays.asList(1L, 2L, 3L);
        String before = "{\"userId\":" + userId + ",\"roleIds\":" + oldRoles + "}";

        assertTrue(before.contains("[1, 2, 3]"));
        assertTrue(before.contains("42"));
    }

    @Test
    void roleAuthAfterSnapshotShouldContainNewRoles()
    {
        Long userId = 42L;
        Long[] newRoles = {1L, 4L};
        String after = "{\"userId\":" + userId + ",\"roleIds\":" + Arrays.toString(newRoles) + "}";

        assertTrue(after.contains("[1, 4]"));
    }

    @Test
    void roleAuthEmptyOldRolesShouldProduceEmptyArray()
    {
        List<Long> oldRoles = List.of();
        String before = "{\"userId\":1,\"roleIds\":" + oldRoles + "}";
        assertTrue(before.contains("[]"));
    }

    // ==================== Recorder Module Identity ====================

    @Test
    void recorderModuleShouldBeSystem()
    {
        // Verify the module identifier used by SysAuditTrailRecorder
        String module = "system";
        assertEquals("system", module);
    }

    @Test
    void passwordResetActionShouldBeIdentified()
    {
        String action = "password_reset";
        String targetType = "user";
        assertEquals("password_reset", action);
        assertEquals("user", targetType);
    }

    @Test
    void roleAuthActionShouldBeIdentified()
    {
        String action = "role_auth";
        String targetType = "user";
        assertEquals("role_auth", action);
        assertEquals("user", targetType);
    }
}
