package com.junsong.member.domain.vo;

import java.util.List;
import java.util.Map;

public class SeckillReportVO {
    private Integer totalSeckillCount;
    private Integer totalParticipantCount;
    private java.math.BigDecimal totalRevenue;
    private List<Map<String, Object>> seckillStats;
    private List<Map<String, Object>> seckillDeptStats;

    public Integer getTotalSeckillCount() {
        return totalSeckillCount;
    }

    public void setTotalSeckillCount(Integer totalSeckillCount) {
        this.totalSeckillCount = totalSeckillCount;
    }

    public Integer getTotalParticipantCount() {
        return totalParticipantCount;
    }

    public void setTotalParticipantCount(Integer totalParticipantCount) {
        this.totalParticipantCount = totalParticipantCount;
    }

    public java.math.BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(java.math.BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public List<Map<String, Object>> getSeckillStats() {
        return seckillStats;
    }

    public void setSeckillStats(List<Map<String, Object>> seckillStats) {
        this.seckillStats = seckillStats;
    }

    public List<Map<String, Object>> getSeckillDeptStats() {
        return seckillDeptStats;
    }

    public void setSeckillDeptStats(List<Map<String, Object>> seckillDeptStats) {
        this.seckillDeptStats = seckillDeptStats;
    }
}
