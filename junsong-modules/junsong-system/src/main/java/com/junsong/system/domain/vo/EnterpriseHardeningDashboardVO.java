package com.junsong.system.domain.vo;

/**
 * R25企业级硬化看板 VO
 */
public class EnterpriseHardeningDashboardVO
{
    /** 高风险审计数 */
    private Integer highRiskAuditCount;

    /** 归档候选数据量 */
    private Long archiveCandidateCount;

    /** 待处理严重告警数 */
    private Integer openCriticalAlertCount;

    /** 口径说明 */
    private String basis;

    public Integer getHighRiskAuditCount()
    {
        return highRiskAuditCount;
    }

    public void setHighRiskAuditCount(Integer highRiskAuditCount)
    {
        this.highRiskAuditCount = highRiskAuditCount;
    }

    public Long getArchiveCandidateCount()
    {
        return archiveCandidateCount;
    }

    public void setArchiveCandidateCount(Long archiveCandidateCount)
    {
        this.archiveCandidateCount = archiveCandidateCount;
    }

    public Integer getOpenCriticalAlertCount()
    {
        return openCriticalAlertCount;
    }

    public void setOpenCriticalAlertCount(Integer openCriticalAlertCount)
    {
        this.openCriticalAlertCount = openCriticalAlertCount;
    }

    public String getBasis()
    {
        return basis;
    }

    public void setBasis(String basis)
    {
        this.basis = basis;
    }
}
