package com.junsong.workflow.lowcode.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.junsong.workflow.lowcode.service.LcBizService;

/**
 * 超时处理器（流程定时器超时分支调用）。
 *
 * <p>当 BPMN 边界定时器触发后，流程流向超时分支的 ServiceTask，
 * ServiceTask 通过 expression 调用本 Bean 的 {@link #handleTimeout} 方法，
 * 将业务实例状态回写为超时状态（如 CANCELLED / EXPIRED）。
 *
 * <p>BPMN 中的 ServiceTask expression 示例：
 * <pre>
 * flowable:expression = "${timeoutHandler.handleTimeout(execution, 'CANCELLED')}"
 * </pre>
 *
 * @author junsong
 */
@Component("timeoutHandler")
public class TimeoutHandler
{
    private static final Logger log = LoggerFactory.getLogger(TimeoutHandler.class);

    @Autowired
    private LcBizService bizService;

    /**
     * 处理流程节点超时，回写业务状态。
     *
     * @param execution    流程执行上下文（由 Flowable 注入）
     * @param targetStatus 超时后业务状态（如 CANCELLED / EXPIRED）
     */
    public void handleTimeout(DelegateExecution execution, String targetStatus)
    {
        String processInstanceId = execution.getProcessInstanceId();
        String processDefinitionId = execution.getProcessDefinitionId();
        log.warn("流程定时器超时触发: pid={}, defId={}, targetStatus={}",
                processInstanceId, processDefinitionId, targetStatus);

        try
        {
            bizService.syncStatus(processInstanceId, targetStatus, "超时自动" + targetStatus, "SYSTEM_TIMER");
            log.info("超时状态回写成功: pid={}, status={}", processInstanceId, targetStatus);
        }
        catch (Exception e)
        {
            log.error("超时状态回写失败: pid={}", processInstanceId, e);
            // 不抛异常，避免流程引擎重试导致死循环
        }
    }
}
