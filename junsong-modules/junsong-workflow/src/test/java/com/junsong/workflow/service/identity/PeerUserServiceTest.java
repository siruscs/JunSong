package com.junsong.workflow.service.identity;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeerUserServiceTest
{
    private StubRemoteUserService remoteUserService;
    private PeerUserService peerUserService;

    @BeforeEach
    void setUp()
    {
        remoteUserService = new StubRemoteUserService();
        peerUserService = new PeerUserService();
        ReflectionTestUtils.setField(peerUserService, "remoteUserService", remoteUserService);
    }

    @Test
    void returnsActiveUser()
    {
        SysUser user = activeUser("zhangsan");
        remoteUserService.userInfoResult = userResult(user);

        assertSame(user, peerUserService.requireActiveUser(" zhangsan "));
    }

    @Test
    void rejectsDisabledUser()
    {
        SysUser user = activeUser("zhangsan");
        user.setStatus("1");
        remoteUserService.userInfoResult = userResult(user);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户已停用或删除: zhangsan", exception.getMessage());
    }

    @Test
    void exposesRemoteFailureMessage()
    {
        remoteUserService.userInfoResult = R.fail("用户服务调用失败");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户体系服务不可用: 用户服务调用失败", exception.getMessage());
    }

    @Test
    void classifiesBadCredentialsResponseAsMissingUser()
    {
        remoteUserService.userInfoResult = R.fail("用户名或密码错误");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser(" zhangsan "));

        assertEquals("用户不存在: zhangsan", exception.getMessage());
    }

    @Test
    void classifiesExplicitMissingUserResponseAsMissingUser()
    {
        remoteUserService.userInfoResult = R.fail("用户不存在");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户不存在: zhangsan", exception.getMessage());
    }

    @Test
    void rejectsBlankUsername()
    {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("  "));

        assertEquals("用户名不能为空", exception.getMessage());
    }

    @Test
    void rejectsNullRemoteResponse()
    {
        remoteUserService.userInfoResult = null;

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户体系服务不可用: 空响应", exception.getMessage());
    }

    @Test
    void rejectsMissingUser()
    {
        remoteUserService.userInfoResult = R.ok(new LoginUser());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户不存在: zhangsan", exception.getMessage());
    }

    @Test
    void rejectsDeletedUser()
    {
        SysUser user = activeUser("zhangsan");
        user.setDelFlag("1");
        remoteUserService.userInfoResult = userResult(user);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户已停用或删除: zhangsan", exception.getMessage());
    }

    @Test
    void rejectsBlankCanonicalUsername()
    {
        SysUser user = activeUser("  ");
        remoteUserService.userInfoResult = userResult(user);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户体系返回的 username 为空: zhangsan", exception.getMessage());
    }

    @Test
    void rejectsNullCanonicalUsername()
    {
        SysUser user = activeUser(null);
        remoteUserService.userInfoResult = userResult(user);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户体系返回的 username 为空: zhangsan", exception.getMessage());
    }

    @Test
    void rejectsMismatchedCanonicalUsername()
    {
        SysUser user = activeUser("lisi");
        remoteUserService.userInfoResult = userResult(user);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireActiveUser("zhangsan"));

        assertEquals("用户体系返回的 username 不匹配: zhangsan", exception.getMessage());
    }

    @Test
    void rejectsNumericAssignee()
    {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireUsername(123L));

        assertEquals("审批人必须使用有效 username", exception.getMessage());
    }

    @Test
    void validatesAndReturnsCanonicalUsername()
    {
        SysUser user = activeUser(" LiSi ");
        remoteUserService.userInfoResult = userResult(user);

        assertEquals("LiSi", peerUserService.requireUsername(" lisi "));
    }

    @Test
    void rejectsAuthorizedDepartmentRemoteFailure()
    {
        SysUser user = activeUser("zhangsan");
        remoteUserService.userInfoResult = userResult(user);
        remoteUserService.deptListResult = R.fail("部门服务调用失败");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireAuthorizedDepts("zhangsan"));

        assertEquals("用户体系服务不可用: 部门服务调用失败", exception.getMessage());
    }

    @Test
    void returnsImmutableAuthorizedDepartmentCopy()
    {
        SysUser user = activeUser("zhangsan");
        SysDept dept = new SysDept();
        dept.setDeptId(10L);
        remoteUserService.userInfoResult = userResult(user);
        remoteUserService.deptListResult = R.ok(List.of(dept));

        List<SysDept> result = peerUserService.requireAuthorizedDepts("zhangsan");

        assertEquals(List.of(dept), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(new SysDept()));
    }

    @Test
    void rejectsNullAuthorizedDepartmentElement()
    {
        SysUser user = activeUser("zhangsan");
        remoteUserService.userInfoResult = userResult(user);
        remoteUserService.deptListResult = R.ok(java.util.Arrays.asList(new SysDept(), null));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> peerUserService.requireAuthorizedDepts("zhangsan"));

        assertEquals("用户体系服务不可用: 授权部门数据包含空值", exception.getMessage());
    }

    private SysUser activeUser(String username)
    {
        SysUser user = new SysUser();
        user.setUserName(username);
        user.setStatus("0");
        user.setDelFlag("0");
        return user;
    }

    private R<LoginUser> userResult(SysUser user)
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setSysUser(user);
        return R.ok(loginUser);
    }

    private static class StubRemoteUserService implements RemoteUserService
    {
        private R<LoginUser> userInfoResult;
        private R<List<SysDept>> deptListResult;

        @Override
        public R<LoginUser> getUserInfo(String username, String source)
        {
            return userInfoResult;
        }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source)
        {
            return deptListResult;
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source)
        {
            throw new UnsupportedOperationException();
        }
    }
}
