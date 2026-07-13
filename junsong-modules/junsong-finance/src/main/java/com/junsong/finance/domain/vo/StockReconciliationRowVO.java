package com.junsong.finance.domain.vo;

/**
 * 库存对账明细行VO。
 *
 * <p>每行描述一个 (tenantId, deptId, productId) 维度的异常，包含期望值、实际值与差值，
 * 以及异常分类代码与安全提示。所有字段均为只读查询结果，不含任何可变更状态。</p>
 *
 * <p>异常代码取值：
 * <ul>
 *   <li>POSITION_WITHOUT_LEDGER - 结存存在但无流水记录</li>
 *   <li>LEDGER_POSITION_MISMATCH - 流水累计与结存不一致</li>
 *   <li>SNAPSHOT_EQUATION_MISMATCH - 快照恒等式不成立（期初+入-出 != 期末）</li>
 *   <li>LATEST_SNAPSHOT_MISMATCH - 最新快照期末结存与当前结存不一致</li>
 * </ul></p>
 *
 * @author junsong
 */
public class StockReconciliationRowVO {

    private Long tenantId;
    private Long deptId;
    private String deptName;
    private Long productId;
    private String productName;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer diffQuantity;
    private String anomalyCode;
    private String safetyNote;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getExpectedQuantity() { return expectedQuantity; }
    public void setExpectedQuantity(Integer expectedQuantity) { this.expectedQuantity = expectedQuantity; }

    public Integer getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(Integer actualQuantity) { this.actualQuantity = actualQuantity; }

    public Integer getDiffQuantity() { return diffQuantity; }
    public void setDiffQuantity(Integer diffQuantity) { this.diffQuantity = diffQuantity; }

    public String getAnomalyCode() { return anomalyCode; }
    public void setAnomalyCode(String anomalyCode) { this.anomalyCode = anomalyCode; }

    public String getSafetyNote() { return safetyNote; }
    public void setSafetyNote(String safetyNote) { this.safetyNote = safetyNote; }
}
