package com.junsong.workflow.lowcode.runtime;

import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LcPageSchemaValidatorTest
{
    private final LcPageSchemaValidator validator = new LcPageSchemaValidator();

    @Test
    void acceptsSchemaReferencingKnownFields()
    {
        assertDoesNotThrow(() -> validator.validate(schema("FORM", "{\"fields\":[\"takeNo\"]}"),
                List.of(field("takeNo"))));
    }

    @Test
    void acceptsObjectFieldReferencesWithLayoutAttributes()
    {
        assertDoesNotThrow(() -> validator.validate(
                schema("FORM", "{\"layout\":\"grid\",\"fields\":[{\"fieldKey\":\"takeNo\",\"span\":12}]}"),
                List.of(field("takeNo"))));
    }

    @Test
    void rejectsMalformedJsonAndUnsupportedPageType()
    {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(schema("FORM", "{bad"), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(schema("KANBAN", "{}"), List.of()));
    }

    @Test
    void rejectsUnknownOrDuplicateSchemaFields()
    {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(schema("LIST", "{\"fields\":[\"missing\"]}"), List.of(field("takeNo"))));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(schema("LIST", "{\"fields\":[\"takeNo\",\"takeNo\"]}"),
                        List.of(field("takeNo"))));
    }

    @Test
    void rejectsFieldReferenceWhenFieldMetadataIsEmpty()
    {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(schema("FORM", "{\"fields\":[\"takeNo\"]}"), List.of()));
    }

    private static LcBizPageSchema schema(String pageType, String json)
    {
        LcBizPageSchema schema = new LcBizPageSchema();
        schema.setBizCode("stocktake"); schema.setPageType(pageType); schema.setSchemaJson(json);
        return schema;
    }

    private static LcBizField field(String key)
    {
        LcBizField field = new LcBizField(); field.setFieldKey(key); return field;
    }
}
