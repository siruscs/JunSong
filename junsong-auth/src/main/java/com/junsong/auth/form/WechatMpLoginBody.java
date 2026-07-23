package com.junsong.auth.form;

/**
 * 微信快捷登录请求体
 */
public class WechatMpLoginBody
{
    /** 微信 wx.login() 返回的临时 code */
    private String code;

    /** 部门/门店ID（可选，不传时使用用户默认部门） */
    private Long deptId;

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }
}
