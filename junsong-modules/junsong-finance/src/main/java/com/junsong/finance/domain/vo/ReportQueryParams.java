package com.junsong.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

public class ReportQueryParams {
    private List<Long> deptIds;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endTime;
    private String timeType;

    public List<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(List<Long> deptIds) {
        this.deptIds = deptIds;
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

    private Long periodId;
    private Long drilldownDeptId;
    private String compareType;

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public Long getDrilldownDeptId() {
        return drilldownDeptId;
    }

    public void setDrilldownDeptId(Long drilldownDeptId) {
        this.drilldownDeptId = drilldownDeptId;
    }

    public String getCompareType() {
        return compareType;
    }

    public void setCompareType(String compareType) {
        this.compareType = compareType;
    }
}
