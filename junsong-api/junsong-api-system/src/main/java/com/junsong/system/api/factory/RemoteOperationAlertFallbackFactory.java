package com.junsong.system.api.factory;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteOperationAlertService;

/**
 * R25 操作告警降级处理
 *
 * @author junsong
 * @since R25
 */
@Component
public class RemoteOperationAlertFallbackFactory implements FallbackFactory<RemoteOperationAlertService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteOperationAlertFallbackFactory.class);

    @Override
    public RemoteOperationAlertService create(Throwable cause) {
        log.error("操作告警服务调用失败:{}", cause.getMessage());
        return new RemoteOperationAlertService() {
            @Override
            public R<Boolean> raiseAlert(Map<String, Object> body, String source) {
                return R.fail("操作告警服务调用失败:" + cause.getMessage());
            }
        };
    }
}
