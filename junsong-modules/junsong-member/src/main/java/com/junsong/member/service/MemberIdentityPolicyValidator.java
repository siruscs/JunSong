package com.junsong.member.service;

public final class MemberIdentityPolicyValidator
{
    private MemberIdentityPolicyValidator() { }

    public static void validate(String mode, Boolean allowAnonymous)
    {
        if (mode == null || !(mode.equals("NAME") || mode.equals("PHONE")
                || mode.equals("MEMBER_NO") || mode.equals("MANUAL")))
            throw new IllegalArgumentException("unsupported identity policy mode");
        if (Boolean.TRUE.equals(allowAnonymous) && "MEMBER_NO".equals(mode))
            throw new IllegalArgumentException("member number policy cannot identify anonymous customers");
    }
}
