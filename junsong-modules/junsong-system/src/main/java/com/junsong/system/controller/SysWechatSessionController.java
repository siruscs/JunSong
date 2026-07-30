package com.junsong.system.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.service.SysWechatSessionService;

/**
 * 微信会话管理（PC 管理端）
 *
 * <p>提供租户级一键使微信登录会话失效能力。
 * 不解除绑定、不删除绑定历史，仅让当前租户下所有微信登录 Token 在下次请求时失效。
 * 密码登录会话不受影响。</p>
 */
@RestController
@RequestMapping("/wechat-session")
public class SysWechatSessionController extends BaseController
{
    @Autowired
    private SysWechatSessionService wechatSessionService;

    /**
     * 查询当前租户的微信会话版本号
     */
    @RequiresPermissions("system:user:wechatSession:revokeAll")
    @GetMapping("/epoch")
    public AjaxResult getEpoch()
    {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null)
        {
            return AjaxResult.error("租户上下文缺失");
        }
        Long epoch = wechatSessionService.getEpoch(tenantId);
        AjaxResult result = AjaxResult.success();
        result.put("tenantId", tenantId);
        result.put("currentEpoch", epoch);
        return result;
    }

    /**
     * 一键使当前租户的所有微信登录会话失效
     *
     * <p>独立权限 system:user:wechatSession:revokeAll，不复用解绑权限。
     * 操作写入审计日志。不返回用户 openid 或 Token 明细。</p>
     *
     * @param reason 操作原因（可选，默认"管理员一键失效微信会话"）
     */
    @RequiresPermissions("system:user:wechatSession:revokeAll")
    @Log(title = "微信会话一键失效", businessType = BusinessType.OTHER)
    @Idempotent(scene = "system:wechat-session:revoke-all")
    @PostMapping("/revoke-all")
    public AjaxResult revokeAll(@RequestParam(value = "reason", required = false) String reason)
    {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null)
        {
            return AjaxResult.error("租户上下文缺失");
        }
        String operator = SecurityUtils.getUsername();
        String revokeReason = StringUtils.isNotEmpty(reason) ? reason : "管理员一键失效微信会话";
        Map<String, Object> result = wechatSessionService.revokeAllWechatSessions(tenantId, operator, revokeReason);
        return AjaxResult.success("微信会话已失效，受影响用户下次微信请求需重新登录", result);
    }
}
