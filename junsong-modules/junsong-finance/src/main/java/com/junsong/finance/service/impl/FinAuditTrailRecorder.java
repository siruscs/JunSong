package com.junsong.finance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 财务模块审计快照记录器。
 * 将高危操作的 before/after 快照写入 sys_audit_trail。
 * 使用 REQUIRES_NEW 事务传播，确保审计失败不影响主业务事务。
 */
@Component
public class FinAuditTrailRecorder
{
    private static final String INSERT_SQL =
            "INSERT INTO sys_audit_trail (module, action, target_type, target_id, before_snapshot, after_snapshot, operator, dept_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(String action, String targetType, String targetId,
                       String beforeSnapshot, String afterSnapshot)
    {
        String operator = safeGetUsername();
        Long deptId = safeGetDeptId();
        try
        {
            jdbcTemplate.update(INSERT_SQL, "finance", action, targetType, targetId,
                    beforeSnapshot, afterSnapshot, operator, deptId);
        }
        catch (Exception ignored)
        {
            // 审计写入失败不阻断业务（REQUIRES_NEW 事务会自行回滚，不影响外层事务）
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
