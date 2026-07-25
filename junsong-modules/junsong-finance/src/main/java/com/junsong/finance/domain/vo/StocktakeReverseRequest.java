package com.junsong.finance.domain.vo;

/**
 * 盘点整单冲销请求（Task 7：安全取消与整单冲销）。
 *
 * 业务规则：
 * - reason：冲销理由（非空，最长 256 字符）
 * - idempotencyKey：冲销幂等键（非空），相同键重复冲销应返回原结果，不同负载拒绝
 * - version：头表版本号（乐观锁，非空）
 *
 * 安全契约：
 * 1. 仅 POSTED 状态任务可冲销
 * 2. 会计期间必须为 ACTIVE（0）
 * 3. 二次冲销拒绝（已 REVERSED 不能再冲销）
 * 4. 整单冲销：写入相反的数量与成本流水，不删除原始证据
 * 5. 原始成本复用：从原过账成本流水读取 unitCost，确保冲销金额与原过账一致
 *
 * @author junsong
 */
public class StocktakeReverseRequest {
    /** 冲销理由（非空） */
    private String reason;
    /** 冲销幂等键（非空） */
    private String idempotencyKey;
    /** 头表版本号（乐观锁，非空） */
    private Integer version;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
