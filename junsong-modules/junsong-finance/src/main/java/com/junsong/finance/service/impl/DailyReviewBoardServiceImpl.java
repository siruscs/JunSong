package com.junsong.finance.service.impl;

import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.DailyReviewBoardVO;
import com.junsong.finance.domain.vo.DailyReviewItemVO;
import com.junsong.finance.domain.vo.DailyReviewQueryParams;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.ReviewQualityDashboardVO;
import com.junsong.finance.domain.vo.ReviewQualityQueryParams;
import com.junsong.finance.domain.vo.WeeklyMemoVO;
import com.junsong.finance.domain.vo.WeeklyReviewBoardVO;
import com.junsong.finance.domain.FinanceReviewKnowledge;
import com.junsong.finance.mapper.DailyReviewBoardMapper;
import com.junsong.finance.mapper.FinanceReviewKnowledgeMapper;
import com.junsong.finance.service.IDailyReviewBoardService;
import com.junsong.finance.service.IReviewQualityService;
import com.junsong.finance.service.IStoreFinanceReportService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每日/周经营复盘 Service 实现。
 * R8-A/R8-F: 基于 fin_sale_record / fin_sale_payment / fin_expense / finance_review_task 真实表。
 *
 * @author junsong
 */
@Service
public class DailyReviewBoardServiceImpl implements IDailyReviewBoardService {

    private static final Logger log = LoggerFactory.getLogger(DailyReviewBoardServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);

    @Autowired
    private DailyReviewBoardMapper dailyReviewBoardMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired(required = false)
    private IStoreFinanceReportService storeReportService;

    @Autowired(required = false)
    private FinanceReviewKnowledgeMapper knowledgeMapper;

    @Autowired(required = false)
    private IReviewQualityService reviewQualityService;

    @Override
    public DailyReviewBoardVO getDailyReviewBoard(DailyReviewQueryParams params) {
        DailyReviewBoardVO vo = new DailyReviewBoardVO();
        vo.setReviewDate(resolveReviewDate(params));
        vo.setDeptId(params.getDeptId());

        // 解析授权门店
        List<Long> deptIds = resolveAuthorizedDeptIds(params);
        vo.setDeptId(params.getDeptId());
        if (params.getDeptId() != null) {
            try {
                vo.setDeptName(dailyReviewBoardMapper.selectDeptName(params.getDeptId()));
            } catch (Exception ignored) {}
        }

        // 表存在性检查
        if (!tablesExist()) {
            vo.getSuggestions().add("部分财务表未创建，请先完成基础数据初始化");
            return vo;
        }

        // 解析日期范围（当天 00:00:00 - 23:59:59）
        LocalDate reviewLocalDate = LocalDate.parse(vo.getReviewDate(), DATE_FMT);
        Date startDate = toDate(reviewLocalDate.atStartOfDay(ZoneId.systemDefault()).toLocalDateTime());
        Date endDate = toDate(reviewLocalDate.atTime(23, 59, 59));

        try {
            BigDecimal sales = nullToZero(dailyReviewBoardMapper.selectSalesAmount(deptIds, startDate, endDate));
            vo.setSalesAmount(sales);
        } catch (Exception e) {
            log.warn("查询销售额失败: {}", e.getMessage());
        }

        try {
            BigDecimal cashIn = nullToZero(dailyReviewBoardMapper.selectCashInAmount(deptIds, startDate, endDate));
            vo.setCashInAmount(cashIn);
        } catch (Exception e) {
            log.warn("查询实收现金失败: {}", e.getMessage());
        }

        try {
            BigDecimal expense = nullToZero(dailyReviewBoardMapper.selectExpenseAmount(deptIds, startDate, endDate));
            vo.setExpenseAmount(expense);
        } catch (Exception e) {
            log.warn("查询费用支出失败: {}", e.getMessage());
        }

        // 净现金流
        vo.setNetCashflowAmount(vo.getCashInAmount().subtract(vo.getExpenseAmount()));

        // 待办任务数
        try {
            vo.setPendingTaskCount(dailyReviewBoardMapper.selectPendingTaskCount(deptIds));
        } catch (Exception e) {
            log.warn("查询待处理任务数失败: {}", e.getMessage());
        }

        // 高优先级任务数
        try {
            vo.setHighPriorityTaskCount(dailyReviewBoardMapper.selectHighPriorityTaskCount(deptIds));
        } catch (Exception e) {
            log.warn("查询高优先级任务数失败: {}", e.getMessage());
        }

        // 关注项 Top 3
        try {
            List<Map<String, Object>> rows = dailyReviewBoardMapper.selectHighPriorityTasks(deptIds, 3);
            List<DailyReviewItemVO> items = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                DailyReviewItemVO item = new DailyReviewItemVO();
                item.setItemType("HIGH_TASK");
                item.setTitle(asString(row.get("title")));
                item.setReason(asString(row.get("reason")));
                item.setSuggestion(asString(row.get("suggestion")));
                item.setTargetRoute(asString(row.get("targetRoute")));
                Object impactAmt = row.get("impactAmount");
                if (impactAmt != null) {
                    item.setImpactAmount(new BigDecimal(impactAmt.toString()));
                }
                items.add(item);
            }
            vo.setFocusItems(items);
        } catch (Exception e) {
            log.warn("查询关注项失败: {}", e.getMessage());
        }

