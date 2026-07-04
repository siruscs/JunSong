package com.junsong.system.domain.vo;

public class ActionTouchRequestVO {
    private String channel;
    private String targetType;
    private String targetRef;
    private String message;
    private Boolean force;

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetRef() { return targetRef; }
    public void setTargetRef(String targetRef) { this.targetRef = targetRef; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getForce() { return force; }
    public void setForce(Boolean force) { this.force = force; }
}
