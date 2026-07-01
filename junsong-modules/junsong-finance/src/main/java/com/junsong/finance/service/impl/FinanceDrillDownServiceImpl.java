package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.DrillDownDetailVO;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.service.IFinanceDrillDownService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务钻取服务实现 - 从报表钻取到明细记录
 */
@Service
public class FinanceDrillDownServiceImpl implements IFinanceDrillDownService {

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private FinSaleRecordMapper finSaleRecordMapper;

    @Autowired
    private FinExpenseMapper finExpenseMapper;

    @Autowired
    private FinProfitShareRecordMapper finProfitShareRecordMapper;

    /**
     * 哨兵部门 ID：非 admin 且无任何授权部门时使用，
     * 保证 Mapper 的 IN (-1) 永远匹配不到真实数据，避免全量泄露。
     */
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);

    // ==================== 销售钻取 ====================

    @Override
    public DrillDownDetailVO getSalesDetail(ReportQueryParams params) {
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();

        // 趋势数据
        List<Map<String, Object>> trendStats = finSaleRecordMapper.selectSaleTrendStats(
                deptIds, params.getStartTime(), params.getEndTime());

        // 门店维度汇总
        List<Map<String, Object>> deptStats = finSaleRecordMapper.selectSalesByDept(
                deptIds, params.getStartTime(), params.getEndTime());

        // 销售笔数
        int totalCount = finSaleRecordMapper.countSaleRecords(
                deptIds, params.getStartTime(), params.getEndTime());

        // 构建合并后的明细记录列表
        List<Map<String, Object>> records = new ArrayList<>();

        // 先添加门店维度汇总数据
        if (deptStats != null) {
            for (Map<String, Object> row : deptStats) {
                Map<String, Object> record = new HashMap<>();
                record.put("recordType", "DEPT_SUMMARY");
                record.put("deptId", row.get("deptId"));
                record.put("deptName", row.get("deptName"));
                record.put("totalSales", toBigDecimal(row.get("totalSales")));
                record.put("orderCount", toInt(row.get("orderCount")));
                record.put("quantity", toInt(row.get("quantity")));
                records.add(record);
            }
        }

        // 再添加趋势数据
        if (trendStats != null) {
            for (Map<String, Object> row : trendStats) {
                Map<String, Object> record = new HashMap<>();
                record.put("recordType", "TREND");
                record.put("dateStr", row.get("dateStr"));
                record.put("deptId", row.get("deptId"));
                record.put("deptName", row.get("deptName"));
                record.put("totalSales", toBigDecimal(row.get("totalSales")));
                record.put("orderCount", toInt(row.get("orderCount")));
                records.add(record);
            }
        }

        DrillDownDetailVO vo = new DrillDownDetailVO();
        vo.setDrillType("SALES");
        vo.setTitle("销售订单明细钻取");
        vo.setTotalCount(totalCount);
        vo.setRecords(records);
        vo.setFilterSummary(buildFilterSummary(params));
        return vo;
    }

    // ==================== 费用钻取 ====================

    @Override
    public DrillDownDetailVO getExpensesDetail(ReportQueryParams params) {
        Map<String, Object> queryParams = buildQueryParams(params);
        List<Long> deptIds = params.getDeptIds();

        // 分类统计
        List<Map<String, Object>> categoryStats = finExpenseMapper.selectExpenseCategoryStats(queryParams);

        // 门店统计
        List<Map<String, Object>> deptStats = finExpenseMapper.selectExpenseDeptStats(queryParams);

        // 费用总额
        BigDecimal totalExpense = nullSafe(finExpenseMapper.selectExpenseTotal(queryParams));

        // 未核销费用数量
        int unverifiedCount = finExpenseMapper.countUnverifiedExpenses(deptIds);

        // 构建合并后的明细记录列表
        List<Map<String, Object>> records = new ArrayList<>();

        // 添加分类维度数据
        if (categoryStats != null) {
            for (Map<String, Object> row : categoryStats) {
                Map<String, Object> record = new HashMap<>();
                record.put("recordType", "CATEGORY");
                record.put("category", row.get("category"));
                record.put("categoryName", row.get("categoryName"));
                record.put("totalAmount", toBigDecimal(row.get("totalAmount")));
                record.put("count", toInt(row.get("count")));
                records.add(record);
            }
        }

        // 添加门店维度数据
        if (deptStats != null) {
            for (Map<String, Object> row : deptStats) {
                Map<String, Object> record = new HashMap<>();
                record.put("recordType", "DEPT_SUMMARY");
                record.put("deptId", row.get("deptId"));
                record.put("deptName", row.get("deptName"));
                record.put("totalAmount", toBigDecimal(row.get("totalAmount")));
                record.put("count", toInt(row.get("count")));
                records.add(record);
            }
        }

        DrillDownDetailVO vo = new DrillDownDetailVO();
        vo.setDrillType("EXPENSES");
        vo.setTitle("费用记录明细钻取");
        vo.setTotalCount(records.size());
        vo.setRecords(records);

        Map<String, Object> filterSummary = buildFilterSummary(params);
        filterSummary.put("totalExpense", totalExpense);
        filterSummary.put("unverifiedCount", unverifiedCount);
        vo.setFilterSummary(filterSummary);
        return vo;
    }

    // ==================== 分润钻取 ====================

    @Override
    public DrillDownDetailVO getProfitShareDetail(ReportQueryParams params) {
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();
        Map<String, Object> queryParams = buildQueryParams(params);

        // 结算数据（按门店）
        List<Map<String, Object>> settlementRows = finProfitShareRecordMapper.selectSettlementByDept(
                deptIds, params.getStartTime(), params.getEndTime());

        // 分润趋势
        List<Map<String, Object>> trendStats = finProfitShareRecordMapper.selectProfitShareTrend(queryParams);

        // 未结算记录数
        int unsettledCount = finProfitShareRecordMapper.countUnsettledRecords(deptIds);

        // 构建合并后的明细记录列表
        List<Map<String, Object>> records = new ArrayList<>();

        // 添加门店结算数据
        if (settlementRows != null) {
            for (Map<String, Object> row : settlementRows) {
                Map<String, Object> record = new HashMap<>();
                record.put("recordType", "SETTLEMENT");
                record.put("deptId", row.get("deptId"));
                record.put("deptName", row.get("deptName"));
                record.put("payableAmount", toBigDecimal(row.get("payableAmount")));
                record.put("paidAmount", toBigDecimal(row.get("paidAmount")));
                record.put("managerShare", toBigDecimal(row.get("managerShare")));
                record.put("investorShare", toBigDecimal(row.get("investorShare")));
                records.add(record);
            }
        }

        // 添加趋势数据
        if (trendStats != null) {
            for (Map<String, Object> row : trendStats) {
                Map<String, Object> record = new HashMap<>();
                record.put("recordType", "TREND");
                record.put("dateStr", row.get("dateStr"));
                record.put("deptId", row.get("deptId"));
                record.put("deptName", row.get("deptName"));
                record.put("profitShareTotal", toBigDecimal(row.get("profitShareTotal")));
                record.put("managerShare", toBigDecimal(row.get("managerShare")));
                record.put("investorShare", toBigDecimal(row.get("investorShare")));
                records.add(record);
            }
        }

        DrillDownDetailVO vo = new DrillDownDetailVO();
        vo.setDrillType("PROFIT_SHARE");
        vo.setTitle("分润结算明细钻取");
        vo.setTotalCount(records.size());
        vo.setRecords(records);

        Map<String, Object> filterSummary = buildFilterSummary(params);
        filterSummary.put("unsettledCount", unsettledCount);
        vo.setFilterSummary(filterSummary);
        return vo;
    }

    // ==================== 公共工具方法 ====================

    /**
     * 构建筛选条件摘要
     */
    private Map<String, Object> buildFilterSummary(ReportQueryParams params) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("deptCount", params.getDeptIds() != null ? params.getDeptIds().size() : 0);
        summary.put("startTime", params.getStartTime());
        summary.put("endTime", params.getEndTime());
        summary.put("timeType", params.getTimeType());
        return summary;
    }

    private Map<String, Object> buildQueryParams(ReportQueryParams params) {
        applyDataScope(params);
        Map<String, Object> map = new HashMap<>();
        // 始终传递 deptIds，确保 Mapper 能生成部门过滤条件（即使为空也会走 IN (-1) 逻辑）
        map.put("deptIds", params.getDeptIds());
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
            allowed = currentDeptId != null ? Collections.singletonList(currentDeptId) : SENTINEL_DEPT_IDS;
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

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        try { return Long.valueOf(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return 0; }
    }
}
