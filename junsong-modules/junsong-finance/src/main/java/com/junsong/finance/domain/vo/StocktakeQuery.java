package com.junsong.finance.domain.vo;

/**
 * 库存盘点任务列表查询参数。
 *
 * 所有字段可选；service 层自动叠加：
 * - tenantId = TenantContext.getTenantId()
 * - deptIds = 当前用户授权部门集合（admin 跳过）
 *
 * @author junsong
 */
public class StocktakeQuery {

    private Long deptId;
    private String status;
    private Long counterUserId;
    private String takeNo;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getCounterUserId() { return counterUserId; }
    public void setCounterUserId(Long counterUserId) { this.counterUserId = counterUserId; }

    public String getTakeNo() { return takeNo; }
    public void setTakeNo(String takeNo) { this.takeNo = takeNo; }
}
