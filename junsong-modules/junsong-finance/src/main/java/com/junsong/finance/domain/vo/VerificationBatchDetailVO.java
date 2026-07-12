package com.junsong.finance.domain.vo;

import com.junsong.finance.domain.FinAdvanceVerifyDetail;
import com.junsong.finance.domain.FinExpenseVerifyBatch;
import com.junsong.finance.domain.FinExpenseVerifyDetail;
import java.util.List;

/** 核销批次详情（含费用明细和借支明细快照）。 */
public class VerificationBatchDetailVO
{
    private FinExpenseVerifyBatch batch;
    private List<FinExpenseVerifyDetail> expenseDetails;
    private List<FinAdvanceVerifyDetail> advanceDetails;

    public FinExpenseVerifyBatch getBatch() { return batch; }
    public void setBatch(FinExpenseVerifyBatch batch) { this.batch = batch; }
    public List<FinExpenseVerifyDetail> getExpenseDetails() { return expenseDetails; }
    public void setExpenseDetails(List<FinExpenseVerifyDetail> expenseDetails) { this.expenseDetails = expenseDetails; }
    public List<FinAdvanceVerifyDetail> getAdvanceDetails() { return advanceDetails; }
    public void setAdvanceDetails(List<FinAdvanceVerifyDetail> advanceDetails) { this.advanceDetails = advanceDetails; }
}
