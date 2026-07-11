package com.junsong.finance.domain.vo;

import java.util.ArrayList;
import java.util.List;

public class ReceivableCollectionDashboardVO {
    private ReceivableCollectionSummaryVO summary = new ReceivableCollectionSummaryVO();
    private List<ReceivableCollectionRowVO> todayFollowUps = new ArrayList<>();
    private List<ReceivableCollectionRowVO> overduePromises = new ArrayList<>();
    private List<ReceivableCollectionRowVO> highRiskReceivables = new ArrayList<>();

    public ReceivableCollectionSummaryVO getSummary() { return summary; }
    public void setSummary(ReceivableCollectionSummaryVO summary) { this.summary = summary == null ? new ReceivableCollectionSummaryVO() : summary; }
    public List<ReceivableCollectionRowVO> getTodayFollowUps() { return todayFollowUps; }
    public void setTodayFollowUps(List<ReceivableCollectionRowVO> todayFollowUps) { this.todayFollowUps = todayFollowUps == null ? new ArrayList<>() : todayFollowUps; }
    public List<ReceivableCollectionRowVO> getOverduePromises() { return overduePromises; }
    public void setOverduePromises(List<ReceivableCollectionRowVO> overduePromises) { this.overduePromises = overduePromises == null ? new ArrayList<>() : overduePromises; }
    public List<ReceivableCollectionRowVO> getHighRiskReceivables() { return highRiskReceivables; }
    public void setHighRiskReceivables(List<ReceivableCollectionRowVO> highRiskReceivables) { this.highRiskReceivables = highRiskReceivables == null ? new ArrayList<>() : highRiskReceivables; }
}
