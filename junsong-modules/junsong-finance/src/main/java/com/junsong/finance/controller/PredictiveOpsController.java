package com.junsong.finance.controller;

import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.PredictiveOpsDashboardVO;
import com.junsong.finance.domain.vo.PredictiveOpsQueryParams;
import com.junsong.finance.domain.vo.WhatIfSimulationParams;
import com.junsong.finance.domain.vo.WhatIfSimulationResultVO;
import com.junsong.finance.service.IPredictiveOpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * R24 预测辅助 V2 Controller。
 *
 * <p>端点路径使用 /predictive-ops/* 全路径形式，与网关 StripPrefix=1 配置对齐。
 * 仅暴露只读计算与只读模拟，不写业务表。</p>
 */
@RestController
public class PredictiveOpsController extends BaseController {

    @Autowired
    private IPredictiveOpsService predictiveOpsService;

    /**
     * 预测仪表盘：4 个对象（现金流/应收/会员/库存）。
     */
    @RequiresPermissions("finance:predictiveOps:view")
    @PostMapping("/predictive-ops/dashboard")
    public AjaxResult dashboard(@RequestBody(required = false) PredictiveOpsQueryParams params) {
        PredictiveOpsDashboardVO dashboard = predictiveOpsService.getDashboard(
                params == null ? new PredictiveOpsQueryParams() : params);
        return AjaxResult.success(dashboard);
    }

    /**
     * 生成预测快照（持久化样本和因子，不修改业务表）。
     */
    @RequiresPermissions("finance:predictiveOps:snapshot")
    @Idempotent(scene = "finance:predictiveOps:snapshot")
    @PostMapping("/predictive-ops/snapshot")
    public AjaxResult snapshot(@RequestBody(required = false) PredictiveOpsQueryParams params) {
        int count = predictiveOpsService.createSnapshot(
                params == null ? new PredictiveOpsQueryParams() : params);
        return AjaxResult.success("成功生成 " + count + " 条预测样本", count);
    }

    /**
     * what-if 模拟（只读，不修改任何业务表）。
     */
    @RequiresPermissions("finance:predictiveOps:simulate")
    @PostMapping("/predictive-ops/what-if")
    public AjaxResult whatIf(@RequestBody WhatIfSimulationParams params) {
        WhatIfSimulationResultVO result = predictiveOpsService.simulateWhatIf(params);
        return AjaxResult.success(result);
    }
}
