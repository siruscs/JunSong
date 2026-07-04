package com.junsong.system.domain.vo;

public class ActionTouchResultVO {
    private Long logId;
    private String actionId;
    private String channel;
    private String touchStatus;
    private String message;
    private String providerResponse;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTouchStatus() { return touchStatus; }
    public void setTouchStatus(String touchStatus) { this.touchStatus = touchStatus; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getProviderResponse() { return providerResponse; }
    public void setProviderResponse(String providerResponse) { this.providerResponse = providerResponse; }
}
