package com.junsong.workflow.lowcode.service;

import com.junsong.workflow.lowcode.domain.LcBizBranchRule;
import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.engine.LcBranchRuleEngine;
import com.junsong.workflow.service.identity.DeptUserResolveService;
import com.junsong.workflow.mapper.WfSysUserMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final WfSysUserMapper sysUserMapper;
    @Autowired
    private LcExpressionService expressionService;

    public LcWorkflowAssembleService(LcMetadataService metadataService,
                                     DeptUserResolveService deptUserResolveService,
                                     WfSysUserMapper sysUserMapper)
    {
        this.metadataService = metadataService;
        this.deptUserResolveService = deptUserResolveService;
        this.sysUserMapper = sysUserMapper;
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

                // 会签/或签：生成人员集合变量 assigneeList_{taskKey}
                String miType = assignee.getMultiInstanceType();
                if (miType != null && !"none".equals(miType) && !miType.isBlank())
                {
                    List<String> assigneeList = resolveAssigneeList(assignee, form, initiator);
                    if (assigneeList != null && !assigneeList.isEmpty())
                    {
                        variables.put("assigneeList_" + assignee.getTaskKey(), assigneeList);
                    }
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
     * 装配子流程（callActivity）输入变量。
     * <p>
     * 在主流程启动前调用：依据业务对象配置的 subProcessKey 判断是否需要调用子流程。
     * 若配置了子流程，则将主流程变量映射为子流程的输入变量。
     * <p>
     * 当前为最小实现：未配置 subProcessKey 时直接返回空 Map 跳过；已配置时默认把
     * 主流程全部变量透传给子流程。
     *
     * @param bizCode   业务编码
     * @param variables 已装配的主流程变量
     * @return 子流程输入变量映射；未配置子流程时返回空 Map
     */
    public Map<String, Object> assembleCallActivity(String bizCode, Map<String, Object> variables)
    {
        Map<String, Object> subProcessVariables = new LinkedHashMap<>();
        if (bizCode == null || bizCode.isBlank())
        {
            return subProcessVariables;
        }
        LcBizObject bizObject = metadataService.selectBizObjectByBizCode(bizCode);
        if (bizObject == null)
        {
            return subProcessVariables;
        }
        String subProcessKey = bizObject.getSubProcessKey();
        if (subProcessKey == null || subProcessKey.isBlank())
        {
            return subProcessVariables;
        }
        // TODO: 实现主流程变量到子流程输入变量的精细化映射。
        //  当前最小实现：将主流程变量整体透传给子流程；后续可基于 subProcessKey
        //  解析子流程输入参数定义，做字段级映射（in/out source 表达式）。
        if (variables != null)
        {
            subProcessVariables.putAll(variables);
        }
        subProcessVariables.put("__subProcessKey", subProcessKey);
        return subProcessVariables;
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
                return resolveFixedUser(value);
            case "FIXED_ROLE":
                return value;
            case "FORM_FIELD_USER":
                return value == null ? null : resolveUserIdentity(asText(form.get(value)), "表单处理人不存在");
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
                String username = resolveUserIdentity(asText(form.get(value)), "表单处理人不存在");
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

    /** Flowable 身份统一使用 username；配置中心保存的数字 userId 在启动前转换。 */
    private String resolveFixedUser(String value)
    {
        return resolveUserIdentity(value, "固定处理人不存在");
    }

    /** Flowable 身份统一使用 username；表单用户字段保存的数字 userId 在启动前转换。 */
    private String resolveUserIdentity(String value, String missingMessagePrefix)
    {
        if (value == null || value.isBlank()) return null;
        String raw = value.trim();
        if (!raw.matches("\\d+")) return raw;
        String username = sysUserMapper.selectUserNameByUserId(Long.valueOf(raw));
        if (username == null || username.isBlank())
        {
            throw new IllegalArgumentException(missingMessagePrefix + ": " + raw);
        }
        return username.trim();
    }

    /**
     * 解析会签人员列表。
     */
    private List<String> resolveAssigneeList(LcBizNodeAssignee assignee, Map<String, Object> form, String initiator)
    {
        String source = assignee.getAssigneeSource();
        String value = assignee.getAssigneeValue();
        List<String> result = new ArrayList<>();
        if (source == null) return result;

        switch (source)
        {
            case "FIXED_USER":
                if (value != null && !value.isBlank())
                {
                    for (String u : value.split(",")) result.add(u.trim());
                }
                break;
            case "FIXED_ROLE":
            case "ROLE":
                if (value != null)
                {
                    List<String> users = deptUserResolveService.getUsersByRoleCode(value);
                    if (users != null) result.addAll(users);
                }
                break;
            case "DEPT":
                if (value != null)
                {
                    List<String> users = deptUserResolveService.getUsersByDeptId(value);
                    if (users != null) result.addAll(users);
                }
                break;
            case "FORM_FIELD_USER":
                if (value != null)
                {
                    String username = resolveUserIdentity(asText(form.get(value)), "表单处理人不存在");
                    if (username != null && !username.isBlank()) result.add(username);
                }
                break;
            case "INITIATOR":
                if (initiator != null) result.add(initiator);
                break;
            case "INITIATOR_LEADER":
                if (initiator != null)
                {
                    String leader = deptUserResolveService.getDirectLeader(initiator);
                    if (leader != null) result.add(leader);
                }
                break;
            case "DEPT_LEADER":
                if (initiator != null)
                {
                    String leader = deptUserResolveService.getDeptLeader(initiator);
                    if (leader != null) result.add(leader);
                }
                break;
            case "EXPRESSION":
                String expr = assignee.getAssigneeExpr();
                if (expr != null && !expr.isBlank())
                {
                    Map<String, Object> vars = new LinkedHashMap<>();
                    vars.put("initiator", initiator);
                    if (form != null) vars.putAll(form);
                    Object evalResult = expressionService.evaluateSpEL(expr, vars);
                    if (evalResult instanceof String && !((String) evalResult).isBlank())
                    {
                        result.add((String) evalResult);
                    }
                    else if (evalResult instanceof Iterable)
                    {
                        @SuppressWarnings("unchecked")
                        Iterable<String> iterable = (Iterable<String>) evalResult;
                        iterable.forEach(result::add);
                    }
                }
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * 将对象安全地转为字符串。
     */
    private static String asText(Object value)
    {
        return value == null ? null : value.toString();
    }

}
