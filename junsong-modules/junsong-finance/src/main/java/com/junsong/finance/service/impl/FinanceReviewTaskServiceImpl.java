package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.domain.vo.ReviewTaskEffectSummaryVO;
import com.junsong.finance.domain.vo.ReviewTaskEffectVO;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.domain.FinanceReviewTaskLog;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.mapper.FinanceReviewTaskMapper;
import com.junsong.finance.mapper.FinanceReviewTaskLogMapper;
import com.junsong.finance.service.IFinanceReviewTaskService;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisContext;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRule;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRuleEngine;
import com.junsong.finance.service.diagnosis.rules.*;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务复盘任务Service实现
 *
 * @author junsong
 */
@Service
public class FinanceReviewTaskServiceImpl implements IFinanceReviewTaskService {

    private static final Logger log = LoggerFactory.getLogger(FinanceReviewTaskServiceImpl.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_IGNORED = "IGNORED";

    @Autowired
    private FinanceReviewTaskMapper reviewTaskMapper;

    @Autowired
    private FinanceReviewTaskLogMapper reviewTaskLogMapper;

    @Autowired
    private FinSaleRecordMapper finSaleRecordMapper;

    @Autowired
    private FinExpenseMapper finExpenseMapper;

    @Autowired
    private FinProfitShareRecordMapper finProfitShareRecordMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    /**
     * Diagnosis rule engine — shared with FinanceReportServiceImpl pattern.
     */
    private final FinanceDiagnosisRuleEngine diagnosisEngine = createDefaultEngine();

    private static FinanceDiagnosisRuleEngine createDefaultEngine() {
        List<FinanceDiagnosisRule> rules = Arrays.asList(
                new SalesDropRule(),
                new ExpenseSpikeRule(),
                new ProfitRateLowRule(),
                new PendingVerifyHighRule(),
                new ProfitShareExceptionRule(),
                new MemberContributionDropRule()
        );
        return new FinanceDiagnosisRuleEngine(rules);
    }

    @Override
    public List<FinanceReviewTask> listTasks(Map<String, Object> params) {
        List<Long> requestedDeptIds = extractDeptIds(params);
        List<Long> resolvedDeptIds = resolveAuthorizedDeptIds(requestedDeptIds);

        // Replace deptId/deptIds in params with resolved authorized list
        params.remove("deptId");
        if (resolvedDeptIds != null && !resolvedDeptIds.isEmpty()) {
            params.put("deptIds", resolvedDeptIds);
        }

        return reviewTaskMapper.selectReviewTaskList(params);
    }

    @Override
    public FinanceReviewTask getTask(Long taskId) {
        return reviewTaskMapper.selectByTaskId(taskId);
    }

    @Override
    public int generateFromDiagnosis(List<Long> deptIds, ReportQueryParams params) {
        List<Long> authorizedDeptIds = resolveAuthorizedDeptIds(deptIds);
        FinanceDiagnosisContext ctx = buildDiagnosisContext(authorizedDeptIds, params);
        List<FinanceDiagnosisResult> results = diagnosisEngine.runAll(ctx);

        Date taskDate = new Date();
        String taskDateStr = new SimpleDateFormat("yyyyMMdd").format(taskDate);
        int insertedCount = 0;

        for (FinanceDiagnosisResult result : results) {
            Long deptId = result.getDeptId();
            String ruleId = result.getRuleId();

            // Build dedup key: FIN_REVIEW:{ruleId}:{deptId}:{yyyyMMdd}
            String alertId = "FIN_REVIEW:" + ruleId + ":" + deptId + ":" + taskDateStr;

            // Dedup check: skip if already exists for same alertId + taskDate
            String taskDateForQuery = new SimpleDateFormat("yyyy-MM-dd").format(taskDate);
            FinanceReviewTask existing = reviewTaskMapper.selectByAlertId(alertId, taskDateForQuery);
            if (existing != null) {
                log.debug("Review task already exists for alertId={}, skipping", alertId);
                continue;
            }

            FinanceReviewTask task = new FinanceReviewTask();
            task.setAlertId(alertId);
            task.setTaskType(ruleId);
            task.setDeptId(deptId);
            task.setDeptName(result.getDeptName());
            task.setTaskDate(taskDate);
            task.setStatus(STATUS_PENDING);
            task.setSeverity(result.getAlertLevel());
            task.setTitle(result.getTitle());
            task.setReason(result.getReason());
            task.setSuggestion(result.getSuggestedAction());
            task.setImpactAmount(result.getImpactAmount() != null ? result.getImpactAmount() : BigDecimal.ZERO);
            task.setTargetRoute(result.getTargetRoute());
            task.setCreateTime(new Date());

            reviewTaskMapper.insertReviewTask(task);
            insertedCount++;
        }

        log.info("Generated {} review tasks from diagnosis ({} results evaluated)", insertedCount, results.size());
        return insertedCount;
    }

    @Override
    public void markInProgress(Long taskId, Long handlerId, String handlerName) {
        verifyTaskAccess(taskId);
        FinanceReviewTask task = loadAndValidate(taskId);
        assertTransitionAllowed(task, STATUS_IN_PROGRESS);

        String beforeStatus = task.getStatus();
        task.setStatus(STATUS_IN_PROGRESS);
        task.setHandlerId(handlerId);
        task.setHandlerName(handlerName);
        task.setUpdateTime(new Date());
        reviewTaskMapper.updateReviewTask(task);
        insertTaskLog(task, "IN_PROGRESS", beforeStatus, null);
    }

    @Override
    public void markDone(Long taskId, Long handlerId, String handlerName, String handlerNote) {
        if (handlerNote == null || handlerNote.trim().isEmpty()) {
            throw new ServiceException("完成任务时处理备注不能为空");
        }
        verifyTaskAccess(taskId);
        FinanceReviewTask task = loadAndValidate(taskId);
        assertTransitionAllowed(task, STATUS_DONE);

        String beforeStatus = task.getStatus();
        task.setStatus(STATUS_DONE);
        task.setHandlerId(handlerId);
        task.setHandlerName(handlerName);
        task.setHandlerNote(handlerNote);
        task.setArchived("1");
        task.setArchiveTime(new Date());
        task.setUpdateTime(new Date());
        reviewTaskMapper.updateReviewTask(task);
        insertTaskLog(task, "DONE", beforeStatus, handlerNote);
    }

    @Override
    public void markIgnored(Long taskId, Long handlerId, String handlerName, String ignoreReason) {
        if (ignoreReason == null || ignoreReason.trim().isEmpty()) {
            throw new ServiceException("忽略任务时忽略原因不能为空");
        }
        verifyTaskAccess(taskId);
        FinanceReviewTask task = loadAndValidate(taskId);
        assertTransitionAllowed(task, STATUS_IGNORED);

        String beforeStatus = task.getStatus();
        task.setStatus(STATUS_IGNORED);
        task.setHandlerId(handlerId);
        task.setHandlerName(handlerName);
        task.setIgnoreReason(ignoreReason);
        task.setArchived("1");
        task.setArchiveTime(new Date());
        task.setUpdateTime(new Date());
        reviewTaskMapper.updateReviewTask(task);
        insertTaskLog(task, "IGNORED", beforeStatus, ignoreReason);
    }

    @Override
    public List<FinanceReviewTaskLog> getTaskLogs(Long taskId) {
        verifyTaskAccess(taskId);
        return reviewTaskLogMapper.selectLogsByTaskId(taskId);
    }

    @Override
    public int reopenTask(Long taskId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ServiceException("重开原因不能为空");
        }
        verifyTaskAccess(taskId);
        FinanceReviewTask task = loadAndValidate(taskId);
        String beforeStatus = task.getStatus();
        if (!STATUS_DONE.equals(beforeStatus) && !STATUS_IGNORED.equals(beforeStatus)) {
            throw new ServiceException("只有已完成或已忽略的任务可以重开");
        }
        task.setStatus(STATUS_IN_PROGRESS);
        task.setArchived("0");
        task.setArchiveTime(null);
        task.setReopenCount(task.getReopenCount() == null ? 1 : task.getReopenCount() + 1);
        task.setHandlerNote(reason);
        task.setUpdateTime(new Date());
        int rows = reviewTaskMapper.updateReviewTask(task);
        insertTaskLog(task, "REOPEN", beforeStatus, reason);
        return rows;
    }

