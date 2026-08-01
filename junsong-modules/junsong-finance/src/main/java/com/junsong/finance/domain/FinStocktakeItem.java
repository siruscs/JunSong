package com.junsong.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 库存盘点行表 finance_stocktake_item。
 *
 * 每行对应一个商品的盘点记录：
 * - expected_quantity: 冻结时结存（盲盘时对盘点人不可见）
 * - movement_quantity_after_freeze: 冻结后到过账前的净移动量
 * - adjusted_expected_quantity: = expected + movement_after_freeze
 * - actual_quantity: 盘点人录入的实际数量
 * - recount_quantity: 复盘人录入的数量
 * - final_quantity: 审批确认的最终数量
 * - variance_quantity: = final - adjusted_expected
 * - unit_cost: 过账时锁定的移动加权平均成本
 * - variance_amount: = variance_quantity * unit_cost
 *
 * @author junsong
 */
public class FinStocktakeItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "行ID", cellType = ColumnType.NUMERIC)
    private Long itemId;
    @Excel(name = "盘点ID", cellType = ColumnType.NUMERIC)
    private Long stocktakeId;
    private Long tenantId;
    @Excel(name = "门店ID", cellType = ColumnType.NUMERIC)
    private Long deptId;
    @Excel(name = "商品ID", cellType = ColumnType.NUMERIC)
    private Long productId;
    @Excel(name = "商品名称")
    private String productName;
    @Excel(name = "账面数量", cellType = ColumnType.NUMERIC)
    private BigDecimal expectedQuantity;
    @Excel(name = "冻结后变动", cellType = ColumnType.NUMERIC)
    private BigDecimal movementQuantityAfterFreeze;
    @Excel(name = "调整后期望", cellType = ColumnType.NUMERIC)
    private BigDecimal adjustedExpectedQuantity;
    @Excel(name = "实盘数量", cellType = ColumnType.NUMERIC)
    private BigDecimal actualQuantity;
    @Excel(name = "复盘数量", cellType = ColumnType.NUMERIC)
    private BigDecimal recountQuantity;
    @Excel(name = "最终数量", cellType = ColumnType.NUMERIC)
    private BigDecimal finalQuantity;
    @Excel(name = "差异数量", cellType = ColumnType.NUMERIC)
    private BigDecimal varianceQuantity;
    @Excel(name = "单位成本")
    private BigDecimal unitCost;
    @Excel(name = "差异金额")
    private BigDecimal varianceAmount;
    @Excel(name = "原因编码")
    private String reasonCode;
    @Excel(name = "原因说明")
    private String reason;
    private String attachments;
    @Excel(name = "盘点人")
    private String countedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "盘点时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date countedTime;
    @Excel(name = "复盘人")
    private String recountedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "复盘时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date recountedTime;
    private Long stockLedgerId;
    private Long costLedgerId;
    private Long reverseStockLedgerId;
    private Long reverseCostLedgerId;
    private String countIdempotencyKey;
    private Integer version;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getStocktakeId() { return stocktakeId; }
    public void setStocktakeId(Long stocktakeId) { this.stocktakeId = stocktakeId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getExpectedQuantity() { return expectedQuantity; }
    public void setExpectedQuantity(BigDecimal expectedQuantity) { this.expectedQuantity = expectedQuantity; }

    public BigDecimal getMovementQuantityAfterFreeze() { return movementQuantityAfterFreeze; }
    public void setMovementQuantityAfterFreeze(BigDecimal movementQuantityAfterFreeze) { this.movementQuantityAfterFreeze = movementQuantityAfterFreeze; }

    public BigDecimal getAdjustedExpectedQuantity() { return adjustedExpectedQuantity; }
    public void setAdjustedExpectedQuantity(BigDecimal adjustedExpectedQuantity) { this.adjustedExpectedQuantity = adjustedExpectedQuantity; }

    public BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }

    public BigDecimal getRecountQuantity() { return recountQuantity; }
    public void setRecountQuantity(BigDecimal recountQuantity) { this.recountQuantity = recountQuantity; }

    public BigDecimal getFinalQuantity() { return finalQuantity; }
    public void setFinalQuantity(BigDecimal finalQuantity) { this.finalQuantity = finalQuantity; }

    public BigDecimal getVarianceQuantity() { return varianceQuantity; }
    public void setVarianceQuantity(BigDecimal varianceQuantity) { this.varianceQuantity = varianceQuantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getVarianceAmount() { return varianceAmount; }
    public void setVarianceAmount(BigDecimal varianceAmount) { this.varianceAmount = varianceAmount; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }

    public String getCountedBy() { return countedBy; }
    public void setCountedBy(String countedBy) { this.countedBy = countedBy; }

    public Date getCountedTime() { return countedTime; }
    public void setCountedTime(Date countedTime) { this.countedTime = countedTime; }

    public String getRecountedBy() { return recountedBy; }
    public void setRecountedBy(String recountedBy) { this.recountedBy = recountedBy; }

    public Date getRecountedTime() { return recountedTime; }
    public void setRecountedTime(Date recountedTime) { this.recountedTime = recountedTime; }

    public Long getStockLedgerId() { return stockLedgerId; }
    public void setStockLedgerId(Long stockLedgerId) { this.stockLedgerId = stockLedgerId; }

    public Long getCostLedgerId() { return costLedgerId; }
    public void setCostLedgerId(Long costLedgerId) { this.costLedgerId = costLedgerId; }

    public Long getReverseStockLedgerId() { return reverseStockLedgerId; }
    public void setReverseStockLedgerId(Long reverseStockLedgerId) { this.reverseStockLedgerId = reverseStockLedgerId; }

    public Long getReverseCostLedgerId() { return reverseCostLedgerId; }
    public void setReverseCostLedgerId(Long reverseCostLedgerId) { this.reverseCostLedgerId = reverseCostLedgerId; }

    public String getCountIdempotencyKey() { return countIdempotencyKey; }
    public void setCountIdempotencyKey(String countIdempotencyKey) { this.countIdempotencyKey = countIdempotencyKey; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
