package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MemberLifecycleTransitionValidatorTest
{
    @Test
    void activeMemberCanExpireOrBeCancelled()
    {
        assertDoesNotThrow(() -> MemberLifecycleTransitionValidator.validate("ACTIVE", "EXPIRED"));
        assertDoesNotThrow(() -> MemberLifecycleTransitionValidator.validate("ACTIVE", "CANCELLED"));
    }

    @Test
    void cancelledMemberCannotReturnToActive()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberLifecycleTransitionValidator.validate("CANCELLED", "ACTIVE"));
    }

    @Test
    void unknownStatusIsRejected()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberLifecycleTransitionValidator.validate("UNKNOWN", "ACTIVE"));
    }
}