    @Override
    public ReviewTaskEffectVO evaluateTaskEffect(Long taskId, Integer windowDays) {
        verifyTaskAccess(taskId);
        FinanceReviewTask task = loadAndValidate(taskId);
        if (!STATUS_DONE.equals(task.getStatus())) {
            throw new ServiceException("只有已完成的任务可以评估效果");
        }

        int days = (windowDays != null && windowDays > 0) ? windowDays : 7;
        Date doneTime = task.getArchiveTime() != null ? task.getArchiveTime() : task.getUpdateTime();
        if (doneTime == null) {
            doneTime = new Date();
        }

        // Before window: [doneTime - days, doneTime)
        Date beforeStart = addDays(doneTime, -days);
        Date beforeEnd = doneTime;

        // After window: [doneTime, doneTime + days)
        Date afterStart = doneTime;
        Date afterEnd = addDays(doneTime, days);

        ReviewTaskEffectVO vo = new ReviewTaskEffectVO();
        vo.setTaskId(taskId);
        vo.setDeptId(task.getDeptId());
        vo.setTaskTitle(task.getTitle());
        vo.setProblemType(task.getTaskType());
        vo.setWindowDays(days);

        // Query amounts
        Map<String, Object> beforeAmounts = reviewTaskMapper.selectTaskEffectAmountWindow(task.getDeptId(), beforeStart, beforeEnd);
        Map<String, Object> afterAmounts = reviewTaskMapper.selectTaskEffectAmountWindow(task.getDeptId(), afterStart, afterEnd);

        BigDecimal beforeSales = toDecimal(beforeAmounts, "salesAmount");
        BigDecimal afterSales = toDecimal(afterAmounts, "salesAmount");
        BigDecimal beforeExpense = toDecimal(beforeAmounts, "expenseAmount");
        BigDecimal afterExpense = toDecimal(afterAmounts, "expenseAmount");
        BigDecimal beforeProfit = beforeSales.subtract(beforeExpense);
        BigDecimal afterProfit = afterSales.subtract(afterExpense);

        vo.setBeforeSales(beforeSales);
        vo.setAfterSales(afterSales);
        vo.setBeforeExpense(beforeExpense);
        vo.setAfterExpense(afterExpense);
        vo.setBeforeProfit(beforeProfit);
        vo.setAfterProfit(afterProfit);

        // Change rates
        vo.setSalesChangeRate(changeRate(beforeSales, afterSales));
        vo.setExpenseChangeRate(changeRate(beforeExpense, afterExpense));
        vo.setProfitChangeRate(changeRate(beforeProfit, afterProfit));

        // Similar open tasks count
        int beforeSimilarOpen = reviewTaskMapper.countSimilarOpenTasks(task.getDeptId(), task.getTaskType(), beforeStart, beforeEnd);
        int afterSimilarOpen = reviewTaskMapper.countSimilarOpenTasks(task.getDeptId(), task.getTaskType(), afterStart, afterEnd);
        vo.setBeforeSimilarOpenCount(beforeSimilarOpen);
        vo.setAfterSimilarOpenCount(afterSimilarOpen);

        // Scoring
        int score = 0;
        List<String> evidence = new ArrayList<>();

        if (afterProfit.compareTo(beforeProfit) > 0) {
            score += 30;
            evidence.add("利润改善: " + beforeProfit + " → " + afterProfit);
        }
        if (afterSales.compareTo(beforeSales) > 0) {
            score += 20;
            evidence.add("销售增长: " + beforeSales + " → " + afterSales);
        }
        if (afterExpense.compareTo(beforeExpense) < 0) {
            score += 20;
            evidence.add("费用下降: " + beforeExpense + " → " + afterExpense);
        }
        if (afterSimilarOpen < beforeSimilarOpen) {
            score += 10;
            evidence.add("同类未完成任务减少: " + beforeSimilarOpen + " → " + afterSimilarOpen);
        }

        vo.setEffectScore(score);
        if (score >= 80) {
            vo.setEffectLevel("GOOD");
        } else if (score >= 50) {
            vo.setEffectLevel("WATCH");
        } else {
            vo.setEffectLevel("NO_IMPROVEMENT");
        }
        vo.setEvidence(evidence);

        return vo;
    }

