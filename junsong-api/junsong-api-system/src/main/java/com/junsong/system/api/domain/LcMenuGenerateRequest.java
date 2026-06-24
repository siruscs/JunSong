package com.junsong.system.api.domain;

import java.io.Serializable;

/**
 * 低代码菜单自动生成入参
 */
public class LcMenuGenerateRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 业务编码，作为菜单 path 末段 */
    private String bizCode;

    /** 菜单显示名 */
    private String bizName;

    /** 父目录名（不存在则创建顶级目录"低代码应用"） */
    private String parentMenuName;

    /** 菜单图标，默认 documentation */
    private String icon;

    public String getBizCode() { return bizCode; }
    public void setBizCode(String bizCode) { this.bizCode = bizCode; }
    public String getBizName() { return bizName; }
    public void setBizName(String bizName) { this.bizName = bizName; }
    public String getParentMenuName() { return parentMenuName; }
    public void setParentMenuName(String parentMenuName) { this.parentMenuName = parentMenuName; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
