package com.junsong.workflow.security;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentWorkflowUserTest
{
    @Test
    void grantsEveryPermissionForGlobalWildcard()
    {
        CurrentWorkflowUser user = new CurrentWorkflowUser(17L, "wjs", Set.of(), Set.of("*:*:*"));

        assertTrue(user.hasPermission("workflow:instance:terminate"));
    }

    @Test
    void normalizesNullRoleAndPermissionSets()
    {
        CurrentWorkflowUser user = new CurrentWorkflowUser(17L, "wjs", null, null);

        assertEquals(Set.of(), user.roleKeys());
        assertEquals(Set.of(), user.permissions());
    }

    @Test
    void defensivelyCopiesRoleAndPermissionSets()
    {
        Set<String> roles = new HashSet<>(Set.of("finance"));
        Set<String> permissions = new HashSet<>(Set.of("workflow:instance:terminate"));
        CurrentWorkflowUser user = new CurrentWorkflowUser(17L, "wjs", roles, permissions);

        roles.add("manager");
        permissions.add("workflow:instance:start");

        assertEquals(Set.of("finance"), user.roleKeys());
        assertEquals(Set.of("workflow:instance:terminate"), user.permissions());
        assertThrows(UnsupportedOperationException.class, () -> user.roleKeys().add("manager"));
        assertThrows(UnsupportedOperationException.class,
                () -> user.permissions().add("workflow:instance:start"));
    }
}
