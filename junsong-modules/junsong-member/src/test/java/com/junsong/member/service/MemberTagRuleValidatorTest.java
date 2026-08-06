package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class MemberTagRuleValidatorTest
{
    @Test
    void acceptsVersionedRule()
    {
        assertDoesNotThrow(() -> MemberTagRuleValidator.validate("REPEAT_BUYER", "复购会员", 1, "{}"));
    }

    @Test
    void rejectsInvalidVersionOrExpression()
    {
        assertThrows(IllegalArgumentException.class,
                () -> MemberTagRuleValidator.validate("R", "规则", 0, "{}"));
        assertThrows(IllegalArgumentException.class,
                () -> MemberTagRuleValidator.validate("R", "规则", 1, ""));
    }
}
