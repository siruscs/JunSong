package com.junsong.finance.controller;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.service.ICashflowDashboardService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 轻量现金流看板 Controller.
 * R7-D: 小店面可用，POST /cashflow/dashboard
 *
 * @author junsong
 */
@RestController
@RequestMapping("/cashflow")
public class CashflowDashboardController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(CashflowDashboardController.class);

    /** 无授权部门时的哨兵值，IN (-1) 永不匹配真实数据 */
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);

    @Autowired
    private ICashflowDashboardService cashflowDashboardService;

    @Autowired
    private RemoteUserService remoteUserService;

    /**
     * 获取现金流看板数据
     * 请求体: { "deptIds": [1,2], "startTime": "2026-01-01", "endTime": "2026-01-31" }
     */
    @RequiresPermissions("finance:cashflow:dashboard")
    @PostMapping("/dashboard")
    public AjaxResult getDashboard(@RequestBody ReportQueryParams params) {
        List<Long> deptIds = resolveAuthorizedDeptIds(params);
        Date startTime = params.getStartTime();
        Date endTime = params.getEndTime();
        return AjaxResult.success(cashflowDashboardService.getCashflowDashboard(deptIds, startTime, endTime));
    }

    // ── 门店授权校验 ──

    /**
     * 计算授权部门ID列表（intersection 逻辑）
     * - admin 用户：使用请求的 deptIds，为空则返回 null（不过滤）
     * - 非admin 用户：取请求 deptIds 与授权 depts 的交集，无授权返回 -1L 哨兵
     */
    private List<Long> resolveAuthorizedDeptIds(ReportQueryParams params) {
        // admin 用户不限制
        if (SecurityUtils.isAdmin()) {
            List<Long> requested = params.getDeptIds();
            return (requested != null && !requested.isEmpty()) ? requested : null;
        }

        // 非admin：加载授权部门
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            return SENTINEL_DEPT_IDS;
        }

        List<Long> requested = params.getDeptIds();
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>(allowed);
        }

        // 取交集
        List<Long> finalAllowed = allowed;
        List<Long> intersection = requested.stream()
                .filter(finalAllowed::contains)
                .collect(Collectors.toList());

        return intersection.isEmpty() ? SENTINEL_DEPT_IDS : intersection;
    }

    /**
     * 通过 RemoteUserService 获取当前用户授权的部门列表
     */
    private List<Long> loadAllowedDeptIds() {
        String username = SecurityUtils.getUsername();
        if (username == null || username.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> response = remoteUserService.getUserDeptList(username, SecurityConstants.INNER);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .map(SysDept::getDeptId)
                    .filter(deptId -> deptId != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取用户授权门店列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
