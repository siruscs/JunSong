package com.junsong.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysOperatingTask;
import com.junsong.system.domain.SysOperatingTaskLog;
import com.junsong.system.mapper.SysOperatingTaskLogMapper;
import com.junsong.system.mapper.SysOperatingTaskMapper;
import com.junsong.system.service.AuthorizedDeptResolver;
import com.junsong.system.service.ISysOperatingTaskService;

/**
 * 经营任务 Service 实现
 *
 * 状态机：PENDING -> IN_PROGRESS -> DONE/REJECTED -> REOPENED -> IN_PROGRESS
 * 所有状态更新使用条件更新（WHERE status=? AND version=?），检查 affected rows == 1。
 * 操作日志与状态更新在同一事务内写入。
 *
 * @author junsong
 */
@Service
public class SysOperatingTaskServiceImpl implements ISysOperatingTaskService
{
    // ==================== 状态常量 ====================
    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    static final String STATUS_DONE = "DONE";
    static final String STATUS_REJECTED = "REJECTED";
    static final String STATUS_REOPENED = "REOPENED";

    // ==================== 优先级常量 ====================
    static final String PRIORITY_URGENT = "URGENT";
    static final String PRIORITY_HIGH = "HIGH";
    static final String PRIORITY_MEDIUM = "MEDIUM";
    static final String PRIORITY_LOW = "LOW";

    // ==================== 日志动作常量 ====================
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_CLAIM = "CLAIM";
    private static final String ACTION_COMPLETE = "COMPLETE";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_REOPEN = "REOPEN";

    @Autowired
    private SysOperatingTaskMapper operatingTaskMapper;

    @Autowired
    private SysOperatingTaskLogMapper operatingTaskLogMapper;

    @Autowired
    private AuthorizedDeptResolver authorizedDeptResolver;

    // ==================== SecurityUtils 包装方法（便于测试覆盖） ====================

    /**
     * 获取当前用户ID（包级私有，测试可覆盖）
     */
    Long currentUserId()
    {
        return SecurityUtils.getUserId();
    }

    /**
     * 获取当前用户名（包级私有，测试可覆盖）
     */
    String currentUsername()
    {
        return SecurityUtils.getUsername();
    }

    // ==================== 查询方法 ====================

    @Override
    public List<SysOperatingTask> selectOperatingTaskList(Map<String, Object> params)
    {
        // 解析授权门店，非 admin 时带 deptIds 过滤
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        if (authorizedDeptIds != null)
        {
            params.put("deptIds", authorizedDeptIds);
        }
        return operatingTaskMapper.selectOperatingTaskList(params);
    }

    @Override
    public SysOperatingTask selectOperatingTaskById(Long taskId)
    {
        return operatingTaskMapper.selectOperatingTaskById(taskId);
    }

    @Override
    public List<SysOperatingTaskLog> selectTaskLogs(Long taskId)
    {
        return operatingTaskLogMapper.selectLogsByTaskId(taskId);
    }

    @Override
    public int countPendingTasks()
    {
        Long assigneeId = currentUserId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        return operatingTaskMapper.countPendingByAssigneeOrDept(assigneeId, authorizedDeptIds);
    }

    // ==================== 幂等创建 ====================

    @Override
    @Transactional
    public SysOperatingTask createOrUpdateTask(SysOperatingTask task)
    {
        // 构建幂等键：{tenantId}:{sourceModule}:{sourceType}:{sourceId}
        Long tenantId = TenantContext.getTenantId();
        task.setTenantId(tenantId);

        if (task.getIdempotencyKey() == null || task.getIdempotencyKey().isEmpty())
        {
            String sourceId = task.getSourceId() != null
                    ? task.getSourceId()
                    : String.valueOf(task.getDeptId());
            task.setIdempotencyKey(tenantId + ":" + task.getSourceModule() + ":" + task.getSourceType() + ":" + sourceId);
        }

        // 幂等检查：已存在则返回已存在任务（不抛错）
        SysOperatingTask existing = operatingTaskMapper.selectByIdempotencyKey(tenantId, task.getIdempotencyKey());
        if (existing != null)
        {
            return existing;
        }

        // 设置默认值
        if (task.getStatus() == null)
        {
            task.setStatus(STATUS_PENDING);
        }
        if (task.getPriority() == null)
        {
            task.setPriority(PRIORITY_MEDIUM);
        }
        if (task.getReopenCount() == null)
        {
            task.setReopenCount(0);
        }
        if (task.getVersion() == null)
        {
            task.setVersion(0);
        }
        if (task.getDelFlag() == null)
        {
            task.setDelFlag("0");
        }
        // 金额精度 scale=2 HALF_UP
        if (task.getImpactAmount() != null)
        {
            task.setImpactAmount(task.getImpactAmount().setScale(2, RoundingMode.HALF_UP));
        }

        operatingTaskMapper.insertOperatingTask(task);

        // 写入创建日志
        insertLog(task.getTaskId(), ACTION_CREATE, null, STATUS_PENDING, null);

        return task;
    }

    // ==================== 状态流转方法 ====================

