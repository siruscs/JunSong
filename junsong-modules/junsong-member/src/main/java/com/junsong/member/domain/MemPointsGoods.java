package com.junsong.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 积分物品表对象 mem_points_goods
 *
 * @author junsong
 */
public class MemPointsGoods extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 物品ID */
    @Excel(name = "物品ID", cellType = ColumnType.NUMERIC)
    private Long goodsId;

    /** 部门ID */
    private Long deptId;

    /** 物品名称 */
    @Excel(name = "物品名称")
    @NotBlank(message = "物品名称不能为空")
    @Size(max = 128, message = "物品名称长度不能超过128个字符")
    private String goodsName;

    /** 物品编码 */
    @Excel(name = "物品编码")
    @NotBlank(message = "物品编码不能为空")
    @Size(max = 64, message = "物品编码长度不能超过64个字符")
    private String goodsCode;

    /** 物品价值(元) */
    @Excel(name = "物品价值(元)", cellType = ColumnType.NUMERIC)
    private BigDecimal goodsValue;

    /** 积分价格 */
    @Excel(name = "积分价格", cellType = ColumnType.NUMERIC)
    @NotNull(message = "积分价格不能为空")
    private BigDecimal pointsPrice;

    /** 库存 */
    @Excel(name = "库存", cellType = ColumnType.NUMERIC)
    private Integer stock;

    /** 已兑换数量 */
    @Excel(name = "已兑换数量", cellType = ColumnType.NUMERIC)
    private Integer exchanged;

    /** 物品图片 */
    @Excel(name = "物品图片")
    @Size(max = 256, message = "物品图片长度不能超过256个字符")
    private String goodsImage;

    /** 状态(0上架/1下架) */
    @Excel(name = "状态", readConverterExp = "0=上架,1=下架")
    private String status;

    /** 删除标志 */
    private String delFlag;

    public Long getGoodsId()
    {
        return goodsId;
    }

    public void setGoodsId(Long goodsId)
    {
        this.goodsId = goodsId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getGoodsName()
    {
        return goodsName;
    }

    public void setGoodsName(String goodsName)
    {
        this.goodsName = goodsName;
    }

    public String getGoodsCode()
    {
        return goodsCode;
    }

    public void setGoodsCode(String goodsCode)
    {
        this.goodsCode = goodsCode;
    }

    public BigDecimal getGoodsValue()
    {
        return goodsValue;
    }

    public void setGoodsValue(BigDecimal goodsValue)
    {
        this.goodsValue = goodsValue;
    }

    public BigDecimal getPointsPrice()
    {
        return pointsPrice;
    }

    public void setPointsPrice(BigDecimal pointsPrice)
    {
        this.pointsPrice = pointsPrice;
    }

    public Integer getStock()
    {
        return stock;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    public Integer getExchanged()
    {
        return exchanged;
    }

    public void setExchanged(Integer exchanged)
    {
        this.exchanged = exchanged;
    }

    public String getGoodsImage()
    {
        return goodsImage;
    }

    public void setGoodsImage(String goodsImage)
    {
        this.goodsImage = goodsImage;
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

    @Override
    public String toString() {
        return "MemPointsGoods{" +
            "goodsId=" + goodsId +
            ", goodsName='" + goodsName + '\'' +
            ", goodsCode='" + goodsCode + '\'' +
            ", goodsValue=" + goodsValue +
            ", pointsPrice=" + pointsPrice +
            ", stock=" + stock +
            ", exchanged=" + exchanged +
            ", status='" + status + '\'' +
            '}';
    }
}