    @Override
    public FinanceReviewTask createFromMemberAction(Map<String, Object> req) {
        // Validate required fields
        Object deptIdObj = req.get("deptId");
        if (deptIdObj == null) {
            throw new ServiceException("门店ID不能为空");
        }
        Long deptId = (deptIdObj instanceof Number) ? ((Number) deptIdObj).longValue() : Long.parseLong(deptIdObj.toString());

        String title = (String) req.get("title");
        if (title == null || title.trim().isEmpty()) {
            throw new ServiceException("任务标题不能为空");
        }

        // Verify dept access
        List<Long> authorized = resolveAuthorizedDeptIds(Collections.singletonList(deptId));
        if (!authorized.contains(deptId)) {
            throw new ServiceException("无权访问所选门店");
        }

        String actionType = (String) req.get("actionType");
        String problemType = (String) req.get("problemType");
        String reason = (String) req.get("reason");
        String sourceId = req.get("sourceId") != null ? req.get("sourceId").toString() : "unknown";

        BigDecimal impactAmount = BigDecimal.ZERO;
        if (req.get("impactAmount") != null) {
            Object amtObj = req.get("impactAmount");
            if (amtObj instanceof BigDecimal) {
                impactAmount = (BigDecimal) amtObj;
            } else if (amtObj instanceof Number) {
                impactAmount = new BigDecimal(amtObj.toString());
            }
        }

        // Build dedup key: MEMBER_ACTION:{sourceId}:{yyyyMMdd}
        Date taskDate = new Date();
        String taskDateStr = new SimpleDateFormat("yyyyMMdd").format(taskDate);
        String alertId = "MEMBER_ACTION:" + sourceId + ":" + taskDateStr;

        // Check if task already exists for same alertId + today's date
        String taskDateForQuery = new SimpleDateFormat("yyyy-MM-dd").format(taskDate);
        FinanceReviewTask existing = reviewTaskMapper.selectByAlertId(alertId, taskDateForQuery);
        if (existing != null) {
            log.debug("Review task already exists for alertId={}, returning existing", alertId);
            return existing;
        }

        // Create new task
        FinanceReviewTask task = new FinanceReviewTask();
        task.setAlertId(alertId);
        task.setTaskType(problemType != null ? problemType : actionType);
        task.setDeptId(deptId);
        task.setTaskDate(taskDate);
        task.setStatus(STATUS_PENDING);
        task.setSeverity("MEDIUM");
        task.setTitle(title);
        task.setReason(reason);
        task.setImpactAmount(impactAmount);
        task.setTargetRoute("/member/dashboard");
        task.setCreateTime(new Date());

        reviewTaskMapper.insertReviewTask(task);
        log.info("Created review task from member action: alertId={}, taskId={}", alertId, task.getTaskId());
        return task;
    }

