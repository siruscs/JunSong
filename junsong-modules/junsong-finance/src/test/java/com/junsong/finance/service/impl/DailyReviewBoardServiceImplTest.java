package com.junsong.finance.service.impl;

import com.junsong.finance.domain.vo.DailyReviewBoardVO;
import com.junsong.finance.domain.vo.DailyReviewItemVO;
import com.junsong.finance.domain.vo.DailyReviewQueryParams;
import com.junsong.finance.domain.vo.WeeklyMemoVO;
import com.junsong.finance.domain.vo.WeeklyReviewBoardVO;
import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.HealthRuleThresholdSuggestionVO;
import com.junsong.finance.domain.vo.StoreHealthTrendRowVO;
import com.junsong.finance.domain.vo.StoreHealthTrendQueryParams;
import com.junsong.finance.domain.vo.StoreHealthTaskGenerateParams;
import com.junsong.finance.domain.vo.StoreOperationSummaryVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;
import com.junsong.finance.domain.FinanceReviewKnowledge;
import com.junsong.finance.mapper.DailyReviewBoardMapper;
import com.junsong.finance.mapper.FinanceReviewKnowledgeMapper;
import com.junsong.finance.service.IStoreFinanceReportService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DailyReviewBoardServiceImpl 单元测试。
 * R8-A/R8-F: 无 Spring 上下文，new ServiceImpl + fake mapper。
 *
 * 注意：授权门店过滤依赖 SecurityUtils.isAdmin()，测试中无安全上下文时
 * isAdmin() 返回 false，会走非 admin 路径，remoteUserService 为空时返回哨兵 [-1L]。
 * 用子类覆写 resolveAuthorizedDeptIds 来模拟 admin 场景。
 */
class DailyReviewBoardServiceImplTest {

    // ===== 每日复盘测试 =====

    @Test
    void todayBoardUsesRealBusinessDateFields() throws Exception {
        // 验证：使用 sale_date/payment_date/expense_date（通过 fake mapper 返回非零值验证调用）
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.salesAmount = new BigDecimal("10000");
        mapper.cashInAmount = new BigDecimal("8000");
        mapper.expenseAmount = new BigDecimal("3000");

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        params.setReviewDate("2026-07-01");

        DailyReviewBoardVO vo = service.getDailyReviewBoard(params);

        assertEquals(new BigDecimal("10000"), vo.getSalesAmount(), "销售额应来自 fin_sale_record.sale_date");
        assertEquals(new BigDecimal("8000"), vo.getCashInAmount(), "实收现金应来自 fin_sale_payment.payment_date");
        assertEquals(new BigDecimal("3000"), vo.getExpenseAmount(), "费用应来自 fin_expense.expense_date");
        assertEquals(new BigDecimal("5000"), vo.getNetCashflowAmount(), "净现金流 = 实收 - 费用");
    }

    @Test
    void zeroDataReturnsZeroAmountsAndHealthyEmptyItems() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.salesAmount = BigDecimal.ZERO;
        mapper.cashInAmount = BigDecimal.ZERO;
        mapper.expenseAmount = BigDecimal.ZERO;
        mapper.pendingTaskCount = 0;
        mapper.highPriorityTaskCount = 0;
        mapper.highPriorityTasks = Collections.emptyList();

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        DailyReviewBoardVO vo = service.getDailyReviewBoard(params);

