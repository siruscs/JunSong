package com.junsong.finance.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 供应商对象 fin_supplier
 * 
 * @author junsong
 */
public class FinSupplier extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 供应商ID */
    @Excel(name = "供应商ID", cellType = ColumnType.NUMERIC)
    private Long supplierId;


    /** 供应商编码 */
    @Excel(name = "供应商编码")
    private String supplierCode;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String supplierName;

    /** 联系人 */
    @Excel(name = "联系人")
    private String contactPerson;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String contactPhone;

    /** 地址 */
    @Excel(name = "地址")
    private String address;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 部门ID */
    private Long deptId;

    public Long getSupplierId()
    {
        return supplierId;
    }

    public void setSupplierId(Long supplierId)
    {
        this.supplierId = supplierId;
    }


    @Size(min = 0, max = 64, message = "供应商编码长度不能超过64个字符")
    public String getSupplierCode()
    {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode)
    {
        this.supplierCode = supplierCode;
    }

    @NotBlank(message = "供应商名称不能为空")
    @Size(min = 0, max = 128, message = "供应商名称长度不能超过128个字符")
    public String getSupplierName()
    {
        return supplierName;
    }

    public void setSupplierName(String supplierName)
    {
        this.supplierName = supplierName;
    }

    @Size(min = 0, max = 64, message = "联系人长度不能超过64个字符")
    public String getContactPerson()
    {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson)
    {
        this.contactPerson = contactPerson;
    }

    @Size(min = 0, max = 32, message = "联系电话长度不能超过32个字符")
    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    @Size(min = 0, max = 256, message = "地址长度不能超过256个字符")
    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("supplierId", getSupplierId())
            .append("supplierCode", getSupplierCode())
            .append("supplierName", getSupplierName())
            .append("contactPerson", getContactPerson())
            .append("contactPhone", getContactPhone())
            .append("address", getAddress())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("deptId", getDeptId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
