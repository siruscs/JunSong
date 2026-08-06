package com.junsong.member.domain;

import java.util.Date;

public class MemMemberStatusHistory
{
    private Long historyId;
    private Long tenantId;
    private Long deptId;
    private Long memberId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private String operator;
    private Date changedAt;

    public Long getHistoryId() { return historyId; }
    public void setHistoryId(Long v) { historyId = v; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long v) { tenantId = v; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long v) { deptId = v; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long v) { memberId = v; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String v) { fromStatus = v; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String v) { toStatus = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }
    public String getOperator() { return operator; }
    public void setOperator(String v) { operator = v; }
    public Date getChangedAt() { return changedAt; }
    public void setChangedAt(Date v) { changedAt = v; }
}
