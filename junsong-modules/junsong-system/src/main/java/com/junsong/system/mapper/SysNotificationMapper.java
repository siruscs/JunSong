package com.junsong.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.system.domain.SysNotification;

/**
 * 系统通知表 数据层
 */
public interface SysNotificationMapper
{
    /**
     * 查询通知信息
     */
    public SysNotification selectNotificationById(Long id);

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
     * 标记已读
     */
    public int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

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
    int countByUserTypeBizId(@Param("userId") Long userId, @Param("type") String type, @Param("bizId") String bizId);

    /**
     * 按用户、dedupKey 统计通知数量（用于 R7-C 工作台告警去重）
     */
    int countByUserDedupKey(@Param("userId") Long userId, @Param("dedupKey") String dedupKey);
}
