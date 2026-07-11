package com.junsong.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;
import java.util.Date;

/**
 * 会员增长动作会员明细对象 mem_growth_action_member
 *
 * @author junsong
 */
public class MemGrowthActionMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 增长动作ID */
    private Long actionId;

    /** 会员ID */
    private Long memberId;

    /** 会员编号 */
    private String memberNo;

    /** 会员姓名 */
    private String memberName;

    /** 门店ID */
    private Long deptId;

    /** 候选分层 */
    private String segmentType;

    /** 生成时成长值 */
    private Long growthValue;

    /** 生成时等级 */
    private String cardType;

    /** 生成时最后活跃时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastActiveTime;

    /** 入选原因 */
    private String candidateReason;

    /** 执行状态 */
    private String executeStatus;

    /** 执行备注 */
    private String executeNote;

    /** 执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date executeTime;

    /** 观察期是否复购 */
    private String repurchased;

    /** 观察期是否签到 */
    private String signedIn;

    /** 观察期成长值是否增长 */
    private String growthIncreased;

    /** 租户ID */
    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberNo() { return memberNo; }
    public void setMemberNo(String memberNo) { this.memberNo = memberNo; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String segmentType) { this.segmentType = segmentType; }

    public Long getGrowthValue() { return growthValue; }
    public void setGrowthValue(Long growthValue) { this.growthValue = growthValue; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public Date getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(Date lastActiveTime) { this.lastActiveTime = lastActiveTime; }

    public String getCandidateReason() { return candidateReason; }
    public void setCandidateReason(String candidateReason) { this.candidateReason = candidateReason; }

    public String getExecuteStatus() { return executeStatus; }
    public void setExecuteStatus(String executeStatus) { this.executeStatus = executeStatus; }

    public String getExecuteNote() { return executeNote; }
    public void setExecuteNote(String executeNote) { this.executeNote = executeNote; }

    public Date getExecuteTime() { return executeTime; }
    public void setExecuteTime(Date executeTime) { this.executeTime = executeTime; }

    public String getRepurchased() { return repurchased; }
    public void setRepurchased(String repurchased) { this.repurchased = repurchased; }

    public String getSignedIn() { return signedIn; }
    public void setSignedIn(String signedIn) { this.signedIn = signedIn; }

    public String getGrowthIncreased() { return growthIncreased; }
    public void setGrowthIncreased(String growthIncreased) { this.growthIncreased = growthIncreased; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
