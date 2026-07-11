package com.junsong.finance.service;

import com.junsong.finance.domain.vo.ReviewQualityDashboardVO;
import com.junsong.finance.domain.vo.ReviewQualityQueryParams;

/**
 * 复盘质量服务
 */
public interface IReviewQualityService {

    ReviewQualityDashboardVO getDashboard(ReviewQualityQueryParams params);
}
