package com.junsong.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.junsong.system.domain.SysOperatingTask;

/**
 * 经营任务表 数据层
 *
 * 注意：tenant_id 条件由 TenantSqlInterceptor 自动注入到 SELECT/UPDATE/DELETE，
 * INSERT 的 tenant_id 由 TenantInterceptor 自动填充。本接口不需要手写 tenant_id 条件。
 *
 * @author junsong
 */
public interface SysOperatingTaskMapper
{
    /**
     * 查询经营任务列表（支持 deptIds/status/assigneeId/sourceModule 过滤）
     */
    public List<SysOperatingTask> selectOperatingTaskList(Map<String, Object> params);

    /**
     * 查询经营任务详情
     */
    public SysOperatingTask selectOperatingTaskById(Long taskId);

    /**
     * 按幂等键查询（用于幂等创建）
     */
    public SysOperatingTask selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                                   @Param("idempotencyKey") String idempotencyKey);

    /**
     * 新增经营任务（useGeneratedKeys）
     */
    public int insertOperatingTask(SysOperatingTask task);

    /**
     * 条件更新状态（乐观锁）
     * WHERE task_id=? AND status=? AND version=?
     * SET status=newStatus, version=version+1, update_time=NOW()
     * 动态 SET assigneeId/handlerNote/rejectReason/reopenCount
     * 返回 affected rows
     */
    public int conditionalUpdateStatus(@Param("taskId") Long taskId,
                                       @Param("oldStatus") String oldStatus,
                                       @Param("oldVersion") Integer oldVersion,
                                       @Param("newStatus") String newStatus,
                                       @Param("assigneeId") Long assigneeId,
                                       @Param("assigneeName") String assigneeName,
                                       @Param("handlerNote") String handlerNote,
                                       @Param("rejectReason") String rejectReason,
                                       @Param("reopenCount") Integer reopenCount);

    /**
     * 通用更新（用于非状态字段）
     */
    public int updateOperatingTask(SysOperatingTask task);

    /**
     * 按状态和门店统计
     */
    public int countByStatusAndDept(@Param("status") String status, @Param("deptIds") List<Long> deptIds);

    /**
     * 按负责人统计待办（status IN PENDING,IN_PROGRESS,REOPENED）
     */
    public int countPendingByAssigneeOrDept(@Param("assigneeId") Long assigneeId,
                                             @Param("deptIds") List<Long> deptIds);
}
