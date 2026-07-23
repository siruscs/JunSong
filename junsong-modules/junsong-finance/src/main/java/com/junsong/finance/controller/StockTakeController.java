package com.junsong.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.StockTakeRequest;
import com.junsong.finance.service.IStockTakeService;

/**
 * 库存盘点Controller。
 *
 * 权限：finance:stock:take
 * 端点：POST /stockTake
 *
 * 安全边界：
 * - 租户由 TenantContext 注入
 * - 部门由后端校验授权范围
 * - 商品必须属于当前部门
 * - 盘盈盘亏原因必填
 * - 幂等基于 takeNo
 *
 * @author junsong
 */
@RestController
@RequestMapping("/stockTake")
public class StockTakeController extends BaseController {

    @Autowired
    private IStockTakeService stockTakeService;

    @RequiresPermissions("finance:stock:take")
    @Log(title = "库存盘点", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult recordStockTake(@RequestBody StockTakeRequest request) {
        Long ledgerId = stockTakeService.recordStockTake(request);
        return AjaxResult.success(ledgerId);
    }
}
