package com.junsong.finance.service;

import com.junsong.finance.domain.vo.CashflowDashboardVO;

import java.util.Date;
import java.util.List;

/**
 * 轻量现金流看板 Service
 *
 * @author junsong
 */
public interface ICashflowDashboardService {

    /**
     * 获取现金流看板数据
     *
     * @param deptIds   授权部门ID列表
     * @param startTime 时间范围起（可为空，默认当月月初）
     * @param endTime   时间范围止（可为空，默认当前时间）
     * @return 看板 VO
     */
    CashflowDashboardVO getCashflowDashboard(List<Long> deptIds, Date startTime, Date endTime);
}
