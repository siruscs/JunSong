package com.junsong.auth.form;

/**
 * 微信绑定现有账号请求体
 */
public class WechatMpBindBody
{
    /** 微信 wx.login() 返回的临时 code */
    private String code;

    /** 已有系统账号用户名 */
    private String username;

    /** 已有系统账号密码 */
    private String password;

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

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
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
