package com.junsong.common.core.idempotency;

import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 默认幂等键解析器：从请求头 X-Idempotency-Key 读取。
 *
 * @author junsong
 */
@Component
public class HeaderIdempotencyKeyResolver implements IdempotencyKeyResolver {

    public static final String HEADER_NAME = "X-Idempotency-Key";

    @Override
    public String resolve(JoinPoint point, Idempotent idempotent) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String key = request.getHeader(HEADER_NAME);
        if (key != null && key.isEmpty()) {
            return null;
        }
        return key;
    }
}
