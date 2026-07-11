package com.junsong.finance.service;

import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.HealthRuleThresholdSuggestionVO;
import com.junsong.finance.domain.vo.StoreHealthTaskGenerateParams;
import com.junsong.finance.domain.vo.StoreHealthTrendQueryParams;
import com.junsong.finance.domain.vo.StoreHealthTrendRowVO;
import com.junsong.finance.domain.vo.StoreOperationSummaryVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;

import java.util.List;
import java.util.Map;

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

    /**
     * R11-C: 获取授权门店健康分趋势
     */
    List<StoreHealthTrendRowVO> getAuthorizedHealthTrend(StoreHealthTrendQueryParams params);

    /**
     * R11-D: 从健康分扣分项生成复盘任务
     * @return Map 包含 insertedCount 和 skippedCount
     */
    Map<String, Integer> generateHealthReviewTasks(StoreHealthTaskGenerateParams params);

    /**
     * R12-C: 健康规则阈值建议 — 分析授权门店数据并建议阈值调整方向
     * @param days 分析天数（默认90天）
     */
    List<HealthRuleThresholdSuggestionVO> getHealthRuleThresholdSuggestions(Integer days);
}
