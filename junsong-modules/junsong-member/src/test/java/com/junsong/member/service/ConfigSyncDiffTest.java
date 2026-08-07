package com.junsong.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigSyncDiffTest
{
    @Test
    void missingTargetIsCreate()
    {
        ConfigSyncDiff diff = ConfigSyncDiff.compare("PRODUCT", "GEL-001",
                Map.of("productName", "凝胶"), null);
        assertEquals("CREATE", diff.operation());
        assertTrue(diff.fields().containsKey("productName"));
    }

    @Test
    void sameTargetIsNoop()
    {
        ConfigSyncDiff diff = ConfigSyncDiff.compare("PRODUCT", "GEL-001",
                Map.of("productName", "凝胶"), Map.of("productName", "凝胶"));
        assertEquals("NOOP", diff.operation());
        assertTrue(diff.fields().isEmpty());
    }

    @Test
    void changedTargetContainsOldAndNewValues()
    {
        ConfigSyncDiff diff = ConfigSyncDiff.compare("PRODUCT", "GEL-001",
                Map.of("productName", "凝胶加强版", "status", "0"),
                Map.of("productName", "凝胶", "status", "1"));
        assertEquals("DIFF", diff.operation());
        assertEquals("凝胶", diff.fields().get("productName").targetValue());
        assertEquals("凝胶加强版", diff.fields().get("productName").sourceValue());
    }

    @Test
    void snapshotWithDatabaseTypesAndJsonTypesIsEquivalent()
    {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("effective_start", "2026-06-08T16:04:55");
        expected.put("packages", List.of(Map.of("purchaseQuantity", 5.0,
                "packagePrice", 2499.0)));

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("effective_start", LocalDateTime.of(2026, 6, 8, 16, 4, 55));
        current.put("packages", List.of(Map.of("purchaseQuantity", new BigDecimal("5.000"),
                "packagePrice", new BigDecimal("2499.00"))));

        assertTrue(ConfigSyncDiff.equivalent(expected, current));
    }
}
