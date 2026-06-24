package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinProfitShareRecordService;

@Service
public class FinAccountingPeriodServiceImpl implements IFinAccountingPeriodService
{
    @Autowired
    private FinAccountingPeriodMapper finAccountingPeriodMapper;

    @Autowired
    private IFinProfitShareRecordService finProfitShareRecordService;

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
        if (!PeriodStatus.ACTIVE.equals(period.getStatus())) {
            throw new ServiceException("只有进行中的周期才能结转");
        }

        // 刷新最终统计数据
        period = refreshPeriodStats(period);

        Date now = new Date();
        period.setEndTime(now);
        period.setCarryForwardTime(now);
        period.setStatus(PeriodStatus.CARRIED);
        period.setUpdateBy(SecurityUtils.getUsername());
        period.setRemark(appendRemark(period.getRemark(), "结转操作"));
        finAccountingPeriodMapper.updateFinAccountingPeriod(period);

        // 执行分润计算（如果净利大于0）
        BigDecimal netProfit = nvl(period.getNetProfit());
        if (netProfit.compareTo(BigDecimal.ZERO) > 0) {
            try {
                finProfitShareRecordService.carryForwardPeriod(period.getPeriodId());
            } catch (ServiceException e) {
                // 分润配置未就绪时仅记录，不阻断结转
                period.setRemark(appendRemark(period.getRemark(), "分润跳过：" + e.getMessage()));
                finAccountingPeriodMapper.updateFinAccountingPeriod(period);
            }
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
        if (deptId == null) {
            throw new ServiceException("机构不能为空");
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

        // 5. 作废该周期的分润记录
        FinProfitShareRecord shareQuery = new FinProfitShareRecord();
        shareQuery.setDeptId(deptId);
        shareQuery.setPeriodId(carriedPeriod.getPeriodId());
        List<FinProfitShareRecord> shares = finProfitShareRecordService.selectFinProfitShareRecordList(shareQuery);
        if (shares != null && !shares.isEmpty()) {
            Long[] shareIds = shares.stream().map(FinProfitShareRecord::getShareId).toArray(Long[]::new);
            finProfitShareRecordService.deleteFinProfitShareRecordByShareIds(shareIds);
        }

        // 6. 回退周期状态为进行中
        carriedPeriod.setStatus(PeriodStatus.ACTIVE);
        carriedPeriod.setCarryForwardTime(null);
        carriedPeriod.setEndTime(null);
        carriedPeriod.setUpdateBy(SecurityUtils.getUsername());
        carriedPeriod.setRemark(appendRemark(carriedPeriod.getRemark(), "结转回退操作"));
        finAccountingPeriodMapper.updateFinAccountingPeriod(carriedPeriod);

        // 刷新统计数据
        return refreshPeriodStats(carriedPeriod);
    }

    public void assertPeriodEditable(Long periodId) {
        if (periodId == null) {
            return;
        }
        FinAccountingPeriod period = finAccountingPeriodMapper.selectFinAccountingPeriodByPeriodId(periodId);
        if (period != null && !PeriodStatus.ACTIVE.equals(period.getStatus())) {
            throw new ServiceException("核算周期已结转，不能修改历史流水");
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

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
