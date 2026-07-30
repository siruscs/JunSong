package com.junsong.workflow.lowcode.sync;

import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.domain.LcBizNodeAssignee;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.event.LcConfigPublishedEvent;
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
import com.junsong.workflow.mapper.WfSysUserMapper;
import org.springframework.context.event.EventListener;
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
    private final WfSysUserMapper sysUserMapper;

    /** 缓存 GENERIC workflow processKey 集合（监听配置发布事件自动刷新） */
    private volatile Set<String> genericProcessKeys = null;

    /**
     * 监听低代码配置发布事件，清空缓存以便下次重新加载。
     * 解决"新增 GENERIC 业务对象后审批状态回写失效直到重启"的问题。
     */
    @EventListener
    public void onConfigPublished(LcConfigPublishedEvent event)
    {
        genericProcessKeys = null;
    }

    public GenericLowcodeWorkflowSyncHandler(
            LcMetadataService metadataService,
            LcBizService bizService,
            LcInstanceService instanceService,
            RuntimeService runtimeService,
            TaskService taskService,
            WfSysUserMapper sysUserMapper)
    {
        this.metadataService = metadataService;
        this.bizService = bizService;
        this.instanceService = instanceService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.sysUserMapper = sysUserMapper;
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
        // 盘点有专用处理器，必须进入原生状态机完成过账和库存流水，不能被通用状态回写吞掉。
        if ("stocktake_apply".equals(key))
        {
            return false;
        }
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
            bindConfiguredAssignee(processInstanceId, nextTask, variables);
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

    /** 将低代码节点配置绑定到刚生成的运行时任务，避免 BPMN 未写 assignee 时形成孤儿待办。 */
    private void bindConfiguredAssignee(String processInstanceId, Task task, Map<String, Object> variables)
    {
        LcBizInstance instance = instanceService.selectByProcessInstanceId(processInstanceId);
        String bizCode = instance == null ? extractKey(task.getProcessDefinitionId()) : instance.getBizCode();
        if (bizCode == null || bizCode.isBlank()) return;
        List<LcBizNodeAssignee> configs = metadataService.selectNodeAssigneesByBizCode(bizCode);
        if (configs == null) return;
        for (LcBizNodeAssignee config : configs)
        {
            if (!task.getTaskDefinitionKey().equals(config.getTaskKey())) continue;
            String source = config.getAssigneeSource();
            String target = null;
            if ("FIXED_USER".equals(source))
            {
                String raw = config.getAssigneeValue();
                if (raw != null && raw.matches("\\d+"))
                {
                    target = sysUserMapper.selectUserNameByUserId(Long.valueOf(raw));
                }
                else target = raw;
            }
            else if ("INITIATOR".equals(source)) target = String.valueOf(variables.get("initiator"));
            else
            {
                String varName = config.getProcessVarName() == null || config.getProcessVarName().isBlank()
                        ? config.getTaskKey() : config.getProcessVarName();
                Object value = variables.get(varName);
                if (value instanceof String) target = (String) value;
            }
            if (target != null && !target.isBlank())
            {
                taskService.setAssignee(task.getId(), target.trim());
            }
            return;
        }
    }

    @Override
    public void afterReject(String processInstanceId, String operator)
    {
        LcBizInstance instance = instanceService.selectByProcessInstanceId(processInstanceId);
        if (instance != null
                && "REJECTED".equals(instance.getWorkflowStatus())
                && "发起人修改".equals(instance.getCurrentTaskName()))
        {
            return;
        }
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
