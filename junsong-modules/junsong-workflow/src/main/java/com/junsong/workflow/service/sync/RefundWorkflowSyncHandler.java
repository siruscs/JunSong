package com.junsong.workflow.service.sync;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.member.api.RemoteRefundApplyService;
import com.junsong.member.api.domain.RefundWorkflowSyncReq;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RefundWorkflowSyncHandler implements WorkflowBusinessSyncHandler
{
    private static final String PROCESS_KEY = "refund_apply";
    private static final BigDecimal FINANCE_THRESHOLD = new BigDecimal("1000");

    private final RemoteRefundApplyService remoteRefundApplyService;

    public RefundWorkflowSyncHandler(RemoteRefundApplyService remoteRefundApplyService)
    {
        this.remoteRefundApplyService = remoteRefundApplyService;
    }

    @Override
    public boolean supports(String processDefinitionId)
    {
        return processDefinitionId != null && processDefinitionId.startsWith(PROCESS_KEY + ":");
    }

    @Override
    public void afterApprove(String currentTaskName, String processInstanceId, String operator, Map<String, Object> variables)
    {
        // 使用 taskKey 而非 taskName 匹配，避免 BPMN 名称变更导致状态回写失败
        // 退款流程的门店负责人审批节点 taskKey 为 "task_store_manager_approve"
        if ("task_store_manager_approve".equals(currentTaskName) || "门店负责人审批".equals(currentTaskName))
        {
            BigDecimal refundAmount = resolveRefundAmount(variables);
            if (refundAmount.compareTo(FINANCE_THRESHOLD) >= 0)
            {
                syncWorkflowStatus(processInstanceId, "PENDING_FINANCE_APPROVAL", "财务复核", operator);
                return;
            }
        }
        syncWorkflowStatus(processInstanceId, "APPROVED", "审批完成", operator);
    }

    @Override
    public void afterReject(String processInstanceId, String operator)
    {
        syncWorkflowStatus(processInstanceId, "REJECTED", "审批驳回", operator);
    }

    private BigDecimal resolveRefundAmount(Map<String, Object> variables)
    {
        if (variables == null || variables.isEmpty())
        {
            return BigDecimal.ZERO;
        }
        Object value = variables.get("refundAmount");
        if (value == null)
        {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private void syncWorkflowStatus(String processInstanceId, String workflowStatus, String currentTaskName, String operator)
    {
        RefundWorkflowSyncReq request = new RefundWorkflowSyncReq();
        request.setProcessInstanceId(processInstanceId);
        request.setWorkflowStatus(workflowStatus);
        request.setCurrentTaskName(currentTaskName);
        request.setOperator(operator);
        remoteRefundApplyService.syncWorkflowStatus(request, SecurityConstants.INNER);
    }
}
