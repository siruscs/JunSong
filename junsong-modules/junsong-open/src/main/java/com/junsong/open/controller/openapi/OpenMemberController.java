package com.junsong.open.controller.openapi;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;


/**
 * 会员能力即服务(Member as a Service)开放API
 *
 * 对外暴露会员查询、积分查询、积分规则、秒杀活动等能力
 *
 * @author junsong
 */
@RestController
@RequestMapping("/members")
public class OpenMemberController extends BaseController
{
    @Autowired
    private RestTemplate restTemplate;

    private static final String MEMBER_BASE = "http://junsong-modules-member:9206";

    /**
     * 查询会员列表
     */
    @GetMapping
    public String listMembers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String memberNo,
            @RequestParam(required = false) String memberName)
    {
        String url = MEMBER_BASE + "/member/list?pageNum=" + pageNum + "&pageSize=" + pageSize;
        if (memberNo != null) url += "&memberNo=" + memberNo;
        if (memberName != null) url += "&memberName=" + memberName;
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 获取会员详情
     */
    @GetMapping("/{id}")
    public AjaxResult getMember(@PathVariable("id") Long id)
    {
        return restTemplate.getForObject(MEMBER_BASE + "/member/" + id, AjaxResult.class);
    }

    /**
     * 按会员编号查询
     */
    @GetMapping("/no/{memberNo}")
    public AjaxResult getByNo(@PathVariable("memberNo") String memberNo)
    {
        return restTemplate.getForObject(MEMBER_BASE + "/member/no/" + memberNo, AjaxResult.class);
    }

    /**
     * 查询会员积分记录
     */
    @GetMapping("/{memberId}/points")
    public String getMemberPoints(
            @PathVariable("memberId") Long memberId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        String url = MEMBER_BASE + "/pointsRecord/list?pageNum=" + pageNum + "&pageSize=" + pageSize + "&memberId=" + memberId;
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 获取当前生效的积分规则
     */
    @GetMapping("/points-rules/effective")
    public AjaxResult getEffectivePointsRule()
    {
        return restTemplate.getForObject(MEMBER_BASE + "/pointsRule/effective", AjaxResult.class);
    }

    /**
     * 写入积分变动记录
     *
     * @param memberId 会员ID
     * @param params   积分变动参数（points积分值、reason原因、idempotencyKey幂等键）
     */
    @PostMapping("/{memberId}/points/records")
    public AjaxResult createPointsRecord(@PathVariable("memberId") Long memberId,
            @RequestBody Map<String, Object> params)
    {
        params.put("memberId", memberId);
        return restTemplate.postForObject(MEMBER_BASE + "/pointsRecord", params, AjaxResult.class);
    }

    /**
     * 积分兑换(写入操作)
     */
    @PostMapping("/points-exchanges")
    public AjaxResult createPointsExchange(@RequestBody Map<String, Object> params)
    {
        return restTemplate.postForObject(MEMBER_BASE + "/pointsExchange", params, AjaxResult.class);
    }

    /**
     * 查询进行中的秒杀活动
     */
    @GetMapping("/seckills/active")
    public AjaxResult getActiveSeckills()
    {
        return restTemplate.getForObject(MEMBER_BASE + "/seckill/listActive", AjaxResult.class);
    }

    /**
     * 会员仪表盘统计
     */
    @GetMapping("/dashboard/stats")
    public AjaxResult getDashboardStats()
    {
        return restTemplate.getForObject(MEMBER_BASE + "/dashboard/stats", AjaxResult.class);
    }

    /**
     * 会员仪表盘趋势(近7天)
     */
    @GetMapping("/dashboard/trend")
    public AjaxResult getDashboardTrend()
    {
        return restTemplate.getForObject(MEMBER_BASE + "/dashboard/trend", AjaxResult.class);
    }

    /**
     * 积分余额排行榜(TOP10)
     */
    @GetMapping("/dashboard/ranking")
    public AjaxResult getDashboardRanking()
    {
        return restTemplate.getForObject(MEMBER_BASE + "/dashboard/ranking", AjaxResult.class);
    }
}
