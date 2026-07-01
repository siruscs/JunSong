package com.junsong.system.api.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteNotificationService;
import java.util.List;
import java.util.Map;

/**
 * 通知服务降级处理
 *
 * @author junsong
 * @since NIGHT-P1-R3B
 */
@Component
public class RemoteNotificationFallbackFactory implements FallbackFactory<RemoteNotificationService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteNotificationFallbackFactory.class);

    @Override
    public RemoteNotificationService create(Throwable cause) {
        log.error("通知服务调用失败:{}", cause.getMessage());
        return new RemoteNotificationService() {
            @Override
            public R<Boolean> batchSendNotification(List<Map<String, Object>> notifications, String source) {
                return R.fail("获取通知服务失败:" + cause.getMessage());
            }

            @Override
            public R<Boolean> checkNotificationExists(Long userId, String type, String bizId, String source) {
                log.warn("通知去重查询降级: userId={}, type={}, bizId={}, cause={}", userId, type, bizId, cause.getMessage());
                return R.ok(false); // Degrade to "not exists" so notification is still sent
            }
        };
    }
}
