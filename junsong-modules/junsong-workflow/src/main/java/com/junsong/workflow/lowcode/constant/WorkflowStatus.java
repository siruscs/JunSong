package com.junsong.workflow.lowcode.constant;

/**
 * 低代码业务流程状态枚举
 * 统一管理散落在各处的状态字符串，避免魔法值
 *
 * @author junsong
 */
public enum WorkflowStatus
{
    /** 草稿 */
    DRAFT("DRAFT", "草稿"),
    /** 审批中 */
    IN_APPROVAL("IN_APPROVAL", "审批中"),
    /** 已通过 */
    APPROVED("APPROVED", "已通过"),
    /** 已驳回 */
    REJECTED("REJECTED", "已驳回"),
    /** 已撤回 */
    WITHDRAWN("WITHDRAWN", "已撤回"),
    /** 待履约 */
    PENDING_FULFILLMENT("PENDING_FULFILLMENT", "待履约"),
    /** 待财务审批 */
    PENDING_FINANCE_APPROVAL("PENDING_FINANCE_APPROVAL", "待财务审批"),
    /** 待总经理审批 */
    PENDING_GM_APPROVAL("PENDING_GM_APPROVAL", "待总经理审批"),
    /** 已履约 */
    FULFILLED("FULFILLED", "已履约"),
    /** 已取消 */
    CANCELLED("CANCELLED", "已取消"),
    /** 已过期 */
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String label;

    WorkflowStatus(String code, String label)
    {
        this.code = code;
        this.label = label;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    /**
     * 根据 code 获取枚举
     */
    public static WorkflowStatus fromCode(String code)
    {
        if (code == null) return null;
        for (WorkflowStatus status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断 code 是否匹配
     */
    public boolean matches(String code)
    {
        return this.code.equals(code);
    }
}
