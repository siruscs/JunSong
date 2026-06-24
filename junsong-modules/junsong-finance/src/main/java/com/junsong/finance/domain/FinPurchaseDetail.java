package com.junsong.finance.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 进货单明细对象 fin_purchase_detail
 * 
 * @author junsong
 */
public class FinPurchaseDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 明细ID */
    @Excel(name = "明细ID", cellType = ColumnType.NUMERIC)
    private Long detailId;


    /** 进货单ID */
    @Excel(name = "进货单ID", cellType = ColumnType.NUMERIC)
    private Long purchaseId;

    /** 商品ID */
    @Excel(name = "商品ID", cellType = ColumnType.NUMERIC)
    private Long productId;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 计量单位 */
    @Excel(name = "计量单位")
    private String unit;

    /** 数量 */
    @Excel(name = "数量", cellType = ColumnType.NUMERIC)
    private Integer quantity;

    /** 单价 */
    @Excel(name = "单价", cellType = ColumnType.NUMERIC)
    private BigDecimal price;

    /** 金额 */
    @Excel(name = "金额", cellType = ColumnType.NUMERIC)
    private BigDecimal amount;

    /** 是否赠品（0否 1是） */
    @Excel(name = "是否赠品", readConverterExp = "0=否,1=是")
    private String isGift;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 部门ID */
    private Long deptId;

    public Long getDetailId()
    {
        return detailId;
    }

    public void setDetailId(Long detailId)
    {
        this.detailId = detailId;
    }


    public Long getPurchaseId()
    {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId)
    {
        this.purchaseId = purchaseId;
    }

    @NotNull(message = "商品ID不能为空")
    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    @NotNull(message = "数量不能为空")
    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    @NotNull(message = "单价不能为空")
    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    @Size(min = 0, max = 1, message = "是否赠品长度不能超过1个字符")
    public String getIsGift()
    {
        return isGift;
    }

    public void setIsGift(String isGift)
    {
        this.isGift = isGift;
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
            .append("detailId", getDetailId())
            .append("purchaseId", getPurchaseId())
            .append("productId", getProductId())
            .append("productName", getProductName())
            .append("unit", getUnit())
            .append("quantity", getQuantity())
            .append("price", getPrice())
            .append("amount", getAmount())
            .append("isGift", getIsGift())
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
