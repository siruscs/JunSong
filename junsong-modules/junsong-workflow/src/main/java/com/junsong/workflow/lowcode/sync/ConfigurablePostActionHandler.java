package com.junsong.workflow.lowcode.sync;

import com.alibaba.fastjson2.JSON;
import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.domain.LcBizPostAction;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import com.junsong.workflow.lowcode.mapper.LcBizPostActionMapper;
import com.junsong.workflow.lowcode.service.LcBizService;
import com.junsong.workflow.lowcode.service.LcConfigVersionService;
import com.junsong.workflow.lowcode.service.LcInstanceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.flowable.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 配置化后置动作执行器。
 * 从 lc_biz_post_action 读取配置，在流程事件触发时执行配置化的后置动作。
 * 作为独立组件被 LcBizServiceImpl 和 TaskController 直接调用，
 * 与 WorkflowBusinessSyncHandler 链并行执行（不互斥）。
 */
@Component
public class ConfigurablePostActionHandler
{
    private static final Logger log = LoggerFactory.getLogger(ConfigurablePostActionHandler.class);

    @Autowired
    private LcBizPostActionMapper postActionMapper;

    @Autowired
    private LcInstanceService instanceService;

    @Autowired
    @Lazy
    private LcBizService bizService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private LcConfigVersionService configVersionService;

    /** 提交后触发 */
    public void onAfterSubmit(String processInstanceId, String operator)
    {
        executePostActions("AFTER_SUBMIT", processInstanceId, operator, Map.of());
    }

    /** 审批通过后触发 */
    public void onAfterApprove(String processInstanceId, String operator, Map<String, Object> variables)
    {
        executePostActions("AFTER_APPROVE", processInstanceId, operator, variables);
    }

    /** 驳回后触发 */
    public void onAfterReject(String processInstanceId, String operator)
    {
        executePostActions("AFTER_REJECT", processInstanceId, operator, Map.of());
    }

    /** 撤回后触发 */
    public void onAfterWithdraw(String processInstanceId, String operator)
    {
        executePostActions("AFTER_WITHDRAW", processInstanceId, operator, Map.of());
    }

    /** 履约后触发 */
    public void onAfterFulfill(String processInstanceId, String operator)
    {
        executePostActions("AFTER_FULFILL", processInstanceId, operator, Map.of());
    }

    private void executePostActions(String triggerEvent, String processInstanceId,
                                     String operator, Map<String, Object> variables)
    {
        LcBizInstance instance = instanceService.selectByProcessInstanceId(processInstanceId);
        if (instance == null || instance.getBizCode() == null)
        {
            return;
        }
        String bizCode = instance.getBizCode();
        List<LcBizPostAction> actions = postActionMapper.selectByBizCodeAndEvent(bizCode, triggerEvent);
        if (actions == null || actions.isEmpty())
        {
            return;
        }
        for (LcBizPostAction action : actions)
        {
            try
            {
                if (!evaluateCondition(action.getConditionExpr(), variables))
                {
                    continue;
                }
                executeAction(action, instance, processInstanceId, operator);
            }
            catch (Exception e)
            {
                log.error("执行后置动作失败: bizCode={}, event={}, actionType={}", bizCode, triggerEvent, action.getActionType(), e);
            }
        }
    }

    /**
     * 优先从已发布快照读取 postActions，无快照则降级读草稿。
     * 保证在途流程的后置动作不受草稿编辑影响。
     */
    private List<LcBizPostAction> getPublishedPostActions(String bizCode, String triggerEvent)
    {
        String json = configVersionService.getLatestPublishedConfigJson(bizCode);
        if (json != null)
        {
            LcBizConfigDTO config = JSON.parseObject(json, LcBizConfigDTO.class);
            if (config.getPostActions() != null)
            {
                List<LcBizPostAction> filtered = new ArrayList<>();
                for (LcBizPostAction a : config.getPostActions())
                {
                    if (triggerEvent.equals(a.getTriggerEvent())
                            && "0".equals(a.getStatus())
                            && "0".equals(a.getDelFlag()))
                    {
                        filtered.add(a);
                    }
                }
                return filtered;
            }
        }
        return postActionMapper.selectByBizCodeAndEvent(bizCode, triggerEvent);
    }

    private boolean evaluateCondition(String conditionExpr, Map<String, Object> variables)
    {
        if (conditionExpr == null || conditionExpr.isBlank())
        {
            return true;
        }
        // 简单 key==value 格式
        String[] parts = conditionExpr.split("==");
        if (parts.length != 2)
        {
            return true;
        }
        String key = parts[0].trim();
        String expected = parts[1].trim();
        Object actual = variables == null ? null : variables.get(key);
        return expected.equals(String.valueOf(actual));
    }

    private void executeAction(LcBizPostAction action, LcBizInstance instance,
                                String processInstanceId, String operator)
    {
        String actionType = action.getActionType();
        switch (actionType)
        {
            case "UPDATE_STATUS":
                bizService.syncStatus(processInstanceId, action.getTargetValue(), "配置化状态流转", operator);
                break;
            default:
                log.warn("未知后置动作类型: {}", actionType);
        }
    }
}
