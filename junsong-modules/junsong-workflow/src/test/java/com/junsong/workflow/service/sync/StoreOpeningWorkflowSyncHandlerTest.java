package com.junsong.workflow.service.sync;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.system.api.RemoteStoreOpeningService;
import com.junsong.system.api.domain.StoreOpeningWorkflowSyncReq;
import org.junit.jupiter.api.Test;

class StoreOpeningWorkflowSyncHandlerTest
{
    @Test
    void supportsStoreOpeningProcessDefinition()
    {
        StoreOpeningWorkflowSyncHandler handler = new StoreOpeningWorkflowSyncHandler(mock(RemoteStoreOpeningService.class));

        org.junit.jupiter.api.Assertions.assertTrue(handler.supports("store_opening_apply:1:abc"));
        org.junit.jupiter.api.Assertions.assertFalse(handler.supports("refund_apply:1:abc"));
    }

    @Test
    void afterApproveOnRegionLeaderMovesToGeneralManager()
    {
        RemoteStoreOpeningService remoteStoreOpeningService = mock(RemoteStoreOpeningService.class);
        StoreOpeningWorkflowSyncHandler handler = new StoreOpeningWorkflowSyncHandler(remoteStoreOpeningService);

        handler.afterApprove("区域负责人审批", "pi-1", "admin");

        verify(remoteStoreOpeningService).syncWorkflowStatus(argThat(req ->
                        req instanceof StoreOpeningWorkflowSyncReq
                                && "pi-1".equals(((StoreOpeningWorkflowSyncReq) req).getProcessInstanceId())
                                && "PENDING_GM_APPROVAL".equals(((StoreOpeningWorkflowSyncReq) req).getWorkflowStatus())
                                && "总经理审批".equals(((StoreOpeningWorkflowSyncReq) req).getCurrentTaskName())
                                && "admin".equals(((StoreOpeningWorkflowSyncReq) req).getOperator())),
                eq(SecurityConstants.INNER));
    }

    @Test
    void afterApproveOnFinalNodeMarksApproved()
    {
        RemoteStoreOpeningService remoteStoreOpeningService = mock(RemoteStoreOpeningService.class);
        StoreOpeningWorkflowSyncHandler handler = new StoreOpeningWorkflowSyncHandler(remoteStoreOpeningService);

        handler.afterApprove("总经理审批", "pi-1", "admin");

        verify(remoteStoreOpeningService).syncWorkflowStatus(argThat(req ->
                        req instanceof StoreOpeningWorkflowSyncReq
                                && "APPROVED".equals(((StoreOpeningWorkflowSyncReq) req).getWorkflowStatus())
                                && "审批完成".equals(((StoreOpeningWorkflowSyncReq) req).getCurrentTaskName())),
                eq(SecurityConstants.INNER));
    }

    @Test
    void afterRejectMarksRejected()
    {
        RemoteStoreOpeningService remoteStoreOpeningService = mock(RemoteStoreOpeningService.class);
        StoreOpeningWorkflowSyncHandler handler = new StoreOpeningWorkflowSyncHandler(remoteStoreOpeningService);

        handler.afterReject("pi-1", "admin");

        verify(remoteStoreOpeningService).syncWorkflowStatus(argThat(req ->
                        req instanceof StoreOpeningWorkflowSyncReq
                                && "REJECTED".equals(((StoreOpeningWorkflowSyncReq) req).getWorkflowStatus())
                                && "审批驳回".equals(((StoreOpeningWorkflowSyncReq) req).getCurrentTaskName())),
                eq(SecurityConstants.INNER));
    }
}
