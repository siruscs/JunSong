package com.junsong.system.api;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.LcMenuGenerateRequest;
import com.junsong.system.api.factory.RemoteMenuFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 菜单服务（低代码菜单自动生成）
 */
@FeignClient(contextId = "remoteMenuService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteMenuFallbackFactory.class)
public interface RemoteMenuService
{
    /**
     * 为低代码业务生成菜单+按钮权限（幂等），返回生成的菜单 id
     *
     * @param request 生成入参
     * @param source 请求来源
     * @return 菜单 id
     */
    @PostMapping("/menu/inner/lowcode-generate")
    R<Long> generateLowcodeMenu(@RequestBody LcMenuGenerateRequest request,
                                @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
