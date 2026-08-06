package com.junsong.member.domain.vo;

import java.util.List;

public class ConfigSyncExecuteRequest
{
    private Long batchId;
    private Long previewVersion;
    private List<ConfigSyncDecision> decisions;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long value) { batchId = value; }
    public Long getPreviewVersion() { return previewVersion; }
    public void setPreviewVersion(Long value) { previewVersion = value; }
    public List<ConfigSyncDecision> getDecisions() { return decisions; }
    public void setDecisions(List<ConfigSyncDecision> value) { decisions = value; }
}
