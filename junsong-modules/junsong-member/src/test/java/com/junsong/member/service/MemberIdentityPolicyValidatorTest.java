package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class MemberIdentityPolicyValidatorTest
{
    @Test
    void acceptsSupportedModes()
    {
        assertDoesNotThrow(() -> MemberIdentityPolicyValidator.validate("NAME", true));
        assertDoesNotThrow(() -> MemberIdentityPolicyValidator.validate("PHONE", false));
    }

    @Test
    void rejectsUnsupportedMode()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberIdentityPolicyValidator.validate("EMAIL", false));
    }

    @Test
    void memberNumberCannotBeAnonymousStrategy()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberIdentityPolicyValidator.validate("MEMBER_NO", true));
    }
}
