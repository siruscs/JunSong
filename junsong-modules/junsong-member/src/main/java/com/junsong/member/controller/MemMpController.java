package com.junsong.member.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.service.IMemMpRoleModuleService;
import com.junsong.member.mapper.MemMpDashboardMapper;
import com.junsong.system.api.domain.SysRole;
import com.junsong.system.api.model.LoginUser;

@RestController
@RequestMapping({"/mp", "/member/mp"})
public class MemMpController extends BaseController {

    private static final List<String> ALL_MODULES = Arrays.asList(
            "member", "pointsGoods", "pointsRecord", "pointsExchange",
            "seckill", "seckillRecord",
            "expense", "advance", "product", "supplier", "purchase", "sale",
            "investorPayment", "investor", "investRecord", "deptProfitConfig",
            "accountingPeriod", "profitShare", "costAccounting",
            "userManage", "deptManage"
    );

    @Autowired
    private IMemMpRoleModuleService mpRoleModuleService;

    @Autowired
    private MemMpDashboardMapper dashboardMapper;

    @GetMapping("/userinfo")
    public AjaxResult getUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return AjaxResult.error("未登录");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", SecurityUtils.getUserId());
        result.put("username", SecurityUtils.getUsername());
        result.put("deptId", SecurityUtils.getDeptId());
        result.put("roles", loginUser.getRoles());

        String nickName = loginUser.getSysUser() != null ? loginUser.getSysUser().getNickName() : SecurityUtils.getUsername();
        result.put("nickName", nickName);

        List<String> modules = getAccessibleModules(loginUser);
        result.put("modules", modules);

        return AjaxResult.success(result);
    }

    @GetMapping("/modules")
    public AjaxResult getModules() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return AjaxResult.error("未登录");
        }
        return AjaxResult.success(getAccessibleModules(loginUser));
    }

    private List<String> getAccessibleModules(LoginUser loginUser) {
        if (SecurityUtils.isAdmin()) {
            return ALL_MODULES;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            return Collections.emptyList();
        }
        List<Long> roleIds = getRoleIds(loginUser);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> configured = mpRoleModuleService.getAccessibleModules(roleIds, deptId);
        return configured;
    }

    private List<Long> getRoleIds(LoginUser loginUser) {
        Set<Long> roleIds = new LinkedHashSet<>();
        if (loginUser.getSysUser() != null && loginUser.getSysUser().getRoles() != null) {
            for (SysRole role : loginUser.getSysUser().getRoles()) {
                if (role.getRoleId() != null) {
                    roleIds.add(role.getRoleId());
                }
            }
        }
        if (roleIds.isEmpty() && loginUser.getRoles() != null && !loginUser.getRoles().isEmpty()) {
            roleIds.addAll(mpRoleModuleService.selectRoleIdsByRoleKeys(loginUser.getRoles()));
        }
        return new ArrayList<>(roleIds);
    }

    @GetMapping("/dashboard/stats")
    public AjaxResult getDashboardStats() {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            return AjaxResult.error("未选择部门");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMembers", dashboardMapper.queryCount(deptId, "totalMembers"));
        stats.put("todayMembers", dashboardMapper.queryCount(deptId, "todayMembers"));
        stats.put("activeMembers", dashboardMapper.queryCount(deptId, "activeMembers"));
        stats.put("totalExpense", dashboardMapper.queryDecimal(deptId, "totalExpense"));
        stats.put("totalSale", dashboardMapper.queryDecimal(deptId, "totalSale"));
        stats.put("totalPurchase", dashboardMapper.queryDecimal(deptId, "totalPurchase"));
        stats.put("totalAdvance", dashboardMapper.queryDecimal(deptId, "totalAdvance"));
        stats.put("todayExpense", dashboardMapper.queryDecimal(deptId, "todayExpense"));
        stats.put("todaySale", dashboardMapper.queryDecimal(deptId, "todaySale"));
        stats.put("unverifiedExpense", dashboardMapper.queryDecimal(deptId, "unverifiedExpense"));
        stats.put("unverifiedAdvance", dashboardMapper.queryDecimal(deptId, "unverifiedAdvance"));
        return AjaxResult.success(stats);
    }

    @GetMapping("/dashboard/trend")
    public AjaxResult getDashboardTrend() {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            return AjaxResult.error("未选择部门");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        LocalDate endDate = today.plusDays(1);

        List<String> dates = new ArrayList<>();
        List<Object> newMembers = new ArrayList<>();
        List<Object> dailyExpense = new ArrayList<>();
        List<Object> dailySale = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        // 一次查询返回 7 天趋势数据（替代原来的 21 次逐日查询）
        List<Map<String, Object>> batchResult = dashboardMapper.queryTrendBatch(
                deptId, startDate.toString(), endDate.toString());

        // 按 stat_date 建立索引，方便逐日填充
        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        if (batchResult != null) {
            for (Map<String, Object> row : batchResult) {
                String key = String.valueOf(row.get("stat_date"));
                byDate.put(key, row);
            }
        }

        // 按固定日期顺序组装结果（无数据的天补零）
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            dates.add(day.format(fmt));

            Map<String, Object> row = byDate.get(day.toString());
            if (row != null) {
                newMembers.add(row.getOrDefault("new_members", BigDecimal.ZERO));
                dailyExpense.add(row.getOrDefault("daily_expense", BigDecimal.ZERO));
                dailySale.add(row.getOrDefault("daily_sale", BigDecimal.ZERO));
            } else {
                newMembers.add(BigDecimal.ZERO);
                dailyExpense.add(BigDecimal.ZERO);
                dailySale.add(BigDecimal.ZERO);
            }
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", dates);
        trend.put("newMembers", newMembers);
        trend.put("dailyExpense", dailyExpense);
        trend.put("dailySale", dailySale);
        return AjaxResult.success(trend);
    }
}
