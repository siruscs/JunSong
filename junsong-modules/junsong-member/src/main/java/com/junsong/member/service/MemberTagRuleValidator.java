package com.junsong.member.service;

public final class MemberTagRuleValidator
{
    private MemberTagRuleValidator() { }

    public static void validate(String code, String name, Integer version, String expression)
    {
        if (code == null || code.isBlank() || code.length() > 64)
            throw new IllegalArgumentException("tag rule code is invalid");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("tag rule name is required");
        if (version == null || version < 1) throw new IllegalArgumentException("tag rule version is invalid");
        if (expression == null || expression.isBlank()) throw new IllegalArgumentException("tag rule expression is required");
    }
}
