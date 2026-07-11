package com.junsong.finance.service;

import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.vo.ExpenseOperationCapabilityVO;
import com.junsong.finance.domain.vo.ExpenseUnverifyVO;
import com.junsong.finance.domain.vo.ExpenseVerifyVO;

public interface IFinExpenseVerificationService
{
    Long verify(ExpenseVerifyVO request, String operator);
    int unverify(Long batchId, ExpenseUnverifyVO request, String operator);
    ExpenseOperationCapabilityVO getCapability(Long expenseId);
    FinExpense getVerificationCandidate(Long expenseId);
}
