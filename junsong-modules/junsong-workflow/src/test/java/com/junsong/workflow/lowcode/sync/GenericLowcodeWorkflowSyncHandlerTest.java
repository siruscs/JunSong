package com.junsong.workflow.lowcode.sync;

import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.service.LcBizService;
import com.junsong.workflow.lowcode.service.LcInstanceService;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import com.junsong.workflow.mapper.WfSysUserMapper;
import java.util.List;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenericLowcodeWorkflowSyncHandlerTest
{
    @Test
    void supportsNativeWorkflowEnabledLowcodeObject()
    {
        LcMetadataService metadataService = mock(LcMetadataService.class);
        LcBizObject stocktake = new LcBizObject();
        stocktake.setBizCode("stocktake");
        stocktake.setStorageMode("NATIVE");
        stocktake.setWorkflowEnabled("1");
        stocktake.setProcessKey("stocktake_apply");
        when(metadataService.selectGenericWorkflowObjects()).thenReturn(List.of(stocktake));

        GenericLowcodeWorkflowSyncHandler handler = new GenericLowcodeWorkflowSyncHandler(
                metadataService,
                mock(LcBizService.class),
                mock(LcInstanceService.class),
                mock(RuntimeService.class),
                mock(TaskService.class),
                mock(WfSysUserMapper.class));

        assertTrue(handler.supports("stocktake_apply:3:deployment"));
        assertFalse(handler.supports("unknown_apply:1:deployment"));
    }
}
