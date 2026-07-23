package com.junsong.auth.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.junsong.auth.config.WechatMiniProgramProperties;
import com.junsong.auth.service.SysLoginService;
import com.junsong.auth.service.SysPasswordService;
import com.junsong.auth.service.SysRecordLogService;
import com.junsong.auth.service.WechatMiniProgramService;
import com.junsong.auth.service.WechatMiniProgramService.WechatIdentity;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.redis.service.RedisService;
import com.junsong.system.api.RemoteUserMpBindingService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.domain.SysUserMpBinding;
import com.junsong.system.api.model.LoginUser;

/**
 * 微信绑定 API 测试
 *
 * <p>覆盖：未绑定首次绑定、已绑定快捷登录、重复绑定、跨租户、停用/删除账号、
 * 解绑后登录失败、密码错误、code失效、错误信息脱敏等场景。</p>
 */
@ExtendWith(MockitoExtension.class)
class WechatMpBindingControllerTest
{
    @Mock
    private RemoteUserService remoteUserService;

    @Mock
    private RemoteUserMpBindingService remoteUserMpBindingService;

    @Mock
    private WechatMiniProgramService wechatMiniProgramService;

    @Mock
    private WechatMiniProgramProperties wechatMpProperties;

    @Mock
    private SysPasswordService passwordService;

    @Mock
    private SysRecordLogService recordLogService;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private SysLoginService sysLoginService;

    private static final String TEST_APP_ID = "wxe9f6685ec5de191f";
    private static final String TEST_OPENID = "test_openid_12345";
    private static final String TEST_UNIONID = "test_unionid_67890";
    private static final String TEST_CODE = "test_wx_code";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final Long TEST_TENANT_ID = 100L;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_BINDING_ID = 200L;
    private static final Long TEST_DEPT_ID = 10L;

    @BeforeEach
    void setUp()
    {
        // 模拟微信配置（lenient：部分测试在 code 交换阶段就失败，不会调用此 stub）
        lenient().when(wechatMpProperties.getAppId()).thenReturn(TEST_APP_ID);
        // 模拟 Redis 无 IP 黑名单
        lenient().when(redisService.getCacheObject(anyString())).thenReturn(null);
        // 模拟租户已启用微信登录（Task 3A：assertWechatLoginEnabled 后端再校验）
        // lenient：部分测试在 assertWechatLoginEnabled 之前就失败，不会调用此 stub
        lenient().when(remoteUserService.isWechatLoginEnabled(anyLong(), eq("inner")))
                .thenReturn(R.ok(true));
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    private SysUser createActiveUser()
    {
        SysUser user = new SysUser();
        user.setUserId(TEST_USER_ID);
        user.setUserName(TEST_USERNAME);
        user.setNickName("测试用户");
        user.setDelFlag("0"); // 正常
        user.setStatus("0");  // 正常
        user.setDeptId(TEST_DEPT_ID);
        user.setTenantId(TEST_TENANT_ID);
        user.setPassword("$2a$10$encryptedPasswordHash");
        return user;
    }

    private LoginUser createLoginUser(SysUser user)
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(user.getUserId());
        loginUser.setUsername(user.getUserName());
        loginUser.setSysUser(user);
        loginUser.setDeptId(user.getDeptId());
        loginUser.setRoles(new HashSet<>());
        loginUser.setPermissions(new HashSet<>());
        return loginUser;
    }

    private SysUserMpBinding createActiveBinding()
    {
        SysUserMpBinding binding = new SysUserMpBinding();
        binding.setBindingId(TEST_BINDING_ID);
        binding.setTenantId(TEST_TENANT_ID);
        binding.setUserId(TEST_USER_ID);
        binding.setAppId(TEST_APP_ID);
        binding.setOpenid(TEST_OPENID);
        binding.setUnionid(TEST_UNIONID);
        binding.setStatus("ACTIVE");
        binding.setBoundTime(new Date());
        binding.setBoundBy(TEST_USERNAME);
        return binding;
    }

    private WechatIdentity createWechatIdentity()
    {
        return new WechatIdentity(TEST_OPENID, TEST_UNIONID);
    }

    // =========================================================================
    // 微信快捷登录测试 POST /auth/mp/wechat/login
    // =========================================================================

    @Test
    void wechatLogin_success_returnsLoginUser()
    {
        // 已绑定微信快捷登录成功
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(createActiveBinding()));

