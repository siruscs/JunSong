package com.junsong.system.api.factory;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteOperationAuditService;

/**
 * R25 操作审计降级处理
 *
 * @author junsong
 * @since R25
 */
@Component
public class RemoteOperationAuditFallbackFactory implements FallbackFactory<RemoteOperationAuditService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteOperationAuditFallbackFactory.class);

    @Override
    public RemoteOperationAuditService create(Throwable cause) {
        log.error("操作审计服务调用失败:{}", cause.getMessage());
        return new RemoteOperationAuditService() {
            @Override
            public R<Boolean> recordSnapshot(Map<String, Object> body, String source) {
                return R.fail("操作审计服务调用失败:" + cause.getMessage());
            }
        };
    }
}
