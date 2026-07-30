package com.junsong.workflow.lowcode.metadata;

import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import com.junsong.workflow.lowcode.runtime.LcPageSchemaValidator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 发布前的低代码元数据静态校验器。
 * 只校验平台契约，不读取业务表，也不允许配置直接拼接危险标识符。
 */
@Component
public class LcMetadataSchemaValidator
{
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,63}");
    private static final Set<String> SUPPORTED_FIELD_TYPES = Set.of(
            "STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATE", "DATETIME", "USER", "DEPT", "DICT",
            "TEXTAREA", "NUMBER", "PERCENT", "DATE-RANGE", "TIME", "TIME-RANGE", "SELECT",
            "MULTI-SELECT", "SYS-REF", "COMPUTED", "REGION", "ADDRESS", "GEO", "FILE", "IMAGE",
            "RICHTEXT", "SUBFORM", "DIVIDER", "TITLE", "TEXT", "MONEY");

    @Autowired
    private LcPageSchemaValidator pageSchemaValidator = new LcPageSchemaValidator();

    public void validate(LcBizConfigDTO config)
    {
        if (config == null || config.getBizObject() == null)
        {
            throw invalid("业务对象配置不能为空");
        }
        LcBizObject object = config.getBizObject();
        requireIdentifier(object.getBizCode(), "bizCode");
        requireText(object.getBizName(), "bizName");
        if ("NATIVE".equalsIgnoreCase(object.getStorageMode()))
        {
            requireIdentifier(object.getTableName(), "tableName");
            requireIdentifier(object.getPkField(), "pkField");
        }
        if (isEnabled(object.getWorkflowEnabled()) && isBlank(object.getProcessKey()))
        {
            throw invalid("启用工作流时 processKey 不能为空");
        }
        validateFields(object.getBizCode(), config.getFields());
        validatePageSchemas(object.getBizCode(), config.getFields(), config.getPageSchemas());
        validateAssignees(object.getBizCode(), object, config.getFields(), config.getNodeAssignees());
        validateBranchRules(object.getBizCode(), object, config.getFields(), config.getBranchRules());
    }

    private void validatePageSchemas(String bizCode, List<LcBizField> fields, List<LcBizPageSchema> schemas)
    {
        if (schemas == null) return;
        for (LcBizPageSchema schema : schemas)
        {
            if (schema == null) throw invalid("页面 Schema 不能为空");
            if (!isBlank(schema.getBizCode()) && !bizCode.equals(schema.getBizCode()))
            {
                throw invalid("页面 Schema bizCode 与业务对象不一致: " + schema.getPageType());
            }
            try
            {
                pageSchemaValidator.validate(schema, fields);
            }
            catch (IllegalArgumentException ex)
            {
                throw invalid("页面 " + schema.getPageType() + " Schema 校验失败: " + ex.getMessage());
            }
        }
    }

    private void validateAssignees(String bizCode, LcBizObject object, List<LcBizField> fields, List<LcBizNodeAssignee> assignees)
    {
        if (!isEnabled(object.getWorkflowEnabled()) || assignees == null) return;
        Set<String> taskKeys = new HashSet<>();
        Map<String, LcBizField> fieldMap = fields == null ? Map.of() : fields.stream()
                .collect(Collectors.toMap(LcBizField::getFieldKey, field -> field, (first, second) -> first));
        for (LcBizNodeAssignee node : assignees)
        {
            requireIdentifier(node.getTaskKey(), "taskKey");
            requireText(node.getTaskName(), "taskName");
            if (!taskKeys.add(node.getTaskKey())) throw invalid("流程节点 taskKey 重复: " + node.getTaskKey());
            if (!bizCode.equals(node.getBizCode()) && !isBlank(node.getBizCode()))
            {
                throw invalid("流程节点 bizCode 与业务对象不一致: " + node.getTaskKey());
            }
            if (isBlank(node.getAssigneeSource()) && isBlank(node.getAssigneeExpr()))
            {
                throw invalid("流程节点必须配置处理人来源或表达式: " + node.getTaskKey());
            }
            if ("VARIABLE".equalsIgnoreCase(node.getAssigneeSource()))
            {
                requireIdentifier(node.getProcessVarName(), "processVarName");
            }
            if ("FORM_FIELD_USER".equalsIgnoreCase(node.getAssigneeSource()))
            {
                requireIdentifier(node.getProcessVarName(), "processVarName");
                requireText(node.getAssigneeValue(), "assigneeValue");
                LcBizField field = fieldMap.get(node.getAssigneeValue());
                if (field == null)
                {
                    throw invalid("FORM_FIELD_USER 指向的字段不存在: " + node.getTaskKey() + "/" + node.getAssigneeValue());
                }
                if (!isUserField(field))
                {
                    throw invalid("FORM_FIELD_USER 必须指向用户选择字段: " + node.getTaskKey() + "/" + node.getAssigneeValue());
                }
            }
        }
    }

    private static boolean isUserField(LcBizField field)
    {
        String fieldType = field.getFieldType() == null ? "" : field.getFieldType();
        String componentType = field.getComponentType() == null ? "" : field.getComponentType();
        String fieldExt = field.getFieldExt() == null ? "" : field.getFieldExt();
        return "USER".equalsIgnoreCase(fieldType)
                || "user-select".equalsIgnoreCase(componentType)
                || fieldExt.contains("\"source\":\"user\"");
    }

    private void validateBranchRules(String bizCode, LcBizObject object, List<LcBizField> fields, List<LcBizBranchRule> branchRules)
    {
        if (!isEnabled(object.getWorkflowEnabled()) || branchRules == null) return;
        Map<String, LcBizField> fieldMap = fields == null ? Map.of() : fields.stream()
                .collect(Collectors.toMap(LcBizField::getFieldKey, field -> field, (first, second) -> first));
        for (LcBizBranchRule rule : branchRules)
        {
            if (!bizCode.equals(rule.getBizCode()) && !isBlank(rule.getBizCode()))
            {
                throw invalid("分支规则 bizCode 与业务对象不一致: " + rule.getGatewayKey());
            }
            requireIdentifier(rule.getGatewayKey(), "gatewayKey");
            requireIdentifier(rule.getFieldKey(), "fieldKey");
            requireText(rule.getOperator(), "operator");
            requireIdentifier(rule.getTargetVarName(), "targetVarName");
            if (!fieldMap.containsKey(rule.getFieldKey()))
            {
                throw invalid("分支规则引用的字段不存在: " + rule.getGatewayKey() + "/" + rule.getFieldKey());
            }
        }
    }

    private void validateFields(String bizCode, List<LcBizField> fields)
    {
        Set<String> keys = new HashSet<>();
        if (fields == null) return;
        for (LcBizField field : fields)
        {
            requireIdentifier(field.getFieldKey(), "fieldKey");
            requireText(field.getFieldLabel(), "fieldLabel");
            requireText(field.getFieldType(), "fieldType");
            if (!SUPPORTED_FIELD_TYPES.contains(field.getFieldType().toUpperCase()))
            {
                throw invalid("不支持的字段类型: " + field.getFieldType());
            }
            if (!keys.add(field.getFieldKey()))
            {
                throw invalid("字段 key 重复: " + field.getFieldKey());
            }
            if (!bizCode.equals(field.getBizCode()) && !isBlank(field.getBizCode()))
            {
                throw invalid("字段 bizCode 与业务对象不一致: " + field.getFieldKey());
            }
            if ("DICT".equalsIgnoreCase(field.getFieldType()) && isBlank(field.getDictType()))
            {
                throw invalid("DICT 字段必须配置 dictType: " + field.getFieldKey());
            }
            if (isEnabled(field.getIsProcessVar()))
            {
                String processVarName = isBlank(field.getProcessVarName()) ? field.getFieldKey() : field.getProcessVarName();
                requireIdentifier(processVarName, "processVarName");
            }
        }
    }

    private static void requireIdentifier(String value, String name)
    {
        if (isBlank(value) || !SAFE_IDENTIFIER.matcher(value).matches())
        {
            throw invalid(name + " 只能使用字母、数字、下划线且必须以字母开头");
        }
    }

    private static void requireText(String value, String name)
    {
        if (isBlank(value)) throw invalid(name + " 不能为空");
    }

    private static boolean isEnabled(String value) { return "1".equals(value) || "Y".equalsIgnoreCase(value); }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
