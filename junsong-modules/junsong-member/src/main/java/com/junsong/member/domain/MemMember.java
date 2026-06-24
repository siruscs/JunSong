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
 * 会员信息对象 mem_member
 *
 * @author junsong
 */
public class MemMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会员ID */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    private Long memberId;

    /** 部门ID */
    private Long deptId;

    /** 会员编号 */
    @Excel(name = "会员编号")
    @NotBlank(message = "会员编号不能为空")
    @Size(max = 64, message = "会员编号长度不能超过64个字符")
    private String memberNo;

    /** 会员姓名 */
    @Excel(name = "会员姓名")
    @NotBlank(message = "会员姓名不能为空")
    @Size(max = 64, message = "会员姓名长度不能超过64个字符")
    private String memberName;

    /** 手机号码 */
    @Excel(name = "手机号码")
    @Size(max = 32, message = "手机号码长度不能超过32个字符")
    private String phone;

    /** 年龄 */
    @Excel(name = "年龄", cellType = ColumnType.NUMERIC)
    private Integer age;

    /** 住址 */
    @Excel(name = "住址")
    @Size(max = 256, message = "住址长度不能超过256个字符")
    private String address;

    /** 身份证号 */
    @Excel(name = "身份证号")
    @Size(max = 32, message = "身份证号长度不能超过32个字符")
    private String idCard;

    /** 会员卡类型 */
    @Excel(name = "会员卡类型", combo = {"experience", "formal", "star1", "star2", "star3", "star4", "star5"})
    @Size(max = 32, message = "会员卡类型长度不能超过32个字符")
    private String cardType;

    /** 会员卡ID */
    private Long cardId;

    /** 入会日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "入会日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date joinDate;

    /** 有效期至 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "有效期至", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireDate;

    /** 总积分 */
    @Excel(name = "总积分", cellType = ColumnType.NUMERIC)
    private BigDecimal totalPoints;

    /** 可用积分 */
    @Excel(name = "可用积分", cellType = ColumnType.NUMERIC)
    private BigDecimal availablePoints;

    /** 秒杀次数（非数据库字段，用于列表展示） */
    private Integer seckillCount;

    /** 状态(0正常/1无效/2已退卡) */
    @Excel(name = "状态", readConverterExp = "0=正常,1=无效,2=已退卡")
    private String status;

    /** 备注 */
    @Excel(name = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getMemberNo()
    {
        return memberNo;
    }

    public void setMemberNo(String memberNo)
    {
        this.memberNo = memberNo;
    }

    public String getMemberName()
    {
        return memberName;
    }

    public void setMemberName(String memberName)
    {
        this.memberName = memberName;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public Integer getAge()
    {
        return age;
    }

    public void setAge(Integer age)
    {
        this.age = age;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getIdCard()
    {
        return idCard;
    }

    public void setIdCard(String idCard)
    {
        this.idCard = idCard;
    }

    public String getCardType()
    {
        return cardType;
    }

    public void setCardType(String cardType)
    {
        this.cardType = cardType;
    }

    public Long getCardId()
    {
        return cardId;
    }

    public void setCardId(Long cardId)
    {
        this.cardId = cardId;
    }

    public Date getJoinDate()
    {
        return joinDate;
    }

    public void setJoinDate(Date joinDate)
    {
        this.joinDate = joinDate;
    }

    public Date getExpireDate()
    {
        return expireDate;
    }

    public void setExpireDate(Date expireDate)
    {
        this.expireDate = expireDate;
    }

    public BigDecimal getTotalPoints()
    {
        return totalPoints;
    }

    public void setTotalPoints(BigDecimal totalPoints)
    {
        this.totalPoints = totalPoints;
    }

    public BigDecimal getAvailablePoints()
    {
        return availablePoints;
    }

    public void setAvailablePoints(BigDecimal availablePoints)
    {
        this.availablePoints = availablePoints;
    }

    public Integer getSeckillCount()
    {
        return seckillCount;
    }

    public void setSeckillCount(Integer seckillCount)
    {
        this.seckillCount = seckillCount;
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
        return "MemMember{" +
            "memberId=" + memberId +
            ", memberNo='" + memberNo + '\'' +
            ", memberName='" + memberName + '\'' +
            ", phone='" + phone + '\'' +
            ", age=" + age +
            ", address='" + address + '\'' +
            ", idCard='" + idCard + '\'' +
            ", cardType='" + cardType + '\'' +
            ", status='" + status + '\'' +
            ", totalPoints=" + totalPoints +
            ", availablePoints=" + availablePoints +
            ", seckillCount=" + seckillCount +
            '}';
    }
}
