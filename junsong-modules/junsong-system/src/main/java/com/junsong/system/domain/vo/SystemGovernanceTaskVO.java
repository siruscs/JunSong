package com.junsong.system.domain.vo;

import java.util.Date;

/**
 * 系统治理任务 VO。
 * 将治理风险转化为可执行任务：明确类型、严重级别、原因、动作和跳转目标。
 */
public class SystemGovernanceTaskVO {
    /** 任务类型（如 EMPTY_MENU / LOGIN_FAIL / MENU_WITHOUT_ROLE 等） */
    private String taskType;

    /** 严重级别：HIGH / MEDIUM / LOW */
    private String severity;

    /** 任务标题 */
    private String title;

    /** 风险原因 */
    private String reason;

    /** 建议动作 */
    private String action;

    /** 跳转路由 */
    private String targetRoute;

    /** 影响数量 */
    private Integer count;

    /** R11-H: 最近处理动作类型 */
    private String lastActionType;

    /** R11-H: 最近处理人 */
    private String lastHandlerName;

    /** R11-H: 最近处理时间 */
    private Date lastActionTime;

    /** R11-H: 最近处理备注 */
    private String lastHandlerNote;

    /** R12-F: 是否已归档（DONE/IGNORED 且未被 REOPEN） */
    private boolean archived;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetRoute() {
        return targetRoute;
    }

    public void setTargetRoute(String targetRoute) {
        this.targetRoute = targetRoute;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getLastActionType() {
        return lastActionType;
    }

    public void setLastActionType(String lastActionType) {
        this.lastActionType = lastActionType;
    }

    public String getLastHandlerName() {
        return lastHandlerName;
    }

    public void setLastHandlerName(String lastHandlerName) {
        this.lastHandlerName = lastHandlerName;
    }

    public Date getLastActionTime() {
        return lastActionTime;
    }

    public void setLastActionTime(Date lastActionTime) {
        this.lastActionTime = lastActionTime;
    }

    public String getLastHandlerNote() {
        return lastHandlerNote;
    }

    public void setLastHandlerNote(String lastHandlerNote) {
        this.lastHandlerNote = lastHandlerNote;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
