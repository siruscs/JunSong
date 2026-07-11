package com.junsong.open.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.open.domain.OpenApiLog;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.service.IOpenApiLogService;
import com.junsong.open.service.IOpenAppSecretService;

/**
 * 开放平台内部接口(供网关调用)
 *
 * 仅允许服务间内部调用（from-source: inner + X-Inner-Token），不通过公网暴露。
 * 网关直接调用 junsong-modules-open:9208，不经过网关路由面。
 *
 * 安全分层：
 *   1. @InnerAuth 校验 from-source: inner 头（防止网关路由面误触达）
 *   2. X-Inner-Token 校验 OPEN_INTERNAL_SECRET（防止 9208 端口误暴露后被伪造 from-source 攻击）
 *   3. 9208 端口仅 Docker 内网可访问（docker-compose expose，不发布到宿主机）
 *
 * @author junsong
 */
@RestController
@RequestMapping("/internal")
public class OpenInternalController extends BaseController
{
    /** 内部服务间密钥，由 OPEN_INTERNAL_SECRET 环境变量注入；为空时 fail closed */
    @Value("${open.internal.secret:}")
    private String innerToken;

    @Autowired
    private IOpenAppSecretService openAppSecretService;

    @Autowired
    private IOpenApiLogService openApiLogService;

    /**
     * 根据AppKey查询Secret信息(供网关验签使用)
     */
    @InnerAuth
    @GetMapping("/secret/byKey/{appKey}")
    public AjaxResult getByAppKey(@PathVariable("appKey") String appKey,
            @RequestHeader(value = "X-Inner-Token", required = false) String token)
    {
        AjaxResult tokenCheck = verifyInnerToken(token);
        if (tokenCheck != null)
        {
            return tokenCheck;
        }

        OpenAppSecret secret = openAppSecretService.selectByAppKey(appKey);
        if (secret == null)
        {
            return AjaxResult.error("AppKey不存在");
        }
        if (!"0".equals(secret.getStatus()))
        {
            return AjaxResult.error("AppKey已被禁用");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("appSecret", secret.getAppSecret());
        result.put("appId", secret.getAppId());
        result.put("tenantId", secret.getTenantId());
        result.put("dailyQuota", secret.getDailyQuota());
        result.put("status", secret.getStatus());
        result.put("keyType", secret.getKeyType());
        return AjaxResult.success(result);
    }

    /**
     * 记录网关层拒绝的调用日志（401/429，请求未到达 open 服务）
     */
    @InnerAuth
    @PostMapping("/log/access")
    public AjaxResult logAccess(@RequestBody OpenApiLog apiLog,
            @RequestHeader(value = "X-Inner-Token", required = false) String token)
    {
        AjaxResult tokenCheck = verifyInnerToken(token);
        if (tokenCheck != null)
        {
            return tokenCheck;
        }

        openApiLogService.insertOpenApiLog(apiLog);
        return AjaxResult.success();
    }

    /**
     * 校验 X-Inner-Token 与配置的 OPEN_INTERNAL_SECRET 是否一致。
     * - 配置密钥为空 → fail closed（防止误部署未配置密钥）
     * - 请求头缺失或不匹配 → 返回 401
     *
     * @return 非 null 表示校验失败，直接返回该 AjaxResult；null 表示校验通过
     */
    private AjaxResult verifyInnerToken(String token)
    {
        if (StringUtils.isEmpty(innerToken))
        {
            logger.error("[OpenInternal] 内部密钥未配置 (open.internal.secret 为空)，fail closed");
            return AjaxResult.error(500, "内部服务密钥未配置，拒绝访问");
        }
        if (StringUtils.isEmpty(token) || !StringUtils.equals(innerToken, token))
        {
            logger.warn("[OpenInternal] X-Inner-Token 校验失败");
            return AjaxResult.error(401, "内部服务鉴权失败");
        }
        return null;
    }
}
