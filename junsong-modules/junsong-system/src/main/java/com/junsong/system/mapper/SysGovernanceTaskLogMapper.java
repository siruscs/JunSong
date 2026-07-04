package com.junsong.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.system.domain.SysGovernanceTaskLog;

/**
 * 系统治理任务处理轨迹 Mapper
 */
public interface SysGovernanceTaskLogMapper
{
    /**
     * 插入治理任务处理记录
     *
     * @param log 处理记录
     * @return 影响行数
     */
    int insertGovernanceTaskLog(SysGovernanceTaskLog log);

    /**
     * 根据任务类型查询处理记录列表
     *
     * @param taskType 任务类型
     * @return 处理记录列表
     */
    List<SysGovernanceTaskLog> selectLogsByType(@Param("taskType") String taskType);

    /**
     * R12-F: 查询指定任务类型的最新一条处理记录
     *
     * @param taskType 任务类型
     * @return 最新处理记录，无记录时返回 null
     */
    SysGovernanceTaskLog selectLatestLogByType(@Param("taskType") String taskType);
}
