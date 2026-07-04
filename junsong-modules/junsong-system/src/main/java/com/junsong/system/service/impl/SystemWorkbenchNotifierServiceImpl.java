package com.junsong.system.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.junsong.system.domain.SysNotification;
import com.junsong.system.domain.vo.WorkbenchTaskVO;
import com.junsong.system.service.ISysNotificationService;
import com.junsong.system.service.ISystemWorkbenchNotifierService;
import com.junsong.system.service.ISystemWorkbenchService;

/**
 * 工作台高优先级任务通知调度服务实现（R7-C）。
 *
 * 流程：
 * 1. 聚合工作台任务，仅处理 severity == HIGH 的任务。
 * 2. 按任务来源解析接收者（基于菜单权限/超管角色）。
 * 3. 构建 SysNotification，dedupKey = sourceModule:taskType:bizId，幂等发送。
 *
 * 接收者查询使用 JdbcTemplate 直读 sys_user/sys_user_role/sys_role_menu/sys_menu（只读）。
 * 查不到授权用户时静默跳过，不抛异常。
 *
 * @author junsong
 */
@Service
public class SystemWorkbenchNotifierServiceImpl implements ISystemWorkbenchNotifierService
{
    private static final Logger log = LoggerFactory.getLogger(SystemWorkbenchNotifierServiceImpl.class);

    /** 通知类型标识 */
    private static final String NOTIFICATION_TYPE = "workbench";

    @Autowired
    private ISystemWorkbenchService systemWorkbenchService;

    @Autowired
    private ISysNotificationService notificationService;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public int notifyHighPriorityTasks()
    {
        List<WorkbenchTaskVO> tasks;
        try
        {
            tasks = systemWorkbenchService.aggregateTasks();
        }
        catch (Exception e)
        {
            log.warn("[WorkbenchNotifier] 聚合工作台任务失败: {}", e.getMessage());
            return 0;
        }
        if (tasks == null || tasks.isEmpty())
        {
            return 0;
        }

        int sent = 0;
        for (WorkbenchTaskVO task : tasks)
        {
            if (!"HIGH".equals(task.getSeverity()))
            {
                continue;
            }
            sent += notifyOneTask(task);
        }
        return sent;
    }

    /**
     * 对单个 HIGH 任务解析接收者并发送通知。
     * R7 回修：按任务 deptId 过滤接收人，非授权门店的用户不收到通知。
     */
    private int notifyOneTask(WorkbenchTaskVO task)
    {
        Set<Long> receiverIds = resolveReceivers(task.getSourceModule(), task.getDeptId());
        if (receiverIds.isEmpty())
        {
            log.debug("[WorkbenchNotifier] 任务 {} 无授权用户，跳过", task.getBizId());
            return 0;
        }

        String dedupKey = buildDedupKey(task);
        int sent = 0;
        for (Long userId : receiverIds)
        {
            SysNotification n = buildNotification(userId, task, dedupKey);
            try
            {
                int rows = notificationService.insertNotification(n);
                sent += rows;
            }
            catch (Exception e)
            {
                log.warn("[WorkbenchNotifier] 发送通知失败 userId={}, dedupKey={}: {}",
                        userId, dedupKey, e.getMessage());
            }
        }
        return sent;
    }

    /**
     * 按任务来源解析接收者用户ID集合。
     * R7 回修：当任务携带 deptId 时，接收人必须满足：
     *   a) 拥有对应菜单权限，且
     *   b) 被授权该门店（sys_user_dept）或为超管（role_id=1）
     * - FINANCE -> 拥有 finance:dashboard:alerts 或 finance:reviewTask:list 权限的用户
     * - MEMBER  -> 拥有 member:dashboard:list 权限的用户
     * - STOCK   -> 拥有 finance:stock:health 权限的用户
     * - SYSTEM  -> role_id=1 的超管
     *
     * @param sourceModule 任务来源模块
     * @param taskDeptId   任务关联门店ID（可为 null，如 SYSTEM 类任务不按门店过滤）
     */
    private Set<Long> resolveReceivers(String sourceModule, Long taskDeptId)
    {
        if (sourceModule == null)
        {
            return Collections.emptySet();
        }
        switch (sourceModule)
        {
            case "FINANCE":
                return findUserIdsByPermissionsAndDept(taskDeptId, "finance:dashboard:alerts", "finance:reviewTask:list");
            case "MEMBER":
                return findUserIdsByPermissionsAndDept(taskDeptId, "member:dashboard:list");
            case "STOCK":
                return findUserIdsByPermissionsAndDept(taskDeptId, "finance:stock:health");
            case "SYSTEM":
                return findSuperAdminUserIds();
            default:
                return Collections.emptySet();
        }
    }

