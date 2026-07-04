package com.junsong.system.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.redis.service.RedisService;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysDashboardHealth;
import com.junsong.system.domain.SysGovernanceTaskLog;
import com.junsong.system.domain.vo.SystemGovernanceTaskVO;
import com.junsong.system.mapper.SysGovernanceTaskLogMapper;
import com.junsong.system.service.ISysDashboardHealthService;
import com.junsong.system.service.impl.SysHealthRuleConfigServiceImpl;

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

    @Autowired
    private SysGovernanceTaskLogMapper governanceTaskLogMapper;

    @Autowired
    private SysHealthRuleConfigServiceImpl healthRuleConfigService;

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
    public AjaxResult governance(@RequestParam(defaultValue = "false") boolean includeArchived)
    {
        Map<String, Object> result = new HashMap<>();

        long emptyMenuCount = queryCount("SELECT COUNT(*) FROM sys_menu WHERE menu_type = 'C' AND (component IS NULL OR component = '')");
        long disabledUserCount = queryCount("SELECT COUNT(*) FROM sys_user WHERE status = '1'");
        long lockedUserCount = 0L;
        long roleWithoutUserCount = queryCount("SELECT COUNT(*) FROM sys_role r LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id WHERE ur.user_id IS NULL AND r.del_flag = '0'");
        long menuWithoutRoleCount = queryCount("SELECT COUNT(*) FROM sys_menu m LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id WHERE m.menu_type IN ('C', 'F') AND rm.role_id IS NULL");
        long recentLoginFailCount = queryCount("SELECT COUNT(*) FROM sys_logininfor WHERE status = '1' AND access_time > NOW() - INTERVAL 1 DAY");
        long todayLoginSuccessCount = queryCount("SELECT COUNT(*) FROM sys_logininfor WHERE status = '0' AND access_time >= CURDATE()");
        long todayLoginFailCount = queryCount("SELECT COUNT(*) FROM sys_logininfor WHERE status = '1' AND access_time >= CURDATE()");
        long recentHighRiskOperCount = queryCount("SELECT COUNT(*) FROM sys_oper_log WHERE business_type IN (2, 3) AND oper_time > NOW() - INTERVAL 7 DAY");
        long unreadNotificationCount = queryCount("SELECT COUNT(*) FROM sys_notification WHERE is_read = '0'");

        result.put("emptyMenuCount", emptyMenuCount);
        result.put("disabledUserCount", disabledUserCount);
        result.put("lockedUserCount", lockedUserCount);
        result.put("roleWithoutUserCount", roleWithoutUserCount);
        result.put("menuWithoutRoleCount", menuWithoutRoleCount);
        result.put("recentLoginFailCount", recentLoginFailCount);
        result.put("todayLoginSuccessCount", todayLoginSuccessCount);
        result.put("todayLoginFailCount", todayLoginFailCount);
        result.put("recentHighRiskOperCount", recentHighRiskOperCount);
        result.put("unreadNotificationCount", unreadNotificationCount);
        result.put("loginFailTopUsers", queryLoginFailTop());

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
        addWarning(warnings, "recentHighRiskOperCount", recentHighRiskOperCount, "warning",
                "近7天有 " + recentHighRiskOperCount + " 次高危操作(更新/删除)，请留意异常变更");
        addWarning(warnings, "unreadNotificationCount", unreadNotificationCount, "info",
                "有 " + unreadNotificationCount + " 条未读系统通知待处理");
        result.put("governanceWarnings", warnings);

        // R8-E: 治理任务化 —— 将风险转化为可执行任务列表
        long downServiceCount = 0L;
        try {
            SysDashboardHealth health = dashboardHealthService.getDashboardHealth();
            downServiceCount = health.getDownServiceCount();
        } catch (Exception ignored) {
            // health service unavailable, treat as 0
        }
        List<SystemGovernanceTaskVO> governanceTasks = buildGovernanceTasks(
                emptyMenuCount, recentLoginFailCount, menuWithoutRoleCount,
                roleWithoutUserCount, recentHighRiskOperCount, downServiceCount);

        // R11-H: 为每个任务附加最近操作记录
        // R12-F: 标记已归档任务（DONE/IGNORED），默认列表过滤掉已归档任务
        markAndFilterArchivedTasks(governanceTasks, includeArchived);
        result.put("governanceTasks", governanceTasks);

        // R10-D: 治理质量评分（读取配置阈值）
        long handled7d = queryCount("SELECT COUNT(*) FROM sys_governance_task_log WHERE action_time > NOW() - INTERVAL 7 DAY");
        result.put("governanceHandled7dCount", handled7d);

        List<String> repeatedTypes = findRepeatedTaskTypes();
        result.put("governanceRepeatedTaskTypes", repeatedTypes);

        int govScore = computeGovernanceQualityScore(emptyMenuCount, recentLoginFailCount, repeatedTypes.size());
        result.put("governanceQualityScore", govScore);

        List<String> govSuggestions = new ArrayList<>();
        if (govScore < 100) {
            if (emptyMenuCount > 0) govSuggestions.add("补全空组件菜单路径");
            if (recentLoginFailCount > healthRuleConfigService.getThreshold("SYS_LOGIN_FAIL_24H", new java.math.BigDecimal(20)).longValue()) {
                govSuggestions.add("排查近24小时登录失败异常");
            }
            if (!repeatedTypes.isEmpty()) govSuggestions.add("重复治理项: " + String.join(", ", repeatedTypes) + "，建议根因修复");
        }
        result.put("governanceQualitySuggestions", govSuggestions);

        return AjaxResult.success(result);
    }

    // ==================== R8-E: 治理任务构建（可测试，纯逻辑无 JDBC）====================

    /**
     * 根据治理指标构建可执行任务列表，按 HIGH / MEDIUM / LOW 排序。
     */
    List<SystemGovernanceTaskVO> buildGovernanceTasks(
            long emptyMenuCount, long recentLoginFailCount, long menuWithoutRoleCount,
            long roleWithoutUserCount, long recentHighRiskOperCount, long downServiceCount) {
        List<SystemGovernanceTaskVO> tasks = new ArrayList<>();

        if (downServiceCount > 0) {
            tasks.add(buildTask("DOWN_SERVICE", "HIGH",
                    "服务可用性异常",
                    "有 " + downServiceCount + " 个服务离线，可能影响业务访问。",
                    "排查网关、注册中心和对应模块进程，优先恢复服务。",
                    "/monitor/server", (int) downServiceCount));
        }
        if (emptyMenuCount > 0) {
            tasks.add(buildTask("EMPTY_MENU", "HIGH",
                    "菜单缺少组件路径",
                    "有 " + emptyMenuCount + " 个菜单项(C)缺少组件路径，可能导致页面白屏。",
                    "进入菜单管理补全组件路径或删除无效菜单。",
                    "/system/menu", (int) emptyMenuCount));
        }
        // R10-FIX-E: 使用配置阈值，与 computeGovernanceQualityScore 口径一致，
        // 避免配置改成 5 后质量分扣分但治理任务不出现。
        long loginFailThreshold = healthRuleConfigService.getThreshold(
                "SYS_LOGIN_FAIL_24H", new java.math.BigDecimal(20)).longValue();
        boolean loginFailEnabled = healthRuleConfigService.isEnabled("SYS_LOGIN_FAIL_24H", true);
        if (loginFailEnabled && recentLoginFailCount > loginFailThreshold) {
            tasks.add(buildTask("LOGIN_FAIL", "HIGH",
                    "登录失败激增",
                    "近24小时有 " + recentLoginFailCount + " 次登录失败，可能存在暴力破解。",
                    "查看登录日志，确认是否存在异常 IP 并考虑封禁。",
                    "/monitor/logininfor", (int) recentLoginFailCount));
        }
        if (menuWithoutRoleCount > 0) {
            tasks.add(buildTask("MENU_WITHOUT_ROLE", "MEDIUM",
                    "菜单未关联角色",
                    "有 " + menuWithoutRoleCount + " 个菜单/按钮未关联任何角色，可能存在权限遗漏。",
                    "进入角色管理为菜单/按钮分配对应角色。",
                    "/system/role", (int) menuWithoutRoleCount));
        }
        if (recentHighRiskOperCount > 0) {
            tasks.add(buildTask("HIGH_RISK_OPER", "MEDIUM",
                    "高危操作告警",
                    "近7天有 " + recentHighRiskOperCount + " 次更新/删除操作，请留意异常变更。",
                    "查看操作日志，确认高危操作是否为预期变更。",
                    "/monitor/operlog", (int) recentHighRiskOperCount));
        }
        if (roleWithoutUserCount > 0) {
            tasks.add(buildTask("ROLE_WITHOUT_USER", "LOW",
                    "空闲角色清理",
                    "有 " + roleWithoutUserCount + " 个角色未分配给任何用户，可能是冗余角色。",
                    "确认角色是否仍需要，不需要则删除以减少治理噪音。",
                    "/system/role", (int) roleWithoutUserCount));
        }

        // 按 HIGH / MEDIUM / LOW 排序
        tasks.sort((a, b) -> severityRank(a.getSeverity()) - severityRank(b.getSeverity()));
        return tasks;
    }

    private SystemGovernanceTaskVO buildTask(String taskType, String severity, String title,
                                              String reason, String action, String targetRoute, int count) {
        SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
        task.setTaskType(taskType);
        task.setSeverity(severity);
        task.setTitle(title);
        task.setReason(reason);
        task.setAction(action);
        task.setTargetRoute(targetRoute);
        task.setCount(count);
        return task;
    }

    private int severityRank(String severity) {
        if ("HIGH".equals(severity)) return 0;
        if ("MEDIUM".equals(severity)) return 1;
        return 2;
    }

    // ==================== R12-F: 归档过滤（可测试，纯逻辑）====================

    /**
     * R12-F: 根据最新治理记录标记任务归档状态，并在 includeArchived=false 时过滤掉已归档任务。
     * 已归档条件：最新记录的 actionType 为 DONE 或 IGNORED。
     */
    List<SystemGovernanceTaskVO> markAndFilterArchivedTasks(List<SystemGovernanceTaskVO> tasks, boolean includeArchived) {
        for (SystemGovernanceTaskVO task : tasks) {
            try {
                SysGovernanceTaskLog latest = governanceTaskLogMapper.selectLatestLogByType(task.getTaskType());
                if (latest != null) {
                    task.setLastActionType(latest.getActionType());
                    task.setLastHandlerName(latest.getHandlerName());
                    task.setLastActionTime(latest.getActionTime());
                    task.setLastHandlerNote(latest.getHandlerNote());
                    if ("DONE".equals(latest.getActionType()) || "IGNORED".equals(latest.getActionType())) {
                        task.setArchived(true);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (!includeArchived) {
            tasks.removeIf(SystemGovernanceTaskVO::isArchived);
        }
        return tasks;
    }

    // ==================== R10-D: 治理质量评分（可测试，纯逻辑）====================

    int computeGovernanceQualityScore(long emptyMenuCount, long recentLoginFailCount, int repeatedCount) {
        int score = 100;
        long emptyThreshold = healthRuleConfigService.getThreshold("SYS_EMPTY_MENU_COUNT", java.math.BigDecimal.ZERO).longValue();
        long loginThreshold = healthRuleConfigService.getThreshold("SYS_LOGIN_FAIL_24H", new java.math.BigDecimal(20)).longValue();

        boolean emptyEnabled = healthRuleConfigService.isEnabled("SYS_EMPTY_MENU_COUNT", true);
        boolean loginEnabled = healthRuleConfigService.isEnabled("SYS_LOGIN_FAIL_24H", true);

        if (emptyEnabled && emptyMenuCount > emptyThreshold) score -= 20;
        if (loginEnabled && recentLoginFailCount > loginThreshold) score -= 20;
        if (repeatedCount > 0) score -= 10;

        return Math.max(score, 0);
    }

    List<String> findRepeatedTaskTypes() {
        List<String> result = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT task_type, COUNT(*) AS cnt FROM sys_governance_task_log "
                + "WHERE action_time > NOW() - INTERVAL 7 DAY GROUP BY task_type HAVING cnt > 1");
            for (Map<String, Object> row : rows) {
                result.add((String) row.get("task_type"));
            }
        } catch (Exception e) {
            // table may not exist yet
        }
        return result;
    }

    private List<Map<String, Object>> queryLoginFailTop()
    {
        try
        {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT user_name AS userName, ipaddr AS ipaddr, COUNT(*) AS failCount "
                    + "FROM sys_logininfor WHERE status = '1' AND access_time > NOW() - INTERVAL 1 DAY "
                    + "GROUP BY user_name, ipaddr ORDER BY failCount DESC LIMIT 5");
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : rows)
            {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userName", row.get("userName"));
                item.put("ipaddr", row.get("ipaddr"));
                item.put("failCount", row.get("failCount"));
                result.add(item);
            }
            return result;
        }
        catch (Exception e)
        {
            return new ArrayList<>();
        }
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

    @PostMapping("/governance/action")
    @RequiresPermissions("system:dashboard:governance")
    public R<String> recordGovernanceAction(@RequestBody SysGovernanceTaskLog log)
    {
        if (log.getTaskType() == null || log.getTaskType().trim().isEmpty())
        {
            return R.fail("taskType 不能为空");
        }
        String actionType = log.getActionType();
        if (actionType == null || (!"ACK".equals(actionType) && !"DONE".equals(actionType) && !"IGNORED".equals(actionType) && !"REOPEN".equals(actionType)))
        {
            return R.fail("actionType 必须是 ACK、DONE、IGNORED 或 REOPEN");
        }
        if (("DONE".equals(actionType) || "IGNORED".equals(actionType) || "REOPEN".equals(actionType))
                && (log.getHandlerNote() == null || log.getHandlerNote().trim().isEmpty()))
        {
            return R.fail("处理完成、忽略或重开时必须填写处理备注");
        }

        // R11-H: 重算 severity/count，不信任前端传入值
        SystemGovernanceTaskVO currentTask = findCurrentGovernanceTask(log.getTaskType());
        if (currentTask != null)
        {
            log.setSeverity(currentTask.getSeverity());
            log.setCountValue(currentTask.getCount());
        }
        else
        {
            log.setSeverity("LOW");
            log.setCountValue(0);
            String note = log.getHandlerNote() != null ? log.getHandlerNote() : "";
            log.setHandlerNote(note + (note.isEmpty() ? "" : " | ") + "当前无活动风险，仅记录知晓");
        }

        log.setHandlerId(SecurityUtils.getUserId());
        log.setHandlerName(SecurityUtils.getUsername());
        log.setActionTime(new Date());
        governanceTaskLogMapper.insertGovernanceTaskLog(log);
        return R.ok("记录成功");
    }

    /**
     * R11-H: 查找当前治理任务列表中匹配 taskType 的任务。
     * 如果当前无该类型风险，返回 null。
     */
    SystemGovernanceTaskVO findCurrentGovernanceTask(String taskType)
    {
        try
        {
            long emptyMenuCount = queryCount("SELECT COUNT(*) FROM sys_menu WHERE menu_type = 'C' AND (component IS NULL OR component = '')");
            long recentLoginFailCount = queryCount("SELECT COUNT(*) FROM sys_logininfor WHERE status = '1' AND access_time > NOW() - INTERVAL 1 DAY");
            long menuWithoutRoleCount = queryCount("SELECT COUNT(*) FROM sys_menu m LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id WHERE m.menu_type IN ('C', 'F') AND rm.role_id IS NULL");
            long roleWithoutUserCount = queryCount("SELECT COUNT(*) FROM sys_role r LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id WHERE ur.user_id IS NULL AND r.del_flag = '0'");
            long recentHighRiskOperCount = queryCount("SELECT COUNT(*) FROM sys_oper_log WHERE business_type IN (2, 3) AND oper_time > NOW() - INTERVAL 7 DAY");
            long downServiceCount = 0L;
            try {
                SysDashboardHealth health = dashboardHealthService.getDashboardHealth();
                downServiceCount = health.getDownServiceCount();
            } catch (Exception ignored) {}
            List<SystemGovernanceTaskVO> tasks = buildGovernanceTasks(
                    emptyMenuCount, recentLoginFailCount, menuWithoutRoleCount,
                    roleWithoutUserCount, recentHighRiskOperCount, downServiceCount);
            return tasks.stream().filter(t -> taskType.equals(t.getTaskType())).findFirst().orElse(null);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @GetMapping("/governance/logs")
    @RequiresPermissions("system:governanceLog:list")
    public R<List<SysGovernanceTaskLog>> getGovernanceLogs(@RequestParam String taskType)
    {
        return R.ok(governanceTaskLogMapper.selectLogsByType(taskType));
    }
}
