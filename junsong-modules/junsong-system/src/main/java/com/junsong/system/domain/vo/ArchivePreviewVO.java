package com.junsong.system.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * R25归档预览 VO
 */
public class ArchivePreviewVO
{
    /** 表名 */
    private String tableName;

    /** 候选数据量 */
    private Long candidateCount;

    /** 归档模式 */
    private String archiveMode;

    /** 是否试运行 */
    private String dryRun;

    /** 归档截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cutoffTime;

    public String getTableName()
    {
        return tableName;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public Long getCandidateCount()
    {
        return candidateCount;
    }

    public void setCandidateCount(Long candidateCount)
    {
        this.candidateCount = candidateCount;
    }

    public String getArchiveMode()
    {
        return archiveMode;
    }

    public void setArchiveMode(String archiveMode)
    {
        this.archiveMode = archiveMode;
    }

    public String getDryRun()
    {
        return dryRun;
    }

    public void setDryRun(String dryRun)
    {
        this.dryRun = dryRun;
    }

    public Date getCutoffTime()
    {
        return cutoffTime;
    }

    public void setCutoffTime(Date cutoffTime)
    {
        this.cutoffTime = cutoffTime;
    }
}
