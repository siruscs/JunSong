package com.junsong.finance.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinancePredictionFactor;
import com.junsong.finance.domain.FinancePredictionSample;
import com.junsong.finance.domain.FinanceWhatIfSimulation;
import com.junsong.finance.domain.vo.PredictionFactorVO;
import com.junsong.finance.domain.vo.PredictionRiskVO;
import com.junsong.finance.domain.vo.PredictiveOpsDashboardVO;
import com.junsong.finance.domain.vo.PredictiveOpsQueryParams;
import com.junsong.finance.domain.vo.WhatIfSimulationParams;
import com.junsong.finance.domain.vo.WhatIfSimulationResultVO;
import com.junsong.finance.mapper.PredictiveOpsMapper;
import com.junsong.finance.service.IPredictiveOpsService;
import com.junsong.member.api.MemberActionPredictionQuery;
import com.junsong.member.api.RemoteMemberPredictionService;
import com.junsong.member.api.domain.MemberActionPredictionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * R24 预测辅助 V2 服务实现。
 *
 * <p>使用可解释规则：基于 R16 现金流快照、R15 应收数据、R17 会员动作（经 Feign 拉取）、
 * R7/R20 库存健康做规则打分。不引入机器学习、不自动执行动作、不写回业务表。</p>
 */
@Service
public class PredictiveOpsServiceImpl implements IPredictiveOpsService {

    private static final Logger log = LoggerFactory.getLogger(PredictiveOpsServiceImpl.class);

    private static final String LOW = "LOW";
    private static final String MEDIUM = "MEDIUM";
    private static final String HIGH = "HIGH";
    private static final String CRITICAL = "CRITICAL";

    private static final String CASHFLOW = "CASHFLOW";
    private static final String RECEIVABLE = "RECEIVABLE";
    private static final String MEMBER_ACTION = "MEMBER_ACTION";
    private static final String STOCK = "STOCK";

    private static final String CASHFLOW_BASIS = "R24 现金流预测基于 R16 快照偏差率、近 7 天净现金流和应收承诺";
    private static final String RECEIVABLE_BASIS = "R24 应收兑现风险基于 R15 催收数据，统计承诺跳票、跟进间隔和账龄";
    private static final String MEMBER_BASIS = "R24 会员动作预测基于 R17 增长动作完成度，由 finance 经 Feign 聚合展示";
    private static final String STOCK_BASIS = "R24 库存风险基于当前库存、流水和快照偏差";
    private static final String WHAT_IF_BASIS = "R24 what-if 是只读模拟，不修改业务表，只调整基线压力分";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private PredictiveOpsMapper predictiveOpsMapper;

    @Autowired
    private RemoteMemberPredictionService remoteMemberPredictionService;

