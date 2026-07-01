package com.junsong.finance.service;

import com.junsong.finance.domain.vo.DrillDownDetailVO;
import com.junsong.finance.domain.vo.ReportQueryParams;

/**
 * 财务钻取服务接口 - 从报表钻取到明细记录
 */
public interface IFinanceDrillDownService {

    /**
     * 销售钻取：从销售报表钻取到销售订单明细
     */
    DrillDownDetailVO getSalesDetail(ReportQueryParams params);

    /**
     * 费用钻取：从费用报表钻取到费用记录明细
     */
    DrillDownDetailVO getExpensesDetail(ReportQueryParams params);

    /**
     * 分润钻取：从分润报表钻取到分润结算记录明细
     */
    DrillDownDetailVO getProfitShareDetail(ReportQueryParams params);
}
