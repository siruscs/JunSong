package com.junsong.workflow.service.identity;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeptUserResolveServiceTest
{
    private StubPeerUserService peerUserService;
    private DeptUserResolveService resolveService;

    @BeforeEach
    void setUp()
    {
        peerUserService = new StubPeerUserService();
        resolveService = new DeptUserResolveService();
        ReflectionTestUtils.setField(resolveService, "peerUserService", peerUserService);
    }

    @Test
    void validatesAndReturnsUserDepartmentLeader()
    {
        SysUser user = userWithDept(dept(10L, " lisi "));
        peerUserService.activeUser = user;
        peerUserService.validatedUsername = "lisi";

        assertEquals("lisi", resolveService.getDeptLeader("zhangsan"));
        assertEquals("lisi", peerUserService.requestedUsername);
    }

    @Test
    void rejectsDepartmentWithoutLeader()
    {
        peerUserService.activeUser = userWithDept(dept(10L, "  "));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> resolveService.getDeptLeader("zhangsan"));

        assertEquals("部门未配置负责人: 10", exception.getMessage());
    }

    @Test
    void fallsBackToFirstAuthorizedDepartment()
    {
        SysUser user = new SysUser();
        peerUserService.activeUser = user;
        peerUserService.authorizedDepts = List.of(dept(20L, "wangwu"));
        peerUserService.validatedUsername = "wangwu";

        assertEquals("wangwu", resolveService.getDeptLeader("zhangsan"));
        assertEquals(1, peerUserService.activeUserCallCount);
        assertEquals(1, peerUserService.authorizedDeptsForValidatedUserCallCount);
    }

    @Test
    void rejectsUserWithoutDepartment()
    {
        peerUserService.activeUser = new SysUser();
        peerUserService.authorizedDepts = List.of();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> resolveService.getDeptLeader("zhangsan"));

        assertEquals("用户没有归属或授权部门: zhangsan", exception.getMessage());
    }

    private SysUser userWithDept(SysDept dept)
    {
        SysUser user = new SysUser();
        user.setDept(dept);
        return user;
    }

    private SysDept dept(long deptId, String leader)
    {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setLeader(leader);
        return dept;
    }

    private static class StubPeerUserService extends PeerUserService
    {
        private SysUser activeUser;
        private List<SysDept> authorizedDepts = List.of();
        private String validatedUsername;
        private String requestedUsername;
        private int activeUserCallCount;
        private int authorizedDeptsForValidatedUserCallCount;

        @Override
        public SysUser requireActiveUser(String username)
        {
            activeUserCallCount++;
            return activeUser;
        }

        @Override
        public List<SysDept> requireAuthorizedDeptsForValidatedUser(String username)
        {
            authorizedDeptsForValidatedUserCallCount++;
            return authorizedDepts;
        }

        @Override
        public String requireUsername(Object raw)
        {
            requestedUsername = raw.toString();
            return validatedUsername;
        }
    }
}
