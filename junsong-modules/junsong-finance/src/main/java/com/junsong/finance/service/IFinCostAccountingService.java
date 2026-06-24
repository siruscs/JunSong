package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinCostAccounting;
import com.junsong.finance.domain.vo.CostAccountingRealTimeVO;

public interface IFinCostAccountingService
{
    public FinCostAccounting selectFinCostAccountingByAccountingId(Long accountingId);
    public List<FinCostAccounting> selectFinCostAccountingList(FinCostAccounting finCostAccounting);
    public int insertFinCostAccounting(FinCostAccounting finCostAccounting);
    public int updateFinCostAccounting(FinCostAccounting finCostAccounting);
    public int deleteFinCostAccountingByAccountingIds(Long[] accountingIds);
    public int deleteFinCostAccountingByAccountingId(Long accountingId);
    public boolean checkAccountingNoUnique(FinCostAccounting finCostAccounting);
    public CostAccountingRealTimeVO getRealTimeStats();
    public CostAccountingRealTimeVO getRealTimeStats(Long deptId);
    public CostAccountingRealTimeVO getPreviewStats(String startDate, String endDate);
    public CostAccountingRealTimeVO getPreviewStats(String startDate, String endDate, Long deptId);
    public boolean checkUnverifiedExpense();
    public boolean checkUnverifiedExpense(Long deptId);
}
