package com.junsong.finance.domain.vo;

/**
 * 库存底座健康问题VO。
 *
 * @author junsong
 */
public class StockHealthIssueVO {

    private String type;
    private String severity;
    private String title;
    private String detail;

    public StockHealthIssueVO() {}

    public StockHealthIssueVO(String type, String severity, String title, String detail) {
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.detail = detail;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}