package com.junsong.workflow.lowcode.metadata;

import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizPageSchema;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LcMetadataSchemaValidatorTest
{
    private final LcMetadataSchemaValidator validator = new LcMetadataSchemaValidator();

    @Test
    void acceptsNativeWorkflowObjectWithValidFields()
    {
        LcBizObject object = object("stocktake", "盘点", "NATIVE", "finance_stocktake", "id", "1", "stocktake_apply");
        LcBizField field = field("stocktake", "takeNo", "盘点单号", "STRING");
        assertDoesNotThrow(() -> validator.validate(config(object, List.of(field))));
    }

    @Test
    void acceptsFrontendNumericAndDisplayFieldTypes()
    {
        LcBizObject object = object("expense", "费用", "GENERIC", null, null, "0", null);
        assertDoesNotThrow(() -> validator.validate(config(object, List.of(
                field("expense", "amount", "金额", "number"),
                field("expense", "remark", "备注", "textarea"),
                field("expense", "tags", "标签", "multi-select")))));
    }

    @Test
    void rejectsWorkflowWithoutProcessKey()
    {
        LcBizObject object = object("expense", "费用", "GENERIC", null, null, "1", null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config(object, List.of())));
    }

    @Test
    void rejectsDuplicateAndUnsafeFieldKeys()
    {
        LcBizObject object = object("leave", "请假", "GENERIC", null, null, "0", null);
        LcBizField first = field("leave", "reason", "原因", "STRING");
        LcBizField duplicate = field("leave", "reason", "重复", "STRING");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config(object, List.of(first, duplicate))));

        LcBizField unsafe = field("leave", "reason;drop", "原因", "STRING");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config(object, List.of(unsafe))));
    }

    @Test
    void rejectsInvalidWorkflowNodeConfiguration()
    {
        LcBizObject object = object("stocktake", "盘点", "GENERIC", null, null, "1", "stocktake_apply");
        LcBizNodeAssignee node = new LcBizNodeAssignee();
        node.setBizCode("stocktake"); node.setTaskKey("Task_Count"); node.setTaskName("盘点");
        node.setAssigneeSource("VARIABLE"); node.setProcessVarName("bad-name");
        LcBizConfigDTO config = config(object, List.of()); config.setNodeAssignees(List.of(node));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));

        node.setProcessVarName("counterUser");
        node.setAssigneeSource(null);
        node.setAssigneeExpr(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }

    @Test
    void rejectsUnsafeProcessVariableName()
    {
        LcBizObject object = object("expense", "费用", "GENERIC", null, null, "1", "expense_apply");
        LcBizField field = field("expense", "amount", "金额", "DECIMAL");
        field.setIsProcessVar("1");
        field.setProcessVarName("amount.total");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config(object, List.of(field))));
    }

    @Test
    void rejectsFormFieldUserAssigneeWithoutExistingUserField()
    {
        LcBizObject object = object("stocktake", "盘点", "NATIVE", "finance_stocktake", "stocktake_id", "1", "stocktake_apply");
        LcBizField counter = field("stocktake", "counter_user_id", "盘点人", "sys-ref");
        LcBizNodeAssignee node = node("stocktake", "Task_Approve", "库存审批", "FORM_FIELD_USER", "approver_user_id", "approverUsername");
        LcBizConfigDTO config = config(object, List.of(counter));
        config.setNodeAssignees(List.of(node));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }

    @Test
    void acceptsFormFieldUserAssigneeWhenFieldIsUserSelector()
    {
        LcBizObject object = object("stocktake", "盘点", "NATIVE", "finance_stocktake", "stocktake_id", "1", "stocktake_apply");
        LcBizField counter = field("stocktake", "counter_user_id", "盘点人", "sys-ref");
        counter.setComponentType("user-select");
        LcBizNodeAssignee node = node("stocktake", "Task_Count", "盘点人录入", "FORM_FIELD_USER", "counter_user_id", "counterUsername");
        LcBizConfigDTO config = config(object, List.of(counter));
        config.setNodeAssignees(List.of(node));

        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    void rejectsBranchRuleWithoutExistingFieldAndSafeTargetVariable()
    {
        LcBizObject object = object("stocktake", "盘点", "NATIVE", "finance_stocktake", "stocktake_id", "1", "stocktake_apply");
        LcBizField recount = field("stocktake", "need_recount", "是否复盘", "BOOLEAN");
        LcBizConfigDTO config = config(object, List.of(recount));

        LcBizBranchRule missingField = branchRule("stocktake", "Gateway_NeedRecount", "missing_field", "EQ", "true", "needRecount");
        config.setBranchRules(List.of(missingField));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));

        LcBizBranchRule unsafeTarget = branchRule("stocktake", "Gateway_NeedRecount", "need_recount", "EQ", "true", "need-recount");
        config.setBranchRules(List.of(unsafeTarget));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    }

    @Test
    void rejectsPageSchemaReferencingUnknownFieldBeforeSave()
    {
        LcBizObject object = object("stocktake", "盘点", "NATIVE", "finance_stocktake", "id", "1", "stocktake_apply");
        LcBizConfigDTO config = config(object, List.of(field("stocktake", "take_no", "盘点单号", "STRING")));
        LcBizPageSchema detail = new LcBizPageSchema();
        detail.setBizCode("stocktake");
        detail.setPageType("DETAIL");
        detail.setSchemaJson("{\"fields\":[{\"fieldKey\":\"missing_field\",\"span\":12}]}");
        config.setPageSchemas(List.of(detail));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
        assertTrue(error.getMessage().contains("DETAIL"));
        assertTrue(error.getMessage().contains("missing_field"));
    }

    @Test
    void acceptsPageSchemaWithLayoutObjectsAndKnownFields()
    {
        LcBizObject object = object("stocktake", "盘点", "NATIVE", "finance_stocktake", "id", "1", "stocktake_apply");
        LcBizConfigDTO config = config(object, List.of(
                field("stocktake", "take_no", "盘点单号", "STRING"),
                field("stocktake", "store_id", "盘点门店", "DEPT")));
        LcBizPageSchema detail = new LcBizPageSchema();
        detail.setBizCode("stocktake");
        detail.setPageType("DETAIL");
        detail.setSchemaJson("{\"fields\":[{\"fieldKey\":\"take_no\",\"span\":12},{\"fieldKey\":\"store_id\",\"span\":12}]}");
        config.setPageSchemas(List.of(detail));

        assertDoesNotThrow(() -> validator.validate(config));
    }

    private static LcBizConfigDTO config(LcBizObject object, List<LcBizField> fields)
    {
        LcBizConfigDTO config = new LcBizConfigDTO();
        config.setBizObject(object);
        config.setFields(fields);
        return config;
    }

    private static LcBizObject object(String code, String name, String storage, String table, String pk,
                                      String workflow, String processKey)
    {
        LcBizObject object = new LcBizObject();
        object.setBizCode(code); object.setBizName(name); object.setStorageMode(storage);
        object.setTableName(table); object.setPkField(pk); object.setWorkflowEnabled(workflow);
        object.setProcessKey(processKey);
        return object;
    }

    private static LcBizField field(String bizCode, String key, String label, String type)
    {
        LcBizField field = new LcBizField();
        field.setBizCode(bizCode); field.setFieldKey(key); field.setFieldLabel(label); field.setFieldType(type);
        return field;
    }

    private static LcBizNodeAssignee node(String bizCode, String taskKey, String taskName, String source,
                                          String value, String processVarName)
    {
        LcBizNodeAssignee node = new LcBizNodeAssignee();
        node.setBizCode(bizCode);
        node.setTaskKey(taskKey);
        node.setTaskName(taskName);
        node.setAssigneeSource(source);
        node.setAssigneeValue(value);
        node.setProcessVarName(processVarName);
        return node;
    }

    private static LcBizBranchRule branchRule(String bizCode, String gatewayKey, String fieldKey, String operator,
                                              String compareValue, String targetVarName)
    {
        LcBizBranchRule rule = new LcBizBranchRule();
        rule.setBizCode(bizCode);
        rule.setGatewayKey(gatewayKey);
        rule.setFieldKey(fieldKey);
        rule.setOperator(operator);
        rule.setCompareValue(compareValue);
        rule.setTargetVarName(targetVarName);
        return rule;
    }
}
