package com.junsong.system.service;

import com.junsong.system.domain.SysStoreOpening;
import com.junsong.common.core.workflow.WorkflowSubmitSnapshot;
import com.junsong.common.core.workflow.WorkflowSyncSnapshot;
import java.util.List;

public interface ISysStoreOpeningService
{
    List<SysStoreOpening> selectStoreOpeningList(SysStoreOpening query);

    SysStoreOpening selectStoreOpeningById(Long id);

    SysStoreOpening selectStoreOpeningByOrderNo(String orderNo);

    int insertStoreOpening(SysStoreOpening storeOpening);

    int updateStoreOpening(SysStoreOpening storeOpening);

    int deleteStoreOpeningById(Long id);

    int markSubmitted(Long id, String processInstanceId, String processDefinitionKey, String currentTaskName, String username);

    int markSubmitted(WorkflowSubmitSnapshot snapshot);

    int withdraw(Long id, String username);

    int syncWorkflowStatus(String processInstanceId, String workflowStatus, String currentTaskName, String username);

    int syncWorkflowStatus(WorkflowSyncSnapshot snapshot);
}
