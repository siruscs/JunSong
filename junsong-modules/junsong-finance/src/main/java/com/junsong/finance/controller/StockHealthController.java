package com.junsong.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.service.IStockHealthService;

/**
 * 库存底座健康检查Controller。
 * 本轮不开放正式库存报表，仅提供健康检查诊断接口。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/stock")
public class StockHealthController extends BaseController {

    @Autowired
    private IStockHealthService stockHealthService;

    /**
     * 查询库存底座健康状态。
     */
    @RequiresPermissions("finance:stock:health")
    @GetMapping("/health")
    public AjaxResult health() {
        return success(stockHealthService.checkHealth(TenantContext.getTenantId(), null));
    }
}