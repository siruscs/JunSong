package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinCostAccounting;
import com.junsong.finance.domain.vo.CostAccountingRealTimeVO;
import com.junsong.finance.mapper.FinCostAccountingMapper;
import com.junsong.finance.mapper.FinInvestRecordMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinCostAccountingService;
import com.junsong.finance.util.CodeGenerator;

@Service
public class FinCostAccountingServiceImpl implements IFinCostAccountingService
{
    @Autowired
    private FinCostAccountingMapper finCostAccountingMapper;

    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    @Autowired
    private FinInvestRecordMapper finInvestRecordMapper;

    @Override
    public FinCostAccounting selectFinCostAccountingByAccountingId(Long accountingId)
    {
        return finCostAccountingMapper.selectFinCostAccountingByAccountingId(accountingId);
    }

    @Override
    public List<FinCostAccounting> selectFinCostAccountingList(FinCostAccounting finCostAccounting)
    {
        return finCostAccountingMapper.selectFinCostAccountingList(finCostAccounting);
    }

    @Transactional
    @Override
    public int insertFinCostAccounting(FinCostAccounting finCostAccounting)
    {
        if (StringUtils.isEmpty(finCostAccounting.getAccountingNo()))
        {
            int todayCount = finCostAccountingMapper.countTodayAccountings();
            finCostAccounting.setAccountingNo(CodeGenerator.generateCostAccountingNo(todayCount));
        }

        finCostAccounting.setDeptId(SecurityUtils.getDeptId());

        FinAccountingPeriod currentPeriod = finAccountingPeriodService.initCurrentPeriod(finCostAccounting.getDeptId());
        finCostAccounting.setStartDate(currentPeriod.getStartTime());
        Date endDate = currentPeriod.getEndTime() == null ? currentPeriod.getBreakEvenTime() : currentPeriod.getEndTime();
        finCostAccounting.setEndDate(endDate == null ? new Date() : endDate);
        finCostAccounting.setTotalExpense(nvl(currentPeriod.getTotalVerifiedExpense()));
        finCostAccounting.setTotalPurchase(nvl(currentPeriod.getTotalPurchase()));
        finCostAccounting.setTotalSale(nvl(currentPeriod.getTotalSaleAmount()));
        finCostAccounting.setTotalPayment(nvl(currentPeriod.getTotalSalePayment()));
        finCostAccounting.setTotalInvest(nvl(finInvestRecordMapper.sumInvestAmountByDeptId(finCostAccounting.getDeptId())));
        finCostAccounting.setCurrentAdvance(nvl(currentPeriod.getTotalUnverifiedAdvance()));
        finCostAccounting.setReturnSituation(nvl(currentPeriod.getNetProfit()));

        return finCostAccountingMapper.insertFinCostAccounting(finCostAccounting);
    }

    @Override
    public boolean checkUnverifiedExpense()
    {
        return checkUnverifiedExpense(null);
    }

    @Override
    public boolean checkUnverifiedExpense(Long deptId)
    {
        Long queryDeptId = deptId == null ? SecurityUtils.getDeptId() : deptId;
        return finCostAccountingMapper.countUnverifiedExpense(queryDeptId) > 0;
    }

    @Transactional
    @Override
    public int updateFinCostAccounting(FinCostAccounting finCostAccounting)
    {
        return finCostAccountingMapper.updateFinCostAccounting(finCostAccounting);
    }

    @Transactional
    @Override
    public int deleteFinCostAccountingByAccountingIds(Long[] accountingIds)
    {
        return finCostAccountingMapper.deleteFinCostAccountingByAccountingIds(accountingIds);
    }

    @Transactional
    @Override
    public int deleteFinCostAccountingByAccountingId(Long accountingId)
    {
        return finCostAccountingMapper.deleteFinCostAccountingByAccountingId(accountingId);
    }

    @Override
    public boolean checkAccountingNoUnique(FinCostAccounting finCostAccounting)
    {
        Long accountingId = finCostAccounting.getAccountingId() == null ? -1L : finCostAccounting.getAccountingId();
        FinCostAccounting info = finCostAccountingMapper.checkAccountingNoUnique(finCostAccounting.getAccountingNo());
        if (StringUtils.isNotNull(info) && info.getAccountingId().longValue() != accountingId.longValue())
        {
            return false;
        }
        return true;
    }

    @Override
    public CostAccountingRealTimeVO getRealTimeStats()
    {
        return getRealTimeStats(null);
    }

    @Override
    public CostAccountingRealTimeVO getRealTimeStats(Long deptId)
    {
        Long queryDeptId = deptId == null ? SecurityUtils.getDeptId() : deptId;
        CostAccountingRealTimeVO vo = new CostAccountingRealTimeVO();

        if (queryDeptId == null)
        {
            vo.setTotalExpenseAmount(nvl(finCostAccountingMapper.sumTotalExpenseAmount()));
            vo.setTotalPurchaseAmount(nvl(finCostAccountingMapper.sumTotalPurchaseAmount()));
            vo.setTotalSaleAmount(nvl(finCostAccountingMapper.sumTotalSaleAmount()));
            vo.setTotalPaymentAmount(nvl(finCostAccountingMapper.sumTotalPaymentAmount()));
            vo.setTotalInvestAmount(nvl(finInvestRecordMapper.sumInvestAmount(null)));
            vo.setCurrentAdvance(nvl(finCostAccountingMapper.sumCurrentAdvance()));
            vo.setProfit(vo.getTotalPaymentAmount().subtract(vo.getTotalExpenseAmount()).subtract(vo.getTotalPurchaseAmount()).subtract(vo.getCurrentAdvance()));
            return vo;
        }

        FinAccountingPeriod currentPeriod = finAccountingPeriodService.initCurrentPeriod(queryDeptId);
        vo.setPeriodId(currentPeriod.getPeriodId());
        vo.setPeriodNo(currentPeriod.getPeriodNo());
        vo.setPeriodStartTime(currentPeriod.getStartTime());
        vo.setPeriodStatus(currentPeriod.getStatus());
        vo.setBreakEvenTime(currentPeriod.getBreakEvenTime());
        vo.setTotalExpenseAmount(nvl(currentPeriod.getTotalVerifiedExpense()));
        vo.setTotalPurchaseAmount(nvl(currentPeriod.getTotalPurchase()));
        vo.setTotalSaleAmount(nvl(currentPeriod.getTotalSaleAmount()));
        vo.setTotalPaymentAmount(nvl(currentPeriod.getTotalSalePayment()));
        vo.setTotalInvestAmount(nvl(finInvestRecordMapper.sumInvestAmountByDeptId(queryDeptId)));
        vo.setCurrentAdvance(nvl(currentPeriod.getTotalUnverifiedAdvance()));
        vo.setProfit(nvl(currentPeriod.getNetProfit()));
        return vo;
    }

    @Override
    public CostAccountingRealTimeVO getPreviewStats(String startDate, String endDate)
    {
        return getPreviewStats(startDate, endDate, null);
    }

    @Override
    public CostAccountingRealTimeVO getPreviewStats(String startDate, String endDate, Long deptId)
    {
        Long queryDeptId = deptId == null ? SecurityUtils.getDeptId() : deptId;
        if (queryDeptId != null)
        {
            return getRealTimeStats(queryDeptId);
        }
        return getRealTimeStats(null);
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }
}
