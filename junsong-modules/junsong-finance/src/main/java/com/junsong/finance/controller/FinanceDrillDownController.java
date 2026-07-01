package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.DrillDownDetailVO;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.service.IFinanceDrillDownService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 财务钻取 Controller - 支持从报表页面钻取到明细记录
 */
@RestController
@RequestMapping("/drilldown")
public class FinanceDrillDownController extends BaseController {

    @Autowired
    private IFinanceDrillDownService drillDownService;

    /**
     * 销售钻取：从销售报表钻取到销售订单明细
     */
    @RequiresPermissions("finance:drilldown:sales")
    @PostMapping("/sales")
    public AjaxResult getSalesDetail(@RequestBody ReportQueryParams params) {
        DrillDownDetailVO detail = drillDownService.getSalesDetail(params);
        return AjaxResult.success(detail);
    }

    /**
     * 费用钻取：从费用报表钻取到费用记录明细
     */
    @RequiresPermissions("finance:drilldown:expenses")
    @PostMapping("/expenses")
    public AjaxResult getExpensesDetail(@RequestBody ReportQueryParams params) {
        DrillDownDetailVO detail = drillDownService.getExpensesDetail(params);
        return AjaxResult.success(detail);
    }

    /**
     * 分润钻取：从分润报表钻取到分润结算记录明细
     */
    @RequiresPermissions("finance:drilldown:profitShare")
    @PostMapping("/profit-share")
    public AjaxResult getProfitShareDetail(@RequestBody ReportQueryParams params) {
        DrillDownDetailVO detail = drillDownService.getProfitShareDetail(params);
        return AjaxResult.success(detail);
    }
}
