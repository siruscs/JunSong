package com.junsong.member.api;

import java.io.Serializable;

/**
 * 会员动作预测查询参数（内部调用）。
 */
public class MemberActionPredictionQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deptId;
    private Integer windowDays = 30;
    private String actionType;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays == null ? 30 : windowDays; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
}
