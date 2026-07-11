package com.junsong.member.domain.vo;

/**
 * 增长动作生成参数
 *
 * @author junsong
 */
public class GrowthActionGenerateParams
{
    private Long deptId;
    private String actionType;
    private String segmentType;
    private String pressureLevel;
    private Integer limit = 30;
    private String actionReason;
    private String suggestedScript;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String segmentType) { this.segmentType = segmentType; }

    public String getPressureLevel() { return pressureLevel; }
    public void setPressureLevel(String pressureLevel) { this.pressureLevel = pressureLevel; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public String getActionReason() { return actionReason; }
    public void setActionReason(String actionReason) { this.actionReason = actionReason; }

    public String getSuggestedScript() { return suggestedScript; }
    public void setSuggestedScript(String suggestedScript) { this.suggestedScript = suggestedScript; }
}
