package com.junsong.workflow.lowcode.service;

import java.io.StringReader;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizObject;

/**
 * BPMN 会签/或签装配服务。
 *
 * <p>在流程部署前，读取 {@code lc_biz_node_assignee} 配置，
 * 为配置了多人审批的 userTask 动态注入 {@code multiInstanceLoopCharacteristics}。
 *
 * <p>支持的审批模式：
 * <ul>
 *   <li><b>parallel</b> - 并行会签：所有处理人同时收到任务，满足完成条件后流程继续</li>
 *   <li><b>sequential</b> - 串行会签：按顺序依次分配给处理人，满足完成条件后流程继续</li>
 * </ul>
 *
 * <p>装配后的 BPMN 结构示例（并行会签）：
 * <pre>
 *   userTask(taskKey)
 *     └─ loopCharacteristics (parallel)
 *          ├─ collection: ${assigneeList_taskKey}
 *          ├─ elementVariable: assignee
 *          └─ completionCondition: ${nrOfCompletedInstances/nrOfInstances >= 1}
 * </pre>
 */
@Service
public class BpmnMultiInstanceAssembleService
{
    private static final Logger log = LoggerFactory.getLogger(BpmnMultiInstanceAssembleService.class);

    @Autowired
    private LcMetadataService metadataService;

    /**
     * 为 BPMN XML 注入会签/或签的多实例特性。
     *
     * @param bpmnXml    原始 BPMN XML（可能已由定时器装配处理过）
     * @param processKey 流程 key
     * @return 注入会签后的 BPMN XML（无会签配置则返回原 XML）
     */
    public String assembleMultiInstance(String bpmnXml, String processKey)
    {
        if (bpmnXml == null || bpmnXml.isBlank() || processKey == null || processKey.isBlank())
        {
            return bpmnXml;
        }

        List<LcBizNodeAssignee> assignees = findMultiInstanceAssignees(processKey);
        if (assignees == null || assignees.isEmpty())
        {
            return bpmnXml;
        }

        log.info("BPMN 会签装配: processKey={}, assigneeCount={}", processKey, assignees.size());

        BpmnModel model;
        try
        {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(bpmnXml));
            model = new BpmnXMLConverter().convertToBpmnModel(reader);
        }
        catch (Exception e)
        {
            log.error("BPMN XML 解析失败，跳过会签装配: processKey={}", processKey, e);
            return bpmnXml;
        }

        Process process = model.getProcessById(processKey);
        if (process == null)
        {
            log.warn("BPMN 中未找到 process[{}], 跳过会签装配", processKey);
            return bpmnXml;
        }

        int injected = 0;
        for (LcBizNodeAssignee assignee : assignees)
        {
            if (injectMultiInstance(process, assignee))
            {
                injected++;
            }
        }

        if (injected == 0)
        {
            return bpmnXml;
        }

        try
        {
            String resultXml = new String(new BpmnXMLConverter().convertToXML(model),
                    java.nio.charset.StandardCharsets.UTF_8);
            log.info("BPMN 会签装配完成: processKey={}, injected={}", processKey, injected);
            return resultXml;
        }
        catch (Exception e)
        {
            log.error("BPMN XML 生成失败，回退原 XML: processKey={}", processKey, e);
            return bpmnXml;
        }
    }

    /**
     * 查询配置了多人审批的节点处理人。
     */
    private List<LcBizNodeAssignee> findMultiInstanceAssignees(String processKey)
    {
        LcBizObject query = new LcBizObject();
        query.setProcessKey(processKey);
        List<LcBizObject> objs = metadataService.selectBizObjectList(query);
        if (objs == null || objs.isEmpty())
        {
            return null;
        }
        String bizCode = objs.get(0).getBizCode();
        List<LcBizNodeAssignee> all = metadataService.selectNodeAssigneesByBizCode(bizCode);
        if (all == null)
        {
            return null;
        }
        return all.stream()
                .filter(a -> a.getMultiInstanceType() != null
                        && !"none".equals(a.getMultiInstanceType())
                        && !a.getMultiInstanceType().isBlank())
                .toList();
    }

    /**
     * 为单个 userTask 注入多实例特性。
     */
    private boolean injectMultiInstance(Process process, LcBizNodeAssignee assignee)
    {
        String taskKey = assignee.getTaskKey();
        FlowElement element = process.getFlowElement(taskKey);
        if (!(element instanceof UserTask))
        {
            log.warn("会签装配: taskKey={} 不是 UserTask，跳过", taskKey);
            return false;
        }

        UserTask userTask = (UserTask) element;

        // 如果已经有多实例配置则跳过（避免重复注入）
        if (userTask.getLoopCharacteristics() != null)
        {
            log.info("会签装配: taskKey={} 已有多实例配置，跳过", taskKey);
            return false;
        }

        String miType = assignee.getMultiInstanceType();
        boolean sequential = "sequential".equalsIgnoreCase(miType);

        MultiInstanceLoopCharacteristics loop = new MultiInstanceLoopCharacteristics();
        loop.setSequential(sequential);

        // 集合变量名：assigneeList_{taskKey}
        String collectionVar = "assigneeList_" + taskKey;
        loop.setInputDataItem("${" + collectionVar + "}");
        loop.setElementVariable("assignee");

        // 完成条件
        String completionCondition = assignee.getCompletionCondition();
        if (completionCondition == null || completionCondition.isBlank())
        {
            // 默认：全部通过（会签）
            completionCondition = "${nrOfCompletedInstances/nrOfInstances >= 1}";
        }
        loop.setCompletionCondition(completionCondition);

        userTask.setLoopCharacteristics(loop);

        // 设置 assignee 为 elementVariable，这样每个实例都会分配给对应的人
        userTask.setAssignee("${assignee}");

        log.info("会签装配: taskKey={}, type={}, sequential={}, condition={}",
                taskKey, miType, sequential, completionCondition);
        return true;
    }
}
