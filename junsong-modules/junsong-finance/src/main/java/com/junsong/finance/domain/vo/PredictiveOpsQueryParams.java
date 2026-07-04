package com.junsong.finance.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 预测辅助 V2 查询参数 VO。
 * 与 R16 CashflowForecastQueryParams 一样保留门店/部门边界。
 */
public class PredictiveOpsQueryParams {

    private Long deptId;
    private List<Long> deptIds = new ArrayList<>();
    private Integer windowDays = 7;
    private String actionType;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds == null ? new ArrayList<>() : deptIds; }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays == null ? 7 : windowDays; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
}