    @Override
    @Transactional
    public int claimTask(Long taskId)
    {
        SysOperatingTask task = loadAndVerifyAccess(taskId);
        if (!STATUS_PENDING.equals(task.getStatus()))
        {
            throw new ServiceException("任务当前状态为 " + task.getStatus() + "，无法认领（仅 PENDING 可认领）");
        }

        Long assigneeId = currentUserId();
        String assigneeName = currentUsername();

        int affected = operatingTaskMapper.conditionalUpdateStatus(
                taskId, STATUS_PENDING, task.getVersion(), STATUS_IN_PROGRESS,
                assigneeId, assigneeName, null, null, null);

        if (affected != 1)
        {
            throw new ServiceException("任务已被他人认领或状态已变更，请刷新后重试");
        }

        insertLog(taskId, ACTION_CLAIM, STATUS_PENDING, STATUS_IN_PROGRESS, null);
        return affected;
    }

    @Override
    @Transactional
    public int completeTask(Long taskId, String handlerNote)
    {
        if (handlerNote == null || handlerNote.trim().isEmpty())
        {
            throw new ServiceException("完成任务时处理备注不能为空");
        }

        SysOperatingTask task = loadAndVerifyAccess(taskId);
        String oldStatus = task.getStatus();
        if (!STATUS_IN_PROGRESS.equals(oldStatus) && !STATUS_REOPENED.equals(oldStatus))
        {
            throw new ServiceException("任务当前状态为 " + oldStatus + "，无法完成（仅 IN_PROGRESS/REOPENED 可完成）");
        }

        int affected = operatingTaskMapper.conditionalUpdateStatus(
                taskId, oldStatus, task.getVersion(), STATUS_DONE,
                null, null, handlerNote, null, null);

        if (affected != 1)
        {
            throw new ServiceException("任务状态已变更，请刷新后重试");
        }

        insertLog(taskId, ACTION_COMPLETE, oldStatus, STATUS_DONE, handlerNote);
        return affected;
    }

    @Override
    @Transactional
    public int rejectTask(Long taskId, String rejectReason)
    {
        if (rejectReason == null || rejectReason.trim().isEmpty())
        {
            throw new ServiceException("驳回任务时驳回原因不能为空");
        }

        SysOperatingTask task = loadAndVerifyAccess(taskId);
        String oldStatus = task.getStatus();
        if (!STATUS_IN_PROGRESS.equals(oldStatus) && !STATUS_REOPENED.equals(oldStatus))
        {
            throw new ServiceException("任务当前状态为 " + oldStatus + "，无法驳回（仅 IN_PROGRESS/REOPENED 可驳回）");
        }

        int affected = operatingTaskMapper.conditionalUpdateStatus(
                taskId, oldStatus, task.getVersion(), STATUS_REJECTED,
                null, null, null, rejectReason, null);

        if (affected != 1)
        {
            throw new ServiceException("任务状态已变更，请刷新后重试");
        }

        insertLog(taskId, ACTION_REJECT, oldStatus, STATUS_REJECTED, rejectReason);
        return affected;
    }

    @Override
    @Transactional
    public int reopenTask(Long taskId, String reason)
    {
        if (reason == null || reason.trim().isEmpty())
        {
            throw new ServiceException("重开任务时原因不能为空");
        }

        SysOperatingTask task = loadAndVerifyAccess(taskId);
        String oldStatus = task.getStatus();
        if (!STATUS_DONE.equals(oldStatus) && !STATUS_REJECTED.equals(oldStatus))
        {
            throw new ServiceException("任务当前状态为 " + oldStatus + "，无法重开（仅 DONE/REJECTED 可重开）");
        }

        int newReopenCount = (task.getReopenCount() != null ? task.getReopenCount() : 0) + 1;

        int affected = operatingTaskMapper.conditionalUpdateStatus(
                taskId, oldStatus, task.getVersion(), STATUS_IN_PROGRESS,
                null, null, null, null, newReopenCount);

        if (affected != 1)
        {
            throw new ServiceException("任务状态已变更，请刷新后重试");
        }

        insertLog(taskId, ACTION_REOPEN, oldStatus, STATUS_IN_PROGRESS, reason);
        return affected;
    }

    // ==================== 内部方法 ====================

    /**
     * 加载任务并校验门店访问权限
     */
    private SysOperatingTask loadAndVerifyAccess(Long taskId)
    {
        SysOperatingTask task = operatingTaskMapper.selectOperatingTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在或无权操作");
        }
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        if (!authorizedDeptResolver.canAccessDept(task.getDeptId(), authorizedDeptIds))
        {
            throw new ServiceException("任务不存在或无权操作");
        }
        return task;
    }

    /**
     * 写入操作日志（tenant_id 由 TenantContext 设置）
     */
    private void insertLog(Long taskId, String action, String oldStatus, String newStatus, String note)
    {
        SysOperatingTaskLog log = new SysOperatingTaskLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setTaskId(taskId);
        log.setAction(action);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperatorId(currentUserId());
        log.setOperatorName(currentUsername());
        log.setNote(note);
        operatingTaskLogMapper.insertTaskLog(log);
    }
}
