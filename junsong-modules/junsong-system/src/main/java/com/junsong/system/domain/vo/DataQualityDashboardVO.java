package com.junsong.system.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据质量看板响应。
 * R20: 聚合所有数据质量问题，按严重级别统计。
 */
public class DataQualityDashboardVO {
    /** 整体状态：HEALTHY / WARN / BLOCKED / ERROR */
    private String status;

    /** 问题总数 */
    private int totalIssueCount;

    /** HIGH 级别问题数 */
    private int highIssueCount;

    /** MEDIUM 级别问题数 */
    private int mediumIssueCount;

    /** LOW 级别问题数 */
    private int lowIssueCount;

    /** 问题明细列表 */
    private List<DataQualityIssueVO> issues = new ArrayList<>();

    /** 数据库查询失败数 */
    private int dbErrorCount;

    /** 数据库查询失败详情 */
    private List<String> dbErrors = new ArrayList<>();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalIssueCount() {
        return totalIssueCount;
    }

    public void setTotalIssueCount(int totalIssueCount) {
        this.totalIssueCount = totalIssueCount;
    }

    public int getHighIssueCount() {
        return highIssueCount;
    }

    public void setHighIssueCount(int highIssueCount) {
        this.highIssueCount = highIssueCount;
    }

    public int getMediumIssueCount() {
        return mediumIssueCount;
    }

    public void setMediumIssueCount(int mediumIssueCount) {
        this.mediumIssueCount = mediumIssueCount;
    }

    public int getLowIssueCount() {
        return lowIssueCount;
    }

    public void setLowIssueCount(int lowIssueCount) {
        this.lowIssueCount = lowIssueCount;
    }

    public List<DataQualityIssueVO> getIssues() {
        return issues;
    }

    public void setIssues(List<DataQualityIssueVO> issues) {
        this.issues = issues;
    }

    public int getDbErrorCount() {
        return dbErrorCount;
    }

    public void setDbErrorCount(int dbErrorCount) {
        this.dbErrorCount = dbErrorCount;
    }

    public List<String> getDbErrors() {
        return dbErrors;
    }

    public void setDbErrors(List<String> dbErrors) {
        this.dbErrors = dbErrors;
    }
}
