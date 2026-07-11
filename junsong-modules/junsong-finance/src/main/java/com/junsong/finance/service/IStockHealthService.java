package com.junsong.finance.service;

import com.junsong.finance.domain.vo.StockHealthVO;

/**
 * 库存底座健康检查服务。
 *
 * @author junsong
 */
public interface IStockHealthService {

    /**
     * 评估库存底座健康状态。
     *
     * @return 健康检查结果
     */
    StockHealthVO checkHealth();
}