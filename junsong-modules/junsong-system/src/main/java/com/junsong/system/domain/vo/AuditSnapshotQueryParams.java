package com.junsong.system.domain.vo;

/**
 * R25审计快照查询参数
 */
public class AuditSnapshotQueryParams
{
    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private String bizId;

    /** 风险级别 */
    private String riskLevel;

    /** 操作类型 */
    private String operation;

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

    public String getRiskLevel()
    {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel)
    {
        this.riskLevel = riskLevel;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }
}
