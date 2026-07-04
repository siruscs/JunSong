package com.junsong.system.domain.vo;

/**
 * R21: 手动触发运维调度结果 VO
 */
public class OperationScheduleTriggerResultVO {
    /** 日志ID */
    private Long logId;

    /** 任务编码 */
    private String jobCode;

    /** 执行状态：SUCCESS / FAILED / SKIPPED / PARTIAL */
    private String status;

    /** 执行结果摘要 */
    private String resultSummary;

    /** 错误信息 */
    private String errorMessage;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