    @Override
    public PredictiveOpsDashboardVO getDashboard(PredictiveOpsQueryParams params) {
        if (params == null) {
            params = new PredictiveOpsQueryParams();
        }
        int windowDays = params.getWindowDays() == null || params.getWindowDays() <= 0 ? 7 : params.getWindowDays();

        PredictiveOpsDashboardVO dashboard = new PredictiveOpsDashboardVO();
        dashboard.setDeptId(params.getDeptId());
        dashboard.setWindowDays(windowDays);

        Date today = truncateToDate(new Date());

        PredictionRiskVO cashflow = buildCashflowRisk(params, windowDays, today);
        PredictionRiskVO receivable = buildReceivableRisk(params, windowDays, today);
        PredictionRiskVO stock = buildStockRisk(params, windowDays, today);
        PredictionRiskVO memberAction = buildMemberActionRisk(params, windowDays, today);

        dashboard.setCashflow(cashflow);
        dashboard.setReceivable(receivable);
        dashboard.setMemberAction(memberAction);
        dashboard.setStock(stock);

        // 现金压力基线 = 现金流 score + 应收 score 的最大值（保守）
        int baseScore = Math.max(cashflow.getScore(), receivable.getScore());
        dashboard.setBasePressureScore(baseScore);
        dashboard.setBasePressureLevel(resolveLevel(baseScore));
        dashboard.setBasis("R24 预测辅助 V2 现金压力基线取现金流与应收最大值");

        // 汇总最近因子（取现金流的因子用于展示）
        List<PredictionFactorVO> recent = new ArrayList<>();
        if (cashflow.getFactors() != null) recent.addAll(cashflow.getFactors());
        if (receivable.getFactors() != null) recent.addAll(receivable.getFactors());
        dashboard.setRecentFactors(recent);

        BigDecimal total = BigDecimal.ZERO;
        if (cashflow.getForecastAmount() != null) total = total.add(cashflow.getForecastAmount());
        if (receivable.getForecastAmount() != null) total = total.add(receivable.getForecastAmount());
        dashboard.setTotalForecastAmount(total);

        return dashboard;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createSnapshot(PredictiveOpsQueryParams params) {
        if (params == null) {
            params = new PredictiveOpsQueryParams();
        }
        int windowDays = params.getWindowDays() == null || params.getWindowDays() <= 0 ? 7 : params.getWindowDays();
        Date today = truncateToDate(new Date());
        String username = currentUsername();

        PredictiveOpsDashboardVO dashboard = getDashboard(params);
        int count = 0;
        for (PredictionRiskVO risk : new PredictionRiskVO[]{dashboard.getCashflow(), dashboard.getReceivable(), dashboard.getMemberAction(), dashboard.getStock()}) {
            if (risk == null) continue;
            FinancePredictionSample sample = new FinancePredictionSample();
            sample.setDeptId(params.getDeptId());
            sample.setPredictionType(risk.getPredictionType());
            sample.setSourceId(String.valueOf(risk.getActionId() == null ? 0L : risk.getActionId()));
            sample.setSampleDate(today);
            sample.setWindowDays(windowDays);
            sample.setScore(risk.getScore());
            sample.setLevel(risk.getLevel());
            sample.setForecastAmount(risk.getForecastAmount());
            sample.setActualAmount(risk.getActualAmount());
            sample.setDeviationAmount(risk.getDeviationAmount());
            sample.setDeviationRate(risk.getDeviationRate());
            sample.setBasis(risk.getBasis());
            sample.setCreateBy(username);
            sample.setCreateTime(new Date());
            predictiveOpsMapper.insertPredictionSample(sample);

            if (risk.getFactors() != null) {
                for (PredictionFactorVO f : risk.getFactors()) {
                    FinancePredictionFactor factor = new FinancePredictionFactor();
                    factor.setSampleId(sample.getSampleId());
                    factor.setFactorCode(f.getFactorCode());
                    factor.setFactorName(f.getFactorName());
                    factor.setFactorValue(f.getFactorValue());
                    factor.setFactorWeight(f.getFactorWeight());
                    factor.setExplanation(f.getExplanation());
                    factor.setCreateTime(new Date());
                    predictiveOpsMapper.insertPredictionFactor(factor);
                }
            }
            count++;
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WhatIfSimulationResultVO simulateWhatIf(WhatIfSimulationParams params) {
        if (params == null) {
            params = new WhatIfSimulationParams();
        }
        int windowDays = params.getWindowDays() == null || params.getWindowDays() <= 0 ? 7 : params.getWindowDays();
        Date today = truncateToDate(new Date());

        PredictiveOpsQueryParams q = new PredictiveOpsQueryParams();
        q.setDeptId(params.getDeptId());
        q.setWindowDays(windowDays);
        PredictiveOpsDashboardVO base = getDashboard(q);
        int baseScore = base.getBasePressureScore();
        String baseLevel = base.getBasePressureLevel();

        int simulatedScore = baseScore;
        List<PredictionFactorVO> factors = new ArrayList<>();
        List<String> impactAreas = new ArrayList<>();

        // 1) 预计回款增加 -> 降低现金压力 (-2 / +1000)
        BigDecimal collectionDelta = nullToZero(params.getExpectedCollectionDelta());
        if (collectionDelta.compareTo(BigDecimal.ZERO) != 0) {
            int reduction = collectionDelta.divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP).intValue() * 2;
            int delta = -Math.min(20, Math.abs(reduction));
            if (collectionDelta.compareTo(BigDecimal.ZERO) < 0) delta = -delta;
            simulatedScore = Math.max(0, simulatedScore + delta);
            factors.add(new PredictionFactorVO(
                    "COLLECTION_DELTA",
                    "预计回款变化",
                    collectionDelta.toPlainString(),
                    delta,
                    "回款每 +1000 元预估降低压力 2 分，最多 -20 分（仅模拟）"));
            impactAreas.add("CASHFLOW");
        }

        // 2) 预计费用增加 -> 提高现金压力 (+3 / +1000)
        BigDecimal expenseDelta = nullToZero(params.getExpectedExpenseDelta());
        if (expenseDelta.compareTo(BigDecimal.ZERO) != 0) {
            int addition = expenseDelta.divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP).intValue() * 3;
            int delta = Math.min(20, Math.abs(addition));
            if (expenseDelta.compareTo(BigDecimal.ZERO) < 0) delta = -delta;
            simulatedScore = Math.min(100, simulatedScore + delta);
            factors.add(new PredictionFactorVO(
                    "EXPENSE_DELTA",
                    "预计费用变化",
                    expenseDelta.toPlainString(),
                    delta,
                    "费用每 +1000 元预估增加压力 3 分，最多 +20 分（仅模拟）"));
            impactAreas.add("CASHFLOW");
        }

        // 3) 催收完成数 -> 降低应收风险 (-3 / +1)
        Integer completedCollection = params.getCompletedCollectionActions() == null ? 0 : params.getCompletedCollectionActions();
        if (completedCollection > 0) {
            int delta = -Math.min(15, completedCollection * 3);
            simulatedScore = Math.max(0, simulatedScore + delta);
            factors.add(new PredictionFactorVO(
                    "COLLECTION_COMPLETED",
                    "催收完成数",
                    String.valueOf(completedCollection),
                    delta,
                    "每完成 1 个催收动作预估降低压力 3 分，最多 -15 分（仅模拟）"));
            impactAreas.add("RECEIVABLE");
        }

        // 4) 会员动作完成数 -> 提示但不改分（按既定建议仅影响会员动作建议）
        Integer completedMember = params.getCompletedMemberActions() == null ? 0 : params.getCompletedMemberActions();
        if (completedMember > 0) {
            factors.add(new PredictionFactorVO(
                    "MEMBER_ACTION_COMPLETED",
                    "会员动作完成数",
                    String.valueOf(completedMember),
                    0,
                    "会员动作完成仅作为复盘建议，不直接调整压力分（不影响会员数据）"));
            impactAreas.add("MEMBER_ACTION");
        }

        // 5) 库存补货调整 -> 降低缺货风险但不直接改分
        BigDecimal stockDelta = nullToZero(params.getStockReplenishmentDelta());
        if (stockDelta.compareTo(BigDecimal.ZERO) != 0) {
            int delta = -Math.min(10, stockDelta.intValue());
            simulatedScore = Math.max(0, simulatedScore + delta);
            factors.add(new PredictionFactorVO(
                    "STOCK_REPLENISHMENT",
                    "库存补货调整",
                    stockDelta.toPlainString(),
                    delta,
                    "每补 +1 单位预估降低缺货压力 1 分，最多 -10 分（仅模拟）"));
            impactAreas.add("STOCK");
        }

        simulatedScore = Math.max(0, Math.min(100, simulatedScore));
        String simulatedLevel = resolveLevel(simulatedScore);
        int deltaScore = simulatedScore - baseScore;
        String deltaLevel;
        if (deltaScore <= -10) deltaLevel = "DECREASE";
        else if (deltaScore >= 10) deltaLevel = "INCREASE";
        else deltaLevel = "UNCHANGED";

        WhatIfSimulationResultVO result = new WhatIfSimulationResultVO();
        result.setDeptId(params.getDeptId());
        result.setWindowDays(windowDays);
        result.setBasePressureScore(baseScore);
        result.setBasePressureLevel(baseLevel);
        result.setSimulatedPressureScore(simulatedScore);
        result.setSimulatedPressureLevel(simulatedLevel);
        result.setDeltaScore(deltaScore);
        result.setDeltaLevel(deltaLevel);
        result.setSimulationDate(today);
        result.setBasis(WHAT_IF_BASIS);
        result.setFactors(factors);
        result.setImpactAreas(impactAreas);
        result.setRecommendation(buildWhatIfRecommendation(deltaLevel));

        // 持久化模拟记录（只写 finance_what_if_simulation，不写业务表）
        FinanceWhatIfSimulation sim = new FinanceWhatIfSimulation();
        sim.setDeptId(params.getDeptId());
        sim.setSimulationDate(today);
        sim.setBasePressureScore(baseScore);
        sim.setSimulatedPressureScore(simulatedScore);
        sim.setDeltaScore(deltaScore);
        sim.setInputJson(safeToJson(params));
        sim.setResultJson(safeToJson(result));
        sim.setCreateBy(currentUsername());
        sim.setCreateTime(new Date());
        predictiveOpsMapper.insertWhatIfSimulation(sim);
        result.setSimulationId(sim.getSimulationId());

        return result;
    }

    // ============ 规则引擎 ============

    private PredictionRiskVO buildCashflowRisk(PredictiveOpsQueryParams params, int windowDays, Date today) {
        PredictionRiskVO risk = new PredictionRiskVO();
        risk.setPredictionType(CASHFLOW);
        risk.setPredictionLabel("现金流预测");
        risk.setWindowDays(windowDays);
        risk.setSampleDate(today);
        risk.setBasis(CASHFLOW_BASIS);

        int score = 0;
        List<PredictionFactorVO> factors = new ArrayList<>();

        // 规则 1: 历史偏差率绝对值 >= 30% +30
        BigDecimal deviationRate = nullToZero(predictiveOpsMapper.selectRecentCashflowDeviation(params));
        if (deviationRate.compareTo(new BigDecimal("0.30")) >= 0) {
            score += 30;
            factors.add(new PredictionFactorVO(
                    "CASHFLOW_DEVIATION_HIGH",
                    "历史偏差率偏高",
                    deviationRate.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%",
                    30,
                    "R16 快照 30 天内平均偏差率绝对值 >= 30%，需要复核预测假设"));
        }

        // 规则 2: 近 7 天净现金流为负 +25
        BigDecimal netCashflow = nullToZero(predictiveOpsMapper.selectRecentNetCashflow(params));
        if (netCashflow.compareTo(BigDecimal.ZERO) < 0) {
            score += 25;
            factors.add(new PredictionFactorVO(
                    "NET_CASHFLOW_NEGATIVE",
                    "近 7 天净现金流为负",
                    netCashflow.toPlainString(),
                    25,
                    "近 7 天净现金流为 " + netCashflow.toPlainString() + "，短期现金紧张"));
        }

        // 规则 3: 逾期承诺占比 >= 20% +30（基于 R15 催收）
        BigDecimal overdueRatio = computeOverdueRatio(params);
        if (overdueRatio != null && overdueRatio.compareTo(new BigDecimal("0.20")) >= 0) {
            score += 30;
            factors.add(new PredictionFactorVO(
                    "OVERDUE_PROMISE_HIGH",
                    "逾期承诺占比高",
                    overdueRatio.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%",
                    30,
                    "R15 催收中已逾期承诺占总承诺 >= 20%，回款风险上升"));
        }

        // 规则 4: 30 天以上应收占比 >= 30% +15
        BigDecimal age30Ratio = computeAge30Ratio(params);
        if (age30Ratio != null && age30Ratio.compareTo(new BigDecimal("0.30")) >= 0) {
            score += 15;
            factors.add(new PredictionFactorVO(
                    "AGE_30_PLUS_RATIO",
                    "30 天以上应收占比高",
                    age30Ratio.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%",
                    15,
                    "R15 应收账龄 30 天以上占比 >= 30%，资金回笼周期偏长"));
        }

        score = Math.max(0, Math.min(100, score));
        risk.setScore(score);
        risk.setLevel(resolveLevel(score));
        risk.setForecastAmount(netCashflow.compareTo(BigDecimal.ZERO) > 0 ? netCashflow : BigDecimal.ZERO);
        risk.setDeviationRate(deviationRate);
        risk.setFactors(factors);
        risk.setRecommendation(buildCashflowRecommendation(score, factors));
        return risk;
    }

    private PredictionRiskVO buildReceivableRisk(PredictiveOpsQueryParams params, int windowDays, Date today) {
        PredictionRiskVO risk = new PredictionRiskVO();
        risk.setPredictionType(RECEIVABLE);
        risk.setPredictionLabel("应收兑现风险");
        risk.setWindowDays(windowDays);
        risk.setSampleDate(today);
        risk.setBasis(RECEIVABLE_BASIS);

        int score = 0;
        List<PredictionFactorVO> factors = new ArrayList<>();
        int totalCount = 0;
        int highRiskCount = 0;

        List<Map<String, Object>> rows = safeList(predictiveOpsMapper.selectReceivableRiskRows(params));
        for (Map<String, Object> row : rows) {
            totalCount++;
            int rowScore = 0;
            List<String> rowReasons = new ArrayList<>();

            // 规则 1: 承诺日期已逾期 +35
            Date promisedPayDate = toDate(row.get("promisedPayDate"));
            if (promisedPayDate != null && promisedPayDate.before(today)) {
                rowScore += 35;
                rowReasons.add("承诺付款日已逾期");
            }

            // 规则 2: 同一客户历史承诺未兑现次数 >= 2 +25
            Object missObj = row.get("historyMissCount");
            long miss = missObj == null ? 0L : toLong(missObj);
            if (miss >= 2) {
                rowScore += 25;
                rowReasons.add("历史跳票 " + miss + " 次");
            }

            // 规则 3: 距上次跟进超过 7 天 +20
            Date lastFollow = toDate(row.get("lastFollowTime"));
            if (lastFollow == null || daysBetween(lastFollow, today) > 7) {
                rowScore += 20;
                rowReasons.add("距上次跟进 > 7 天");
            }

            // 规则 4: 账龄超过 30 天 +20
            Object ageObj = row.get("ageDays");
            int age = ageObj == null ? 0 : toInt(ageObj);
            if (age > 30) {
                rowScore += 20;
                rowReasons.add("账龄 " + age + " 天");
            }

            rowScore = Math.max(0, Math.min(100, rowScore));
            if (rowScore >= 60) {
                highRiskCount++;
            }
        }

        // 聚合分数 = 高风险行比例
        if (totalCount > 0) {
            double ratio = highRiskCount * 1.0 / totalCount;
            if (ratio >= 0.5) {
                score += 35;
                factors.add(new PredictionFactorVO(
                        "HIGH_RISK_RATIO",
                        "高风险应收行占比",
                        (ratio * 100) + "%",
                        35,
                        "R15 应收中超过 50% 的行命中 60+ 分，建议批量处理"));
            } else if (ratio >= 0.25) {
                score += 20;
                factors.add(new PredictionFactorVO(
                        "MEDIUM_RISK_RATIO",
                        "中高风险应收行占比",
                        (ratio * 100) + "%",
                        20,
                        "R15 应收中 25%-50% 命中 60+ 分，建议纳入例会"));
            }
        }

        // 单独的聚合因子（取所有行 reasons 中最高频的）
        if (highRiskCount > 0) {
            factors.add(new PredictionFactorVO(
                    "RECEIVABLE_TOTAL",
                    "高风险应收行数",
                    highRiskCount + " / " + totalCount,
                    0,
                    "命中" + highRiskCount + "行高风险，建议按门店/客户维度分批处理"));
        }

        score = Math.max(0, Math.min(100, score));
        risk.setScore(score);
        risk.setLevel(resolveLevel(score));
        risk.setForecastAmount(BigDecimal.ZERO);
        risk.setFactors(factors);
        risk.setRecommendation(buildReceivableRecommendation(score));
        return risk;
    }

    private PredictionRiskVO buildStockRisk(PredictiveOpsQueryParams params, int windowDays, Date today) {
        PredictionRiskVO risk = new PredictionRiskVO();
        risk.setPredictionType(STOCK);
        risk.setPredictionLabel("库存风险");
        risk.setWindowDays(windowDays);
        risk.setSampleDate(today);
        risk.setBasis(STOCK_BASIS);

        int score = 0;
        List<PredictionFactorVO> factors = new ArrayList<>();
        List<Map<String, Object>> rows = safeList(predictiveOpsMapper.selectStockRiskRows(params));

        int negativeCount = 0;
        int lowStockCount = 0;
        int slowMovingCount = 0;
        int snapshotMismatchCount = 0;

        for (Map<String, Object> row : rows) {
            BigDecimal quantity = toBigDecimal(row.get("currentQuantity"));
            BigDecimal recentOutbound = toBigDecimal(row.get("recentOutbound"));
            Object mismatchObj = row.get("snapshotMismatch");
            int mismatch = mismatchObj == null ? 0 : toInt(mismatchObj);

            // 规则 1: 当前负库存 +40
            if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                negativeCount++;
            }
            // 规则 2: 近 7 天出库高且库存可用天数 < 3 +30
            if (recentOutbound.compareTo(new BigDecimal("20")) > 0
                    && quantity.compareTo(recentOutbound.divide(new BigDecimal("7"), 0, RoundingMode.HALF_UP).multiply(new BigDecimal("3"))) < 0) {
                lowStockCount++;
            }
            // 规则 3: 近 30 天无出库且库存金额高 +25
            if (recentOutbound.compareTo(BigDecimal.ZERO) == 0 && quantity.compareTo(new BigDecimal("100")) > 0) {
                slowMovingCount++;
            }
            // 规则 4: 快照与当前库存偏差 +20
            if (mismatch == 1) {
                snapshotMismatchCount++;
            }
        }

        if (negativeCount > 0) {
            score += 40;
            factors.add(new PredictionFactorVO(
                    "STOCK_NEGATIVE",
                    "负库存",
                    String.valueOf(negativeCount),
                    40,
                    negativeCount + " 个门店商品出现负结存，需要人工核查"));
        }
        if (lowStockCount > 0) {
            score += 30;
            factors.add(new PredictionFactorVO(
                    "STOCK_LOW_AVAILABLE",
                    "缺货风险",
                    String.valueOf(lowStockCount),
                    30,
                    lowStockCount + " 个门店商品出库速度高且可用天数 < 3"));
        }
        if (slowMovingCount > 0) {
            score += 25;
            factors.add(new PredictionFactorVO(
                    "STOCK_SLOW_MOVING",
                    "滞销风险",
                    String.valueOf(slowMovingCount),
                    25,
                    slowMovingCount + " 个门店商品近期无出库且库存偏高"));
        }
        if (snapshotMismatchCount > 0) {
            score += 20;
            factors.add(new PredictionFactorVO(
                    "STOCK_SNAPSHOT_MISMATCH",
                    "快照偏差",
                    String.valueOf(snapshotMismatchCount),
                    20,
                    snapshotMismatchCount + " 个门店商品当日快照与结存不一致"));
        }

        score = Math.max(0, Math.min(100, score));
        risk.setScore(score);
        risk.setLevel(resolveLevel(score));
        risk.setForecastAmount(BigDecimal.ZERO);
        risk.setFactors(factors);
        risk.setRecommendation(buildStockRecommendation(score));
        return risk;
    }

