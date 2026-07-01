package com.junsong.finance.service;

import com.junsong.finance.domain.vo.AccountingPeriodCheckResultVO;

/**
 * 核算周期锁账前检查服务接口
 *
 * @author junsong
 */
public interface IAccountingPeriodCheckService
{
    /**
     * 锁账前检查：检查指定部门下是否存在阻塞或警告项
     *
     * @param deptId 部门ID
     * @return 检查结果
     */
    AccountingPeriodCheckResultVO checkBeforeLock(Long deptId);
}
