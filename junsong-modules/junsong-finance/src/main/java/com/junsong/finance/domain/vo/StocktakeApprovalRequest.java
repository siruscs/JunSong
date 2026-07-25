package com.junsong.finance.domain.vo;

/**
 * 盘点审批请求（Task 5：提交、阈值复盘与审批）。
 *
 * 业务规则：
 * - decision：APPROVE（通过）/ REJECT（驳回，回到 COUNTING 重新盘点）
 * - comment：审批意见
 * - version：头表版本号（乐观锁）
 *
 * @author junsong
 */
public class StocktakeApprovalRequest {
    /** 审批决定：APPROVE / REJECT */
    private String decision;
    /** 审批意见 */
    private String comment;
    /** 头表版本号（乐观锁） */
    private Integer version;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
