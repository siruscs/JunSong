package com.junsong.system.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.system.domain.SysNotification;
import com.junsong.system.service.ISysNotificationService;

/**
 * 内部通知服务接口 —— 仅供微服务间调用使用。
 * <p>网关层会剥离 FROM_SOURCE 请求头，因此浏览器请求无法到达此处。</p>
 *
 * @author junsong
 * @since NIGHT-P1-R3B
 */
@RestController
@RequestMapping("/notification/internal")
public class InternalNotificationController {

    @Autowired
    private ISysNotificationService notificationService;

    @InnerAuth
    @PostMapping("/batch-send")
    public R<Boolean> batchSend(@RequestBody List<Map<String, Object>> notifications) {
        for (Map<String, Object> n : notifications) {
            SysNotification notification = new SysNotification();
            notification.setUserId(toLong(n.get("userId")));
            notification.setTitle((String) n.get("title"));
            notification.setContent((String) n.get("content"));
            notification.setType((String) n.get("type"));
            notification.setLinkUrl((String) n.get("linkUrl"));
            notification.setBizId((String) n.get("bizId"));
            notificationService.insertNotification(notification);
        }
        return R.ok(true);
    }

    @InnerAuth
    @GetMapping("/check-exists")
    public R<Boolean> checkNotificationExists(
            @RequestParam Long userId,
            @RequestParam String type,
            @RequestParam String bizId) {
        int count = notificationService.countByUserTypeBizId(userId, type, bizId);
        return R.ok(count > 0);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
