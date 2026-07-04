package com.junsong.system.domain;

import java.util.Date;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * R25操作审计快照 sys_operation_audit_snapshot
 */
public class SysOperationAuditSnapshot extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 审计快照ID */
    private Long auditId;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private String bizId;

    /** 操作类型 */
    private String operation;

    /** 风险级别 HIGH/MEDIUM/LOW */
    private String riskLevel;

    /** 变更前快照JSON */
    private String beforeSnapshot;

    /** 变更后快照JSON */
    private String afterSnapshot;

    /** 差异摘要 */
    private String diffSummary;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人 */
    private String operatorName;

    /** 请求IP */
    private String requestIp;

    public Long getAuditId()
    {
        return auditId;
    }

    public void setAuditId(Long auditId)
    {
        this.auditId = auditId;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public String getBizId()
    {
        return bizId;
    }

    public void setBizId(String bizId)
    {
        this.bizId = bizId;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public String getRiskLevel()
    {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel)
    {
        this.riskLevel = riskLevel;
    }

    public String getBeforeSnapshot()
    {
        return beforeSnapshot;
    }

    public void setBeforeSnapshot(String beforeSnapshot)
    {
        this.beforeSnapshot = beforeSnapshot;
    }

    public String getAfterSnapshot()
    {
        return afterSnapshot;
    }

    public void setAfterSnapshot(String afterSnapshot)
    {
        this.afterSnapshot = afterSnapshot;
    }

    public String getDiffSummary()
    {
        return diffSummary;
    }

    public void setDiffSummary(String diffSummary)
    {
        this.diffSummary = diffSummary;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getRequestIp()
    {
        return requestIp;
    }

    public void setRequestIp(String requestIp)
    {
        this.requestIp = requestIp;
    }

    @Override
    public Date getCreateTime()
    {
        return super.getCreateTime();
    }
}
