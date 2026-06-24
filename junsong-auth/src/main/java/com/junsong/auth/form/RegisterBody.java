package com.junsong.auth.form;

/**
 * 用户注册对象
 * 
 * @author junsong
 */
public class RegisterBody extends LoginBody
{
    /** 姓名（昵称） */
    private String nickName;

    /** 手机号码 */
    private String phonenumber;

    /** 身份证号 */
    private String idCard;

    /** 邀请码 */
    private String inviteCode;

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getIdCard()
    {
        return idCard;
    }

    public void setIdCard(String idCard)
    {
        this.idCard = idCard;
    }

    public String getInviteCode()
    {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode)
    {
        this.inviteCode = inviteCode;
    }
}
