package com.junsong.finance.service.impl;


import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.*;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareDetailMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.service.IFinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceReportServiceImpl implements IFinanceReportService {

    @Autowired
    private FinExpenseMapper finExpenseMapper;

    @Autowired
    private FinProfitShareRecordMapper finProfitShareRecordMapper;

    @Autowired
    private FinProfitShareDetailMapper finProfitShareDetailMapper;

    @Autowired
    private FinSaleRecordMapper finSaleRecordMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public ExpenseReportVO getExpenseReport(ReportQueryParams params) {
        ExpenseReportVO vo = new ExpenseReportVO();

        Map<String, Object> queryParams = buildQueryParams(params);
        vo.setTotalExpense(finExpenseMapper.selectExpenseTotal(queryParams));
        vo.setCategoryStats(finExpenseMapper.selectExpenseCategoryStats(queryParams));
        vo.setTrendStats(finExpenseMapper.selectExpenseTrendStats(queryParams));
        vo.setDeptStats(finExpenseMapper.selectExpenseDeptStats(queryParams));

        return vo;
    }

    @Override
    public CostReportVO getCostReport(ReportQueryParams params) {
        CostReportVO vo = new CostReportVO();

        Map<String, Object> queryParams = buildQueryParams(params);
        vo.setTotalCost(finExpenseMapper.selectExpenseTotal(queryParams));
        vo.setCategoryStats(finExpenseMapper.selectExpenseCategoryStats(queryParams));
        vo.setTrendStats(buildCostTrendStats(finExpenseMapper.selectExpenseTrendStats(queryParams)));
        vo.setDeptStats(finExpenseMapper.selectExpenseDeptStats(queryParams));

        return vo;
    }

    @Override
    public ProfitShareReportVO getProfitShareReport(ReportQueryParams params) {
        ProfitShareReportVO vo = new ProfitShareReportVO();

        Map<String, Object> queryParams = buildQueryParams(params);

        vo.setTotalManagerProfit(finProfitShareRecordMapper.selectManagerProfitTotal(queryParams));
        vo.setTotalInvestorProfit(finProfitShareRecordMapper.selectInvestorProfitTotal(queryParams));
        vo.setTotalProfitShare(finProfitShareRecordMapper.selectProfitShareTotal(queryParams));
        vo.setManagerStats(finProfitShareRecordMapper.selectManagerProfitByDept(queryParams));
        vo.setInvestorStats(finProfitShareRecordMapper.selectInvestorProfitByDept(queryParams));
        vo.setTrendStats(finProfitShareRecordMapper.selectProfitShareTrend(queryParams));

        return vo;
    }

    @Override
    public SaleReportVO getSaleReport(ReportQueryParams params) {
        SaleReportVO vo = new SaleReportVO();

        applyDataScope(params);
        List<Map<String, Object>> trendStats = finSaleRecordMapper.selectSaleTrendStats(params.getDeptIds(), params.getStartTime(), params.getEndTime());
        BigDecimal totalSales = sumField(trendStats, "totalSales");
        vo.setTotalSales(totalSales);
        vo.setTotalCount(0);
        vo.setAvgPrice(BigDecimal.ZERO);
        vo.setDeptStats(buildDeptTotalStats(trendStats, "totalSales"));
        vo.setRankStats(buildDeptTotalStats(trendStats, "totalSales"));
        vo.setTrendStats(trendStats);

        return vo;
    }

    @Override
    public ProfitReportVO getProfitReport(ReportQueryParams params) {
        ProfitReportVO vo = new ProfitReportVO();

        applyDataScope(params);
        Map<String, Object> queryParams = buildQueryParams(params);
        List<Map<String, Object>> salesTrend = finSaleRecordMapper.selectSaleTrendStats(params.getDeptIds(), params.getStartTime(), params.getEndTime());
        List<Map<String, Object>> costTrend = buildCostTrendStats(finExpenseMapper.selectExpenseTrendStats(queryParams));
        List<Map<String, Object>> profitTrend = buildProfitTrendStats(salesTrend, costTrend);
        BigDecimal totalProfit = sumField(profitTrend, "profit");
        vo.setTotalProfit(totalProfit);
        vo.setProfitRate(BigDecimal.ZERO);
        vo.setRecoveryRate(BigDecimal.ZERO);
        vo.setDeptStats(buildDeptTotalStats(profitTrend, "profit"));
        vo.setRecoveryStats(buildDeptTotalStats(profitTrend, "profit"));
        vo.setTrendStats(profitTrend);

        return vo;
    }

    @Override
    public StockReportVO getStockReport(ReportQueryParams params) {
        StockReportVO vo = new StockReportVO();

        applyDataScope(params);
        vo.setTotalStock(0);
        vo.setTotalValue(BigDecimal.ZERO);
        vo.setLowStockCount(0);
        vo.setDeptStats(new ArrayList<>());
        vo.setCategoryStats(new ArrayList<>());

        return vo;
    }

    private List<Map<String, Object>> buildCostTrendStats(List<Map<String, Object>> expenseTrendStats) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : expenseTrendStats) {
            Map<String, Object> row = new HashMap<>();
            row.put("dateStr", item.get("dateStr"));
            row.put("deptId", item.get("deptId"));
            row.put("deptName", item.get("deptName"));
            row.put("totalAmount", toBigDecimal(item.get("expenseAmount")));
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> buildProfitTrendStats(List<Map<String, Object>> salesTrendStats, List<Map<String, Object>> costTrendStats) {
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        for (Map<String, Object> item : salesTrendStats) {
            String key = buildTrendKey(item);
            Map<String, Object> row = resultMap.computeIfAbsent(key, k -> buildTrendRow(item));
            row.put("profit", toBigDecimal(row.get("profit")).add(toBigDecimal(item.get("totalSales"))));
        }
        for (Map<String, Object> item : costTrendStats) {
            String key = buildTrendKey(item);
            Map<String, Object> row = resultMap.computeIfAbsent(key, k -> buildTrendRow(item));
            row.put("profit", toBigDecimal(row.get("profit")).subtract(toBigDecimal(item.get("totalAmount"))));
        }
        return resultMap.values().stream()
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("dateStr")) + String.valueOf(item.get("deptId"))))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildTrendRow(Map<String, Object> source) {
        Map<String, Object> row = new HashMap<>();
        row.put("dateStr", source.get("dateStr"));
        row.put("deptId", source.get("deptId"));
        row.put("deptName", source.get("deptName"));
        row.put("profit", BigDecimal.ZERO);
        return row;
    }

    private String buildTrendKey(Map<String, Object> item) {
        return String.valueOf(item.get("dateStr")) + "_" + String.valueOf(item.get("deptId"));
    }

    private List<Map<String, Object>> buildDeptTotalStats(List<Map<String, Object>> rows, String fieldName) {
        Map<String, Map<String, Object>> resultMap = new LinkedHashMap<>();
        for (Map<String, Object> item : rows) {
            String deptId = String.valueOf(item.get("deptId"));
            Map<String, Object> row = resultMap.computeIfAbsent(deptId, key -> {
                Map<String, Object> value = new HashMap<>();
                value.put("deptId", item.get("deptId"));
                value.put("deptName", item.get("deptName"));
                value.put(fieldName, BigDecimal.ZERO);
                return value;
            });
            row.put(fieldName, toBigDecimal(row.get(fieldName)).add(toBigDecimal(item.get(fieldName))));
        }
        return new ArrayList<>(resultMap.values());
    }

    private BigDecimal sumField(List<Map<String, Object>> rows, String fieldName) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            total = total.add(toBigDecimal(row.get(fieldName)));
        }
        return total;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Map<String, Object> buildQueryParams(ReportQueryParams params) {
        applyDataScope(params);
        Map<String, Object> map = new HashMap<>();
        if (params.getDeptIds() != null && !params.getDeptIds().isEmpty()) {
            map.put("deptIds", params.getDeptIds());
        }
        if (params.getStartTime() != null) {
            map.put("startTime", params.getStartTime());
        }
        if (params.getEndTime() != null) {
            map.put("endTime", params.getEndTime());
        }
        return map;
    }

    private void applyDataScope(ReportQueryParams params) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            Long currentDeptId = SecurityUtils.getDeptId();
            allowed = currentDeptId != null ? Collections.singletonList(currentDeptId) : Collections.emptyList();
        }
        List<Long> requested = params.getDeptIds();
        if (requested == null || requested.isEmpty()) {
            params.setDeptIds(new ArrayList<>(allowed));
            return;
        }
        List<Long> finalAllowed = allowed;
        List<Long> filtered = requested.stream()
                .filter(finalAllowed::contains)
                .collect(Collectors.toList());
        params.setDeptIds(filtered.isEmpty() ? new ArrayList<>(allowed) : filtered);
    }

    private List<Long> loadAllowedDeptIds() {
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