        SysUser user = createActiveUser();
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfoById(TEST_USER_ID, "inner"))
                .thenReturn(R.ok(loginUser));

        LoginUser result = sysLoginService.wechatLogin(TEST_CODE, null);

        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserid());
        assertEquals(TEST_USERNAME, result.getUsername());
        // 验证更新了最近登录时间
        verify(remoteUserMpBindingService).updateLastLoginTime(TEST_TENANT_ID, TEST_BINDING_ID, "inner");
    }

    @Test
    void wechatLogin_noBinding_throwsServiceException()
    {
        // 未绑定微信身份，登录失败
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null)); // 无绑定

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        // 错误信息不泄露 openid
        assertFalse(ex.getMessage().contains(TEST_OPENID));
        assertFalse(ex.getMessage().toLowerCase().contains("openid"));
    }

    @Test
    void wechatLogin_codeExpired_throwsServiceException()
    {
        // 微信 code 失效
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenThrow(new ServiceException("微信登录凭证已失效，请重新发起登录"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        assertEquals("微信登录凭证已失效，请重新发起登录", ex.getMessage());
    }

    @Test
    void wechatLogin_userDeleted_throwsServiceException()
    {
        // 账号已删除，登录失败
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(createActiveBinding()));

        SysUser user = createActiveUser();
        user.setDelFlag("2"); // 已删除
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfoById(TEST_USER_ID, "inner"))
                .thenReturn(R.ok(loginUser));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        assertTrue(ex.getMessage().contains("已删除"));
    }

    @Test
    void wechatLogin_userDisabled_throwsServiceException()
    {
        // 账号已停用，登录失败
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(createActiveBinding()));

        SysUser user = createActiveUser();
        user.setStatus("1"); // 停用
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfoById(TEST_USER_ID, "inner"))
                .thenReturn(R.ok(loginUser));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        assertTrue(ex.getMessage().contains("停用") || ex.getMessage().contains("已停用"));
    }

    @Test
    void wechatLogin_afterUnbind_throwsServiceException()
    {
        // 解绑后微信登录失败（绑定已 REVOKED，查询返回 null）
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null)); // 解绑后查不到 ACTIVE 绑定

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        assertFalse(ex.getMessage().contains(TEST_OPENID));
    }

    @Test
    void wechatLogin_feignFail_throwsServiceException()
    {
        // Feign 调用失败，fail-closed
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.fail("服务调用失败"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        assertFalse(ex.getMessage().contains(TEST_OPENID));
    }

    // ── Task 3A：租户未启用微信登录时 fail-closed ──

    @Test
    void wechatLogin_wechatLoginDisabled_throwsServiceException()
    {
        // 租户关闭了微信登录，即使已绑定也应拒绝
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(createActiveBinding()));
        // 覆盖 setUp 中的 lenient mock：租户未启用微信登录
        when(remoteUserService.isWechatLoginEnabled(TEST_TENANT_ID, "inner"))
                .thenReturn(R.ok(false));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        assertTrue(ex.getMessage().contains("微信登录未开启"));
        // 不应该继续查询用户信息
        verify(remoteUserService, never()).getUserInfoById(anyLong(), eq("inner"));
    }

    @Test
    void wechatLogin_isWechatLoginEnabledFeignFail_throwsServiceException()
    {
        // isWechatLoginEnabled Feign 调用失败，fail-closed
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(createActiveBinding()));
        when(remoteUserService.isWechatLoginEnabled(TEST_TENANT_ID, "inner"))
                .thenReturn(R.fail("服务不可用"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        assertTrue(ex.getMessage().contains("微信登录未开启"));
    }

    // =========================================================================
    // 微信绑定测试 POST /auth/mp/wechat/bind
    // =========================================================================

    @Test
    void wechatBind_success_returnsLoginUser()
    {
        // 未绑定首次绑定成功
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null)); // 无现有绑定

        SysUser user = createActiveUser();
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfo(TEST_USERNAME, "inner"))
                .thenReturn(R.ok(loginUser));
        // 密码校验通过（不抛异常）
        doNothing().when(passwordService).validate(user, TEST_PASSWORD);
        when(remoteUserMpBindingService.insert(any(SysUserMpBinding.class), eq("inner")))
                .thenReturn(R.ok(1));

        LoginUser result = sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null);

        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserid());
        // 验证插入了绑定关系
        verify(remoteUserMpBindingService).insert(any(SysUserMpBinding.class), eq("inner"));
    }

    @Test
    void wechatBind_alreadyBound_throwsServiceException()
    {
        // 同一 (appId, openid) 已绑定另一个账号，不能重复绑定
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());

        SysUserMpBinding existingBinding = createActiveBinding();
        existingBinding.setUserId(999L); // 绑定到了另一个用户
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(existingBinding));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        // 错误信息不泄露 openid
        assertFalse(ex.getMessage().contains(TEST_OPENID));
        assertFalse(ex.getMessage().toLowerCase().contains("openid"));
    }

    @Test
    void wechatBind_alreadyBoundSameUser_throwsServiceException()
    {
        // 同一用户重复绑定，提示已绑定
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());

        SysUserMpBinding existingBinding = createActiveBinding(); // 同一用户
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(existingBinding));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        assertTrue(ex.getMessage().contains("已绑定") || ex.getMessage().contains("绑定"));
    }

    @Test
    void wechatBind_wrongPassword_throwsServiceException()
    {
        // 绑定时密码错误
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null));

        SysUser user = createActiveUser();
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfo(TEST_USERNAME, "inner"))
                .thenReturn(R.ok(loginUser));
        // 密码校验失败
        doThrow(new ServiceException("用户不存在/密码错误"))
                .when(passwordService).validate(user, TEST_PASSWORD);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        // 不应该创建绑定
        verify(remoteUserMpBindingService, never()).insert(any(), eq("inner"));
    }

    @Test
    void wechatBind_userDeleted_throwsServiceException()
    {
        // 绑定时账号已删除
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null));

        SysUser user = createActiveUser();
        user.setDelFlag("2"); // 已删除
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfo(TEST_USERNAME, "inner"))
                .thenReturn(R.ok(loginUser));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        assertTrue(ex.getMessage().contains("已删除"));
        verify(remoteUserMpBindingService, never()).insert(any(), eq("inner"));
    }

    @Test
    void wechatBind_userDisabled_throwsServiceException()
    {
        // 绑定时账号已停用
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null));

        SysUser user = createActiveUser();
        user.setStatus("1"); // 停用
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfo(TEST_USERNAME, "inner"))
                .thenReturn(R.ok(loginUser));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        assertTrue(ex.getMessage().contains("停用"));
        verify(remoteUserMpBindingService, never()).insert(any(), eq("inner"));
    }

    @Test
    void wechatBind_codeExpired_throwsServiceException()
    {
        // 绑定时微信 code 失效
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenThrow(new ServiceException("微信登录凭证已失效，请重新发起登录"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        assertEquals("微信登录凭证已失效，请重新发起登录", ex.getMessage());
    }

    @Test
    void wechatBind_feignInsertFail_throwsServiceException()
    {
        // 绑定写入失败（如唯一键冲突）
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null));

        SysUser user = createActiveUser();
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfo(TEST_USERNAME, "inner"))
                .thenReturn(R.ok(loginUser));
        doNothing().when(passwordService).validate(user, TEST_PASSWORD);
        when(remoteUserMpBindingService.insert(any(SysUserMpBinding.class), eq("inner")))
                .thenReturn(R.fail("唯一键冲突"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        // 错误信息脱敏，不泄露内部错误
        assertFalse(ex.getMessage().contains(TEST_OPENID));
    }

    // ── Task 3A：租户未启用微信登录时绑定 fail-closed ──

    @Test
    void wechatBind_wechatLoginDisabled_throwsServiceException()
    {
        // 租户关闭了微信登录，绑定也应拒绝
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null)); // 无现有绑定

        SysUser user = createActiveUser();
        LoginUser loginUser = createLoginUser(user);
        when(remoteUserService.getUserInfo(TEST_USERNAME, "inner"))
                .thenReturn(R.ok(loginUser));
        // 覆盖 setUp 中的 lenient mock：租户未启用微信登录
        when(remoteUserService.isWechatLoginEnabled(TEST_TENANT_ID, "inner"))
                .thenReturn(R.ok(false));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        assertTrue(ex.getMessage().contains("微信登录未开启"));
        // 不应该插入绑定关系
        verify(remoteUserMpBindingService, never()).insert(any(), eq("inner"));
    }

    // =========================================================================
    // 微信解绑测试 POST /auth/mp/wechat/unbind
    // =========================================================================

    @Test
    void wechatUnbind_success()
    {
        // 解绑成功
        SysUserMpBinding binding = createActiveBinding();
        when(remoteUserMpBindingService.selectByUserId(TEST_TENANT_ID, TEST_USER_ID, "inner"))
                .thenReturn(R.ok(List.of(binding)));
        when(remoteUserMpBindingService.revoke(any(SysUserMpBinding.class), eq("inner")))
                .thenReturn(R.ok(1));

        sysLoginService.wechatUnbind(TEST_TENANT_ID, TEST_USER_ID, TEST_USERNAME, "用户主动解绑");

        verify(remoteUserMpBindingService).revoke(any(SysUserMpBinding.class), eq("inner"));
    }

    @Test
    void wechatUnbind_noBinding_throwsServiceException()
    {
        // 无绑定关系，解绑失败
        when(remoteUserMpBindingService.selectByUserId(TEST_TENANT_ID, TEST_USER_ID, "inner"))
                .thenReturn(R.ok(Collections.emptyList()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatUnbind(TEST_TENANT_ID, TEST_USER_ID, TEST_USERNAME, null));

        assertFalse(ex.getMessage().contains(TEST_OPENID));
    }

    @Test
    void wechatUnbind_alreadyRevoked_throwsServiceException()
    {
        // 绑定已撤销，不能重复解绑
        SysUserMpBinding binding = createActiveBinding();
        binding.setStatus("REVOKED");
        when(remoteUserMpBindingService.selectByUserId(TEST_TENANT_ID, TEST_USER_ID, "inner"))
                .thenReturn(R.ok(List.of(binding)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatUnbind(TEST_TENANT_ID, TEST_USER_ID, TEST_USERNAME, null));

        verify(remoteUserMpBindingService, never()).revoke(any(), eq("inner"));
    }

    // =========================================================================
    // 查询绑定列表测试 GET /auth/mp/wechat/binding
    // =========================================================================

    @Test
    void getWechatBindings_returnsList()
    {
        SysUserMpBinding binding = createActiveBinding();
        when(remoteUserMpBindingService.selectByUserId(TEST_TENANT_ID, TEST_USER_ID, "inner"))
                .thenReturn(R.ok(List.of(binding)));

        List<SysUserMpBinding> result = sysLoginService.getWechatBindings(TEST_TENANT_ID, TEST_USER_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
    }

    @Test
    void getWechatBindings_empty_returnsEmptyList()
    {
        when(remoteUserMpBindingService.selectByUserId(TEST_TENANT_ID, TEST_USER_ID, "inner"))
                .thenReturn(R.ok(Collections.emptyList()));

        List<SysUserMpBinding> result = sysLoginService.getWechatBindings(TEST_TENANT_ID, TEST_USER_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // 安全性测试：错误信息不泄露敏感数据
    // =========================================================================

    @Test
    void wechatLogin_errorMessageDoesNotLeakOpenidOrAppSecret()
    {
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatLogin(TEST_CODE, null));

        String msg = ex.getMessage().toLowerCase();
        assertFalse(msg.contains(TEST_OPENID));
        assertFalse(msg.contains("openid"));
        assertFalse(msg.contains("appsecret"));
        assertFalse(msg.contains(TEST_APP_ID));
    }

    @Test
    void wechatBind_errorMessageDoesNotLeakOpenidOrAppSecret()
    {
        when(wechatMiniProgramService.exchangeCodeForIdentity(TEST_CODE))
                .thenReturn(createWechatIdentity());

        SysUserMpBinding existingBinding = createActiveBinding();
        existingBinding.setUserId(999L);
        when(remoteUserMpBindingService.selectByAppOpenid(TEST_APP_ID, TEST_OPENID, "inner"))
                .thenReturn(R.ok(existingBinding));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> sysLoginService.wechatBind(TEST_CODE, TEST_USERNAME, TEST_PASSWORD, null));

        String msg = ex.getMessage().toLowerCase();
        assertFalse(msg.contains(TEST_OPENID));
        assertFalse(msg.contains("openid"));
        assertFalse(msg.contains("appsecret"));
        assertFalse(msg.contains(TEST_APP_ID));
    }
}
