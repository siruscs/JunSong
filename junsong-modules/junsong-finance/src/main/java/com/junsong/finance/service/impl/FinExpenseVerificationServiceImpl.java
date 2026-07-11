package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAdvance;
import com.junsong.finance.domain.FinAdvanceVerifyDetail;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinExpenseVerifyBatch;
import com.junsong.finance.domain.FinExpenseVerifyDetail;
import com.junsong.finance.domain.vo.ExpenseOperationCapabilityVO;
import com.junsong.finance.domain.vo.ExpenseUnverifyVO;
import com.junsong.finance.domain.vo.ExpenseVerifyVO;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.mapper.FinAdvanceMapper;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinExpenseVerifyBatchMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinExpenseVerificationService;

@Service
public class FinExpenseVerificationServiceImpl implements IFinExpenseVerificationService
{
    private static final Logger log = LoggerFactory.getLogger(FinExpenseVerificationServiceImpl.class);
    private final FinExpenseMapper expenseMapper;
    private final FinAdvanceMapper advanceMapper;
    private final FinExpenseVerifyBatchMapper batchMapper;
    private final IFinAccountingPeriodService periodService;
    private final FinAccountingPeriodMapper periodMapper;
    private final FinAuditTrailRecorder auditRecorder;
    private final Supplier<Long> deptResolver;

    public FinExpenseVerificationServiceImpl(FinExpenseMapper expenseMapper, FinAdvanceMapper advanceMapper,
        FinExpenseVerifyBatchMapper batchMapper, IFinAccountingPeriodService periodService, FinAccountingPeriodMapper periodMapper, FinAuditTrailRecorder auditRecorder)
    { this(expenseMapper, advanceMapper, batchMapper, periodService, periodMapper, auditRecorder, SecurityUtils::getDeptId); }

