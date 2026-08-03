package com.junsong.common.security.feign;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.idempotency.HeaderIdempotencyKeyResolver;
import com.junsong.common.core.utils.ServletUtils;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.utils.ip.IpUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * feign 请求拦截器
 * 
 * @author junsong
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor
{
    @Override
    public void apply(RequestTemplate requestTemplate)
    {
        HttpServletRequest httpServletRequest = ServletUtils.getRequest();
        if (StringUtils.isNotNull(httpServletRequest))
        {
            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
            // 传递用户信息请求头，防止丢失
            String userId = headers.get(SecurityConstants.DETAILS_USER_ID);
            if (StringUtils.isNotEmpty(userId))
            {
                requestTemplate.header(SecurityConstants.DETAILS_USER_ID, userId);
            }
            String userKey = headers.get(SecurityConstants.USER_KEY);
            if (StringUtils.isNotEmpty(userKey))
            {
                requestTemplate.header(SecurityConstants.USER_KEY, userKey);
            }
            String userName = headers.get(SecurityConstants.DETAILS_USERNAME);
            if (StringUtils.isNotEmpty(userName))
            {
                requestTemplate.header(SecurityConstants.DETAILS_USERNAME, userName);
            }
            String authentication = headers.get(SecurityConstants.AUTHORIZATION_HEADER);
            if (StringUtils.isNotEmpty(authentication))
            {
                requestTemplate.header(SecurityConstants.AUTHORIZATION_HEADER, authentication);
            }

            // 传递幂等键，确保内部服务调用链路中 @Idempotent 注解能正确解析
            String idempotencyKey = headers.get(HeaderIdempotencyKeyResolver.HEADER_NAME);
            if (StringUtils.isNotEmpty(idempotencyKey))
            {
                requestTemplate.header(HeaderIdempotencyKeyResolver.HEADER_NAME, idempotencyKey);
            }

            // 配置客户端IP
            requestTemplate.header("X-Forwarded-For", IpUtils.getIpAddr());
        }
    }
}