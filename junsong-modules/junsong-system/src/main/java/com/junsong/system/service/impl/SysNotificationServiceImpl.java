package com.junsong.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.system.domain.SysNotification;
import com.junsong.system.mapper.SysNotificationMapper;
import com.junsong.system.service.ISysNotificationService;
import com.junsong.system.service.NotificationPushService;

/**
 * 系统通知 服务层实现
 */
@Service
public class SysNotificationServiceImpl implements ISysNotificationService
{
    @Autowired
    private SysNotificationMapper notificationMapper;

    @Autowired
    private NotificationPushService notificationPushService;

    @Override
    public List<SysNotification> selectNotificationList(SysNotification notification)
    {
        return notificationMapper.selectNotificationList(notification);
    }

    @Override
    public int selectUnreadCount(Long userId)
    {
        return notificationMapper.selectUnreadCount(userId);
    }

    @Override
    public int insertNotification(SysNotification notification)
    {
        // dedup_key 幂等：相同 user_id + dedupKey 已存在则跳过，避免重复通知
        if (notification.getDedupKey() != null && !notification.getDedupKey().isEmpty()
                && notification.getUserId() != null)
        {
            int exists = notificationMapper.countByUserDedupKey(
                    notification.getUserId(), notification.getDedupKey());
            if (exists > 0)
            {
                return 0;
            }
        }
        int rows = notificationMapper.insertNotification(notification);
        if (rows > 0 && notification.getUserId() != null)
        {
            notificationPushService.pushToUser(notification.getUserId(), notification);
        }
        return rows;
    }

    @Override
    public int batchInsertNotification(List<SysNotification> notifications)
    {
        int count = 0;
        for (SysNotification n : notifications)
        {
            int rows = notificationMapper.insertNotification(n);
            count += rows;
            if (rows > 0 && n.getUserId() != null)
            {
                notificationPushService.pushToUser(n.getUserId(), n);
            }
        }
        return count;
    }

    @Override
    public int markAsRead(Long id)
    {
        return notificationMapper.markAsRead(id);
    }

    @Override
    public int markAllAsRead(Long userId)
    {
        return notificationMapper.markAllAsRead(userId);
    }

    @Override
    public int deleteNotificationByIds(Long[] ids)
    {
        return notificationMapper.deleteNotificationByIds(ids);
    }

    @Override
    public int countByUserTypeBizId(Long userId, String type, String bizId)
    {
        return notificationMapper.countByUserTypeBizId(userId, type, bizId);
    }
}
