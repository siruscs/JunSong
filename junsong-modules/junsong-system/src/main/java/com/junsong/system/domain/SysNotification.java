package com.junsong.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 系统通知表 sys_notification
 */
public class SysNotification extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 通知ID */
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型（register_audit=注册审核, system=系统） */
    private String type;

    /** 跳转链接 */
    private String linkUrl;

    /** 业务ID */
    private String bizId;

    /** 是否已读（0未读 1已读） */
    private String isRead;

    /** 阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getLinkUrl()
    {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl)
    {
        this.linkUrl = linkUrl;
    }

    public String getBizId()
    {
        return bizId;
    }

    public void setBizId(String bizId)
    {
        this.bizId = bizId;
    }

    public String getIsRead()
    {
        return isRead;
    }

    public void setIsRead(String isRead)
    {
        this.isRead = isRead;
    }

    public Date getReadTime()
    {
        return readTime;
    }

    public void setReadTime(Date readTime)
    {
        this.readTime = readTime;
    }
}
