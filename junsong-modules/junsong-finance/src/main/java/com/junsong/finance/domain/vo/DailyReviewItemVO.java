package com.junsong.finance.domain.vo;

/**
 * 每日复盘关注项 VO。
 * R8-A: 聚合到 Top 3 关注项，点击可跳转 targetRoute。
 */
public class DailyReviewItemVO {

    /** 关注项类型：HIGH_TASK / CASHFLOW_NEGATIVE / EXPENSE_HIGH 等 */
    private String itemType;

    /** 标题 */
    private String title;

    /** 原因说明 */
    private String reason;

    /** 建议动作 */
    private String suggestion;

    /** 跳转路由 */
    private String targetRoute;

    /** 影响金额（可选） */
    private java.math.BigDecimal impactAmount;

    public DailyReviewItemVO() {}

    public DailyReviewItemVO(String itemType, String title, String reason, String suggestion, String targetRoute) {
        this.itemType = itemType;
        this.title = title;
        this.reason = reason;
        this.suggestion = suggestion;
        this.targetRoute = targetRoute;
    }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getTargetRoute() { return targetRoute; }
    public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }

    public java.math.BigDecimal getImpactAmount() { return impactAmount; }
    public void setImpactAmount(java.math.BigDecimal impactAmount) { this.impactAmount = impactAmount; }
}
