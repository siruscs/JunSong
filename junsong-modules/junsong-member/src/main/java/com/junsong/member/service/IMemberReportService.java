package com.junsong.member.service;

import com.junsong.member.domain.vo.MemberReportQueryParams;
import com.junsong.member.domain.vo.MemberReportVO;
import com.junsong.member.domain.vo.SeckillReportVO;

public interface IMemberReportService {
    MemberReportVO getMemberReport(MemberReportQueryParams params);
    SeckillReportVO getSeckillReport(MemberReportQueryParams params);
    com.junsong.member.domain.vo.MemberContributionReportVO getContributionReport(com.junsong.member.domain.vo.MemberReportQueryParams params);
}
