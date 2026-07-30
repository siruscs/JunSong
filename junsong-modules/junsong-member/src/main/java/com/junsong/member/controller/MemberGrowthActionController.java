package com.junsong.member.controller;

import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.member.domain.MemGrowthActionMember;
import com.junsong.member.domain.vo.GrowthActionCandidateVO;
import com.junsong.member.domain.vo.GrowthActionDashboardVO;
import com.junsong.member.domain.vo.GrowthActionEffectVO;
import com.junsong.member.domain.vo.GrowthActionExecuteParams;
import com.junsong.member.domain.vo.GrowthActionGenerateParams;
import com.junsong.member.domain.vo.GrowthActionQueryParams;
import com.junsong.member.service.IMemberGrowthActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员增长动作Controller
 *
 * 端点路径使用 /growth-action/* 全路径形式，与网关 StripPrefix=1 配置对齐：
 * 前端请求 /member/growth-action/dashboard -> 网关剥离 /member -> 后端 /growth-action/dashboard
 *
 * @author junsong
 */
@RestController
public class MemberGrowthActionController extends BaseController
{
    @Autowired
    private IMemberGrowthActionService memberGrowthActionService;

    /**
     * 增长动作看板
     */
    @RequiresPermissions("member:growthAction:view")
    @PostMapping("/growth-action/dashboard")
    public AjaxResult dashboard(@RequestBody(required = false) GrowthActionQueryParams params)
    {
        GrowthActionDashboardVO dashboard = memberGrowthActionService.getDashboard(params);
        return AjaxResult.success(dashboard);
    }

    /**
     * 候选会员列表
     */
    @RequiresPermissions("member:growthAction:view")
    @PostMapping("/growth-action/candidates")
    public AjaxResult candidates(@RequestBody(required = false) GrowthActionQueryParams params)
    {
        List<GrowthActionCandidateVO> candidates = memberGrowthActionService.listCandidates(params);
        return AjaxResult.success(candidates);
    }

    /**
     * 按动作ID查询会员明细（执行弹窗使用，只返回该动作下的真实明细）
     */
    @RequiresPermissions("member:growthAction:view")
    @GetMapping("/growth-action/members")
    public AjaxResult members(@RequestParam("actionId") Long actionId)
    {
        List<MemGrowthActionMember> members = memberGrowthActionService.listActionMembers(actionId);
        return AjaxResult.success(members);
    }

    /**
     * 生成增长动作
     */
    @RequiresPermissions("member:growthAction:generate")
    @Idempotent(scene = "member:growth-action:generate")
    @PostMapping("/growth-action/generate")
    public AjaxResult generate(@RequestBody GrowthActionGenerateParams params)
    {
        try {
            int count = memberGrowthActionService.generateAction(params);
            if (count == 0) {
                return AjaxResult.success("当前条件下无候选会员，未生成增长动作");
            }
            return AjaxResult.success("成功生成增长动作，候选会员" + count + "人");
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 执行增长动作
     */
    @RequiresPermissions("member:growthAction:execute")
    @Idempotent(scene = "member:growthAction:execute", highRisk = true, ttlSeconds = 2592000)
    @PostMapping("/growth-action/execute")
    public AjaxResult execute(@RequestBody GrowthActionExecuteParams params)
    {
        try {
            memberGrowthActionService.executeAction(params);
            return AjaxResult.success("执行成功");
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 增长效果复盘
     */
    @RequiresPermissions("member:growthAction:effect")
    @PostMapping("/growth-action/effect")
    public AjaxResult effect(@RequestBody(required = false) GrowthActionQueryParams params)
    {
        GrowthActionEffectVO effect = memberGrowthActionService.getEffect(params);
        return AjaxResult.success(effect);
    }
}
