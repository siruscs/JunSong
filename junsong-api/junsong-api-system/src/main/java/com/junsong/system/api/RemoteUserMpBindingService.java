package com.junsong.system.api;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.SysUserMpBinding;
import com.junsong.system.api.factory.RemoteUserMpBindingFallbackFactory;

/**
 * 小程序微信账号绑定关系 服务（Feign 客户端）
 */
@FeignClient(contextId = "remoteUserMpBindingService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteUserMpBindingFallbackFactory.class)
public interface RemoteUserMpBindingService
{
    /**
     * 按 (appId, openid) 全局查询 ACTIVE 绑定（仅限微信快捷登录流程）
     */
    @GetMapping("/user/mp-binding/by-app-openid")
    R<SysUserMpBinding> selectByAppOpenid(
            @RequestParam("appId") String appId,
            @RequestParam("openid") String openid,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 按 (tenantId, userId) 查询绑定列表
     */
    @GetMapping("/user/mp-binding/by-user-id")
    R<List<SysUserMpBinding>> selectByUserId(
            @RequestParam("tenantId") Long tenantId,
            @RequestParam("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 新增绑定关系
     */
    @PostMapping("/user/mp-binding")
    R<Integer> insert(
            @RequestBody SysUserMpBinding binding,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 撤销绑定
     */
    @PutMapping("/user/mp-binding/revoke")
    R<Integer> revoke(
            @RequestBody SysUserMpBinding binding,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 更新最近登录时间
     */
    @PutMapping("/user/mp-binding/login-time")
    R<Integer> updateLastLoginTime(
            @RequestParam("tenantId") Long tenantId,
            @RequestParam("bindingId") Long bindingId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
