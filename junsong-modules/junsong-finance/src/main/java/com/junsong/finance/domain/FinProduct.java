package com.junsong.finance.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 商品对象 fin_product
 * 
 * @author junsong
 */
public class FinProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 商品ID */
    @Excel(name = "商品ID", cellType = ColumnType.NUMERIC)
    private Long productId;


    /** 商品编码 */
    @Excel(name = "商品编码")
    private String productCode;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 商品分类ID */
    @Excel(name = "商品分类ID", cellType = ColumnType.NUMERIC)
    private Long categoryId;

    /** 计量单位 */
    @Excel(name = "计量单位")
    private String unit;

    /** 进货价格 */
    @Excel(name = "进货价格", cellType = ColumnType.NUMERIC)
    private BigDecimal purchasePrice;

    /** 销售价格 */
    @Excel(name = "销售价格", cellType = ColumnType.NUMERIC)
    private BigDecimal salePrice;

    /** 库存数量 */
    @Excel(name = "库存数量", cellType = ColumnType.NUMERIC)
    private Integer stockNum;

    /** 最低库存预警 */
    @Excel(name = "最低库存预警", cellType = ColumnType.NUMERIC)
    private Integer minStock;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 部门ID */
    private Long deptId;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }


    @Size(min = 0, max = 64, message = "商品编码长度不能超过64个字符")
    public String getProductCode()
    {
        return productCode;
    }

    public void setProductCode(String productCode)
    {
        this.productCode = productCode;
    }

    @NotBlank(message = "商品名称不能为空")
    @Size(min = 0, max = 128, message = "商品名称长度不能超过128个字符")
    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    @Size(min = 0, max = 32, message = "计量单位长度不能超过32个字符")
    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public BigDecimal getPurchasePrice()
    {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice)
    {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSalePrice()
    {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice)
    {
        this.salePrice = salePrice;
    }

    public Integer getStockNum()
    {
        return stockNum;
    }

    public void setStockNum(Integer stockNum)
    {
        this.stockNum = stockNum;
    }

    public Integer getMinStock()
    {
        return minStock;
    }

    public void setMinStock(Integer minStock)
    {
        this.minStock = minStock;
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
            .append("productId", getProductId())
            .append("productCode", getProductCode())
            .append("productName", getProductName())
            .append("categoryId", getCategoryId())
            .append("unit", getUnit())
            .append("purchasePrice", getPurchasePrice())
            .append("salePrice", getSalePrice())
            .append("stockNum", getStockNum())
            .append("minStock", getMinStock())
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