    private PredictionRiskVO buildMemberActionRisk(PredictiveOpsQueryParams params, int windowDays, Date today) {
        PredictionRiskVO risk = new PredictionRiskVO();
        risk.setPredictionType(MEMBER_ACTION);
        risk.setPredictionLabel("会员动作转化");
        risk.setWindowDays(windowDays);
        risk.setSampleDate(today);
        risk.setBasis(MEMBER_BASIS);

        int score = 0;
        List<PredictionFactorVO> factors = new ArrayList<>();

        try {
            MemberActionPredictionQuery query = new MemberActionPredictionQuery();
            query.setDeptId(params.getDeptId());
            query.setWindowDays(windowDays);
            query.setActionType(params.getActionType());
            R<List<MemberActionPredictionItem>> resp = remoteMemberPredictionService.listMemberActionPredictions(query, SecurityConstants.INNER);
            if (resp == null || resp.getCode() != R.SUCCESS || resp.getData() == null || resp.getData().isEmpty()) {
                factors.add(new PredictionFactorVO(
                        "MEMBER_SERVICE_UNAVAILABLE",
                        "会员预测服务不可用",
                        resp == null ? "null" : String.valueOf(resp.getCode()),
                        0,
                        "无法获取会员动作预测，已回退到 LOW 等级（" + (resp == null ? "" : resp.getMsg()) + "）"));
                risk.setLevel(LOW);
                risk.setScore(0);
                risk.setFactors(factors);
                risk.setRecommendation("会员预测服务暂不可用，请稍后再试或检查 junsong-member 服务");
                return risk;
            }
            int highCount = 0;
            int criticalCount = 0;
            for (MemberActionPredictionItem item : resp.getData()) {
                if (CRITICAL.equals(item.getLevel())) criticalCount++;
                if (HIGH.equals(item.getLevel())) highCount++;
            }
            if (criticalCount > 0) {
                score += 60;
                factors.add(new PredictionFactorVO(
                        "MEMBER_CRITICAL_COUNT",
                        "CRITICAL 会员动作",
                        String.valueOf(criticalCount),
                        60,
                        criticalCount + " 个会员动作预测为 CRITICAL，建议立即复核"));
            } else if (highCount > 0) {
                score += 30;
                factors.add(new PredictionFactorVO(
                        "MEMBER_HIGH_COUNT",
                        "HIGH 会员动作",
                        String.valueOf(highCount),
                        30,
                        highCount + " 个会员动作预测为 HIGH"));
            } else {
                factors.add(new PredictionFactorVO(
                        "MEMBER_LOW_COUNT",
                        "会员动作整体稳健",
                        "0 CRITICAL / 0 HIGH",
                        0,
                        "R17 会员动作预测整体为 LOW 或 MEDIUM，按现有节奏跟进"));
            }
        } catch (Exception e) {
            log.warn("R24 拉取会员预测失败: {}", e.getMessage());
            factors.add(new PredictionFactorVO(
                    "MEMBER_SERVICE_UNAVAILABLE",
                    "会员预测服务不可用",
                    "exception",
                    0,
                    "远端调用异常：" + e.getMessage()));
            risk.setLevel(LOW);
            risk.setScore(0);
            risk.setFactors(factors);
            risk.setRecommendation("会员预测服务暂不可用，已回退到 LOW 等级");
            return risk;
        }

        score = Math.max(0, Math.min(100, score));
        risk.setScore(score);
        risk.setLevel(resolveLevel(score));
        risk.setForecastAmount(BigDecimal.ZERO);
        risk.setFactors(factors);
        risk.setRecommendation(buildMemberRecommendation(score));
        return risk;
    }

