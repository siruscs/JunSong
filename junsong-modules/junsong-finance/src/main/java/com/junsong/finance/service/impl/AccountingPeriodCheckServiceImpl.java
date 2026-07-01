package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinAdvance;
import com.junsong.finance.domain.FinInvestorPayment;
import com.junsong.finance.domain.vo.AccountingPeriodCheckItemVO;
import com.junsong.finance.domain.vo.AccountingPeriodCheckResultVO;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinAdvanceMapper;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinInvestorPaymentMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.service.IAccountingPeriodCheckService;

/**
 * 核算周期锁账前检查服务实现
 *
 * @author junsong
 */
@Service
public class AccountingPeriodCheckServiceImpl implements IAccountingPeriodCheckService
{
    @Autowired
    private FinExpenseMapper finExpenseMapper;

    @Autowired
    private FinAdvanceMapper finAdvanceMapper;

    @Autowired
    private FinProfitShareRecordMapper finProfitShareRecordMapper;

    @Autowired
    private FinInvestorPaymentMapper finInvestorPaymentMapper;

    @Autowired
    private FinAccountingPeriodMapper finAccountingPeriodMapper;

    @Override
    public AccountingPeriodCheckResultVO checkBeforeLock(Long deptId)
    {
        List<Long> deptIds = Collections.singletonList(deptId);
        List<AccountingPeriodCheckItemVO> items = new ArrayList<>();

        // Look up current period first for period-filtered queries
        FinAccountingPeriod currentPeriod = finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptId);
        Long currentPeriodId = currentPeriod != null ? currentPeriod.getPeriodId() : null;

        // 1. 未核销费用 → WARNING (filtered by current periodId if available)
        int expenseCount;
        BigDecimal expenseAmount;
        if (currentPeriodId != null)
        {
            expenseCount = finExpenseMapper.countUnverifiedExpensesByPeriodId(deptIds, currentPeriodId);
            expenseAmount = finExpenseMapper.sumUnverifiedExpenseAmountByPeriodId(deptIds, currentPeriodId);
        }
        else
        {
            expenseCount = finExpenseMapper.countUnverifiedExpenses(deptIds);
            expenseAmount = finExpenseMapper.sumUnverifiedExpenseAmount(deptIds);
        }
        items.add(new AccountingPeriodCheckItemVO(
                "UNVERIFIED_EXPENSE",
                "WARNING",
                "未核销费用",
                "存在" + expenseCount + "笔未核销费用，总金额¥" + expenseAmount.setScale(2, java.math.RoundingMode.HALF_UP),
                expenseCount,
                expenseAmount != null ? expenseAmount : BigDecimal.ZERO
        ));

        // 2. 未核销借支 → INFO (filtered by current periodId if available)
        FinAdvance advanceQuery = new FinAdvance();
        advanceQuery.setDeptId(deptId);
        advanceQuery.setStatus("0");
        if (currentPeriodId != null)
        {
            advanceQuery.setPeriodId(currentPeriodId);
        }
        List<FinAdvance> advances = finAdvanceMapper.selectFinAdvanceList(advanceQuery);
        int advanceCount = advances.size();
        BigDecimal advanceAmount = advances.stream()
                .map(a -> a.getAdvanceAmount() != null ? a.getAdvanceAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        items.add(new AccountingPeriodCheckItemVO(
                "UNVERIFIED_ADVANCE",
                "INFO",
                "未核销借支",
                "存在" + advanceCount + "笔未核销借支，总金额¥" + advanceAmount.setScale(2, java.math.RoundingMode.HALF_UP),
                advanceCount,
                advanceAmount
        ));

        // 3. 未结算分润 → BLOCK (filtered by current periodId if available)
        int unsettledCount;
        if (currentPeriodId != null)
        {
            unsettledCount = finProfitShareRecordMapper.countUnsettledRecordsByPeriodId(deptIds, currentPeriodId);
        }
        else
        {
            unsettledCount = finProfitShareRecordMapper.countUnsettledRecords(deptIds);
        }
        items.add(new AccountingPeriodCheckItemVO(
                "UNSETTLED_PROFIT_SHARE",
                "BLOCK",
                "未结算分润",
                "存在" + unsettledCount + "笔未结算的分润记录，必须先完成结算",
                unsettledCount,
                BigDecimal.ZERO
        ));

        // 4. 未返款投资人 → WARNING (filtered by current periodId if available)
        FinInvestorPayment paymentQuery = new FinInvestorPayment();
        paymentQuery.setDeptId(deptId);
        paymentQuery.setPaymentStatus("0");
        if (currentPeriodId != null)
        {
            paymentQuery.setPeriodId(currentPeriodId);
        }
        List<FinInvestorPayment> payments = finInvestorPaymentMapper.selectFinInvestorPaymentList(paymentQuery);
        int paymentCount = payments.size();
        BigDecimal paymentAmount = payments.stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        items.add(new AccountingPeriodCheckItemVO(
                "UNPAID_INVESTOR",
                "WARNING",
                "未返款投资人",
                "存在" + paymentCount + "笔未返款记录，总金额¥" + paymentAmount.setScale(2, java.math.RoundingMode.HALF_UP),
                paymentCount,
                paymentAmount
        ));

        // 判断 canLock / hasWarning
        boolean hasBlock = items.stream()
                .anyMatch(i -> "BLOCK".equals(i.getLevel()) && i.getCount() > 0);
        boolean hasWarning = items.stream()
                .anyMatch(i -> "WARNING".equals(i.getLevel()) && i.getCount() > 0);

        AccountingPeriodCheckResultVO result = new AccountingPeriodCheckResultVO();
        result.setCanLock(!hasBlock);
        result.setHasWarning(hasWarning);
        result.setDeptId(deptId);
        result.setItems(items);

        // 填充当前周期信息
        if (currentPeriod != null)
        {
            result.setPeriodId(currentPeriod.getPeriodId());
            result.setPeriodName(currentPeriod.getPeriodNo());
        }

        return result;
    }
}
