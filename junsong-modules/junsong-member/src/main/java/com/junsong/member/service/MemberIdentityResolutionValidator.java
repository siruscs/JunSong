package com.junsong.member.service;

public final class MemberIdentityResolutionValidator
{
    private MemberIdentityResolutionValidator() { }

    public static void validate(String name, String phone, String memberNo, String mode)
    {
        if (mode == null || mode.isBlank()) throw new IllegalArgumentException("identity mode is required");
        if ("ANONYMOUS".equals(mode))
        {
            return;
        }
        boolean valid = switch (mode)
        {
            case "NAME" -> hasText(name);
            case "PHONE" -> hasText(phone);
            case "MEMBER_NO" -> hasText(memberNo);
            case "MANUAL" -> hasText(name) || hasText(phone) || hasText(memberNo);
            default -> throw new IllegalArgumentException("unsupported identity mode");
        };
        if (!valid) throw new IllegalArgumentException("configured customer identity is required");
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.isBlank();
    }
}
