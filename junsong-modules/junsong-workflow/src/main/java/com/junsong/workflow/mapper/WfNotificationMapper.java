package com.junsong.workflow.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 向 sys_notification 表写入通知（轻量级，避免跨模块依赖）
 */
public interface WfNotificationMapper
{
    @Insert("INSERT INTO sys_notification(user_id, title, content, type, link_url, biz_id, is_read, create_time) " +
            "VALUES(#{userId}, #{title}, #{content}, #{type}, #{linkUrl}, #{bizId}, '0', NOW())")
    int insertNotification(@Param("userId") Long userId,
                           @Param("title") String title,
                           @Param("content") String content,
                           @Param("type") String type,
                           @Param("linkUrl") String linkUrl,
                           @Param("bizId") String bizId);
}
