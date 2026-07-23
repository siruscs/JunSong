package com.junsong.system.service;

import java.util.List;
import java.util.Map;
import com.junsong.system.domain.SysOperatingTask;
import com.junsong.system.domain.SysOperatingTaskLog;

/**
 * 经营任务 Service 接口
 *
 * @author junsong
 */
public interface ISysOperatingTaskService
{
    /**
     * 查询经营任务列表（按授权门店过滤）
     */
    public List<SysOperatingTask> selectOperatingTaskList(Map<String, Object> params);

    /**
     * 查询经营任务详情
     */
    public SysOperatingTask selectOperatingTaskById(Long taskId);

    /**
     * 幂等创建经营任务（idempotency_key 已存在则返回已存在任务）
     */
    public SysOperatingTask createOrUpdateTask(SysOperatingTask task);

    /**
     * 认领任务（PENDING -> IN_PROGRESS）
     */
    public int claimTask(Long taskId);

    /**
     * 完成任务（IN_PROGRESS/REOPENED -> DONE）
     */
    public int completeTask(Long taskId, String handlerNote);

    /**
     * 驳回任务（IN_PROGRESS/REOPENED -> REJECTED）
     */
    public int rejectTask(Long taskId, String rejectReason);

    /**
     * 重开任务（DONE/REJECTED -> IN_PROGRESS，reopen_count+1）
     */
    public int reopenTask(Long taskId, String reason);

    /**
     * 查询任务操作日志
     */
    public List<SysOperatingTaskLog> selectTaskLogs(Long taskId);

    /**
     * 当前用户待办计数（分配给当前用户的 + 当前门店内 PENDING 待认领的）
     */
    public int countPendingTasks();
}
