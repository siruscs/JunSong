package com.junsong.open.controller.openapi;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.open.context.OpenApiRequestContextHolder;
import com.junsong.open.domain.OpenApp;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.service.IOpenAppSecretService;
import com.junsong.open.service.IOpenAppService;

/**
 * 平台基础能力(Foundation as a Service)开放API
 *
 * 对外暴露应用查询和Key概览能力。
 * 注意：此控制器提供公共API路径(/apps)，区别于管理后台路径(/app)。
 * 公共 openapi 端点由网关 HMAC 签名 + X-Open-* 上下文拦截器保护，不再依赖用户权限注解。
 * 所有查询强制按 X-Open-Tenant-Id 租户隔离，防止跨租户读取。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/open/apps")
public class OpenFoundationController extends BaseController
{
    @Autowired
    private IOpenAppService openAppService;

    @Autowired
    private IOpenAppSecretService openAppSecretService;

    /**
     * 查询授权应用基础信息（只返回当前租户可见应用）
     */
    @GetMapping
    public AjaxResult listOpenApps()
    {
        Long tenantId = OpenApiRequestContextHolder.get().getTenantId();
        OpenApp query = new OpenApp();
        query.setTenantId(tenantId);
        List<OpenApp> list = openAppService.selectOpenAppList(query);
        return success(list);
    }

    /**
     * 查询应用Key概览（敏感字段不通过公共接口返回）
     * 先校验 appId 归属当前租户，再返回 Key 列表。
     */
    @GetMapping("/{appId}/keys")
    public AjaxResult listAppKeys(@PathVariable("appId") Long appId)
    {
        Long tenantId = OpenApiRequestContextHolder.get().getTenantId();
        OpenApp app = openAppService.selectOpenAppById(appId);
        if (app == null || !tenantId.equals(app.getTenantId()))
        {
            throw new ServiceException("应用不存在或无权访问");
        }
        List<OpenAppSecret> keys = openAppSecretService.selectKeysByAppId(appId);
        keys.forEach(key -> key.setAppSecret(null));
        return success(keys);
    }
}
