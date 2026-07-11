package com.junsong.finance.domain.vo;

import java.util.Date;
import java.util.List;

/**
 * 复盘任务动作效果汇总 VO
 */
public class ReviewTaskEffectSummaryVO {

    private int evaluatedTaskCount;
    private int goodEffectCount;
    private int watchEffectCount;
    private int noImprovementCount;
    private int averageEffectScore;
    private List<ReopenCandidateVO> reopenCandidates;

    public int getEvaluatedTaskCount() { return evaluatedTaskCount; }
    public void setEvaluatedTaskCount(int evaluatedTaskCount) { this.evaluatedTaskCount = evaluatedTaskCount; }
    public int getGoodEffectCount() { return goodEffectCount; }
    public void setGoodEffectCount(int goodEffectCount) { this.goodEffectCount = goodEffectCount; }
    public int getWatchEffectCount() { return watchEffectCount; }
    public void setWatchEffectCount(int watchEffectCount) { this.watchEffectCount = watchEffectCount; }
    public int getNoImprovementCount() { return noImprovementCount; }
    public void setNoImprovementCount(int noImprovementCount) { this.noImprovementCount = noImprovementCount; }
    public int getAverageEffectScore() { return averageEffectScore; }
    public void setAverageEffectScore(int averageEffectScore) { this.averageEffectScore = averageEffectScore; }
    public List<ReopenCandidateVO> getReopenCandidates() { return reopenCandidates; }
    public void setReopenCandidates(List<ReopenCandidateVO> reopenCandidates) { this.reopenCandidates = reopenCandidates; }

    public static class ReopenCandidateVO {
        private Long taskId;
        private String title;
        private String taskType;
        private String deptName;
        private Date archiveTime;
        private Integer reopenCount;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public String getDeptName() { return deptName; }
        public void setDeptName(String deptName) { this.deptName = deptName; }
        public Date getArchiveTime() { return archiveTime; }
        public void setArchiveTime(Date archiveTime) { this.archiveTime = archiveTime; }
        public Integer getReopenCount() { return reopenCount; }
        public void setReopenCount(Integer reopenCount) { this.reopenCount = reopenCount; }
    }
}
