package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class ProfitShareExceptionVO {
    private String exceptionType;
    private Long deptId;
    private String deptName;
    private String message;
    private BigDecimal expectedAmount = BigDecimal.ZERO;
    private BigDecimal actualAmount = BigDecimal.ZERO;

    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
}
