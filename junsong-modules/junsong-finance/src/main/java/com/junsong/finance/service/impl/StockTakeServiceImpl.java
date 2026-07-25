package com.junsong.finance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.vo.StockTakeRequest;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IStockTakeService;

/**
 * 库存盘点服务实现（Task 8：旧接口收口）。
 *
 * 收口策略：fail-closed 迁移响应。
 * 旧 POST /stockTake 直接改库存的通道已关闭，所有调用抛出 ServiceException，
 * 提示使用新工作流 /stocktakes（POST /stocktakes 创建任务 → 盲盘 → 提交 → 审批 → 过账）。
 *
 * 安全契约：
 * 1. 不得调用 insertFinStockLedger / updatePositionQuantity / insertPositionIfAbsent
 * 2. 不得调用 selectPositionQuantityForUpdate / countByReferenceNo
 * 3. 不得调用 selectFinProductByProductIdAndDeptId
 * 4. 保留路由映射，返回明确迁移提示，便于消费者升级
 *
 * 消费者：junsong-miniprogram/src/api/stockTake.js（Task 10 替换为 stocktake.js）。
 *
 * @author junsong
 */
@Service
public class StockTakeServiceImpl implements IStockTakeService {

    /**
     * 保留旧依赖注入以保持 Spring 兼容性，但收口后不再调用。
     * Task 13 独立评审可验证这些字段从未被 recordStockTake 使用。
     */
    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Autowired
    private FinProductMapper finProductMapper;

    @Override
    public Long recordStockTake(StockTakeRequest request) {
        // Task 8: 旧接口收口 —— fail-closed 迁移响应。
        // 旧 POST /stockTake 直接改库存的通道已关闭。
        // 请使用新工作流：POST /stocktakes 创建盘点任务，经过盲盘、提交、审批后过账。
        throw new ServiceException(
                "旧盘点接口 POST /stockTake 已收口，禁止直接修改库存。"
                + "请使用新工作流 /stocktakes：创建任务 → 盲盘 → 提交 → 审批 → 过账。");
    }
}
