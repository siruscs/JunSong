package com.junsong.finance.domain.vo;

/**
 * 期初库存审批请求。
 *
 * 业务规则：
 * - decision：审批决定（APPROVE / REJECT），非空
 * - comment：审批意见（可选）
 * - version：头表版本号（乐观锁，非空）
 *
 * 安全契约：
 * 1. 仅 SUBMITTED 状态可审批
 * 2. APPROVE 流转至 APPROVED；REJECT 回退至 DRAFT
 * 3. 审批人不能是创建人
 *
 * @author junsong
 */
public class StockInitApproveRequest {

    /** 审批决定（APPROVE / REJECT） */
    private String decision;
    /** 审批意见 */
    private String comment;
    /** 头表版本号（乐观锁，非空） */
    private Integer version;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
