package com.junsong.workflow.lowcode.runtime;

import com.junsong.workflow.lowcode.domain.LcBizField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LcFieldRuntimePolicyTest
{
    @Test
    void listAndDetailUseTheirOwnVisibilityFlags()
    {
        LcBizField field = field("1", "1", "1", null);
        assertTrue(LcFieldRuntimePolicy.resolve(field, "LIST").visible());
        assertTrue(LcFieldRuntimePolicy.resolve(field, "DETAIL").visible());
        assertFalse(LcFieldRuntimePolicy.resolve(field, "DETAIL").editable());
    }

    @Test
    void stageControlsEditabilityAndRequiredFlag()
    {
        LcBizField field = field("0", "0", "1", "FULFILLMENT");
        assertFalse(LcFieldRuntimePolicy.resolve(field, "CREATE").visible());
        LcFieldRuntimePolicy.Policy policy = LcFieldRuntimePolicy.resolve(field, "FULFILLMENT");
        assertTrue(policy.visible());
        assertTrue(policy.editable());
        assertTrue(policy.required());
    }

    @Test
    void nullOrUnknownPageFailsClosed()
    {
        LcBizField field = field("1", "1", "1", null);
        assertFalse(LcFieldRuntimePolicy.resolve(field, null).visible());
        assertFalse(LcFieldRuntimePolicy.resolve(field, "UNKNOWN").visible());
    }

    private static LcBizField field(String list, String detail, String required, String stage)
    {
        LcBizField field = new LcBizField();
        field.setIsList(list); field.setIsDetail(detail); field.setRequired(required); field.setStage(stage);
        return field;
    }
}
