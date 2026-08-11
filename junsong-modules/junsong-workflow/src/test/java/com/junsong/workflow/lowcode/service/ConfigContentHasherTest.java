package com.junsong.workflow.lowcode.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ConfigContentHasherTest
{
    @Test
    void producesStableSha256ForSameContent()
    {
        String first = ConfigContentHasher.sha256("{\"bizCode\":\"expense\"}");
        String second = ConfigContentHasher.sha256("{\"bizCode\":\"expense\"}");

        assertEquals(64, first.length());
        assertEquals(first, second);
    }

    @Test
    void changesWhenSnapshotContentChanges()
    {
        assertNotEquals(
                ConfigContentHasher.sha256("{\"version\":1}"),
                ConfigContentHasher.sha256("{\"version\":2}"));
    }
}
