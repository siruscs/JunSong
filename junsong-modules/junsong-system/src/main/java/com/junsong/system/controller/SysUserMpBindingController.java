package com.junsong.system.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.system.api.domain.SysUserMpBinding;
import com.junsong.system.service.ISysUserMpBindingService;

/**
 * 小程序微信账号绑定关系（内部接口，供 auth 服务通过 Feign 调用）
 *
 * <p>所有接口使用 @InnerAuth 保护，仅允许内部服务调用。</p>
 */
@RestController
@RequestMapping("/user/mp-binding")
public class SysUserMpBindingController
{
    private static final Logger log = LoggerFactory.getLogger(SysUserMpBindingController.class);

    @Autowired
    private ISysUserMpBindingService userMpBindingService;

    /**
     * 按 (appId, openid) 全局查询 ACTIVE 绑定（仅限微信快捷登录流程）
     */
    @InnerAuth
    @GetMapping("/by-app-openid")
    public R<SysUserMpBinding> selectByAppOpenid(
            @RequestParam("appId") String appId,
            @RequestParam("openid") String openid)
    {
        SysUserMpBinding binding = userMpBindingService.selectActiveByAppOpenidForLogin(appId, openid);
        return R.ok(binding);
    }

    /**
     * 按 (tenantId, userId) 查询绑定列表
     */
    @InnerAuth
    @GetMapping("/by-user-id")
    public R<List<SysUserMpBinding>> selectByUserId(
            @RequestParam("tenantId") Long tenantId,
            @RequestParam("userId") Long userId)
    {
        List<SysUserMpBinding> list = userMpBindingService.selectByUserId(tenantId, userId);
        return R.ok(list);
    }

    /**
     * 新增绑定关系（含 upsert 逻辑）
     *
     * <p>解绑后 REVOKED 记录仍占用 (app_id, openid) 唯一键，直接 INSERT 会触发
     * DuplicateKeyException。此方法捕获该异常后判断冲突记录状态：
     * <ul>
     *   <li>ACTIVE → 真正的重复绑定，返回失败</li>
     *   <li>REVOKED → 重新激活该记录（更新为新的绑定用户信息）</li>
     * </ul>
     * 并发竞态（REVOKED 记录已被其他请求激活）返回 0 行时视为已绑定。</p>
     */
    @InnerAuth
    @Idempotent(scene = "system:user-mp-binding:insert")
    @PostMapping
    public R<Integer> insert(@RequestBody SysUserMpBinding binding)
    {
        try
        {
            return R.ok(userMpBindingService.insertBinding(binding));
        }
        catch (DuplicateKeyException e)
        {
            log.warn("mp-binding insert: unique key conflict, appId={}, trying upsert", binding.getAppId());
            // 唯一键冲突：检查是 ACTIVE（真重复）还是 REVOKED（可重新激活）
            SysUserMpBinding active = userMpBindingService.selectActiveByAppOpenidForLogin(
                    binding.getAppId(), binding.getOpenid());
            if (active == null)
            {
                // 冲突记录是 REVOKED 状态，尝试重新激活
                int rows = userMpBindingService.reactivateBinding(binding);
                if (rows > 0)
                {
                    return R.ok(rows);
                }
                // 并发竞态：REVOKED 记录已被其他请求激活，视为已绑定
                return R.fail("该微信已绑定其他账号，请先解绑或更换微信");
            }
            // ACTIVE 记录已存在，真正的重复绑定
            return R.fail("该微信已绑定其他账号，请先解绑或更换微信");
        }
    }

    /**
     * 撤销绑定
     */
    @InnerAuth
    @PutMapping("/revoke")
    public R<Integer> revoke(@RequestBody SysUserMpBinding binding)
    {
        return R.ok(userMpBindingService.revokeBinding(binding));
    }

    /**
     * 更新最近登录时间
     */
    @InnerAuth
    @PutMapping("/login-time")
    public R<Integer> updateLastLoginTime(
            @RequestParam("tenantId") Long tenantId,
            @RequestParam("bindingId") Long bindingId)
    {
        return R.ok(userMpBindingService.updateLastLoginTime(tenantId, bindingId));
    }
}
