package com.junsong.system.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.redis.service.RedisService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.domain.SysDashboardHealth;
import com.junsong.system.service.ISysDashboardHealthService;

@RestController
@RequestMapping("/dashboard")
public class SysDashboardController
{
    private static final String DASHBOARD_HEALTH_CACHE_KEY = "dashboard:health";
    private static final long DASHBOARD_CACHE_TTL_SECONDS = 15;

    @Autowired
    private ISysDashboardHealthService dashboardHealthService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RequiresPermissions("monitor:dashboard:view")
    @GetMapping("/health")
    public AjaxResult health()
    {
        Object cached = redisService.getCacheObject(DASHBOARD_HEALTH_CACHE_KEY);
        if (cached != null)
        {
            return AjaxResult.success(cached);
        }
        Object health = dashboardHealthService.getDashboardHealth();
        redisService.setCacheObject(DASHBOARD_HEALTH_CACHE_KEY, health, DASHBOARD_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return AjaxResult.success(health);
    }

    @RequiresPermissions("monitor:dashboard:view")
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", queryCount("SELECT COUNT(*) FROM sys_user WHERE del_flag = '0'"));
        stats.put("roleCount", queryCount("SELECT COUNT(*) FROM sys_role WHERE del_flag = '0'"));
        stats.put("menuCount", queryCount("SELECT COUNT(*) FROM sys_menu"));
        stats.put("deptCount", queryCount("SELECT COUNT(*) FROM sys_dept WHERE del_flag = '0'"));
        stats.put("noticeCount", queryCount("SELECT COUNT(*) FROM sys_notice WHERE del_flag = '0'"));
        stats.put("webhookCount", queryCount("SELECT COUNT(*) FROM webhook_subscription WHERE del_flag = '0'"));

        SysDashboardHealth health = dashboardHealthService.getDashboardHealth();
        stats.put("healthLevel", health.getLevel());
        stats.put("overallScore", health.getOverallScore());
        stats.put("serviceCount", health.getServiceCount());
        stats.put("upServiceCount", health.getUpServiceCount());
        stats.put("downServiceCount", health.getDownServiceCount());
        return AjaxResult.success(stats);
    }

    private long queryCount(String sql)
    {
        try
        {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0L;
        }
        catch (EmptyResultDataAccessException e)
        {
            return 0L;
        }
        catch (Exception e)
        {
            return 0L;
        }
    }

    @RequiresPermissions("monitor:dashboard:view")
    @GetMapping("/governance")
    public AjaxResult governance()
    {
        Map<String, Object> result = new HashMap<>();

        long emptyMenuCount = queryCount("SELECT COUNT(*) FROM sys_menu WHERE menu_type = 'C' AND (component IS NULL OR component = '')");
        long disabledUserCount = queryCount("SELECT COUNT(*) FROM sys_user WHERE status = '1'");
        long lockedUserCount = 0L;
        long roleWithoutUserCount = queryCount("SELECT COUNT(*) FROM sys_role r LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id WHERE ur.user_id IS NULL AND r.del_flag = '0'");
        long menuWithoutRoleCount = queryCount("SELECT COUNT(*) FROM sys_menu m LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id WHERE m.menu_type IN ('C', 'F') AND rm.role_id IS NULL");
        long recentLoginFailCount = queryCount("SELECT COUNT(*) FROM sys_logininfor WHERE status = '1' AND login_time > NOW() - INTERVAL 1 DAY");

        result.put("emptyMenuCount", emptyMenuCount);
        result.put("disabledUserCount", disabledUserCount);
        result.put("lockedUserCount", lockedUserCount);
        result.put("roleWithoutUserCount", roleWithoutUserCount);
        result.put("menuWithoutRoleCount", menuWithoutRoleCount);
        result.put("recentLoginFailCount", recentLoginFailCount);

        List<Map<String, Object>> warnings = new ArrayList<>();
        addWarning(warnings, "emptyMenuCount", emptyMenuCount, "warning",
                "有 " + emptyMenuCount + " 个菜单项(C)缺少组件路径，可能导致页面白屏");
        addWarning(warnings, "disabledUserCount", disabledUserCount, "info",
                "有 " + disabledUserCount + " 个用户已被停用");
        addWarning(warnings, "roleWithoutUserCount", roleWithoutUserCount, "warning",
                "有 " + roleWithoutUserCount + " 个角色未分配给任何用户，可能是冗余角色");
        addWarning(warnings, "menuWithoutRoleCount", menuWithoutRoleCount, "warning",
                "有 " + menuWithoutRoleCount + " 个菜单/按钮未关联任何角色，可能存在权限遗漏");
        addWarning(warnings, "recentLoginFailCount", recentLoginFailCount, "danger",
                "近24小时有 " + recentLoginFailCount + " 次登录失败，请关注是否存在暴力破解");
        result.put("governanceWarnings", warnings);

        return AjaxResult.success(result);
    }

    private void addWarning(List<Map<String, Object>> warnings, String key, long count, String level, String message)
    {
        if (count > 0)
        {
            Map<String, Object> w = new HashMap<>();
            w.put("key", key);
            w.put("count", count);
            w.put("level", level);
            w.put("message", message);
            warnings.add(w);
        }
    }
}
