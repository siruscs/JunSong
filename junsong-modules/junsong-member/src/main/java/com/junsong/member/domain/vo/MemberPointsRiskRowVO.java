package com.junsong.member.domain.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 高积分会员风险行：用于积分经营摘要中的高积分会员清单。
 * 手机号强制脱敏。
 */
public class MemberPointsRiskRowVO {
    private Long memberId;
    private String memberNo;
    private String memberName;
    private String maskedPhone;
    private Long deptId;
    private String deptName;
    private Long availablePoints;
    private BigDecimal estimatedLiability;
    private Date lastOrderTime;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Long getAvailablePoints() {
        return availablePoints;
    }

    public void setAvailablePoints(Long availablePoints) {
        this.availablePoints = availablePoints;
    }

    public BigDecimal getEstimatedLiability() {
        return estimatedLiability;
    }

    public void setEstimatedLiability(BigDecimal estimatedLiability) {
        this.estimatedLiability = estimatedLiability;
    }

    public Date getLastOrderTime() {
        return lastOrderTime;
    }

    public void setLastOrderTime(Date lastOrderTime) {
        this.lastOrderTime = lastOrderTime;
    }
}
