package com.junsong.finance.service;

import java.math.BigDecimal;

/**
 * 库存成本计价服务（移动加权平均法）。
 *
 * 所有读写按 tenant + dept + product 隔离，与 fin_stock_position 使用相同租户键和锁顺序。
 * BigDecimal 中间单位成本 scale 6，最终金额 scale 2，均 HALF_UP。
 * 销售成本取出库瞬间固化成本，禁止读取当前商品采购价回算历史成本。
 *
 * @author junsong
 */
public interface IStockCostService {

    /**
     * 采购入库：按本次入库数量和金额更新移动加权平均成本。
     * 赠品数量计入 quantity 但不计入 amount（金额为 0），从而摊薄平均成本。
     *
     * @param quantity 入库数量（正数）
     * @param amount 入库金额（2位小数，赠品为 0）
     * @param sourceLedgerId 来源单据ID（采购单ID），用于成本流水追溯
     */
    void applyPurchaseInbound(Long tenantId, Long deptId, Long productId,
                              int quantity, BigDecimal amount,
                              Long sourceLedgerId, String operator);

    /**
     * 采购冲销：按当前平均成本反向减少库存数量和金额。
     * 移动加权平均法下，采购冲销按当前平均成本逆转（原批次成本已混入平均）。
     *
     * @param reverseQuantity 冲销数量（正数）
     */
    void reversePurchaseInbound(Long tenantId, Long deptId, Long productId,
                                int reverseQuantity,
                                Long sourceLedgerId, String operator);

    /**
     * 销售出库：按出库瞬间的平均成本固化销售成本。
     * 出库后平均成本不变（移动加权平均法：销售不改变平均成本）。
     *
     * @param quantity 出库数量（正数）
     * @param allowNegative 是否允许负库存
     * @return 固化的单位成本（6位小数），调用方应存入库存流水的 unit_cost 字段供后续冲销
     */
    BigDecimal applySaleOutbound(Long tenantId, Long deptId, Long productId,
                                 int quantity, boolean allowNegative,
                                 Long sourceLedgerId, String operator);

    /**
     * 销售冲销：按原销售出库时固化的单位成本反向恢复库存金额。
     * 冲销后重新计算平均成本（视同入库）。
     *
     * @param quantity 冲销数量（正数）
     * @param originalUnitCost 原销售出库时固化的单位成本（6位小数）
     */
    void reverseSaleOutbound(Long tenantId, Long deptId, Long productId,
                             int quantity, BigDecimal originalUnitCost,
                             Long sourceLedgerId, String operator);
}
