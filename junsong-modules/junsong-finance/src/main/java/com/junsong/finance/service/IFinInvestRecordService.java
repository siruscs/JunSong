package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinInvestRecord;

public interface IFinInvestRecordService
{
    public FinInvestRecord selectFinInvestRecordByInvestId(Long investId);
    public List<FinInvestRecord> selectFinInvestRecordList(FinInvestRecord finInvestRecord);
    public int insertFinInvestRecord(FinInvestRecord finInvestRecord);
    public int updateFinInvestRecord(FinInvestRecord finInvestRecord);
    public int deleteFinInvestRecordByInvestIds(Long[] investIds);
}
