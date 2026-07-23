package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.OperatingMetric;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.service.IFinanceOperatingMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统一经营指标控制器（Phase 5）。
 *
 * PC 和小程序通过同一端点获取指标，保证口径一致。
 * 旧 dashboard 接口保留兼容，客户端逐步迁移。
 *
 * 权限：finance:dashboard:operation（现有）
 * 端点：POST /operatingMetrics
 */
@RestController
@RequestMapping("/operatingMetrics")
public class FinanceOperatingMetricController extends BaseController {

    @Autowired
    private IFinanceOperatingMetricService operatingMetricService;

    @RequiresPermissions("finance:dashboard:operation")
    @PostMapping
    public AjaxResult getOperatingMetrics(@RequestBody(required = false) ReportQueryParams params) {
        List<OperatingMetric> metrics = operatingMetricService.getOperatingMetrics(params);
        return AjaxResult.success(metrics);
    }
}
