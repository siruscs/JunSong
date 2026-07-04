package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.vo.OperationScheduleDashboardVO;
import com.junsong.system.domain.vo.OperationScheduleLogVO;

/**
 * R21 运维调度日志服务接口。
 * 负责调度执行日志的记录、状态更新和看板聚合。
 */
public interface ISysOperationScheduleLogService {

    /**
     * 记录调度开始，创建 RUNNING 状态日志。
     *
     * @param jobCode     任务编码
     * @param jobName     任务名称
     * @param triggerType 触发类型（CRON / MANUAL）
     * @return 创建的日志 VO（含 logId）
     */
    OperationScheduleLogVO start(String jobCode, String jobName, String triggerType);

    /**
     * 标记执行成功。
     *
     * @param logId         日志ID
     * @param affectedRows  影响行数
     * @param resultSummary 结果摘要
     */
    void finishSuccess(Long logId, int affectedRows, String resultSummary);

    /**
     * 标记执行跳过（条件不满足）。
     *
     * @param logId         日志ID
     * @param resultSummary 跳过原因摘要
     */
    void finishSkipped(Long logId, String resultSummary);

    /**
     * 标记部分成功。
     *
     * @param logId         日志ID
     * @param affectedRows  已处理行数
     * @param resultSummary 结果摘要
     * @param errorMessage  部分失败原因
     */
    void finishPartial(Long logId, int affectedRows, String resultSummary, String errorMessage);

    /**
     * 标记执行失败，记录异常信息。
     *
     * @param logId     日志ID
     * @param throwable 异常对象（类名和消息会被完整记录）
     */
    void finishFailed(Long logId, Throwable throwable);

    /**
     * 获取调度看板聚合数据。
     *
     * @return 看板 VO
     */
    OperationScheduleDashboardVO getDashboard();

    /**
     * 查询最近调度日志。
     *
     * @param jobCode 任务编码（可为 null 表示查全部）
     * @param limit   最大返回条数
     * @return 日志列表（按时间倒序）
     */
    List<OperationScheduleLogVO> listRecent(String jobCode, int limit);
}
