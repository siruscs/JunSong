package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.StoreExpenseCategoryVO;
import com.junsong.finance.domain.vo.StoreOperationSummaryVO;
import com.junsong.finance.domain.vo.StorePendingItemVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;
import com.junsong.finance.domain.vo.StoreReviewTaskVO;
import com.junsong.finance.domain.vo.StoreTrendRowVO;
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
            rows = Collections.emptyList();
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
        fillStoreDerivedMetrics(rows);
        AuthorizedStorePortfolioVO vo = buildPortfolio(selectedDeptIds, allowedDeptIds, rows);
        List<StoreReviewTaskVO> reviewTasks = buildStoreReviewTasks(rows);
        vo.setReviewTasks(reviewTasks);

        // 回填每个门店的复盘任务数
        Map<Long, Long> taskCountByDept = reviewTasks.stream()
                .collect(Collectors.groupingBy(StoreReviewTaskVO::getDeptId, Collectors.counting()));
        for (AuthorizedStoreRowVO row : rows) {
            row.setReviewTaskCount(taskCountByDept.getOrDefault(row.getDeptId(), 0L).intValue());
            row.setAlertCount(row.getReviewTaskCount()); // 预警与复盘任务同源
        }

        vo.setSuggestions(buildPortfolioSuggestions(vo));
        return vo;
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

            // 5 维度健康分（满分 100，每个维度独立扣分并记录原因）
            int score = 100;
            Map<String, Integer> breakdown = new LinkedHashMap<>();
            List<String> reasons = new ArrayList<>();

            // 维度 1：销售趋势（-20）— 销售额较上期下降 > 20%
            if (row.getSalesChangeRate() != null && row.getSalesChangeRate().compareTo(new BigDecimal("-20")) < 0) {
                score -= 20;
                breakdown.put("salesTrend", -20);
                reasons.add("SALES_DROP");
            } else {
                breakdown.put("salesTrend", 0);
            }

            // 维度 2：费用控制（-15）— 费用超过销售额的 70%
            if (row.getTotalSales().compareTo(BigDecimal.ZERO) > 0
                    && row.getTotalExpense().compareTo(row.getTotalSales().multiply(new BigDecimal("0.7"))) > 0) {
                score -= 15;
                breakdown.put("expenseControl", -15);
                reasons.add("EXPENSE_SPIKE");
            } else {
                breakdown.put("expenseControl", 0);
            }

            // 维度 3：利润率（-20）— 利润率低于 5% 或为负
            if (row.getTotalSales().compareTo(BigDecimal.ZERO) > 0
                    && profitRate.compareTo(new BigDecimal("5")) < 0) {
                score -= 20;
                breakdown.put("profitMargin", -20);
                reasons.add(profit.compareTo(BigDecimal.ZERO) < 0 ? "NEGATIVE_PROFIT" : "LOW_MARGIN");
            } else if (row.getTotalSales().compareTo(BigDecimal.ZERO) == 0) {
                score -= 30;
                breakdown.put("profitMargin", -30);
                reasons.add("NO_SALES");
            } else {
                breakdown.put("profitMargin", 0);
            }

            // 维度 4：核销及时性（-15）— 未核销金额超过 1000
            if (row.getUnverifiedAmount().compareTo(new BigDecimal("1000")) > 0) {
                score -= 15;
                breakdown.put("verificationTimeliness", -15);
                reasons.add("UNVERIFIED_HIGH");
            } else {
                breakdown.put("verificationTimeliness", 0);
            }

            // 维度 5：会员贡献（-10）— 会员销售占比低于 20%（有销售时）
            if (row.getTotalSales().compareTo(BigDecimal.ZERO) > 0
                    && memberRatio.compareTo(new BigDecimal("20")) < 0) {
                score -= 10;
                breakdown.put("memberContribution", -10);
                reasons.add("MEMBER_LOW");
            } else {
                breakdown.put("memberContribution", 0);
            }

            score = Math.max(0, score);
            row.setHealthScore(score);
            row.setHealthLevel(score >= 80 ? "GOOD" : (score >= 60 ? "WATCH" : "RISK"));
            row.setHealthBreakdown(breakdown);
            row.setReviewReasons(reasons);
        }
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
