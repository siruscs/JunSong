package com.junsong.finance.service;

import java.util.List;

import com.junsong.finance.domain.vo.StockHealthVO;
import com.junsong.finance.domain.vo.StockReconciliationResultVO;

/**
 * 库存底座健康检查服务。
 *
 * @author junsong
 */
public interface IStockHealthService {

    /**
     * 评估库存底座健康状态（按租户 + 门店范围）。
     *
     * @param tenantId 租户ID（必填，null 时 fail-closed 抛出 ServiceException）
     * @param deptIds  门店ID列表，null/空表示该租户下全部门店
     * @return 健康检查结果
     */
    StockHealthVO checkHealth(Long tenantId, List<Long> deptIds);

    /**
     * 库存存量对账（只读，不修改任何数据）。
     *
     * <p>按四类异常规则检测 (tenantId, deptId, productId) 维度的不一致行：
     * <ul>
     *   <li>POSITION_WITHOUT_LEDGER</li>
     *   <li>LEDGER_POSITION_MISMATCH</li>
     *   <li>SNAPSHOT_EQUATION_MISMATCH</li>
     *   <li>LATEST_SNAPSHOT_MISMATCH</li>
     * </ul></p>
     *
     * @param tenantId 租户ID（必填，null 时 fail-closed 抛出 ServiceException）
     * @param deptIds  门店ID列表，null/空表示该租户下全部门店
     * @return 对账结果，包含全部异常明细行、异常计数与整体状态
     */
    StockReconciliationResultVO reconcileStock(Long tenantId, List<Long> deptIds);
}
