package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.HealthRuleThresholdSuggestionVO;
import com.junsong.finance.domain.vo.StoreExpenseCategoryVO;
import com.junsong.finance.domain.vo.StoreHealthFactorVO;
import com.junsong.finance.domain.vo.StoreHealthTaskGenerateParams;
import com.junsong.finance.domain.vo.StoreHealthTrendQueryParams;
import com.junsong.finance.domain.vo.StoreHealthTrendRowVO;
import com.junsong.finance.domain.vo.StoreOperationSummaryVO;
import com.junsong.finance.domain.vo.StorePendingItemVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;
import com.junsong.finance.domain.vo.StoreReviewTaskVO;
import com.junsong.finance.domain.vo.StoreTrendRowVO;
import com.junsong.finance.mapper.FinanceReviewTaskMapper;
import com.junsong.finance.mapper.StoreFinanceReportMapper;
import com.junsong.finance.service.IStoreFinanceReportService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StoreFinanceReportServiceImpl implements IStoreFinanceReportService {

    @Autowired
    private StoreFinanceReportMapper storeFinanceReportMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired(required = false)
    private HealthRuleConfigReader healthRuleConfigReader;

    @Autowired(required = false)
    private FinanceReviewTaskMapper financeReviewTaskMapper;

    private static final List<String> SUPPORTED_TIME_TYPES = List.of("day", "week", "month");

    @Override
    public AuthorizedStorePortfolioVO getAuthorizedPortfolio(AuthorizedStoreReportQueryParams params) {
        normalizePortfolioTimeType(params);
        List<Long> allowedDeptIds = loadAllowedDeptIds();
        if (allowedDeptIds.isEmpty()) {
            Long userDeptId = SecurityUtils.getDeptId();
            if (userDeptId != null) {
                allowedDeptIds = Collections.singletonList(userDeptId);
            }
        }
        if (allowedDeptIds.isEmpty()) {
            throw new ServiceException("无可查看的授权门店");
        }
        List<Long> selectedDeptIds = filterSelectedDeptIds(params.getDeptIds(), allowedDeptIds);
        if (selectedDeptIds.isEmpty()) {
            throw new ServiceException("无可查看的授权门店");
        }
        List<AuthorizedStoreRowVO> rows = storeFinanceReportMapper.selectAuthorizedStoreRows(
                selectedDeptIds, params.getStartTime(), params.getEndTime());
        if (rows == null) {
            rows = new ArrayList<>();
        } else {
            rows = new ArrayList<>(rows); // 确保可变，以便后续排序
        }
        // 查询会员销售并按 deptId 合并到 rows
        List<Map<String, Object>> memberSalesRows = storeFinanceReportMapper.selectMemberSalesByDepts(
                selectedDeptIds, params.getStartTime(), params.getEndTime());
        Map<Long, BigDecimal> memberSalesByDept = new LinkedHashMap<>();
        if (memberSalesRows != null) {
            for (Map<String, Object> r : memberSalesRows) {
                Long deptId = r.get("deptId") instanceof Number
                        ? ((Number) r.get("deptId")).longValue() : null;
                BigDecimal amount = r.get("memberSales") instanceof BigDecimal
                        ? (BigDecimal) r.get("memberSales") : new BigDecimal(String.valueOf(r.get("memberSales")));
                if (deptId != null) {
                    memberSalesByDept.put(deptId, amount);
                }
            }
        }
        for (AuthorizedStoreRowVO row : rows) {
            row.setMemberSalesAmount(memberSalesByDept.getOrDefault(row.getDeptId(), BigDecimal.ZERO));
        }
        // R8-C: 合并实收现金、已核销费用
        Map<Long, BigDecimal> cashInByDept = extractDecimalMap(
                storeFinanceReportMapper.selectCashInByDepts(selectedDeptIds, params.getStartTime(), params.getEndTime()),
                "cashInAmount");
        Map<Long, BigDecimal> verifiedExpenseByDept = extractDecimalMap(
                storeFinanceReportMapper.selectVerifiedExpenseByDepts(selectedDeptIds, params.getStartTime(), params.getEndTime()),
                "verifiedExpenseAmount");
        for (AuthorizedStoreRowVO row : rows) {
            row.setCashInAmount(cashInByDept.getOrDefault(row.getDeptId(), BigDecimal.ZERO));
            BigDecimal verifiedExpense = verifiedExpenseByDept.getOrDefault(row.getDeptId(), BigDecimal.ZERO);
            row.setNetCashflowAmount(row.getCashInAmount().subtract(verifiedExpense));
        }
        fillStoreDerivedMetrics(rows);
        AuthorizedStorePortfolioVO vo = buildPortfolio(selectedDeptIds, allowedDeptIds, rows);
        // R11-B: 授权门店均值对比
        fillAuthorizedAverageComparison(rows, vo);
        List<StoreReviewTaskVO> reviewTasks = buildStoreReviewTasks(rows);
        vo.setReviewTasks(reviewTasks);

        // 回填每个门店的复盘任务数
        Map<Long, Long> taskCountByDept = reviewTasks.stream()
                .collect(Collectors.groupingBy(StoreReviewTaskVO::getDeptId, Collectors.counting()));
        for (AuthorizedStoreRowVO row : rows) {
            row.setReviewTaskCount(taskCountByDept.getOrDefault(row.getDeptId(), 0L).intValue());
            row.setAlertCount(row.getReviewTaskCount()); // 预警与复盘任务同源
        }

        // R8-C: 合并高优先级任务数和主要风险，计算 primaryRisk 和 nextAction
        List<Map<String, Object>> highRiskRows = storeFinanceReportMapper.selectHighRiskTaskCountByDepts(selectedDeptIds);
        Map<Long, Integer> highRiskCountByDept = extractIntMap(highRiskRows, "highRiskCount");
        Map<Long, String> primaryReasonByDept = extractStringMap(highRiskRows, "primaryReason");
        for (AuthorizedStoreRowVO row : rows) {
            int highRisk = highRiskCountByDept.getOrDefault(row.getDeptId(), 0);
            row.setHighRiskCount(highRisk);
            String reviewReason = primaryReasonByDept.get(row.getDeptId());
            row.setPrimaryRisk(resolvePrimaryRisk(row, reviewReason));
            row.setNextAction(resolveNextAction(row.getPrimaryRisk()));
        }

        // R8-C: 排序 — HIGH risk count desc, net cashflow asc, profit rate asc, sales amount desc
        rows.sort(Comparator.comparing(AuthorizedStoreRowVO::getHighRiskCount, Comparator.reverseOrder())
                .thenComparing(AuthorizedStoreRowVO::getNetCashflowAmount)
                .thenComparing(AuthorizedStoreRowVO::getOperatingProfitRate)
                .thenComparing(AuthorizedStoreRowVO::getTotalSales, Comparator.reverseOrder()));

        vo.setSuggestions(buildPortfolioSuggestions(vo));
        return vo;
    }

    @Override
    public List<StoreHealthTrendRowVO> getAuthorizedHealthTrend(StoreHealthTrendQueryParams params) {
        String groupBy = "month".equals(params.getTimeType()) ? "month" : "week";
        List<Long> allowedDeptIds = loadAllowedDeptIds();
        if (allowedDeptIds.isEmpty()) {
            Long userDeptId = SecurityUtils.getDeptId();
            if (userDeptId != null) {
                allowedDeptIds = Collections.singletonList(userDeptId);
            }
        }
        if (allowedDeptIds.isEmpty()) {
            throw new ServiceException("无可查看的授权门店");
        }
        List<Long> selectedDeptIds = filterSelectedDeptIds(params.getDeptIds(), allowedDeptIds);
        if (selectedDeptIds.isEmpty()) {
            throw new ServiceException("无权访问所选门店");
        }

        List<Map<String, Object>> rows = storeFinanceReportMapper.selectHealthTrendByDepts(
                selectedDeptIds, params.getStartTime(), params.getEndTime(), groupBy);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        // R11-FIX-B: 主查询已通过 UNION ALL 包含费用数据，无需再查 expenseLookup。
        // R11-FIX-C: 高风险任务数改为按周期统计，避免同一门店风险数重复塞到每个周期。
        Map<String, Integer> riskLookup = new HashMap<>();
        List<Map<String, Object>> riskRows = storeFinanceReportMapper.selectHighRiskTaskCountByDeptsAndPeriod(
                selectedDeptIds, params.getStartTime(), params.getEndTime(), groupBy);
        if (riskRows != null) {
            for (Map<String, Object> rr : riskRows) {
                Long deptId = rr.get("deptId") instanceof Number ? ((Number) rr.get("deptId")).longValue() : null;
                String periodLabel = rr.get("periodLabel") instanceof String ? (String) rr.get("periodLabel") : null;
                int count = rr.get("highRiskCount") instanceof Number ? ((Number) rr.get("highRiskCount")).intValue() : 0;
                if (deptId != null && periodLabel != null) {
                    riskLookup.put(deptId + "|" + periodLabel, count);
                }
            }
        }

        List<StoreHealthTrendRowVO> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            StoreHealthTrendRowVO vo = new StoreHealthTrendRowVO();
            Long deptId = r.get("deptId") instanceof Number ? ((Number) r.get("deptId")).longValue() : null;
            vo.setDeptId(deptId);
            vo.setDeptName(r.get("deptName") instanceof String ? (String) r.get("deptName") : null);
            String periodLabel = r.get("periodLabel") instanceof String ? (String) r.get("periodLabel") : null;
            vo.setPeriodLabel(periodLabel);
            BigDecimal sales = toBigDecimal(r.get("totalSales"));

            // R11-FIX-B: 直接使用主查询返回的真实费用数据
            BigDecimal expense = toBigDecimal(r.get("totalExpense"));

            vo.setTotalSales(sales);
            vo.setTotalExpense(expense);
            BigDecimal profit = sales.subtract(expense);
            BigDecimal profitRate = sales.compareTo(BigDecimal.ZERO) > 0
                    ? profit.multiply(new BigDecimal("100")).divide(sales, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            vo.setOperatingProfitRate(profitRate);
            // 净现金流 = 销售 - 费用
            vo.setNetCashflowAmount(profit);
            // R11-FIX-C: 按周期+门店查找高风险任务数
            vo.setHighRiskCount(riskLookup.getOrDefault(deptId + "|" + periodLabel, 0));
            // 简化健康分: 基于利润率
            int score = 100;
            if (profitRate.compareTo(readThreshold("STORE_PROFIT_RATE_LOW", new BigDecimal("5"))) < 0) {
                score -= 30;
            }
            if (sales.compareTo(BigDecimal.ZERO) == 0) {
                score -= 20;
            }
            score = Math.max(0, score);
            vo.setHealthScore(score);
            vo.setHealthLevel(score >= 85 ? "GOOD" : (score >= 70 ? "WATCH" : "RISK"));
            result.add(vo);
        }
        return result;
    }

    @Override
    public Map<String, Integer> generateHealthReviewTasks(StoreHealthTaskGenerateParams params) {
        if (financeReviewTaskMapper == null) {
            throw new ServiceException("复盘任务模块不可用");
        }
        // 1. Resolve authorized deptIds
        List<Long> allowedDeptIds = loadAllowedDeptIds();
        if (allowedDeptIds.isEmpty()) {
            Long userDeptId = SecurityUtils.getDeptId();
            if (userDeptId != null) {
                allowedDeptIds = Collections.singletonList(userDeptId);
            }
        }
        if (allowedDeptIds.isEmpty()) {
            throw new ServiceException("无可查看的授权门店");
        }
        List<Long> selectedDeptIds = filterSelectedDeptIds(params.getDeptIds(), allowedDeptIds);
        if (selectedDeptIds.isEmpty()) {
            throw new ServiceException("无权访问所选门店");
        }

        // 2. Recompute portfolio with health factors
        AuthorizedStoreReportQueryParams portfolioParams = new AuthorizedStoreReportQueryParams();
        portfolioParams.setDeptIds(selectedDeptIds);
        portfolioParams.setStartTime(params.getStartTime());
        portfolioParams.setEndTime(params.getEndTime());
        portfolioParams.setTimeType("day");
        AuthorizedStorePortfolioVO portfolio = getAuthorizedPortfolio(portfolioParams);

        // 3. For each store + matching factor, create task if no active duplicate
        List<String> targetFactors = params.getFactorCodes();
        int inserted = 0;
        int skipped = 0;
        String today = java.time.LocalDate.now().toString();

        for (AuthorizedStoreRowVO store : portfolio.getStores()) {
            if (store.getHealthFactors() == null) continue;
            for (StoreHealthFactorVO factor : store.getHealthFactors()) {
                if (targetFactors != null && !targetFactors.isEmpty()
                        && !targetFactors.contains(factor.getFactorCode())) {
                    continue;
                }
                String title = "门店健康复盘：" + factor.getFactorName();
                String alertId = "STORE_HEALTH_" + store.getDeptId() + "_" + factor.getFactorCode();

                // Idempotent: skip if active task already exists
                FinanceReviewTask existing = financeReviewTaskMapper.selectByAlertId(alertId, today);
                if (existing != null && ("PENDING".equals(existing.getStatus()) || "IN_PROGRESS".equals(existing.getStatus()))) {
                    skipped++;
                    continue;
                }

                FinanceReviewTask task = new FinanceReviewTask();
                task.setTaskType("STORE_HEALTH");
                task.setDeptId(store.getDeptId());
                task.setDeptName(store.getDeptName());
                task.setTaskDate(new Date());
                task.setStatus("PENDING");
                task.setSeverity(factor.getSeverity());
                task.setTitle(title);
                task.setReason(factor.getReason());
                task.setSuggestion(factor.getSuggestion());
                task.setImpactAmount(factor.getMetricValue() != null ? factor.getMetricValue().abs() : BigDecimal.ZERO);
                task.setAlertId(alertId);
                task.setTargetRoute("/finance/report/store?deptId=" + store.getDeptId());
                financeReviewTaskMapper.insertReviewTask(task);
                inserted++;
            }
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("insertedCount", inserted);
        result.put("skippedCount", skipped);
        return result;
    }

    // ── R12-C: 健康规则阈值建议 ──

    private static final List<String> HEALTH_RULE_CODES = List.of(
            "SALES_DECLINE", "EXPENSE_SURGE", "PROFIT_MARGIN_LOW",
            "PENDING_TASKS", "STORE_REVIEW_SCORE_LOW"
    );

    private static final Map<String, String> HEALTH_RULE_NAMES = Map.of(
            "SALES_DECLINE", "销售下滑",
            "EXPENSE_SURGE", "费用激增",
            "PROFIT_MARGIN_LOW", "利润率偏低",
            "PENDING_TASKS", "待核销任务积压",
            "STORE_REVIEW_SCORE_LOW", "复盘质量偏低"
    );

    /** Map rule code to the internal factor code used in health factor list */
    private static final Map<String, String> RULE_TO_FACTOR_CODE = Map.of(
            "SALES_DECLINE", "STORE_SALES_DROP_RATE",
            "EXPENSE_SURGE", "STORE_EXPENSE_RATE_HIGH",
            "PROFIT_MARGIN_LOW", "STORE_PROFIT_RATE_LOW",
            "PENDING_TASKS", "STORE_PENDING_AMOUNT_HIGH",
            "STORE_REVIEW_SCORE_LOW", "STORE_REVIEW_SCORE_LOW"
    );

    @Override
    public List<HealthRuleThresholdSuggestionVO> getHealthRuleThresholdSuggestions(Integer days) {
        if (days == null || days < 1) {
            days = 90;
        }

        // 1. Load authorized stores
        List<Long> allowedDeptIds = loadAllowedDeptIds();
        if (allowedDeptIds.isEmpty()) {
            Long userDeptId = SecurityUtils.getDeptId();
            if (userDeptId != null) {
                allowedDeptIds = Collections.singletonList(userDeptId);
            }
        }
        if (allowedDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Compute date range
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        // 3. Load portfolio for the range
        AuthorizedStoreReportQueryParams portfolioParams = new AuthorizedStoreReportQueryParams();
        portfolioParams.setDeptIds(allowedDeptIds);
        portfolioParams.setStartTime(toDate(start));
        portfolioParams.setEndTime(toDate(end));
        portfolioParams.setTimeType("day");

        AuthorizedStorePortfolioVO portfolio;
        try {
            portfolio = getAuthorizedPortfolio(portfolioParams);
        } catch (Exception e) {
            return Collections.emptyList();
        }

        List<AuthorizedStoreRowVO> stores = portfolio.getStores();
        int storeCount = stores != null ? stores.size() : 0;

        // 4. Analyze each rule
        List<HealthRuleThresholdSuggestionVO> results = new ArrayList<>();
        for (String ruleCode : HEALTH_RULE_CODES) {
            results.add(buildThresholdSuggestion(ruleCode, stores, storeCount, days));
        }
        return results;
    }

    private HealthRuleThresholdSuggestionVO buildThresholdSuggestion(
            String ruleCode, List<AuthorizedStoreRowVO> stores, int storeCount, int days) {

        HealthRuleThresholdSuggestionVO vo = new HealthRuleThresholdSuggestionVO();
        vo.setRuleCode(ruleCode);
        vo.setRuleName(HEALTH_RULE_NAMES.getOrDefault(ruleCode, ruleCode));
        vo.setSampleDays(days);

        // Read current threshold from config
        String factorCode = RULE_TO_FACTOR_CODE.getOrDefault(ruleCode, ruleCode);
        BigDecimal defaultThreshold = getDefaultThreshold(ruleCode);
        BigDecimal currentThreshold = readThreshold(factorCode, defaultThreshold);
        vo.setCurrentThreshold(currentThreshold.toPlainString());

        // Insufficient data check
        if (days < 14 || storeCount == 0) {
            vo.setSuggestionType("INSUFFICIENT_DATA");
            vo.setReason(days < 14 ? "分析天数不足14天，数据量不够" : "无授权门店数据");
            vo.setSuggestedThreshold(currentThreshold.toPlainString());
            vo.setAffectedStoreCount(0);
            return vo;
        }

        // Count how many stores trigger this rule (have the matching health factor)
        int triggeredCount = 0;
        List<BigDecimal> metricValues = new ArrayList<>();

        for (AuthorizedStoreRowVO store : stores) {
            BigDecimal metricValue = extractMetricForRule(ruleCode, store);
            if (metricValue != null) {
                metricValues.add(metricValue);
            }
            boolean triggered = isRuleTriggered(ruleCode, store, currentThreshold);
            if (triggered) {
                triggeredCount++;
            }
        }

        vo.setAffectedStoreCount(triggeredCount);

        // Compute percentiles from metric values
        if (!metricValues.isEmpty()) {
            Collections.sort(metricValues);
            vo.setP50(percentile(metricValues, 50));
            vo.setP75(percentile(metricValues, 75));
            vo.setP90(percentile(metricValues, 90));
        } else {
            vo.setP50(BigDecimal.ZERO);
            vo.setP75(BigDecimal.ZERO);
            vo.setP90(BigDecimal.ZERO);
        }

        // Determine suggestion type based on trigger rate
        double triggerRate = (double) triggeredCount / storeCount;

        if (triggerRate > 0.6) {
            vo.setSuggestionType("RELAX");
            vo.setReason(String.format(
                    "%.0f%% 的门店触发该规则（%d/%d），阈值可能过严",
                    triggerRate * 100, triggeredCount, storeCount));
            vo.setSuggestedThreshold(suggestRelaxedThreshold(ruleCode, currentThreshold, vo.getP75()));
        } else if (triggerRate < 0.05) {
            vo.setSuggestionType("TIGHTEN");
            vo.setReason(String.format(
                    "仅 %.0f%% 的门店触发该规则（%d/%d），阈值可能过松",
                    triggerRate * 100, triggeredCount, storeCount));
            vo.setSuggestedThreshold(suggestTightenedThreshold(ruleCode, currentThreshold, vo.getP50()));
        } else {
            vo.setSuggestionType("KEEP");
            vo.setReason(String.format(
                    "%.0f%% 的门店触发该规则（%d/%d），阈值设置合理",
                    triggerRate * 100, triggeredCount, storeCount));
            vo.setSuggestedThreshold(currentThreshold.toPlainString());
        }

        return vo;
    }

    private boolean isRuleTriggered(String ruleCode, AuthorizedStoreRowVO store, BigDecimal threshold) {
        if (store.getHealthFactors() == null) {
            return false;
        }
        String factorCode = RULE_TO_FACTOR_CODE.getOrDefault(ruleCode, ruleCode);
        return store.getHealthFactors().stream()
                .anyMatch(f -> factorCode.equals(f.getFactorCode()));
    }

    private BigDecimal extractMetricForRule(String ruleCode, AuthorizedStoreRowVO store) {
        switch (ruleCode) {
            case "PROFIT_MARGIN_LOW":
                return store.getOperatingProfitRate();
            case "SALES_DECLINE":
                return store.getSalesChangeRate();
            case "EXPENSE_SURGE":
                if (store.getTotalSales() != null && store.getTotalSales().compareTo(BigDecimal.ZERO) > 0
                        && store.getTotalExpense() != null) {
                    return store.getTotalExpense().multiply(new BigDecimal("100"))
                            .divide(store.getTotalSales(), 2, RoundingMode.HALF_UP);
                }
                return BigDecimal.ZERO;
            case "PENDING_TASKS":
                return store.getUnverifiedAmount();
            case "STORE_REVIEW_SCORE_LOW":
                return store.getReviewScore();
            default:
                return null;
        }
    }

    private BigDecimal getDefaultThreshold(String ruleCode) {
        switch (ruleCode) {
            case "SALES_DECLINE": return new BigDecimal("-20");
            case "EXPENSE_SURGE": return new BigDecimal("35");
            case "PROFIT_MARGIN_LOW": return new BigDecimal("5");
            case "PENDING_TASKS": return new BigDecimal("1000");
            case "STORE_REVIEW_SCORE_LOW": return new BigDecimal("60");
            default: return BigDecimal.ZERO;
        }
    }

    private String suggestRelaxedThreshold(String ruleCode, BigDecimal current, BigDecimal p75) {
        switch (ruleCode) {
            case "SALES_DECLINE":
                // Relax: allow more decline (e.g. from -20 to -30)
                return current.subtract(new BigDecimal("10")).toPlainString();
            case "EXPENSE_SURGE":
                // Relax: allow higher expense rate
                return current.add(new BigDecimal("10")).toPlainString();
            case "PROFIT_MARGIN_LOW":
                // Relax: lower profit threshold
                return current.subtract(new BigDecimal("2")).max(BigDecimal.ZERO).toPlainString();
            case "PENDING_TASKS":
                // Relax: higher pending amount
                return current.add(new BigDecimal("500")).toPlainString();
            case "STORE_REVIEW_SCORE_LOW":
                // Relax: lower review score threshold
                return current.subtract(new BigDecimal("10")).max(BigDecimal.ZERO).toPlainString();
            default:
                return current.toPlainString();
        }
    }

    private String suggestTightenedThreshold(String ruleCode, BigDecimal current, BigDecimal p50) {
        switch (ruleCode) {
            case "SALES_DECLINE":
                // Tighten: less decline allowed (e.g. from -20 to -10)
                return current.add(new BigDecimal("10")).toPlainString();
            case "EXPENSE_SURGE":
                // Tighten: lower expense rate allowed
                return current.subtract(new BigDecimal("5")).max(BigDecimal.ZERO).toPlainString();
            case "PROFIT_MARGIN_LOW":
                // Tighten: higher profit threshold
                return current.add(new BigDecimal("3")).toPlainString();
            case "PENDING_TASKS":
                // Tighten: lower pending amount
                return current.subtract(new BigDecimal("200")).max(BigDecimal.ZERO).toPlainString();
            case "STORE_REVIEW_SCORE_LOW":
                // Tighten: higher review score threshold
                return current.add(new BigDecimal("10")).toPlainString();
            default:
                return current.toPlainString();
        }
    }

    private BigDecimal percentile(List<BigDecimal> sorted, int pct) {
        if (sorted.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int index = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        return BigDecimal.ZERO;
    }

    private Map<Long, BigDecimal> extractDecimalMap(List<Map<String, Object>> rows, String key) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        if (rows == null) return result;
        for (Map<String, Object> r : rows) {
            Long deptId = r.get("deptId") instanceof Number
                    ? ((Number) r.get("deptId")).longValue() : null;
            BigDecimal value = r.get(key) instanceof BigDecimal
                    ? (BigDecimal) r.get(key)
                    : new BigDecimal(String.valueOf(r.getOrDefault(key, "0")));
            if (deptId != null) {
                result.put(deptId, value);
            }
        }
        return result;
    }

    private Map<Long, Integer> extractIntMap(List<Map<String, Object>> rows, String key) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        if (rows == null) return result;
        for (Map<String, Object> r : rows) {
            Long deptId = r.get("deptId") instanceof Number
                    ? ((Number) r.get("deptId")).longValue() : null;
            int value = r.get(key) instanceof Number
                    ? ((Number) r.get(key)).intValue() : 0;
            if (deptId != null) {
                result.put(deptId, value);
            }
        }
        return result;
    }

    private Map<Long, String> extractStringMap(List<Map<String, Object>> rows, String key) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (rows == null) return result;
        for (Map<String, Object> r : rows) {
            Long deptId = r.get("deptId") instanceof Number
                    ? ((Number) r.get("deptId")).longValue() : null;
            String value = r.get(key) != null ? String.valueOf(r.get(key)) : null;
            if (deptId != null) {
                result.put(deptId, value);
            }
        }
        return result;
    }

    /** R8-C: 优先级 — 复盘任务原因 > 现金流为负 > 利润率偏低 */
    private String resolvePrimaryRisk(AuthorizedStoreRowVO row, String reviewReason) {
        if (row.getHighRiskCount() != null && row.getHighRiskCount() > 0) {
            return reviewReason != null && !reviewReason.isBlank() ? reviewReason : "高优先级复盘任务待处理";
        }
        if (row.getNetCashflowAmount() != null && row.getNetCashflowAmount().compareTo(BigDecimal.ZERO) < 0) {
            return "现金流为负";
        }
        if (row.getOperatingProfitRate() != null && row.getOperatingProfitRate().compareTo(new BigDecimal("5")) < 0) {
            return "利润率偏低";
        }
        return null;
    }

    /** R8-C: 根据 primaryRisk 给出具体行动文本 */
    private String resolveNextAction(String primaryRisk) {
        if (primaryRisk == null) {
            return "继续观察";
        }
        if (primaryRisk.contains("复盘任务")) {
            return "优先处理高优先级复盘任务";
        }
        if (primaryRisk.contains("现金流为负")) {
            return "优先复核大额费用支出，确保现金流入覆盖支出";
        }
        if (primaryRisk.contains("利润率偏低")) {
            return "优化成本结构或提升客单价";
        }
        return "优先复盘：" + primaryRisk;
    }

    private void normalizePortfolioTimeType(AuthorizedStoreReportQueryParams params) {
        String timeType = params.getTimeType();
        if (timeType == null || timeType.isBlank() || !SUPPORTED_TIME_TYPES.contains(timeType)) {
            params.setTimeType("day");
        }
    }

    private List<Long> filterSelectedDeptIds(List<Long> requested, List<Long> allowed) {
        if (requested == null || requested.isEmpty()) {
            return allowed;
        }
        return requested.stream()
                .filter(allowed::contains)
                .toList();
    }

    private void fillStoreDerivedMetrics(List<AuthorizedStoreRowVO> rows) {
        for (AuthorizedStoreRowVO row : rows) {
            if (row.getTotalSales() == null) row.setTotalSales(BigDecimal.ZERO);
            if (row.getTotalExpense() == null) row.setTotalExpense(BigDecimal.ZERO);
            if (row.getSaleCount() == null) row.setSaleCount(0);
            if (row.getSaleQuantity() == null) row.setSaleQuantity(0);
            if (row.getUnverifiedAmount() == null) row.setUnverifiedAmount(BigDecimal.ZERO);
            if (row.getMemberSalesAmount() == null) row.setMemberSalesAmount(BigDecimal.ZERO);
            if (row.getCashInAmount() == null) row.setCashInAmount(BigDecimal.ZERO);
            if (row.getNetCashflowAmount() == null) row.setNetCashflowAmount(BigDecimal.ZERO);
            if (row.getHighRiskCount() == null) row.setHighRiskCount(0);

            BigDecimal profit = row.getTotalSales().subtract(row.getTotalExpense());
            row.setOperatingProfit(profit);

            BigDecimal profitRate = row.getTotalSales().compareTo(BigDecimal.ZERO) > 0
                    ? profit.multiply(new BigDecimal("100")).divide(row.getTotalSales(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            row.setOperatingProfitRate(profitRate);

            BigDecimal avgOrder = row.getSaleCount() > 0
                    ? row.getTotalSales().divide(new BigDecimal(row.getSaleCount()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            row.setAvgOrderAmount(avgOrder);

            // 会员销售占比
            BigDecimal memberRatio = row.getTotalSales().compareTo(BigDecimal.ZERO) > 0
                    ? row.getMemberSalesAmount().multiply(new BigDecimal("100"))
                        .divide(row.getTotalSales(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            row.setMemberSalesRatio(memberRatio);

            // R11-B: 健康分 V2 — 可配置阈值 + 扣分因子列表
            BigDecimal expenseRate = row.getTotalSales().compareTo(BigDecimal.ZERO) > 0
                    ? row.getTotalExpense().multiply(new BigDecimal("100")).divide(row.getTotalSales(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal pendingAmount = row.getUnverifiedAmount(); // pendingExpenseAmount in R11

            int score = 100;
            Map<String, Integer> breakdown = new LinkedHashMap<>();
            List<String> reasons = new ArrayList<>();
            List<StoreHealthFactorVO> factors = new ArrayList<>();

            // 读取可配置阈值（缺失则用默认值）
            BigDecimal profitThreshold = readThreshold("STORE_PROFIT_RATE_LOW", new BigDecimal("5"));
            BigDecimal salesDropThreshold = readThreshold("STORE_SALES_DROP_RATE", new BigDecimal("-20"));
            BigDecimal expenseThreshold = readThreshold("STORE_EXPENSE_RATE_HIGH", new BigDecimal("35"));
            BigDecimal pendingThreshold = readThreshold("STORE_PENDING_AMOUNT_HIGH", new BigDecimal("1000"));

            // 因子 1：利润率偏低 (-30 HIGH)
            if (profitRate.compareTo(profitThreshold) < 0) {
                int deduct = 30;
                score -= deduct;
                breakdown.put("profitMargin", -deduct);
                reasons.add(profit.compareTo(BigDecimal.ZERO) < 0 ? "NEGATIVE_PROFIT" : "LOW_MARGIN");
                factors.add(buildFactor("STORE_PROFIT_RATE_LOW", "门店利润率偏低", "HIGH", deduct,
                        profitRate, profitThreshold,
                        profit.compareTo(BigDecimal.ZERO) < 0 ? "利润为负" : "利润率低于" + profitThreshold + "%",
                        readSuggestion("STORE_PROFIT_RATE_LOW", "利润率偏低，建议复核售价、折扣、费用和进货成本。"),
                        "/finance/overview"));
            } else {
                breakdown.put("profitMargin", 0);
            }

            // 因子 2：销售下滑 (-25 HIGH) — 仅在有上期数据时检查
            if (row.getSalesChangeRate() != null) {
                if (row.getSalesChangeRate().compareTo(salesDropThreshold) < 0) {
                    int deduct = 25;
                    score -= deduct;
                    breakdown.put("salesTrend", -deduct);
                    reasons.add("SALES_DROP");
                    factors.add(buildFactor("STORE_SALES_DROP_RATE", "门店销售下滑", "HIGH", deduct,
                            row.getSalesChangeRate(), salesDropThreshold,
                            "销售较上期下降" + row.getSalesChangeRate() + "%",
                            readSuggestion("STORE_SALES_DROP_RATE", "销售下滑超过" + salesDropThreshold + "%，建议复盘客流、活动和会员触达。"),
                            "/finance/overview"));
                } else {
                    breakdown.put("salesTrend", 0);
                }
            } else {
                breakdown.put("salesTrend", 0);
            }

            // 因子 3：费用率偏高 (-15 MEDIUM)
            if (expenseRate.compareTo(expenseThreshold) > 0) {
                int deduct = 15;
                score -= deduct;
                breakdown.put("expenseControl", -deduct);
                reasons.add("EXPENSE_SPIKE");
                factors.add(buildFactor("STORE_EXPENSE_RATE_HIGH", "门店费用率偏高", "MEDIUM", deduct,
                        expenseRate, expenseThreshold,
                        "费用率" + expenseRate + "%超过阈值" + expenseThreshold + "%",
                        readSuggestion("STORE_EXPENSE_RATE_HIGH", "费用率偏高，建议查看费用分类和未核销费用。"),
                        "/finance/expense"));
            } else {
                breakdown.put("expenseControl", 0);
            }

            // 因子 4：未核销金额偏高 (-15 MEDIUM)
            if (pendingAmount.compareTo(pendingThreshold) > 0) {
                int deduct = 15;
                score -= deduct;
                breakdown.put("verificationTimeliness", -deduct);
                reasons.add("UNVERIFIED_HIGH");
                factors.add(buildFactor("STORE_PENDING_AMOUNT_HIGH", "门店未核销金额偏高", "MEDIUM", deduct,
                        pendingAmount, pendingThreshold,
                        "未核销金额" + pendingAmount + "超过阈值" + pendingThreshold,
                        readSuggestion("STORE_PENDING_AMOUNT_HIGH", "未核销金额偏高，建议优先处理费用和借支核销。"),
                        "/finance/expense"));
            } else {
                breakdown.put("verificationTimeliness", 0);
            }

            // 因子 5：净现金流为负 (-15 HIGH)
            if (row.getNetCashflowAmount().compareTo(BigDecimal.ZERO) < 0) {
                int deduct = 15;
                score -= deduct;
                breakdown.put("cashflow", -deduct);
                reasons.add("CASHFLOW_NEGATIVE");
                factors.add(buildFactor("STORE_CASHFLOW_NEGATIVE", "门店净现金流为负", "HIGH", deduct,
                        row.getNetCashflowAmount(), BigDecimal.ZERO,
                        "净现金流为" + row.getNetCashflowAmount(),
                        readSuggestion("STORE_CASHFLOW_NEGATIVE", "净现金流为负，建议优先复核大额费用支出。"),
                        "/finance/overview"));
            } else {
                breakdown.put("cashflow", 0);
            }

            // 因子 6：会员贡献偏低 (-10 MEDIUM) — 有销售时
            if (row.getTotalSales().compareTo(BigDecimal.ZERO) > 0
                    && memberRatio.compareTo(new BigDecimal("20")) < 0) {
                int deduct = 10;
                score -= deduct;
                breakdown.put("memberContribution", -deduct);
                reasons.add("MEMBER_LOW");
                factors.add(buildFactor("MEMBER_CONTRIBUTION_LOW", "会员贡献偏低", "MEDIUM", deduct,
                        memberRatio, new BigDecimal("20"),
                        "会员销售占比" + memberRatio + "%低于20%",
                        "会员销售占比偏低，建议加强会员触达和活动推广。",
                        "/member/overview"));
            } else {
                breakdown.put("memberContribution", 0);
            }

            // 因子 7：复盘质量偏低 (-10 LOW) — R11-P1-fix
            BigDecimal reviewScoreThreshold = readThreshold("STORE_REVIEW_SCORE_LOW", new BigDecimal("60"));
            BigDecimal reviewScore = computeReviewScore(row.getDeptId());
            row.setReviewScore(reviewScore);
            if (reviewScore.compareTo(reviewScoreThreshold) < 0) {
                int deduct = 10;
                score -= deduct;
                breakdown.put("reviewQuality", -deduct);
                reasons.add("REVIEW_SCORE_LOW");
                factors.add(buildFactor("STORE_REVIEW_SCORE_LOW", "复盘质量偏低", "LOW", deduct,
                        reviewScore, reviewScoreThreshold,
                        "复盘完成率" + reviewScore + "%低于阈值" + reviewScoreThreshold + "%",
                        readSuggestion("STORE_REVIEW_SCORE_LOW", "复盘任务完成率偏低，建议优先处理待复盘项。"),
                        "/finance/reviewTask"));
            } else {
                breakdown.put("reviewQuality", 0);
            }

            score = Math.max(0, score);
            row.setHealthScore(score);
            // R11 V2: 等级阈值调整 — >=85 GOOD, 70-84 WATCH, <70 RISK
            row.setHealthLevel(score >= 85 ? "GOOD" : (score >= 70 ? "WATCH" : "RISK"));
            row.setHealthScoreVersion("R11_V2");
            row.setHealthBreakdown(breakdown);
            row.setReviewReasons(reasons);
            // 按严重度排序: HIGH > MEDIUM > LOW, 同级别按扣分降序
            factors.sort(Comparator.comparing(StoreHealthFactorVO::getSeverity, (a, b) -> {
                int oa = "HIGH".equals(a) ? 0 : ("MEDIUM".equals(a) ? 1 : 2);
                int ob = "HIGH".equals(b) ? 0 : ("MEDIUM".equals(b) ? 1 : 2);
                return Integer.compare(oa, ob);
            }).thenComparing(StoreHealthFactorVO::getDeductedScore, Comparator.reverseOrder()));
            row.setHealthFactors(factors);
            row.setHealthSummary(buildHealthSummary(score, row.getHealthLevel(), factors));
        }
    }

    private BigDecimal readThreshold(String ruleCode, BigDecimal defaultValue) {
        return healthRuleConfigReader != null
                ? healthRuleConfigReader.getThreshold(ruleCode, defaultValue)
                : defaultValue;
    }

    private String readSuggestion(String ruleCode, String defaultValue) {
        return healthRuleConfigReader != null
                ? healthRuleConfigReader.getSuggestion(ruleCode, defaultValue)
                : defaultValue;
    }

    /** Compute review score as completion rate (DONE / total * 100) for a single dept. */
    private BigDecimal computeReviewScore(Long deptId) {
        if (financeReviewTaskMapper == null || deptId == null) {
            return new BigDecimal("100");
        }
        try {
            List<Long> singleDept = Collections.singletonList(deptId);
            int doneCount = financeReviewTaskMapper.countByStatus("DONE", singleDept);
            int totalCount = doneCount
                    + financeReviewTaskMapper.countByStatus("PENDING", singleDept)
                    + financeReviewTaskMapper.countByStatus("IN_PROGRESS", singleDept);
            if (totalCount == 0) {
                return new BigDecimal("100");
            }
            return BigDecimal.valueOf(doneCount * 100L)
                    .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return new BigDecimal("100");
        }
    }

    private StoreHealthFactorVO buildFactor(String code, String name, String severity, int deduct,
                                             BigDecimal metricValue, BigDecimal threshold,
                                             String reason, String suggestion, String targetRoute) {
        StoreHealthFactorVO f = new StoreHealthFactorVO();
        f.setFactorCode(code);
        f.setFactorName(name);
        f.setSeverity(severity);
        f.setDeductedScore(deduct);
        f.setMetricValue(metricValue);
        f.setThresholdValue(threshold);
        f.setReason(reason);
        f.setSuggestion(suggestion);
        f.setTargetRoute(targetRoute);
        return f;
    }

    private String buildHealthSummary(int score, String level, List<StoreHealthFactorVO> factors) {
        if (factors.isEmpty()) {
            return "经营状态良好，暂无扣分项。";
        }
        long highCount = factors.stream().filter(f -> "HIGH".equals(f.getSeverity())).count();
        long medCount = factors.stream().filter(f -> "MEDIUM".equals(f.getSeverity())).count();
        StringBuilder sb = new StringBuilder();
        sb.append("健康分").append(score).append("分（").append(level).append("）");
        if (highCount > 0) sb.append("，").append(highCount).append("项高风险");
        if (medCount > 0) sb.append("，").append(medCount).append("项中风险");
        sb.append("。优先处理：").append(factors.get(0).getFactorName());
        return sb.toString();
    }

    private AuthorizedStorePortfolioVO buildPortfolio(List<Long> selectedDeptIds, List<Long> allowedDeptIds,
                                                       List<AuthorizedStoreRowVO> rows) {
        AuthorizedStorePortfolioVO vo = new AuthorizedStorePortfolioVO();
        vo.setAllowedDeptIds(allowedDeptIds);
        vo.setSelectedDeptIds(selectedDeptIds);
        vo.setStoreCount(rows.size());
        vo.setStores(rows);

        BigDecimal totalSales = rows.stream().map(AuthorizedStoreRowVO::getTotalSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = rows.stream().map(AuthorizedStoreRowVO::getTotalExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal operatingProfit = totalSales.subtract(totalExpense);
        BigDecimal profitRate = totalSales.compareTo(BigDecimal.ZERO) > 0
                ? operatingProfit.multiply(new BigDecimal("100")).divide(totalSales, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        vo.setTotalSales(totalSales);
        vo.setTotalExpense(totalExpense);
        vo.setOperatingProfit(operatingProfit);
        vo.setOperatingProfitRate(profitRate);

        // 门店均值对比
        int count = rows.size();
        if (count > 0) {
            BigDecimal n = new BigDecimal(count);
            vo.setAvgSales(totalSales.divide(n, 2, RoundingMode.HALF_UP));
            vo.setAvgExpense(totalExpense.divide(n, 2, RoundingMode.HALF_UP));
            vo.setAvgProfit(operatingProfit.divide(n, 2, RoundingMode.HALF_UP));
            vo.setAvgProfitRate(profitRate); // 总体利润率即均值参考
        } else {
            vo.setAvgSales(BigDecimal.ZERO);
            vo.setAvgExpense(BigDecimal.ZERO);
            vo.setAvgProfit(BigDecimal.ZERO);
            vo.setAvgProfitRate(BigDecimal.ZERO);
        }
        return vo;
    }

    private List<StoreReviewTaskVO> buildStoreReviewTasks(List<AuthorizedStoreRowVO> rows) {
        List<StoreReviewTaskVO> tasks = new ArrayList<>();
        for (AuthorizedStoreRowVO row : rows) {
            if (row.getReviewReasons() == null) continue;
            for (String reason : row.getReviewReasons()) {
                StoreReviewTaskVO task = new StoreReviewTaskVO();
                task.setDeptId(row.getDeptId());
                task.setDeptName(row.getDeptName());
                task.setTaskType(reason);
                task.setAmount(row.getOperatingProfit());
                switch (reason) {
                    case "NEGATIVE_PROFIT":
                        task.setSeverity("HIGH");
                        task.setTitle(row.getDeptName() + " 经营利润为负");
                        task.setReason("经营利润为负，需优先复盘");
                        break;
                    case "NO_SALES":
                        task.setSeverity("HIGH");
                        task.setTitle(row.getDeptName() + " 无销售记录");
                        task.setReason("本期无销售记录");
                        break;
                    case "LOW_MARGIN":
                        task.setSeverity("MEDIUM");
                        task.setTitle(row.getDeptName() + " 利润率偏低");
                        task.setReason("利润率低于 10%");
                        break;
                    case "UNVERIFIED_HIGH":
                        task.setSeverity("MEDIUM");
                        task.setTitle(row.getDeptName() + " 未核销金额较高");
                        task.setReason("未核销金额超过 1000");
                        break;
                    case "EXPENSE_SPIKE":
                        task.setSeverity("MEDIUM");
                        task.setTitle(row.getDeptName() + " 费用占比过高");
                        task.setReason("费用超过销售额的 70%");
                        break;
                    case "SALES_DROP":
                        task.setSeverity("MEDIUM");
                        task.setTitle(row.getDeptName() + " 销售额下滑");
                        task.setReason("销售额较上期下降超过 20%");
                        break;
                    case "MEMBER_LOW":
                        task.setSeverity("LOW");
                        task.setTitle(row.getDeptName() + " 会员贡献偏低");
                        task.setReason("会员销售占比低于 20%");
                        break;
                    default:
                        task.setSeverity("LOW");
                        task.setTitle(row.getDeptName() + " " + reason);
                        task.setReason(reason);
                        break;
                }
                task.setPriority(severityRank(task.getSeverity()));
                tasks.add(task);
            }
        }
        tasks.sort(Comparator.<StoreReviewTaskVO, Integer>comparing(StoreReviewTaskVO::getPriority)
                .thenComparing(t -> t.getAmount() != null ? t.getAmount().abs() : BigDecimal.ZERO, Comparator.reverseOrder())
                .thenComparing(StoreReviewTaskVO::getDeptName));
        if (tasks.size() > 20) {
            tasks = tasks.subList(0, 20);
        }
        return tasks;
    }

    private int severityRank(String severity) {
        if ("HIGH".equals(severity)) return 1;
        if ("MEDIUM".equals(severity)) return 2;
        return 3;
    }

    private void fillAuthorizedAverageComparison(List<AuthorizedStoreRowVO> rows, AuthorizedStorePortfolioVO vo) {
        BigDecimal avgSales = vo.getAvgSales() != null ? vo.getAvgSales() : BigDecimal.ZERO;
        BigDecimal avgProfitRate = vo.getAvgProfitRate() != null ? vo.getAvgProfitRate() : BigDecimal.ZERO;
        for (AuthorizedStoreRowVO row : rows) {
            row.setAuthorizedAverageSales(avgSales);
            row.setAuthorizedAverageProfitRate(avgProfitRate);
            // salesVsAuthorizedAverageRate: (store sales - avg) / avg * 100
            if (avgSales.compareTo(BigDecimal.ZERO) > 0) {
                row.setSalesVsAuthorizedAverageRate(
                    row.getTotalSales().subtract(avgSales).multiply(new BigDecimal("100"))
                        .divide(avgSales, 2, RoundingMode.HALF_UP));
            } else {
                row.setSalesVsAuthorizedAverageRate(BigDecimal.ZERO);
            }
            // profitRateVsAuthorizedAverage: store profitRate - avg profitRate
            row.setProfitRateVsAuthorizedAverage(
                row.getOperatingProfitRate().subtract(avgProfitRate));
        }
    }

    private List<String> buildPortfolioSuggestions(AuthorizedStorePortfolioVO vo) {
        List<String> suggestions = new ArrayList<>();
        long riskCount = vo.getStores().stream().filter(s -> "RISK".equals(s.getHealthLevel())).count();
        if (riskCount > 0) {
            suggestions.add("存在 " + riskCount + " 家风险门店，建议优先处理利润为负或费用异常门店。");
        }
        BigDecimal totalUnverified = vo.getStores().stream()
                .map(s -> s.getUnverifiedAmount() != null ? s.getUnverifiedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalUnverified.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add("授权门店存在未核销金额，建议按复盘任务清单逐项跟进。");
        }
        if (riskCount == 0 && totalUnverified.compareTo(BigDecimal.ZERO) == 0) {
            suggestions.add("授权门店整体经营状态稳定，可重点复盘高利润门店的可复制动作。");
        }
        return suggestions;
    }

    @Override
    public StoreOperationSummaryVO getSummary(StoreReportQueryParams params) {
        validateStoreAccess(params);

        StoreOperationSummaryVO vo = new StoreOperationSummaryVO();
        vo.setDeptId(params.getDeptId());

        BigDecimal totalSales = nullSafe(storeFinanceReportMapper.selectStoreTotalSales(params));
        Integer saleCount = storeFinanceReportMapper.countStoreSaleRecords(params);
        if (saleCount == null) {
            saleCount = 0;
        }
        Integer saleQuantity = storeFinanceReportMapper.sumStoreSaleQuantity(params);
        if (saleQuantity == null) {
            saleQuantity = 0;
        }
        BigDecimal totalExpense = nullSafe(storeFinanceReportMapper.selectStoreTotalExpense(params));
        BigDecimal unverifiedExpense = nullSafe(storeFinanceReportMapper.selectStoreUnverifiedExpense(params));
        BigDecimal unverifiedAdvance = nullSafe(storeFinanceReportMapper.selectStoreUnverifiedAdvance(params));

        BigDecimal avgOrderAmount = saleCount > 0
                ? totalSales.divide(new BigDecimal(saleCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal operatingProfit = totalSales.subtract(totalExpense);

        BigDecimal operatingProfitRate = totalSales.compareTo(BigDecimal.ZERO) > 0
                ? operatingProfit.multiply(new BigDecimal("100")).divide(totalSales, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        vo.setTotalSales(totalSales);
        vo.setSaleCount(saleCount);
        vo.setSaleQuantity(saleQuantity);
        vo.setAvgOrderAmount(avgOrderAmount);
        vo.setTotalExpense(totalExpense);
        vo.setUnverifiedExpense(unverifiedExpense);
        vo.setUnverifiedAdvance(unverifiedAdvance);
        vo.setOperatingProfit(operatingProfit);
        vo.setOperatingProfitRate(operatingProfitRate);

        String periodStatus = storeFinanceReportMapper.selectCurrentAccountingPeriodStatus(params.getDeptId());
        vo.setAccountingPeriodStatus(periodStatus);

        List<String> alerts = buildAlerts(totalSales, operatingProfit, unverifiedExpense, unverifiedAdvance);
        vo.setAlerts(alerts);

        // 合并销售趋势和费用趋势
        List<StoreTrendRowVO> salesTrend = storeFinanceReportMapper.selectStoreSalesTrend(params);
        if (salesTrend == null) {
            salesTrend = Collections.emptyList();
        }
        List<StoreTrendRowVO> expenseTrend = storeFinanceReportMapper.selectStoreExpenseTrend(params);
        if (expenseTrend == null) {
            expenseTrend = Collections.emptyList();
        }
        vo.setTrendRows(mergeTrends(salesTrend, expenseTrend));

        // 费用分类（含百分比）
        List<StoreExpenseCategoryVO> categories = storeFinanceReportMapper.selectStoreExpenseCategories(params);
        if (categories == null) {
            categories = Collections.emptyList();
        }
        fillCategoryPercents(categories, totalExpense);
        vo.setExpenseCategories(categories);

        // 未核销清单
        List<StorePendingItemVO> pendingExpenses = storeFinanceReportMapper.selectStoreUnverifiedExpenses(params);
        if (pendingExpenses == null) {
            pendingExpenses = Collections.emptyList();
        }
        List<StorePendingItemVO> pendingAdvances = storeFinanceReportMapper.selectStoreUnverifiedAdvances(params);
        if (pendingAdvances == null) {
            pendingAdvances = Collections.emptyList();
        }
        List<StorePendingItemVO> allPending = new ArrayList<>(pendingExpenses);
        allPending.addAll(pendingAdvances);
        vo.setPendingItems(allPending);

        // 上期对比
        fillPreviousPeriod(vo, params);

        // 经营建议
        vo.setSuggestions(buildSuggestions(vo));

        return vo;
    }

    private void fillPreviousPeriod(StoreOperationSummaryVO vo, StoreReportQueryParams params) {
        if (params.getStartTime() == null || params.getEndTime() == null) {
            vo.setPreviousTotalSales(BigDecimal.ZERO);
            vo.setPreviousTotalExpense(BigDecimal.ZERO);
            vo.setPreviousOperatingProfit(BigDecimal.ZERO);
            vo.setSalesChangeRate(BigDecimal.ZERO);
            vo.setExpenseChangeRate(BigDecimal.ZERO);
            vo.setProfitChangeRate(BigDecimal.ZERO);
            return;
        }

        DateRange prev = previousRange(params.getStartTime(), params.getEndTime());
        BigDecimal prevSales = nullSafe(storeFinanceReportMapper.selectStoreTotalSalesForRange(
                params.getDeptId(), prev.startTime, prev.endTime));
        BigDecimal prevExpense = nullSafe(storeFinanceReportMapper.selectStoreTotalExpenseForRange(
                params.getDeptId(), prev.startTime, prev.endTime));
        BigDecimal prevProfit = prevSales.subtract(prevExpense);

        vo.setPreviousTotalSales(prevSales);
        vo.setPreviousTotalExpense(prevExpense);
        vo.setPreviousOperatingProfit(prevProfit);
        vo.setSalesChangeRate(calculateChangeRate(vo.getTotalSales(), prevSales));
        vo.setExpenseChangeRate(calculateChangeRate(vo.getTotalExpense(), prevExpense));
        vo.setProfitChangeRate(calculateChangeRate(vo.getOperatingProfit(), prevProfit));
    }

    private DateRange previousRange(Date startTime, Date endTime) {
        LocalDate start = startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1);
        return new DateRange(toDate(previousStart), toDate(previousEnd));
    }

    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0 && current.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("100.00");
        }
        return current.subtract(previous)
                .multiply(new BigDecimal("100"))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private static java.util.Date toDate(LocalDate localDate) {
        return java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static class DateRange {
        private final Date startTime;
        private final Date endTime;
        DateRange(Date startTime, Date endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private List<String> buildAlerts(BigDecimal totalSales, BigDecimal operatingProfit,
                                     BigDecimal unverifiedExpense, BigDecimal unverifiedAdvance) {
        List<String> alerts = new ArrayList<>();
        if (unverifiedExpense.compareTo(BigDecimal.ZERO) > 0) {
            alerts.add("存在未核销费用 " + unverifiedExpense + " 元");
        }
        if (unverifiedAdvance.compareTo(BigDecimal.ZERO) > 0) {
            alerts.add("存在未核销借支 " + unverifiedAdvance + " 元");
        }
        if (totalSales.compareTo(BigDecimal.ZERO) == 0) {
            alerts.add("本期暂无销售记录");
        }
        if (operatingProfit.compareTo(BigDecimal.ZERO) < 0) {
            alerts.add("本期经营利润为负，请关注费用支出");
        }
        return alerts;
    }

    private List<String> buildSuggestions(StoreOperationSummaryVO vo) {
        List<String> suggestions = new ArrayList<>();
        if (vo.getOperatingProfit().compareTo(BigDecimal.ZERO) < 0) {
            suggestions.add("经营利润为负，建议优先复核大额费用和低毛利销售记录。");
        }
        if (vo.getUnverifiedExpense().add(vo.getUnverifiedAdvance()).compareTo(new BigDecimal("1000")) > 0) {
            suggestions.add("未核销金额较高，建议当天完成费用和借支核销。");
        }
        if (vo.getExpenseChangeRate() != null && vo.getExpenseChangeRate().compareTo(new BigDecimal("30")) > 0) {
            suggestions.add("费用较上期增长超过 30%，建议拆解费用分类并确认是否为一次性支出。");
        }
        if (vo.getSalesChangeRate() != null && vo.getSalesChangeRate().compareTo(new BigDecimal("-20")) < 0) {
            suggestions.add("销售额较上期下降超过 20%，建议复盘到店转化和促销活动效果。");
        }
        return suggestions;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private List<StoreTrendRowVO> mergeTrends(List<StoreTrendRowVO> salesTrend, List<StoreTrendRowVO> expenseTrend) {
        Map<String, StoreTrendRowVO> merged = new LinkedHashMap<>();

        for (StoreTrendRowVO row : salesTrend) {
            StoreTrendRowVO m = new StoreTrendRowVO();
            m.setDateStr(row.getDateStr());
            m.setSalesAmount(nullSafe(row.getSalesAmount()));
            m.setExpenseAmount(BigDecimal.ZERO);
            m.setOperatingProfit(nullSafe(row.getSalesAmount()));
            merged.put(row.getDateStr(), m);
        }

        for (StoreTrendRowVO row : expenseTrend) {
            StoreTrendRowVO m = merged.get(row.getDateStr());
            if (m == null) {
                m = new StoreTrendRowVO();
                m.setDateStr(row.getDateStr());
                m.setSalesAmount(BigDecimal.ZERO);
                m.setOperatingProfit(BigDecimal.ZERO);
                merged.put(row.getDateStr(), m);
            }
            m.setExpenseAmount(nullSafe(row.getExpenseAmount()));
            m.setOperatingProfit(m.getSalesAmount().subtract(m.getExpenseAmount()));
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(StoreTrendRowVO::getDateStr))
                .toList();
    }

    private void fillCategoryPercents(List<StoreExpenseCategoryVO> categories, BigDecimal totalExpense) {
        if (categories.isEmpty()) {
            return;
        }
        for (StoreExpenseCategoryVO cat : categories) {
            if (cat.getAmount() == null) {
                cat.setAmount(BigDecimal.ZERO);
            }
            if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                cat.setPercent(cat.getAmount().multiply(new BigDecimal("100"))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP));
            } else {
                cat.setPercent(BigDecimal.ZERO);
            }
        }
    }

    private void validateStoreAccess(StoreReportQueryParams params) {
        if (params.getDeptId() == null) {
            throw new ServiceException("门店ID不能为空");
        }
        normalizeTimeType(params);
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            Long userDeptId = SecurityUtils.getDeptId();
            if (userDeptId != null) {
                allowed = Collections.singletonList(userDeptId);
            }
        }
        if (!allowed.contains(params.getDeptId())) {
            throw new ServiceException("无权查看该门店经营报表");
        }
    }

    private void normalizeTimeType(StoreReportQueryParams params) {
        String timeType = params.getTimeType();
        if (timeType == null || timeType.isBlank() || !SUPPORTED_TIME_TYPES.contains(timeType)) {
            params.setTimeType("day");
        }
    }

    List<Long> loadAllowedDeptIds() {
        String username = SecurityUtils.getUsername();
        if (username == null || username.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> response = remoteUserService.getUserDeptList(username, SecurityConstants.INNER);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .map(SysDept::getDeptId)
                    .filter(deptId -> deptId != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
