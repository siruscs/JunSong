package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.vo.AccountingPeriodCheckItemVO;
import com.junsong.finance.domain.vo.AccountingPeriodCheckResultVO;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinProfitShareRecordService;
import com.junsong.finance.service.IAccountingPeriodCheckService;

@Service
public class FinAccountingPeriodServiceImpl implements IFinAccountingPeriodService
{
    /**
     * 锁账绕过标志：供结转回退等特权操作临时跳过 assertPeriodEditable 检查。
     * 使用 ThreadLocal 确保仅对当前线程生效，并在特权操作结束后立即清除。
     */
    private static final ThreadLocal<Boolean> BYPASS_EDITABLE = new ThreadLocal<>();

    @Autowired
    private FinAccountingPeriodMapper finAccountingPeriodMapper;

    @Autowired
    private IFinProfitShareRecordService finProfitShareRecordService;

    @Autowired
    private FinAuditTrailRecorder auditTrailRecorder;

    @Autowired
    private IAccountingPeriodCheckService accountingPeriodCheckService;

    @Autowired
    private FinProfitShareRecordMapper finProfitShareRecordMapper;

    @Autowired
    private com.junsong.finance.service.IFinCompositeAccountingService compositeAccountingService;

    public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long periodId) { return finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId); }

    @Transactional(rollbackFor = Exception.class)
    public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) {
        FinAccountingPeriod currentPeriod = finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptId);
        return currentPeriod == null ? null : refreshPeriodStats(currentPeriod);
    }

    @Transactional(rollbackFor = Exception.class)
    public FinAccountingPeriod initCurrentPeriod(Long deptId) {
        if (deptId == null) {
            throw new ServiceException("机构不能为空");
        }
        FinAccountingPeriod currentPeriod = finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptId);
        if (currentPeriod != null) {
            return refreshPeriodStats(currentPeriod);
        }

        FinAccountingPeriod period = new FinAccountingPeriod();
        Date now = new Date();
        period.setDeptId(deptId);
        period.setPeriodNo(buildPeriodNo(deptId, now));
        period.setStartTime(now);
        period.setStatus(PeriodStatus.ACTIVE);
        fillZeroStats(period);
        finAccountingPeriodMapper.insertFinAccountingPeriod(period);
        return period;
    }

    /**
     * 试算回本：只刷新统计数据并返回结果，不改变周期状态
     */
    @Transactional(rollbackFor = Exception.class)
    public FinAccountingPeriod trialBreakEven(Long deptId) {
        FinAccountingPeriod period = initCurrentPeriod(deptId);
        // 只刷新统计数据，不改变状态
        return refreshPeriodStats(period);
    }

    /**
     * 结转：关闭当前进行中的周期，执行分润计算，创建新周期
     */
    @Transactional(rollbackFor = Exception.class)
    public FinAccountingPeriod carryForward(Long deptId) {
        if (deptId == null) {
            throw new ServiceException("机构不能为空");
        }
        FinAccountingPeriod period = finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptId);
        if (period == null) {
            throw new ServiceException("当前没有进行中的核算周期");
        }
        period = finAccountingPeriodMapper.selectPeriodForUpdate(period.getPeriodId(), TenantContext.getTenantId(), deptId);
        if (period == null) throw new ServiceException("当前核算周期不存在或无权访问");
        if (!PeriodStatus.ACTIVE.equals(period.getStatus())) {
            throw new ServiceException("只有进行中的周期才能结转");
        }

        // 锁账前检查：存在 BLOCK 项时不允许结转
        AccountingPeriodCheckResultVO checkResult = accountingPeriodCheckService.checkBeforeLock(deptId);
        if (!checkResult.isCanLock()) {
            StringBuilder sb = new StringBuilder("存在阻断项，无法结转：");
            for (AccountingPeriodCheckItemVO item : checkResult.getItems()) {
                if ("BLOCK".equals(item.getLevel()) && item.getCount() > 0) {
                    sb.append(item.getTitle()).append("(").append(item.getDescription()).append(")；");
                }
            }
            throw new ServiceException(sb.toString());
        }

        // 刷新最终统计数据
        period = refreshPeriodStats(period);

        String beforeSnapshot = "{\"periodId\":" + period.getPeriodId() + ",\"status\":\"" + period.getStatus()
                + "\",\"totalSaleAmount\":" + period.getTotalSaleAmount() + ",\"totalVerifiedExpense\":" + period.getTotalVerifiedExpense()
                + ",\"netProfit\":" + period.getNetProfit() + "}";

        Date now = new Date();
        period.setEndTime(now);
        period.setCarryForwardTime(now);
        period.setCarryForwardBy(SecurityUtils.getUsername());
        period.setStatus(PeriodStatus.CARRIED);
        period.setUpdateBy(SecurityUtils.getUsername());
        period.setRemark(appendRemark(period.getRemark(), "结转操作"));
        // DB 唯一键兜底：从 AOP ThreadLocal 读取幂等键填充到业务表
        period.setTenantId(TenantContext.getTenantId());
        period.setCarryForwardIdempotencyKey(
                com.junsong.common.core.idempotency.IdempotencyResultStore.currentKey());
        finAccountingPeriodMapper.updateFinAccountingPeriod(period);

        auditTrailRecorder.record("period_carry_forward", "accounting_period", String.valueOf(period.getPeriodId()),
                beforeSnapshot,
                "{\"periodId\":" + period.getPeriodId() + ",\"status\":\"" + period.getStatus() + "\",\"endTime\":\"" + now + "\"}");

        // 执行分润计算（如果净利大于0）
        BigDecimal netProfit = nvl(period.getNetProfit());
        if (netProfit.compareTo(BigDecimal.ZERO) > 0) {
            // 预检查分润配置：配置缺失时直接抛出明确提示，避免进入分润事务后导致 rollback-only
            finProfitShareRecordService.checkProfitConfigReady(deptId);
            try {
                finProfitShareRecordService.carryForwardPeriod(period.getPeriodId());
            } catch (ServiceException e) {
                // 分润配置未就绪时仅记录，不阻断结转（不再次 update，避免事务被标记回滚后再次写入引发 UnexpectedRollbackException）
                period.setRemark(appendRemark(period.getRemark(), "分润跳过：" + e.getMessage()));
            }
        }

        // 自动纳入复合核算池（不阻断单店结转流程，失败仅记录日志）
        try {
            compositeAccountingService.autoIncludeAfterPeriodCarryForward(period.getPeriodId());
        } catch (Exception e) {
            period.setRemark(appendRemark(period.getRemark(), "复合核算自动纳入失败：" + e.getMessage()));
        }

        // 创建新周期
        ensureNextPeriod(deptId, now);

        return finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptId);
    }

    /**
     * 结转回退：删除当前进行中的空周期，将最新已结转周期回退为进行中
     */
    @Transactional(rollbackFor = Exception.class)
    public FinAccountingPeriod rollbackCarryForward(Long deptId) {
        return rollbackCarryForward(deptId, null);
    }

    /**
     * 结转回退（含原因）：反结账必须填写原因并记录审计
     */
    @Transactional(rollbackFor = Exception.class)
    public FinAccountingPeriod rollbackCarryForward(Long deptId, String reason) {
        if (deptId == null) {
            throw new ServiceException("机构不能为空");
        }
        String rollbackReason = reason == null ? "" : reason.trim();
        if (rollbackReason.isEmpty()) {
            throw new ServiceException("反结账原因不能为空");
        }

        // 1. 找到当前进行中的周期
        FinAccountingPeriod activePeriod = finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptId);
        if (activePeriod == null) {
            throw new ServiceException("当前没有进行中的核算周期，无法回退");
        }

        // 2. 检查当前进行中的周期是否有业务数据
        BigDecimal totalExpense = nvl(finAccountingPeriodMapper.selectTotalVerifiedExpense(activePeriod.getPeriodId(), deptId, activePeriod.getStartTime(), null));
        BigDecimal totalPurchase = nvl(finAccountingPeriodMapper.selectTotalPurchase(activePeriod.getPeriodId(), deptId, activePeriod.getStartTime(), null));
        BigDecimal totalSalePayment = nvl(finAccountingPeriodMapper.selectTotalSalePayment(activePeriod.getPeriodId(), deptId, activePeriod.getStartTime(), null));
        BigDecimal totalUnverifiedAdvance = nvl(finAccountingPeriodMapper.selectTotalUnverifiedAdvance(activePeriod.getPeriodId(), deptId, activePeriod.getStartTime(), null));
        BigDecimal totalBusiness = totalExpense.add(totalPurchase).add(totalSalePayment).add(totalUnverifiedAdvance);
        if (totalBusiness.compareTo(BigDecimal.ZERO) > 0) {
            throw new ServiceException("当前进行中的周期已有业务数据，无法回退结转");
        }

        // 3. 删除当前进行中的周期（软删除）
        finAccountingPeriodMapper.deleteFinAccountingPeriodByPeriodId(activePeriod.getPeriodId());

        // 4. 找到最新已结转的周期
        FinAccountingPeriod carriedPeriod = finAccountingPeriodMapper.selectLatestCarriedPeriodByDeptId(deptId);
        if (carriedPeriod == null) {
            throw new ServiceException("没有已结转的周期可以回退");
        }

        // 4.1 回退保护：若该周期已纳入复合核算池，禁止直接反结账
        if (compositeAccountingService.isPeriodIncludedInComposite(carriedPeriod.getPeriodId())) {
            throw new ServiceException("该周期已纳入复合核算，请先处理复合核算明细后再反结账");
        }

        // 5. 作废该周期的分润记录（特权操作：临时绕过锁账检查，因为回退本身就是要修改已结转周期）
        BYPASS_EDITABLE.set(true);
        try {
            FinProfitShareRecord shareQuery = new FinProfitShareRecord();
            shareQuery.setDeptId(deptId);
            shareQuery.setPeriodId(carriedPeriod.getPeriodId());
            List<FinProfitShareRecord> shares = finProfitShareRecordService.selectFinProfitShareRecordList(shareQuery);
            if (shares != null && !shares.isEmpty()) {
                Long[] shareIds = shares.stream().map(FinProfitShareRecord::getShareId).toArray(Long[]::new);
                finProfitShareRecordService.deleteFinProfitShareRecordByShareIds(shareIds);
            }
        } finally {
            BYPASS_EDITABLE.remove();
        }

        // 6. 回退周期状态为进行中
        String beforeSnapshot = "{\"periodId\":" + carriedPeriod.getPeriodId() + ",\"status\":\"" + carriedPeriod.getStatus()
                + "\",\"carryForwardTime\":\"" + carriedPeriod.getCarryForwardTime() + "\"}";
        Date rollbackTime = new Date();
        carriedPeriod.setStatus(PeriodStatus.ACTIVE);
        // 回退后成为新的当前进行中周期：重置开始时间为回退时间，清空结束时间/结转时间/结转人
        carriedPeriod.setStartTime(rollbackTime);
        carriedPeriod.setEndTime(null);
        carriedPeriod.setBreakEvenTime(null);
        carriedPeriod.setCarryForwardTime(null);
        carriedPeriod.setCarryForwardBy(null);
        carriedPeriod.setUpdateBy(SecurityUtils.getUsername());
        String remarkMsg = "结转回退操作，原因：" + rollbackReason;
        carriedPeriod.setRemark(appendRemark(carriedPeriod.getRemark(), remarkMsg));
        // DB 唯一键兜底：从 AOP ThreadLocal 读取幂等键填充到业务表
        // 注意：回退操作复用 carry_forward_idempotency_key 列，但幂等键值不同（由 AOP scene 区分）
        carriedPeriod.setTenantId(TenantContext.getTenantId());
        carriedPeriod.setCarryForwardIdempotencyKey(
                com.junsong.common.core.idempotency.IdempotencyResultStore.currentKey());
        finAccountingPeriodMapper.updateFinAccountingPeriod(carriedPeriod);

        String afterSnapshot = "{\"periodId\":" + carriedPeriod.getPeriodId() + ",\"status\":\"" + carriedPeriod.getStatus()
                + "\",\"reason\":\"" + escapeJson(rollbackReason) + "\"}";
        auditTrailRecorder.record("period_rollback", "accounting_period", String.valueOf(carriedPeriod.getPeriodId()),
                beforeSnapshot, afterSnapshot);

        // 刷新统计数据
        return refreshPeriodStats(carriedPeriod);
    }

    public void assertPeriodEditable(Long periodId) {
        // 特权操作（如结转回退）临时跳过锁账检查
        if (Boolean.TRUE.equals(BYPASS_EDITABLE.get())) {
            return;
        }
        if (periodId == null) {
            return;
        }
        FinAccountingPeriod period = finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId);
        if (period != null && !PeriodStatus.ACTIVE.equals(period.getStatus())) {
            String statusLabel = PeriodStatus.CARRIED.equals(period.getStatus()) ? "已结转" : "已回本待结转";
            String msg = "会计期间「" + period.getPeriodNo() + "」" + statusLabel + "，不能修改历史流水";
            if (period.getCarryForwardBy() != null) {
                msg += "（锁账人：" + period.getCarryForwardBy() + "）";
            }
            throw new ServiceException(msg);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod finAccountingPeriod) {
        List<FinAccountingPeriod> periods = finAccountingPeriodMapper.selectFinAccountingPeriodList(finAccountingPeriod);
        for (int i = 0; i < periods.size(); i++) {
            FinAccountingPeriod period = periods.get(i);
            if (PeriodStatus.ACTIVE.equals(period.getStatus())) {
                periods.set(i, refreshPeriodStats(period));
                break;
            }
        }
        return periods;
    }
    public int insertFinAccountingPeriod(FinAccountingPeriod finAccountingPeriod) {
        if (StringUtils.isEmpty(finAccountingPeriod.getStatus())) { finAccountingPeriod.setStatus(PeriodStatus.ACTIVE); }
        if (finAccountingPeriod.getStartTime() == null) { finAccountingPeriod.setStartTime(new Date()); }
        if (StringUtils.isEmpty(finAccountingPeriod.getPeriodNo())) { finAccountingPeriod.setPeriodNo(buildPeriodNo(finAccountingPeriod.getDeptId(), finAccountingPeriod.getStartTime())); }
        fillZeroStats(finAccountingPeriod);
        return finAccountingPeriodMapper.insertFinAccountingPeriod(finAccountingPeriod);
    }
    public int updateFinAccountingPeriod(FinAccountingPeriod finAccountingPeriod) { return finAccountingPeriodMapper.updateFinAccountingPeriod(finAccountingPeriod); }
    public int deleteFinAccountingPeriodByPeriodIds(Long[] periodIds) { return finAccountingPeriodMapper.deleteFinAccountingPeriodByPeriodIds(periodIds); }

    /**
     * 运维调整：只调整历史核算周期起始时间，不重新核算金额，不重新计算分润。
     * 如果被调整周期已存在分润记录，则同步把分润时间改为周期结束时间。
     */
    @Transactional(rollbackFor = Exception.class)
    public FinAccountingPeriod opsAdjustStartTime(Long periodId, Date startTime, Date endTime, String reason) {
        // 1. 参数校验
        if (periodId == null) {
            throw new ServiceException("周期ID不能为空");
        }
        if (startTime == null) {
            throw new ServiceException("新起始时间不能为空");
        }
        String adjustReason = reason == null ? "" : reason.trim();
        if (adjustReason.isEmpty()) {
            throw new ServiceException("调整原因不能为空");
        }
        if (adjustReason.length() < 5) {
            throw new ServiceException("调整原因不少于5个字");
        }

        // 2. 查询周期
        FinAccountingPeriod period = finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId);
        if (period == null) {
            throw new ServiceException("核算周期不存在");
        }

        Date oldStartTime = period.getStartTime();
        Date oldEndTime = period.getEndTime();

        // 3. 时间校验
        // 如果提供了新结束时间，则新起始时间必须早于新结束时间
        // 否则使用周期现有结束时间校验
        Date effectiveEndTime = endTime != null ? endTime : oldEndTime;
        Date now = new Date();
        if (effectiveEndTime != null) {
            if (!startTime.before(effectiveEndTime)) {
                throw new ServiceException("新起始时间必须早于周期结束时间");
            }
        } else {
            if (!startTime.before(now)) {
                throw new ServiceException("新起始时间必须早于当前时间");
            }
        }
        // 如果提供了新结束时间，新结束时间必须早于当前时间（历史周期）
        if (endTime != null && !endTime.before(now)) {
            throw new ServiceException("新结束时间必须早于当前时间（仅支持调整历史周期）");
        }

        // 4. 相邻周期保护
        // 注意：不再限制新起始时间不得早于上一周期结束时间，以便用户倒序补录历史数据
        // 仅保留下一周期约束：新结束时间不得晚于下一周期起始时间
        FinAccountingPeriod nextPeriod = finAccountingPeriodMapper.selectNextPeriod(period.getDeptId(), oldStartTime, periodId);
        if (nextPeriod != null && nextPeriod.getStartTime() != null) {
            Date checkEndTime = endTime != null ? endTime : startTime;
            if (checkEndTime.after(nextPeriod.getStartTime())) {
                throw new ServiceException("周期结束时间不得晚于下一周期起始时间（" + formatTime(nextPeriod.getStartTime()) + "）");
            }
        }

        // 5. 保存调整前快照
        String beforeSnapshot = buildOpsAdjustBeforeSnapshot(period);

        // 6. 只更新周期 startTime/endTime、updateBy、remark（不触碰金额和其他字段）
        String username = SecurityUtils.getUsername();
        String newRemark = appendRemark(period.getRemark(), "运维调整起始/结束时间：" + adjustReason);
        finAccountingPeriodMapper.updateStartTimeOnly(periodId, startTime, endTime, username, newRemark);

        // 7. 查询该周期对应的分润记录，如新 endTime 不为空则同步 shareTime 为新 endTime
        String oldShareTimeStr = "";
        String newShareTimeStr = "";
        Date shareTime = endTime != null ? endTime : oldEndTime;
        FinProfitShareRecord shareRecord = finProfitShareRecordMapper.selectFinProfitShareRecordByPeriodId(periodId);
        if (shareRecord != null && shareTime != null) {
            oldShareTimeStr = formatTime(shareRecord.getShareTime());
            finProfitShareRecordMapper.updateShareTimeByPeriodId(periodId, shareTime, username,
                    appendRemark(shareRecord.getRemark(), "运维调整同步分润时间：" + adjustReason));
            newShareTimeStr = formatTime(shareTime);
        }

        // 8. 记录审计（不调用任何统计刷新或分润重算方法）
        String afterSnapshot = buildOpsAdjustAfterSnapshot(periodId, period.getDeptId(), period.getPeriodNo(),
                oldStartTime, startTime, oldEndTime, endTime, shareTime, oldShareTimeStr, newShareTimeStr, adjustReason);
        auditTrailRecorder.record("period_ops_adjust_start_time", "accounting_period", String.valueOf(periodId),
                beforeSnapshot, afterSnapshot);

        // 9. 返回最新周期（不调用 refreshPeriodStats，避免重算）
        return finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId);
    }

    private String buildOpsAdjustBeforeSnapshot(FinAccountingPeriod period) {
        return "{\"periodId\":" + period.getPeriodId()
                + ",\"deptId\":" + period.getDeptId()
                + ",\"periodNo\":\"" + escapeJson(safeStr(period.getPeriodNo())) + "\""
                + ",\"oldStartTime\":\"" + formatTime(period.getStartTime()) + "\""
                + ",\"oldEndTime\":\"" + formatTime(period.getEndTime()) + "\""
                + ",\"netProfit\":" + period.getNetProfit()
                + ",\"managerProfitAmount\":" + period.getManagerProfitAmount()
                + ",\"investorProfitAmount\":" + period.getInvestorProfitAmount()
                + "}";
    }

    private String buildOpsAdjustAfterSnapshot(Long periodId, Long deptId, String periodNo,
                                                Date oldStartTime, Date newStartTime,
                                                Date oldEndTime, Date newEndTime,
                                                Date shareTime,
                                                String oldShareTime, String newShareTime, String reason) {
        return "{\"periodId\":" + periodId
                + ",\"deptId\":" + deptId
                + ",\"periodNo\":\"" + escapeJson(safeStr(periodNo)) + "\""
                + ",\"oldStartTime\":\"" + formatTime(oldStartTime) + "\""
                + ",\"newStartTime\":\"" + formatTime(newStartTime) + "\""
                + ",\"oldEndTime\":\"" + formatTime(oldEndTime) + "\""
                + ",\"newEndTime\":\"" + formatTime(newEndTime) + "\""
                + ",\"shareTime\":\"" + formatTime(shareTime) + "\""
                + ",\"oldShareTime\":\"" + safeStr(oldShareTime) + "\""
                + ",\"newShareTime\":\"" + safeStr(newShareTime) + "\""
                + ",\"reason\":\"" + escapeJson(reason) + "\""
                + "}";
    }

    private String formatTime(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private String safeStr(String value) {
        return value == null ? "" : value;
    }

    private FinAccountingPeriod refreshPeriodStats(FinAccountingPeriod period) {
        Date endTime = period.getEndTime();
        Long periodId = period.getPeriodId();
        Long deptId = period.getDeptId();
        Date startTime = period.getStartTime();
        period.setTotalVerifiedExpense(nvl(finAccountingPeriodMapper.selectTotalVerifiedExpense(periodId, deptId, startTime, endTime)));
        period.setTotalPurchase(nvl(finAccountingPeriodMapper.selectTotalPurchase(periodId, deptId, startTime, endTime)));
        period.setTotalSalePayment(nvl(finAccountingPeriodMapper.selectTotalSalePayment(periodId, deptId, startTime, endTime)));
        period.setTotalSaleAmount(nvl(finAccountingPeriodMapper.selectTotalSaleAmount(periodId, deptId, startTime, endTime)));
        period.setTotalUnverifiedAdvance(nvl(finAccountingPeriodMapper.selectTotalUnverifiedAdvance(periodId, deptId, startTime, endTime)));
        period.setNetProfit(nvl(period.getTotalSalePayment())
                .subtract(nvl(period.getTotalVerifiedExpense()))
                .subtract(nvl(period.getTotalPurchase()))
                .subtract(nvl(period.getTotalUnverifiedAdvance())));
        finAccountingPeriodMapper.updateFinAccountingPeriod(period);
        return finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(period.getPeriodId());
    }

    private void fillZeroStats(FinAccountingPeriod period) {
        period.setTotalVerifiedExpense(nvl(period.getTotalVerifiedExpense()));
        period.setTotalPurchase(nvl(period.getTotalPurchase()));
        period.setTotalSalePayment(nvl(period.getTotalSalePayment()));
        period.setTotalSaleAmount(nvl(period.getTotalSaleAmount()));
        period.setTotalUnverifiedAdvance(nvl(period.getTotalUnverifiedAdvance()));
        period.setNetProfit(nvl(period.getNetProfit()));
        period.setManagerProfitAmount(nvl(period.getManagerProfitAmount()));
        period.setInvestorProfitAmount(nvl(period.getInvestorProfitAmount()));
    }

    private void ensureNextPeriod(Long deptId, Date startTime) {
        if (finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptId) != null) {
            return;
        }
        FinAccountingPeriod nextPeriod = new FinAccountingPeriod();
        nextPeriod.setDeptId(deptId);
        nextPeriod.setPeriodNo(buildPeriodNo(deptId, startTime) + "N");
        nextPeriod.setStartTime(startTime);
        nextPeriod.setStatus(PeriodStatus.ACTIVE);
        nextPeriod.setRemark("结转后自动开启的新核算周期");
        fillZeroStats(nextPeriod);
        finAccountingPeriodMapper.insertFinAccountingPeriod(nextPeriod);
    }

    private String buildPeriodNo(Long deptId, Date startTime) {
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(startTime == null ? new Date() : startTime);
        return "AP" + time + (deptId == null ? "" : deptId);
    }

    private String appendRemark(String oldRemark, String newRemark) {
        if (oldRemark == null || oldRemark.isEmpty()) {
            return newRemark;
        }
        return oldRemark + "；" + newRemark;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
