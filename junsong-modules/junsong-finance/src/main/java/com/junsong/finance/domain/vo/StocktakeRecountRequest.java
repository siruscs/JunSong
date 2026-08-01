package com.junsong.finance.domain.vo;

/**
 * 盘点复盘行录入请求（Task 5：阈值复盘）。
 *
 * 业务规则：
 * - recountQuantity：必填，非负（>= 0）
 * - idempotencyKey：必填，租户内唯一；相同键相同负载返回原结果；不同负载拒绝
 * - reasonCode / reason：当 recountQuantity 与 adjustedExpectedQuantity 不等时必填
 * - version：行表当前版本号，乐观锁谓词
 *
 * 安全契约：
 * 1. 仅 RECOUNTING 状态任务允许录入复盘
 * 2. 非 admin 时仅分配的 recountUserId 可录入
 * 3. recountUserId 必须与 counterUserId 不同（在分配阶段已强制，此处再次校验）
 *
 * @author junsong
 */
public class StocktakeRecountRequest {
    /** 复盘数量（非负） */
    private java.math.BigDecimal recountQuantity;
    /** 损耗原因代码（方差非零时必填） */
    private String reasonCode;
    /** 损耗原因文字说明（方差非零时必填） */
    private String reason;
    /** 幂等键（租户内唯一） */
    private String idempotencyKey;
    /** 行表版本号（乐观锁） */
    private Integer version;

    public java.math.BigDecimal getRecountQuantity() { return recountQuantity; }
    public void setRecountQuantity(java.math.BigDecimal recountQuantity) { this.recountQuantity = recountQuantity; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
