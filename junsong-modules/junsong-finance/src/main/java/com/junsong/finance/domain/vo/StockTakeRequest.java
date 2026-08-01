package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 库存盘点录入请求。
 *
 * 安全边界：
 * - 租户：由 TenantContext 注入，客户端不可设置
 * - 部门：由后端校验是否在授权范围内，客户端不可绕过
 * - 商品：必须属于当前部门
 * - 数量：盘点后实际数量，必须 >= 0
 * - 原因：盘盈盘亏必须填写原因
 * - 幂等：基于 takeNo 唯一约束，重复提交被拒绝
 *
 * @author junsong
 */
public class StockTakeRequest {

    /** 盘点单号（客户端生成的唯一编号，用于幂等） */
    private String takeNo;

    /** 门店ID（由后端校验授权范围） */
    private Long deptId;

    /** 商品ID */
    private Long productId;

    /** 盘点后实际数量（必须 >= 0） */
    private java.math.BigDecimal actualQuantity;

    /** 盘点前系统库存（客户端展示用，后端以加锁查询为准） */
    private java.math.BigDecimal expectedQuantity;

    /** 单位成本（盘盈时按此成本入账；盘亏时按移动加权平均成本出账，客户端传入仅作参考） */
    private BigDecimal unitCost;

    /** 盘盈盘亏原因（必填） */
    private String reason;

    public String getTakeNo() { return takeNo; }
    public void setTakeNo(String takeNo) { this.takeNo = takeNo; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public java.math.BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(java.math.BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }

    public java.math.BigDecimal getExpectedQuantity() { return expectedQuantity; }
    public void setExpectedQuantity(java.math.BigDecimal expectedQuantity) { this.expectedQuantity = expectedQuantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
