package com.junsong.workflow.security;

import java.util.Set;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.api.model.LoginUser;
import org.springframework.stereotype.Component;

@Component
public class CurrentWorkflowUserFacade
{
    public CurrentWorkflowUser current()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser != null && loginUser.getUserid() != null
                ? loginUser.getUserid() : SecurityUtils.getUserId();
        String username = loginUser != null && loginUser.getUsername() != null && !loginUser.getUsername().isBlank()
                ? loginUser.getUsername() : SecurityUtils.getUsername();

        if (userId == null || username == null || username.isBlank())
        {
            throw new ServiceException("未获取到当前登录用户");
        }

        Set<String> roles = loginUser == null ? Set.of() : loginUser.getRoles();
        Set<String> permissions = loginUser == null ? Set.of() : loginUser.getPermissions();
        return new CurrentWorkflowUser(userId, username.trim(), roles, permissions);
    }
}
