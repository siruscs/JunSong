package com.junsong.member.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 增长动作列表行VO
 *
 * @author junsong
 */
public class GrowthActionRowVO
{
    private Long actionId;
    private String actionNo;
    private Long deptId;
    private String deptName;
    private String actionType;
    private String actionTitle;
    private String pressureLevel;
    private Integer candidateCount;
    private Integer executedCount;
    private String status;
    private String actionReason;
    private String suggestedScript;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }

    public String getActionNo() { return actionNo; }
    public void setActionNo(String actionNo) { this.actionNo = actionNo; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionTitle() { return actionTitle; }
    public void setActionTitle(String actionTitle) { this.actionTitle = actionTitle; }

    public String getPressureLevel() { return pressureLevel; }
    public void setPressureLevel(String pressureLevel) { this.pressureLevel = pressureLevel; }

    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }

    public Integer getExecutedCount() { return executedCount; }
    public void setExecutedCount(Integer executedCount) { this.executedCount = executedCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActionReason() { return actionReason; }
    public void setActionReason(String actionReason) { this.actionReason = actionReason; }

    public String getSuggestedScript() { return suggestedScript; }
    public void setSuggestedScript(String suggestedScript) { this.suggestedScript = suggestedScript; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
