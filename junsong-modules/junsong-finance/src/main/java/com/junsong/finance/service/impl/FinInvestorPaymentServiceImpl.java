package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinInvestorPayment;
import com.junsong.finance.domain.vo.InvestorPaymentSummary;
import com.junsong.finance.mapper.FinInvestorPaymentMapper;
import com.junsong.finance.mapper.FinAdvanceMapper;
import com.junsong.finance.mapper.FinInvestRecordMapper;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinInvestorPaymentService;
import com.junsong.finance.util.CodeGenerator;

@Service
public class FinInvestorPaymentServiceImpl implements IFinInvestorPaymentService
{
    @Autowired
    private FinInvestorPaymentMapper finInvestorPaymentMapper;

    @Autowired
    private FinAdvanceMapper finAdvanceMapper;

    @Autowired
    private FinInvestRecordMapper finInvestRecordMapper;

    @Autowired
    private IFinAccountingPeriodService finAccountingPeriodService;

    @Override
    public FinInvestorPayment selectFinInvestorPaymentByPaymentId(Long paymentId)
    {
        return finInvestorPaymentMapper.selectFinInvestorPaymentByPaymentId(paymentId);
    }

    @Override
    public List<FinInvestorPayment> selectFinInvestorPaymentList(FinInvestorPayment finInvestorPayment)
    {
        return finInvestorPaymentMapper.selectFinInvestorPaymentList(finInvestorPayment);
    }

