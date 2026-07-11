package com.junsong.system.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.system.service.ISysOperationAuditService;

/**
 * R25 内部操作审计服务接口 —— 仅供微服务间调用使用。
 * <p>网关层会剥离 FROM_SOURCE 请求头，因此浏览器请求无法到达此处。</p>
 *
 * @author junsong
 * @since R25
 */
@RestController
@RequestMapping("/operation-audit/internal")
public class InternalOperationAuditController {

    @Autowired
    private ISysOperationAuditService auditService;

    @InnerAuth
    @PostMapping("/record")
    public R<Boolean> record(@RequestBody Map<String, Object> body) {
        String bizType = (String) body.get("bizType");
        String bizId = String.valueOf(body.get("bizId"));
        String operation = (String) body.get("operation");
        String riskLevel = (String) body.get("riskLevel");
        Object before = body.get("beforeJson");
        Object after = body.get("afterJson");
        auditService.recordSnapshot(bizType, bizId, operation, riskLevel, before, after);
        return R.ok(true);
    }
}
