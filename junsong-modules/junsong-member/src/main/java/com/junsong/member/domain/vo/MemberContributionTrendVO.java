package com.junsong.member.domain.vo;

import java.math.BigDecimal;

public class MemberContributionTrendVO {
    private String dateStr;
    private int newMemberCount;
    private int activeMemberCount;
    private BigDecimal memberSales = BigDecimal.ZERO;
    private BigDecimal nonMemberSales = BigDecimal.ZERO;

    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }
    public int getNewMemberCount() { return newMemberCount; }
    public void setNewMemberCount(int newMemberCount) { this.newMemberCount = newMemberCount; }
    public int getActiveMemberCount() { return activeMemberCount; }
    public void setActiveMemberCount(int activeMemberCount) { this.activeMemberCount = activeMemberCount; }
    public BigDecimal getMemberSales() { return memberSales; }
    public void setMemberSales(BigDecimal memberSales) { this.memberSales = memberSales; }
    public BigDecimal getNonMemberSales() { return nonMemberSales; }
    public void setNonMemberSales(BigDecimal nonMemberSales) { this.nonMemberSales = nonMemberSales; }
}
