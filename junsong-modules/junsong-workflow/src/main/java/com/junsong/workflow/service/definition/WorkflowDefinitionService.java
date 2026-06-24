package com.junsong.workflow.service.definition;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.workflow.controller.dto.definition.DefinitionSummaryResp;
import com.junsong.workflow.controller.dto.definition.DefinitionXmlResp;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionReq;
import com.junsong.workflow.controller.dto.definition.DeployDefinitionResp;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionReq;
import com.junsong.workflow.controller.dto.definition.ValidateDefinitionResp;
import com.junsong.workflow.lowcode.service.BpmnTimerAssembleService;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.stereotype.Service;

@Service
public class WorkflowDefinitionService
{
    private final RepositoryService repositoryService;
    private final BpmnTimerAssembleService timerAssembleService;

    public WorkflowDefinitionService(RepositoryService repositoryService,
                                     BpmnTimerAssembleService timerAssembleService)
    {
        this.repositoryService = repositoryService;
        this.timerAssembleService = timerAssembleService;
    }

    public List<DefinitionSummaryResp> list(String key, String keyword, Boolean latestOnly, String category)
    {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        if (key != null && !key.isBlank())
        {
            query.processDefinitionKey(key);
        }
        if (keyword != null && !keyword.isBlank())
        {
            query.processDefinitionNameLike("%" + keyword + "%");
        }
        if (category != null && !category.isBlank())
        {
            query.processDefinitionCategory(category);
        }
        if (Boolean.TRUE.equals(latestOnly))
        {
            query.latestVersion();
        }
        return query.orderByProcessDefinitionKey()
                .asc()
                .orderByProcessDefinitionVersion()
                .desc()
                .list()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询所有已部署流程的分类列表（去重）
     */
    public List<String> listCategories()
    {
        return repositoryService.createProcessDefinitionQuery()
                .list()
                .stream()
                .map(ProcessDefinition::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public DefinitionSummaryResp detail(String definitionId)
    {
        return toSummary(requireDefinition(definitionId));
    }

    public DefinitionXmlResp loadXml(String definitionId)
    {
        ProcessDefinition definition = requireDefinition(definitionId);
        try (InputStream inputStream = repositoryService.getResourceAsStream(
                definition.getDeploymentId(), definition.getResourceName()))
        {
            if (inputStream == null)
            {
                throw new ServiceException("流程定义XML不存在: " + definitionId);
            }
            byte[] bytes = inputStream.readAllBytes();
            String xml = new String(bytes, StandardCharsets.UTF_8);
            if (!xml.contains("bpmndi:BPMNDiagram") && !xml.contains("<BPMNDiagram"))
            {
                ProcessDefinition latestDefinition = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(definition.getKey())
                        .latestVersion()
                        .singleResult();
                if (latestDefinition != null && !latestDefinition.getId().equals(definitionId))
                {
                    try (InputStream latestStream = repositoryService.getResourceAsStream(
                            latestDefinition.getDeploymentId(), latestDefinition.getResourceName()))
                    {
                        if (latestStream != null)
                        {
                            String latestXml = new String(latestStream.readAllBytes(), StandardCharsets.UTF_8);
                            if (latestXml.contains("bpmndi:BPMNDiagram") || latestXml.contains("<BPMNDiagram"))
                            {
                                xml = latestXml;
                            }
                        }
                    }
                }
            }
            return new DefinitionXmlResp(
                    definition.getId(),
                    definition.getKey(),
                    definition.getName(),
                    definition.getVersion(),
                    xml);
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            throw new ServiceException("读取流程定义XML失败: " + definitionId + ", " + exception.getMessage());
        }
    }

    public ValidateDefinitionResp validate(ValidateDefinitionReq request)
    {
        org.flowable.bpmn.model.Process process = validateProcess(request == null ? null : request.xml());
        return new ValidateDefinitionResp(true, List.of(), process.getId(), process.getName());
    }

    public DeployDefinitionResp deploy(DeployDefinitionReq request)
    {
        if (request == null)
        {
            throw new ServiceException("BPMN XML 不能为空");
        }
        org.flowable.bpmn.model.Process process = validateProcess(request.xml());
        String resourceName = process.getId() + ".bpmn20.xml";
        // 装配节点定时器（边界定时器），无配置则返回原 XML
        String deployXml = timerAssembleService.assembleTimers(request.xml(), process.getId());
        Deployment deployment = repositoryService.createDeployment()
                .key(process.getId())
                .name(resolveDeploymentName(request, process))
                .category(request != null && request.category() != null ? request.category() : "")
                .addString(resourceName, deployXml)
                .deploy();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .processDefinitionKey(process.getId())
                .latestVersion()
                .singleResult();
        if (definition == null)
        {
            throw new ServiceException("部署成功但未找到流程定义: " + deployment.getId());
        }
        return new DeployDefinitionResp(
                deployment.getId(),
                definition.getId(),
                definition.getKey(),
                definition.getName(),
                definition.getVersion());
    }

    public void suspend(String definitionId)
    {
        requireDefinition(definitionId);
        repositoryService.suspendProcessDefinitionById(definitionId, true, null);
    }

    public void activate(String definitionId)
    {
        requireDefinition(definitionId);
        repositoryService.activateProcessDefinitionById(definitionId, true, null);
    }

    public void deleteDeployment(String deploymentId, Boolean cascade)
    {
        if (deploymentId == null || deploymentId.isBlank())
        {
            throw new ServiceException("部署不存在: " + deploymentId);
        }
        repositoryService.deleteDeployment(deploymentId, Boolean.TRUE.equals(cascade));
    }

    private ProcessDefinition requireDefinition(String definitionId)
    {
        if (definitionId == null || definitionId.isBlank())
        {
            throw new ServiceException("流程定义不存在: " + definitionId);
        }
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId)
                .singleResult();
        if (definition == null)
        {
            throw new ServiceException("流程定义不存在: " + definitionId);
        }
        return definition;
    }

    private DefinitionSummaryResp toSummary(ProcessDefinition definition)
    {
        Deployment deployment = repositoryService.createDeploymentQuery()
                .deploymentId(definition.getDeploymentId())
                .singleResult();
        return new DefinitionSummaryResp(
                definition.getId(),
                definition.getKey(),
                definition.getName(),
                definition.getVersion(),
                definition.isSuspended(),
                definition.getDeploymentId(),
                definition.getResourceName(),
                deployment == null ? null : deployment.getDeploymentTime(),
                definition.getCategory());
    }

    private org.flowable.bpmn.model.Process validateProcess(String xml)
    {
        BpmnModel model = parseRequiredModel(xml);
        org.flowable.bpmn.model.Process process = model.getMainProcess();
        if (process == null || process.getId() == null || process.getId().isBlank())
        {
            throw new ServiceException("流程定义缺少 process key");
        }
        if (process.getName() == null || process.getName().isBlank())
        {
            throw new ServiceException("流程定义缺少 process name");
        }
        boolean hasStart = process.getFlowElements().stream().anyMatch(StartEvent.class::isInstance);
        boolean hasEnd = process.getFlowElements().stream().anyMatch(EndEvent.class::isInstance);
        if (!hasStart)
        {
            throw new ServiceException("流程定义缺少开始节点");
        }
        if (!hasEnd)
        {
            throw new ServiceException("流程定义缺少结束节点");
        }
        return process;
    }

    private BpmnModel parseRequiredModel(String xml)
    {
        if (xml == null || xml.isBlank())
        {
            throw new ServiceException("BPMN XML 不能为空");
        }
        try
        {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
            BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(reader);
            if (model.getProcesses().isEmpty())
            {
                throw new ServiceException("流程定义缺少 process");
            }
            return model;
        }
        catch (XMLStreamException | ServiceException exception)
        {
            throw exception instanceof ServiceException serviceException
                    ? serviceException
                    : new ServiceException("BPMN XML 解析失败");
        }
        catch (Exception exception)
        {
            throw new ServiceException("BPMN XML 解析失败");
        }
    }

    private String resolveDeploymentName(
            DeployDefinitionReq request,
            org.flowable.bpmn.model.Process process)
    {
        if (request.deploymentName() != null && !request.deploymentName().isBlank())
        {
            return request.deploymentName();
        }
        return process.getName();
    }
}
