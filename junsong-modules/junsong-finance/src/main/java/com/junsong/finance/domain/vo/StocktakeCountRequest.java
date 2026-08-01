package com.junsong.finance.domain.vo;

/**
 * 盘点行录入请求（Task 4：盲盘与幂等行录入）。
 *
 * 业务规则：
 * - actualQuantity：必填，非负（>= 0）
 * - idempotencyKey：必填，租户内唯一；相同键重复请求返回原结果；相同键不同负载拒绝
 * - reasonCode / reason：当 actualQuantity 与期望数量不等（方差非零）时必填
 * - attachments：JSON 字符串，可选（证据图片/文件）
 * - version：行表当前版本号，乐观锁谓词
 *
 * 盲盘保护：本请求不携带也不返回期望数量，counter 在提交前无法获知期望值。
 *
 * @author junsong
 */
public class StocktakeCountRequest {
    /** 实际盘点数量（非负） */
    private java.math.BigDecimal actualQuantity;
    /** 损耗原因代码（方差非零时必填）：EXPIRED/DAMAGED/THEFT/WEIGHING/OPERATION/MISSING_TRANSACTION/OTHER */
    private String reasonCode;
    /** 损耗原因文字说明（方差非零时必填） */
    private String reason;
    /** 附件 JSON 字符串，例如 [{"name":"damage.jpg","url":"/profile/upload/damage.jpg"}] */
    private String attachments;
    /** 幂等键（租户内唯一）：相同键重复请求返回原结果，不同负载拒绝 */
    private String idempotencyKey;
    /** 行表版本号（乐观锁） */
    private Integer version;

    public java.math.BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(java.math.BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
