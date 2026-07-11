package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinProfitShareRecord;

public interface IFinProfitShareRecordService
{
    public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId);
    public FinProfitShareRecord carryForwardPeriod(Long periodId);
    /**
     * 预检查分润配置是否就绪（不开启事务，避免影响调用方事务）
     * @param deptId 机构ID
     * @throws com.junsong.common.core.exception.ServiceException 配置缺失或未启用时抛出
     */
    public void checkProfitConfigReady(Long deptId);
    public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord finProfitShareRecord);
    public int insertFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord);
    public int updateFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord);
    public int deleteFinProfitShareRecordByShareIds(Long[] shareIds);
}
