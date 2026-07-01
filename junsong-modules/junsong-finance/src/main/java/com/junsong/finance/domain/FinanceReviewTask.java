package com.junsong.finance.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 财务复盘任务对象 finance_review_task
 *
 * @author junsong
 */
public class FinanceReviewTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 任务类型，对应诊断规则ID */
    private String taskType;

    /** 门店ID */
    private Long deptId;

    /** 门店名称 */
    private String deptName;

    /** 核算周期ID */
    private Long periodId;

    /** 任务日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date taskDate;

    /** 状态: PENDING/IN_PROGRESS/DONE/IGNORED */
    private String status;

    /** 严重级别: HIGH/MEDIUM/LOW */
    private String severity;

    /** 任务标题 */
    private String title;

    /** 触发原因 */
    private String reason;

    /** 建议动作 */
    private String suggestion;

    /** 影响金额 */
    private BigDecimal impactAmount;

    /** 处理人ID */
    private Long handlerId;

    /** 处理人姓名 */
    private String handlerName;

    /** 处理备注 */
    private String handlerNote;

    /** 忽略原因 */
    private String ignoreReason;

    /** 去重Key */
    private String alertId;

    /** 跳转路由 */
    private String targetRoute;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public Date getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(Date taskDate) {
        this.taskDate = taskDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public BigDecimal getImpactAmount() {
        return impactAmount;
    }

    public void setImpactAmount(BigDecimal impactAmount) {
        this.impactAmount = impactAmount;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getHandlerNote() {
        return handlerNote;
    }

    public void setHandlerNote(String handlerNote) {
        this.handlerNote = handlerNote;
    }

    public String getIgnoreReason() {
        return ignoreReason;
    }

    public void setIgnoreReason(String ignoreReason) {
        this.ignoreReason = ignoreReason;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getTargetRoute() {
        return targetRoute;
    }

    public void setTargetRoute(String targetRoute) {
        this.targetRoute = targetRoute;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("taskId", getTaskId())
                .append("taskType", getTaskType())
                .append("deptId", getDeptId())
                .append("deptName", getDeptName())
                .append("periodId", getPeriodId())
                .append("taskDate", getTaskDate())
                .append("status", getStatus())
                .append("severity", getSeverity())
                .append("title", getTitle())
                .append("reason", getReason())
                .append("suggestion", getSuggestion())
                .append("impactAmount", getImpactAmount())
                .append("handlerId", getHandlerId())
                .append("handlerName", getHandlerName())
                .append("handlerNote", getHandlerNote())
                .append("ignoreReason", getIgnoreReason())
                .append("alertId", getAlertId())
                .append("targetRoute", getTargetRoute())
                .append("delFlag", getDelFlag())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