        // 经营建议
        buildSuggestions(vo);

        return vo;
    }

    @Override
    public WeeklyReviewBoardVO getWeeklyReviewBoard(DailyReviewQueryParams params) {
        WeeklyReviewBoardVO vo = new WeeklyReviewBoardVO();
        String reviewDateStr = resolveReviewDate(params);
        vo.setDeptId(params.getDeptId());

        List<Long> deptIds = resolveAuthorizedDeptIds(params);
        if (params.getDeptId() != null) {
            try {
                vo.setDeptName(dailyReviewBoardMapper.selectDeptName(params.getDeptId()));
            } catch (Exception ignored) {}
        }

        if (!tablesExist()) {
            vo.setWeeklySummary("部分财务表未创建，请先完成基础数据初始化");
            vo.setNextWeekFocus("完成基础数据初始化后重新查看周复盘");
            return vo;
        }

        // 归一到所在自然周（周一到周日）
        LocalDate reviewLocalDate = LocalDate.parse(reviewDateStr, DATE_FMT);
        LocalDate weekStart = reviewLocalDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate prevWeekStart = weekStart.minusDays(7);
        LocalDate prevWeekEnd = weekEnd.minusDays(7);

        vo.setWeekStart(weekStart.format(DATE_FMT));
        vo.setWeekEnd(weekEnd.format(DATE_FMT));

        Date thisWeekStart = toDate(weekStart.atStartOfDay());
        Date thisWeekEnd = toDate(weekEnd.atTime(23, 59, 59));
        Date prevWeekStartD = toDate(prevWeekStart.atStartOfDay());
        Date prevWeekEndD = toDate(prevWeekEnd.atTime(23, 59, 59));

        // 本周数据
        try {
            vo.setSalesAmount(nullToZero(dailyReviewBoardMapper.selectSalesAmount(deptIds, thisWeekStart, thisWeekEnd)));
            vo.setCashInAmount(nullToZero(dailyReviewBoardMapper.selectCashInAmount(deptIds, thisWeekStart, thisWeekEnd)));
            vo.setExpenseAmount(nullToZero(dailyReviewBoardMapper.selectExpenseAmount(deptIds, thisWeekStart, thisWeekEnd)));
            vo.setNetCashflowAmount(vo.getCashInAmount().subtract(vo.getExpenseAmount()));
        } catch (Exception e) {
            log.warn("周复盘本周查询失败: {}", e.getMessage());
        }

        // 上周数据
        try {
            vo.setPreviousWeekSalesAmount(nullToZero(dailyReviewBoardMapper.selectSalesAmount(deptIds, prevWeekStartD, prevWeekEndD)));
            vo.setPreviousWeekExpenseAmount(nullToZero(dailyReviewBoardMapper.selectExpenseAmount(deptIds, prevWeekStartD, prevWeekEndD)));
            BigDecimal prevCashIn = nullToZero(dailyReviewBoardMapper.selectCashInAmount(deptIds, prevWeekStartD, prevWeekEndD));
            vo.setPreviousWeekNetCashflowAmount(prevCashIn.subtract(vo.getPreviousWeekExpenseAmount()));
        } catch (Exception e) {
            log.warn("周复盘上周查询失败: {}", e.getMessage());
        }

        // 环比变化率
        vo.setSalesChangeRate(computeChangeRate(vo.getSalesAmount(), vo.getPreviousWeekSalesAmount()));
        vo.setExpenseChangeRate(computeChangeRate(vo.getExpenseAmount(), vo.getPreviousWeekExpenseAmount()));
        vo.setCashflowChangeRate(computeChangeRate(vo.getNetCashflowAmount(), vo.getPreviousWeekNetCashflowAmount()));

        // 任务统计
        try {
            vo.setPendingTaskCount(dailyReviewBoardMapper.selectPendingTaskCount(deptIds));
            vo.setCompletedTaskCount(dailyReviewBoardMapper.selectCompletedTaskCount(deptIds, thisWeekStart, thisWeekEnd));
        } catch (Exception e) {
            log.warn("周复盘任务查询失败: {}", e.getMessage());
        }

        // 周总结和下周重点
        buildWeeklySummaryAndNextFocus(vo);
        return vo;
    }

    /**
     * R8-F: 计算环比变化率 = (本周 - 上周) / 上周 * 100，上周为0时返回0。
     */
    protected BigDecimal computeChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 1, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * R8-F: 生成周总结和下周重点。
     */
    private void buildWeeklySummaryAndNextFocus(WeeklyReviewBoardVO vo) {
        StringBuilder summary = new StringBuilder();
        summary.append("本周销售").append(vo.getSalesAmount()).append("元");
        if (vo.getSalesChangeRate().compareTo(BigDecimal.ZERO) != 0) {
            summary.append("（环比").append(vo.getSalesChangeRate().compareTo(BigDecimal.ZERO) > 0 ? "+" : "")
                    .append(vo.getSalesChangeRate()).append("%）");
        }
        summary.append("，净现金流").append(vo.getNetCashflowAmount()).append("元");
        summary.append("，完成任务").append(vo.getCompletedTaskCount()).append("个");
        if (vo.getPendingTaskCount() > 0) {
            summary.append("，仍有").append(vo.getPendingTaskCount()).append("个待处理");
        }
        vo.setWeeklySummary(summary.toString());

        List<String> focus = new ArrayList<>();
        if (vo.getNetCashflowAmount().compareTo(BigDecimal.ZERO) < 0) {
            focus.add("净现金流为负，重点核查费用支出与实收差异");
        }
        if (vo.getSalesChangeRate().compareTo(BigDecimal.ZERO) < 0) {
            focus.add("销售环比下滑，分析下滑原因并制定提升方案");
        }
        if (vo.getPendingTaskCount() > 0) {
            focus.add("清理" + vo.getPendingTaskCount() + "个待处理任务");
        }
        if (vo.getExpenseChangeRate().compareTo(BigDecimal.ZERO) > 0) {
            focus.add("费用环比上升，关注成本控制");
        }
        if (focus.isEmpty()) {
            focus.add("经营状态平稳，保持当前节奏并关注增长机会");
        }
        vo.setNextWeekFocus(String.join("；", focus));
    }

    // ===== 内部方法 =====

    private void buildSuggestions(DailyReviewBoardVO vo) {
        List<String> suggestions = vo.getSuggestions();
        if (vo.getNetCashflowAmount().compareTo(BigDecimal.ZERO) < 0) {
            suggestions.add("当日净现金流为负，建议核查费用支出与实收差异");
        }
        if (vo.getHighPriorityTaskCount() > 0) {
            suggestions.add("有 " + vo.getHighPriorityTaskCount() + " 个高优先级任务待处理，建议优先跟进");
        }
        if (vo.getPendingTaskCount() > 0 && vo.getHighPriorityTaskCount() == 0) {
            suggestions.add("有 " + vo.getPendingTaskCount() + " 个待处理任务，建议安排时间复盘");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("经营状态正常，建议保持每日复盘节奏");
        }
    }

    private boolean tablesExist() {
        String[] requiredTables = {"fin_sale_record", "fin_sale_payment", "fin_expense", "finance_review_task"};
        for (String table : requiredTables) {
            try {
                if (dailyReviewBoardMapper.checkTableExists(table) == 0) return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private String resolveReviewDate(DailyReviewQueryParams params) {
        if (params.getReviewDate() != null && !params.getReviewDate().isEmpty()) {
            return params.getReviewDate();
        }
        return LocalDate.now().format(DATE_FMT);
    }

    /**
     * 计算授权门店（admin 不过滤，非 admin 取交集，无授权返回哨兵）
     * 复用 CashflowDashboardController 的模式
     * protected 以便测试覆写
     */
    protected List<Long> resolveAuthorizedDeptIds(DailyReviewQueryParams params) {
        if (SecurityUtils.isAdmin()) {
            // admin：优先用 deptId，其次 deptIds，都为空则 null（不过滤）
            if (params.getDeptId() != null) {
                return Collections.singletonList(params.getDeptId());
            }
            List<Long> requested = params.getDeptIds();
            return (requested != null && !requested.isEmpty()) ? requested : null;
        }

        // 非 admin：加载授权门店
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            return SENTINEL_DEPT_IDS;
        }

        // 优先用 deptId，其次 deptIds
        if (params.getDeptId() != null) {
            if (allowed.contains(params.getDeptId())) {
                return Collections.singletonList(params.getDeptId());
            }
            return SENTINEL_DEPT_IDS; // 未授权门店
        }

        List<Long> requested = params.getDeptIds();
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>(allowed);
        }

        // 取交集
        List<Long> finalAllowed = allowed;
        List<Long> intersection = requested.stream()
                .filter(finalAllowed::contains)
                .collect(Collectors.toList());

        return intersection.isEmpty() ? SENTINEL_DEPT_IDS : intersection;
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
            log.warn("获取用户授权门店列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static Date toDate(java.time.LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static Date toDate(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // ==================== R10-F: 周经营纪要 ====================

    @Override
    public WeeklyMemoVO getWeeklyMemo(DailyReviewQueryParams params) {
        WeeklyMemoVO memo = new WeeklyMemoVO();

        // 先获取周复盘数据
        WeeklyReviewBoardVO weekly = getWeeklyReviewBoard(params);
        memo.setWeekStart(weekly.getWeekStart());
        memo.setWeekEnd(weekly.getWeekEnd());

        // headline
        memo.setHeadline(buildHeadline(weekly));

        // keyChanges from change rates
        List<String> keyChanges = new ArrayList<>();
        if (weekly.getSalesChangeRate().abs().compareTo(BigDecimal.ONE) > 0) {
            keyChanges.add("销售额环比 " + (weekly.getSalesChangeRate().compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + weekly.getSalesChangeRate() + "%");
        }
        if (weekly.getExpenseChangeRate().abs().compareTo(BigDecimal.ONE) > 0) {
            keyChanges.add("费用环比 " + (weekly.getExpenseChangeRate().compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + weekly.getExpenseChangeRate() + "%");
        }
        if (weekly.getCashflowChangeRate().abs().compareTo(BigDecimal.ONE) > 0) {
            keyChanges.add("净现金流环比 " + (weekly.getCashflowChangeRate().compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + weekly.getCashflowChangeRate() + "%");
        }
        memo.setKeyChanges(keyChanges);

        // completedActions from finance_review_task_log DONE this week
        List<String> completedActions = new ArrayList<>();
        try {
            List<Long> deptIds = resolveAuthorizedDeptIds(params);
            LocalDate weekStart = LocalDate.parse(weekly.getWeekStart(), DATE_FMT);
            LocalDate weekEnd = LocalDate.parse(weekly.getWeekEnd(), DATE_FMT);
            Date start = toDate(weekStart);
            Date end = toDate(weekEnd.atTime(23, 59, 59));
            List<String> notes = dailyReviewBoardMapper.selectDoneTaskNotes(deptIds, start, end);
            completedActions.addAll(notes != null ? notes.subList(0, Math.min(5, notes.size())) : Collections.emptyList());
        } catch (Exception e) {
            log.warn("获取已完成任务备注失败", e);
        }
        memo.setCompletedActions(completedActions);

        // unresolvedRisks from finance_review_task PENDING/IN_PROGRESS HIGH
        List<String> unresolvedRisks = new ArrayList<>();
        try {
            List<Long> deptIds = resolveAuthorizedDeptIds(params);
            List<String> risks = dailyReviewBoardMapper.selectUnresolvedHighRiskTasks(deptIds);
            unresolvedRisks.addAll(risks != null ? risks : Collections.emptyList());
        } catch (Exception e) {
            log.warn("获取未解决高风险任务失败", e);
        }
        memo.setUnresolvedRisks(unresolvedRisks);

        // nextWeekFocus = weekly nextWeekFocus + unresolvedRisks
        List<String> nextFocus = new ArrayList<>();
        if (weekly.getNextWeekFocus() != null && !weekly.getNextWeekFocus().isEmpty()) {
            nextFocus.add(weekly.getNextWeekFocus());
        }
        nextFocus.addAll(unresolvedRisks);
        memo.setNextWeekFocus(nextFocus);

        // R11-G: 门店健康分布
        fillStoreHealthDistribution(memo, params);

        // R11-G: 可复用知识提示
        fillReusableKnowledgeHints(memo, unresolvedRisks);

        // R10-FIX-F: 接入复盘质量分，与复盘质量看板口径一致
        fillReviewQualityScore(memo, params);

        return memo;
    }

    private String buildHeadline(WeeklyReviewBoardVO weekly) {
        StringBuilder sb = new StringBuilder();
        sb.append("本周销售 ").append(weekly.getSalesAmount().toPlainString()).append("元");
        if (weekly.getSalesChangeRate().abs().compareTo(BigDecimal.ONE) > 0) {
            sb.append("，环比").append(weekly.getSalesChangeRate().compareTo(BigDecimal.ZERO) > 0 ? "增长" : "下降")
              .append(" ").append(weekly.getSalesChangeRate().abs()).append("%");
        }
        sb.append("；完成 ").append(weekly.getCompletedTaskCount()).append(" 项任务");
        return sb.toString();
    }

    // ==================== R11-G: 门店健康分 + 知识提示 ====================

    private void fillStoreHealthDistribution(WeeklyMemoVO memo, DailyReviewQueryParams params) {
        if (storeReportService == null) return;
        try {
            AuthorizedStoreReportQueryParams query = new AuthorizedStoreReportQueryParams();
            AuthorizedStorePortfolioVO portfolio = storeReportService.getAuthorizedPortfolio(query);
            if (portfolio == null || portfolio.getStores() == null || portfolio.getStores().isEmpty()) return;

            int risk = 0, watch = 0, good = 0;
            List<String> highlights = new ArrayList<>();
            for (AuthorizedStoreRowVO store : portfolio.getStores()) {
                String level = store.getHealthLevel();
                if ("RISK".equals(level)) {
                    risk++;
                    if (highlights.size() < 5) {
                        highlights.add(store.getDeptName() + " 健康分 " + store.getHealthScore() + "（风险）");
                    }
                } else if ("WATCH".equals(level)) {
                    watch++;
                    if (highlights.size() < 5) {
                        highlights.add(store.getDeptName() + " 健康分 " + store.getHealthScore() + "（关注）");
                    }
                } else {
                    good++;
                }
            }
            memo.setRiskStoreCount(risk);
            memo.setWatchStoreCount(watch);
            memo.setGoodStoreCount(good);
            memo.setStoreHealthHighlights(highlights);
        } catch (Exception e) {
            log.warn("获取门店健康分布失败", e);
        }
    }

    private void fillReusableKnowledgeHints(WeeklyMemoVO memo, List<String> unresolvedRisks) {
        if (knowledgeMapper == null) return;
        try {
            // 用未解决风险的关键词作为 problemType 优先匹配
            List<String> problemTypes = new ArrayList<>();
            for (String risk : unresolvedRisks) {
                if (risk.contains("销售")) problemTypes.add("SALES_DROP");
                if (risk.contains("费用")) problemTypes.add("EXPENSE_SPIKE");
                if (risk.contains("利润")) problemTypes.add("PROFIT_RATE_DROP");
                if (risk.contains("核销")) problemTypes.add("PENDING_VERIFY");
                if (risk.contains("分润")) problemTypes.add("PROFIT_SHARE_EXCEPTION");
                if (risk.contains("会员")) problemTypes.add("MEMBER_CONTRIBUTION_DROP");
            }
            List<FinanceReviewKnowledge> knowledges = knowledgeMapper.selectRecentReusable(
                    problemTypes.stream().distinct().collect(Collectors.toList()), 5);
            List<String> hints = new ArrayList<>();
            for (FinanceReviewKnowledge k : knowledges) {
                hints.add(k.getTitle() + "：" + k.getActionTaken());
            }
            memo.setReusableKnowledgeHints(hints);
        } catch (Exception e) {
            log.warn("获取可复用知识提示失败", e);
        }
    }

    /**
     * R10-FIX-F: 接入复盘质量分，与复盘质量看板口径一致。
     * 调用 ReviewQualityService 获取本周复盘质量分，失败时保持默认 0。
     */
    private void fillReviewQualityScore(WeeklyMemoVO memo, DailyReviewQueryParams params) {
        if (reviewQualityService == null) return;
        try {
            ReviewQualityQueryParams qParams = new ReviewQualityQueryParams();
            qParams.setDeptId(params.getDeptId());
            qParams.setDeptIds(params.getDeptIds());
            // 按周纪要的起止日期过滤
            if (memo.getWeekStart() != null && !memo.getWeekStart().isEmpty()) {
                qParams.setStartDate(memo.getWeekStart());
            }
            if (memo.getWeekEnd() != null && !memo.getWeekEnd().isEmpty()) {
                qParams.setEndDate(memo.getWeekEnd());
            }
            ReviewQualityDashboardVO qualityVO = reviewQualityService.getDashboard(qParams);
            if (qualityVO != null && qualityVO.getQualityScore() != null) {
                memo.setReviewQualityScore(qualityVO.getQualityScore());
            }
        } catch (Exception e) {
            log.warn("获取复盘质量分失败", e);
        }
    }
}
