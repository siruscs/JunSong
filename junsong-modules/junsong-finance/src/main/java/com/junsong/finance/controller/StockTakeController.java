package com.junsong.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.StockTakeRequest;
import com.junsong.finance.service.IStockTakeService;

/**
 * 库存盘点Controller（Task 8：旧接口收口）。
 *
 * 权限：finance:stock:take
 * 端点：POST /stockTake
 *
 * 收口策略：fail-closed 迁移响应。
 * 旧接口保留路由映射，但所有调用由 StockTakeServiceImpl.recordStockTake 抛出
 * ServiceException，返回迁移提示。不直接修改库存。
 *
 * 消费者请迁移至新工作流：POST /stocktakes（创建任务 → 盲盘 → 提交 → 审批 → 过账）。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/stockTake")
public class StockTakeController extends BaseController {

    @Autowired
    private IStockTakeService stockTakeService;

    @RequiresPermissions("finance:stock:take")
    @Log(title = "库存盘点-旧接口已收口", businessType = BusinessType.INSERT)
    @Idempotent(scene = "stockTake:record", highRisk = true, ttlSeconds = 2592000)
    @PostMapping
    public AjaxResult recordStockTake(@RequestBody StockTakeRequest request) {
        // Task 8: 旧接口收口。service 会抛出 ServiceException，永远不会返回 success。
        stockTakeService.recordStockTake(request);
        return AjaxResult.success();
    }
}
