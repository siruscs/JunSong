package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.mapper.FinanceReviewTaskMapper;
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

        task.setStatus(STATUS_IN_PROGRESS);
        task.setHandlerId(handlerId);
        task.setHandlerName(handlerName);
        task.setUpdateTime(new Date());
        reviewTaskMapper.updateReviewTask(task);
    }

    @Override
    public void markDone(Long taskId, Long handlerId, String handlerName, String handlerNote) {
        if (handlerNote == null || handlerNote.trim().isEmpty()) {
            throw new ServiceException("完成任务时处理备注不能为空");
        }
        verifyTaskAccess(taskId);
        FinanceReviewTask task = loadAndValidate(taskId);
        assertTransitionAllowed(task, STATUS_DONE);

        task.setStatus(STATUS_DONE);
        task.setHandlerId(handlerId);
        task.setHandlerName(handlerName);
        task.setHandlerNote(handlerNote);
        task.setUpdateTime(new Date());
        reviewTaskMapper.updateReviewTask(task);
    }

    @Override
    public void markIgnored(Long taskId, Long handlerId, String handlerName, String ignoreReason) {
        if (ignoreReason == null || ignoreReason.trim().isEmpty()) {
            throw new ServiceException("忽略任务时忽略原因不能为空");
        }
        verifyTaskAccess(taskId);
        FinanceReviewTask task = loadAndValidate(taskId);
        assertTransitionAllowed(task, STATUS_IGNORED);

        task.setStatus(STATUS_IGNORED);
        task.setHandlerId(handlerId);
        task.setHandlerName(handlerName);
        task.setIgnoreReason(ignoreReason);
        task.setUpdateTime(new Date());
        reviewTaskMapper.updateReviewTask(task);
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
