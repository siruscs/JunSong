package com.junsong.finance.domain.vo;

import java.util.Date;
import java.util.List;

public class StoreHealthTrendQueryParams {
    private List<Long> deptIds;
    private Date startTime;
    private Date endTime;
    private String timeType; // week/month, default week

    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public String getTimeType() { return timeType; }
    public void setTimeType(String timeType) { this.timeType = timeType; }
}
