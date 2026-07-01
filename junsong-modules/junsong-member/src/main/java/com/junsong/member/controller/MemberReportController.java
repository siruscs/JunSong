package com.junsong.member.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.member.domain.vo.MemberReportQueryParams;
import com.junsong.member.domain.vo.MemberReportVO;
import com.junsong.member.domain.vo.SeckillReportVO;
import com.junsong.member.service.IMemberActivityRoiService;
import com.junsong.member.service.IMemberReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
public class MemberReportController extends BaseController {

    @Autowired
    private IMemberReportService memberReportService;

    @Autowired
    private IMemberActivityRoiService memberActivityRoiService;

    @RequiresPermissions("member:report:member")
    @PostMapping("/member")
    public AjaxResult getMemberReport(@RequestBody MemberReportQueryParams params) {
        MemberReportVO report = memberReportService.getMemberReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("member:report:seckill")
    @PostMapping("/seckill")
    public AjaxResult getSeckillReport(@RequestBody MemberReportQueryParams params) {
        SeckillReportVO report = memberReportService.getSeckillReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("member:report:contribution")
    @PostMapping("/contribution")
    public AjaxResult getContributionReport(@RequestBody com.junsong.member.domain.vo.MemberReportQueryParams params) {
        com.junsong.member.domain.vo.MemberContributionReportVO report = memberReportService.getContributionReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("member:report:member")
    @PostMapping("/activity-roi")
    public AjaxResult getActivityRoi(@RequestParam(required = false) Long deptId,
                                     @RequestParam(required = false) Long activityId) {
        return AjaxResult.success(memberActivityRoiService.getActivityRoiList(deptId, activityId));
    }
}
