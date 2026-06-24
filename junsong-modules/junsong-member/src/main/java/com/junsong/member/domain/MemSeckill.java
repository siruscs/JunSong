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
 * 秒杀/团购活动对象 mem_seckill
 *
 * @author junsong
 */
public class MemSeckill extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 秒杀ID */
    @Excel(name = "秒杀ID", cellType = ColumnType.NUMERIC)
    private Long seckillId;

    /** 部门ID */
    private Long deptId;

    /** 秒杀编号 */
    @Excel(name = "秒杀编号")
    @NotBlank(message = "秒杀编号不能为空")
    @Size(max = 64, message = "秒杀编号长度不能超过64个字符")
    private String seckillNo;

    /** 秒杀名称 */
    @Excel(name = "秒杀名称")
    @NotBlank(message = "秒杀名称不能为空")
    @Size(max = 128, message = "秒杀名称长度不能超过128个字符")
    private String seckillName;

    /** 类型(秒杀/团购) */
    @Excel(name = "类型", readConverterExp = "秒杀=秒杀,团购=团购")
    @NotBlank(message = "类型不能为空")
    @Size(max = 32, message = "类型长度不能超过32个字符")
    private String seckillType;

    /** 秒杀日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "秒杀日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date seckillDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 秒杀时间段 */
    @Excel(name = "秒杀时间段")
    @Size(max = 64, message = "秒杀时间段长度不能超过64个字符")
    private String seckillTime;

    /** 秒杀金额 */
    @Excel(name = "秒杀金额", cellType = ColumnType.NUMERIC)
    private BigDecimal seckillAmount;

    /** 秒杀单价 */
    @Excel(name = "秒杀单价", cellType = ColumnType.NUMERIC)
    private BigDecimal seckillPrice;

    /** 总份额 */
    @Excel(name = "总份额", cellType = ColumnType.NUMERIC)
    private Integer totalShares;

    /** 剩余份额 */
    @Excel(name = "剩余份额", cellType = ColumnType.NUMERIC)
    private Integer remainShares;

    /** 秒杀政策 */
    @Excel(name = "秒杀政策")
    @Size(max = 500, message = "秒杀政策长度不能超过500个字符")
    private String policy;

    /** 状态(0进行中/1已结束/2已取消) */
    @Excel(name = "状态", readConverterExp = "0=进行中,1=已结束,2=已取消")
    private String status;

    /** 备注 */
    @Excel(name = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getSeckillId()
    {
        return seckillId;
    }

    public void setSeckillId(Long seckillId)
    {
        this.seckillId = seckillId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getSeckillNo()
    {
        return seckillNo;
    }

    public void setSeckillNo(String seckillNo)
    {
        this.seckillNo = seckillNo;
    }

    public String getSeckillName()
    {
        return seckillName;
    }

    public void setSeckillName(String seckillName)
    {
        this.seckillName = seckillName;
    }

    public String getSeckillType()
    {
        return seckillType;
    }

    public void setSeckillType(String seckillType)
    {
        this.seckillType = seckillType;
    }

    public Date getSeckillDate()
    {
        return seckillDate;
    }

    public void setSeckillDate(Date seckillDate)
    {
        this.seckillDate = seckillDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public String getSeckillTime()
    {
        return seckillTime;
    }

    public void setSeckillTime(String seckillTime)
    {
        this.seckillTime = seckillTime;
    }

    public BigDecimal getSeckillAmount()
    {
        return seckillAmount;
    }

    public void setSeckillAmount(BigDecimal seckillAmount)
    {
        this.seckillAmount = seckillAmount;
    }

    public BigDecimal getSeckillPrice()
    {
        return seckillPrice;
    }

    public void setSeckillPrice(BigDecimal seckillPrice)
    {
        this.seckillPrice = seckillPrice;
    }

    public Integer getTotalShares()
    {
        return totalShares;
    }

    public void setTotalShares(Integer totalShares)
    {
        this.totalShares = totalShares;
    }

    public Integer getRemainShares()
    {
        return remainShares;
    }

    public void setRemainShares(Integer remainShares)
    {
        this.remainShares = remainShares;
    }

    public String getPolicy()
    {
        return policy;
    }

    public void setPolicy(String policy)
    {
        this.policy = policy;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
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
        return "MemSeckill{" +
            "seckillId=" + seckillId +
            ", seckillNo='" + seckillNo + '\'' +
            ", seckillName='" + seckillName + '\'' +
            ", seckillType='" + seckillType + '\'' +
            ", seckillDate=" + seckillDate +
            ", endDate=" + endDate +
            ", seckillTime='" + seckillTime + '\'' +
            ", seckillAmount=" + seckillAmount +
            ", seckillPrice=" + seckillPrice +
            ", totalShares=" + totalShares +
            ", remainShares=" + remainShares +
            ", status='" + status + '\'' +
            '}';
    }
}
