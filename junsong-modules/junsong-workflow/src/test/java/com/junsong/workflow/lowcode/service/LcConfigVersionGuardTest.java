package com.junsong.workflow.lowcode.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LcConfigVersionGuardTest
{
    @Test
    void acceptsLegacyDraftWithoutSourceVersion()
    {
        assertDoesNotThrow(() -> LcConfigVersionGuard.requireCompatible(null, 3));
    }

    @Test
    void acceptsDraftBasedOnCurrentPublishedVersion()
    {
        assertDoesNotThrow(() -> LcConfigVersionGuard.requireCompatible(3, 3));
    }

    @Test
    void rejectsDraftBasedOnStalePublishedVersion()
    {
        assertThrows(IllegalStateException.class,
                () -> LcConfigVersionGuard.requireCompatible(2, 3));
    }
}
