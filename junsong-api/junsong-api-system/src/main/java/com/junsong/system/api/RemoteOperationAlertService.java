package com.junsong.system.api;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.factory.RemoteOperationAlertFallbackFactory;

/**
 * R25 操作告警远程调用接口
 *
 * <p>body 携带字段：ruleKey / dedupKey / sourceType / sourceId / severity / title / content</p>
 *
 * @author junsong
 * @since R25
 */
@FeignClient(contextId = "remoteOperationAlertService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteOperationAlertFallbackFactory.class)
public interface RemoteOperationAlertService {

    /**
     * 触发告警（按 dedupKey 去重，内部服务调用）
     *
     * @param body   告警内容
     * @param source 请求来源标识（由 SecurityConstants.INNER 传入）
     * @return 操作结果
     */
    @PostMapping("/operation-alert/internal/raise")
    R<Boolean> raiseAlert(
            @RequestBody Map<String, Object> body,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
