package com.junsong.member.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MemberCampaignPolicyServiceImplTest
{
    @Test
    void normalizeDateAcceptsJdbcLocalDateTime()
    {
        assertNotNull(MemberCampaignPolicyServiceImpl.normalizeDate(LocalDateTime.of(2026, 8, 5, 8, 4, 55)));
    }
}
