package com.junsong.member.service;

import java.math.BigDecimal;

/** Validates immutable points/growth ledger operation metadata. */
public final class MemberLedgerOperationValidator
{
    private MemberLedgerOperationValidator() { }

    public static void validate(String sourceType, String sourceId, String dedupKey, BigDecimal delta)
    {
        if (sourceType == null || sourceType.isBlank() || sourceId == null || sourceId.isBlank()
                || dedupKey == null || dedupKey.isBlank() || delta == null || delta.signum() == 0)
        {
            throw new IllegalArgumentException("ledger source, dedup key and non-zero delta are required");
        }
    }
}
