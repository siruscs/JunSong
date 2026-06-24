package com.junsong.workflow.service.sync;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.system.api.RemoteStoreOpeningService;
import com.junsong.system.api.domain.StoreOpeningWorkflowSyncReq;
import org.springframework.stereotype.Component;

@Component
public class StoreOpeningWorkflowSyncHandler implements WorkflowBusinessSyncHandler
{
    private static final String PROCESS_KEY = "store_opening_apply";

    private final RemoteStoreOpeningService remoteStoreOpeningService;

    public StoreOpeningWorkflowSyncHandler(RemoteStoreOpeningService remoteStoreOpeningService)
    {
        this.remoteStoreOpeningService = remoteStoreOpeningService;
    }

    @Override
    public boolean supports(String processDefinitionId)
    {
        return processDefinitionId != null && processDefinitionId.startsWith(PROCESS_KEY + ":");
    }

    @Override
    public void afterApprove(String currentTaskName, String processInstanceId, String operator)
    {
        // 使用 taskKey 而非 taskName 匹配，避免 BPMN 名称变更导致状态回写失败
        // 门店开业流程的区域负责人审批节点 taskKey 为 "task_region_director_approve"
        if ("task_region_director_approve".equals(currentTaskName) || "区域负责人审批".equals(currentTaskName))
        {
            syncWorkflowStatus(processInstanceId, "PENDING_GM_APPROVAL", "总经理审批", operator);
            return;
        }
        syncWorkflowStatus(processInstanceId, "APPROVED", "审批完成", operator);
    }

    @Override
    public void afterReject(String processInstanceId, String operator)
    {
        syncWorkflowStatus(processInstanceId, "REJECTED", "审批驳回", operator);
    }

    private void syncWorkflowStatus(String processInstanceId, String workflowStatus, String currentTaskName, String operator)
    {
        StoreOpeningWorkflowSyncReq request = new StoreOpeningWorkflowSyncReq();
        request.setProcessInstanceId(processInstanceId);
        request.setWorkflowStatus(workflowStatus);
        request.setCurrentTaskName(currentTaskName);
        request.setOperator(operator);
        remoteStoreOpeningService.syncWorkflowStatus(request, SecurityConstants.INNER);
    }
}
