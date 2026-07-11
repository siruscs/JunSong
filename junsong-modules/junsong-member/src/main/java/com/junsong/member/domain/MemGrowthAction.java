package com.junsong.member.domain;

import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 会员增长动作对象 mem_growth_action
 *
 * @author junsong
 */
public class MemGrowthAction extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 增长动作ID */
    private Long actionId;

    /** 动作编号 */
    private String actionNo;

    /** 门店ID */
    private Long deptId;

    /** 门店名称 */
    private String deptName;

    /** 动作类型 */
    private String actionType;

    /** 动作标题 */
    private String actionTitle;

    /** 来源类型 */
    private String sourceType;

    /** 现金压力等级 */
    private String pressureLevel;

    /** 候选会员数 */
    private Integer candidateCount;

    /** 已执行会员数 */
    private Integer executedCount;

    /** 状态 */
    private String status;

    /** 动作原因 */
    private String actionReason;

    /** 建议话术 */
    private String suggestedScript;

    /** 效果观察窗口 */
    private Integer effectWindowDays;

    /** 租户ID */
    private Long tenantId;

    /** 删除标志 */
    private String delFlag;

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

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

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

    public Integer getEffectWindowDays() { return effectWindowDays; }
    public void setEffectWindowDays(Integer effectWindowDays) { this.effectWindowDays = effectWindowDays; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
