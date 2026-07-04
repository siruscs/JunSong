package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.domain.SysOperationAlertEvent;
import com.junsong.system.domain.vo.AlertEventQueryParams;

/**
 * R25操作告警事件 数据层
 */
public interface SysOperationAlertEventMapper
{
    /**
     * 新增告警事件
     */
    int insertAlertEvent(SysOperationAlertEvent event);

    /**
     * 按去重键查询OPEN状态事件
     */
    SysOperationAlertEvent selectOpenByDedupKey(String dedupKey);

    /**
     * 命中次数+1
     */
    int incrementHitCount(Long eventId);

    /**
     * 更新事件状态
     */
    int updateEventStatus(Long eventId, String status);

    /**
     * 查询告警事件列表
     */
    List<SysOperationAlertEvent> selectAlertEvents(AlertEventQueryParams params);

    /**
     * 统计OPEN且CRITICAL的告警数
     */
    int countOpenCritical();
}
