package com.junsong.finance.domain.vo;

/**
 * 期初库存过账请求。
 *
 * 业务规则：
 * - postIdempotencyKey：过账幂等键（非空，租户内唯一）
 *   相同键对应同一批次视为幂等重放返回成功；不同批次则拒绝
 * - version：头表版本号（乐观锁，非空）
 *
 * 安全契约：
 * 1. 仅 APPROVED 状态可过账
 * 2. 会计期间必须为 ACTIVE（0）
 * 3. batchNo 已有库存流水则拒绝重复过账
 *
 * @author junsong
 */
public class StockInitPostRequest {

    /** 过账幂等键（非空，租户内唯一） */
    private String postIdempotencyKey;
    /** 头表版本号（乐观锁，非空） */
    private Integer version;

    public String getPostIdempotencyKey() { return postIdempotencyKey; }
    public void setPostIdempotencyKey(String postIdempotencyKey) { this.postIdempotencyKey = postIdempotencyKey; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
