package com.junsong.workflow.lowcode.service;

import java.io.StringReader;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.workflow.lowcode.domain.LcBizNodeTimer;
import com.junsong.workflow.lowcode.domain.LcBizObject;

/**
 * BPMN 定时器装配服务。
 *
 * <p>在流程部署前，读取 {@code lc_biz_node_timer} 配置，
 * 为配置了定时器的 userTask 动态挂载边界定时器（Timer Boundary Event）+ 超时处理分支。
 *
 * <p>装配后的 BPMN 结构：
 * <pre>
 *   userTask(taskKey)
 *     └─ boundaryEvent(timer_{taskKey}, cancelActivity=true, timeDuration={duration})
 *          └─ sequenceFlow ─▶ serviceTask(timeout_svc_{taskKey}, expression=timeoutHandler.handleTimeout)
 *                               └─ sequenceFlow ─▶ endEvent(end_timeout_{taskKey})
 * </pre>
 *
 * <p>超时触发后，ServiceTask 调用 {@link TimeoutHandler#handleTimeout} 回写业务状态。
 *
 * @author junsong
 */
@Service
public class BpmnTimerAssembleService
{
    private static final Logger log = LoggerFactory.getLogger(BpmnTimerAssembleService.class);

    @Autowired
    private LcMetadataService metadataService;

    /**
     * 为 BPMN XML 注入节点定时器边界事件。
     *
     * @param bpmnXml    原始 BPMN XML
     * @param processKey 流程 key（用于查 bizCode → nodeTimers）
     * @return 注入定时器后的 BPMN XML（无定时器配置则返回原 XML）
     */
    public String assembleTimers(String bpmnXml, String processKey)
    {
        if (bpmnXml == null || bpmnXml.isBlank() || processKey == null || processKey.isBlank())
        {
            return bpmnXml;
        }

        // 1. 通过 processKey 查 bizCode → nodeTimers
        List<LcBizNodeTimer> timers = findNodeTimers(processKey);
        if (timers == null || timers.isEmpty())
        {
            return bpmnXml;
        }

        log.info("BPMN 定时器装配: processKey={}, timerCount={}", processKey, timers.size());

        // 2. 解析 XML → BpmnModel
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
            log.error("BPMN XML 解析失败，跳过定时器装配: processKey={}", processKey, e);
            return bpmnXml;
        }

        Process process = model.getProcessById(processKey);
        if (process == null)
        {
            log.warn("BPMN 中未找到 process[{}], 跳过定时器装配", processKey);
            return bpmnXml;
        }

        // 3. 为每个定时器配置挂边界事件
        int injected = 0;
        for (LcBizNodeTimer timer : timers)
        {
            if (injectTimer(process, timer))
            {
                injected++;
            }
        }

        if (injected == 0)
        {
            return bpmnXml;
        }

        // 4. BpmnModel → XML
        try
        {
            String resultXml = new String(new BpmnXMLConverter().convertToXML(model),
                    java.nio.charset.StandardCharsets.UTF_8);
            log.info("BPMN 定时器装配完成: processKey={}, injected={}", processKey, injected);
            return resultXml;
        }
        catch (Exception e)
        {
            log.error("BPMN XML 生成失败，回退原 XML: processKey={}", processKey, e);
            return bpmnXml;
        }
    }

    /**
     * 通过 processKey 查询节点定时器配置。
     * processKey → LcBizObject.bizCode → lc_biz_node_timer
     */
    private List<LcBizNodeTimer> findNodeTimers(String processKey)
    {
        LcBizObject query = new LcBizObject();
        query.setProcessKey(processKey);
        List<LcBizObject> objs = metadataService.selectBizObjectList(query);
        if (objs == null || objs.isEmpty())
        {
            return null;
        }
        String bizCode = objs.get(0).getBizCode();
        return metadataService.selectNodeTimersByBizCode(bizCode);
    }

    /**
     * 为单个 userTask 挂载边界定时器 + 超时处理分支。
     *
     * @return true 注入成功, false 跳过（taskKey 不存在等）
     */
    private boolean injectTimer(Process process, LcBizNodeTimer timer)
    {
        String taskKey = timer.getTaskKey();
        FlowElement element = process.getFlowElement(taskKey);
        if (!(element instanceof UserTask))
        {
            log.warn("定时器装配: taskKey={} 不是 UserTask，跳过", taskKey);
            return false;
        }
        UserTask userTask = (UserTask) element;

        // 避免重复注入（如果已有同 id 的边界事件则跳过）
        String boundaryId = "timer_" + taskKey;
        if (process.getFlowElement(boundaryId) != null)
        {
            log.info("定时器装配: taskKey={} 已有边界定时器，跳过", taskKey);
            return false;
        }

        String duration = timer.getDuration();
        String targetStatus = timer.getTargetStatus() != null ? timer.getTargetStatus() : "CANCELLED";
        String taskName = timer.getTaskName() != null ? timer.getTaskName() : taskKey;

        // 1. 边界定时器
        BoundaryEvent boundary = new BoundaryEvent();
        boundary.setId(boundaryId);
        boundary.setAttachedToRefId(taskKey);
        boundary.setAttachedToRef(userTask);
        boundary.setCancelActivity(true);  // 中断型：取消当前任务
        TimerEventDefinition timerDef = new TimerEventDefinition();
        timerDef.setTimeDuration(duration);  // ISO-8601: P3D / P45D
        boundary.addEventDefinition(timerDef);

        // 2. 超时处理 ServiceTask（expression 调用 TimeoutHandler）
        String svcId = "timeout_svc_" + taskKey;
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId(svcId);
        serviceTask.setName("超时处理-" + taskName);
        serviceTask.setImplementationType("expression");
        serviceTask.setImplementation("${timeoutHandler.handleTimeout(execution, '" + targetStatus + "')}");

        // 3. 超时结束节点
        String endId = "end_timeout_" + taskKey;
        EndEvent endEvent = new EndEvent();
        endEvent.setId(endId);
        endEvent.setName("超时结束-" + taskName);

        // 4. 连线
        SequenceFlow flow1 = new SequenceFlow();
        flow1.setId("flow_timer_to_svc_" + taskKey);
        flow1.setSourceRef(boundaryId);
        flow1.setTargetRef(svcId);
        flow1.setSourceFlowElement(boundary);
        flow1.setTargetFlowElement(serviceTask);

        SequenceFlow flow2 = new SequenceFlow();
        flow2.setId("flow_svc_to_end_" + taskKey);
        flow2.setSourceRef(svcId);
        flow2.setTargetRef(endId);
        flow2.setSourceFlowElement(serviceTask);
        flow2.setTargetFlowElement(endEvent);

        // 5. 添加到 Process
        process.addFlowElement(boundary);
        process.addFlowElement(serviceTask);
        process.addFlowElement(endEvent);
        process.addFlowElement(flow1);
        process.addFlowElement(flow2);

        log.info("定时器装配: taskKey={}, duration={}, targetStatus={}", taskKey, duration, targetStatus);
        return true;
    }
}
