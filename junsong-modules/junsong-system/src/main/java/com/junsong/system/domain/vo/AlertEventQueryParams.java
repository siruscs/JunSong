package com.junsong.system.domain.vo;

/**
 * R25告警事件查询参数
 */
public class AlertEventQueryParams
{
    /** 状态 */
    private String status;

    /** 严重级别 */
    private String severity;

    /** 规则键 */
    private String ruleKey;

    /** 来源类型 */
    private String sourceType;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public String getRuleKey()
    {
        return ruleKey;
    }

    public void setRuleKey(String ruleKey)
    {
        this.ruleKey = ruleKey;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }
}
