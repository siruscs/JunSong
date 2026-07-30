package com.junsong.workflow.lowcode.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 页面 Schema 发布前校验，防止运行时页面与业务字段元数据脱节。 */
@Component
public class LcPageSchemaValidator
{
    private static final Set<String> PAGE_TYPES = Set.of("FORM", "LIST", "DETAIL");

    public void validate(LcBizPageSchema schema, List<LcBizField> fields)
    {
        if (schema == null || blank(schema.getBizCode())) throw invalid("页面 Schema 业务对象不能为空");
        if (schema.getPageType() == null || !PAGE_TYPES.contains(schema.getPageType().toUpperCase()))
        {
            throw invalid("不支持的页面类型: " + schema.getPageType());
        }
        if (blank(schema.getSchemaJson())) throw invalid("页面 Schema 内容不能为空");

        JSONObject root;
        try { root = JSON.parseObject(schema.getSchemaJson()); }
        catch (JSONException ex) { throw invalid("页面 Schema 不是合法 JSON"); }
        if (root == null || root.isEmpty()) throw invalid("页面 Schema 不能为空对象");

        Set<String> knownFields = new HashSet<>();
        if (fields != null) for (LcBizField field : fields) knownFields.add(field.getFieldKey());
        if (root.containsKey("fields"))
        {
            List<String> schemaFields = extractFieldKeys(root.get("fields"));
            Set<String> unique = new HashSet<>();
            for (String key : schemaFields)
            {
                if (blank(key) || !unique.add(key)) throw invalid("Schema 字段为空或重复: " + key);
                if (!knownFields.contains(key))
                {
                    throw invalid("Schema 引用了不存在的字段: " + key);
                }
            }
        }
    }

    public List<String> extractFieldKeys(Object fieldsNode)
    {
        if (!(fieldsNode instanceof JSONArray schemaFields))
        {
            throw invalid("fields 必须是字符串数组或字段对象数组");
        }
        List<String> keys = new ArrayList<>();
        for (Object item : schemaFields)
        {
            if (item instanceof String key)
            {
                keys.add(key);
            }
            else if (item instanceof JSONObject object)
            {
                keys.add(object.getString("fieldKey"));
            }
            else
            {
                throw invalid("fields 只能包含字段Key字符串或包含 fieldKey 的对象");
            }
        }
        return keys;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
