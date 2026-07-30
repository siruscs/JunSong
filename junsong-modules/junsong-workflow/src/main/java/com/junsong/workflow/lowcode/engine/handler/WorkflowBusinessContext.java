package com.junsong.workflow.lowcode.engine.handler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用业务流程上下文。平台层只传递标准元数据和流程变量，
 * 不直接依赖库存、费用、人事等领域对象。
 */
public final class WorkflowBusinessContext
{
    private final String businessType;
    private final String businessId;
    private final String businessNo;
    private final Long tenantId;
    private final Long deptId;
    private final String processInstanceId;
    private final String idempotencyKey;
    private final Map<String, Object> variables;

    public WorkflowBusinessContext(String businessType,
                                   String businessId,
                                   String businessNo,
                                   Long tenantId,
                                   Long deptId,
                                   String processInstanceId,
                                   String idempotencyKey,
                                   Map<String, Object> variables)
    {
        this.businessType = requireText(businessType, "businessType");
        this.businessId = requireText(businessId, "businessId");
        this.businessNo = requireText(businessNo, "businessNo");
        this.tenantId = requirePositive(tenantId, "tenantId");
        this.deptId = deptId;
        this.processInstanceId = processInstanceId;
        this.idempotencyKey = idempotencyKey;
        this.variables = variables == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    public String getBusinessType() { return businessType; }
    public String getBusinessId() { return businessId; }
    public String getBusinessNo() { return businessNo; }
    public Long getTenantId() { return tenantId; }
    public Long getDeptId() { return deptId; }
    public String getProcessInstanceId() { return processInstanceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Map<String, Object> getVariables() { return variables; }

    private static String requireText(String value, String name)
    {
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }

    private static Long requirePositive(Long value, String name)
    {
        if (value == null || value <= 0)
        {
            throw new IllegalArgumentException(name + " 必须为正数");
        }
        return value;
    }
}
