package com.junsong.system.domain.vo;

/**
 * 单条数据质量问题。
 * R20: 只读展示，不做自动修复。
 */
public class DataQualityIssueVO {
    /** 问题类型（如 FINANCE_SALE_WITHOUT_DEPT） */
    private String issueType;

    /** 所属模块：finance / member / system / stock */
    private String module;

    /** 严重级别：HIGH / MEDIUM / LOW */
    private String severity;

    /** 问题数量 */
    private Long issueCount;

    /** 来源表 */
    private String sourceTables;

    /** 问题原因说明 */
    private String reason;

    /** 钻取路径 */
    private String drilldownPath;

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Long getIssueCount() {
        return issueCount;
    }

    public void setIssueCount(Long issueCount) {
        this.issueCount = issueCount;
    }

    public String getSourceTables() {
        return sourceTables;
    }

    public void setSourceTables(String sourceTables) {
        this.sourceTables = sourceTables;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDrilldownPath() {
        return drilldownPath;
    }

    public void setDrilldownPath(String drilldownPath) {
        this.drilldownPath = drilldownPath;
    }
}
