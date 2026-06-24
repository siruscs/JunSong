package com.junsong.workflow.lowcode.sync;

import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.service.LcBizService;
import com.junsong.workflow.lowcode.service.LcInstanceService;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import com.junsong.workflow.service.sync.WorkflowBusinessSyncHandler;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

/**
 * 低代码通用流程状态回写 Handler。
 * 匹配所有已登记为 GENERIC 且启用流程的业务对象 processKey。
 * 审批推进后根据流程状态回写 lc_biz_instance。
 */
@Component
public class GenericLowcodeWorkflowSyncHandler implements WorkflowBusinessSyncHandler
{
    private final LcMetadataService metadataService;
    private final LcBizService bizService;
    private final LcInstanceService instanceService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /** 缓存 GENERIC workflow processKey 集合（简单内存缓存，配置变更后需重启） */
    private volatile Set<String> genericProcessKeys = null;

    public GenericLowcodeWorkflowSyncHandler(
            LcMetadataService metadataService,
            LcBizService bizService,
            LcInstanceService instanceService,
            RuntimeService runtimeService,
            TaskService taskService)
    {
        this.metadataService = metadataService;
        this.bizService = bizService;
        this.instanceService = instanceService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Override
    public boolean supports(String processDefinitionId)
    {
        if (processDefinitionId == null || processDefinitionId.isBlank())
        {
            return false;
        }
        // processDefinitionId 形如 "key:ver:id"，取冒号前的 key
        String key = extractKey(processDefinitionId);
        return getGenericProcessKeys().contains(key);
    }

    @Override
    public void afterApprove(String currentTaskName, String processInstanceId, String operator, Map<String, Object> variables)
    {
        // 判断流程是否还有活动任务
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().list();

        if (pi == null && (activeTasks == null || activeTasks.isEmpty()))
        {
            // 流程已结束 → APPROVED
            bizService.syncStatus(processInstanceId, "APPROVED", "审批完成", operator);
            return;
        }

        if (!activeTasks.isEmpty())
        {
            Task nextTask = activeTasks.get(0);
            String nextTaskName = nextTask.getName();
            String nextTaskKey = nextTask.getTaskDefinitionKey();

            // 判断是否履约任务：通过 lc_biz_node_assignee 中该 task 配置 assignee_source='INITIATOR'
            // 或业务对象 fulfillment_enabled='1' 且这是审批通过后的首个任务
            boolean isFulfillment = isFulfillmentTask(processInstanceId, nextTaskKey);
            if (isFulfillment)
            {
                bizService.syncStatus(processInstanceId, "PENDING_FULFILLMENT", nextTaskName, operator);
            }
            else
            {
                bizService.syncStatus(processInstanceId, "IN_APPROVAL", nextTaskName, operator);
            }
        }
        else
        {
            // 流程实例仍存在但无活动任务 → APPROVED
            bizService.syncStatus(processInstanceId, "APPROVED", "审批完成", operator);
        }
    }

    @Override
    public void afterReject(String processInstanceId, String operator)
    {
        bizService.syncStatus(processInstanceId, "REJECTED", "审批驳回", operator);
    }

    private Set<String> getGenericProcessKeys()
    {
        if (genericProcessKeys == null)
        {
            synchronized (this)
            {
                if (genericProcessKeys == null)
                {
                    Set<String> keys = ConcurrentHashMap.newKeySet();
                    List<LcBizObject> objects = metadataService.selectGenericWorkflowObjects();
                    if (objects != null)
                    {
                        for (LcBizObject obj : objects)
                        {
                            if (obj.getProcessKey() != null && !obj.getProcessKey().isBlank())
                            {
                                keys.add(obj.getProcessKey());
                            }
                        }
                    }
                    genericProcessKeys = keys;
                }
            }
        }
        return genericProcessKeys;
    }

    private String extractKey(String processDefinitionId)
    {
        int colon = processDefinitionId.indexOf(':');
        if (colon > 0)
        {
            return processDefinitionId.substring(0, colon);
        }
        return processDefinitionId;
    }

    /**
     * 判断是否履约任务。
     * 规则：通过流程实例反查 bizCode → 查 lc_biz_node_assignee，
     * 若该 taskKey 节点处理人来源为 INITIATOR（履约人=发起人本人）且业务对象启用履约，则视为履约任务。
     */
    private boolean isFulfillmentTask(String processInstanceId, String taskKey)
    {
        if (taskKey == null)
        {
            return false;
        }
        LcBizInstance instance = instanceService.selectByProcessInstanceId(processInstanceId);
        if (instance == null || instance.getBizCode() == null)
        {
            return false;
        }
        String bizCode = instance.getBizCode();
        LcBizObject bizObject = metadataService.selectBizObjectByBizCode(bizCode);
        if (bizObject == null || !"1".equals(bizObject.getFulfillmentEnabled()))
        {
            return false;
        }
        List<LcBizNodeAssignee> assignees = metadataService.selectNodeAssigneesByBizCode(bizCode);
        if (assignees != null)
        {
            for (LcBizNodeAssignee assignee : assignees)
            {
                if (taskKey.equals(assignee.getTaskKey()) && "INITIATOR".equals(assignee.getAssigneeSource()))
                {
                    return true;
                }
            }
        }
        return false;
    }
}