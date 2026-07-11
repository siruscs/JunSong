package com.junsong.finance.domain;

import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 复盘知识库对象 finance_review_knowledge
 *
 * @author junsong
 */
public class FinanceReviewKnowledge extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 知识ID */
    private Long knowledgeId;

    /** 来源复盘任务ID */
    private Long taskId;

    /** 来源门店ID */
    private Long deptId;

    /** 问题类型 */
    private String problemType;

    /** 知识标题 */
    private String title;

    /** 问题摘要 */
    private String problemSummary;

    /** 原因分析 */
    private String rootCause;

    /** 采取动作 */
    private String actionTaken;

    /** 效果摘要 */
    private String resultSummary;

    /** 是否可复用 */
    private String reusable;

    /** 来源处理人 */
    private String sourceHandlerName;

    /** 备注 */
    private String remark;

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getProblemType() {
        return problemType;
    }

    public void setProblemType(String problemType) {
        this.problemType = problemType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProblemSummary() {
        return problemSummary;
    }

    public void setProblemSummary(String problemSummary) {
        this.problemSummary = problemSummary;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getReusable() {
        return reusable;
    }

    public void setReusable(String reusable) {
        this.reusable = reusable;
    }

    public String getSourceHandlerName() {
        return sourceHandlerName;
    }

    public void setSourceHandlerName(String sourceHandlerName) {
        this.sourceHandlerName = sourceHandlerName;
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("knowledgeId", getKnowledgeId())
                .append("taskId", getTaskId())
                .append("deptId", getDeptId())
                .append("problemType", getProblemType())
                .append("title", getTitle())
                .append("problemSummary", getProblemSummary())
                .append("rootCause", getRootCause())
                .append("actionTaken", getActionTaken())
                .append("resultSummary", getResultSummary())
                .append("reusable", getReusable())
                .append("sourceHandlerName", getSourceHandlerName())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
