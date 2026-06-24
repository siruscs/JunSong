package com.junsong.member.service.impl;

import com.junsong.member.domain.vo.MemberReportQueryParams;
import com.junsong.member.domain.vo.MemberReportVO;
import com.junsong.member.domain.vo.SeckillReportVO;
import com.junsong.member.service.IMemberReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MemberReportServiceImpl implements IMemberReportService {

    @Override
    public MemberReportVO getMemberReport(MemberReportQueryParams params) {
        MemberReportVO vo = new MemberReportVO();
        vo.setTotalMemberCount(0);
        vo.setTodayNewMemberCount(0);
        vo.setActiveMemberCount(0);
        vo.setMemberGrowthStats(new ArrayList<>());
        vo.setMemberTypeStats(new ArrayList<>());
        return vo;
    }

    @Override
    public SeckillReportVO getSeckillReport(MemberReportQueryParams params) {
        SeckillReportVO vo = new SeckillReportVO();
        vo.setTotalSeckillCount(0);
        vo.setTotalParticipantCount(0);
        vo.setTotalRevenue(BigDecimal.ZERO);
        vo.setSeckillStats(new ArrayList<>());
        vo.setSeckillDeptStats(new ArrayList<>());
        return vo;
    }
}
