package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinInvestorPayment;

public interface IFinInvestorPaymentService
{
    public FinInvestorPayment selectFinInvestorPaymentByPaymentId(Long paymentId);
    public List<FinInvestorPayment> selectFinInvestorPaymentList(FinInvestorPayment finInvestorPayment);
    public int insertFinInvestorPayment(FinInvestorPayment finInvestorPayment);
    public int updateFinInvestorPayment(FinInvestorPayment finInvestorPayment);
    public int deleteFinInvestorPaymentByPaymentIds(Long[] paymentIds);
    public int deleteFinInvestorPaymentByPaymentId(Long paymentId);
    public boolean checkPaymentNoUnique(FinInvestorPayment finInvestorPayment);
    public com.junsong.finance.domain.vo.InvestorPaymentSummary getInvestorPaymentSummary();
    public com.junsong.finance.domain.vo.InvestorPaymentSummary getInvestorPaymentSummary(Long deptId);
}
