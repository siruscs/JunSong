package com.junsong.workflow.service.definition;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.workflow.controller.dto.definition.DefinitionSummaryResp;
import com.junsong.workflow.controller.dto.definition.DefinitionXmlResp;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionReq;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionResp;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionReq;
import com.junsong.workflow.lowcode.service.BpmnTimerAssembleService;
import com.junsong.workflow.lowcode.service.BpmnMultiInstanceAssembleService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowDefinitionServiceTest
{
    private static final String VALID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         targetNamespace="Examples">
              <process id="leave_apply" name="请假">
                <startEvent id="start" />
                <sequenceFlow id="flow1" sourceRef="start" targetRef="end" />
                <endEvent id="end" />
              </process>
            </definitions>
            """;

    private RepositoryService repositoryService;
    private ProcessEngine processEngine;
    private BpmnTimerAssembleService bpmnTimerAssembleService;
    private BpmnMultiInstanceAssembleService bpmnMultiInstanceAssembleService;
    private WorkflowDefinitionService service;

    @BeforeEach
    void setUp()
    {
        repositoryService = mock(RepositoryService.class);
        processEngine = mock(ProcessEngine.class);
        bpmnTimerAssembleService = mock(BpmnTimerAssembleService.class);
        bpmnMultiInstanceAssembleService = mock(BpmnMultiInstanceAssembleService.class);
        service = new WorkflowDefinitionService(repositoryService, processEngine, bpmnTimerAssembleService,
                bpmnMultiInstanceAssembleService);
    }

    @Test
    void validateRejectsBlankXml()
    {
        ValidateDefinitionReq request = new ValidateDefinitionReq("   ", "leave_apply", "请假");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validate(request));

        assertEquals("BPMN XML 不能为空", exception.getMessage());
    }

    @Test
    void validateRejectsDirectVariableAccessInGatewayCondition()
    {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             targetNamespace="Examples">
                  <process id="stocktake_apply" name="库存盘点申请">
                    <startEvent id="start" />
                    <sequenceFlow id="flow1" sourceRef="start" targetRef="gateway" />
                    <exclusiveGateway id="gateway" />
                    <sequenceFlow id="flow2" sourceRef="gateway" targetRef="end">
                      <conditionExpression xsi:type="tFormalExpression"><![CDATA[${needRecount == true}]]></conditionExpression>
                    </sequenceFlow>
                    <endEvent id="end" />
                  </process>
                </definitions>
                """;

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validate(new ValidateDefinitionReq(xml, "stocktake_apply", "库存盘点申请")));

        assertEquals("网关条件表达式禁止直接读取变量: needRecount，请使用 execution.getVariable('needRecount')", exception.getMessage());
    }

    @Test
    void loadXmlReadsDefinitionResourceAndReturnsMetadata()
    {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionId("def-1")).thenReturn(query);
        when(query.processDefinitionKey("leave_apply")).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(definition);
        when(definition.getId()).thenReturn("def-1");
        when(definition.getKey()).thenReturn("leave_apply");
        when(definition.getName()).thenReturn("请假");
        when(definition.getVersion()).thenReturn(3);
        when(definition.getDeploymentId()).thenReturn("dep-1");
        when(definition.getResourceName()).thenReturn("leave.bpmn20.xml");
        when(repositoryService.getResourceAsStream("dep-1", "leave.bpmn20.xml"))
                .thenReturn(new ByteArrayInputStream("<definitions />".getBytes(StandardCharsets.UTF_8)));

        DefinitionXmlResp response = service.loadXml("def-1");

        assertEquals("def-1", response.definitionId());
        assertEquals("<definitions />", response.xml());
    }

    @Test
    void deployValidXmlCreatesDeploymentAfterValidation()
    {
        DeploymentBuilder deploymentBuilder = mock(DeploymentBuilder.class);
        Deployment deployment = mock(Deployment.class);
        ProcessDefinitionQuery definitionQuery = mock(ProcessDefinitionQuery.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(repositoryService.createDeployment()).thenReturn(deploymentBuilder);
        when(deploymentBuilder.key("leave_apply")).thenReturn(deploymentBuilder);
        when(deploymentBuilder.name("请假流程")).thenReturn(deploymentBuilder);
        when(deploymentBuilder.category("")).thenReturn(deploymentBuilder);
        when(deploymentBuilder.addString("leave_apply.bpmn20.xml", VALID_XML)).thenReturn(deploymentBuilder);
        when(deploymentBuilder.deploy()).thenReturn(deployment);
        when(deployment.getId()).thenReturn("dep-1");
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(definitionQuery);
        when(definitionQuery.deploymentId("dep-1")).thenReturn(definitionQuery);
        when(definitionQuery.processDefinitionKey("leave_apply")).thenReturn(definitionQuery);
        when(definitionQuery.latestVersion()).thenReturn(definitionQuery);
        when(definitionQuery.singleResult()).thenReturn(definition);
        when(definition.getId()).thenReturn("def-1");
        when(definition.getKey()).thenReturn("leave_apply");
        when(definition.getName()).thenReturn("请假");
        when(definition.getVersion()).thenReturn(5);
        // BPMN 定时器装配 mock（返回原 XML，不做修改）
        when(bpmnTimerAssembleService.assembleTimers(VALID_XML, "leave_apply")).thenReturn(VALID_XML);
        when(bpmnMultiInstanceAssembleService.assembleMultiInstance(VALID_XML, "leave_apply")).thenReturn(VALID_XML);

        DeployDefinitionResp response = service.deploy(new DeployDefinitionReq(VALID_XML, "leave_apply", "请假", "请假流程", null));

        assertEquals("dep-1", response.deploymentId());
        verify(deploymentBuilder).deploy();
    }

    @Test
    void listMapsDefinitionSummary()
    {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        DeploymentQuery deploymentQuery = mock(DeploymentQuery.class);
        Deployment deployment = mock(Deployment.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey("leave")).thenReturn(query);
        when(query.processDefinitionNameLike("%请假%")).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.orderByProcessDefinitionKey()).thenReturn(query);
        when(query.asc()).thenReturn(query);
        when(query.orderByProcessDefinitionVersion()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(definition));
        when(definition.getId()).thenReturn("def-1");
        when(definition.getKey()).thenReturn("leave_apply");
        when(definition.getName()).thenReturn("请假");
        when(definition.getVersion()).thenReturn(2);
        when(definition.isSuspended()).thenReturn(false);
        when(definition.getDeploymentId()).thenReturn("dep-1");
        when(definition.getResourceName()).thenReturn("leave.bpmn20.xml");
        when(definition.getCategory()).thenReturn(null);
        when(repositoryService.createDeploymentQuery()).thenReturn(deploymentQuery);
        when(deploymentQuery.deploymentId("dep-1")).thenReturn(deploymentQuery);
        when(deploymentQuery.singleResult()).thenReturn(deployment);
        when(deployment.getDeploymentTime()).thenReturn(new Date(1000L));

        List<DefinitionSummaryResp> response = service.list("leave", "请假", true, null);

        assertEquals("def-1", response.get(0).definitionId());
        assertEquals("leave_apply", response.get(0).processKey());
    }

    @Test
    void deleteDefinitionRejectsDeploymentWithMultipleDefinitions()
    {
        ProcessDefinitionQuery requireQuery = mock(ProcessDefinitionQuery.class);
        ProcessDefinitionQuery countQuery = mock(ProcessDefinitionQuery.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(requireQuery, countQuery);
        when(requireQuery.processDefinitionId("def-1")).thenReturn(requireQuery);
        when(requireQuery.singleResult()).thenReturn(definition);
        when(definition.getDeploymentId()).thenReturn("dep-1");
        when(countQuery.deploymentId("dep-1")).thenReturn(countQuery);
        when(countQuery.count()).thenReturn(2L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.deleteDefinition("def-1", false));

        assertEquals("该部署包含 2 个流程定义，禁止按部署删除；请先拆分部署后再删除单个流程", exception.getMessage());
        verify(repositoryService, never()).deleteDeployment("dep-1", false);
    }
}
