package com.junsong.member.domain;

import com.junsong.common.core.workflow.AbstractWorkflowBusinessEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class MemRefundApply extends AbstractWorkflowBusinessEntity
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING_STORE_APPROVAL = "PENDING_STORE_APPROVAL";
    public static final String STATUS_PENDING_FINANCE_APPROVAL = "PENDING_FINANCE_APPROVAL";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private Long id;
    private String refundNo;
    private Long memberId;
    private String memberName;
    private String memberPhone;
    private Long storeId;
    private String storeName;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundRemark;
    private String storeApprover;
    private String financeApprover;
    private String delFlag;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getRefundNo()
    {
        return refundNo;
    }

    public void setRefundNo(String refundNo)
    {
        this.refundNo = refundNo;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    @NotBlank(message = "会员姓名不能为空")
    @Size(max = 64, message = "会员姓名不能超过64个字符")
    public String getMemberName()
    {
        return memberName;
    }

    public void setMemberName(String memberName)
    {
        this.memberName = memberName;
    }

    public String getMemberPhone()
    {
        return memberPhone;
    }

    public void setMemberPhone(String memberPhone)
    {
        this.memberPhone = memberPhone;
    }

    public Long getStoreId()
    {
        return storeId;
    }

    public void setStoreId(Long storeId)
    {
        this.storeId = storeId;
    }

    public String getStoreName()
    {
        return storeName;
    }

    public void setStoreName(String storeName)
    {
        this.storeName = storeName;
    }

    @NotNull(message = "退款金额不能为空")
    public BigDecimal getRefundAmount()
    {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount)
    {
        this.refundAmount = refundAmount;
    }

    @Size(max = 255, message = "退款原因不能超过255个字符")
    public String getRefundReason()
    {
        return refundReason;
    }

    public void setRefundReason(String refundReason)
    {
        this.refundReason = refundReason;
    }

    @Size(max = 500, message = "退款备注不能超过500个字符")
    public String getRefundRemark()
    {
        return refundRemark;
    }

    public void setRefundRemark(String refundRemark)
    {
        this.refundRemark = refundRemark;
    }

    public String getStoreApprover()
    {
        return storeApprover;
    }

    public void setStoreApprover(String storeApprover)
    {
        this.storeApprover = storeApprover;
    }

    public String getFinanceApprover()
    {
        return financeApprover;
    }

    public void setFinanceApprover(String financeApprover)
    {
        this.financeApprover = financeApprover;
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
                .append("refundNo", getRefundNo())
                .append("memberName", getMemberName())
                .append("refundAmount", getRefundAmount())
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
