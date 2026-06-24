package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinProfitShareRecord;

public interface IFinProfitShareRecordService
{
    public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId);
    public FinProfitShareRecord carryForwardPeriod(Long periodId);
    public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord finProfitShareRecord);
    public int insertFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord);
    public int updateFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord);
    public int deleteFinProfitShareRecordByShareIds(Long[] shareIds);
}
