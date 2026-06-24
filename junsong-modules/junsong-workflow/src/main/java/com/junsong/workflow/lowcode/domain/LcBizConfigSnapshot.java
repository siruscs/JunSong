package com.junsong.workflow.lowcode.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class LcBizConfigSnapshot
{
    private Long id;
    private String bizCode;
    private Integer versionNo;
    private String configJson;
    private String status;
    private String publishRemark;
    private String delFlag;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBizCode() { return bizCode; }
    public void setBizCode(String bizCode) { this.bizCode = bizCode; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPublishRemark() { return publishRemark; }
    public void setPublishRemark(String publishRemark) { this.publishRemark = publishRemark; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
