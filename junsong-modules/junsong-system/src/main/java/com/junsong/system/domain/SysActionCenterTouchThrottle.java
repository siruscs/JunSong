package com.junsong.system.domain;

import java.io.Serializable;
import java.util.Date;

public class SysActionCenterTouchThrottle implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long throttleId;
    private String throttleKey;
    private String channel;
    private String targetRef;
    private Date lastTouchTime;
    private int touchCount24h;
    private Date createTime;
    private Date updateTime;

    public Long getThrottleId() { return throttleId; }
    public void setThrottleId(Long throttleId) { this.throttleId = throttleId; }
    public String getThrottleKey() { return throttleKey; }
    public void setThrottleKey(String throttleKey) { this.throttleKey = throttleKey; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTargetRef() { return targetRef; }
    public void setTargetRef(String targetRef) { this.targetRef = targetRef; }
    public Date getLastTouchTime() { return lastTouchTime; }
    public void setLastTouchTime(Date lastTouchTime) { this.lastTouchTime = lastTouchTime; }
    public int getTouchCount24h() { return touchCount24h; }
    public void setTouchCount24h(int touchCount24h) { this.touchCount24h = touchCount24h; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
