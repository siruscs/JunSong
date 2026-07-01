package com.junsong.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class StoreReportQueryParams {
    private Long deptId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endTime;
    private String timeType;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getTimeType() {
        return timeType;
    }

    public void setTimeType(String timeType) {
        this.timeType = timeType;
    }
}
