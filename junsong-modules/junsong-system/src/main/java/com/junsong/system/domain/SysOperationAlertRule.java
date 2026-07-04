package com.junsong.system.domain;

import com.junsong.common.core.web.domain.BaseEntity;

/**
 * R25操作告警规则 sys_operation_alert_rule
 */
public class SysOperationAlertRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long ruleId;

    /** 规则键 */
    private String ruleKey;

    /** 规则名称 */
    private String ruleName;

    /** 来源类型 */
    private String sourceType;

    /** 严重级别 HIGH/MEDIUM/LOW/CRITICAL */
    private String severity;

    /** 阈值配置JSON */
    private String thresholdJson;

    /** 是否启用 1是 0否 */
    private String enabled;

    public Long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId(Long ruleId)
    {
        this.ruleId = ruleId;
    }

    public String getRuleKey()
    {
        return ruleKey;
    }

    public void setRuleKey(String ruleKey)
    {
        this.ruleKey = ruleKey;
    }

    public String getRuleName()
    {
        return ruleName;
    }

    public void setRuleName(String ruleName)
    {
        this.ruleName = ruleName;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public String getThresholdJson()
    {
        return thresholdJson;
    }

    public void setThresholdJson(String thresholdJson)
    {
        this.thresholdJson = thresholdJson;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }
}
