package com.junsong.finance.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinInvestRecord;

public interface FinInvestRecordMapper
{
    public FinInvestRecord selectFinInvestRecordByInvestId(Long investId);
    public List<FinInvestRecord> selectFinInvestRecordList(FinInvestRecord finInvestRecord);
    public int insertFinInvestRecord(FinInvestRecord finInvestRecord);
    public int updateFinInvestRecord(FinInvestRecord finInvestRecord);
    public int deleteFinInvestRecordByInvestId(Long investId);
    public int deleteFinInvestRecordByInvestIds(Long[] investIds);
    public BigDecimal sumInvestAmount(@Param("deptId") Long deptId);
    public BigDecimal sumInvestAmountByDeptId(@Param("deptId") Long deptId);
}
