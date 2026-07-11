package com.junsong.finance.domain.vo;

import java.util.List;

/**
 * 复盘质量看板查询参数
 */
public class ReviewQualityQueryParams {

    private Long deptId;
    private List<Long> deptIds;
    private String startDate;
    private String endDate;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
