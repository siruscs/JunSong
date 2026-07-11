package com.junsong.member.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 增长动作候选会员VO
 *
 * @author junsong
 */
public class GrowthActionCandidateVO
{
    private Long memberId;
    private String memberNo;
    private String memberName;
    private Long deptId;
    private String deptName;
    private String segmentType;
    private Long growthValue;
    private String cardType;
    private String cardTypeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastActiveTime;

    private Integer activeDaysAgo;
    private Boolean recentRepurchased;
    private Integer recentSignInCount;
    private String candidateReason;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberNo() { return memberNo; }
    public void setMemberNo(String memberNo) { this.memberNo = memberNo; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String segmentType) { this.segmentType = segmentType; }

    public Long getGrowthValue() { return growthValue; }
    public void setGrowthValue(Long growthValue) { this.growthValue = growthValue; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getCardTypeName() { return cardTypeName; }
    public void setCardTypeName(String cardTypeName) { this.cardTypeName = cardTypeName; }

    public Date getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(Date lastActiveTime) { this.lastActiveTime = lastActiveTime; }

    public Integer getActiveDaysAgo() { return activeDaysAgo; }
    public void setActiveDaysAgo(Integer activeDaysAgo) { this.activeDaysAgo = activeDaysAgo; }

    public Boolean getRecentRepurchased() { return recentRepurchased; }
    public void setRecentRepurchased(Boolean recentRepurchased) { this.recentRepurchased = recentRepurchased; }

    public Integer getRecentSignInCount() { return recentSignInCount; }
    public void setRecentSignInCount(Integer recentSignInCount) { this.recentSignInCount = recentSignInCount; }

    public String getCandidateReason() { return candidateReason; }
    public void setCandidateReason(String candidateReason) { this.candidateReason = candidateReason; }
}
