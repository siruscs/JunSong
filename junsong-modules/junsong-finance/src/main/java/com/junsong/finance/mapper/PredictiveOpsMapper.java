package com.junsong.finance.mapper;

import com.junsong.finance.domain.FinancePredictionFactor;
import com.junsong.finance.domain.FinancePredictionSample;
import com.junsong.finance.domain.FinanceWhatIfSimulation;
import com.junsong.finance.domain.vo.PredictiveOpsQueryParams;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * R24 预测辅助 V2 数据访问接口。
 * 所有只读查询均带 dept_id / tenant_id 边界。
 */
public interface PredictiveOpsMapper {

    /**
     * 现金流近 N 天偏差率绝对值（基于 R16 快照计算）。
     */
    BigDecimal selectRecentCashflowDeviation(@Param("params") PredictiveOpsQueryParams params);

    /**
     * 现金流近 7 天净流入（实收 - 费用）。
     */
    BigDecimal selectRecentNetCashflow(@Param("params") PredictiveOpsQueryParams params);

    /**
     * 应收 R15 风险行：返回 (collectionId, deptId, status, promisedPayDate, followCount, lastFollowTime, ageDays, memberId, historyMissCount)。
     */
    List<Map<String, Object>> selectReceivableRiskRows(@Param("params") PredictiveOpsQueryParams params);

    /**
     * 库存风险行：返回 (deptId, productId, currentQuantity, recentOutboundDays, slowMovingDays, snapshotMismatch)。
     */
    List<Map<String, Object>> selectStockRiskRows(@Param("params") PredictiveOpsQueryParams params);

    /**
     * 插入预测样本。
     */
    int insertPredictionSample(FinancePredictionSample sample);

    /**
     * 插入预测因子。
     */
    int insertPredictionFactor(FinancePredictionFactor factor);

    /**
     * 插入 what-if 模拟记录。
     */
    int insertWhatIfSimulation(FinanceWhatIfSimulation simulation);
}