    // ============ 工具方法 ============

    private BigDecimal computeOverdueRatio(PredictiveOpsQueryParams params) {
        // 直接基于 mapper 行数估算：使用行中含"已逾期"条件的比例
        // 真实比例需要 GROUP BY；为保持只读与简单实现，这里返回行级的命中比例
        List<Map<String, Object>> rows = safeList(predictiveOpsMapper.selectReceivableRiskRows(params));
        if (rows.isEmpty()) return null;
        long overdue = rows.stream()
                .filter(r -> {
                    Date d = toDate(r.get("promisedPayDate"));
                    return d != null && d.before(truncateToDate(new Date()));
                })
                .count();
        return new BigDecimal(overdue)
                .divide(new BigDecimal(rows.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeAge30Ratio(PredictiveOpsQueryParams params) {
        List<Map<String, Object>> rows = safeList(predictiveOpsMapper.selectReceivableRiskRows(params));
        if (rows.isEmpty()) return null;
        long age30 = rows.stream()
                .filter(r -> {
                    Object ageObj = r.get("ageDays");
                    int age = ageObj == null ? 0 : toInt(ageObj);
                    return age > 30;
                })
                .count();
        return new BigDecimal(age30)
                .divide(new BigDecimal(rows.size()), 4, RoundingMode.HALF_UP);
    }

    private String resolveLevel(int score) {
        if (score >= 80) return CRITICAL;
        if (score >= 60) return HIGH;
        if (score >= 30) return MEDIUM;
        return LOW;
    }

    private String buildCashflowRecommendation(int score, List<PredictionFactorVO> factors) {
        if (score >= 80) return "建议负责人当天复核现金压力，必要时调整经营节奏";
        if (score >= 60) return "建议进入动作中心处理，加快本周承诺回款兑现";
        if (score >= 30) return "在经营例会中关注，优先处理" + factors.size() + "个风险因子";
        return "现金流稳健，按现有节奏推进";
    }

    private String buildReceivableRecommendation(int score) {
        if (score >= 80) return "建议负责人当天批量处理高风险应收";
        if (score >= 60) return "建议动作中心人工处理高风险应收行";
        if (score >= 30) return "在经营例会中关注应收兑现进度";
        return "应收兑现稳健，按计划跟进";
    }

    private String buildStockRecommendation(int score) {
        if (score >= 80) return "建议立即核查库存底座，先处理负库存";
        if (score >= 60) return "建议在动作中心处理缺货和滞销";
        if (score >= 30) return "在经营例会中关注库存健康";
        return "库存健康，按现有节奏跟进";
    }

    private String buildMemberRecommendation(int score) {
        if (score >= 80) return "建议立即复核 CRITICAL 会员动作";
        if (score >= 60) return "建议动作中心人工处理会员动作";
        if (score >= 30) return "在经营例会中关注会员动作效果";
        return "会员动作整体稳健";
    }

    private String buildWhatIfRecommendation(String deltaLevel) {
        if ("DECREASE".equals(deltaLevel)) return "模拟显示压力下降，建议在真实经营中复盘此场景";
        if ("INCREASE".equals(deltaLevel)) return "模拟显示压力上升，请谨慎评估参数后重试";
        return "模拟显示压力基本不变，可作为基线参考";
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private Date toDate(Object value) {
        if (value == null) return null;
        if (value instanceof Date) return (Date) value;
        if (value instanceof java.sql.Date) return new Date(((java.sql.Date) value).getTime());
        return null;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception e) { return 0L; }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return 0; }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        try { return new BigDecimal(String.valueOf(value)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int daysBetween(Date from, Date to) {
        long diff = to.getTime() - from.getTime();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    private Date truncateToDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String currentUsername() {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String safeToJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("R24 序列化失败: {}", e.getMessage());
            return "{}";
        }
    }
}
