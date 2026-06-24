package com.junsong.workflow.security;

import java.util.Set;

import org.springframework.util.PatternMatchUtils;

public record CurrentWorkflowUser(Long userId, String username, Set<String> roleKeys, Set<String> permissions)
{
    private static final String ALL_PERMISSION = "*:*:*";

    public CurrentWorkflowUser
    {
        roleKeys = roleKeys == null ? Set.of() : Set.copyOf(roleKeys);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean hasPermission(String permission)
    {
        return permissions.stream()
                .anyMatch(value -> ALL_PERMISSION.equals(value) || PatternMatchUtils.simpleMatch(value, permission));
    }
}