    FinExpenseVerificationServiceImpl(FinExpenseMapper expenseMapper, FinAdvanceMapper advanceMapper,
        FinExpenseVerifyBatchMapper batchMapper, IFinAccountingPeriodService periodService, FinAccountingPeriodMapper periodMapper,
        FinAuditTrailRecorder auditRecorder, Supplier<Long> deptResolver)
    { this.expenseMapper=expenseMapper; this.advanceMapper=advanceMapper; this.batchMapper=batchMapper; this.periodService=periodService; this.periodMapper=periodMapper; this.auditRecorder=auditRecorder; this.deptResolver=deptResolver; }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long verify(ExpenseVerifyVO request, String operator)
    {
        Long tenantId = TenantContext.getTenantId();
        Long deptId = deptResolver.get();
        if (deptId == null) throw new ServiceException("无法确定当前门店，禁止核销");
        if (request.getExpenseIds() == null || request.getExpenseIds().isEmpty()) throw new ServiceException("请选择要核销的费用记录");
        requireUnique(request.getExpenseIds());
        List<Long> advanceIds = request.getAdvanceIds() == null ? List.of() : request.getAdvanceIds();
        requireUnique(advanceIds);
        FinExpenseVerifyBatch existing = batchMapper.selectByRequestId(tenantId, deptId, request.getRequestId());
        if (existing != null) return validateReplay(existing, request, tenantId, deptId);
        List<FinExpense> expenses = expenseMapper.selectFinExpenseByExpenseIdsScoped(request.getExpenseIds(), tenantId, deptId);
        if (expenses == null || expenses.size() != request.getExpenseIds().size()) throw new ServiceException("费用记录不存在或无权访问");
        List<FinAdvance> advances = advanceIds.isEmpty() ? List.of() : advanceMapper.selectFinAdvanceByAdvanceIdsScoped(advanceIds, tenantId, deptId);
        if (advances == null || advances.size() != advanceIds.size()) throw new ServiceException("借支记录不存在或无权访问");
        for (FinExpense e : expenses) { if (!deptId.equals(e.getDeptId())) throw new ServiceException("核销记录必须属于同一部门"); if (!"0".equals(e.getStatus())) throw new ServiceException("费用状态已变化，请刷新后重试"); requirePeriod(e.getPeriodId()); }
        for (FinAdvance a : advances) { if (!deptId.equals(a.getDeptId())) throw new ServiceException("核销记录必须属于同一部门"); if (!"0".equals(a.getStatus())) throw new ServiceException("借支状态已变化，请刷新后重试"); requirePeriod(a.getPeriodId()); }
        Date now = new Date();
        BigDecimal expenseTotal = sumExpenses(expenses);
        BigDecimal advanceTotal = sumAdvances(advances);
        BigDecimal difference = money(expenseTotal.subtract(advanceTotal));
        FinExpenseVerifyBatch batch = new FinExpenseVerifyBatch(); batch.setBatchNo("HX" + now.getTime()); batch.setRequestId(request.getRequestId()); batch.setTenantId(tenantId); batch.setDeptId(deptId); batch.setTotalExpenseAmount(expenseTotal); batch.setTotalAdvanceAmount(advanceTotal); batch.setDifferenceAmount(difference); batch.setStatus(FinExpenseVerifyBatch.STATUS_VERIFIED); batch.setSourceType(FinExpenseVerifyBatch.SOURCE_NORMAL); batch.setVerifyBy(operator); batch.setVerifyTime(now); batch.setVersion(0);
        try { if (batchMapper.insertBatch(batch) != 1) throw new ServiceException("核销批次创建失败"); }
        catch (DuplicateKeyException ex) { FinExpenseVerifyBatch replay=batchMapper.selectByRequestId(tenantId,deptId,request.getRequestId()); if(replay!=null)return validateReplay(replay,request,tenantId,deptId); throw new ServiceException("核销请求并发冲突，请重试"); }
        if (batch.getBatchId() == null) throw new ServiceException("核销批次主键生成失败");
        List<FinExpenseVerifyDetail> expenseDetails = new ArrayList<>();
        for (FinExpense e : expenses) { FinExpenseVerifyDetail d=new FinExpenseVerifyDetail(); d.setBatchId(batch.getBatchId()); d.setExpenseId(e.getExpenseId()); d.setTenantId(tenantId); d.setDeptId(deptId); d.setExpenseAmount(e.getExpenseAmount()); d.setOriginalStatus(e.getStatus()); d.setOriginalAdvanceId(e.getAdvanceId()); d.setPeriodId(e.getPeriodId()); expenseDetails.add(d); }
        if (batchMapper.insertExpenseDetails(expenseDetails) != expenseDetails.size()) throw new ServiceException("费用核销明细保存失败");
        List<FinAdvanceVerifyDetail> advanceDetails = new ArrayList<>();
        for (FinAdvance a : advances) { FinAdvanceVerifyDetail d=detail(batch.getBatchId(), tenantId, deptId, a, FinAdvanceVerifyDetail.RELATION_SOURCE, "0"); advanceDetails.add(d); }
        if (!advances.isEmpty() && difference.signum()!=0) { boolean supplement=difference.signum()>0; FinAdvance generated=new FinAdvance(); generated.setDeptId(deptId); generated.setPeriodId(advances.get(0).getPeriodId()); generated.setAdvanceDate(now); generated.setAdvanceAmount(difference.abs()); generated.setAdvanceNo("TZ"+now.getTime()); generated.setPurpose(supplement ? "费用核销补款" : "费用核销节余"); generated.setStatus(supplement ? "1" : "0"); generated.setVerifyBy(supplement ? operator : null); generated.setVerifyTime(supplement ? now : null); generated.setDelFlag("0"); generated.setCreateBy(operator); if(advanceMapper.insertFinAdvance(generated)!=1||generated.getAdvanceId()==null)throw new ServiceException("核销差额记录生成失败"); advanceDetails.add(detail(batch.getBatchId(), tenantId, deptId, generated, supplement ? FinAdvanceVerifyDetail.RELATION_SUPPLEMENT : FinAdvanceVerifyDetail.RELATION_SURPLUS, "1")); }
        if (!advanceDetails.isEmpty() && batchMapper.insertAdvanceDetails(advanceDetails) != advanceDetails.size()) throw new ServiceException("借支核销明细保存失败");
        Long primaryAdvance = advances.isEmpty() ? null : advances.get(0).getAdvanceId();
        for (FinExpense e : expenses) if (expenseMapper.markExpenseVerified(e.getExpenseId(), primaryAdvance, operator, now,tenantId,deptId)==0) throw new ServiceException("费用状态已变化，请刷新后重试");
        for (FinAdvance a : advances) if (advanceMapper.markAdvanceVerified(a.getAdvanceId(), operator, now,tenantId,deptId)==0) throw new ServiceException("借支状态已变化，请刷新后重试");
        if (auditRecorder != null) auditRecorder.record("EXPENSE_VERIFY", "expense_verify_batch", String.valueOf(batch.getBatchId()), null, "batchNo="+batch.getBatchNo()+",expenseIds="+request.getExpenseIds()+",advanceIds="+advanceIds+",expenseTotal="+expenseTotal+",advanceTotal="+advanceTotal);
        return batch.getBatchId();
    }

