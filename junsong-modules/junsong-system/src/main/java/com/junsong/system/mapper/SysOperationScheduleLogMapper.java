package com.junsong.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.system.domain.SysOperationScheduleLog;

/**
 * R21: 经营调度执行日志 Mapper。
 * 所有方法均为只读或日志写入，不修改业务数据。
 */
public interface SysOperationScheduleLogMapper
{
    /**
     * 插入调度执行日志（useGeneratedKeys 回填 logId）
     *
     * @param log 调度日志
     */
    void insertLog(SysOperationScheduleLog log);

    /**
     * 按 logId 查询日志
     *
     * @param logId 日志ID
     * @return 日志记录
     */
    SysOperationScheduleLog selectById(@Param("logId") Long logId);

    /**
     * 更新调度执行状态
     *
     * @param log 调度日志（需包含 logId）
     */
    void updateLog(SysOperationScheduleLog log);

    /**
     * 查询指定 jobCode 的最近 N 条日志
     *
     * @param jobCode 任务编码
     * @param limit   返回条数
     * @return 日志列表
     */
    List<SysOperationScheduleLog> selectRecent(@Param("jobCode") String jobCode, @Param("limit") int limit);

    /**
     * 查询每个 jobCode 的最新一条日志
     *
     * @return 各任务最新日志列表
     */
    List<SysOperationScheduleLog> selectLatestPerJobCode();

    /**
     * 统计最近 24 小时内 FAILED / PARTIAL 状态的记录数
     *
     * @return 失败数
     */
    int countFailedInLast24h();

    /**
     * 查询最近的 FAILED / PARTIAL 日志
     *
     * @return 失败日志列表
     */
    List<SysOperationScheduleLog> selectRecentFailures();
}
