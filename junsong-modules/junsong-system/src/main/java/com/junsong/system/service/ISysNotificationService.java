package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.SysNotification;

/**
 * 系统通知 服务层
 */
public interface ISysNotificationService
{
    /**
     * 查询通知列表
     */
    public List<SysNotification> selectNotificationList(SysNotification notification);

    /**
     * 查询用户未读通知数
     */
    public int selectUnreadCount(Long userId);

    /**
     * 新增通知
     */
    public int insertNotification(SysNotification notification);

    /**
     * 批量发送通知
     */
    public int batchInsertNotification(List<SysNotification> notifications);

    /**
     * 标记已读
     */
    public int markAsRead(Long id, Long userId);

    /**
     * 标记用户全部已读
     */
    public int markAllAsRead(Long userId);

    /**
     * 批量删除通知
     */
    public int deleteNotificationByIds(Long[] ids);

    /**
     * 按用户、类型、bizId 统计通知数量（用于去重）
     */
    int countByUserTypeBizId(Long userId, String type, String bizId);
}
