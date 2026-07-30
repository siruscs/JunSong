package com.junsong.workflow.lowcode.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 将页面 Schema 与字段元数据装配成前端唯一消费模型。 */
@Component
public class LcRuntimePageAssembler
{
    private final LcPageSchemaValidator schemaValidator;

    public LcRuntimePageAssembler(LcPageSchemaValidator schemaValidator)
    {
        this.schemaValidator = schemaValidator;
    }

    public RuntimePage assemble(LcBizPageSchema schema, List<LcBizField> fields)
    {
        schemaValidator.validate(schema, fields);
        Map<String, LcBizField> byKey = (fields == null ? List.<LcBizField>of() : fields).stream()
                .collect(Collectors.toMap(LcBizField::getFieldKey, Function.identity(), (left, right) -> left));
        JSONObject root = JSON.parseObject(schema.getSchemaJson());
        List<String> configured = root.containsKey("fields") ? schemaValidator.extractFieldKeys(root.get("fields")) : null;
        List<LcBizField> ordered = new ArrayList<>();
        if (configured != null)
        {
            for (String key : configured) ordered.add(byKey.get(key));
        }
        else
        {
            ordered.addAll(byKey.values());
            ordered.sort(Comparator.comparing(LcBizField::getOrderNum,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        List<RuntimeField> runtimeFields = ordered.stream()
                .map(field -> {
                    LcFieldRuntimePolicy.Policy policy = LcFieldRuntimePolicy.resolve(field, schema.getPageType());
                    return new RuntimeField(field.getFieldKey(), field.getFieldLabel(), field.getFieldType(),
                            field.getComponentType(), policy.visible(), policy.editable(), policy.required(),
                            field.getDefaultValue(), field.getDictType(), field.getFieldExt());
                }).toList();
        return new RuntimePage(schema.getBizCode(), schema.getPageType().toUpperCase(), runtimeFields, root);
    }

    public record RuntimePage(String bizCode, String pageType, List<RuntimeField> fields, JSONObject schema) { }

    public record RuntimeField(String fieldKey, String fieldLabel, String fieldType, String componentType,
                               boolean visible, boolean editable, boolean required, String defaultValue,
                               String dictType, String fieldExt) { }
}
