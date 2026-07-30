package com.junsong.workflow.service.sync;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.finance.api.RemoteStocktakeService;
import com.junsong.finance.api.domain.StocktakeWorkflowSyncReq;
import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.mapper.LcBizInstanceMapper;
import com.junsong.workflow.lowcode.service.LcBizService;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 盘点工作流同步处理器。
 *
 * 工作流模块在审批/驳回/提交后回调盘点服务，更新 process_instance_id 与 current_node。
 * 状态机仍保留在 FinStocktakeServiceImpl，工作流仅用于待办/追踪。
 */
@Component
@Order(0)
public class StocktakeWorkflowSyncHandler implements WorkflowBusinessSyncHandler
{
    private static final String PROCESS_KEY = "stocktake_apply";

    private final RemoteStocktakeService remoteStocktakeService;
    private final LcBizInstanceMapper lcBizInstanceMapper;
    private final LcBizService lcBizService;

    public StocktakeWorkflowSyncHandler(RemoteStocktakeService remoteStocktakeService,
                                        LcBizInstanceMapper lcBizInstanceMapper,
                                        LcBizService lcBizService)
    {
        this.remoteStocktakeService = remoteStocktakeService;
        this.lcBizInstanceMapper = lcBizInstanceMapper;
        this.lcBizService = lcBizService;
    }

    @Override
    public boolean supports(String processDefinitionId)
    {
        return PROCESS_KEY.equals(processDefinitionId)
                || (processDefinitionId != null && processDefinitionId.startsWith(PROCESS_KEY + ":"));
    }

    @Override
    public int priority()
    {
        return 100;
    }

    @Override
    public void afterApprove(String currentTaskName, String processInstanceId, String operator, Map<String, Object> variables)
    {
        syncWorkflowStatus(resolveStocktakeId(variables), processInstanceId, currentTaskName, "APPROVE", operator);
    }

    @Override
    public void afterReject(String processInstanceId, String operator)
    {
        syncWorkflowStatus(null, processInstanceId, "审批驳回", "REJECT", operator);
    }

    @Override
    public void afterSubmit(String processInstanceId, String operator)
    {
        syncWorkflowStatus(null, processInstanceId, "已提交", "SUBMIT", operator);
    }

    @Override
    public void afterComplete(String processInstanceId, String operator, Map<String, Object> variables)
    {
        // 盘点流程排除了通用低代码处理器，终审必须由专用处理器同时落低代码快照。
        lcBizService.syncStatus(processInstanceId, "APPROVED", "审批完成", operator);
        return;
        /*
        StocktakeWorkflowSyncReq request = new StocktakeWorkflowSyncReq();
        request.setProcessInstanceId(processInstanceId);
        Map<String, Object> flowVariables = variables == null ? Map.of() : variables;
        String businessKey = firstText(flowVariables, "businessKey", "takeNo");
        Map<String, Object> formData = mapValue(flowVariables.get("formData"));

        // 低代码发起的流程把业务单号和表单保存在 lc_biz_instance，
        // Flowable 变量只保存流程控制变量，不能作为业务单据数据源。
        LcBizInstance instance = lcBizInstanceMapper.selectByProcessInstanceId(processInstanceId);
        if (instance != null) {
            if (businessKey.isBlank()) businessKey = instance.getOrderNo();
            if (formData.isEmpty() && instance.getFormDataMap() != null) formData = instance.getFormDataMap();
        }
        request.setBusinessKey(businessKey);
        request.setFormData(formData);
        request.setCurrentNode("审批完成");
        request.setAction("COMPLETE");
        remoteStocktakeService.syncWorkflowStatus(request, SecurityConstants.INNER);
        */
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value)
    {
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        return (Map<String, Object>) map;
    }

    private static String firstText(Map<String, Object> variables, String... keys)
    {
        for (String key : keys) {
            Object value = variables.get(key);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
        }
        return "";
    }

    private Long resolveStocktakeId(Map<String, Object> variables)
    {
        if (variables == null || variables.isEmpty())
        {
            return null;
        }
        Object value = variables.get("stocktakeId");
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private void syncWorkflowStatus(Long stocktakeId, String processInstanceId, String currentNode, String action, String operator)
    {
        if (stocktakeId == null && "APPROVE".equalsIgnoreCase(action) && "审批人审批".equals(currentNode)) {
            LcBizInstance instance = lcBizInstanceMapper.selectByProcessInstanceId(processInstanceId);
            if (instance != null) {
                StocktakeWorkflowSyncReq complete = new StocktakeWorkflowSyncReq();
                complete.setProcessInstanceId(processInstanceId);
                complete.setBusinessKey(instance.getOrderNo());
                complete.setFormData(instance.getFormDataMap());
                complete.setCurrentNode("审批完成");
                complete.setAction("COMPLETE");
                remoteStocktakeService.syncWorkflowStatus(complete, SecurityConstants.INNER,
                        workflowIdempotencyKey(processInstanceId, "COMPLETE", "审批完成"));
                return;
            }
        }
        StocktakeWorkflowSyncReq request = new StocktakeWorkflowSyncReq();
        request.setStocktakeId(stocktakeId);
        request.setProcessInstanceId(processInstanceId);
        request.setCurrentNode(currentNode);
        request.setAction(action);
        remoteStocktakeService.syncWorkflowStatus(request, SecurityConstants.INNER, workflowIdempotencyKey(processInstanceId, action, currentNode));
    }

    private static String workflowIdempotencyKey(String processInstanceId, String action, String currentNode)
    {
        if ("COMPLETE".equalsIgnoreCase(action)) return "workflow:stocktake:" + processInstanceId + ":complete";
        return "workflow:stocktake:" + processInstanceId + ":" + action + ":" + currentNode;
    }
}
