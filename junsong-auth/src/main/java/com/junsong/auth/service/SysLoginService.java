package com.junsong.auth.service;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.junsong.auth.config.WechatMiniProgramProperties;
import com.junsong.auth.service.WechatMiniProgramService.WechatIdentity;
import com.junsong.auth.form.RegisterBody;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.constant.Constants;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.UserConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.enums.UserStatus;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.text.Convert;
import com.junsong.common.core.utils.DateUtils;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.utils.ip.IpUtils;
import com.junsong.common.redis.service.RedisService;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.api.RemoteUserMpBindingService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.domain.SysUserMpBinding;
import com.junsong.system.api.model.LoginUser;

/**
 * 登录校验方法
 *
 * @author junsong
 */
@Component
public class SysLoginService
{
    private static final Logger log = LoggerFactory.getLogger(SysLoginService.class);

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RemoteUserMpBindingService remoteUserMpBindingService;

    @Autowired
    private WechatMiniProgramService wechatMiniProgramService;

    @Autowired
    private WechatMiniProgramProperties wechatMpProperties;

    @Autowired
    private SysPasswordService passwordService;

    @Autowired
    private SysRecordLogService recordLogService;

    @Autowired
    private RedisService redisService;

    /**
     * 登录
     */
    public LoginUser login(String username, String password, Long deptId)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户/密码必须填写");
            throw new ServiceException("用户/密码必须填写");
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户密码不在指定范围");
            throw new ServiceException("用户密码不在指定范围");
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户名不在指定范围");
            throw new ServiceException("用户名不在指定范围");
        }
        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "很遗憾，访问IP已被列入系统黑名单");
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }
        // 查询用户信息
        R<LoginUser> userResult = remoteUserService.getUserInfo(username, SecurityConstants.INNER);

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        LoginUser userInfo = userResult.getData();
        SysUser user = userResult.getData().getSysUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "对不起，您的账号已被删除");
            throw new ServiceException("对不起，您的账号：" + username + " 已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户已停用，请联系管理员");
            throw new ServiceException("对不起，您的账号：" + username + " 已停用");
        }
        passwordService.validate(user, password);
        
        // 设置当前选择的部门ID
        if (deptId != null)
        {
            userInfo.setDeptId(deptId);
            // 同时设置sysUser的deptId，确保数据权限正常工作
            user.setDeptId(deptId);
        }
        else if (user.getDeptId() != null)
        {
            // 前端未传deptId时，使用用户默认部门（来自sys_user表）
            userInfo.setDeptId(user.getDeptId());
        }
        
        recordLogService.recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功");
        recordLoginInfo(user.getUserId());
        return userInfo;
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        // 更新用户登录IP
        sysUser.setLoginIp(IpUtils.getIpAddr());
        // 更新用户登录时间
        sysUser.setLoginDate(DateUtils.getNowDate());
        remoteUserService.recordUserLogin(sysUser, SecurityConstants.INNER);
    }

    /**
     * 退出
     */
    public void logout(String loginName)
    {
        recordLogService.recordLogininfor(loginName, Constants.LOGOUT, "退出成功");
    }

    /**
     * 解锁
     */
    public void unlock(String password)
    {
        String username = SecurityUtils.getUsername();
        // 或密码为空 错误
        if (StringUtils.isEmpty(password))
        {
            throw new ServiceException("密码不能为空");
        }
        // 查询用户信息
        R<LoginUser> userResult = remoteUserService.getUserInfo(username, SecurityConstants.INNER);

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        SysUser user = userResult.getData().getSysUser();
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            throw new ServiceException("密码错误，请重新输入");
        }
    }

    /**
     * 注册
     */
    public void register(RegisterBody registerBody)
    {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            throw new ServiceException("用户/密码必须填写");
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new ServiceException("账户长度必须在2到20个字符之间");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }

        // 注册用户信息
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(StringUtils.isNotEmpty(registerBody.getNickName()) ? registerBody.getNickName() : username);
        sysUser.setPhonenumber(registerBody.getPhonenumber());
        sysUser.setIdCard(registerBody.getIdCard());
        sysUser.setInviteCode(registerBody.getInviteCode());
        sysUser.setPwdUpdateDate(DateUtils.getNowDate());
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        R<?> registerResult = remoteUserService.registerUserInfo(sysUser, SecurityConstants.INNER);

        if (R.FAIL == registerResult.getCode())
        {
            throw new ServiceException(registerResult.getMsg());
        }
        recordLogService.recordLogininfor(username, Constants.REGISTER, "注册成功");
    }

    // =========================================================================
    // 微信小程序快捷登录 / 绑定 / 解绑
    // =========================================================================

    /**
     * 微信快捷登录
     *
     * <p>流程：code 换 openid → 查询 ACTIVE 绑定 → 按 userId 获取 LoginUser →
     * 校验账号状态 → 更新最近登录时间。</p>
     *
     * @param code   微信 wx.login() 返回的临时 code
     * @param deptId 部门/门店ID（可选，不传时使用用户默认部门）
     * @return 登录用户信息
     * @throws ServiceException 当未绑定、code 失效、账号停用/删除、服务调用失败时抛出
     */
    public LoginUser wechatLogin(String code, Long deptId)
    {
        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }

        // 1. code 换 openid / unionid（失败时直接抛出，错误信息已脱敏）
        WechatIdentity identity = wechatMiniProgramService.exchangeCodeForIdentity(code);
        String appId = wechatMpProperties.getAppId();
        String openid = identity.openid();

        // 2. 按 (appId, openid) 全局查询 ACTIVE 绑定
        R<SysUserMpBinding> bindingResult = remoteUserMpBindingService
                .selectByAppOpenid(appId, openid, SecurityConstants.INNER);
        if (bindingResult == null || R.FAIL == bindingResult.getCode())
        {
            // Feign 调用失败，fail-closed，错误信息不泄露 openid
            log.warn("wechat mp login: binding query failed, appOpenid masked={}", maskOpenid(openid));
            throw new ServiceException("登录校验失败，请稍后重试");
        }
        SysUserMpBinding binding = bindingResult.getData();
        if (binding == null)
        {
            // 无 ACTIVE 绑定
            log.warn("wechat mp login: no active binding, appOpenid masked={}", maskOpenid(openid));
            throw new ServiceException("未绑定账号，请先绑定后再使用微信快捷登录");
        }

        // 2.5. 后端再校验：该租户是否启用微信登录
        assertWechatLoginEnabled(binding.getTenantId());

        // 3. 按绑定中的 userId 获取 LoginUser
        R<LoginUser> userResult = remoteUserService.getUserInfoById(binding.getUserId(), SecurityConstants.INNER);
        if (userResult == null || R.FAIL == userResult.getCode() || userResult.getData() == null)
        {
            log.warn("wechat mp login: user query failed, userId={}", binding.getUserId());
            throw new ServiceException("账号信息查询失败，请稍后重试");
        }
        LoginUser userInfo = userResult.getData();
        SysUser user = userInfo.getSysUser();

        // 4. 校验账号状态
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(user.getUserName(), Constants.LOGIN_FAIL, "账号已删除");
            throw new ServiceException("对不起，您的账号已删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(user.getUserName(), Constants.LOGIN_FAIL, "账号已停用");
            throw new ServiceException("对不起，您的账号已停用");
        }

        // 5. 设置部门
        if (deptId != null)
        {
            userInfo.setDeptId(deptId);
            user.setDeptId(deptId);
        }
        else if (user.getDeptId() != null)
        {
            userInfo.setDeptId(user.getDeptId());
        }

        // 6. 更新最近登录时间
        remoteUserMpBindingService.updateLastLoginTime(
                binding.getTenantId(), binding.getBindingId(), SecurityConstants.INNER);

        recordLogService.recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, "微信快捷登录成功");
        recordLoginInfo(user.getUserId());
        return userInfo;
    }

    /**
     * 微信绑定现有账号
     *
     * <p>流程：验证账号凭据 → code 换 openid → 检查是否已绑定 → 插入绑定关系 → 返回 LoginUser。</p>
     *
     * <p>安全约束：</p>
     * <ul>
     *   <li>必须验证已有账号的密码，不自动创建账号。</li>
     *   <li>同一 (appId, openid) 已绑定时拒绝重复绑定。</li>
     *   <li>错误信息不泄露 openid / unionid / AppSecret。</li>
     * </ul>
     *
     * @param code     微信 wx.login() 返回的临时 code
     * @param username 已有系统账号用户名
     * @param password 已有系统账号密码
     * @param deptId   部门/门店ID（可选）
     * @return 登录用户信息
     * @throws ServiceException 当已绑定、密码错误、code 失效、账号停用/删除时抛出
     */
    public LoginUser wechatBind(String code, String username, String password, Long deptId)
    {
        // 基础校验
        if (StringUtils.isAnyBlank(code, username, password))
        {
            throw new ServiceException("微信凭证/用户名/密码必须填写");
        }

        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "访问IP已被列入系统黑名单");
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }

        // 1. code 换 openid / unionid
        WechatIdentity identity = wechatMiniProgramService.exchangeCodeForIdentity(code);
        String appId = wechatMpProperties.getAppId();
        String openid = identity.openid();

        // 2. 检查是否已存在绑定（同一 appId+openid 全局唯一）
        R<SysUserMpBinding> existingResult = remoteUserMpBindingService
                .selectByAppOpenid(appId, openid, SecurityConstants.INNER);
        if (existingResult != null && existingResult.getData() != null)
        {
            // 已存在绑定，拒绝重复绑定，错误信息不泄露 openid
            log.warn("wechat mp bind: already bound, appOpenid masked={}", maskOpenid(openid));
            throw new ServiceException("该微信已绑定其他账号，请先解绑或更换微信");
        }

        // 3. 通过用户名查询账号信息
        R<LoginUser> userResult = remoteUserService.getUserInfo(username, SecurityConstants.INNER);
        if (userResult == null || R.FAIL == userResult.getCode() || userResult.getData() == null)
        {
            throw new ServiceException(userResult != null ? userResult.getMsg() : "账号信息查询失败");
        }
        LoginUser userInfo = userResult.getData();
        SysUser user = userInfo.getSysUser();

        // 3.5. 后端再校验：该租户是否启用微信登录
        assertWechatLoginEnabled(user.getTenantId());

        // 4. 校验账号状态
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "账号已删除");
            throw new ServiceException("对不起，您的账号已删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "账号已停用");
            throw new ServiceException("对不起，您的账号已停用");
        }

        // 5. 验证密码（绑定必须验证已有账号凭据）
        passwordService.validate(user, password);

        // 6. 设置部门
        if (deptId != null)
        {
            userInfo.setDeptId(deptId);
            user.setDeptId(deptId);
        }
        else if (user.getDeptId() != null)
        {
            userInfo.setDeptId(user.getDeptId());
        }

        // 7. 插入绑定关系
        SysUserMpBinding binding = new SysUserMpBinding();
        binding.setTenantId(user.getTenantId());
        binding.setUserId(user.getUserId());
        binding.setAppId(appId);
        binding.setOpenid(openid);
        binding.setUnionid(identity.unionid());
        binding.setBoundBy(username);
        R<Integer> insertResult = remoteUserMpBindingService.insert(binding, SecurityConstants.INNER);
        if (insertResult == null || R.FAIL == insertResult.getCode())
        {
            // 唯一键冲突或写入失败，fail-closed，错误信息不泄露 openid
            log.warn("wechat mp bind: insert failed, appOpenid masked={}", maskOpenid(openid));
            throw new ServiceException("绑定失败，请稍后重试");
        }

        recordLogService.recordLogininfor(username, Constants.LOGIN_SUCCESS, "微信绑定并登录成功");
        recordLoginInfo(user.getUserId());
        return userInfo;
    }

    /**
     * 微信解绑
     *
     * <p>撤销当前用户的 ACTIVE 绑定关系，状态置为 REVOKED 保留审计链。</p>
     *
     * @param tenantId     租户ID
     * @param userId       用户ID
     * @param username     操作人用户名（用于审计）
     * @param revokeReason 解绑原因（可选）
     * @throws ServiceException 当无有效绑定、解绑失败时抛出
     */
    public void wechatUnbind(Long tenantId, Long userId, String username, String revokeReason)
    {
        // 1. 查询当前用户的所有绑定关系
        R<List<SysUserMpBinding>> bindingsResult = remoteUserMpBindingService
                .selectByUserId(tenantId, userId, SecurityConstants.INNER);
        if (bindingsResult == null || R.FAIL == bindingsResult.getCode() || bindingsResult.getData() == null)
        {
            throw new ServiceException("绑定关系查询失败，请稍后重试");
        }
        List<SysUserMpBinding> bindings = bindingsResult.getData();

        // 2. 查找 ACTIVE 绑定（同一用户最多一个 ACTIVE 绑定）
        SysUserMpBinding activeBinding = null;
        for (SysUserMpBinding b : bindings)
        {
            if ("ACTIVE".equals(b.getStatus()))
            {
                activeBinding = b;
                break;
            }
        }
        if (activeBinding == null)
        {
            // 无有效绑定，错误信息不泄露 openid
            log.warn("wechat mp unbind: no active binding, tenantId={}, userId={}", tenantId, userId);
            throw new ServiceException("未找到有效的微信绑定关系");
        }

        // 3. 撤销绑定（条件更新：status='ACTIVE' 才能被撤销）
        activeBinding.setRevokedBy(username);
        activeBinding.setRevokeReason(revokeReason);
        R<Integer> revokeResult = remoteUserMpBindingService.revoke(activeBinding, SecurityConstants.INNER);
        if (revokeResult == null || R.FAIL == revokeResult.getCode()
                || revokeResult.getData() == null || revokeResult.getData() == 0)
        {
            log.warn("wechat mp unbind: revoke failed, bindingId={}", activeBinding.getBindingId());
            throw new ServiceException("解绑失败，请稍后重试");
        }

        recordLogService.recordLogininfor(username, Constants.LOGOUT, "微信解绑成功");
    }

    /**
     * 查询当前用户的微信绑定列表
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @return 绑定关系列表（可能包含 ACTIVE 和 REVOKED 历史记录）
     */
    public List<SysUserMpBinding> getWechatBindings(Long tenantId, Long userId)
    {
        R<List<SysUserMpBinding>> result = remoteUserMpBindingService
                .selectByUserId(tenantId, userId, SecurityConstants.INNER);
        if (result == null || R.FAIL == result.getCode() || result.getData() == null)
        {
            return Collections.emptyList();
        }
        return result.getData();
    }

    /**
     * 后端再校验：指定租户是否启用微信登录。
     * fail-closed：配置为 false、缺失、非法值或读取异常时拒绝。
     *
     * @param tenantId 租户ID
     * @throws ServiceException 当微信登录未开启或读取失败时
     */
    private void assertWechatLoginEnabled(Long tenantId)
    {
        R<Boolean> result = remoteUserService.isWechatLoginEnabled(tenantId, SecurityConstants.INNER);
        if (result == null || R.FAIL == result.getCode() || !Boolean.TRUE.equals(result.getData()))
        {
            throw new ServiceException("微信登录未开启，请使用账号密码登录");
        }
    }

    /**
     * openid 脱敏（仅保留前 4 位 + ***），用于日志输出。
     */
    private String maskOpenid(String openid)
    {
        if (openid == null || openid.length() <= 4)
        {
            return "***";
        }
        return openid.substring(0, 4) + "***";
    }
}
