package com.junsong.workflow.security;

import java.util.Set;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentWorkflowUserFacadeTest
{
    private final CurrentWorkflowUserFacade facade = new CurrentWorkflowUserFacade();

    @BeforeEach
    void clearSecurityContextBeforeTest()
    {
        SecurityContextHolder.remove();
    }

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.remove();
    }

    @Test
    void returnsAuthenticatedWorkflowUser()
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(17L);
        loginUser.setUsername("wjs");
        loginUser.setRoles(Set.of("finance", "manager"));
        loginUser.setPermissions(Set.of("workflow:instance:terminate"));
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);

        CurrentWorkflowUser currentUser = facade.current();

        assertEquals(17L, currentUser.userId());
        assertEquals("wjs", currentUser.username());
        assertEquals(Set.of("finance", "manager"), currentUser.roleKeys());
        assertTrue(currentUser.hasPermission("workflow:instance:terminate"));
    }

    @Test
    void rejectsMissingLoginContext()
    {
        ServiceException exception = assertThrows(ServiceException.class, facade::current);

        assertEquals("未获取到当前登录用户", exception.getMessage());
    }

    @Test
    void trimsAuthenticatedUsername()
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(17L);
        loginUser.setUsername(" wjs ");
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);

        CurrentWorkflowUser currentUser = facade.current();

        assertEquals("wjs", currentUser.username());
    }
}
