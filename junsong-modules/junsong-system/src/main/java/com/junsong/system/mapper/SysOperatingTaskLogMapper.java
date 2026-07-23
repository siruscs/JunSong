package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.domain.SysOperatingTaskLog;

/**
 * 经营任务操作日志表 数据层
 *
 * @author junsong
 */
public interface SysOperatingTaskLogMapper
{
    /**
     * 新增任务操作日志
     */
    public int insertTaskLog(SysOperatingTaskLog log);

    /**
     * 按任务ID查询操作日志（按 create_time 升序）
     */
    public List<SysOperatingTaskLog> selectLogsByTaskId(Long taskId);
}
