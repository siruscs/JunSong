package com.junsong.finance.domain.vo;

/**
 * 库存盘点任务分配请求（分配盘点人和复盘人）。
 *
 * 业务规则：
 * - 仅 DRAFT 状态允许分配
 * - 必须在当前用户授权部门集合内
 * - counterUserId 必填，recountUserId 可选
 * - version 必填（乐观锁）
 *
 * @author junsong
 */
public class StocktakeAssignRequest {

    private Long counterUserId;
    private Long recountUserId;
    private Integer version;

    public Long getCounterUserId() { return counterUserId; }
    public void setCounterUserId(Long counterUserId) { this.counterUserId = counterUserId; }

    public Long getRecountUserId() { return recountUserId; }
    public void setRecountUserId(Long recountUserId) { this.recountUserId = recountUserId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
