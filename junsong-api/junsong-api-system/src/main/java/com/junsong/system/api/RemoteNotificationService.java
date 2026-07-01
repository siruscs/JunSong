package com.junsong.system.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.factory.RemoteNotificationFallbackFactory;
import java.util.List;
import java.util.Map;

/**
 * 通知服务远程调用接口
 *
 * @author junsong
 * @since NIGHT-P1-R3B
 */
@FeignClient(contextId = "remoteNotificationService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteNotificationFallbackFactory.class)
public interface RemoteNotificationService {

    /**
     * 批量发送系统通知（内部服务调用）
     *
     * @param notifications 通知列表，每条包含 userId、title、content、type、linkUrl、bizId
     * @param source 请求来源标识（由 SecurityConstants.FROM_SOURCE 传入）
     * @return 操作结果
     */
    @PostMapping("/notification/internal/batch-send")
    R<Boolean> batchSendNotification(
            @RequestBody List<Map<String, Object>> notifications,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 检查通知是否已存在（去重查询，内部服务调用）
     *
     * @param userId 用户ID
     * @param type   通知类型
     * @param bizId  业务唯一标识
     * @param source 请求来源标识
     * @return true 表示已存在（应跳过发送）
     */
    @GetMapping("/notification/internal/check-exists")
    R<Boolean> checkNotificationExists(
            @RequestParam("userId") Long userId,
            @RequestParam("type") String type,
            @RequestParam("bizId") String bizId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
