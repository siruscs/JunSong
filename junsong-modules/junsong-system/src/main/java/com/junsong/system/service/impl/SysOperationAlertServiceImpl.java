package com.junsong.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import com.junsong.system.domain.SysOperationAlertEvent;
import com.junsong.system.mapper.SysOperationAlertEventMapper;
import com.junsong.system.service.ISysOperationAlertService;
import com.junsong.system.domain.vo.AlertEventQueryParams;

/**
 * R25操作告警 服务实现
 */
@Service
public class SysOperationAlertServiceImpl implements ISysOperationAlertService
{
    private final SysOperationAlertEventMapper alertEventMapper;

    public SysOperationAlertServiceImpl(SysOperationAlertEventMapper alertEventMapper)
    {
        this.alertEventMapper = alertEventMapper;
    }

    @Override
    public void raiseAlert(String ruleKey, String dedupKey, String sourceType, String sourceId, String severity, String title, String content)
    {
        SysOperationAlertEvent existing = alertEventMapper.selectOpenByDedupKey(dedupKey);
        if (existing != null)
        {
            alertEventMapper.incrementHitCount(existing.getEventId());
            return;
        }
        SysOperationAlertEvent event = new SysOperationAlertEvent();
        event.setRuleKey(ruleKey);
        event.setDedupKey(dedupKey);
        event.setSourceType(sourceType);
        event.setSourceId(sourceId == null ? "" : sourceId);
        event.setSeverity(severity);
        event.setStatus("OPEN");
        event.setTitle(title);
        event.setContent(content == null ? "" : content);
        Date now = new Date();
        event.setFirstSeenTime(now);
        event.setLastSeenTime(now);
        event.setHitCount(1);
        event.setCreateTime(now);
        alertEventMapper.insertAlertEvent(event);
    }

    @Override
    public List<SysOperationAlertEvent> listEvents(AlertEventQueryParams params)
    {
        return alertEventMapper.selectAlertEvents(params);
    }

    @Override
    public int ackEvent(Long eventId)
    {
        return alertEventMapper.updateEventStatus(eventId, "ACKED");
    }

    @Override
    public int resolveEvent(Long eventId)
    {
        return alertEventMapper.updateEventStatus(eventId, "RESOLVED");
    }
}
