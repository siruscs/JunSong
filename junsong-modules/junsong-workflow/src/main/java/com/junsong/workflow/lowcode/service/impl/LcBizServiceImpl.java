package com.junsong.workflow.lowcode.service.impl;

import com.alibaba.fastjson2.JSON;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.workflow.lowcode.domain.LcBizInstance;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import com.junsong.workflow.lowcode.domain.dto.LcBizConfigDTO;
import com.junsong.workflow.lowcode.service.LcBizService;
import com.junsong.workflow.lowcode.service.LcConfigVersionService;
import com.junsong.workflow.lowcode.service.LcInstanceService;
import com.junsong.workflow.lowcode.service.LcMetadataService;
import com.junsong.workflow.lowcode.service.LcWorkflowAssembleService;
import com.junsong.workflow.lowcode.sync.ConfigurablePostActionHandler;
import com.junsong.workflow.lowcode.validator.LcSubmitValidator;
import com.junsong.workflow.security.CurrentWorkflowUser;
import com.junsong.workflow.security.CurrentWorkflowUserFacade;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LcBizServiceImpl implements LcBizService
{
    private final LcInstanceService instanceService;
    private final LcMetadataService metadataService;
    private final LcWorkflowAssembleService assembleService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final TaskService taskService;
    private final CurrentWorkflowUserFacade userFacade;
    private final ConfigurablePostActionHandler postActionHandler;
    private final LcConfigVersionService configVersionService;
    /** 提交前校验器注册表：按 type() 索引 */
    private final Map<String, LcSubmitValidator> validatorRegistry;

    public LcBizServiceImpl(
            LcInstanceService instanceService,
            LcMetadataService metadataService,
            LcWorkflowAssembleService assembleService,
            RuntimeService runtimeService,
            RepositoryService repositoryService,
            TaskService taskService,
            CurrentWorkflowUserFacade userFacade,
            ConfigurablePostActionHandler postActionHandler,
            LcConfigVersionService configVersionService,
            List<LcSubmitValidator> validators)
    {
        this.instanceService = instanceService;
        this.metadataService = metadataService;
        this.assembleService = assembleService;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.taskService = taskService;
        this.userFacade = userFacade;
        this.postActionHandler = postActionHandler;
        this.configVersionService = configVersionService;
        this.validatorRegistry = new HashMap<>();
        if (validators != null)
        {
            for (LcSubmitValidator v : validators)
            {
                this.validatorRegistry.put(v.type(), v);
            }
        }
    }

    /**
     * 运行态读取业务对象：优先读已发布快照，无快照则降级读草稿。
     * 保证在途流程不受草稿编辑影响。
     */
    private LcBizObject getPublishedBizObject(String bizCode)
    {
        String json = configVersionService.getLatestPublishedConfigJson(bizCode);
        if (json != null)
        {
            LcBizConfigDTO config = JSON.parseObject(json, LcBizConfigDTO.class);
            if (config.getBizObject() != null)
            {
                return config.getBizObject();
            }
        }
        return metadataService.selectBizObjectByBizCode(bizCode);
    }

    @Override
    public List<LcBizInstance> list(String bizCode, LcBizInstance query)
    {
        query.setBizCode(bizCode);
        return instanceService.selectLcBizInstanceList(query);
    }

    @Override
    public LcBizInstance getById(String bizCode, Long id)
    {
        LcBizInstance instance = instanceService.selectLcBizInstanceById(id);
        if (instance == null || !bizCode.equals(instance.getBizCode()))
        {
            return null;
        }
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> save(String bizCode, Map<String, Object> formData)
    {
        LcBizObject bizObject = metadataService.selectBizObjectByBizCode(bizCode);
        if (bizObject == null)
        {
            return R.fail("业务对象不存在: " + bizCode);
        }
        String orderNo = generateOrderNo(bizObject.getOrderNoPrefix());
        LcBizInstance instance = new LcBizInstance();
        instance.setBizCode(bizCode);
        instance.setOrderNo(orderNo);
        instance.setFormData(JSON.toJSONString(formData));
        instance.setWorkflowStatus("DRAFT");
        instance.setDelFlag("0");
        CurrentWorkflowUser user = userFacade.current();
        instance.setCreateBy(user.username());
        instance.setUpdateBy(user.username());
        instanceService.insertLcBizInstance(instance);
        return R.ok(instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> update(String bizCode, Long id, Map<String, Object> formData)
    {
        LcBizInstance instance = getById(bizCode, id);
        if (instance == null)
        {
            return R.fail("单据不存在");
        }
        instance.setFormData(JSON.toJSONString(formData));
        instance.setUpdateBy(userFacade.current().username());
        instanceService.updateLcBizInstance(instance);
        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> delete(String bizCode, Long[] ids)
    {
        // 校验待删除的实例是否属于该 bizCode，防止跨业务对象删除
        for (Long id : ids)
        {
            LcBizInstance instance = instanceService.selectLcBizInstanceById(id);
            if (instance == null || !bizCode.equals(instance.getBizCode()))
            {
                return R.fail("单据 [" + id + "] 不属于业务对象 [" + bizCode + "]，禁止删除");
            }
        }
        instanceService.deleteLcBizInstanceByIds(ids);
        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Map<String, Object>> submit(String bizCode, Long id)
    {
        LcBizInstance instance = getById(bizCode, id);
        if (instance == null)
        {
            return R.fail("单据不存在");
        }
        if (!"DRAFT".equals(instance.getWorkflowStatus()) && !"REJECTED".equals(instance.getWorkflowStatus()))
        {
            return R.fail("当前状态不允许提交");
        }
        LcBizObject bizObject = getPublishedBizObject(bizCode);
        if (bizObject == null || !"1".equals(bizObject.getWorkflowEnabled()))
        {
            return R.fail("业务对象未启用流程");
        }
        String processKey = bizObject.getProcessKey();
        if (processKey == null || processKey.isBlank())
        {
            return R.fail("业务对象未配置 processKey");
        }

        // 提交前校验（空间查重等配置化校验器）
        Map<String, Object> formData = JSON.parseObject(instance.getFormData(), Map.class);
        runSubmitValidators(bizObject, formData);

        // 装配流程变量
        CurrentWorkflowUser user = userFacade.current();
        Map<String, Object> variables = assembleService.assembleVariables(bizCode, formData, user.username());
        variables.put("initiator", user.username());

        // 发起流程（参考 ProcessInstanceController.start）
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();
        if (def == null)
        {
            return R.fail("找不到流程定义: " + processKey);
        }
        if (def.isSuspended())
        {
            return R.fail("流程定义已挂起: " + processKey);
        }
        Authentication.setAuthenticatedUserId(user.username());
        try
        {
            ProcessInstance pi = runtimeService.startProcessInstanceById(def.getId(), instance.getOrderNo(), variables);
            // 回写快照
            instance.setProcessInstanceId(pi.getId());
            instance.setProcessDefinitionKey(def.getKey());
            instance.setWorkflowStatus("IN_APPROVAL");
            // 取发起后的第一个活动任务名
            Task firstTask = taskService.createTaskQuery().processInstanceId(pi.getId()).active().singleResult();
            instance.setCurrentTaskName(firstTask != null ? firstTask.getName() : "已提交");
            instance.setSubmitter(user.username());
            instance.setSubmitTime(new java.util.Date());
            instance.setUpdateBy(user.username());
            instanceService.updateWorkflowSnapshot(instance);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("processInstanceId", pi.getId());
            result.put("processDefinitionKey", def.getKey());
            result.put("orderNo", instance.getOrderNo());
            return R.ok(result);
        }
        finally
        {
            Authentication.setAuthenticatedUserId(null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> withdraw(String bizCode, Long id)
    {
        LcBizInstance instance = getById(bizCode, id);
        if (instance == null)
        {
            return R.fail("单据不存在");
        }
        String pid = instance.getProcessInstanceId();
        if (pid == null || pid.isBlank())
        {
            return R.fail("未发起流程");
        }
        // 终止流程
        runtimeService.deleteProcessInstance(pid, "用户撤回");
        // 回写已撤回
        instance.setWorkflowStatus("WITHDRAWN");
        instance.setCurrentTaskName("已撤回");
        instance.setUpdateBy(userFacade.current().username());
        instanceService.updateWorkflowSnapshot(instance);

        // 触发配置化后置动作
        postActionHandler.onAfterWithdraw(pid, userFacade.current().username());

        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> fulfill(String bizCode, Long id, Map<String, Object> formData)
    {
        LcBizInstance instance = getById(bizCode, id);
        if (instance == null)
        {
            return R.fail("单据不存在");
        }
        if (!"PENDING_FULFILLMENT".equals(instance.getWorkflowStatus()))
        {
            return R.fail("当前状态不允许履约提交");
        }
        String pid = instance.getProcessInstanceId();
        if (pid == null || pid.isBlank())
        {
            return R.fail("流程实例不存在");
        }
        // 合并 formData（履约阶段字段）
        Map<String, Object> existing = JSON.parseObject(instance.getFormData(), Map.class);
        if (existing == null)
        {
            existing = new LinkedHashMap<>();
        }
        if (formData != null)
        {
            existing.putAll(formData);
        }
        instance.setFormData(JSON.toJSONString(existing));

        // 完成当前履约任务
        Task fulfillmentTask = taskService.createTaskQuery().processInstanceId(pid).active().singleResult();
        if (fulfillmentTask != null)
        {
            taskService.complete(fulfillmentTask.getId());
        }

        // 回写履约完成
        instance.setWorkflowStatus("FULFILLED");
        instance.setCurrentTaskName("履约完成");
        instance.setUpdateBy(userFacade.current().username());
        instanceService.updateLcBizInstance(instance);
        return R.ok();
    }

    @Override
    public void syncStatus(String processInstanceId, String workflowStatus, String currentTaskName, String operator)
    {
        LcBizInstance instance = instanceService.selectByProcessInstanceId(processInstanceId);
        if (instance == null)
        {
            return;
        }
        instance.setWorkflowStatus(workflowStatus);
        instance.setCurrentTaskName(currentTaskName);
        instance.setUpdateBy(operator);
        instanceService.updateWorkflowSnapshot(instance);
    }

    /**
     * 执行提交前校验器链。
     *
     * <p>读取业务对象配置的 {@code submit_validators}（JSON 数组），按 {@code type} 分发到对应实现。
     * 任一校验器抛出 {@link ServiceException} 即终止提交。
     *
     * @param bizObject 业务对象
     * @param formData  表单数据
     */
    private void runSubmitValidators(LcBizObject bizObject, Map<String, Object> formData)
    {
        String validatorsJson = bizObject.getSubmitValidators();
        if (StringUtils.isEmpty(validatorsJson))
        {
            return;
        }
        List<Map<String, Object>> configs;
        try
        {
            configs = JSON.parseObject(validatorsJson, List.class);
        }
        catch (Exception e)
        {
            throw new ServiceException("提交校验器配置解析失败: " + e.getMessage());
        }
        if (configs == null || configs.isEmpty())
        {
            return;
        }
        for (Map<String, Object> config : configs)
        {
            Object typeObj = config.get("type");
            String type = typeObj == null ? "" : typeObj.toString();
            LcSubmitValidator validator = validatorRegistry.get(type);
            if (validator == null)
            {
                throw new ServiceException("未找到提交校验器实现: " + type);
            }
            validator.validate(bizObject, formData, config);
        }
    }

    /** 生成单号：{prefix}-yyyyMMdd-{5位随机} */
    private String generateOrderNo(String prefix)
    {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(10000, 99999);
        String p = (prefix == null || prefix.isBlank()) ? "LC" : prefix;
        return p + "-" + date + "-" + rand;
    }
}