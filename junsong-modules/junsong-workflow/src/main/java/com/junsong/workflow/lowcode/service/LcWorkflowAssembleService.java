package com.junsong.workflow.lowcode.service;

import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.engine.LcBranchRuleEngine;
import com.junsong.workflow.service.identity.DeptUserResolveService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 低代码流程变量装配服务。
 * 依据字段元数据 / 节点处理人来源 / 分支规则，把表单数据装配为流程变量。
 */
@Service
public class LcWorkflowAssembleService
{
    private final LcMetadataService metadataService;
    private final DeptUserResolveService deptUserResolveService;
    @Autowired
    private LcExpressionService expressionService;

    public LcWorkflowAssembleService(LcMetadataService metadataService,
                                     DeptUserResolveService deptUserResolveService)
    {
        this.metadataService = metadataService;
        this.deptUserResolveService = deptUserResolveService;
    }

    public Map<String, Object> assembleVariables(String bizCode, Map<String, Object> formData)
    {
        return assembleVariables(bizCode, formData, null);
    }

    /**
     * 装配流程变量。
     *
     * @param bizCode   业务编码
     * @param formData  表单数据
     * @param initiator 发起人 username（用于解析发起人主管/部门负责人等动态处理人；可为 null）
     */
    public Map<String, Object> assembleVariables(String bizCode, Map<String, Object> formData, String initiator)
    {
        Map<String, Object> variables = new LinkedHashMap<>();
        Map<String, Object> form = formData == null ? Map.of() : formData;

        // 1. 流程变量字段：is_process_var = '1'
        List<LcBizField> fields = metadataService.selectFieldsByBizCode(bizCode);
        if (fields != null)
        {
            for (LcBizField field : fields)
            {
                if ("1".equals(field.getIsProcessVar()))
                {
                    String varName = (field.getProcessVarName() != null && !field.getProcessVarName().isBlank())
                            ? field.getProcessVarName() : field.getFieldKey();
                    variables.put(varName, form.get(field.getFieldKey()));
                }
            }
        }

        // 2. 节点处理人来源解析
        List<LcBizNodeAssignee> assignees = metadataService.selectNodeAssigneesByBizCode(bizCode);
        if (assignees != null)
        {
            for (LcBizNodeAssignee assignee : assignees)
            {
                String varName = (assignee.getProcessVarName() != null && !assignee.getProcessVarName().isBlank())
                        ? assignee.getProcessVarName() : assignee.getTaskKey();
                Object value = resolveAssignee(assignee, form, initiator);
                if (value != null)
                {
                    variables.put(varName, value);
                }
            }
        }

        // 3. 分支规则编译：把字段值与比较值按操作符判定为布尔结果，写入 target_var_name
        List<LcBizBranchRule> rules = metadataService.selectBranchRulesByBizCode(bizCode);
        if (rules != null)
        {
            for (LcBizBranchRule rule : rules)
            {
                // 参与判定的原始字段也保证进入变量上下文，供网关表达式直接引用
                if (rule.getFieldKey() != null && !variables.containsKey(rule.getFieldKey()))
                {
                    variables.put(rule.getFieldKey(), form.get(rule.getFieldKey()));
                }
                String targetVar = rule.getTargetVarName();
                if (targetVar != null && !targetVar.isBlank())
                {
                    boolean matched = LcBranchRuleEngine.evaluate(rule, form);
                    variables.put(targetVar, matched);
                }
            }
        }
        return variables;
    }

    /**
     * 处理人来源解析。
     * - FIXED_USER / FIXED_ROLE：固定值
     * - FORM_FIELD_USER：取表单字段值（assignee_value 为 field_key）
     * - INITIATOR：发起人本人
     * - INITIATOR_LEADER：发起人的直属上级
     * - DEPT_LEADER：发起人所在部门负责人
     * - FORM_FIELD_DEPT_LEADER：取表单字段（assignee_value）指向的用户，再解析其部门负责人
     * - ROLE：角色编码 → 解析为该角色下的用户列表
     * - DEPT：部门ID → 解析为该部门下的用户列表
     * - EXPRESSION：SpEL 表达式 → 求值结果为用户ID或列表
     */
    private Object resolveAssignee(LcBizNodeAssignee assignee, Map<String, Object> form, String initiator)
    {
        String source = assignee.getAssigneeSource();
        String value = assignee.getAssigneeValue();
        if (source == null)
        {
            return null;
        }
        switch (source)
        {
            case "FIXED_USER":
            case "FIXED_ROLE":
                return value;
            case "FORM_FIELD_USER":
                return value == null ? null : asText(form.get(value));
            case "INITIATOR":
                return initiator;
            case "INITIATOR_LEADER":
                return initiator == null ? null : deptUserResolveService.getDirectLeader(initiator);
            case "DEPT_LEADER":
                return initiator == null ? null : deptUserResolveService.getDeptLeader(initiator);
            case "FORM_FIELD_DEPT_LEADER":
            {
                if (value == null)
                {
                    return null;
                }
                String username = asText(form.get(value));
                return username == null ? null : deptUserResolveService.getDeptLeader(username);
            }
            case "ROLE":
            {
                // assignee_value = 角色编码，解析为该角色下的全部用户（Candidate Users）
                if (value == null) return null;
                List<String> users = deptUserResolveService.getUsersByRoleCode(value);
                return users == null || users.isEmpty() ? null : users;
            }
            case "DEPT":
            {
                // assignee_value = 部门ID，解析为该部门下的全部用户（Candidate Users）
                if (value == null) return null;
                List<String> users = deptUserResolveService.getUsersByDeptId(value);
                return users == null || users.isEmpty() ? null : users;
            }
            case "EXPRESSION":
            {
                // assignee_expr = SpEL 表达式，如 "#amount > 1000 ? 'admin' : 'user_zhang'"
                String expr = assignee.getAssigneeExpr();
                if (expr == null || expr.isBlank()) return null;
                Map<String, Object> vars = new LinkedHashMap<>();
                vars.put("initiator", initiator);
                // 将表单字段全部注入上下文
                if (form != null) vars.putAll(form);
                Object result = expressionService.evaluateSpEL(expr, vars);
                if (result instanceof String) return result;
                if (result instanceof Iterable)
                {
                    @SuppressWarnings("unchecked")
                    Iterable<String> iterable = (Iterable<String>) result;
                    List<String> list = new ArrayList<>();
                    iterable.forEach(list::add);
                    return list;
                }
                return null;
            }
            default:
                return null;
        }
    }

}