    @Override
    public ReviewTaskEffectSummaryVO summarizeEffect(List<Long> deptIds, Integer windowDays) {
        List<Long> authorizedDeptIds = resolveAuthorizedDeptIds(deptIds);
        if (authorizedDeptIds != null && !authorizedDeptIds.isEmpty()) {
            deptIds = authorizedDeptIds;
        }

        int days = (windowDays != null && windowDays > 0) ? windowDays : 7;
        Date sinceDate = addDays(new Date(), -days * 2);

        List<FinanceReviewTask> doneTasks = reviewTaskMapper.selectRecentDoneTasks(deptIds, sinceDate, 50);

        int goodCount = 0, watchCount = 0, noImprovementCount = 0;
        int totalScore = 0;
        int evaluated = 0;

        for (FinanceReviewTask task : doneTasks) {
            try {
                ReviewTaskEffectVO effect = evaluateTaskEffect(task.getTaskId(), days);
                evaluated++;
                totalScore += effect.getEffectScore();
                switch (effect.getEffectLevel()) {
                    case "GOOD": goodCount++; break;
                    case "WATCH": watchCount++; break;
                    default: noImprovementCount++; break;
                }
            } catch (Exception e) {
                // skip tasks that can't be evaluated
            }
        }

        // Reopen candidates
        Date cutoff = addDays(new Date(), -days);
        List<FinanceReviewTask> candidates = reviewTaskMapper.selectReopenCandidates(deptIds, cutoff, 5);
        List<ReviewTaskEffectSummaryVO.ReopenCandidateVO> reopenList = new ArrayList<>();
        for (FinanceReviewTask c : candidates) {
            ReviewTaskEffectSummaryVO.ReopenCandidateVO rc = new ReviewTaskEffectSummaryVO.ReopenCandidateVO();
            rc.setTaskId(c.getTaskId());
            rc.setTitle(c.getTitle());
            rc.setTaskType(c.getTaskType());
            rc.setDeptName(c.getDeptName());
            rc.setArchiveTime(c.getArchiveTime());
            rc.setReopenCount(c.getReopenCount());
            reopenList.add(rc);
        }

        ReviewTaskEffectSummaryVO summary = new ReviewTaskEffectSummaryVO();
        summary.setEvaluatedTaskCount(evaluated);
        summary.setGoodEffectCount(goodCount);
        summary.setWatchEffectCount(watchCount);
        summary.setNoImprovementCount(noImprovementCount);
        summary.setAverageEffectScore(evaluated > 0 ? totalScore / evaluated : 0);
        summary.setReopenCandidates(reopenList);
        return summary;
    }

