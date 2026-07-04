package com.junsong.system.domain;

import java.io.Serializable;
import java.util.Date;

public class SysActionCenterTouchLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long logId;
    private String actionId;
    private String sourceType;
    private String sourceId;
    private String channel;
    private String targetType;
    private String targetRef;
    private String touchStatus;
    private String requestDigest;
    private String messageSummary;
    private String providerResponse;
    private String errorMessage;
    private Long operatorId;
    private String operatorName;
    private Date createTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetRef() { return targetRef; }
    public void setTargetRef(String targetRef) { this.targetRef = targetRef; }
    public String getTouchStatus() { return touchStatus; }
    public void setTouchStatus(String touchStatus) { this.touchStatus = touchStatus; }
    public String getRequestDigest() { return requestDigest; }
    public void setRequestDigest(String requestDigest) { this.requestDigest = requestDigest; }
    public String getMessageSummary() { return messageSummary; }
    public void setMessageSummary(String messageSummary) { this.messageSummary = messageSummary; }
    public String getProviderResponse() { return providerResponse; }
    public void setProviderResponse(String providerResponse) { this.providerResponse = providerResponse; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
