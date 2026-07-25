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

    /**
     * 成本调整：在当前 ACTIVE 期间生成有原因的成本调整流水。
     * 仅更新库存金额和平均成本，不改变库存数量；写入 COST_ADJUST 流水。
     * 调用方必须先校验期间状态为 ACTIVE 并要求非空原因。
     *
     * @param amount 调整金额（正数调增，负数调减）
     * @param reason 调整原因（非空）
     * @param operator 操作者
     */
    void applyCostAdjustment(Long tenantId, Long deptId, Long productId,
                             BigDecimal amount, String reason, String operator);

    /**
     * 盘点盘亏：按当前移动加权平均成本减少库存金额，库存数量同步减少。
     * 写入 STOCKTAKE_LOSS_OUT 成本流水（不可变审计记录）。
     *
     * 业务规则：
     * - quantity 必须为正数（表示盘亏的绝对量）
     * - 按当前 avgUnitCost * quantity 计算盘亏金额
     * - 平均成本不变（与销售出库一致：出库不改变平均成本）
     * - 不允许负库存（盘亏后库存 < 0 拒绝）
     *
     * @param quantity 盘亏数量（正数）
     * @param sourceLedgerId 来源库存流水ID（盘点行 stockLedgerId），用于成本可追溯
     * @param operator 操作者
     * @return 成本流水ID（FinStockCostLedger.costLedgerId）
     */
    Long applyStocktakeLoss(Long tenantId, Long deptId, Long productId,
                            int quantity, Long sourceLedgerId, String operator);

    /**
     * 盘点盘盈：按指定金额增加库存金额，库存数量同步增加。
     * 写入 STOCKTAKE_GAIN_IN 成本流水（不可变审计记录）。
     *
     * 业务规则：
     * - quantity 必须为正数（表示盘盈的绝对量）
     * - amount=null 时按当前平均成本 × 数量计算入账金额（默认估值，财务可后续调整）
     * - amount 非 null 时按指定金额入账（财务确认的评估价等）
     * - 盘盈后重新计算平均成本（视同入库）
     *
     * @param quantity 盘盈数量（正数）
     * @param amount 盘盈金额（2位小数，非负）；null 表示按当前平均成本估值
     * @param sourceLedgerId 来源库存流水ID（盘点行 stockLedgerId），用于成本可追溯
     * @param operator 操作者
     * @return 成本流水ID（FinStockCostLedger.costLedgerId）
     */
    Long applyStocktakeGain(Long tenantId, Long deptId, Long productId,
                            int quantity, BigDecimal amount,
                            Long sourceLedgerId, String operator);

    /**
     * 盘点调整冲销：按原固化单位成本反向恢复/扣减库存金额与数量。
     * 写入 COST_REVERSE_IN 或 COST_REVERSE_OUT 成本流水（取决于原调整方向）。
     *
     * 业务规则：
     * - quantity 为正数（冲销的绝对量）
     * - unitCost 为原盘点过账时固化的单位成本
     * - 原盘亏冲销 → 数量增加、金额按原成本回补（COST_REVERSE_IN）
     * - 原盘盈冲销 → 数量减少、金额按原成本扣减（COST_REVERSE_OUT）
     * - 不允许冲销后库存 < 0
     *
     * @param quantity 冲销数量（正数）
     * @param unitCost 原固化单位成本
     * @param sourceLedgerId 来源库存流水ID（冲销行的 stockLedgerId）
     * @param originalCostLedgerId 原过账成本流水ID
     * @param operator 操作者
     * @return 冲销成本流水ID
     */
    Long reverseStocktakeAdjustment(Long tenantId, Long deptId, Long productId,
                                    int quantity, BigDecimal unitCost,
                                    Long sourceLedgerId, Long originalCostLedgerId,
                                    String operator);

    /**
     * 查询原过账成本流水的固化单位成本（Task 7 冲销前置依赖）。
     *
     * 用途：盘点整单冲销时，行表的 unitCost 字段可能在 Task 6 过账时未固化（为 null）。
     * 此方法通过 costLedgerId 查询原成本流水，返回其 unitCost，确保冲销金额与原过账一致。
     *
     * 安全契约：
     * - 校验 costLedgerId 归属当前租户（防止跨租户读取）
     * - 校验 costLedger 类型为盘点调整（STOCKTAKE_LOSS_OUT / STOCKTAKE_GAIN_IN）
     * - 不存在或不匹配时返回 null（调用方必须 fail-closed）
     *
     * @param tenantId 租户ID
     * @param costLedgerId 原过账成本流水ID
     * @return 原固化单位成本（6位小数）；不存在或不匹配返回 null
     */
    BigDecimal getCostLedgerUnitCost(Long tenantId, Long costLedgerId);
}
