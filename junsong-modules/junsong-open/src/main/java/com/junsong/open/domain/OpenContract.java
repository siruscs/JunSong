package com.junsong.open.domain;

import java.util.Date;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 开放平台合约对象 open_contract
 *
 * @author junsong
 */
public class OpenContract extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "合约编号")
    private String contractNo;

    @Excel(name = "ISV-ID")
    private Long isvId;

    /** ISV名称（关联查询用） */
    @Excel(name = "ISV名称")
    private String isvName;

    /** 合约类型：standard / custom */
    @Excel(name = "合约类型")
    private String contractType;

    @Excel(name = "合约标题")
    private String title;

    private String terms;

    private Date startDate;

    private Date endDate;

    /** 日调用配额 */
    @Excel(name = "日调用配额")
    private Integer dailyQuota;

    /** 可用能力列表（逗号分隔） */
    @Excel(name = "可用能力")
    private String allowedCapabilities;

    /** 状态：DRAFT / ACTIVE / EXPIRING / EXPIRED / TERMINATED */
    @Excel(name = "合约状态")
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }

    public Long getIsvId() { return isvId; }
    public void setIsvId(Long isvId) { this.isvId = isvId; }

    public String getIsvName() { return isvName; }
    public void setIsvName(String isvName) { this.isvName = isvName; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public Integer getDailyQuota() { return dailyQuota; }
    public void setDailyQuota(Integer dailyQuota) { this.dailyQuota = dailyQuota; }

    public String getAllowedCapabilities() { return allowedCapabilities; }
    public void setAllowedCapabilities(String allowedCapabilities) { this.allowedCapabilities = allowedCapabilities; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
