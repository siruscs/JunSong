package com.junsong.workflow.service.sync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.member.api.RemoteRefundApplyService;
import com.junsong.member.api.domain.RefundWorkflowSyncReq;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RefundWorkflowSyncHandlerTest
{
    @Test
    void supportsRefundProcessDefinition()
    {
        RefundWorkflowSyncHandler handler = new RefundWorkflowSyncHandler(mock(RemoteRefundApplyService.class));

        assertTrue(handler.supports("refund_apply:1:abc"));
        assertFalse(handler.supports("store_opening_apply:1:abc"));
    }

    @Test
    void afterApproveOnStoreApproverMarksApprovedForSmallAmount()
    {
        RemoteRefundApplyService remoteRefundApplyService = mock(RemoteRefundApplyService.class);
        RefundWorkflowSyncHandler handler = new RefundWorkflowSyncHandler(remoteRefundApplyService);

        handler.afterApprove("门店负责人审批", "pi-r-1", "admin", Map.of("refundAmount", new BigDecimal("88.00")));

        verify(remoteRefundApplyService).syncWorkflowStatus(argThat(req ->
                        req instanceof RefundWorkflowSyncReq
                                && "pi-r-1".equals(((RefundWorkflowSyncReq) req).getProcessInstanceId())
                                && "APPROVED".equals(((RefundWorkflowSyncReq) req).getWorkflowStatus())
                                && "审批完成".equals(((RefundWorkflowSyncReq) req).getCurrentTaskName())),
                eq(SecurityConstants.INNER));
    }

    @Test
    void afterApproveOnStoreApproverMovesToFinanceForLargeAmount()
    {
        RemoteRefundApplyService remoteRefundApplyService = mock(RemoteRefundApplyService.class);
        RefundWorkflowSyncHandler handler = new RefundWorkflowSyncHandler(remoteRefundApplyService);

        handler.afterApprove("门店负责人审批", "pi-r-2", "admin", Map.of("refundAmount", new BigDecimal("1888.00")));

        verify(remoteRefundApplyService).syncWorkflowStatus(argThat(req ->
                        req instanceof RefundWorkflowSyncReq
                                && "pi-r-2".equals(((RefundWorkflowSyncReq) req).getProcessInstanceId())
                                && "PENDING_FINANCE_APPROVAL".equals(((RefundWorkflowSyncReq) req).getWorkflowStatus())
                                && "财务复核".equals(((RefundWorkflowSyncReq) req).getCurrentTaskName())),
                eq(SecurityConstants.INNER));
    }

    @Test
    void afterApproveOnFinanceMarksApproved()
    {
        RemoteRefundApplyService remoteRefundApplyService = mock(RemoteRefundApplyService.class);
        RefundWorkflowSyncHandler handler = new RefundWorkflowSyncHandler(remoteRefundApplyService);

        handler.afterApprove("财务复核", "pi-r-3", "admin", Map.of("refundAmount", new BigDecimal("1888.00")));

        verify(remoteRefundApplyService).syncWorkflowStatus(argThat(req ->
                        req instanceof RefundWorkflowSyncReq
                                && "APPROVED".equals(((RefundWorkflowSyncReq) req).getWorkflowStatus())
                                && "审批完成".equals(((RefundWorkflowSyncReq) req).getCurrentTaskName())),
                eq(SecurityConstants.INNER));
    }

    @Test
    void afterRejectMarksRejected()
    {
        RemoteRefundApplyService remoteRefundApplyService = mock(RemoteRefundApplyService.class);
        RefundWorkflowSyncHandler handler = new RefundWorkflowSyncHandler(remoteRefundApplyService);

        handler.afterReject("pi-r-4", "admin");

        verify(remoteRefundApplyService).syncWorkflowStatus(argThat(req ->
                        req instanceof RefundWorkflowSyncReq
                                && "REJECTED".equals(((RefundWorkflowSyncReq) req).getWorkflowStatus())
                                && "审批驳回".equals(((RefundWorkflowSyncReq) req).getCurrentTaskName())),
                eq(SecurityConstants.INNER));
    }
}
