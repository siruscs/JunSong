package com.junsong.finance.service;

import com.junsong.finance.domain.vo.OperatingMetric;
import com.junsong.finance.domain.vo.ReportQueryParams;

import java.util.List;

/**
 * 统一经营指标服务（Phase 5）。
 *
 * PC 和小程序调用同一端点，后端负责租户/部门范围和口径统一。
 */
public interface IFinanceOperatingMetricService {

    /**
     * 获取统一经营指标列表。
     *
     * @param params 查询参数（deptIds / timeType / startTime / endTime）
     * @return 10 个统一指标（销售/费用/净现金流/应收/逾期/库存风险/会员新增/活跃会员/待核销/待办任务）
     */
    List<OperatingMetric> getOperatingMetrics(ReportQueryParams params);
}
