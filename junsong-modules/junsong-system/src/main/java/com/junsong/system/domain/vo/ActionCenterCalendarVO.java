package com.junsong.system.domain.vo;

public class ActionCenterCalendarVO {
    private String date;
    private int pendingCount;
    private int overdueCount;
    private int doneCount;
    private int effectPendingCount;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
    public int getOverdueCount() { return overdueCount; }
    public void setOverdueCount(int overdueCount) { this.overdueCount = overdueCount; }
    public int getDoneCount() { return doneCount; }
    public void setDoneCount(int doneCount) { this.doneCount = doneCount; }
    public int getEffectPendingCount() { return effectPendingCount; }
    public void setEffectPendingCount(int effectPendingCount) { this.effectPendingCount = effectPendingCount; }
}
