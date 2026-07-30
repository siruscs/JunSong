package com.junsong.workflow.lowcode.runtime;

import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LcRuntimePageAssemblerTest
{
    private final LcRuntimePageAssembler assembler = new LcRuntimePageAssembler(new LcPageSchemaValidator());

    @Test
    void usesSchemaOrderAndUnifiedRuntimePolicy()
    {
        LcBizField first = field("amount", "金额", "DECIMAL", "1", "1", "1", 2);
        LcBizField second = field("reason", "原因", "STRING", "1", "1", "0", 1);
        LcRuntimePageAssembler.RuntimePage page = assembler.assemble(
                schema("FORM", "{\"fields\":[\"reason\",\"amount\"]}"), List.of(first, second));

        assertEquals(List.of("reason", "amount"), page.fields().stream().map(LcRuntimePageAssembler.RuntimeField::fieldKey).toList());
        assertTrue(page.fields().get(0).visible());
        assertFalse(page.fields().get(0).required());
        assertTrue(page.fields().get(1).required());
    }

    @Test
    void usesObjectFieldSchemaOrder()
    {
        LcBizField first = field("amount", "金额", "DECIMAL", "1", "1", "1", 2);
        LcBizField second = field("reason", "原因", "STRING", "1", "1", "0", 1);
        LcRuntimePageAssembler.RuntimePage page = assembler.assemble(
                schema("FORM", "{\"fields\":[{\"fieldKey\":\"reason\",\"span\":12},{\"fieldKey\":\"amount\",\"span\":12}]}"),
                List.of(first, second));

        assertEquals(List.of("reason", "amount"),
                page.fields().stream().map(LcRuntimePageAssembler.RuntimeField::fieldKey).toList());
    }

    @Test
    void fallsBackToMetadataOrderWhenSchemaHasNoFieldList()
    {
        LcBizField first = field("first", "第一项", "STRING", "1", "1", "0", 1);
        LcBizField second = field("second", "第二项", "STRING", "1", "1", "0", 2);
        var page = assembler.assemble(schema("LIST", "{\"layout\":\"table\"}"), List.of(second, first));
        assertEquals(List.of("first", "second"), page.fields().stream().map(LcRuntimePageAssembler.RuntimeField::fieldKey).toList());
    }

    private static LcBizPageSchema schema(String type, String json)
    {
        LcBizPageSchema schema = new LcBizPageSchema(); schema.setBizCode("expense"); schema.setPageType(type); schema.setSchemaJson(json); return schema;
    }

    private static LcBizField field(String key, String label, String type, String list, String detail,
                                    String required, Integer order)
    {
        LcBizField field = new LcBizField(); field.setFieldKey(key); field.setFieldLabel(label); field.setFieldType(type);
        field.setIsList(list); field.setIsDetail(detail); field.setRequired(required); field.setOrderNum(order); return field;
    }
}
