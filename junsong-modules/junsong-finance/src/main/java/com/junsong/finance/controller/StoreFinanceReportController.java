package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.StoreOperationSummaryVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;
import com.junsong.finance.service.IStoreFinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report/store")
public class StoreFinanceReportController extends BaseController {

    @Autowired
    private IStoreFinanceReportService storeFinanceReportService;

    @RequiresPermissions("finance:report:store")
    @PostMapping("/summary")
    public AjaxResult getStoreSummary(@RequestBody StoreReportQueryParams params) {
        StoreOperationSummaryVO vo = storeFinanceReportService.getSummary(params);
        return AjaxResult.success(vo);
    }

    @RequiresPermissions("finance:report:store")
    @PostMapping("/authorized/portfolio")
    public AjaxResult getAuthorizedPortfolio(@RequestBody AuthorizedStoreReportQueryParams params) {
        AuthorizedStorePortfolioVO vo = storeFinanceReportService.getAuthorizedPortfolio(params);
        return AjaxResult.success(vo);
    }
}
