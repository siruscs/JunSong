package com.junsong.system.service;

import com.junsong.system.domain.vo.DataQualityDashboardVO;

/**
 * 数据质量服务接口。
 * R20: 只读聚合现有表问题，不写数据库。
 */
public interface ISysDataQualityService {

    /**
     * 获取数据质量看板数据。
     * @return 看板 VO，包含状态、统计和问题明细
     */
    DataQualityDashboardVO getDashboard();
}
