package com.junsong.member.domain.vo;

import java.util.List;
import java.util.Map;

public class ConfigSyncPreviewRequest
{
    private String syncType;
    private Long sourceRecordId;
    private List<Long> targetDeptIds;
    private String idempotencyKey;
    /** 政策同步时的目标机构到核算周期映射。 */
    private Map<Long, Long> targetPeriodIds;

    public String getSyncType() { return syncType; }
    public void setSyncType(String value) { syncType = value; }
    public Long getSourceRecordId() { return sourceRecordId; }
    public void setSourceRecordId(Long value) { sourceRecordId = value; }
    public List<Long> getTargetDeptIds() { return targetDeptIds; }
    public void setTargetDeptIds(List<Long> value) { targetDeptIds = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { idempotencyKey = value; }
    public Map<Long, Long> getTargetPeriodIds() { return targetPeriodIds; }
    public void setTargetPeriodIds(Map<Long, Long> value) { targetPeriodIds = value; }
}
