package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.SysOperationAlertEvent;
import com.junsong.system.domain.vo.AlertEventQueryParams;

/**
 * R25操作告警 服务层
 */
public interface ISysOperationAlertService
{
    /**
     * 触发告警（按 dedupKey 去重）
     */
    void raiseAlert(String ruleKey, String dedupKey, String sourceType, String sourceId, String severity, String title, String content);

    /**
     * 查询告警事件列表
     */
    List<SysOperationAlertEvent> listEvents(AlertEventQueryParams params);

    /**
     * 确认告警事件
     */
    int ackEvent(Long eventId);

    /**
     * 解决告警事件
     */
    int resolveEvent(Long eventId);
}
