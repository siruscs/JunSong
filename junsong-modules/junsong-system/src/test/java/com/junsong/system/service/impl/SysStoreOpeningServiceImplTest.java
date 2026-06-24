package com.junsong.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.junsong.system.domain.SysStoreOpening;
import com.junsong.system.mapper.SysStoreOpeningMapper;
import com.junsong.common.core.workflow.WorkflowSubmitSnapshot;
import com.junsong.common.core.workflow.WorkflowSyncSnapshot;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SysStoreOpeningServiceImplTest
{
    @Test
    void createsDraftFormWithGeneratedOrderNo() throws Exception
    {
        SysStoreOpeningMapper storeOpeningMapper = Mockito.mock(SysStoreOpeningMapper.class);
        when(storeOpeningMapper.insertStoreOpening(any(SysStoreOpening.class))).thenReturn(1);
        SysStoreOpeningServiceImpl service = new SysStoreOpeningServiceImpl();
        Field mapperField = SysStoreOpeningServiceImpl.class.getDeclaredField("storeOpeningMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, storeOpeningMapper);
        SysStoreOpening form = new SysStoreOpening();
        form.setStoreName("松颐门店");

        int result = service.insertStoreOpening(form);

        ArgumentCaptor<SysStoreOpening> captor = ArgumentCaptor.forClass(SysStoreOpening.class);
        verify(storeOpeningMapper).insertStoreOpening(captor.capture());
        assertEquals(1, result);
        assertEquals(SysStoreOpening.STATUS_DRAFT, captor.getValue().getWorkflowStatus());
        assertTrue(captor.getValue().getOrderNo().startsWith("SOA-"));
    }

    @Test
    void submitForApprovalMovesDraftToPendingRegionApproval() throws Exception
    {
        SysStoreOpeningMapper storeOpeningMapper = Mockito.mock(SysStoreOpeningMapper.class);
        when(storeOpeningMapper.updateWorkflowSnapshot(any(SysStoreOpening.class))).thenReturn(1);
        SysStoreOpeningServiceImpl service = new SysStoreOpeningServiceImpl();
        Field mapperField = SysStoreOpeningServiceImpl.class.getDeclaredField("storeOpeningMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, storeOpeningMapper);

        int result = service.markSubmitted(1L, "pi-1", "store_opening_apply", "区域负责人审批", "admin");

        ArgumentCaptor<SysStoreOpening> captor = ArgumentCaptor.forClass(SysStoreOpening.class);
        verify(storeOpeningMapper).updateWorkflowSnapshot(captor.capture());
        assertEquals(1, result);
        assertEquals(SysStoreOpening.STATUS_PENDING_REGION_APPROVAL, captor.getValue().getWorkflowStatus());
        assertEquals("pi-1", captor.getValue().getProcessInstanceId());
    }

    @Test
    void submitSnapshotUsesUnifiedWorkflowSnapshotObject() throws Exception
    {
        SysStoreOpeningMapper storeOpeningMapper = Mockito.mock(SysStoreOpeningMapper.class);
        when(storeOpeningMapper.updateWorkflowSnapshot(any(SysStoreOpening.class))).thenReturn(1);
        SysStoreOpeningServiceImpl service = new SysStoreOpeningServiceImpl();
        Field mapperField = SysStoreOpeningServiceImpl.class.getDeclaredField("storeOpeningMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, storeOpeningMapper);

        WorkflowSubmitSnapshot snapshot = new WorkflowSubmitSnapshot();
        snapshot.setId(1L);
        snapshot.setProcessInstanceId("pi-2");
        snapshot.setProcessDefinitionKey("store_opening_apply");
        snapshot.setCurrentTaskName("区域负责人审批");
        snapshot.setOperator("admin");

        int result = service.markSubmitted(snapshot);

        ArgumentCaptor<SysStoreOpening> captor = ArgumentCaptor.forClass(SysStoreOpening.class);
        verify(storeOpeningMapper).updateWorkflowSnapshot(captor.capture());
        assertEquals(1, result);
        assertEquals(SysStoreOpening.STATUS_PENDING_REGION_APPROVAL, captor.getValue().getWorkflowStatus());
        assertEquals("admin", captor.getValue().getSubmitter());
    }

    @Test
    void syncSnapshotUsesUnifiedWorkflowSyncObject() throws Exception
    {
        SysStoreOpeningMapper storeOpeningMapper = Mockito.mock(SysStoreOpeningMapper.class);
        when(storeOpeningMapper.updateWorkflowSnapshot(any(SysStoreOpening.class))).thenReturn(1);
        SysStoreOpeningServiceImpl service = new SysStoreOpeningServiceImpl();
        Field mapperField = SysStoreOpeningServiceImpl.class.getDeclaredField("storeOpeningMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, storeOpeningMapper);

        WorkflowSyncSnapshot snapshot = new WorkflowSyncSnapshot();
        snapshot.setProcessInstanceId("pi-3");
        snapshot.setWorkflowStatus(SysStoreOpening.STATUS_APPROVED);
        snapshot.setCurrentTaskName("审批完成");
        snapshot.setOperator("admin");

        int result = service.syncWorkflowStatus(snapshot);

        ArgumentCaptor<SysStoreOpening> captor = ArgumentCaptor.forClass(SysStoreOpening.class);
        verify(storeOpeningMapper).updateWorkflowSnapshot(captor.capture());
        assertEquals(1, result);
        assertEquals("pi-3", captor.getValue().getProcessInstanceId());
        assertEquals(SysStoreOpening.STATUS_APPROVED, captor.getValue().getWorkflowStatus());
    }
}
