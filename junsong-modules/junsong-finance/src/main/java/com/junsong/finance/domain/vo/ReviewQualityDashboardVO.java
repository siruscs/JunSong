package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 复盘质量看板 VO
 */
public class ReviewQualityDashboardVO {

    private BigDecimal qualityScore = BigDecimal.ZERO;
    private int totalTaskCount;
    private int doneTaskCount;
    private int overdueTaskCount;
    private BigDecimal overdueRatio = BigDecimal.ZERO;
    private BigDecimal avgFirstResponseHours = BigDecimal.ZERO;
    private BigDecimal avgCloseHours = BigDecimal.ZERO;
    private int noNoteDoneCount;
    private List<String> suggestions = new ArrayList<>();

    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    public int getTotalTaskCount() { return totalTaskCount; }
    public void setTotalTaskCount(int totalTaskCount) { this.totalTaskCount = totalTaskCount; }
    public int getDoneTaskCount() { return doneTaskCount; }
    public void setDoneTaskCount(int doneTaskCount) { this.doneTaskCount = doneTaskCount; }
    public int getOverdueTaskCount() { return overdueTaskCount; }
    public void setOverdueTaskCount(int overdueTaskCount) { this.overdueTaskCount = overdueTaskCount; }
    public BigDecimal getOverdueRatio() { return overdueRatio; }
    public void setOverdueRatio(BigDecimal overdueRatio) { this.overdueRatio = overdueRatio; }
    public BigDecimal getAvgFirstResponseHours() { return avgFirstResponseHours; }
    public void setAvgFirstResponseHours(BigDecimal avgFirstResponseHours) { this.avgFirstResponseHours = avgFirstResponseHours; }
    public BigDecimal getAvgCloseHours() { return avgCloseHours; }
    public void setAvgCloseHours(BigDecimal avgCloseHours) { this.avgCloseHours = avgCloseHours; }
    public int getNoNoteDoneCount() { return noNoteDoneCount; }
    public void setNoNoteDoneCount(int noNoteDoneCount) { this.noNoteDoneCount = noNoteDoneCount; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
