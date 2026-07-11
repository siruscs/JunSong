package com.junsong.member.domain.vo;

/**
 * 增长动作执行参数
 *
 * @author junsong
 */
public class GrowthActionExecuteParams
{
    private Long actionId;
    private Long memberId;
    private String executeStatus;
    private String executeNote;

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getExecuteStatus() { return executeStatus; }
    public void setExecuteStatus(String executeStatus) { this.executeStatus = executeStatus; }

    public String getExecuteNote() { return executeNote; }
    public void setExecuteNote(String executeNote) { this.executeNote = executeNote; }
}
