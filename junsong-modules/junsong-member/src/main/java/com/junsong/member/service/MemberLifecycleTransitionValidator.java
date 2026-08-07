package com.junsong.member.service;

import java.util.Map;
import java.util.Set;

/** Validates member lifecycle transitions independently from persistence. */
public final class MemberLifecycleTransitionValidator
{
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE", "EXPIRED", "CANCELLED", "ANONYMIZED");
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "ACTIVE", Set.of("INACTIVE", "EXPIRED", "CANCELLED"),
            "INACTIVE", Set.of("ACTIVE", "CANCELLED"),
            "EXPIRED", Set.of("ACTIVE", "CANCELLED"),
            "CANCELLED", Set.of("ANONYMIZED"),
            "ANONYMIZED", Set.of());

    private MemberLifecycleTransitionValidator() { }

    public static void validate(String current, String target)
    {
        if (!STATUSES.contains(current) || !STATUSES.contains(target)
                || !ALLOWED.getOrDefault(current, Set.of()).contains(target))
        {
            throw new IllegalArgumentException("invalid member lifecycle transition");
        }
    }
}
