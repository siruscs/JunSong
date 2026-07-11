package com.junsong.member.controller;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemMemberSignIn;
import com.junsong.member.service.IMemberSignInService;

/**
 * 会员签到Controller
 * 网关路径: /member/signIn/**
 */
@RestController
@RequestMapping("/signIn")
public class MemSignInController extends BaseController
{
    @Autowired
    private IMemberSignInService signInService;

    /**
     * 查询签到记录列表
     */
    @RequiresPermissions("member:signIn:list")
    @GetMapping("/list")
    public TableDataInfo list(MemMemberSignIn signIn)
    {
        startPage();
        List<MemMemberSignIn> list = signInService.selectSignInList(signIn);
        return getDataTable(list);
    }

    /**
     * 导出签到记录列表
     */
    @RequiresPermissions("member:signIn:export")
    @Log(title = "签到记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MemMemberSignIn signIn)
    {
        List<MemMemberSignIn> list = signInService.selectSignInList(signIn);
        ExcelUtil<MemMemberSignIn> util = new ExcelUtil<>(MemMemberSignIn.class);
        util.exportExcel(response, list, "签到记录数据");
    }

    /**
     * 删除签到/补签到记录
     */
    @RequiresPermissions("member:signIn:remove")
    @Log(title = "签到记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{signIds}")
    public AjaxResult remove(@PathVariable Long[] signIds)
    {
        return toAjax(signInService.deleteSignInByIds(signIds, SecurityUtils.getUsername()));
    }

    /**
     * 会员签到
     * 支持传 signDate 参数（指定日期补签），为空时默认当天
     * 实时签到不允许传未来日期
     */
    @RequiresPermissions("member:signIn:add")
    @Log(title = "会员签到", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult signIn(@RequestBody MemMemberSignIn params)
    {
        if (params.getMemberId() == null)
        {
            return error("会员ID不能为空");
        }
        String operator = SecurityUtils.getUsername();
        Map<String, Object> data = signInService.signIn(params.getMemberId(), params.getSignDate(), operator);
        AjaxResult ajax = AjaxResult.success("签到成功");
        ajax.put("data", data);
        return ajax;
    }

    /**
     * 批量补录签到
     * 权限: member:signIn:backfill
     * 支持两种模式：SELECT_DATES（选择具体日期）和 COUNT_ONLY（输入次数自动分配）
     */
    @RequiresPermissions("member:signIn:backfill")
    @Log(title = "批量补录签到", businessType = BusinessType.INSERT)
    @PostMapping("/backfill")
    public AjaxResult backfill(@RequestBody Map<String, Object> params)
    {
        Long memberId = params.get("memberId") == null ? null : Long.valueOf(params.get("memberId").toString());
        if (memberId == null)
        {
            return error("会员ID不能为空");
        }
        String targetMonth = params.get("targetMonth") == null ? null : params.get("targetMonth").toString();
        String fillMode = params.get("fillMode") == null ? null : params.get("fillMode").toString();
        String remark = params.get("remark") == null ? null : params.get("remark").toString();
        String operator = SecurityUtils.getUsername();

        @SuppressWarnings("unchecked")
        List<String> signDates = params.get("signDates") == null ? null : (List<String>) params.get("signDates");
        Integer signCount = params.get("signCount") == null ? null
                : Integer.valueOf(params.get("signCount").toString());

        Map<String, Object> data = signInService.backfillSignIn(memberId, targetMonth, fillMode,
                signDates, signCount, remark, operator);
        AjaxResult ajax = AjaxResult.success("补录成功");
        ajax.put("data", data);
        return ajax;
    }

    /**
     * 查询今日签到状态
     */
    @RequiresPermissions("member:signIn:query")
    @GetMapping("/today")
    public AjaxResult today(@RequestParam("memberId") Long memberId)
    {
        MemMemberSignIn signIn = signInService.getTodaySignIn(memberId);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", signIn);
        ajax.put("signedIn", signIn != null);
        return ajax;
    }

    /**
     * 查询签到日历
     */
    @RequiresPermissions("member:signIn:query")
    @GetMapping("/calendar")
    public AjaxResult calendar(@RequestParam("memberId") Long memberId, @RequestParam("month") String month)
    {
        List<MemMemberSignIn> list = signInService.selectMonthlyCalendar(memberId, month);
        return AjaxResult.success(list);
    }

    /**
     * 签到预览：查询会员当前等级名称、单次签到积分、单次签到成长值
     * 用于签到/补录弹窗提交前展示预估信息
     */
    @RequiresPermissions("member:signIn:query")
    @GetMapping("/preview")
    public AjaxResult preview(@RequestParam("memberId") Long memberId)
    {
        Map<String, Object> data = signInService.previewSignIn(memberId);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", data);
        return ajax;
    }
}
