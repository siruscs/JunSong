package com.junsong.finance.service;

import com.junsong.finance.domain.vo.StockTakeRequest;

/**
 * 库存盘点服务接口。
 *
 * 盘点流程：
 * 1. 校验租户上下文、部门授权、商品归属
 * 2. 加行锁查询当前库存
 * 3. 计算盘盈/盘亏差额
 * 4. 写入 STOCK_TAKE_GAIN 或 STOCK_TAKE_LOSS 流水
 * 5. 更新当前库存结存
 * 6. 联动成本层（盘亏时按移动加权平均成本出账）
 *
 * 幂等：基于 takeNo 查询是否已存在盘点流水，存在则拒绝重复提交。
 *
 * @author junsong
 */
public interface IStockTakeService {

    /**
     * 录入盘点结果。
     *
     * @param request 盘点请求
     * @return 盘点流水ID
     */
    Long recordStockTake(StockTakeRequest request);
}
