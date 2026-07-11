package com.junsong.finance.domain.vo;

import java.util.Date;
import java.util.List;

public class StoreHealthTaskGenerateParams {
    private List<Long> deptIds;
    private Date startTime;
    private Date endTime;
    private List<String> factorCodes;

    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public List<String> getFactorCodes() { return factorCodes; }
    public void setFactorCodes(List<String> factorCodes) { this.factorCodes = factorCodes; }
}