        assertEquals(BigDecimal.ZERO, vo.getSalesAmount());
        assertEquals(BigDecimal.ZERO, vo.getCashInAmount());
        assertEquals(BigDecimal.ZERO, vo.getExpenseAmount());
        assertEquals(BigDecimal.ZERO, vo.getNetCashflowAmount());
        assertEquals(0, vo.getPendingTaskCount());
        assertEquals(0, vo.getHighPriorityTaskCount());
        assertTrue(vo.getFocusItems().isEmpty(), "无数据时关注项应为空");
        assertFalse(vo.getSuggestions().isEmpty(), "应提供默认建议");
    }

    @Test
    void pendingHighReviewTaskAppearsInFocusItems() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.highPriorityTaskCount = 1;

        Map<String, Object> taskRow = new HashMap<>();
        taskRow.put("title", "销售额下滑");
        taskRow.put("reason", "近7天销售额下降25%");
        taskRow.put("suggestion", "请核查门店促销策略");
        taskRow.put("targetRoute", "/finance/salesOperation");
        taskRow.put("impactAmount", new BigDecimal("5000"));
        mapper.highPriorityTasks = Collections.singletonList(taskRow);

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        DailyReviewBoardVO vo = service.getDailyReviewBoard(params);

        assertEquals(1, vo.getHighPriorityTaskCount());
        assertEquals(1, vo.getFocusItems().size());
        DailyReviewItemVO item = vo.getFocusItems().get(0);
        assertEquals("HIGH_TASK", item.getItemType());
        assertEquals("销售额下滑", item.getTitle());
        assertEquals("/finance/salesOperation", item.getTargetRoute());
    }

    @Test
    void suggestionsMentionCashflowNegativeWhenNetNegative() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.cashInAmount = new BigDecimal("2000");
        mapper.expenseAmount = new BigDecimal("5000");

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        DailyReviewBoardVO vo = service.getDailyReviewBoard(params);

        assertTrue(vo.getNetCashflowAmount().compareTo(BigDecimal.ZERO) < 0, "净现金流应为负");
        boolean hasNegativeSuggestion = vo.getSuggestions().stream()
                .anyMatch(s -> s.contains("净现金流为负") || s.contains("现金流"));
        assertTrue(hasNegativeSuggestion, "建议中应提及净现金流为负");
    }

    @Test
    void tableMissingReturnsZeroWithoutError() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.tableExists = false;

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        DailyReviewBoardVO vo = service.getDailyReviewBoard(params);

        assertEquals(BigDecimal.ZERO, vo.getSalesAmount());
        assertEquals(0, vo.getPendingTaskCount());
        assertFalse(vo.getSuggestions().isEmpty());
        assertTrue(vo.getSuggestions().get(0).contains("表未创建"));
    }

    // ===== 周复盘测试 (R8-F) =====

    @Test
    void weeklyReviewReturnsWeekStartAndWeekEnd() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.salesAmount = new BigDecimal("50000");

        // 2026-07-01 是周三，应归一到 2026-06-29(周一)~2026-07-05(周日)
        DailyReviewQueryParams params = new DailyReviewQueryParams();
        params.setReviewDate("2026-07-01");

        WeeklyReviewBoardVO vo = service.getWeeklyReviewBoard(params);

        assertEquals("2026-06-29", vo.getWeekStart(), "周开始应为周一 2026-06-29");
        assertEquals("2026-07-05", vo.getWeekEnd(), "周结束应为周日 2026-07-05");
        assertEquals(new BigDecimal("50000"), vo.getSalesAmount(), "周复盘销售额应正确查询");
    }

    @Test
    void weeklyReviewCalculatesNetCashflow() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.cashInAmount = new BigDecimal("40000");
        mapper.expenseAmount = new BigDecimal("15000");

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        WeeklyReviewBoardVO vo = service.getWeeklyReviewBoard(params);

        assertEquals(new BigDecimal("25000"), vo.getNetCashflowAmount(), "周净现金流 = 实收 - 费用");
    }

    @Test
    void weeklyReviewCalculatesChangeRates() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");

        // 本周 sales=12000，上周 sales=10000 -> salesChangeRate = +20.0%
        // FakeMapper 对本周和上周返回相同值，所以这里测试环比计算逻辑
        // 本周: sales=12000, expense=3000, cashIn=12000 -> netCashflow=9000
        // 上周: sales=12000(同值), expense=3000(同值), cashIn=12000(同值) -> changeRate=0
        mapper.salesAmount = new BigDecimal("12000");
        mapper.expenseAmount = new BigDecimal("3000");
        mapper.cashInAmount = new BigDecimal("12000");

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        WeeklyReviewBoardVO vo = service.getWeeklyReviewBoard(params);

        // 上周数据与本周相同（FakeMapper 返回固定值），环比应为 0
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getSalesChangeRate()),
                "上周和本周相同，环比应为 0");
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getExpenseChangeRate()),
                "费用环比应为 0");
    }

    @Test
    void weeklyReviewComputesCompletedTaskCount() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.completedTaskCount = 5;
        mapper.pendingTaskCount = 3;

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        WeeklyReviewBoardVO vo = service.getWeeklyReviewBoard(params);

        assertEquals(5, vo.getCompletedTaskCount(), "应返回本周已完成任务数");
        assertEquals(3, vo.getPendingTaskCount(), "应返回待处理任务数");
    }

    @Test
    void weeklyReviewGeneratesSummaryAndNextFocus() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        mapper.salesAmount = new BigDecimal("80000");
        mapper.cashInAmount = new BigDecimal("60000");
        mapper.expenseAmount = new BigDecimal("2000");
        mapper.completedTaskCount = 4;

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        WeeklyReviewBoardVO vo = service.getWeeklyReviewBoard(params);

        assertNotNull(vo.getWeeklySummary(), "周总结不应为 null");
        assertTrue(vo.getWeeklySummary().contains("80000"), "周总结应包含销售额: " + vo.getWeeklySummary());
        assertTrue(vo.getWeeklySummary().contains("4"), "周总结应包含完成任务数: " + vo.getWeeklySummary());

        assertNotNull(vo.getNextWeekFocus(), "下周重点不应为 null");
        assertFalse(vo.getNextWeekFocus().isEmpty(), "下周重点不应为空");
    }

    @Test
    void weeklyReviewNegativeCashflowMentionedInNextFocus() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");
        // 净现金流为负：cashIn=2000, expense=5000 -> netCashflow=-3000
        mapper.cashInAmount = new BigDecimal("2000");
        mapper.expenseAmount = new BigDecimal("5000");

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        WeeklyReviewBoardVO vo = service.getWeeklyReviewBoard(params);

        assertTrue(vo.getNetCashflowAmount().compareTo(BigDecimal.ZERO) < 0, "净现金流应为负");
        assertTrue(vo.getNextWeekFocus().contains("净现金流为负"),
                "下周重点应提及净现金流为负: " + vo.getNextWeekFocus());
    }

    @Test
    void reviewDateEmptyDefaultsToToday() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        params.setReviewDate(null); // 空 = 今天
        DailyReviewBoardVO vo = service.getDailyReviewBoard(params);

        assertNotNull(vo.getReviewDate());
        // 格式应为 yyyy-MM-dd
        assertTrue(vo.getReviewDate().matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void deptIdPassedToMapperWhenAdmin() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithAdmin();
        FakeMapper mapper = (FakeMapper) getField(service, "dailyReviewBoardMapper");

        DailyReviewQueryParams params = new DailyReviewQueryParams();
        params.setDeptId(100L);

        service.getDailyReviewBoard(params);

        // admin 场景下 deptId=100 应传给 mapper
        assertNotNull(mapper.lastDeptIds);
        assertTrue(mapper.lastDeptIds.contains(100L), "admin 传 deptId=100 应传给 mapper");
    }

    // ===== R11-G: 周经营纪要增强 测试 =====

    @Test
    void weeklyMemo_includesRiskWatchGoodCounts() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithStoreAndKnowledge(
                Arrays.asList(storeRow(100L, "A店", 60, "RISK"), storeRow(200L, "B店", 75, "WATCH"), storeRow(300L, "C店", 90, "GOOD")),
                Collections.emptyList());

        WeeklyMemoVO memo = service.getWeeklyMemo(new DailyReviewQueryParams());

        assertEquals(1, memo.getRiskStoreCount());
        assertEquals(1, memo.getWatchStoreCount());
        assertEquals(1, memo.getGoodStoreCount());
    }

    @Test
    void riskStore_createsHealthHighlight() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithStoreAndKnowledge(
                Arrays.asList(storeRow(100L, "旗舰店", 55, "RISK")),
                Collections.emptyList());

        WeeklyMemoVO memo = service.getWeeklyMemo(new DailyReviewQueryParams());

        assertFalse(memo.getStoreHealthHighlights().isEmpty());
        assertTrue(memo.getStoreHealthHighlights().get(0).contains("旗舰店"));
        assertTrue(memo.getStoreHealthHighlights().get(0).contains("风险"));
    }

    @Test
    void reusableKnowledge_appearsInHints() throws Exception {
        FinanceReviewKnowledge k = new FinanceReviewKnowledge();
        k.setTitle("销售下滑处理经验");
        k.setActionTaken("增加促销活动");
        k.setProblemType("SALES_DROP");

        DailyReviewBoardServiceImpl service = createServiceWithStoreAndKnowledge(
                Collections.emptyList(),
                Collections.singletonList(k));

        WeeklyMemoVO memo = service.getWeeklyMemo(new DailyReviewQueryParams());

        assertFalse(memo.getReusableKnowledgeHints().isEmpty());
        assertTrue(memo.getReusableKnowledgeHints().get(0).contains("销售下滑处理经验"));
    }

    @Test
    void noKnowledge_returnsEmptyHints() throws Exception {
        DailyReviewBoardServiceImpl service = createServiceWithStoreAndKnowledge(
                Collections.emptyList(), Collections.emptyList());

        WeeklyMemoVO memo = service.getWeeklyMemo(new DailyReviewQueryParams());

        assertTrue(memo.getReusableKnowledgeHints().isEmpty());
    }

    private AuthorizedStoreRowVO storeRow(Long deptId, String name, int healthScore, String level) {
        AuthorizedStoreRowVO row = new AuthorizedStoreRowVO();
        row.setDeptId(deptId);
        row.setDeptName(name);
        row.setHealthScore(healthScore);
        row.setHealthLevel(level);
        return row;
    }

    private DailyReviewBoardServiceImpl createServiceWithStoreAndKnowledge(
            List<AuthorizedStoreRowVO> stores, List<FinanceReviewKnowledge> knowledges) throws Exception {
        DailyReviewBoardServiceImpl service = new DailyReviewBoardServiceImpl() {
            @Override
            protected List<Long> resolveAuthorizedDeptIds(DailyReviewQueryParams params) {
                return null;
            }
        };
        setField(service, "dailyReviewBoardMapper", new FakeMapper());

        FakeStoreReportService fakeStoreService = new FakeStoreReportService(stores);
        setField(service, "storeReportService", fakeStoreService);

        FakeKnowledgeMapper fakeKnowledgeMapper = new FakeKnowledgeMapper(knowledges);
        setField(service, "knowledgeMapper", fakeKnowledgeMapper);

        return service;
    }

    // ===== 辅助方法 =====

    /**
     * 创建一个 admin 场景的 Service（覆写授权门店解析，返回 null = 不过滤）
     */
    private DailyReviewBoardServiceImpl createServiceWithAdmin() throws Exception {
        FakeMapper mapper = new FakeMapper();
        DailyReviewBoardServiceImpl service = new DailyReviewBoardServiceImpl() {
            @Override
            protected List<Long> resolveAuthorizedDeptIds(DailyReviewQueryParams params) {
                if (params.getDeptId() != null) {
                    return Collections.singletonList(params.getDeptId());
                }
                List<Long> requested = params.getDeptIds();
                return (requested != null && !requested.isEmpty()) ? requested : null;
            }
        };
        setField(service, "dailyReviewBoardMapper", mapper);
        return service;
    }

    private static Object getField(Object target, String name) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " not found in " + target.getClass().getName());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(name + " not found in " + target.getClass().getName());
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    // ===== Fake Mapper =====

    static class FakeMapper implements DailyReviewBoardMapper {
        BigDecimal salesAmount = BigDecimal.ZERO;
        BigDecimal cashInAmount = BigDecimal.ZERO;
        BigDecimal expenseAmount = BigDecimal.ZERO;
        int pendingTaskCount = 0;
        int highPriorityTaskCount = 0;
        int completedTaskCount = 0;
        List<Map<String, Object>> highPriorityTasks = new ArrayList<>();
        boolean tableExists = true;

        // 记录最后一次调用的参数
        List<Long> lastDeptIds = null;
        Date lastStartDate = null;
        Date lastEndDate = null;

        @Override
        public BigDecimal selectSalesAmount(List<Long> deptIds, Date startDate, Date endDate) {
            lastDeptIds = deptIds;
            lastStartDate = startDate;
            lastEndDate = endDate;
            return salesAmount;
        }

        @Override
        public BigDecimal selectCashInAmount(List<Long> deptIds, Date startDate, Date endDate) {
            lastDeptIds = deptIds;
            return cashInAmount;
        }

        @Override
        public BigDecimal selectExpenseAmount(List<Long> deptIds, Date startDate, Date endDate) {
            lastDeptIds = deptIds;
            return expenseAmount;
        }

        @Override
        public int selectPendingTaskCount(List<Long> deptIds) {
            lastDeptIds = deptIds;
            return pendingTaskCount;
        }

        @Override
        public int selectHighPriorityTaskCount(List<Long> deptIds) {
            lastDeptIds = deptIds;
            return highPriorityTaskCount;
        }

        @Override
        public int selectCompletedTaskCount(List<Long> deptIds, Date startDate, Date endDate) {
            lastDeptIds = deptIds;
            lastStartDate = startDate;
            lastEndDate = endDate;
            return completedTaskCount;
        }

        @Override
        public List<Map<String, Object>> selectHighPriorityTasks(List<Long> deptIds, int limit) {
            lastDeptIds = deptIds;
            return highPriorityTasks;
        }

        @Override
        public String selectDeptName(Long deptId) {
            return "测试门店";
        }

        @Override
        public int checkTableExists(String tableName) {
            return tableExists ? 1 : 0;
        }

        @Override
        public List<String> selectDoneTaskNotes(List<Long> deptIds, Date startDate, Date endDate) {
            return new ArrayList<>();
        }

        @Override
        public List<String> selectUnresolvedHighRiskTasks(List<Long> deptIds) {
            return new ArrayList<>();
        }
    }

    // ===== R11-G Fakes =====

    static class FakeStoreReportService implements IStoreFinanceReportService {
        final List<AuthorizedStoreRowVO> stores;

        FakeStoreReportService(List<AuthorizedStoreRowVO> stores) {
            this.stores = stores;
        }

        @Override
        public StoreOperationSummaryVO getSummary(StoreReportQueryParams params) {
            return null;
        }

        @Override
        public AuthorizedStorePortfolioVO getAuthorizedPortfolio(AuthorizedStoreReportQueryParams params) {
            AuthorizedStorePortfolioVO portfolio = new AuthorizedStorePortfolioVO();
            portfolio.setStores(stores);
            return portfolio;
        }

        @Override
        public List<StoreHealthTrendRowVO> getAuthorizedHealthTrend(StoreHealthTrendQueryParams params) {
            return Collections.emptyList();
        }

        @Override
        public Map<String, Integer> generateHealthReviewTasks(StoreHealthTaskGenerateParams params) {
            return Collections.emptyMap();
        }

        @Override
        public List<HealthRuleThresholdSuggestionVO> getHealthRuleThresholdSuggestions(Integer days) {
            return Collections.emptyList();
        }
    }

    static class FakeKnowledgeMapper implements FinanceReviewKnowledgeMapper {
        final List<FinanceReviewKnowledge> knowledges;

        FakeKnowledgeMapper(List<FinanceReviewKnowledge> knowledges) {
            this.knowledges = knowledges;
        }

        @Override
        public List<FinanceReviewKnowledge> selectKnowledgeList(Map<String, Object> params) {
            return knowledges;
        }

        @Override
        public FinanceReviewKnowledge selectByKnowledgeId(Long knowledgeId) {
            return knowledges.isEmpty() ? null : knowledges.get(0);
        }

        @Override
        public FinanceReviewKnowledge selectReusableByTaskId(Long taskId) {
            return null;
        }

        @Override
        public int insertKnowledge(FinanceReviewKnowledge knowledge) {
            return 1;
        }

        @Override
        public int updateKnowledge(FinanceReviewKnowledge knowledge) {
            return 1;
        }

        @Override
        public List<FinanceReviewKnowledge> selectRecentReusable(List<String> problemTypes, int limit) {
            return knowledges;
        }

        @Override
        public List<FinanceReviewKnowledge> selectRecommendations(String problemType, Long deptId, List<String> keywords, List<Long> allowedDeptIds, Integer limit) {
            return Collections.emptyList();
        }
    }
}
