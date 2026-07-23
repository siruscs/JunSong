package com.junsong.system.service;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 授权门店解析器（D1 解决方案）
 *
 * 提取自 SystemWorkbenchServiceImpl.resolveAuthorizedDeptIds()，新代码统一使用，
 * 不回改旧代码。采用多门店授权交集模型：
 * - 超管（admin）：返回 null（表示不过滤，查询全部）
 * - 非超管：查 sys_user_dept 获取授权门店列表；无授权时返回 [-1L]（哨兵值，fail closed）
 *
 * @author junsong
 */
@Component
public class AuthorizedDeptResolver
{
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * 解析当前用户授权门店 ID 列表。
     *
     * @return null=超管不过滤；非空列表=授权门店（无授权时为 [-1L] 哨兵值）
     */
    public List<Long> resolveAuthorizedDeptIds()
    {
        try
        {
            Long userId = SecurityUtils.getUserId();
            if (userId == null)
            {
                return Collections.singletonList(-1L);
            }
            // 超管不过滤
            if (SecurityUtils.isAdmin(userId))
            {
                return null;
            }
            // 非超管：查 sys_user_dept
            if (jdbcTemplate == null)
            {
                return Collections.singletonList(-1L);
            }
            List<Long> deptIds = jdbcTemplate.queryForList(
                    "SELECT dept_id FROM sys_user_dept WHERE user_id = ? AND status = '0'",
                    Long.class, userId);
            if (deptIds == null || deptIds.isEmpty())
            {
                return Collections.singletonList(-1L);
            }
            return deptIds;
        }
        catch (Exception e)
        {
            return Collections.singletonList(-1L);
        }
    }

    /**
     * 判断指定门店是否在授权列表内。
     *
     * @param deptId 待校验门店ID
     * @param authorizedDeptIds 授权门店列表（null 表示超管，返回 true）
     * @return true=可访问；false=无权
     */
    public boolean canAccessDept(Long deptId, List<Long> authorizedDeptIds)
    {
        if (authorizedDeptIds == null)
        {
            // 超管，不过滤
            return true;
        }
        if (deptId == null)
        {
            return false;
        }
        return authorizedDeptIds.contains(deptId);
    }
}
