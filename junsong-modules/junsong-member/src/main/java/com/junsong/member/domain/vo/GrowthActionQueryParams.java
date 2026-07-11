package com.junsong.member.domain.vo;

/**
 * 增长动作查询参数
 *
 * @author junsong
 */
public class GrowthActionQueryParams
{
    private Long deptId;
    private Long actionId;
    private String actionType;
    private String segmentType;
    private String status;
    private String pressureLevel;
    private Integer windowDays = 30;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String segmentType) { this.segmentType = segmentType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPressureLevel() { return pressureLevel; }
    public void setPressureLevel(String pressureLevel) { this.pressureLevel = pressureLevel; }

    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays; }
}
