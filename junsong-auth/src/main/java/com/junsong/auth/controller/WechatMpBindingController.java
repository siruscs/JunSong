package com.junsong.auth.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.auth.form.WechatMpBindBody;
import com.junsong.auth.form.WechatMpLoginBody;
import com.junsong.auth.form.WechatMpUnbindBody;
import com.junsong.auth.service.SysLoginService;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.security.service.TokenService;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.api.domain.SysUserMpBinding;
import com.junsong.system.api.model.LoginUser;

/**
 * 微信小程序绑定/登录/解绑 控制器
 *
 * <p>端点：</p>
 * <ul>
 *   <li>{@code POST /auth/mp/wechat/login} — 微信快捷登录（公开）</li>
 *   <li>{@code POST /auth/mp/wechat/bind} — 绑定现有账号（公开，需验证账号密码）</li>
 *   <li>{@code POST /auth/mp/wechat/unbind} — 解绑（需登录）</li>
 *   <li>{@code GET /auth/mp/wechat/binding} — 查询绑定状态（需登录）</li>
 * </ul>
 *
 * @author junsong
 */
@RestController
@RequestMapping("/mp/wechat")
public class WechatMpBindingController extends BaseController
{
    @Autowired
    private SysLoginService sysLoginService;

    @Autowired
    private TokenService tokenService;

    /**
     * 微信快捷登录
     *
     * <p>已绑定微信的用户使用 wx.login() 获取的 code 直接登录，无需输入用户名密码。
     * 登录成功后调用 {@link TokenService#createTokenMp} 生成小程序专用 Token，
     * 标记 authSource=WECHAT_MP，受"微信会话一键失效"管理。</p>
     */
    @PostMapping("/login")
    public R<?> login(@RequestBody WechatMpLoginBody form)
    {
        LoginUser userInfo = sysLoginService.wechatLogin(form.getCode(), form.getDeptId());
        return R.ok(tokenService.createTokenMp(userInfo, "WECHAT_MP"));
    }

    /**
     * 微信绑定现有账号
     *
     * <p>首次绑定时需验证已有系统账号的用户名和密码，绑定成功后自动登录。
     * 同一 (appId, openid) 全局只能绑定一个系统账号。
     * 绑定后登录标记 authSource=WECHAT_MP，受"微信会话一键失效"管理。</p>
     */
    @PostMapping("/bind")
    public R<?> bind(@RequestBody WechatMpBindBody form)
    {
        LoginUser userInfo = sysLoginService.wechatBind(
                form.getCode(), form.getUsername(), form.getPassword(), form.getDeptId());
        return R.ok(tokenService.createTokenMp(userInfo, "WECHAT_MP"));
    }

    /**
     * 微信解绑
     *
     * <p>撤销当前登录用户的 ACTIVE 绑定关系，状态置为 REVOKED 保留审计链。
     * 解绑后该微信身份将无法用于快捷登录。</p>
     */
    @PostMapping("/unbind")
    public R<?> unbind(@RequestBody WechatMpUnbindBody form)
    {
        Long tenantId = getCurrentTenantId();
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        sysLoginService.wechatUnbind(tenantId, userId, username, form.getRevokeReason());
        return R.ok();
    }

    /**
     * 查询当前用户的微信绑定状态
     *
     * <p>返回绑定列表（含 ACTIVE 和 REVOKED 历史记录），前端根据 status 判断是否已绑定。</p>
     */
    @GetMapping("/binding")
    public R<List<SysUserMpBinding>> binding()
    {
        Long tenantId = getCurrentTenantId();
        Long userId = SecurityUtils.getUserId();
        List<SysUserMpBinding> bindings = sysLoginService.getWechatBindings(tenantId, userId);
        return R.ok(bindings);
    }

    /**
     * 从当前登录用户上下文获取租户ID。
     *
     * @throws ServiceException 当用户未登录或上下文缺失时 fail-closed
     */
    private Long getCurrentTenantId()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || loginUser.getSysUser() == null
                || loginUser.getSysUser().getTenantId() == null)
        {
            throw new ServiceException("登录信息已失效，请重新登录");
        }
        return loginUser.getSysUser().getTenantId();
    }
}
