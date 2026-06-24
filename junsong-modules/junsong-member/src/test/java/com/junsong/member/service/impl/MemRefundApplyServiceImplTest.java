package com.junsong.member.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.junsong.common.core.workflow.WorkflowSubmitSnapshot;
import com.junsong.common.core.workflow.WorkflowSyncSnapshot;
import com.junsong.member.domain.MemRefundApply;
import com.junsong.member.mapper.MemRefundApplyMapper;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MemRefundApplyServiceImplTest
{
    @Test
    void createsDraftRefundWithGeneratedRefundNo() throws Exception
    {
        MemRefundApplyMapper refundApplyMapper = Mockito.mock(MemRefundApplyMapper.class);
        when(refundApplyMapper.insertMemRefundApply(any(MemRefundApply.class))).thenReturn(1);
        MemRefundApplyServiceImpl service = new MemRefundApplyServiceImpl();
        Field mapperField = MemRefundApplyServiceImpl.class.getDeclaredField("refundApplyMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, refundApplyMapper);

        MemRefundApply form = new MemRefundApply();
        form.setMemberName("张三");
        form.setRefundAmount(new BigDecimal("88.00"));

        int result = service.insertMemRefundApply(form);

        ArgumentCaptor<MemRefundApply> captor = ArgumentCaptor.forClass(MemRefundApply.class);
        verify(refundApplyMapper).insertMemRefundApply(captor.capture());
        assertEquals(1, result);
        assertEquals(MemRefundApply.STATUS_DRAFT, captor.getValue().getWorkflowStatus());
        assertTrue(captor.getValue().getRefundNo().startsWith("MRA-"));
    }

    @Test
    void submitSnapshotMovesRefundToPendingStoreApproval() throws Exception
    {
        MemRefundApplyMapper refundApplyMapper = Mockito.mock(MemRefundApplyMapper.class);
        when(refundApplyMapper.updateWorkflowSnapshot(any(MemRefundApply.class))).thenReturn(1);
        MemRefundApplyServiceImpl service = new MemRefundApplyServiceImpl();
        Field mapperField = MemRefundApplyServiceImpl.class.getDeclaredField("refundApplyMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, refundApplyMapper);

        WorkflowSubmitSnapshot snapshot = new WorkflowSubmitSnapshot();
        snapshot.setId(1L);
        snapshot.setProcessInstanceId("pi-r-1");
        snapshot.setProcessDefinitionKey("refund_apply");
        snapshot.setCurrentTaskName("门店负责人审批");
        snapshot.setOperator("admin");

        int result = service.markSubmitted(snapshot);

        ArgumentCaptor<MemRefundApply> captor = ArgumentCaptor.forClass(MemRefundApply.class);
        verify(refundApplyMapper).updateWorkflowSnapshot(captor.capture());
        assertEquals(1, result);
        assertEquals(MemRefundApply.STATUS_PENDING_STORE_APPROVAL, captor.getValue().getWorkflowStatus());
    }

    @Test
    void syncSnapshotUpdatesWorkflowStatus() throws Exception
    {
        MemRefundApplyMapper refundApplyMapper = Mockito.mock(MemRefundApplyMapper.class);
        when(refundApplyMapper.updateWorkflowSnapshot(any(MemRefundApply.class))).thenReturn(1);
        MemRefundApplyServiceImpl service = new MemRefundApplyServiceImpl();
        Field mapperField = MemRefundApplyServiceImpl.class.getDeclaredField("refundApplyMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, refundApplyMapper);

        WorkflowSyncSnapshot snapshot = new WorkflowSyncSnapshot();
        snapshot.setProcessInstanceId("pi-r-2");
        snapshot.setWorkflowStatus(MemRefundApply.STATUS_APPROVED);
        snapshot.setCurrentTaskName("审批完成");
        snapshot.setOperator("admin");

        int result = service.syncWorkflowStatus(snapshot);

        ArgumentCaptor<MemRefundApply> captor = ArgumentCaptor.forClass(MemRefundApply.class);
        verify(refundApplyMapper).updateWorkflowSnapshot(captor.capture());
        assertEquals(1, result);
        assertEquals("pi-r-2", captor.getValue().getProcessInstanceId());
        assertEquals(MemRefundApply.STATUS_APPROVED, captor.getValue().getWorkflowStatus());
    }
}
