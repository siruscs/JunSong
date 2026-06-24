package com.junsong.workflow.controller;

import java.util.List;

import com.junsong.common.core.domain.R;
import com.junsong.workflow.controller.dto.definition.DefinitionSummaryResp;
import com.junsong.workflow.controller.dto.definition.DefinitionXmlResp;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionReq;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionResp;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionReq;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionResp;
import com.junsong.workflow.service.definition.WorkflowDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessDefinitionControllerTest
{
    private WorkflowDefinitionService workflowDefinitionService;
    private ProcessDefinitionController controller;

    @BeforeEach
    void setUp()
    {
        workflowDefinitionService = mock(WorkflowDefinitionService.class);
        controller = new ProcessDefinitionController(workflowDefinitionService);
    }

    @Test
    void listDelegatesToWorkflowDefinitionService()
    {
        when(workflowDefinitionService.list("leave", "请假", true, null))
                .thenReturn(List.of(new DefinitionSummaryResp("def-1", "leave_apply", "请假", 3, false, "dep-1",
                        "leave_apply.bpmn20.xml", null, null)));

        R<List<DefinitionSummaryResp>> response = controller.list("leave", "请假", null, true);

        assertEquals("def-1", response.getData().get(0).definitionId());
        verify(workflowDefinitionService).list("leave", "请假", true, null);
    }

    @Test
    void xmlEndpointReturnsWrappedPayloadInsteadOfRawFlowableStream()
    {
        when(workflowDefinitionService.loadXml("def-1"))
                .thenReturn(new DefinitionXmlResp("def-1", "leave_apply", "请假", 3, "<definitions />"));

        R<DefinitionXmlResp> response = controller.xml("def-1");

        assertEquals("def-1", response.getData().definitionId());
        verify(workflowDefinitionService).loadXml("def-1");
    }

    @Test
    void validateEndpointDelegatesXmlValidationWithoutDeploying()
    {
        ValidateDefinitionReq request = new ValidateDefinitionReq("<definitions />", "leave_apply", "请假");
        when(workflowDefinitionService.validate(request))
                .thenReturn(new ValidateDefinitionResp(true, List.of(), "leave_apply", "请假"));

        R<ValidateDefinitionResp> response = controller.validate(request);

        assertTrue(response.getData().valid());
        verify(workflowDefinitionService).validate(request);
    }

    @Test
    void deployEndpointAcceptsJsonPayloadInsteadOfMultipart()
    {
        DeployDefinitionReq request = new DeployDefinitionReq("<definitions />", "leave_apply", "请假", "请假流程", "leave");
        when(workflowDefinitionService.deploy(request))
                .thenReturn(new DeployDefinitionResp("dep-1", "def-1", "leave_apply", "请假", 5));

        R<DeployDefinitionResp> response = controller.deploy(request);

        assertEquals("dep-1", response.getData().deploymentId());
        verify(workflowDefinitionService).deploy(request);
    }
}
