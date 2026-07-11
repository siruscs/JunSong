package com.junsong.member.controller;

import java.math.BigDecimal;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.api.domain.SaleGrowthAwardReq;
import com.junsong.member.domain.MemGrowthRecord;
import com.junsong.member.domain.MemGrowthRule;
import com.junsong.member.service.IMemberGrowthService;
import com.junsong.member.service.IMemberGrowthRuleService;

/**
 * 会员成长值Controller
 * 网关路径: /member/growth/**
 */
@RestController
@RequestMapping("/growth")
public class MemGrowthController extends BaseController
{
    @Autowired
    private IMemberGrowthService growthService;

    @Autowired
    private IMemberGrowthRuleService growthRuleService;

    /**
     * 查询成长值记录列表
     */
    @RequiresPermissions("member:growth:list")
    @GetMapping("/list")
    public TableDataInfo list(MemGrowthRecord record)
    {
        startPage();
        List<MemGrowthRecord> list = growthService.selectGrowthRecordList(record);
        return getDataTable(list);
    }

    /**
     * 导出成长值记录列表
     */
    @RequiresPermissions("member:growth:export")
    @Log(title = "成长值记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemGrowthRecord record)
    {
        List<MemGrowthRecord> list = growthService.selectGrowthRecordList(record);
        ExcelUtil<MemGrowthRecord> util = new ExcelUtil<>(MemGrowthRecord.class);
        util.exportExcel(response, list, "成长值记录数据");
    }

    /**
     * 查询成长值汇总
     */
    @RequiresPermissions("member:growth:query")
    @GetMapping("/summary")
    public AjaxResult summary(@RequestParam("memberId") Long memberId)
    {
        com.junsong.member.service.IMemberGrowthService.GrowthSummary summary =
                growthService.getGrowthSummary(memberId);
        return AjaxResult.success(summary);
    }

    /**
     * 手工调整积分和成长值
     */
    @RequiresPermissions("member:growth:adjust")
    @Log(title = "成长值调整", businessType = BusinessType.UPDATE)
    @PostMapping("/adjust")
    public AjaxResult adjust(@RequestBody java.util.Map<String, Object> params)
    {
        Long memberId = Long.valueOf(params.get("memberId").toString());
        BigDecimal pointsChange = params.containsKey("pointsChange") && params.get("pointsChange") != null
                ? new BigDecimal(params.get("pointsChange").toString()) : BigDecimal.ZERO;
        Long growthChange = params.containsKey("growthChange") && params.get("growthChange") != null
                ? Long.valueOf(params.get("growthChange").toString()) : 0L;
        String remark = params.containsKey("remark") ? params.get("remark").toString() : null;
        String operator = SecurityUtils.getUsername();

        com.junsong.member.service.IMemberGrowthService.GrowthAwardResult result =
                growthService.manualAdjust(memberId, pointsChange, growthChange, remark, operator);
        AjaxResult ajax = AjaxResult.success("调整成功");
        ajax.put("data", result);
        return ajax;
    }

    /**
     * 查询成长规则
     */
    @RequiresPermissions("member:growth:query")
    @GetMapping("/rule")
    public AjaxResult getRule()
    {
        MemGrowthRule rule = growthRuleService.getGrowthRule();
        return AjaxResult.success(rule);
    }

    /**
     * 修改成长规则
     */
    @RequiresPermissions("member:growth:edit")
    @Log(title = "成长规则", businessType = BusinessType.UPDATE)
    @PutMapping("/rule")
    public AjaxResult updateRule(@RequestBody MemGrowthRule rule)
    {
        rule.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(growthRuleService.updateGrowthRule(rule));
    }

    /**
     * 销售消费奖励入账（内部接口，财务模块通过Feign调用）
     * 幂等键: SALE:{saleId}
     */
    @InnerAuth
    @PostMapping("/inner/awardSale")
    public R<Boolean> awardSaleGrowth(@RequestBody SaleGrowthAwardReq request,
                                       @RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        String operator = request.getOperator() != null ? request.getOperator() : "finance";
        boolean success = growthService.awardSaleGrowth(
                request.getMemberId(),
                request.getMemberNo(),
                request.getMemberName(),
                request.getDeptId(),
                request.getSaleId(),
                request.getSaleAmount(),
                operator);
        return R.ok(success);
    }
}
