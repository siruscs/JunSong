package com.junsong.finance.domain.vo;

import java.util.ArrayList;
import java.util.List;

public class CashflowForecastQueryParams {
    private Long deptId;
    private List<Long> deptIds = new ArrayList<>();
    private String startTime;
    private String endTime;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds == null ? new ArrayList<>() : deptIds; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