    @Transactional
    @Override
    public int insertFinInvestorPayment(FinInvestorPayment finInvestorPayment)
    {
        finInvestorPayment.setDeptId(SecurityUtils.getDeptId());
        // DB 唯一键兜底：从 AOP ThreadLocal 读取幂等键填充到业务表
        finInvestorPayment.setTenantId(com.junsong.common.core.context.TenantContext.getTenantId());
        finInvestorPayment.setIdempotencyKey(com.junsong.common.core.idempotency.IdempotencyResultStore.currentKey());
        if (StringUtils.isEmpty(finInvestorPayment.getSourceType()))
        {
            finInvestorPayment.setSourceType("0");
        }
        if ("1".equals(finInvestorPayment.getSourceType()))
        {
            throw new ServiceException("结转自动返款只能由分润结转生成");
        }
        fillCurrentPeriod(finInvestorPayment);
        finAccountingPeriodService.assertPeriodEditable(finInvestorPayment.getPeriodId());
        normalizeManualPaymentType(finInvestorPayment);
        validatePaymentAmount(finInvestorPayment);
        if (StringUtils.isEmpty(finInvestorPayment.getPaymentStatus()))
        {
            finInvestorPayment.setPaymentStatus("1");
        }

        // 自动生成返款单号（带重试机制防止并发重复）
        if (StringUtils.isEmpty(finInvestorPayment.getPaymentNo()))
        {
            int retryCount = 0;
            int maxRetries = 3;
            while (retryCount < maxRetries) {
                try {
                    int todayCount = finInvestorPaymentMapper.countTodayInvestorPayments();
                    finInvestorPayment.setPaymentNo(CodeGenerator.generateInvestorPaymentNo(todayCount + retryCount));
                    return finInvestorPaymentMapper.insertFinInvestorPayment(finInvestorPayment);
                } catch (DuplicateKeyException e) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        throw new ServiceException("返款单号生成失败，请稍后重试");
                    }
                }
            }
        }

        return finInvestorPaymentMapper.insertFinInvestorPayment(finInvestorPayment);
    }

    @Transactional
    @Override
    public int updateFinInvestorPayment(FinInvestorPayment finInvestorPayment)
    {
        FinInvestorPayment oldPayment = finInvestorPaymentMapper.selectFinInvestorPaymentByPaymentId(finInvestorPayment.getPaymentId());
        if (oldPayment != null && "1".equals(oldPayment.getSourceType()))
        {
            throw new ServiceException("结转自动生成的返款记录不允许手工修改");
        }
        if (oldPayment != null)
        {
            assertPaymentPeriodEditable(oldPayment);
        }
        finAccountingPeriodService.assertPeriodEditable(finInvestorPayment.getPeriodId());
        finInvestorPayment.setSourceType("0");
        normalizeManualPaymentType(finInvestorPayment);
        validatePaymentAmount(finInvestorPayment);
        return finInvestorPaymentMapper.updateFinInvestorPayment(finInvestorPayment);
    }

    @Transactional
    @Override
    public int deleteFinInvestorPaymentByPaymentIds(Long[] paymentIds)
    {
        if (paymentIds != null)
        {
            for (Long paymentId : paymentIds)
            {
                checkManualPayment(paymentId, "结转自动生成的返款记录不允许手工删除");
                FinInvestorPayment payment = finInvestorPaymentMapper.selectFinInvestorPaymentByPaymentId(paymentId);
                assertPaymentPeriodEditable(payment);
            }
        }
        return finInvestorPaymentMapper.deleteFinInvestorPaymentByPaymentIds(paymentIds);
    }

    @Transactional
    @Override
    public int deleteFinInvestorPaymentByPaymentId(Long paymentId)
    {
        checkManualPayment(paymentId, "结转自动生成的返款记录不允许手工删除");
        FinInvestorPayment payment = finInvestorPaymentMapper.selectFinInvestorPaymentByPaymentId(paymentId);
        assertPaymentPeriodEditable(payment);
        return finInvestorPaymentMapper.deleteFinInvestorPaymentByPaymentId(paymentId);
    }

    @Override
    public boolean checkPaymentNoUnique(FinInvestorPayment finInvestorPayment)
    {
        Long paymentId = finInvestorPayment.getPaymentId() == null ? -1L : finInvestorPayment.getPaymentId();
        FinInvestorPayment info = finInvestorPaymentMapper.checkPaymentNoUnique(finInvestorPayment.getPaymentNo());
        if (StringUtils.isNotNull(info) && info.getPaymentId().longValue() != paymentId.longValue())
        {
            return false;
        }
        return true;
    }

    @Override
    public InvestorPaymentSummary getInvestorPaymentSummary()
    {
        return getInvestorPaymentSummary(null);
    }

    @Override
    public InvestorPaymentSummary getInvestorPaymentSummary(Long deptId)
    {
        InvestorPaymentSummary summary = new InvestorPaymentSummary();

        java.math.BigDecimal totalInvestAmount = nvl(finInvestRecordMapper.sumInvestAmount(deptId));
        java.math.BigDecimal totalReturnAmount = nvl(finInvestorPaymentMapper.sumTotalReturnAmountByDeptId(deptId));
        java.math.BigDecimal reserveFund = totalInvestAmount.subtract(totalReturnAmount);

        summary.setTotalInvestAmount(totalInvestAmount);
        summary.setTotalReturnAmount(totalReturnAmount);
        summary.setReserveFund(reserveFund);
        summary.setVerifiedExpenseAmount(nvl(finInvestorPaymentMapper.sumVerifiedExpensesByDeptId(deptId)));
        summary.setUnverifiedAdvanceAmount(nvl(finAdvanceMapper.sumUnverifiedAdvancesByDeptId(deptId)));

        // 费用余额 = 投资资金余额 - 已核销费用 - 未核销借支，仅用于资金占用展示，不参与回本判断。
        if (summary.getReserveFund() != null && summary.getVerifiedExpenseAmount() != null && summary.getUnverifiedAdvanceAmount() != null)
        {
            java.math.BigDecimal balance = summary.getReserveFund()
                .subtract(summary.getVerifiedExpenseAmount())
                .subtract(summary.getUnverifiedAdvanceAmount());
            summary.setStoreExpenseBalance(balance);
            summary.setExpenseBalance(balance);
        }
        
        return summary;
    }

    private java.math.BigDecimal nvl(java.math.BigDecimal value)
    {
        return value == null ? java.math.BigDecimal.ZERO : value;
    }

    private void normalizeManualPaymentType(FinInvestorPayment payment)
    {
        if (StringUtils.isEmpty(payment.getPaymentType()))
        {
            payment.setPaymentType("return");
        }
        if (!"return".equals(payment.getPaymentType()))
        {
            throw new ServiceException("投资来源请在投资来源记录中维护，投资人返款只允许登记返款");
        }
    }

    private void fillCurrentPeriod(FinInvestorPayment payment)
    {
        if (payment.getPeriodId() == null && payment.getDeptId() != null)
        {
            FinAccountingPeriod period = finAccountingPeriodService.initCurrentPeriod(payment.getDeptId());
            payment.setPeriodId(period.getPeriodId());
        }
    }

    private void validatePaymentAmount(FinInvestorPayment payment)
    {
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("返款金额必须大于0");
        }
    }

    private void checkManualPayment(Long paymentId, String message)
    {
        FinInvestorPayment payment = finInvestorPaymentMapper.selectFinInvestorPaymentByPaymentId(paymentId);
        if (payment != null && "1".equals(payment.getSourceType()))
        {
            throw new ServiceException(message);
        }
    }

    private void assertPaymentPeriodEditable(FinInvestorPayment payment)
    {
        if (payment != null)
        {
            finAccountingPeriodService.assertPeriodEditable(payment.getPeriodId());
        }
    }
}
