package com.junsong.system.service;

import com.junsong.system.domain.vo.EnterpriseHardeningDashboardVO;

/**
 * R25企业级硬化看板 服务层
 */
public interface IEnterpriseHardeningService
{
    /**
     * 获取硬化看板数据
     */
    EnterpriseHardeningDashboardVO getDashboard();
}
