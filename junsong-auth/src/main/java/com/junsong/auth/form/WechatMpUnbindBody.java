package com.junsong.auth.form;

/**
 * 微信解绑请求体
 */
public class WechatMpUnbindBody
{
    /** 解绑原因（可选，用于审计日志） */
    private String revokeReason;

    public String getRevokeReason()
    {
        return revokeReason;
    }

    public void setRevokeReason(String revokeReason)
    {
        this.revokeReason = revokeReason;
    }
}
