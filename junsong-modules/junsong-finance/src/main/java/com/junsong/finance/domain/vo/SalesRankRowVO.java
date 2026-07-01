package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class SalesRankRowVO {
    private Long id;
    private String name;
    private BigDecimal amount = BigDecimal.ZERO;
    private int quantity;
    private Long deptId;
    private String deptName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
}
