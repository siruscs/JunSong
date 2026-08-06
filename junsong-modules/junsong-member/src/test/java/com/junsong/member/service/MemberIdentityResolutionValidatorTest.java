package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MemberIdentityResolutionValidatorTest
{
    @Test
    void acceptsMemberWithoutPhoneOrIdCardWhenNameIsPresent()
    {
        assertDoesNotThrow(() -> MemberIdentityResolutionValidator.validate("张三", null, null, "NAME"));
    }

    @Test
    void rejectsEmptyIdentityForNonAnonymousPurchase()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberIdentityResolutionValidator.validate(null, null, null, "NAME"));
    }

    @Test
    void allowsAnonymousWalkInOnlyWithAnonymousMode()
    {
        assertDoesNotThrow(() -> MemberIdentityResolutionValidator.validate(null, null, null, "ANONYMOUS"));
        assertThrows(IllegalArgumentException.class,
                () -> MemberIdentityResolutionValidator.validate(null, null, null, "PHONE"));
    }

    @Test
    void configuredModeRequiresItsConfiguredIdentity()
    {
        assertDoesNotThrow(() -> MemberIdentityResolutionValidator.validate("张三", null, null, "NAME"));
        assertThrows(IllegalArgumentException.class,
                () -> MemberIdentityResolutionValidator.validate(null, "13800138000", null, "NAME"));
        assertDoesNotThrow(() -> MemberIdentityResolutionValidator.validate(null, "13800138000", null, "PHONE"));
        assertThrows(IllegalArgumentException.class,
                () -> MemberIdentityResolutionValidator.validate("张三", null, null, "PHONE"));
    }

    @Test
    void unsupportedModeIsRejected()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberIdentityResolutionValidator.validate("张三", null, null, "EMAIL"));
    }
}