    /**
     * R13-E: 从逾期应收生成催收复盘任务。
     * 对同一 saleId，若已有 open 状态的 RECEIVABLE_COLLECTION 任务，则去重不重复创建。
     */
    @Override
    public int generateReceivableCollectionTasks(Long deptId, Integer minAgeDays, BigDecimal minUnpaidAmount) {
        if (deptId == null) {
            throw new ServiceException("门店ID不能为空");
        }
        int effectiveMinAgeDays = minAgeDays != null ? minAgeDays : 14;
        BigDecimal effectiveMinAmount = minUnpaidAmount != null ? minUnpaidAmount : new BigDecimal("500");

        // 查询该门店所有未缴清销售单
        FinSaleRecord query = new FinSaleRecord();
        query.setDeptId(deptId);
        List<FinSaleRecord> receivables = finSaleRecordMapper.selectReceivableList(query);

        String todayStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int created = 0;

        for (FinSaleRecord sale : receivables) {
            // 过滤：账龄和金额
            if (sale.getSaleDate() == null) continue;
            long ageDays = java.time.temporal.ChronoUnit.DAYS.between(
                    sale.getSaleDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                    java.time.LocalDate.now());
            BigDecimal unpaid = sale.getSaleAmount().subtract(
                    sale.getPaidAmount() != null ? sale.getPaidAmount() : BigDecimal.ZERO);
            if (ageDays < effectiveMinAgeDays || unpaid.compareTo(effectiveMinAmount) < 0) {
                continue;
            }

            // 去重：同一 saleId 同一天不重复创建
            String alertId = "RECEIVABLE_COLLECTION:" + sale.getSaleId() + ":" + todayStr;
            FinanceReviewTask existing = reviewTaskMapper.selectByAlertId(alertId, todayStr);
            if (existing != null) {
                continue;
            }

            FinanceReviewTask task = new FinanceReviewTask();
            task.setTaskType("RECEIVABLE_COLLECTION");
            task.setDeptId(deptId);
            task.setTaskDate(new Date());
            task.setStatus(STATUS_PENDING);
            task.setSeverity(unpaid.compareTo(new BigDecimal("2000")) >= 0 ? "HIGH" : "MEDIUM");
            task.setTitle("催收跟进：" + sale.getSaleNo());
            task.setReason("销售单 " + sale.getSaleNo() + " 账龄 " + ageDays + " 天，剩余应收 ¥" + unpaid);
            task.setSuggestion("联系客户确认付款计划，核实欠款原因");
            task.setImpactAmount(unpaid);
            task.setAlertId(alertId);
            task.setTargetRoute("/finance/sale?tab=receivable");
            task.setDelFlag("0");
            task.setArchived("0");
            task.setReopenCount(0);
            task.setCreateTime(new Date());

            reviewTaskMapper.insertReviewTask(task);
            created++;
        }
        return created;
    }

    private static BigDecimal toDecimal(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return BigDecimal.ZERO;
        Object val = map.get(key);
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        return BigDecimal.ZERO;
    }

