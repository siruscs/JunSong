package com.junsong.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.junsong.system.domain.SysNotification;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 通知推送服务
 *
 * 通过 STOMP over WebSocket 将通知实时推送到前端。
 * 个人通知发往 /user/{userId}/queue/notifications，
 * 全站广播发往 /topic/notification-broadcast。
 *
 * @author junsong
 */
@Service
public class NotificationPushService
{
    private static final Logger log = LoggerFactory.getLogger(NotificationPushService.class);

    private static final String USER_QUEUE_NOTIFICATIONS = "/queue/notifications";
    private static final String TOPIC_BROADCAST = "/topic/notification-broadcast";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 向指定用户推送通知
     *
     * @param userId       目标用户 ID
     * @param notification 通知实体
     */
    public void pushToUser(Long userId, SysNotification notification)
    {
        try
        {
            ObjectNode payload = buildPayload(notification);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId), USER_QUEUE_NOTIFICATIONS, payload);
            log.debug("[NotificationPush] 推送通知到用户 userId={}, title={}", userId, notification.getTitle());
        }
        catch (Exception e)
        {
            log.warn("[NotificationPush] 推送失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 全站广播通知（如系统公告）
     *
     * @param notification 通知实体
     */
    public void broadcast(SysNotification notification)
    {
        try
        {
            ObjectNode payload = buildPayload(notification);
            messagingTemplate.convertAndSend(TOPIC_BROADCAST, payload);
            log.debug("[NotificationPush] 全站广播: {}", notification.getTitle());
        }
        catch (Exception e)
        {
            log.warn("[NotificationPush] 广播失败: {}", e.getMessage());
        }
    }

    private ObjectNode buildPayload(SysNotification n)
    {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", n.getId());
        node.put("title", n.getTitle() != null ? n.getTitle() : "");
        node.put("content", n.getContent() != null ? n.getContent() : "");
        node.put("type", n.getType() != null ? n.getType() : "");
        node.put("linkUrl", n.getLinkUrl());
        node.put("bizId", n.getBizId());
        node.put("createTime", n.getCreateTime() != null ? n.getCreateTime().toString() : "");
        return node;
    }
}
