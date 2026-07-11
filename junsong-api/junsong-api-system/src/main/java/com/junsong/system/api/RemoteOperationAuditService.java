package com.junsong.system.api;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.factory.RemoteOperationAuditFallbackFactory;

/**
 * R25 操作审计远程调用接口
 *
 * <p>body 携带字段：bizType / bizId / operation / riskLevel / beforeJson / afterJson</p>
 *
 * @author junsong
 * @since R25
 */
@FeignClient(contextId = "remoteOperationAuditService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteOperationAuditFallbackFactory.class)
public interface RemoteOperationAuditService {

    /**
     * 记录操作快照（内部服务调用）
     *
     * @param body   快照内容
     * @param source 请求来源标识（由 SecurityConstants.INNER 传入）
     * @return 操作结果
     */
    @PostMapping("/operation-audit/internal/record")
    R<Boolean> recordSnapshot(
            @RequestBody Map<String, Object> body,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