    private static BigDecimal changeRate(BigDecimal before, BigDecimal after) {
        if (before.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return after.subtract(before)
                .multiply(new BigDecimal("100"))
                .divide(before, 2, java.math.RoundingMode.HALF_UP);
    }

    private static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    // ─── Task log helper ────────────────────────────────────────────────────

    /**
     * Insert an audit log entry for a task state transition.
     *
     * @param task         the task that was updated
     * @param actionType   the action performed (IN_PROGRESS, DONE, IGNORED)
     * @param beforeStatus the status before the transition
     * @param handlerNote  optional note (may be null)
     */
    private void insertTaskLog(FinanceReviewTask task, String actionType, String beforeStatus, String handlerNote) {
        try {
            FinanceReviewTaskLog taskLog = new FinanceReviewTaskLog();
            taskLog.setTaskId(task.getTaskId());
            taskLog.setDeptId(task.getDeptId());
            taskLog.setActionType(actionType);
            taskLog.setBeforeStatus(beforeStatus);
            taskLog.setAfterStatus(task.getStatus());
            taskLog.setHandlerId(task.getHandlerId());
            taskLog.setHandlerName(task.getHandlerName());
            taskLog.setHandlerNote(handlerNote);
            taskLog.setActionTime(new Date());
            reviewTaskLogMapper.insertFinanceReviewTaskLog(taskLog);
        } catch (Exception e) {
            log.warn("Failed to insert task log for taskId={}, actionType={}", task.getTaskId(), actionType, e);
        }
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    /**
     * Load the list of department IDs that the current user is authorized to access.
     * Admin users get an empty list (meaning unrestricted).
     * Non-admin users get their authorized dept list from RemoteUserService,
     * falling back to their current dept context, or sentinel -1L if none available.
     */
    private List<Long> loadAllowedDeptIds() {
        if (SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            return Collections.emptyList();
        }
        try {
            if (remoteUserService != null) {
                R<List<SysDept>> result = remoteUserService.getUserDeptList(
                        SecurityUtils.getUsername(), SecurityConstants.INNER);
                if (result != null && result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                    return result.getData().stream().map(SysDept::getDeptId).collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load user dept list from remote service, falling back to current dept", e);
        }
        Long currentDeptId = SecurityUtils.getDeptId();
        if (currentDeptId != null) {
            return Collections.singletonList(currentDeptId);
        }
        return Collections.singletonList(-1L);
    }

    /**
     * Resolve requested deptIds against the user's authorized dept list.
     * Admin users pass through all requested deptIds (or empty list if none requested).
     * Non-admin users get the intersection of requested and authorized depts.
     *
     * @throws ServiceException if the intersection is empty (user has no access to any requested dept)
     */
    private List<Long> resolveAuthorizedDeptIds(List<Long> requestedDeptIds) {
        if (SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            return requestedDeptIds == null ? Collections.emptyList() : requestedDeptIds;
        }
        List<Long> allowedDeptIds = loadAllowedDeptIds();
        if (allowedDeptIds.isEmpty()) {
            // Admin-like (empty allowed means unrestricted) — should not reach here for non-admin
            return requestedDeptIds == null ? Collections.emptyList() : requestedDeptIds;
        }
        if (requestedDeptIds == null || requestedDeptIds.isEmpty()) {
            return allowedDeptIds;
        }
        Set<Long> allowed = new HashSet<>(allowedDeptIds);
        List<Long> resolved = requestedDeptIds.stream()
                .filter(allowed::contains).distinct().collect(Collectors.toList());
        if (resolved.isEmpty()) {
            throw new ServiceException("无权访问所选门店");
        }
        return resolved;
    }

    /**
     * Verify that the current user has access to the given task's department.
     * Admin users always pass. Non-admin users must have the task's deptId in their authorized list.
     *
     * @throws ServiceException if the task doesn't exist or the user lacks authorization
     */
    private void verifyTaskAccess(Long taskId) {
        FinanceReviewTask task = reviewTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            List<Long> allowed = loadAllowedDeptIds();
            if (!allowed.contains(task.getDeptId())) {
                throw new ServiceException("无权操作此门店的复盘任务");
            }
        }
    }

    /**
     * Extract deptIds from the params map. Supports both "deptIds" (List) and "deptId" (single Long).
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractDeptIds(Map<String, Object> params) {
        Object deptIdsObj = params.get("deptIds");
        if (deptIdsObj instanceof List) {
            return (List<Long>) deptIdsObj;
        }
        Object deptIdObj = params.get("deptId");
        if (deptIdObj instanceof Long) {
            return Collections.singletonList((Long) deptIdObj);
        }
        if (deptIdObj instanceof Number) {
            return Collections.singletonList(((Number) deptIdObj).longValue());
        }
        return null;
    }

    /**
     * Load a task by ID and verify it exists.
     */
    private FinanceReviewTask loadAndValidate(Long taskId) {
        FinanceReviewTask task = reviewTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new ServiceException("复盘任务不存在，taskId=" + taskId);
        }
        return task;
    }

    /**
     * Validate that the state transition from current status to target status is allowed.
     *
     * Allowed transitions:
     *   PENDING     -> IN_PROGRESS, DONE, IGNORED
     *   IN_PROGRESS -> DONE, IGNORED
     *   DONE        -> (terminal, no transitions)
     *   IGNORED     -> (terminal, no transitions)
     */
    private void assertTransitionAllowed(FinanceReviewTask task, String targetStatus) {
        String currentStatus = task.getStatus();

        if (STATUS_DONE.equals(currentStatus)) {
            throw new ServiceException("已完成的任务不能再修改状态");
        }
        if (STATUS_IGNORED.equals(currentStatus)) {
            throw new ServiceException("已忽略的任务不能再修改状态");
        }

        // PENDING and IN_PROGRESS can transition to DONE or IGNORED
        // PENDING can also transition to IN_PROGRESS
        if (STATUS_PENDING.equals(currentStatus)) {
            // PENDING -> IN_PROGRESS, DONE, IGNORED are all valid
            return;
        }
        if (STATUS_IN_PROGRESS.equals(currentStatus)) {
            if (STATUS_IN_PROGRESS.equals(targetStatus)) {
                throw new ServiceException("任务已经是处理中状态");
            }
            // IN_PROGRESS -> DONE, IGNORED are valid
            return;
        }

        throw new ServiceException("不支持的状态转换: " + currentStatus + " -> " + targetStatus);
    }

    /**
     * Build a diagnosis context from mapper data, mirroring FinanceReportServiceImpl.
     */
    private FinanceDiagnosisContext buildDiagnosisContext(List<Long> deptIds, ReportQueryParams params) {
        FinanceDiagnosisContext ctx = new FinanceDiagnosisContext();

        // Sales metrics
        ctx.setMonthSales(nullSafe(finSaleRecordMapper.selectMonthTotalSales(deptIds)));
        ctx.setPrevMonthSales(nullSafe(finSaleRecordMapper.selectMonthTotalSalesForPrev(deptIds)));

        // Expense metrics
        ctx.setMonthExpense(nullSafe(finExpenseMapper.selectMonthTotalExpense(deptIds)));
        ctx.setPrevMonthExpense(nullSafe(finExpenseMapper.selectMonthTotalExpenseForPrev(deptIds)));

        // Derived profit metrics
        BigDecimal netProfit = ctx.getMonthSales().subtract(ctx.getMonthExpense());
        ctx.setNetProfit(netProfit);
        ctx.setProfitRate(ctx.getMonthSales().compareTo(BigDecimal.ZERO) > 0
                ? netProfit.multiply(new BigDecimal("100")).divide(ctx.getMonthSales(), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Verification metrics
        ctx.setUnverifiedExpenseCount(finExpenseMapper.countUnverifiedExpenses(deptIds));
        ctx.setUnverifiedExpenseAmount(nullSafe(finExpenseMapper.sumUnverifiedExpenseAmount(deptIds)));

        // Profit share metrics
        ctx.setUnsettledProfitShareCount(finProfitShareRecordMapper.countUnsettledRecords(deptIds));

        // Member metrics
        BigDecimal memberSales = nullSafe(finSaleRecordMapper.selectMemberSales(deptIds, params.getStartTime(), params.getEndTime()));
        ctx.setMemberSales(memberSales);
        if (ctx.getMonthSales().compareTo(BigDecimal.ZERO) > 0) {
            ctx.setMemberSalesRatio(memberSales.multiply(new BigDecimal("100"))
                    .divide(ctx.getMonthSales(), 2, java.math.RoundingMode.HALF_UP));
        }

        return ctx;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
