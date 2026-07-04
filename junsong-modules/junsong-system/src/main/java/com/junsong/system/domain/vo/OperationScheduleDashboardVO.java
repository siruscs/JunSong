package com.junsong.system.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * R21: 运维调度看板 VO
 */
public class OperationScheduleDashboardVO {
    /** 每个 jobCode 的最近一条日志 */
    private List<OperationScheduleLogVO> recentLogs = new ArrayList<>();

    /** 最近 24 小时内 FAILED / PARTIAL 总数 */
    private int failureCount24h;

    /** 最近的 FAILED / PARTIAL 日志 */
    private List<OperationScheduleLogVO> recentFailures = new ArrayList<>();

    public List<OperationScheduleLogVO> getRecentLogs() {
        return recentLogs;
    }

    public void setRecentLogs(List<OperationScheduleLogVO> recentLogs) {
        this.recentLogs = recentLogs;
    }

    public int getFailureCount24h() {
        return failureCount24h;
    }

    public void setFailureCount24h(int failureCount24h) {
        this.failureCount24h = failureCount24h;
    }

    public List<OperationScheduleLogVO> getRecentFailures() {
        return recentFailures;
    }

    public void setRecentFailures(List<OperationScheduleLogVO> recentFailures) {
        this.recentFailures = recentFailures;
    }
}
