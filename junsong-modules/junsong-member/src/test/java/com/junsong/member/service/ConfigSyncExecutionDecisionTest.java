package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigSyncExecutionDecisionTest
{
    @Test
    void diffRequiresAnExplicitOverwriteOrSkipDecision()
    {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigSyncExecutionDecisions.resolve("DIFF", null));
        assertEquals("OVERWRITE", ConfigSyncExecutionDecisions.resolve("DIFF", "OVERWRITE"));
        assertEquals("SKIP", ConfigSyncExecutionDecisions.resolve("DIFF", "skip"));
    }

    @Test
    void createAndNoopHaveSafeDefaultsButRejectUnsupportedDecision()
    {
        assertEquals("CREATE", ConfigSyncExecutionDecisions.resolve("CREATE", null));
        assertEquals("SKIP", ConfigSyncExecutionDecisions.resolve("NOOP", null));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigSyncExecutionDecisions.resolve("CREATE", "OVERWRITE"));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigSyncExecutionDecisions.validateAll(Map.of(1L, "CREATE"), Map.of(1L, "DIFF", 2L, "NOOP")));
    }

    @Test
    void conflictIsBlockedAndDefaultsToSkip()
    {
        assertEquals("SKIP", ConfigSyncExecutionDecisions.resolve("CONFLICT", null));
        assertEquals("SKIP", ConfigSyncExecutionDecisions.resolve("CONFLICT", "SKIP"));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigSyncExecutionDecisions.resolve("CONFLICT", "CREATE"));
    }

    @Test
    void levelInUseIsBlockedAndCanOnlyBeSkipped()
    {
        assertEquals("SKIP", ConfigSyncExecutionDecisions.resolve("IMPACT_BLOCKED", null));
        assertEquals("SKIP", ConfigSyncExecutionDecisions.resolve("IMPACT_BLOCKED", "skip"));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigSyncExecutionDecisions.resolve("IMPACT_BLOCKED", "OVERWRITE"));
    }
}