    /**
     * 构建去重键：sourceModule:taskType:bizId（bizId 为空时用空串占位）。
     */
    private String buildDedupKey(WorkbenchTaskVO task)
    {
        String bizId = task.getBizId() != null ? task.getBizId() : "";
        return task.getSourceModule() + ":" + task.getTaskType() + ":" + bizId;
    }

    /**
     * 构建 SysNotification。
     */
    private SysNotification buildNotification(Long userId, WorkbenchTaskVO task, String dedupKey)
    {
        SysNotification n = new SysNotification();
        n.setUserId(userId);
        n.setTitle(task.getTitle() != null ? task.getTitle() : "工作台高优先级告警");
        StringBuilder content = new StringBuilder();
        if (task.getReason() != null)
        {
            content.append(task.getReason());
        }
        if (task.getSuggestion() != null)
        {
            if (content.length() > 0)
            {
                content.append(" 建议：");
            }
            content.append(task.getSuggestion());
        }
        n.setContent(content.toString());
        n.setType(NOTIFICATION_TYPE);
        n.setLinkUrl(task.getTargetRoute());
        n.setBizId(task.getBizId());
        n.setIsRead("0");
        n.setDedupKey(dedupKey);
        return n;
    }

    // ==================== 接收者查询（可被子类覆写以测试） ====================

    /**
     * 查询拥有任一指定权限的活跃用户ID集合，并按门店授权过滤。
     *
     * R7 回修：当 taskDeptId 不为 null 时，接收人必须满足以下条件之一：
     *   a) 超管（role_id=1），或
     *   b) 在 sys_user_dept 中有 (user_id, taskDeptId, status='0') 记录
     * 当 taskDeptId 为 null 时（如 SYSTEM 类任务），不做门店过滤。
     *
     * 可被子类覆写以注入测试数据。
     *
     * @param taskDeptId 任务关联门店ID（null=不过滤门店）
     * @param perms      所需菜单权限列表
     */
    protected Set<Long> findUserIdsByPermissionsAndDept(Long taskDeptId, String... perms)
    {
        if (jdbcTemplate == null || perms == null || perms.length == 0)
        {
            return Collections.emptySet();
        }
        String placeholders = String.join(",", Collections.nCopies(perms.length, "?"));
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT u.user_id FROM sys_user u "
                + "INNER JOIN sys_user_role ur ON u.user_id = ur.user_id "
                + "INNER JOIN sys_role_menu rm ON ur.role_id = rm.role_id "
                + "INNER JOIN sys_menu m ON rm.menu_id = m.menu_id "
                + "WHERE u.del_flag = '0' AND u.status = '0' AND m.perms IN (" + placeholders + ")");

        // 按门店授权过滤
        List<Object> args = new java.util.ArrayList<>(java.util.Arrays.asList((Object[]) perms));
        if (taskDeptId != null)
        {
            sql.append(" AND (")
               .append("ur.role_id = 1")  // 超管不受门店限制
               .append(" OR EXISTS (SELECT 1 FROM sys_user_dept ud ")
               .append("WHERE ud.user_id = u.user_id AND ud.dept_id = ? AND ud.status = '0')")
               .append(")");
            args.add(taskDeptId);
        }

        try
        {
            List<Long> ids = jdbcTemplate.queryForList(sql.toString(), Long.class, args.toArray());
            return new LinkedHashSet<>(ids);
        }
        catch (Exception e)
        {
            log.warn("[WorkbenchNotifier] 查询授权用户失败 perms={}, deptId={}: {}",
                    Arrays.toString(perms), taskDeptId, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 查询超管（role_id=1）用户ID集合。
     * 可被子类覆写以注入测试数据。
     */
    protected Set<Long> findSuperAdminUserIds()
    {
        if (jdbcTemplate == null)
        {
            return Collections.emptySet();
        }
        String sql = "SELECT DISTINCT u.user_id FROM sys_user u "
                + "INNER JOIN sys_user_role ur ON u.user_id = ur.user_id "
                + "WHERE u.del_flag = '0' AND u.status = '0' AND ur.role_id = 1";
        try
        {
            List<Long> ids = jdbcTemplate.queryForList(sql, Long.class);
            return new LinkedHashSet<>(ids);
        }
        catch (Exception e)
        {
            log.warn("[WorkbenchNotifier] 查询超管失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 仅供测试注入：设置 JdbcTemplate。
     */
    void setJdbcTemplate(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 仅供测试注入：设置工作台服务。
     */
    void setSystemWorkbenchService(ISystemWorkbenchService systemWorkbenchService)
    {
        this.systemWorkbenchService = systemWorkbenchService;
    }

    /**
     * 仅供测试注入：设置通知服务。
     */
    void setNotificationService(ISysNotificationService notificationService)
    {
        this.notificationService = notificationService;
    }
}
