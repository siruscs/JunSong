package com.junsong.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.auth.form.LoginBody;
import com.junsong.auth.form.RegisterBody;
import com.junsong.auth.form.UnLockBody;
import com.junsong.auth.service.SysLoginService;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.security.auth.AuthUtil;
import com.junsong.common.security.service.TokenService;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.common.core.utils.JwtUtils;
import com.junsong.system.api.model.LoginUser;

/**
 * token 控制
 * 
 * @author junsong
 */
@RestController
public class TokenController extends BaseController
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService sysLoginService;

    @PostMapping("login")
    public R<?> login(@RequestBody LoginBody form)
    {
        // 用户登录
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword(), form.getDeptId());
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    @DeleteMapping("logout")
    public R<?> logout(HttpServletRequest request)
    {
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            String username = JwtUtils.getUserName(token);
            // 删除用户缓存记录
            AuthUtil.logoutByToken(token);
            // 记录用户退出日志
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    /**
     * 小程序登录（不触发PC端单点登录互踢）
     */
    @PostMapping("mp/login")
    public R<?> mpLogin(@RequestBody LoginBody form)
    {
        // 小程序不走验证码
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword(), form.getDeptId());
        return R.ok(tokenService.createTokenMp(userInfo));
    }

    @PostMapping("refresh")
    public R<?> refresh(HttpServletRequest request)
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            // 刷新令牌有效期
            tokenService.refreshToken(loginUser);
            return R.ok();
        }
        return R.ok();
    }

    @PostMapping("register")
    public R<?> register(@RequestBody RegisterBody registerBody)
    {
        // 用户注册
        sysLoginService.register(registerBody);
        return R.ok();
    }

    /**
     * 解锁屏幕
     */
    @PostMapping("/unlockscreen")
    public R<?> unlockScreen(@RequestBody UnLockBody unLockBody)
    {
        sysLoginService.unlock(unLockBody.getPassword());
        return R.ok();
    }
}
