package com.junsong.system.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.system.service.ISysOperationAlertService;

/**
 * R25 内部操作告警服务接口 —— 仅供微服务间调用使用。
 * <p>网关层会剥离 FROM_SOURCE 请求头，因此浏览器请求无法到达此处。</p>
 *
 * @author junsong
 * @since R25
 */
@RestController
@RequestMapping("/operation-alert/internal")
public class InternalOperationAlertController {

    @Autowired
    private ISysOperationAlertService alertService;

    @InnerAuth
    @PostMapping("/raise")
    public R<Boolean> raise(@RequestBody Map<String, Object> body) {
        alertService.raiseAlert(
                (String) body.get("ruleKey"),
                (String) body.get("dedupKey"),
                (String) body.get("sourceType"),
                body.get("sourceId") == null ? null : String.valueOf(body.get("sourceId")),
                (String) body.get("severity"),
                (String) body.get("title"),
                body.get("content") == null ? null : String.valueOf(body.get("content"))
        );
        return R.ok(true);
    }
}
