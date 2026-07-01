package com.junsong.finance.domain.vo;

import java.util.List;
import java.util.Map;

/**
 * 钻取明细 VO - 从报表钻取到具体记录
 */
public class DrillDownDetailVO {
    private String drillType;          // SALES / EXPENSES / PROFIT_SHARE
    private String title;              // 显示标题
    private int totalCount;            // 总记录数
    private List<Map<String, Object>> records;  // 明细记录
    private Map<String, Object> filterSummary;  // 当前筛选条件摘要

    public String getDrillType() { return drillType; }
    public void setDrillType(String drillType) { this.drillType = drillType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public List<Map<String, Object>> getRecords() { return records; }
    public void setRecords(List<Map<String, Object>> records) { this.records = records; }
    public Map<String, Object> getFilterSummary() { return filterSummary; }
    public void setFilterSummary(Map<String, Object> filterSummary) { this.filterSummary = filterSummary; }
}
