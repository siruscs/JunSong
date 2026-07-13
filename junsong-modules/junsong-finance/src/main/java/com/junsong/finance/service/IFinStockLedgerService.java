package com.junsong.finance.service;

import java.math.BigDecimal;

/**
 * 库存流水写入服务：所有库存变动的统一入口，采用"当前库存表行锁 + 差额对账"模型。
 * 业务模块调用对账方法并传入"该单据对该商品的目标数量"，服务自动计算与已记录净额的差额，
 * 生成正向或反向流水，天然幂等，支持新增/修改/删除全场景，避免库存口径失真。
 *
 * @author junsong
 */
public interface IFinStockLedgerService {

    /**
     * 采购入库对账：将某采购单对某商品的入库量对齐到 targetQuantity。
     * targetQuantity=0 表示该明细被删除或数量清零，将反向冲销已入库量。
     *
     * @param tenantId 租户ID（不可为空，为空立即失败关闭）
     * @param deptId 门店ID
     * @param productId 商品ID
     * @param productName 商品名称
     * @param referenceId 关联采购单ID
     * @param referenceNo 关联采购单号
     * @param targetQuantity 目标入库数量（>=0）
     * @param unitCost 单位成本
     * @param operator 操作人
     */
    void reconcilePurchaseStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                                String referenceNo, Integer targetQuantity, BigDecimal unitCost, String operator);

    /**
     * 采购入库对账（含显式入库金额）：与上方法相同，但显式传入入库总金额。
     * 用于同商品混合普通+赠品明细时，传入剔除赠品后的真实采购金额，
     * 保证赠品数量进入库存但金额不进入成本池。
     *
     * @param inboundAmount 入库总金额（2位小数，赠品金额为0），null 时回退为 unitCost * targetQuantity
     */
    void reconcilePurchaseStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                                String referenceNo, Integer targetQuantity, BigDecimal unitCost,
                                BigDecimal inboundAmount, String operator);

    /**
     * 销售出库对账：将某销售单对某商品的出库量对齐到 targetQuantity。
     * targetQuantity=0 表示该销售被删除或数量清零，将反向回补已出库量。
     *
     * @param tenantId 租户ID（不可为空，为空立即失败关闭）
     * @param deptId 门店ID
     * @param productId 商品ID
     * @param productName 商品名称
     * @param referenceId 关联销售单ID
     * @param referenceNo 关联销售单号
     * @param targetQuantity 目标出库数量（>=0）
     * @param operator 操作人
     */
    void reconcileSaleStock(Long tenantId, Long deptId, Long productId, String productName, Long referenceId,
                            String referenceNo, Integer targetQuantity, String operator);
}