package com.junsong.member.domain.vo;

import java.util.List;
import java.util.Map;

public class MemberReportVO {
    private Integer totalMemberCount;
    private Integer todayNewMemberCount;
    private Integer activeMemberCount;
    private List<Map<String, Object>> memberGrowthStats;
    private List<Map<String, Object>> memberTypeStats;

    public Integer getTotalMemberCount() {
        return totalMemberCount;
    }

    public void setTotalMemberCount(Integer totalMemberCount) {
        this.totalMemberCount = totalMemberCount;
    }

    public Integer getTodayNewMemberCount() {
        return todayNewMemberCount;
    }

    public void setTodayNewMemberCount(Integer todayNewMemberCount) {
        this.todayNewMemberCount = todayNewMemberCount;
    }

    public Integer getActiveMemberCount() {
        return activeMemberCount;
    }

    public void setActiveMemberCount(Integer activeMemberCount) {
        this.activeMemberCount = activeMemberCount;
    }

    public List<Map<String, Object>> getMemberGrowthStats() {
        return memberGrowthStats;
    }

    public void setMemberGrowthStats(List<Map<String, Object>> memberGrowthStats) {
        this.memberGrowthStats = memberGrowthStats;
    }

    public List<Map<String, Object>> getMemberTypeStats() {
        return memberTypeStats;
    }

    public void setMemberTypeStats(List<Map<String, Object>> memberTypeStats) {
        this.memberTypeStats = memberTypeStats;
    }
}
