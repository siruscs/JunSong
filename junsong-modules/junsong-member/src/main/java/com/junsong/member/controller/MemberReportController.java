package com.junsong.member.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.member.domain.vo.MemberReportQueryParams;
import com.junsong.member.domain.vo.MemberReportVO;
import com.junsong.member.domain.vo.SeckillReportVO;
import com.junsong.member.service.IMemberReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class MemberReportController extends BaseController {

    @Autowired
    private IMemberReportService memberReportService;

    @PostMapping("/member")
    public AjaxResult getMemberReport(@RequestBody MemberReportQueryParams params) {
        MemberReportVO report = memberReportService.getMemberReport(params);
        return AjaxResult.success(report);
    }

    @PostMapping("/seckill")
    public AjaxResult getSeckillReport(@RequestBody MemberReportQueryParams params) {
        SeckillReportVO report = memberReportService.getSeckillReport(params);
        return AjaxResult.success(report);
    }
}
