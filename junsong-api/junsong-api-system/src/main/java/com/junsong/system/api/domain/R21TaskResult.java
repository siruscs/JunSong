package com.junsong.system.api.domain;

import java.io.Serializable;

/**
 * R21 运维调度任务执行结果（跨服务共享）。
 * task 类执行业务逻辑后返回，由 system controller 统一记录调度日志。
 */
public class R21TaskResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 执行状态：SUCCESS / FAILED / SKIPPED / PARTIAL */
    private String status;

    /** 已处理行数 */
    private int processedCount;

    /** 结果摘要 */
    private String resultSummary;

    /** 错误信息（FAILED/PARTIAL 时填充） */
    private String errorMessage;

    public static R21TaskResult success(int processedCount, String resultSummary)
    {
        R21TaskResult r = new R21TaskResult();
        r.status = "SUCCESS";
        r.processedCount = processedCount;
        r.resultSummary = resultSummary;
        return r;
    }

    public static R21TaskResult skipped(String resultSummary)
    {
        R21TaskResult r = new R21TaskResult();
        r.status = "SKIPPED";
        r.resultSummary = resultSummary;
        return r;
    }

    public static R21TaskResult partial(int processedCount, String resultSummary, String errorMessage)
    {
        R21TaskResult r = new R21TaskResult();
        r.status = "PARTIAL";
        r.processedCount = processedCount;
        r.resultSummary = resultSummary;
        r.errorMessage = errorMessage;
        return r;
    }

    public static R21TaskResult failed(Throwable ex)
    {
        R21TaskResult r = new R21TaskResult();
        r.status = "FAILED";
        r.errorMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return r;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getProcessedCount() { return processedCount; }
    public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
