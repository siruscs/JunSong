package com.junsong.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * R25操作告警事件 sys_operation_alert_event
 */
public class SysOperationAlertEvent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 事件ID */
    private Long eventId;

    /** 规则键 */
    private String ruleKey;

    /** 去重键 */
    private String dedupKey;

    /** 来源类型 */
    private String sourceType;

    /** 来源业务ID */
    private String sourceId;

    /** 严重级别 */
    private String severity;

    /** 状态 OPEN/ACKED/RESOLVED */
    private String status;

    /** 告警标题 */
    private String title;

    /** 告警内容 */
    private String content;

    /** 首次发现时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;

    /** 最近发现时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenTime;

    /** 命中次数 */
    private Integer hitCount;

    public Long getEventId()
    {
        return eventId;
    }

    public void setEventId(Long eventId)
    {
        this.eventId = eventId;
    }

    public String getRuleKey()
    {
        return ruleKey;
    }

    public void setRuleKey(String ruleKey)
    {
        this.ruleKey = ruleKey;
    }

    public String getDedupKey()
    {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey)
    {
        this.dedupKey = dedupKey;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(String sourceId)
    {
        this.sourceId = sourceId;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Date getFirstSeenTime()
    {
        return firstSeenTime;
    }

    public void setFirstSeenTime(Date firstSeenTime)
    {
        this.firstSeenTime = firstSeenTime;
    }

    public Date getLastSeenTime()
    {
        return lastSeenTime;
    }

    public void setLastSeenTime(Date lastSeenTime)
    {
        this.lastSeenTime = lastSeenTime;
    }

    public Integer getHitCount()
    {
        return hitCount;
    }

    public void setHitCount(Integer hitCount)
    {
        this.hitCount = hitCount;
    }
}
