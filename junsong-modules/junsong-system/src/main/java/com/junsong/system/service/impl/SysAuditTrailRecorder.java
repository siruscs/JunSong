package com.junsong.system.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 系统模块审计快照记录器。
 * 将高危操作的 before/after 快照写入 sys_audit_trail。
 */
@Component
public class SysAuditTrailRecorder
{
    private static final String INSERT_SQL =
            "INSERT INTO sys_audit_trail (module, action, target_type, target_id, before_snapshot, after_snapshot, operator, dept_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void record(String action, String targetType, String targetId,
                       String beforeSnapshot, String afterSnapshot)
    {
        String operator = safeGetUsername();
        Long deptId = safeGetDeptId();
        try
        {
            jdbcTemplate.update(INSERT_SQL, "system", action, targetType, targetId,
                    beforeSnapshot, afterSnapshot, operator, deptId);
        }
        catch (Exception ignored)
        {
            // 审计写入失败不阻断业务
        }
    }

    /**
     * 查询用户当前密码哈希（用于 before 快照）。
     */
    public String queryPasswordHash(Long userId)
    {
        try
        {
            return jdbcTemplate.queryForObject(
                    "SELECT password FROM sys_user WHERE user_id = ?", String.class, userId);
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    private String safeGetUsername()
    {
        try { return SecurityUtils.getUsername(); } catch (Exception e) { return null; }
    }

    private Long safeGetDeptId()
    {
        try { return SecurityUtils.getDeptId(); } catch (Exception e) { return null; }
    }
}
