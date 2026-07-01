package com.junsong.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysNotification;
import com.junsong.system.service.ISysNotificationService;

/**
 * 系统通知 控制器
 */
@RestController
@RequestMapping("/notification")
public class SysNotificationController extends BaseController
{
    @Autowired
    private ISysNotificationService notificationService;

    /**
     * 获取当前用户通知列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysNotification notification)
    {
        Long userId = SecurityUtils.getUserId();
        notification.setUserId(userId);
        startPage();
        List<SysNotification> list = notificationService.selectNotificationList(notification);
        return getDataTable(list);
    }

    /**
     * 获取未读通知数
     */
    @GetMapping("/unread-count")
    public AjaxResult unreadCount()
    {
        Long userId = SecurityUtils.getUserId();
        int count = notificationService.selectUnreadCount(userId);
        return AjaxResult.success(count);
    }

    /**
     * 标记已读
     */
    @PutMapping("/read/{id}")
    public AjaxResult markRead(@PathVariable Long id)
    {
        notificationService.markAsRead(id);
        return AjaxResult.success();
    }

    /**
     * 标记全部已读
     */
    @PutMapping("/read-all")
    public AjaxResult markAllRead()
    {
        Long userId = SecurityUtils.getUserId();
        notificationService.markAllAsRead(userId);
        return AjaxResult.success();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        notificationService.deleteNotificationByIds(ids);
        return AjaxResult.success();
    }
}
