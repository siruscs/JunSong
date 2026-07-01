package com.junsong.member.domain.vo;

import java.math.BigDecimal;

/**
 * 会员分层汇总行 VO，用于概览分层区展示各分层会员数量与占比。
 */
public class MemberSegmentRowVO {
    private String segmentType;
    private String segmentName;
    private Long memberCount;
    private BigDecimal ratio;

    public String getSegmentType() {
        return segmentType;
    }

    public void setSegmentType(String segmentType) {
        this.segmentType = segmentType;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public Long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Long memberCount) {
        this.memberCount = memberCount;
    }

    public BigDecimal getRatio() {
        return ratio;
    }

    public void setRatio(BigDecimal ratio) {
        this.ratio = ratio;
    }
}
