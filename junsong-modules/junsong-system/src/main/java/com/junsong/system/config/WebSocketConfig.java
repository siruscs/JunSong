package com.junsong.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 配置
 *
 * 提供实时通知推送能力。客户端通过 STOMP over WebSocket 连接后，
 * 可订阅 /user/queue/notifications 接收个人通知。
 *
 * 消息代理：
 *   /topic — 广播主题（管理员全站推送）
 *   /queue — 点对点队列（个人通知）
 *
 * 端点：
 *   /ws/notification — WebSocket 端点（支持 SockJS 降级）
 *
 * @author junsong
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
{
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry)
    {
        // 简单内存消息代理（生产环境可替换为 RabbitMQ/ActiveMQ）
        registry.enableSimpleBroker("/topic", "/queue");
        // 客户端发送消息的前缀（本场景不需要客户端发送，仅做预留）
        registry.setApplicationDestinationPrefixes("/app");
        // 用户专属目标前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry)
    {
        registry.addEndpoint("/ws/notification")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
