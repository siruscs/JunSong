package com.junsong.finance.service;

import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.StoreOperationSummaryVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;

/**
 * 单门店财务报表Service接口
 */
public interface IStoreFinanceReportService {

    /**
     * 获取单门店经营总览
     */
    StoreOperationSummaryVO getSummary(StoreReportQueryParams params);

    /**
     * 获取授权多门店复盘聚合数据
     */
    AuthorizedStorePortfolioVO getAuthorizedPortfolio(AuthorizedStoreReportQueryParams params);
}
