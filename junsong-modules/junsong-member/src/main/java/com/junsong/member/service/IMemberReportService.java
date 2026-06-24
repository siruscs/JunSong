package com.junsong.member.service;

import com.junsong.member.domain.vo.MemberReportQueryParams;
import com.junsong.member.domain.vo.MemberReportVO;
import com.junsong.member.domain.vo.SeckillReportVO;

public interface IMemberReportService {
    MemberReportVO getMemberReport(MemberReportQueryParams params);
    SeckillReportVO getSeckillReport(MemberReportQueryParams params);
}