    private FinAdvanceVerifyDetail detail(Long batchId,Long tenantId,Long deptId,FinAdvance a,String relation,String generated) { FinAdvanceVerifyDetail d=new FinAdvanceVerifyDetail(); d.setBatchId(batchId); d.setAdvanceId(a.getAdvanceId()); d.setTenantId(tenantId); d.setDeptId(deptId); d.setAdvanceAmount(a.getAdvanceAmount()); d.setOriginalStatus(a.getStatus()); d.setPeriodId(a.getPeriodId()); d.setRelationType(relation); d.setGeneratedFlag(generated); return d; }
    private void checkPeriod(Long id) { if (id != null) periodService.assertPeriodEditable(id); }
    private void requirePeriod(Long id) { if (id == null) throw new ServiceException("核算周期不能为空，禁止核销"); checkPeriod(id); }
    private Long validateReplay(FinExpenseVerifyBatch batch, ExpenseVerifyVO request, Long tenantId, Long deptId)
    {
        List<FinExpenseVerifyDetail> expenses = batchMapper.selectExpenseDetails(batch.getBatchId(), tenantId, deptId);
        List<FinAdvanceVerifyDetail> advances = batchMapper.selectAdvanceDetails(batch.getBatchId(), tenantId, deptId);
        Set<Long> expectedExpenses = request.getExpenseIds() == null ? Set.of() : new HashSet<>(request.getExpenseIds());
        Set<Long> actualExpenses = expenses == null ? Set.of() : expenses.stream().map(FinExpenseVerifyDetail::getExpenseId).collect(java.util.stream.Collectors.toSet());
        Set<Long> expectedAdvances = request.getAdvanceIds() == null ? Set.of() : new HashSet<>(request.getAdvanceIds());
        Set<Long> actualAdvances = advances == null ? Set.of() : advances.stream().filter(d -> FinAdvanceVerifyDetail.RELATION_SOURCE.equals(d.getRelationType())).map(FinAdvanceVerifyDetail::getAdvanceId).collect(java.util.stream.Collectors.toSet());
        if (!expectedExpenses.equals(actualExpenses) || !expectedAdvances.equals(actualAdvances)) throw new ServiceException("请求编号已用于其他核销内容");
        return batch.getBatchId();
    }
    private void requireUnique(List<Long> ids) { if (new HashSet<>(ids).size()!=ids.size()) throw new ServiceException("请求记录不能重复"); }
    private BigDecimal sumExpenses(List<FinExpense> rows) { BigDecimal total=BigDecimal.ZERO; for(FinExpense row:rows){if(row.getExpenseAmount()==null)throw new ServiceException("费用金额不能为空");total=total.add(row.getExpenseAmount());}return money(total); }
    private BigDecimal sumAdvances(List<FinAdvance> rows) { BigDecimal total=BigDecimal.ZERO; for(FinAdvance row:rows){if(row.getAdvanceAmount()==null)throw new ServiceException("借支金额不能为空");total=total.add(row.getAdvanceAmount());}return money(total); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    @Override
    public FinExpense getVerificationCandidate(Long expenseId)
    {
        Long tenantId = TenantContext.getTenantId();
        Long deptId = deptResolver.get();
        if (tenantId == null || deptId == null) throw new ServiceException("无法确定当前租户或门店，禁止核销");
        List<FinExpense> rows = expenseMapper.selectFinExpenseByExpenseIdsScoped(List.of(expenseId), tenantId, deptId);
        if (rows == null || rows.size() != 1) throw new ServiceException("费用记录不存在或无权访问");
        FinExpense expense = rows.get(0);
        if (!Objects.equals(expenseId, expense.getExpenseId()) || !Objects.equals(deptId, expense.getDeptId()) || !"0".equals(expense.getDelFlag())) throw new ServiceException("费用记录不存在或无权访问");
        if (!"0".equals(expense.getStatus())) throw new ServiceException("费用记录不可核销");
        return expense;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unverify(Long batchId, ExpenseUnverifyVO request, String operator)
    {
        Long tenantId = TenantContext.getTenantId();
        Long deptId = deptResolver.get();
        if (deptId == null) throw new ServiceException("无法确定当前门店，禁止反核销");
        FinExpenseVerifyBatch batch = batchMapper.selectBatchForUpdate(batchId, tenantId, deptId);
        if (batch == null) throw new ServiceException("核销批次不存在或无权访问");
        if (FinExpenseVerifyBatch.STATUS_REVERSED.equals(batch.getStatus())) {
            if (Objects.equals(request.getRequestId(), batch.getReverseRequestId())) return 0;
            throw new ServiceException("该核销批次已反核销");
        }
        if (!FinExpenseVerifyBatch.STATUS_VERIFIED.equals(batch.getStatus())) throw new ServiceException("核销批次状态不允许反核销");
        if (FinExpenseVerifyBatch.SOURCE_LEGACY.equals(batch.getSourceType())) throw new ServiceException("历史核销批次不能自动反核销");
        List<FinExpenseVerifyDetail> expenseDetails = batchMapper.selectExpenseDetails(batchId, tenantId, deptId);
        List<FinAdvanceVerifyDetail> advanceDetails = batchMapper.selectAdvanceDetails(batchId, tenantId, deptId);
        if (expenseDetails == null || expenseDetails.isEmpty()) throw new ServiceException("核销费用明细不存在");
        if (advanceDetails == null) advanceDetails = List.of();
        // NORMAL batches are created only from unverified originals with no verify metadata; LEGACY batches are rejected above.
        if (expenseDetails.stream().anyMatch(d -> !"0".equals(d.getOriginalStatus())) || advanceDetails.stream().filter(d -> FinAdvanceVerifyDetail.RELATION_SOURCE.equals(d.getRelationType())).anyMatch(d -> !"0".equals(d.getOriginalStatus()))) throw new ServiceException("核销快照不符合反核销条件");
        List<Long> expenseIds = expenseDetails.stream().map(FinExpenseVerifyDetail::getExpenseId).toList();
        List<FinExpense> currentExpenses = batchMapper.selectCurrentExpensesForUpdate(batchId, tenantId, deptId);
        List<FinAdvance> currentAdvances = batchMapper.selectCurrentAdvancesForUpdate(batchId, tenantId, deptId);
        if (currentExpenses == null || currentExpenses.size() != expenseIds.size()) throw new ServiceException("费用状态已变化，请刷新后重试");
        if (currentAdvances == null || currentAdvances.size() != advanceDetails.size()) throw new ServiceException("借支状态已变化，请刷新后重试");
        Set<Long> periodIds = new HashSet<>();
        java.util.Map<Long,FinExpense> expenseById = currentExpenses.stream().collect(java.util.stream.Collectors.toMap(FinExpense::getExpenseId, e -> e));
        for (FinExpenseVerifyDetail d : expenseDetails) { FinExpense e=expenseById.get(d.getExpenseId()); requireSamePeriod(d.getPeriodId(), e==null?null:e.getPeriodId(), periodIds); }
        java.util.Map<Long,FinAdvance> advanceById = currentAdvances.stream().collect(java.util.stream.Collectors.toMap(FinAdvance::getAdvanceId, a -> a));
        for (FinAdvanceVerifyDetail d : advanceDetails) { FinAdvance a=advanceById.get(d.getAdvanceId()); requireSamePeriod(d.getPeriodId(), a==null?null:a.getPeriodId(), periodIds); }
        for (Long periodId : periodIds.stream().sorted().toList()) {
            com.junsong.finance.domain.FinAccountingPeriod locked = periodMapper.selectPeriodForUpdate(periodId, tenantId, deptId);
            if (locked == null) throw new ServiceException("核算周期不存在或无权访问");
            if (!PeriodStatus.ACTIVE.equals(locked.getStatus())) throw new ServiceException("核算周期已锁定或结转，不能反核销");
            periodService.assertPeriodEditable(periodId);
        }
        List<FinAdvanceVerifyDetail> sourceDetails = advanceDetails.stream().filter(d -> FinAdvanceVerifyDetail.RELATION_SOURCE.equals(d.getRelationType())).toList();
        Long associatedAdvanceId = sourceDetails.isEmpty() ? null : sourceDetails.get(0).getAdvanceId();
        for (FinExpense e : currentExpenses) if (!"1".equals(e.getStatus()) || !Objects.equals(associatedAdvanceId,e.getAdvanceId()) || !Objects.equals(batch.getVerifyBy(),e.getVerifyBy()) || !Objects.equals(batch.getVerifyTime(),e.getVerifyTime())) throw new ServiceException("费用核销信息已变化，请刷新后重试");
        for (FinAdvanceVerifyDetail d : advanceDetails) validateAdvanceState(d, advanceById.get(d.getAdvanceId()), batch);
        List<FinAdvanceVerifyDetail> generatedDetails = advanceDetails.stream().filter(d -> "1".equals(d.getGeneratedFlag())).toList();
        for (FinAdvanceVerifyDetail d : generatedDetails) if (batchMapper.countGeneratedAdvanceDownstreamReferences(d.getAdvanceId(), batchId, tenantId, deptId) > 0) throw new ServiceException("核销生成的借支记录已被后续业务使用，不能反核销");
        for (FinExpenseVerifyDetail d : expenseDetails) if (batchMapper.restoreExpenseSnapshot(d.getExpenseId(), d.getOriginalStatus(), d.getOriginalAdvanceId(), tenantId, deptId, associatedAdvanceId, batch.getVerifyBy(), batch.getVerifyTime()) != 1) throw new ServiceException("费用状态已变化，请刷新后重试");
        for (FinAdvanceVerifyDetail d : sourceDetails) if (batchMapper.restoreAdvanceSnapshot(d.getAdvanceId(), d.getOriginalStatus(), tenantId, deptId, batch.getVerifyBy(), batch.getVerifyTime()) != 1) throw new ServiceException("借支状态已变化，请刷新后重试");
        for (FinAdvanceVerifyDetail d : generatedDetails) { FinAdvance a=advanceById.get(d.getAdvanceId()); if (batchMapper.invalidateGeneratedAdvance(d.getAdvanceId(), tenantId, deptId, a.getStatus(), a.getVerifyBy(), a.getVerifyTime()) != 1) throw new ServiceException("核销差额记录状态已变化，请刷新后重试"); }
        Date now = new Date();
        if (batchMapper.markBatchReversed(batchId, tenantId, deptId, batch.getVersion(), operator, now, request.getReason(), request.getRequestId()) != 1) throw new ServiceException("反核销状态已变化，请刷新后重试");
        if (auditRecorder != null) auditRecorder.record("EXPENSE_UNVERIFY", "expense_verify_batch", String.valueOf(batchId), null, "batchNo="+batch.getBatchNo()+",reason="+request.getReason()+",expenseIds="+expenseIds);
        return 1;
    }
    private void requireSamePeriod(Long snapshot, Long current, Set<Long> ids) { if(snapshot==null||current==null)throw new ServiceException("核算周期不能为空，不能反核销"); if(!snapshot.equals(current))throw new ServiceException("核算周期已变化，不能反核销"); ids.add(snapshot); ids.add(current); }
    private void validateAdvanceState(FinAdvanceVerifyDetail d, FinAdvance a, FinExpenseVerifyBatch b) { if(a==null)throw new ServiceException("借支状态已变化，请刷新后重试"); boolean surplus=FinAdvanceVerifyDetail.RELATION_SURPLUS.equals(d.getRelationType()); if(surplus&&"0".equals(a.getStatus())) { if(a.getVerifyBy()!=null||a.getVerifyTime()!=null)throw new ServiceException("借支核销信息已变化，请刷新后重试"); return; } if(!"1".equals(a.getStatus())||!Objects.equals(b.getVerifyBy(),a.getVerifyBy())||!Objects.equals(b.getVerifyTime(),a.getVerifyTime()))throw new ServiceException("借支核销信息已变化，请刷新后重试"); }
    @Override
    public ExpenseOperationCapabilityVO getCapability(Long expenseId)
    {
        ExpenseOperationCapabilityVO result = new ExpenseOperationCapabilityVO();
        result.setCanVerify(false);
        result.setCanUnverify(false);
        Long tenantId = TenantContext.getTenantId();
        Long deptId = deptResolver.get();
        if (expenseId == null || deptId == null) return disabled(result, "无法确定费用或当前门店");
        try
        {
            List<FinExpense> scopedExpenses = expenseMapper.selectFinExpenseByExpenseIdsScoped(List.of(expenseId), tenantId, deptId);
            FinExpense expense = scopedExpenses == null || scopedExpenses.size() != 1 ? null : scopedExpenses.get(0);
            if (expense == null || !deptId.equals(expense.getDeptId())) return disabled(result, "费用记录不存在或无权访问");
            if (expense.getPeriodId() == null) return disabled(result, "费用未关联核算周期");
            periodService.assertPeriodEditable(expense.getPeriodId());
            if ("0".equals(expense.getStatus()))
            {
                result.setCanVerify(true);
                return result;
            }
            if (!"1".equals(expense.getStatus())) return disabled(result, "费用状态不允许核销操作");
            FinExpenseVerifyBatch batch = batchMapper.selectByExpenseId(expenseId, tenantId, deptId);
            if (batch == null) return disabled(result, "未找到可追溯的核销批次");
            result.setBatchId(batch.getBatchId());
            if (!FinExpenseVerifyBatch.STATUS_VERIFIED.equals(batch.getStatus())) return disabled(result, "核销批次已反核销或状态已变化");
            if (!FinExpenseVerifyBatch.SOURCE_NORMAL.equals(batch.getSourceType())) return disabled(result, "历史核销批次不能自动反核销");
            List<FinExpenseVerifyDetail> expenseDetails = batchMapper.selectExpenseDetails(batch.getBatchId(), tenantId, deptId);
            List<FinAdvanceVerifyDetail> advanceDetails = batchMapper.selectAdvanceDetails(batch.getBatchId(), tenantId, deptId);
            if (expenseDetails == null || expenseDetails.isEmpty() || advanceDetails == null) return disabled(result, "核销明细不完整");
            List<Long> batchExpenseIds = expenseDetails.stream().map(FinExpenseVerifyDetail::getExpenseId).toList();
            List<Long> batchAdvanceIds = advanceDetails.stream().map(FinAdvanceVerifyDetail::getAdvanceId).toList();
            List<FinExpense> currentExpenses = expenseMapper.selectFinExpenseByExpenseIdsScoped(batchExpenseIds, tenantId, deptId);
            List<FinAdvance> currentAdvances = batchAdvanceIds.isEmpty() ? List.of() : advanceMapper.selectFinAdvanceByAdvanceIdsScoped(batchAdvanceIds, tenantId, deptId);
            if (currentExpenses == null || currentExpenses.size() != expenseDetails.size()
                || currentAdvances == null || currentAdvances.size() != advanceDetails.size())
                return disabled(result, "核销关联数据已变化");
            FinExpenseVerifyDetail snapshot = expenseDetails.stream().filter(d -> expenseId.equals(d.getExpenseId())).findFirst().orElse(null);
            if (snapshot == null || snapshot.getPeriodId() == null || !snapshot.getPeriodId().equals(expense.getPeriodId())) return disabled(result, "费用核算周期已变化");
            List<FinAdvanceVerifyDetail> source = advanceDetails.stream().filter(d -> FinAdvanceVerifyDetail.RELATION_SOURCE.equals(d.getRelationType())).toList();
            Long expectedAdvanceId = source.isEmpty() ? null : source.get(0).getAdvanceId();
            if (!Objects.equals(expectedAdvanceId, expense.getAdvanceId()) || !Objects.equals(batch.getVerifyBy(), expense.getVerifyBy()) || !Objects.equals(batch.getVerifyTime(), expense.getVerifyTime())) return disabled(result, "费用核销信息已变化");
            for (FinExpenseVerifyDetail detail : expenseDetails)
            {
                if (detail.getPeriodId() == null) return disabled(result, "核销快照缺少核算周期");
                periodService.assertPeriodEditable(detail.getPeriodId());
                FinExpense current = currentExpenses.stream().filter(e -> detail.getExpenseId().equals(e.getExpenseId())).findFirst().orElse(null);
                if (current == null || !"1".equals(current.getStatus()) || !detail.getPeriodId().equals(current.getPeriodId())
                    || !Objects.equals(expectedAdvanceId, current.getAdvanceId()) || !Objects.equals(batch.getVerifyBy(), current.getVerifyBy())
                    || !Objects.equals(batch.getVerifyTime(), current.getVerifyTime())) return disabled(result, "费用核销信息已变化");
            }
            for (FinAdvanceVerifyDetail detail : advanceDetails)
            {
                if (detail.getPeriodId() == null) return disabled(result, "核销快照缺少核算周期");
                periodService.assertPeriodEditable(detail.getPeriodId());
                FinAdvance current = currentAdvances.stream().filter(a -> detail.getAdvanceId().equals(a.getAdvanceId())).findFirst().orElse(null);
                if (current == null || !detail.getPeriodId().equals(current.getPeriodId())) return disabled(result, "借支核销信息已变化");
                try { validateAdvanceState(detail, current, batch); }
                catch (ServiceException ex) { return disabled(result, ex.getMessage()); }
                if ("1".equals(detail.getGeneratedFlag()) && batchMapper.countGeneratedAdvanceDownstreamReferences(
                    detail.getAdvanceId(), batch.getBatchId(), tenantId, deptId) > 0)
                    return disabled(result, "核销生成的借支记录已被后续业务使用，不能反核销");
            }
            result.setCanUnverify(true);
            return result;
        }
        catch (ServiceException ex)
        {
            String message = ex.getMessage();
            return disabled(result, message == null || message.isBlank() ? "当前状态暂不允许操作" : message);
        }
        catch (RuntimeException ex)
        {
            log.warn("Failed to calculate expense operation capability, expenseId={}", expenseId, ex);
            return disabled(result, "当前状态暂不允许操作");
        }
    }

    private ExpenseOperationCapabilityVO disabled(ExpenseOperationCapabilityVO result, String reason)
    {
        result.setOperationDisabledReason(reason);
        return result;
    }
}
