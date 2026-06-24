package com.junsong.system.domain;

import com.junsong.common.core.workflow.AbstractWorkflowBusinessEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class SysStoreOpening extends AbstractWorkflowBusinessEntity
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING_REGION_APPROVAL = "PENDING_REGION_APPROVAL";
    public static final String STATUS_PENDING_GM_APPROVAL = "PENDING_GM_APPROVAL";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private Long id;
    private String orderNo;
    private String storeName;
    private String storeShortName;
    private String storeType;
    private Date expectedOpeningDate;
    private String regionName;
    private String regionLeader;
    private String generalManager;
    private String storeManagerName;
    private String province;
    private String city;
    private String district;
    private String addressDetail;
    private BigDecimal siteArea;
    private String siteMode;
    private Integer plannedStaffCount;
    private BigDecimal initialInvestmentAmount;
    private BigDecimal estimatedMonthlyRevenue;
    private String openingReason;
    private String delFlag;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    @NotBlank(message = "门店名称不能为空")
    @Size(max = 128, message = "门店名称不能超过128个字符")
    public String getStoreName()
    {
        return storeName;
    }

    public void setStoreName(String storeName)
    {
        this.storeName = storeName;
    }

    public String getStoreShortName()
    {
        return storeShortName;
    }

    public void setStoreShortName(String storeShortName)
    {
        this.storeShortName = storeShortName;
    }

    public String getStoreType()
    {
        return storeType;
    }

    public void setStoreType(String storeType)
    {
        this.storeType = storeType;
    }

    @NotNull(message = "预计开业日期不能为空")
    public Date getExpectedOpeningDate()
    {
        return expectedOpeningDate;
    }

    public void setExpectedOpeningDate(Date expectedOpeningDate)
    {
        this.expectedOpeningDate = expectedOpeningDate;
    }

    public String getRegionName()
    {
        return regionName;
    }

    public void setRegionName(String regionName)
    {
        this.regionName = regionName;
    }

    public String getRegionLeader()
    {
        return regionLeader;
    }

    public void setRegionLeader(String regionLeader)
    {
        this.regionLeader = regionLeader;
    }

    public String getGeneralManager()
    {
        return generalManager;
    }

    public void setGeneralManager(String generalManager)
    {
        this.generalManager = generalManager;
    }

    public String getStoreManagerName()
    {
        return storeManagerName;
    }

    public void setStoreManagerName(String storeManagerName)
    {
        this.storeManagerName = storeManagerName;
    }

    public String getProvince()
    {
        return province;
    }

    public void setProvince(String province)
    {
        this.province = province;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getDistrict()
    {
        return district;
    }

    public void setDistrict(String district)
    {
        this.district = district;
    }

    public String getAddressDetail()
    {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail)
    {
        this.addressDetail = addressDetail;
    }

    public BigDecimal getSiteArea()
    {
        return siteArea;
    }

    public void setSiteArea(BigDecimal siteArea)
    {
        this.siteArea = siteArea;
    }

    public String getSiteMode()
    {
        return siteMode;
    }

    public void setSiteMode(String siteMode)
    {
        this.siteMode = siteMode;
    }

    public Integer getPlannedStaffCount()
    {
        return plannedStaffCount;
    }

    public void setPlannedStaffCount(Integer plannedStaffCount)
    {
        this.plannedStaffCount = plannedStaffCount;
    }

    public BigDecimal getInitialInvestmentAmount()
    {
        return initialInvestmentAmount;
    }

    public void setInitialInvestmentAmount(BigDecimal initialInvestmentAmount)
    {
        this.initialInvestmentAmount = initialInvestmentAmount;
    }

    public BigDecimal getEstimatedMonthlyRevenue()
    {
        return estimatedMonthlyRevenue;
    }

    public void setEstimatedMonthlyRevenue(BigDecimal estimatedMonthlyRevenue)
    {
        this.estimatedMonthlyRevenue = estimatedMonthlyRevenue;
    }

    @Size(max = 1000, message = "开业说明不能超过1000个字符")
    public String getOpeningReason()
    {
        return openingReason;
    }

    public void setOpeningReason(String openingReason)
    {
        this.openingReason = openingReason;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("orderNo", getOrderNo())
                .append("storeName", getStoreName())
                .append("workflowStatus", getWorkflowStatus())
                .append("currentTaskName", getCurrentTaskName())
                .append("processInstanceId", getProcessInstanceId())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
