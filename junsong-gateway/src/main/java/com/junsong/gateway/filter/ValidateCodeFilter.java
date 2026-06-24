package com.junsong.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.redis.service.RedisService;
import com.junsong.gateway.config.properties.CaptchaProperties;
import com.junsong.gateway.service.ValidateCodeService;
import reactor.core.publisher.Mono;

@Component
public class ValidateCodeFilter extends AbstractGatewayFilterFactory<Object>
{
    private static final Logger log = LoggerFactory.getLogger(ValidateCodeFilter.class);

    private final static String[] VALIDATE_URL = new String[] { "/auth/login", "/auth/register" };

    @Autowired
    private CaptchaProperties captchaProperties;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ValidateCodeService validateCodeService;

    @Override
    public GatewayFilter apply(Object config)
    {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!StringUtils.equalsAnyIgnoreCase(request.getURI().getPath(), VALIDATE_URL))
            {
                return chain.filter(exchange);
            }

            Object captchaEnabledObj = redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.captchaEnabled");
            if ("false".equalsIgnoreCase(normalizeBooleanValue(captchaEnabledObj)))
            {
                return chain.filter(exchange);
            }

            boolean originalCaptchaEnabled = captchaProperties.getEnabled();
            if (!originalCaptchaEnabled)
            {
                return chain.filter(exchange);
            }

            HttpHeaders headers = request.getHeaders();
            MediaType contentType = headers.getContentType();
            if (contentType == null || !contentType.includes(MediaType.APPLICATION_JSON))
            {
                return setUnauthorizedResponse(exchange.getResponse(), "请求类型不支持");
            }

            return DataBufferUtils.join(request.getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        String body = new String(bytes, StandardCharsets.UTF_8);
                        try
                        {
                            JSONObject obj = JSON.parseObject(body);
                            String code = obj.getString("code");
                            String uuid = obj.getString("uuid");
                            validateCodeService.checkCaptcha(code, uuid);
                        }
                        catch (Exception e)
                        {
                            return setUnauthorizedResponse(exchange.getResponse(), e.getMessage());
                        }
                        ServerHttpRequest newRequest = request.mutate().build();
                        return chain.filter(exchange.mutate().request(newRequest).build());
                    });
        };
    }

    private Mono<Void> setUnauthorizedResponse(ServerHttpResponse response, String message)
    {
        log.warn("验证码校验失败: {}", message);
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        AjaxResult result = AjaxResult.error(message);
        byte[] bytes = JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private String normalizeBooleanValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String text = value.toString().trim();
        while (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\""))
        {
            text = text.substring(1, text.length() - 1).trim();
        }
        text = text.replace("\\n", "").replace("\\r", "");
        return text.trim();
    }
}
